package com.nexomc.nexo.mechanics.custom_block.noteblock

import com.nexomc.nexo.NexoPlugin
import com.nexomc.nexo.api.NexoBlocks
import com.nexomc.nexo.mechanics.MechanicFactory
import com.nexomc.nexo.mechanics.custom_block.CustomBlockFactory
import com.nexomc.nexo.mechanics.custom_block.noteblock.beacon.BeaconListener
import com.nexomc.nexo.mechanics.custom_block.noteblock.logstrip.LogStripListener
import com.nexomc.nexo.nms.NMSHandlers
import com.nexomc.nexo.utils.getStringListOrNull
import com.nexomc.nexo.utils.logs.Logs
import com.nexomc.nexo.utils.to
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import net.kyori.adventure.key.Key
import org.bukkit.Instrument
import org.bukkit.block.Block
import org.bukkit.block.data.type.NoteBlock
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.inventory.ItemStack
import team.unnamed.creative.blockstate.BlockState
import team.unnamed.creative.blockstate.MultiVariant
import team.unnamed.creative.blockstate.Variant

class NoteBlockMechanicFactory(section: ConfigurationSection) : MechanicFactory(section) {
    val toolTypes: List<String> = section.getStringListOrNull("tool_types") ?: CustomBlockFactory.instance()?.toolTypes ?: listOf()
    val reimplementNoteblockFeatures: Boolean = section.getBoolean("reimplement_noteblock_features", false)

    val BLOCK_PER_VARIATION = Int2ObjectOpenHashMap<NoteBlockMechanic>()
    private val variants = Object2ObjectOpenHashMap<String, MultiVariant>()

    init {
        instance = this

        NoteBlockDatapack().createDatapack()

        registerListeners(NoteBlockMechanicListener(), LogStripListener(), BeaconListener())

        // Physics-related stuff
        if (!NMSHandlers.handler().noteblockUpdatesDisabled())
            registerListeners(NoteBlockMechanicPhysicsListener())

        when {
            !NMSHandlers.handler().noteblockUpdatesDisabled() -> {
                Logs.logError("Papers block-updates.disable-noteblock-updates is not enabled.")
                if (reimplementNoteblockFeatures) Logs.logError("reimplement_noteblock_feature mechanic will not be enabled")
                Logs.logError("It is HIGHLY recommended to enable this setting for improved performance and prevent bugs with noteblocks")
                Logs.logError("Otherwise Nexo needs to listen to very taxing events, which also introduces some bugs")
                Logs.logError("You can enable this setting in ServerFolder/config/paper-global.yml", true)
            }
            reimplementNoteblockFeatures -> registerListeners(RegularNoteBlockListener())
        }
    }

    fun generateBlockState(): BlockState {
        val noteKey = Key.key("minecraft:note_block")
        variants["instrument=harp,powered=false,note=0"] =
            MultiVariant.of(Variant.builder().model(Key.key("block/note_block")).build())
        val noteState = NexoPlugin.instance().packGenerator().resourcePack().blockState(noteKey)
        if (noteState != null) variants += noteState.variants()
        return BlockState.of(noteKey, variants)
    }


    override fun getMechanic(itemID: String?) = super.getMechanic(itemID) as? NoteBlockMechanic?

    override fun getMechanic(itemStack: ItemStack?) = super.getMechanic(itemStack) as? NoteBlockMechanic?

    override fun parse(section: ConfigurationSection): NoteBlockMechanic? {
        val mechanic = NoteBlockMechanic(this, section)

        if (mechanic.customVariation !in 1..MAX_BLOCK_VARIATION) {
            Logs.logError("The custom variation of the block ${mechanic.itemID} is not between 1 and $MAX_BLOCK_VARIATION!")
            Logs.logWarn("The item has failed to build for now to prevent bugs and issues.")
            return null
        }

        BLOCK_PER_VARIATION[mechanic.customVariation]?.takeIf { !allowedSameVariation(mechanic, it) }?.let {
            Logs.logError( "${mechanic.itemID} is set to use custom_variation ${mechanic.customVariation} but it is already used by ${it.itemID}")
            Logs.logWarn("The item has failed to build for now to prevent bugs and issues.")
            return null
        }

        val (instrument, note, powered) = mechanic.blockData?.let { instrumentName(it.instrument) to it.note.id to it.isPowered } ?: return null
        variants["instrument=$instrument,note=$note,powered=$powered"] = when {
            mechanic.directional?.isParentBlock() == false -> {
                val parentMechanic = mechanic.directional.parentMechanic ?: mechanic
                directionalModelJson((parentMechanic.model), mechanic, parentMechanic)
            }
            else -> MultiVariant.of(Variant.builder().model(mechanic.model).build())
        }

        BLOCK_PER_VARIATION[mechanic.customVariation] = mechanic
        addToImplemented(mechanic)
        return mechanic
    }

    private fun instrumentName(instrument: Instrument): String {
        return when (instrument) {
            Instrument.BASS_DRUM -> "basedrum"
            Instrument.PIANO -> "harp"
            Instrument.SNARE_DRUM -> "snare"
            Instrument.STICKS -> "hat"
            Instrument.BASS_GUITAR -> "bass"
            else -> instrument.name.lowercase()
        }
    }

    private fun allowedSameVariation(mechanic: NoteBlockMechanic, oldMechanic: NoteBlockMechanic?): Boolean {
        if (oldMechanic == null || mechanic.itemID == oldMechanic.itemID) return true
        if (!mechanic.isDirectional || !oldMechanic.isDirectional) return false

        val (oldParent, currentParent) = oldMechanic.directional!!.parentMechanic to mechanic.directional!!.parentMechanic
        return when {
            mechanic.itemID == oldParent?.itemID -> true
            oldMechanic.itemID == currentParent?.itemID -> true
            else -> mechanic.directional.anyMatch(oldMechanic.itemID) || oldMechanic.directional.anyMatch(mechanic.itemID)
        }
    }

    private fun directionalModelJson(
        modelKey: Key?,
        mechanic: NoteBlockMechanic,
        parentMechanic: NoteBlockMechanic
    ): MultiVariant {
        val itemId = mechanic.itemID
        val parent = parentMechanic.directional!!
        val variantBuilder = Variant.builder()
        val subBlockModel = mechanic.directional?.directionalModel(mechanic) ?: modelKey
        variantBuilder.model(subBlockModel)
        // If subModel is specified and is different from parent we don't want to rotate it
        if (subBlockModel != modelKey) return MultiVariant.of(variantBuilder.build())

        when(itemId) {
            parent.yBlock -> return MultiVariant.of(variantBuilder.build())
            parent.xBlock -> variantBuilder.x(90)
            parent.zBlock -> {
                variantBuilder.x(90)
                variantBuilder.y(90)
            }
            parent.northBlock -> return MultiVariant.of(variantBuilder.build())
            parent.eastBlock -> variantBuilder.y(90)
            parent.southBlock -> variantBuilder.y(180)
            parent.westBlock -> variantBuilder.y(270)
            parent.upBlock -> variantBuilder.x(270)
            parent.downBlock -> variantBuilder.x(90)
        }

        return MultiVariant.of(variantBuilder.build())
    }

    companion object {
        private const val MAX_PER_INSTRUMENT = 50
        val MAX_BLOCK_VARIATION = Instrument.entries.size * MAX_PER_INSTRUMENT - 1
        private var instance: NoteBlockMechanicFactory? = null
        val isEnabled: Boolean
            get() = instance != null

        fun instance(): NoteBlockMechanicFactory? {
            return instance
        }

        /**
         * Attempts to set the block directly to the model and texture of a Nexo item.
         *
         * @param block  The block to update.
         * @param itemId The Nexo item ID.
         */
        fun setBlockModel(block: Block, itemId: String?) {
            block.blockData = NexoBlocks.noteBlockMechanic(itemId)?.blockData ?: return
        }

        fun getMechanic(blockData: NoteBlock): NoteBlockMechanic? {
            return instance?.BLOCK_PER_VARIATION?.values?.firstOrNull { it.blockData == blockData }
        }
    }
}
