package com.nexomc.nexo.mechanics.custom_block

import com.nexomc.nexo.mechanics.Mechanic
import com.nexomc.nexo.mechanics.MechanicFactory
import com.nexomc.nexo.mechanics.custom_block.chorusblock.ChorusBlockFactory
import com.nexomc.nexo.mechanics.custom_block.noteblock.NoteBlockMechanicFactory
import com.nexomc.nexo.mechanics.custom_block.stringblock.StringBlockMechanicFactory
import com.nexomc.nexo.utils.blocksounds.BlockSounds
import com.nexomc.nexo.utils.logs.Logs
import org.bukkit.block.data.BlockData
import org.bukkit.block.data.MultipleFacing
import org.bukkit.block.data.type.NoteBlock
import org.bukkit.block.data.type.Tripwire
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.inventory.ItemStack
import team.unnamed.creative.ResourcePack
import team.unnamed.creative.blockstate.BlockState
import team.unnamed.creative.sound.SoundRegistry

class CustomBlockFactory(section: ConfigurationSection) : MechanicFactory(section) {
    val toolTypes = section.getStringList("tool_types")
    val customSounds = section.getConfigurationSection("custom_block_sounds")?.let(::CustomBlockSounds) ?: CustomBlockSounds()

    val NOTEBLOCK = DefaultBlockType("NOTEBLOCK", NoteBlockMechanicFactory.instance())
    val STRINGBLOCK = DefaultBlockType("STRINGBLOCK", StringBlockMechanicFactory.instance())
    val CHORUSBLOCK = DefaultBlockType("CHORUSBLOCK", ChorusBlockFactory.instance())

    data class CustomBlockSounds(val enabled: Boolean = true, val playersOnly: Boolean = false) {
        constructor(section: ConfigurationSection) : this(section.getBoolean("enabled", true), section.getBoolean("players_only", false))
    }

    companion object {
        private var instance: CustomBlockFactory? = null

        fun instance() = instance

        fun getMechanic(blockData: BlockData) = when (blockData) {
            is NoteBlock -> NoteBlockMechanicFactory.getMechanic(blockData)
            is Tripwire -> StringBlockMechanicFactory.getMechanic(blockData)
            is MultipleFacing -> ChorusBlockFactory.getMechanic(blockData)
            else -> null
        }
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

        for (type: CustomBlockType in CustomBlockRegistry.types) {
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
        for (type: CustomBlockType in CustomBlockRegistry.types) {
            if (type.factory() == null) continue

            //TODO Implement this by having factories inherit and override CustomBlockFactory
            //blockStates.add(type.factory().generateBlockStateFile());
        }

        if (customSounds.enabled) {
            arrayOf(
                BlockSounds.NEXO_WOOD_SOUND_REGISTRY,
                BlockSounds.VANILLA_WOOD_SOUND_REGISTRY,
                BlockSounds.NEXO_STONE_SOUND_REGISTRY,
                BlockSounds.VANILLA_STONE_SOUND_REGISTRY
            ).forEach { soundRegistry: SoundRegistry ->
                (resourcePack.soundRegistry(soundRegistry.namespace())?.let {
                    SoundRegistry.soundRegistry().sounds(soundRegistry.sounds().plus(it.sounds())).namespace(soundRegistry.namespace()).build()
                } ?: soundRegistry).addTo(resourcePack)
            }
        }
    }

    fun toolTypes(type: CustomBlockType): List<String> {
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

    override fun getMechanic(itemID: String?) = super.getMechanic(itemID) as? CustomBlockMechanic?

    override fun getMechanic(itemStack: ItemStack?) = super.getMechanic(itemStack) as? CustomBlockMechanic?


}
