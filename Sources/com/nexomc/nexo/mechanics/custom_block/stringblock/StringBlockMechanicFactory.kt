package com.nexomc.nexo.mechanics.custom_block.stringblock

import com.nexomc.nexo.NexoPlugin
import com.nexomc.nexo.compatibilities.worldedit.WrappedWorldEdit
import com.nexomc.nexo.mechanics.Mechanic
import com.nexomc.nexo.mechanics.MechanicFactory
import com.nexomc.nexo.mechanics.custom_block.CustomBlockFactory
import com.nexomc.nexo.mechanics.custom_block.stringblock.sapling.SaplingJob
import com.nexomc.nexo.mechanics.custom_block.stringblock.sapling.SaplingListener
import com.nexomc.nexo.nms.NMSHandlers
import com.nexomc.nexo.utils.getStringListOrNull
import com.nexomc.nexo.utils.logs.Logs
import com.nexomc.nexo.utils.ticks
import com.nexomc.nexo.utils.to
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import kotlinx.coroutines.Job
import net.kyori.adventure.key.Key
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.block.data.BlockData
import org.bukkit.block.data.type.Tripwire
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.inventory.ItemStack
import team.unnamed.creative.blockstate.BlockState
import team.unnamed.creative.blockstate.MultiVariant
import team.unnamed.creative.blockstate.Variant
import kotlin.time.Duration

class StringBlockMechanicFactory(section: ConfigurationSection) : MechanicFactory(section) {
    val toolTypes: List<String> = section.getStringListOrNull("tool_types") ?: CustomBlockFactory.instance()?.toolTypes ?: listOf()
    private var sapling = false
    private val saplingGrowthCheckDelay: Duration = section.getInt("sapling_growth_check_delay").ticks
    val disableVanillaString: Boolean = section.getBoolean("disable_vanilla_strings", true)

    val BLOCK_PER_VARIATION = Int2ObjectOpenHashMap<StringBlockMechanic>()
    private val variants = Object2ObjectOpenHashMap<String, MultiVariant>()

    init {
        instance = this

        registerListeners(StringBlockMechanicListener(), SaplingListener())
        if (saplingGrowthCheckDelay.isPositive()) registerSaplingMechanic()

        registerListeners(StringBlockMechanicPaperListener())

        if (!NMSHandlers.handler().tripwireUpdatesDisabled()) {
            registerListeners(StringBlockMechanicPhysicsListener())
            Logs.logError("Papers block-updates.disable-tripwire-updates is not enabled.")
            Logs.logError("It is HIGHLY recommended to enable this setting for improved performance and prevent bugs with tripwires")
            Logs.logError("Otherwise Nexo needs to listen to very taxing events, which also introduces some bugs")
            Logs.logError("You can enable this setting in ServerFolder/config/paper-global.yml", true)
        }
    }

    fun generateBlockState(): BlockState {
        val stringKey = Key.key("minecraft:tripwire")
        variants["east=false,west=false,south=false,north=false,attached=false,disarmed=false,powered=false"] =
            MultiVariant.of(Variant.builder().model(Key.key("block/barrier")).build())
        val stringState = NexoPlugin.instance().packGenerator().resourcePack().blockState(stringKey)
        if (stringState != null) variants += stringState.variants()

        return BlockState.of(stringKey, variants)
    }


    override fun getMechanic(itemID: String?) = super.getMechanic(itemID) as? StringBlockMechanic

    override fun getMechanic(itemStack: ItemStack?) = super.getMechanic(itemStack) as? StringBlockMechanic

    override fun parse(section: ConfigurationSection): Mechanic? {
        val mechanic = StringBlockMechanic(this, section)

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

    private fun blockstateVariant(mechanic: StringBlockMechanic): String {
        val (east, west, south, north) = setOf(BlockFace.EAST, BlockFace.WEST, BlockFace.SOUTH, BlockFace.NORTH).mapNotNull { mechanic.blockData?.hasFace(it) }
        val (attached, disarmed, powered) = mechanic.blockData?.let { it.isAttached to it.isDisarmed to it.isPowered } ?: return ""
        return "east=${east},west=${west},south=${south},north=${north},attached=${attached},disarmed=${disarmed},powered=${powered}"
    }

    fun registerSaplingMechanic() {
        if (sapling || !WrappedWorldEdit.loaded) return
        saplingTask?.cancel()
        saplingTask = SaplingJob.launchJob(saplingGrowthCheckDelay)
        sapling = true
    }

    fun getMechanic(blockData: BlockData): StringBlockMechanic? {
        if (blockData !is Tripwire) return null
        return BLOCK_PER_VARIATION.values.firstOrNull { it.blockData == blockData }
    }

    companion object {
        val MAX_BLOCK_VARIATION = 1..127
        private var instance: StringBlockMechanicFactory? = null
        private var saplingTask: Job? = null
        val isEnabled: Boolean
            get() = instance != null

        fun instance() = instance

        /**
         * Attempts to set the block directly to the model and texture of a Nexo item.
         *
         * @param block  The block to update.
         * @param itemId The Nexo item ID.
         */
        fun setBlockModel(block: Block, itemId: String?) {
            block.blockData = instance?.getMechanic(itemId)?.blockData ?: return
        }
    }
}
