package com.nexomc.nexo.mechanics.custom_block.chorusblock

import com.nexomc.nexo.NexoPlugin
import com.nexomc.nexo.api.NexoBlocks
import com.nexomc.nexo.mechanics.Mechanic
import com.nexomc.nexo.mechanics.MechanicFactory
import com.nexomc.nexo.mechanics.custom_block.CustomBlockFactory
import com.nexomc.nexo.nms.NMSHandlers
import com.nexomc.nexo.utils.getStringListOrNull
import com.nexomc.nexo.utils.logs.Logs
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import net.kyori.adventure.key.Key
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.block.data.MultipleFacing
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.inventory.ItemStack
import team.unnamed.creative.blockstate.BlockState
import team.unnamed.creative.blockstate.MultiVariant
import team.unnamed.creative.blockstate.Variant

class ChorusBlockFactory(section: ConfigurationSection) : MechanicFactory(section) {
    val toolTypes: List<String> = section.getStringListOrNull("tool_types") ?: CustomBlockFactory.instance()?.toolTypes ?: listOf()
    val BLOCK_PER_VARIATION = Int2ObjectOpenHashMap<ChorusBlockMechanic>()
    private val variants = Object2ObjectOpenHashMap<String, MultiVariant>()

    init {
        instance = this

        registerListeners(ChorusBlockListener())

        registerListeners(ChorusBlockMechanicPaperListener())

        if (!NMSHandlers.handler().chorusplantUpdateDisabled()) {
            Logs.logError("Papers block-updates.disable-chorus-plant-updates is not enabled.")
            Logs.logError("It is HIGHLY recommended to enable this setting for improved performance and prevent bugs with chorus-plants")
            Logs.logError("Otherwise Nexo needs to listen to very taxing events, which also introduces some bugs")
            Logs.logError("You can enable this setting in ServerFolder/config/paper-global.yml", true)
        }
    }

    fun generateBlockState(): BlockState {
        val chorusKey = Key.key("minecraft:chorus_plant")
        variants["east=false,west=false,south=false,north=false,up=false,down=false"] =
            MultiVariant.of(Variant.builder().model(Key.key("minecraft:block/chorus_plant")).build())
        val chorusState = NexoPlugin.instance().packGenerator().resourcePack().blockState(chorusKey)
        if (chorusState != null) variants += chorusState.variants()

        return BlockState.of(chorusKey, variants)
    }

    override fun getMechanic(itemID: String?) = super.getMechanic(itemID) as? ChorusBlockMechanic?

    override fun getMechanic(itemStack: ItemStack?) = super.getMechanic(itemStack) as? ChorusBlockMechanic?

    override fun parse(section: ConfigurationSection): Mechanic? {
        val mechanic = ChorusBlockMechanic(this, section)

        if (mechanic.customVariation !in MAX_BLOCK_VARIATION) {
            Logs.logError("The custom variation of the block ${mechanic.itemID} is not between 1 and ${MAX_BLOCK_VARIATION.last}!")
            Logs.logWarn("The item has failed to build for now to prevent bugs and issues.")
            return null
        }

        BLOCK_PER_VARIATION[mechanic.customVariation]?.takeIf { it.itemID != mechanic.itemID }?.let {
            Logs.logError("${mechanic.itemID} is set to use custom_variation ${mechanic.customVariation} but it is already used by ${it.itemID}")
            Logs.logWarn("The item has failed to build for now to prevent bugs and issues.")
            return null
        }

        variants[blockstateVariant(mechanic)] = MultiVariant.of(Variant.builder().model(mechanic.model).build())
        BLOCK_PER_VARIATION[mechanic.customVariation] = mechanic
        addToImplemented(mechanic)
        return mechanic
    }

    private fun blockstateVariant(mechanic: ChorusBlockMechanic): String {
        val t = mechanic.blockData!!
        return "east=${t.hasFace(BlockFace.EAST)},west=${t.hasFace(BlockFace.WEST)},south=${t.hasFace(BlockFace.SOUTH)},north=${t.hasFace(BlockFace.NORTH)},up=${t.hasFace(BlockFace.UP)},down=${t.hasFace(BlockFace.DOWN)}"
    }

    companion object {
        val MAX_BLOCK_VARIATION = 1..63
        private var instance: ChorusBlockFactory? = null
        val isEnabled: Boolean
            get() = instance != null

        fun instance(): ChorusBlockFactory? {
            return instance
        }

        /**
         * Attempts to set the block directly to the model and texture of a Nexo item.
         *
         * @param block  The block to update.
         * @param itemId The Nexo item ID.
         */
        fun setBlockModel(block: Block, itemId: String?) {
            block.blockData = NexoBlocks.chorusBlockMechanic(itemId)?.blockData ?: return
        }

        fun getMechanic(blockData: MultipleFacing): ChorusBlockMechanic? {
            return instance?.takeIf { blockData.material == Material.CHORUS_PLANT }?.BLOCK_PER_VARIATION?.values?.firstOrNull { it.blockData == blockData }
        }
    }
}