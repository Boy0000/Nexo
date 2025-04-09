package com.nexomc.nexo.mechanics.furniture

import com.google.common.base.Preconditions
import com.jeff_media.morepersistentdatatypes.DataType
import com.nexomc.nexo.NexoPlugin
import com.nexomc.nexo.api.NexoItems
import com.nexomc.nexo.compatibilities.blocklocker.BlockLockerMechanic
import com.nexomc.nexo.items.ItemBuilder
import com.nexomc.nexo.mechanics.Mechanic
import com.nexomc.nexo.mechanics.MechanicFactory
import com.nexomc.nexo.mechanics.breakable.BreakableMechanic
import com.nexomc.nexo.mechanics.furniture.bed.FurnitureBed
import com.nexomc.nexo.mechanics.furniture.evolution.EvolvingFurniture
import com.nexomc.nexo.mechanics.furniture.hitbox.BarrierHitbox
import com.nexomc.nexo.mechanics.furniture.hitbox.FurnitureHitbox
import com.nexomc.nexo.mechanics.furniture.jukebox.JukeboxBlock
import com.nexomc.nexo.mechanics.furniture.rotatable.Rotatable
import com.nexomc.nexo.mechanics.furniture.seats.FurnitureSeat
import com.nexomc.nexo.mechanics.light.LightMechanic
import com.nexomc.nexo.mechanics.limitedplacing.LimitedPlacing
import com.nexomc.nexo.mechanics.storage.StorageMechanic
import com.nexomc.nexo.mechanics.storage.StorageType
import com.nexomc.nexo.utils.BlockHelpers
import com.nexomc.nexo.utils.BlockHelpers.toCenterBlockLocation
import com.nexomc.nexo.utils.PluginUtils
import com.nexomc.nexo.utils.VersionUtil
import com.nexomc.nexo.utils.actions.ClickAction
import com.nexomc.nexo.utils.actions.ClickAction.Companion.parseList
import com.nexomc.nexo.utils.blocksounds.BlockSounds
import com.nexomc.nexo.utils.filterFast
import com.nexomc.nexo.utils.getStringListOrNull
import com.nexomc.nexo.utils.logs.Logs
import com.nexomc.nexo.utils.mapFast
import com.nexomc.nexo.utils.mapNotNullFast
import com.ticxo.modelengine.api.ModelEngineAPI
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.block.data.Bisected
import org.bukkit.block.data.type.TrapDoor
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.ItemDisplay
import org.bukkit.entity.Player
import org.bukkit.persistence.PersistentDataType
import org.joml.Vector3f

class FurnitureMechanic(mechanicFactory: MechanicFactory?, section: ConfigurationSection) : Mechanic(mechanicFactory, section,
    { item -> item.customTag<Byte, Byte>(FURNITURE_KEY, PersistentDataType.BYTE, 1.toByte()) }
) {
    val limitedPlacing: LimitedPlacing? = section.getConfigurationSection("limited_placing")?.let(::LimitedPlacing)
    val storage: StorageMechanic? = section.getConfigurationSection("storage")?.let(::StorageMechanic)
    val blockSounds: BlockSounds? = section.getConfigurationSection("block_sounds")?.let(::BlockSounds)
    val jukebox: JukeboxBlock? = section.getConfigurationSection("jukebox")?.let(::JukeboxBlock)
    val farmlandRequired: Boolean = section.getBoolean("farmland_required", false)
    val evolution: EvolvingFurniture? = section.getConfigurationSection("evolution")?.let(::EvolvingFurniture)?.apply { (factory as FurnitureFactory).registerEvolution() }
    val light: LightMechanic
    val modelEngineID: String? = section.getString("modelengine_id", null)
    private val placedItemId: String = section.getString("item", itemID)!!
    private val placedItemModel: Key? = section.getString("item_model")?.let(Key::key)?.takeIf { VersionUtil.atleast("1.21.3") }
    val seats = section.getStringList("seats").mapNotNullFast(FurnitureSeat::getSeat)
    val clickActions: List<ClickAction> = parseList(section)
    val properties: FurnitureProperties = section.getConfigurationSection("properties")?.let(::FurnitureProperties) ?: FurnitureProperties()
    val rotatable: Rotatable = section.get("rotatable")?.let(::Rotatable) ?: Rotatable()
    val blockLocker: BlockLockerMechanic? = section.getConfigurationSection("blocklocker")?.let(::BlockLockerMechanic)
    val restrictedRotation: RestrictedRotation = section.getString("restricted_rotation")?.let(RestrictedRotation::fromString) ?: RestrictedRotation.STRICT
    val breakable: BreakableMechanic = BreakableMechanic(section)
    val waterloggable: Boolean = section.getBoolean("waterloggable")
    val beds = section.getStringListOrNull("beds")?.map(::FurnitureBed) ?: listOf()

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
        val barrierHitboxes = hitbox.barriers.mapFast(BarrierHitbox::toVector3f)
        this.light = LightMechanic(section)
        val overlap = light.lightBlocks.filterFast { it.toVector3f() in barrierHitboxes }
        if (overlap.isNotEmpty()) {
            Logs.logError("Furniture $itemID has lights that overlap with the barrierHitboxes at: ${overlap.joinToString()}")
            Logs.logWarn("Nexo will ignore any lights that conflict with a barrier...")
        }

        this.light.lightBlocks.removeAll(overlap)
    }

    val isModelEngine: Boolean = PluginUtils.isModelEngineEnabled && modelEngineID != null

    fun placedItem(baseEntity: ItemDisplay): ItemBuilder {
        var builder: ItemBuilder? = null

        // Mechanic-specific item-overrides
        if (!light.isEmpty && light.toggleable && FurnitureHelpers.lightState(baseEntity)) {
            builder = light.toggledItemModel?.let { ItemBuilder(Material.LEATHER_HORSE_ARMOR).setItemModel(it) }
                ?: light.toggledModel?.let(NexoItems::itemFromId)
        }

        // Default Furniture items
        return builder ?: placedItemModel?.let { ItemBuilder(Material.LEATHER_HORSE_ARMOR).setItemModel(it) }
            ?: NexoItems.itemFromId(placedItemId) ?: NexoItems.itemFromId(itemID) ?: ItemBuilder(Material.BARRIER)
    }

    val hasLimitedPlacing: Boolean = limitedPlacing != null

    val isStorage = storage != null
    val hasBlockSounds = blockSounds != null
    val isJukebox = jukebox != null
    val hasSeats = seats.isNotEmpty()
    val hasBeds = beds.isNotEmpty()
    val hasEvolution = evolution != null
    fun isInteractable(player: Player?) = rotatable.shouldRotate(player) || hasSeats || isStorage || light.toggleable || clickActions.isNotEmpty()

    fun place(location: Location) = place(location, location.yaw, BlockFace.UP, true)

    fun place(location: Location, yaw: Float, facing: BlockFace) = place(location, yaw, facing, true)

    fun place(location: Location, yaw: Float, facing: BlockFace, checkSpace: Boolean): ItemDisplay? {
        if (!location.isWorldLoaded) return null
        if (checkSpace && this.notEnoughSpace(location, yaw)) return null
        Preconditions.checkNotNull(location.world)

        val baseEntity = location.world.spawn(correctedSpawnLocation(location, facing), ItemDisplay::class.java) { e ->
            setBaseFurnitureData(e, yaw, facing)
        }

        if (isModelEngine) spawnModelEngineFurniture(baseEntity)
        FurnitureSeat.spawnSeats(baseEntity, this)

        return baseEntity
    }

    private fun correctedSpawnLocation(baseLocation: Location, facing: BlockFace): Location {
        val (isWall, isRoof) = (limitedPlacing?.isWall == true) to (limitedPlacing?.isRoof == true)
        val (isFixed, isNone) = properties.isFixedTransform to properties.isNoneTransform
        val solidBelow = baseLocation.block.getRelative(BlockFace.DOWN).isSolid
        val hitboxOffset = (hitbox.hitboxHeight() - 1).toFloat().takeUnless { isRoof && facing == BlockFace.DOWN } ?: -0.49f

        val correctedLocation = when {
            isFixed && (facing == BlockFace.UP || (facing.modY == 0 && solidBelow && !isWall)) -> toCenterBlockLocation(baseLocation)
            else -> baseLocation.toCenterLocation()
        }.apply {
            if (isRoof && facing == BlockFace.DOWN) y += -hitboxOffset
        }

        if (isNone && !isWall && !isRoof) return correctedLocation
        // Since roof-furniture need to be more or less flipped, we have to add 0.5 (0.49 or it is "inside" the block above) to the Y coordinate
        if (isFixed && isWall && facing.modY == 0 && !solidBelow) correctedLocation.apply {
            val scale = 0.49 * properties.scale.y().times(2)
            x += -facing.modX * scale
            z += -facing.modZ * scale
        }

        return correctedLocation
    }

    private fun setBaseFurnitureData(baseEntity: ItemDisplay, yaw: Float, blockFace: BlockFace) {
        baseEntity.isPersistent = true
        baseEntity.isInvulnerable = true
        baseEntity.isSilent = true
        baseEntity.isCustomNameVisible = false
        val item = NexoItems.itemFromId(itemID)
        val customName = item?.itemName ?: item?.displayName ?: Component.text(itemID)
        baseEntity.customName(customName)

        val (isWall, isFloor) = (limitedPlacing?.isWall == true) to (limitedPlacing?.isFloor == true)
        val (isRoof, isFixed) = (limitedPlacing?.isRoof == true) to properties.isFixedTransform
        val yaw = when {
            isWall && isFixed && blockFace.modY == 0 -> 90f * blockFace.ordinal - 180
            else -> yaw
        }
        val pitch = when {
            isFixed && isFloor && blockFace == BlockFace.UP -> -90f
            isFixed && isFloor && !isWall && blockFace.modY == 0 -> -90f
            isFixed && isRoof && blockFace == BlockFace.DOWN -> 90f
            else -> 0f
        }
        baseEntity.setRotation(yaw, pitch)

        correctFurnitureTranslation(baseEntity, blockFace)

        val pdc = baseEntity.persistentDataContainer
        pdc.set(FURNITURE_KEY, PersistentDataType.STRING, itemID)
        if (hasEvolution) pdc.set(EVOLUTION_KEY, PersistentDataType.INTEGER, 0)
        if (storage?.storageType == StorageType.STORAGE) {
            pdc.set(StorageMechanic.STORAGE_KEY, DataType.ITEM_STACK_ARRAY, arrayOf())
        }
    }

    private fun correctFurnitureTranslation(baseEntity: ItemDisplay, blockFace: BlockFace) {
        baseEntity.transformation = baseEntity.transformation.apply {
            // If placed on a non-replaceable, non-full block (slabs, carpets...) or against furniture that is
            val against = baseEntity.location.block.getRelative(blockFace.oppositeFace)
            if (against.isReplaceable || (against.blockData as? TrapDoor)?.let { !it.isOpen && it.half == Bisected.Half.TOP } == true) return@apply

            val offset = baseEntity(against)?.transformation?.translation?.y ?: let {
                val bb = against.boundingBox
                when (blockFace) {
                    BlockFace.UP -> bb.height - if (properties.isFixedTransform) 0.99 else 1.01
                    BlockFace.DOWN -> bb.height - if (properties.isFixedTransform) 0.99 else 0.0
                    else -> 0.0
                }.also {
                    if (bb.height !in 0.01..0.99 || blockFace.modY == 0) return@apply
                    if (baseEntity.location.clone().apply { y += it }.toVector() in bb) return@apply
                }
            }.toFloat()

            when {
                properties.isFixedTransform -> translation.set(properties.translation.add(0f, 0f, offset, Vector3f()))
                else -> translation.set(properties.translation.add(0f, offset, 0f, Vector3f()))
            }
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
        if (hasSeats) FurnitureSeat.removeSeats(baseEntity)
        if (hasBeds) FurnitureBed.removeBeds(baseEntity)
        val packetManager = FurnitureFactory.instance()?.packetManager()
        packetManager?.removeInteractionHitboxPacket(baseEntity, this)
        packetManager?.removeShulkerHitboxPacket(baseEntity, this)
        packetManager?.removeBarrierHitboxPacket(baseEntity, this)
        packetManager?.removeLightMechanicPacket(baseEntity, this)

        if (!baseEntity.isDead) baseEntity.remove()
    }

    fun notEnoughSpace(baseEntity: ItemDisplay, yaw: Float = baseEntity.yaw): Boolean {
        return hitbox.hitboxLocations(baseEntity.location, yaw).any { !BlockHelpers.isReplaceable(it.block, baseEntity.uniqueId) }
    }

    fun notEnoughSpace(rootLocation: Location, yaw: Float): Boolean {
        return hitbox.hitboxLocations(rootLocation.clone(), yaw).any { !BlockHelpers.isReplaceable(it.block) }
    }

    fun runClickActions(player: Player) {
        for (action: ClickAction in clickActions) if (action.canRun(player)) action.performActions(player)
    }

    fun rotateFurniture(baseEntity: ItemDisplay) {
        val newYaw = baseEntity.yaw + (if (restrictedRotation == RestrictedRotation.VERY_STRICT) 45f else 22.5f)
        if (notEnoughSpace(baseEntity, newYaw)) return
        baseEntity.setRotation(newYaw, baseEntity.pitch)
        hitbox.refreshHitboxes(baseEntity, this)
        light.refreshLights(baseEntity, this)
    }

    companion object {
        val FURNITURE_KEY = NamespacedKey(NexoPlugin.instance(), "furniture")
        val DISPLAY_NAME_KEY = NamespacedKey(NexoPlugin.instance(), "display_name")
        val FURNITURE_DYE_KEY = NamespacedKey(NexoPlugin.instance(), "furniture_dye")
        val FURNITURE_LIGHT_KEY = NamespacedKey(NexoPlugin.instance(), "furniture_light")
        val MODELENGINE_KEY = NamespacedKey(NexoPlugin.instance(), "modelengine")
        val EVOLUTION_KEY = NamespacedKey(NexoPlugin.instance(), "evolution")

        fun baseEntity(block: Block?): ItemDisplay? {
            return IFurniturePacketManager.baseEntityFromHitbox(BlockLocation(block?.location ?: return null))
        }

        fun baseEntity(location: Location?): ItemDisplay? {
            return IFurniturePacketManager.baseEntityFromHitbox(BlockLocation(location ?: return null))
        }

        fun baseEntity(location: BlockLocation?): ItemDisplay? {
            return IFurniturePacketManager.baseEntityFromHitbox(location ?: return null)
        }

        fun baseEntity(interactionId: Int): ItemDisplay? {
            return IFurniturePacketManager.baseEntityFromHitbox(interactionId)
        }

    }
}
