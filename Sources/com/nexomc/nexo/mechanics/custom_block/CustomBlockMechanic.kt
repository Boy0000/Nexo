package com.nexomc.nexo.mechanics.custom_block

import com.nexomc.nexo.compatibilities.blocklocker.BlockLockerMechanic
import com.nexomc.nexo.mechanics.Mechanic
import com.nexomc.nexo.mechanics.MechanicFactory
import com.nexomc.nexo.mechanics.breakable.BreakableMechanic
import com.nexomc.nexo.mechanics.light.LightMechanic
import com.nexomc.nexo.mechanics.limitedplacing.LimitedPlacing
import com.nexomc.nexo.utils.actions.ClickAction
import com.nexomc.nexo.utils.actions.ClickAction.Companion.parseList
import com.nexomc.nexo.utils.blocksounds.BlockSounds
import net.kyori.adventure.key.Key
import org.bukkit.Material
import org.bukkit.block.data.BlockData
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.Player

abstract class CustomBlockMechanic(factory: MechanicFactory, section: ConfigurationSection) : Mechanic(factory, section) {
    val type: CustomBlockType? = CustomBlockRegistry.fromMechanicSection(section)
    val customVariation: Int = section.getInt("custom_variation")
    val model: Key = Key.key(section.getString("model", section.parent!!.getString("Pack.model", itemID))!!)
    open val blockData: BlockData? = createBlockData()

    open val blockSounds: BlockSounds? = section.getConfigurationSection("block_sounds")?.let(::BlockSounds)
    val light: LightMechanic = LightMechanic(section)
    val limitedPlacing: LimitedPlacing? = section.getConfigurationSection("limited_placing")?.let(::LimitedPlacing)
    val clickActions: List<ClickAction> = parseList(section)
    val blockLocker: BlockLockerMechanic? = section.getConfigurationSection("blocklocker")?.let(::BlockLockerMechanic)
    open val breakable: BreakableMechanic = BreakableMechanic(section)
    val isBlastResistant: Boolean = section.getBoolean("blast_resistant")
    val immovable: Boolean = section.getBoolean("immovable")

    open fun createBlockData(): BlockData? = Material.AIR.createBlockData()

    open fun hasLight(): Boolean {
        return light.lightBlocks.isEmpty()
    }

    fun hasLimitedPlacing(): Boolean {
        return limitedPlacing != null
    }

    fun hasBlockSounds(): Boolean {
        return blockSounds != null
    }

    fun hasClickActions(): Boolean {
        return clickActions.isNotEmpty()
    }

    fun runClickActions(player: Player) {
        clickActions.asSequence().filter { it.canRun(player) }.forEach { it.performActions(player) }
    }
}
