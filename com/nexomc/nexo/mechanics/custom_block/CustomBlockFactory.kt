package com.nexomc.nexo.mechanics.custom_block

import com.nexomc.nexo.NexoPlugin
import com.nexomc.nexo.mechanics.Mechanic
import com.nexomc.nexo.mechanics.MechanicFactory
import com.nexomc.nexo.mechanics.MechanicsManager
import com.nexomc.nexo.mechanics.custom_block.noteblock.NoteBlockMechanicFactory
import com.nexomc.nexo.mechanics.custom_block.stringblock.StringBlockMechanicFactory
import com.nexomc.nexo.utils.blocksounds.BlockSounds
import com.nexomc.nexo.utils.logs.Logs
import org.bukkit.block.data.BlockData
import org.bukkit.block.data.type.NoteBlock
import org.bukkit.block.data.type.Tripwire
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.inventory.ItemStack
import team.unnamed.creative.ResourcePack
import team.unnamed.creative.blockstate.BlockState
import team.unnamed.creative.sound.SoundRegistry

class CustomBlockFactory(mechanicId: String) : MechanicFactory(mechanicId) {

    val NOTEBLOCK = DefaultBlockType("NOTEBLOCK", NoteBlockMechanicFactory.instance())
    val STRINGBLOCK = DefaultBlockType("STRINGBLOCK", StringBlockMechanicFactory.instance())

    companion object {
        private var instance: CustomBlockFactory? = null

        val isEnabled = instance != null

        fun instance() = instance

        fun getMechanic(blockData: BlockData) = when (blockData) {
            is NoteBlock -> NoteBlockMechanicFactory.getMechanic(blockData)
            is Tripwire -> StringBlockMechanicFactory.getMechanic(blockData)
            else -> null
        }
    }

    init {
        instance = this
        CustomBlockRegistry.register(NOTEBLOCK)
        CustomBlockRegistry.register(STRINGBLOCK)
        MechanicsManager.registerListeners(
            NexoPlugin.instance(),
            mechanicID,
            CustomBlockListener(),
            CustomBlockMiningListener()
        )
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

        blockStates.forEach { blockState ->
            (resourcePack.blockState(blockState.key())?.let {
                BlockState.of(blockState.key(), blockState.variants().plus(it.variants()), blockState.multipart().plus(it.multipart()))
            } ?: blockState).addTo(resourcePack)
        }
    }

    fun soundRegistries(resourcePack: ResourcePack) {
        val soundRegistries = mutableListOf<SoundRegistry>()

        for (type: CustomBlockType in CustomBlockRegistry.types) {
            if (type.factory() == null) continue

            //TODO Implement this by having factories inherit and override CustomBlockFactory
            //blockStates.add(type.factory().generateBlockStateFile());
        }
        if (NoteBlockMechanicFactory.isEnabled) soundRegistries += listOf(
            BlockSounds.NEXO_WOOD_SOUND_REGISTRY,
            BlockSounds.VANILLA_WOOD_SOUND_REGISTRY
        )
        if (StringBlockMechanicFactory.isEnabled) soundRegistries += listOf(
            BlockSounds.NEXO_STONE_SOUND_REGISTRY,
            BlockSounds.VANILLA_STONE_SOUND_REGISTRY
        )

        soundRegistries.forEach { soundRegistry: SoundRegistry ->
            (resourcePack.soundRegistry(soundRegistry.namespace())?.let {
                SoundRegistry.soundRegistry().sounds(soundRegistry.sounds().plus(it.sounds())).namespace(soundRegistry.namespace()).build()
            } ?: soundRegistry).addTo(resourcePack)
        }
    }

    fun toolTypes(type: CustomBlockType): List<String> {
        return when {
            type === STRINGBLOCK -> StringBlockMechanicFactory.instance()?.toolTypes ?: listOf()
            type === NOTEBLOCK -> NoteBlockMechanicFactory.instance()?.toolTypes ?: listOf()
            else -> listOf()
        }
    }

    override fun parse(section: ConfigurationSection): Mechanic? {
        val itemId = section.parent?.parent?.name ?: return null
        val type = CustomBlockRegistry.fromMechanicSection(section) ?: return null

        if (type.factory() == null) {
            Logs.logError(itemId + " attempted to use " + type.name() + "-type but it has been disabled")
            return null
        }

        return (type.factory()?.parse(section) as? CustomBlockMechanic)?.apply(::addToImplemented)
    }

    override fun getMechanic(itemID: String?) = super.getMechanic(itemID) as? CustomBlockMechanic?

    override fun getMechanic(itemStack: ItemStack?) = super.getMechanic(itemStack) as? CustomBlockMechanic?


}
