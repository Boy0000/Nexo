package com.nexomc.nexo.mechanics.custom_block.stringblock

import com.nexomc.nexo.NexoPlugin
import com.nexomc.nexo.mechanics.Mechanic
import com.nexomc.nexo.mechanics.MechanicFactory
import com.nexomc.nexo.mechanics.MechanicsManager
import com.nexomc.nexo.mechanics.custom_block.CustomBlockFactory
import com.nexomc.nexo.mechanics.custom_block.stringblock.sapling.SaplingListener
import com.nexomc.nexo.mechanics.custom_block.stringblock.sapling.SaplingTask
import com.nexomc.nexo.nms.NMSHandlers
import com.nexomc.nexo.utils.VersionUtil
import com.nexomc.nexo.utils.logs.Logs
import net.kyori.adventure.key.Key
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.block.data.type.Tripwire
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.inventory.ItemStack
import team.unnamed.creative.blockstate.BlockState
import team.unnamed.creative.blockstate.MultiVariant
import team.unnamed.creative.blockstate.Variant
import kotlin.collections.set

class StringBlockMechanicFactory(section: ConfigurationSection) : MechanicFactory(section) {
    val toolTypes: List<String> = section.getStringList("tool_types")
    private var sapling = false
    private val saplingGrowthCheckDelay: Int = section.getInt("sapling_growth_check_delay")
    private val customSounds: Boolean = section.getBoolean("custom_block_sounds", true)
    val disableVanillaString: Boolean = section.getBoolean("disable_vanilla_strings", true)
    private var notifyOfDeprecation = true

    private val variants = mutableMapOf<String, MultiVariant>()

    init {
        instance = this

        registerListeners(StringBlockMechanicListener(), SaplingListener())
        if (customSounds) registerListeners(StringBlockSoundListener())

        if (VersionUtil.isPaperServer) registerListeners(StringBlockMechanicPaperListener())
        if (!VersionUtil.isPaperServer || !NMSHandlers.handler().tripwireUpdatesDisabled())
            registerListeners(StringBlockMechanicPhysicsListener())

        if (VersionUtil.isPaperServer && !NMSHandlers.handler().tripwireUpdatesDisabled()) {
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


    override fun getMechanic(itemID: String?) = super.getMechanic(itemID) as? StringBlockMechanic?

    override fun getMechanic(itemStack: ItemStack?) = super.getMechanic(itemStack) as? StringBlockMechanic?

    override fun parse(section: ConfigurationSection): Mechanic? {
        val mechanic = StringBlockMechanic(this, section)

        if (section.name == mechanicID && notifyOfDeprecation) {
            notifyOfDeprecation = false
            Logs.logError("${mechanic.itemID} is using Mechanics.stringblock which is deprecated...")
            Logs.logWarn("It is recommended to use the new format, Mechanics.custom_block.type: ${CustomBlockFactory.instance()?.STRINGBLOCK}")
        }

        if (mechanic.customVariation !in 1..MAX_BLOCK_VARIATION) {
            Logs.logError("The custom variation of the block ${mechanic.itemID} is not between 1 and $MAX_BLOCK_VARIATION!")
            Logs.logWarn("The item has failed to build for now to prevent bugs and issues.")
            return null
        }

        BLOCK_PER_VARIATION[mechanic.customVariation]?.takeIf { it.itemID != mechanic.itemID }?.let {
            Logs.logError("${mechanic.itemID} is set to use custom_variation ${mechanic.customVariation} but it is already used by ${it.itemID}")
            Logs.logWarn("The item has failed to build for now to prevent bugs and issues.")
            return null
        }

        variants[getBlockstateVariantName(mechanic)] = MultiVariant.of(Variant.builder().model(mechanic.model).build())
        BLOCK_PER_VARIATION[mechanic.customVariation] = mechanic
        addToImplemented(mechanic)
        return mechanic
    }

    private fun getBlockstateVariantName(mechanic: StringBlockMechanic): String {
        val t = mechanic.blockData
        return "east=${t!!.hasFace(BlockFace.EAST)},west=${t.hasFace(BlockFace.WEST)},south=${t.hasFace(BlockFace.SOUTH)},north=${t.hasFace(BlockFace.NORTH)},attached=${t.isAttached},disarmed=${t.isDisarmed},powered=${t.isPowered}"
    }

    fun registerSaplingMechanic() {
        if (sapling) return
        saplingTask?.cancel()
        saplingTask = SaplingTask(saplingGrowthCheckDelay)
        saplingTask!!.runTaskTimer(NexoPlugin.instance(), 0, saplingGrowthCheckDelay.toLong())
        sapling = true
    }

    companion object {
        val BLOCK_PER_VARIATION = mutableMapOf<Int, StringBlockMechanic>()
        const val MAX_BLOCK_VARIATION = 127
        private var instance: StringBlockMechanicFactory? = null
        private var saplingTask: SaplingTask? = null
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
            val stringBlockMechanic = MechanicsManager.getMechanicFactory("stringblock")?.getMechanic(itemId) as? StringBlockMechanic ?: return
            block.blockData = stringBlockMechanic.blockData!!
        }

        fun getMechanic(blockData: Tripwire) = BLOCK_PER_VARIATION.values.firstOrNull { it.blockData == blockData }
    }
}
