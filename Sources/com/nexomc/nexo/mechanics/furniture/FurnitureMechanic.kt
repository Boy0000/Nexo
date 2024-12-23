package com.nexomc.nexo.mechanics.furniture

import com.nexomc.nexo.NexoPlugin
import com.nexomc.nexo.api.NexoItems
import com.nexomc.nexo.compatibilities.blocklocker.BlockLockerMechanic
import com.nexomc.nexo.items.ItemBuilder
import com.nexomc.nexo.mechanics.Mechanic
import com.nexomc.nexo.mechanics.MechanicFactory
import com.nexomc.nexo.mechanics.breakable.BreakableMechanic
import com.nexomc.nexo.mechanics.furniture.evolution.EvolvingFurniture
import com.nexomc.nexo.mechanics.furniture.hitbox.FurnitureHitbox
import com.nexomc.nexo.mechanics.furniture.jukebox.JukeboxBlock
import com.nexomc.nexo.mechanics.furniture.seats.FurnitureSeat
import com.nexomc.nexo.mechanics.light.LightMechanic
import com.nexomc.nexo.mechanics.limitedplacing.LimitedPlacing
import com.nexomc.nexo.mechanics.storage.StorageMechanic
import com.nexomc.nexo.mechanics.storage.StorageType
import com.nexomc.nexo.utils.AdventureUtils
import com.nexomc.nexo.utils.BlockHelpers.toCenterBlockLocation
import com.nexomc.nexo.utils.PluginUtils.isEnabled
import com.nexomc.nexo.utils.VersionUtil
import com.nexomc.nexo.utils.actions.ClickAction
import com.nexomc.nexo.utils.actions.ClickAction.Companion.parseList
import com.nexomc.nexo.utils.blocksounds.BlockSounds
import com.nexomc.nexo.utils.logs.Logs
import com.jeff_media.morepersistentdatatypes.DataType
import com.nexomc.nexo.api.NexoFurniture
import com.nexomc.nexo.configs.Message
import com.nexomc.nexo.mechanics.furniture.hitbox.BarrierHitbox
import com.nexomc.nexo.mechanics.furniture.rotatable.Rotatable
import com.nexomc.nexo.utils.BlockHelpers
import com.ticxo.modelengine.api.ModelEngineAPI
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.NamespacedKey
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.Entity
import org.bukkit.entity.Interaction
import org.bukkit.entity.ItemDisplay
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.joml.Quaternionf
import org.joml.Vector3f

class FurnitureMechanic(mechanicFactory: MechanicFactory?, section: ConfigurationSection) :
    Mechanic(mechanicFactory, section, { itemBuilder: ItemBuilder ->
        itemBuilder.customTag<Byte, Byte>(FURNITURE_KEY, PersistentDataType.BYTE, 1.toByte())
    }) {
    val limitedPlacing: LimitedPlacing? = section.getConfigurationSection("limited_placing")?.let(::LimitedPlacing)
    val storage: StorageMechanic? = section.getConfigurationSection("storage")?.let(::StorageMechanic)
    val blockSounds: BlockSounds? = section.getConfigurationSection("block_sounds")?.let(::BlockSounds)
    val jukebox: JukeboxBlock? = section.getConfigurationSection("jukebox")?.let(::JukeboxBlock)
    val farmlandRequired: Boolean = section.getBoolean("farmland_required", false)
    val evolution: EvolvingFurniture? = section.getConfigurationSection("evolution")?.let(::EvolvingFurniture)?.apply { (factory as FurnitureFactory).registerEvolution() }
    val light: LightMechanic
    val lightIsToggleable: Boolean = section.getBoolean("lights_toggleable")
    val modelEngineID: String? = section.getString("modelengine_id", null)
    val placedItemId: String = section.getString("item", itemID)!!
    val placedItemModel: Key? = section.getString("item_model")?.let(Key::key)
    val seats = section.getStringList("seats").mapNotNull(FurnitureSeat::getSeat)
    val clickActions: List<ClickAction> = parseList(section)
    val properties: FurnitureProperties = section.getConfigurationSection("properties")?.let(::FurnitureProperties) ?: FurnitureProperties()
    val rotatable: Rotatable = section.get("rotatable")?.let(::Rotatable) ?: Rotatable()
    val blockLocker: BlockLockerMechanic? = section.getConfigurationSection("blocklocker")?.let(::BlockLockerMechanic)
    val restrictedRotation: RestrictedRotation = section.getString("restricted_rotation")?.let(RestrictedRotation::fromString) ?: RestrictedRotation.STRICT
    val breakable: BreakableMechanic = BreakableMechanic(section)

    val hitbox: FurnitureHitbox = section.getConfigurationSection("hitbox")?.let(::FurnitureHitbox) ?: FurnitureHitbox.EMPTY

    enum class RestrictedRotation {
        NONE, STRICT, VERY_STRICT;

        companion object {
            fun fromString(string: String?) = entries.firstOrNull { it.name.uppercase() == string?.uppercase() } ?: STRICT.apply {
                Logs.logError("Invalid restricted rotation: $string")
                Logs.logError("Allowed ones are: ${entries.joinToString()}")
                Logs.logWarn("Setting to $this")
            }
        }
    }

    init {
        val barrierHitboxes = hitbox.barriers().map(BarrierHitbox::toVector3f)
        val lightBlocks = LightMechanic(section).lightBlocks
        val overlap = lightBlocks.filter { it.toVector3f() in barrierHitboxes }.joinToString()
        if (overlap.isNotEmpty()) {
            Logs.logError("Furniture $itemID has lights that overlap with the barrierHitboxes at: $overlap")
            Logs.logWarn("Nexo will ignore any lights that conflict with a barrier...")
        }

        this.light = lightBlocks.filter { it.toVector3f() !in barrierHitboxes }.let(::LightMechanic)
    }

    val isModelEngine: Boolean
        get() = modelEngineID != null

    fun hasLimitedPlacing(): Boolean {
        return limitedPlacing != null
    }

    fun isStorage() = storage != null
    fun hasBlockSounds() = blockSounds != null
    fun isJukebox() = jukebox != null
    fun hasSeats() = seats.isNotEmpty()
    fun hasEvolution() = evolution != null
    val isInteractable = rotatable.rotatable || hasSeats() || isStorage()

    fun place(location: Location) = place(location, 0f, BlockFace.NORTH, true)

    fun place(location: Location, yaw: Float, facing: BlockFace) = place(location, yaw, facing, true)

    fun place(location: Location, yaw: Float, facing: BlockFace, checkSpace: Boolean): ItemDisplay? {
        if (!location.isWorldLoaded()) return null
        if (checkSpace && this.notEnoughSpace(location, yaw)) return null
        checkNotNull(location.getWorld())

        val baseEntity = location.getWorld().spawn(correctedSpawnLocation(location, facing), ItemDisplay::class.java) { e ->
            setBaseFurnitureData(e, yaw, facing)
        }

        if (this.isModelEngine && isEnabled("ModelEngine")) spawnModelEngineFurniture(baseEntity)
        FurnitureSeat.spawnSeats(baseEntity, this)

        return baseEntity
    }

    private fun correctedSpawnLocation(baseLocation: Location, facing: BlockFace): Location {
        val isWall = limitedPlacing?.isWall == true
        val isRoof = limitedPlacing?.isRoof == true
        val isFixed = properties.displayTransform == ItemDisplay.ItemDisplayTransform.FIXED
        val correctedLocation = when {
            isFixed && facing == BlockFace.UP -> toCenterBlockLocation(baseLocation)
            else -> BlockHelpers.toCenterLocation(baseLocation)
        }

        if (properties.isNoneTransform && !isWall && !isRoof) return correctedLocation
        val scale = properties.scale.y()
        // Since roof-furniture need to be more or less flipped, we have to add 0.5 (0.49 or it is "inside" the block above) to the Y coordinate
        if (isFixed && isWall && facing.modY == 0)
            correctedLocation.add(-facing.modX * (0.49 * scale.times(2)), 0.0, -facing.modZ * (0.49 * scale.times(2)))

        val hitboxOffset = (hitbox.hitboxHeight() - 1).toFloat()

        return correctedLocation.apply {
            y += (if (isRoof && facing == BlockFace.DOWN) if (isFixed) 0.49 else (-1 * hitboxOffset).toDouble() else 0.0)
        }
    }

    private fun setBaseFurnitureData(baseEntity: ItemDisplay, yaw: Float, blockFace: BlockFace) {
        var yaw = yaw
        baseEntity.isPersistent = true
        baseEntity.isInvulnerable = true
        baseEntity.isSilent = true
        baseEntity.isCustomNameVisible = false
        val item = NexoItems.itemFromId(itemID)
        val customName = item?.itemName ?: item?.displayName ?: Component.text(itemID)
        if (VersionUtil.isPaperServer) baseEntity.customName(customName)
        else baseEntity.customName = AdventureUtils.LEGACY_SERIALIZER.serialize(customName)

        val pitch: Float
        if (limitedPlacing != null && properties.isFixedTransform) {
            pitch = when {
                limitedPlacing.isFloor && blockFace == BlockFace.UP -> -90f
                limitedPlacing.isRoof && blockFace == BlockFace.DOWN -> 90f
                else -> 0f
            }

            if (limitedPlacing.isWall && blockFace.modY == 0) yaw = 90f * blockFace.ordinal - 180
        } else pitch = 0f

        baseEntity.setRotation(yaw, pitch)

        // No Packet-logic for Spigot Servers, so we set this here
        if (!VersionUtil.isPaperServer) {
            baseEntity.itemDisplayTransform = properties.displayTransform
            if (properties.isFixedTransform)
                baseEntity.transformation = baseEntity.transformation.apply { scale.set(0.5) }
            if (!VersionUtil.isPaperServer) baseEntity.setItemStack(item?.build())
        }

        val pdc = baseEntity.persistentDataContainer
        pdc.set(FURNITURE_KEY, PersistentDataType.STRING, itemID)
        if (hasEvolution()) pdc.set(EVOLUTION_KEY, PersistentDataType.INTEGER, 0)
        if (storage?.storageType == StorageType.STORAGE) {
            pdc.set<ByteArray, Array<ItemStack>>(StorageMechanic.STORAGE_KEY, DataType.ITEM_STACK_ARRAY, arrayOf())
        }
    }

    private fun spawnModelEngineFurniture(entity: ItemDisplay) {
        val modelEntity = ModelEngineAPI.getOrCreateModeledEntity(entity)
        val activeModel = ModelEngineAPI.createActiveModel(modelEngineID)
        modelEntity.addModel(activeModel, true)
        modelEntity.isModelRotationLocked = false
        modelEntity.isBaseEntityVisible = false
    }

    fun removeBaseEntity(baseEntity: ItemDisplay) {
        if (hasSeats()) FurnitureSeat.removeSeats(baseEntity)
        val packetManager = FurnitureFactory.instance()?.packetManager()
        packetManager?.removeFurnitureEntityPacket(baseEntity, this)
        packetManager?.removeInteractionHitboxPacket(baseEntity, this)
        packetManager?.removeBarrierHitboxPacket(baseEntity, this)
        packetManager?.removeLightMechanicPacket(baseEntity, this)

        if (!baseEntity.isDead) baseEntity.remove()
    }

    fun notEnoughSpace(baseEntity: ItemDisplay, yaw: Float = baseEntity.location.yaw): Boolean {
        return hitbox.hitboxLocations(baseEntity.location, yaw).any { !BlockHelpers.isReplaceable(it.block, baseEntity.uniqueId) }
    }

    fun notEnoughSpace(rootLocation: Location, yaw: Float): Boolean {
        return hitbox.hitboxLocations(rootLocation.clone(), yaw).any { !BlockHelpers.isReplaceable(it.block) }
    }

    fun runClickActions(player: Player) {
        for (action: ClickAction in clickActions) if (action.canRun(player)) action.performActions(player)
    }

    fun baseEntity(block: Block?): ItemDisplay? {
        if (block == null) return null
        return IFurniturePacketManager.baseEntityFromHitbox(BlockLocation(block.location))
    }

    fun baseEntity(location: Location?): ItemDisplay? {
        if (location == null) return null
        return IFurniturePacketManager.baseEntityFromHitbox(BlockLocation(location))
    }

    fun baseEntity(blockLocation: BlockLocation): ItemDisplay? {
        return IFurniturePacketManager.baseEntityFromHitbox(blockLocation)
    }

    fun baseEntity(interactionId: Int): ItemDisplay? {
        return IFurniturePacketManager.baseEntityFromHitbox(interactionId)
    }

    fun rotateFurniture(baseEntity: ItemDisplay) {
        val newYaw = baseEntity.location.yaw + (if (restrictedRotation == RestrictedRotation.VERY_STRICT) 45f else 22.5f)
        if (notEnoughSpace(baseEntity, newYaw)) return
        baseEntity.setRotation(newYaw, baseEntity.location.pitch)
        hitbox.refreshHitboxes(baseEntity, this)
    }

    companion object {
        val FURNITURE_KEY = NamespacedKey(NexoPlugin.instance(), "furniture")
        val FURNITURE_DYE_KEY = NamespacedKey(NexoPlugin.instance(), "furniture_dye")
        val FURNITURE_LIGHT_KEY = NamespacedKey(NexoPlugin.instance(), "furniture_light")
        val MODELENGINE_KEY = NamespacedKey(NexoPlugin.instance(), "modelengine")
        val EVOLUTION_KEY = NamespacedKey(NexoPlugin.instance(), "evolution")
    }
}
