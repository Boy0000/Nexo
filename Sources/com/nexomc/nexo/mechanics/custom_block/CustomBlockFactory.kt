package com.nexomc.nexo.mechanics.custom_block

import com.jeff_media.morepersistentdatatypes.DataType
import com.nexomc.nexo.api.events.custom_block.chorusblock.NexoChorusBlockBreakEvent
import com.nexomc.nexo.api.events.custom_block.chorusblock.NexoChorusBlockDropLootEvent
import com.nexomc.nexo.api.events.custom_block.noteblock.NexoNoteBlockBreakEvent
import com.nexomc.nexo.api.events.custom_block.noteblock.NexoNoteBlockDropLootEvent
import com.nexomc.nexo.api.events.custom_block.stringblock.NexoStringBlockBreakEvent
import com.nexomc.nexo.api.events.custom_block.stringblock.NexoStringBlockDropLootEvent
import com.nexomc.nexo.mechanics.Mechanic
import com.nexomc.nexo.mechanics.MechanicFactory
import com.nexomc.nexo.mechanics.custom_block.chorusblock.ChorusBlockFactory
import com.nexomc.nexo.mechanics.custom_block.chorusblock.ChorusBlockMechanic
import com.nexomc.nexo.mechanics.custom_block.noteblock.NoteBlockMechanic
import com.nexomc.nexo.mechanics.custom_block.noteblock.NoteBlockMechanicFactory
import com.nexomc.nexo.mechanics.custom_block.noteblock.NoteMechanicHelpers
import com.nexomc.nexo.mechanics.custom_block.stringblock.StringBlockMechanic
import com.nexomc.nexo.mechanics.custom_block.stringblock.StringBlockMechanicFactory
import com.nexomc.nexo.mechanics.custom_block.stringblock.StringMechanicHelpers
import com.nexomc.nexo.mechanics.custom_block.stringblock.sapling.SaplingMechanic
import com.nexomc.nexo.mechanics.storage.StorageMechanic
import com.nexomc.nexo.mechanics.storage.StorageType
import com.nexomc.nexo.utils.BlockHelpers
import com.nexomc.nexo.utils.BlockHelpers.persistentDataContainer
import com.nexomc.nexo.utils.EventUtils.call
import com.nexomc.nexo.utils.ItemUtils.damageItem
import com.nexomc.nexo.utils.SchedulerUtils
import com.nexomc.nexo.utils.blocksounds.BlockSounds
import com.nexomc.nexo.utils.drops.Drop
import com.nexomc.nexo.utils.logs.Logs
import org.bukkit.Effect
import org.bukkit.GameEvent
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.block.data.BlockData
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.Player
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import team.unnamed.creative.ResourcePack
import team.unnamed.creative.blockstate.BlockState
import team.unnamed.creative.sound.SoundRegistry

class CustomBlockFactory(section: ConfigurationSection) : MechanicFactory(section) {
    val toolTypes = section.getStringList("tool_types")
    val customSounds = section.getConfigurationSection("custom_block_sounds")?.let(::CustomBlockSounds) ?: CustomBlockSounds()

    val NOTEBLOCK = object : CustomBlockType<NoteBlockMechanic> {
        override fun name(): String = "NOTEBLOCK"
        override fun factory(): NoteBlockMechanicFactory? = NoteBlockMechanicFactory.instance()
        override fun getMechanic(block: Block): NoteBlockMechanic? = factory()?.getMechanic(block.blockData)
        override fun getMechanic(blockData: BlockData): NoteBlockMechanic? = factory()?.getMechanic(blockData)
        override fun toolTypes(): List<String> = factory()?.toolTypes ?: toolTypes
        override fun placeCustomBlock(location: Location, itemID: String?) {
            val block = location.block
            NoteBlockMechanicFactory.setBlockModel(block, itemID)
            val mechanic = getMechanic(block) ?: return

            if (mechanic.storage()?.storageType == StorageType.STORAGE) {
                block.persistentDataContainer.set(StorageMechanic.STORAGE_KEY, DataType.ITEM_STACK_ARRAY, emptyArray())
            }
            NoteMechanicHelpers.checkNoteBlockAbove(location)
        }
        override fun placeCustomBlock(player: Player, hand: EquipmentSlot, item: ItemStack, mechanic: NoteBlockMechanic, placedAgainst: Block, blockFace: BlockFace) {
            CustomBlockHelpers.makePlayerPlaceBlock(player, hand, item, placedAgainst, blockFace, mechanic, mechanic.blockData)
        }

        override fun removeCustomBlock(block: Block, player: Player?, overrideDrop: Drop?): Boolean {
            val (itemInHand, loc) = (player?.inventory?.itemInMainHand ?: ItemStack(Material.AIR)) to block.location
            val mechanic = getMechanic(block)?.let { it.directional?.parentMechanic ?: it } ?: return false

            var drop = overrideDrop ?: mechanic.breakable.drop
            if (player != null) {
                if (player.gameMode == GameMode.CREATIVE) drop = Drop.emptyDrop()
                val noteBlockBreakEvent = NexoNoteBlockBreakEvent(mechanic, block, player, drop)
                if (!noteBlockBreakEvent.call()) return false

                if (overrideDrop != null || player.gameMode != GameMode.CREATIVE) drop = noteBlockBreakEvent.drop

                block.world.sendGameEvent(player, GameEvent.BLOCK_DESTROY, loc.toVector())
                loc.getNearbyPlayers(64.0).forEach { if (it != player) it.playEffect(loc, Effect.STEP_SOUND, block.blockData) }
            }

            if (!drop.isEmpty) {
                val loots = drop.spawns(loc, itemInHand)
                if (loots.isNotEmpty() && player != null) {
                    NexoNoteBlockDropLootEvent(mechanic, block, player, loots).call()
                    damageItem(player, itemInHand)
                }
            }

            mechanic.storage()?.takeIf { it.storageType == StorageType.STORAGE }?.dropStorageContent(block)
            block.type = Material.AIR
            NoteMechanicHelpers.checkNoteBlockAbove(loc)
            return true
        }

        override fun placeWorldEdit(location: Location, mechanic: NoteBlockMechanic) {}
        override fun removeWorldEdit(location: Location, mechanic: NoteBlockMechanic) {}

        override val clazz: Class<NoteBlockMechanic>
            get() = NoteBlockMechanic::class.java
    }
    val STRINGBLOCK = object : CustomBlockType<StringBlockMechanic> {
        override fun name(): String = "STRINGBLOCK"
        override fun factory(): StringBlockMechanicFactory? = StringBlockMechanicFactory.instance()
        override fun getMechanic(block: Block): StringBlockMechanic? = factory()?.getMechanic(block.blockData)
        override fun getMechanic(blockData: BlockData): StringBlockMechanic? = factory()?.getMechanic(blockData)
        override fun toolTypes(): List<String> = factory()?.toolTypes ?: toolTypes
        override fun placeCustomBlock(location: Location, itemID: String?) {
            val block = location.block
            val blockAbove = block.getRelative(BlockFace.UP)
            StringBlockMechanicFactory.setBlockModel(block, itemID)
            val mechanic = getMechanic(block) ?: return
            if (mechanic.isTall) {
                if (blockAbove.type !in BlockHelpers.REPLACEABLE_BLOCKS) return
                else blockAbove.type = Material.TRIPWIRE
            }

            if (mechanic.isSapling()) mechanic.sapling()?.takeIf { it.canGrowNaturally }?.let {
                block.persistentDataContainer.set(SaplingMechanic.SAPLING_KEY, PersistentDataType.INTEGER, it.naturalGrowthTime)
            }
        }
        override fun placeCustomBlock(player: Player, hand: EquipmentSlot, item: ItemStack, mechanic: StringBlockMechanic, placedAgainst: Block, blockFace: BlockFace) {
            CustomBlockHelpers.makePlayerPlaceBlock(player, hand, item, placedAgainst, blockFace, mechanic, mechanic.blockData)
        }

        override fun removeCustomBlock(block: Block, player: Player?, overrideDrop: Drop?): Boolean {
            val mechanic = getMechanic(block) ?: return false
            val itemInHand = player?.inventory?.itemInMainHand ?: ItemStack(Material.AIR)

            var drop = overrideDrop ?: mechanic.breakable.drop
            if (player != null) {
                if (player.gameMode == GameMode.CREATIVE) drop = Drop.emptyDrop()

                val wireBlockBreakEvent = NexoStringBlockBreakEvent(mechanic, block, player, drop)
                if (!wireBlockBreakEvent.call()) return false

                if (overrideDrop != null || player.gameMode != GameMode.CREATIVE) drop = wireBlockBreakEvent.drop

                block.world.sendGameEvent(player, GameEvent.BLOCK_DESTROY, block.location.toVector())
            }

            if (!drop.isEmpty) {
                val loots = drop.spawns(block.location, itemInHand)
                if (loots.isNotEmpty() && player != null) {
                    NexoStringBlockDropLootEvent(mechanic, block, player, loots).call()
                }
            }

            val blockAbove = block.getRelative(BlockFace.UP)
            if (mechanic.isTall) blockAbove.type = Material.AIR
            block.type = Material.AIR
            SchedulerUtils.foliaScheduler.runAtLocation(block.location) {
                StringMechanicHelpers.fixClientsideUpdate(block.location)
                if (blockAbove.type == Material.TRIPWIRE) removeCustomBlock(blockAbove, player, overrideDrop)
            }
            return true
        }

        override fun placeWorldEdit(location: Location, mechanic: StringBlockMechanic) {}
        override fun removeWorldEdit(location: Location, mechanic: StringBlockMechanic) {}

        override val clazz: Class<StringBlockMechanic>
            get() = StringBlockMechanic::class.java
    }
    val CHORUSBLOCK: CustomBlockType<ChorusBlockMechanic> = object : CustomBlockType<ChorusBlockMechanic> {
        override fun name(): String = "CHORUSBLOCK"
        override fun factory(): ChorusBlockFactory? = ChorusBlockFactory.instance()
        override fun getMechanic(block: Block): ChorusBlockMechanic? = factory()?.getMechanic(block.blockData)
        override fun getMechanic(blockData: BlockData): ChorusBlockMechanic? = factory()?.getMechanic(blockData)
        override fun toolTypes(): List<String> = factory()?.toolTypes ?: toolTypes

        override fun placeCustomBlock(location: Location, itemID: String?) {
            ChorusBlockFactory.setBlockModel(location.block, itemID)
        }

        override fun placeCustomBlock(player: Player, hand: EquipmentSlot, item: ItemStack, mechanic: ChorusBlockMechanic, placedAgainst: Block, blockFace: BlockFace) {
            CustomBlockHelpers.makePlayerPlaceBlock(player, hand, item, placedAgainst, blockFace, mechanic, mechanic.blockData)
        }

        override fun removeCustomBlock(block: Block, player: Player?, overrideDrop: Drop?): Boolean {
            val (itemInHand, loc) = (player?.inventory?.itemInMainHand ?: ItemStack(Material.AIR)) to block.location
            val mechanic = getMechanic(block) ?: return false

            var drop = overrideDrop ?: mechanic.breakable.drop
            if (player != null) {
                if (player.gameMode == GameMode.CREATIVE) drop = Drop.emptyDrop()

                val chorusBlockBreakEvent = NexoChorusBlockBreakEvent(mechanic, block, player, drop)
                if (!chorusBlockBreakEvent.call()) return false

                if (overrideDrop != null || player.gameMode != GameMode.CREATIVE) drop = chorusBlockBreakEvent.drop

                block.world.sendGameEvent(player, GameEvent.BLOCK_DESTROY, loc.toVector())
                loc.getNearbyPlayers(64.0).forEach { if (it != player) it.playEffect(loc, Effect.STEP_SOUND, block.blockData) }
            }

            if (!drop.isEmpty) {
                val loots = drop.spawns(loc, itemInHand)
                if (loots.isNotEmpty() && player != null) {
                    NexoChorusBlockDropLootEvent(mechanic, block, player, loots).call()
                    damageItem(player, itemInHand)
                }
            }

            block.type = Material.AIR
            return true
        }

        override fun placeWorldEdit(location: Location, mechanic: ChorusBlockMechanic) {}
        override fun removeWorldEdit(location: Location, mechanic: ChorusBlockMechanic) {}

        override val clazz: Class<ChorusBlockMechanic>
            get() = ChorusBlockMechanic::class.java
    }

    data class CustomBlockSounds(val enabled: Boolean = true) {
        constructor(section: ConfigurationSection) : this(section.getBoolean("enabled", true))
    }

    companion object {
        private var instance: CustomBlockFactory? = null

        fun instance() = instance
    }

    init {
        instance = this
        CustomBlockRegistry.register(NOTEBLOCK)
        CustomBlockRegistry.register(STRINGBLOCK)
        CustomBlockRegistry.register(CHORUSBLOCK)
        registerListeners(CustomBlockListener(), CustomBlockMiningListener())

        if (customSounds.enabled) registerListeners(CustomBlockSoundListener(customSounds))
    }

    fun blockStates(resourcePack: ResourcePack) {
        val blockStates = mutableListOf<BlockState>()

        for (type in CustomBlockRegistry.types) {
            if (type.factory() == null) continue

            //TODO Implement this by having factories inherit and override CustomBlockFactory
            //blockStates.add(type.factory().generateBlockStateFile());
        }

        NoteBlockMechanicFactory.instance()?.generateBlockState()?.let(blockStates::add)
        StringBlockMechanicFactory.instance()?.generateBlockState()?.let(blockStates::add)
        ChorusBlockFactory.instance()?.generateBlockState()?.let(blockStates::add)

        blockStates.forEach { blockState ->
            (resourcePack.blockState(blockState.key())?.let {
                BlockState.of(blockState.key(), blockState.variants().plus(it.variants()), blockState.multipart().plus(it.multipart()))
            } ?: blockState).addTo(resourcePack)
        }
    }

    fun soundRegistries(resourcePack: ResourcePack) {
        for (type in CustomBlockRegistry.types) {
            if (type.factory() == null) continue

            //TODO Implement this by having factories inherit and override CustomBlockFactory
            //blockStates.add(type.factory().generateBlockStateFile());
        }

        if (customSounds.enabled) arrayOf(BlockSounds.NEXO_WOOD_SOUND_REGISTRY, BlockSounds.VANILLA_WOOD_SOUND_REGISTRY).forEach { soundRegistry: SoundRegistry ->
            (resourcePack.soundRegistry(soundRegistry.namespace())?.let {
                SoundRegistry.soundRegistry().sounds(soundRegistry.sounds().plus(it.sounds())).namespace(soundRegistry.namespace()).build()
            } ?: soundRegistry).addTo(resourcePack)
        }
    }

    fun toolTypes(type: CustomBlockType<*>): List<String> {
        return when {
            type === STRINGBLOCK -> StringBlockMechanicFactory.instance()?.toolTypes ?: toolTypes
            type === NOTEBLOCK -> NoteBlockMechanicFactory.instance()?.toolTypes ?: toolTypes
            type === CHORUSBLOCK -> ChorusBlockFactory.instance()?.toolTypes ?: toolTypes
            else -> listOf()
        }
    }

    override fun parse(section: ConfigurationSection): Mechanic? {
        val itemId = section.parent?.parent?.name ?: return null
        val type = CustomBlockRegistry.fromMechanicSection(section) ?: return null

        if (type.factory() == null) {
            Logs.logError(itemId + " attempted to use ${type.name()}-type but it has been disabled")
            return null
        }

        return (type.factory()?.parse(section) as? CustomBlockMechanic)?.apply(::addToImplemented)
    }

    override fun getMechanic(itemID: String?) = super.getMechanic(itemID) as? CustomBlockMechanic

    override fun getMechanic(itemStack: ItemStack?) = super.getMechanic(itemStack) as? CustomBlockMechanic


}
