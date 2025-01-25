package com.nexomc.nexo.mechanics.custom_block.noteblock

import com.nexomc.nexo.mechanics.MechanicFactory
import com.nexomc.nexo.mechanics.breakable.BreakableMechanic
import com.nexomc.nexo.mechanics.custom_block.CustomBlockMechanic
import com.nexomc.nexo.mechanics.custom_block.noteblock.directional.DirectionalBlock
import com.nexomc.nexo.mechanics.custom_block.noteblock.logstrip.LogStripping
import com.nexomc.nexo.mechanics.storage.StorageMechanic
import com.nexomc.nexo.utils.blocksounds.BlockSounds
import org.bukkit.NamespacedKey
import org.bukkit.block.data.type.NoteBlock
import org.bukkit.configuration.ConfigurationSection

class NoteBlockMechanic(factory: MechanicFactory, section: ConfigurationSection) : CustomBlockMechanic(factory, section) {
    // Creates an instance of CustomBlockMechanic and applies the below
    private val canIgnite: Boolean = section.getBoolean("can_ignite", false)
    private val isFalling: Boolean = section.getBoolean("is_falling", false)
    private val beaconBaseBlock: Boolean = section.getBoolean("beacon_base_block", false)
    val instrument: String = section.getString("instrument", "block.note_block.bass")!!
    private val logStripping: LogStripping? = section.getConfigurationSection("log_strip")?.let(::LogStripping)
    val directional: DirectionalBlock? = section.getConfigurationSection("directional")?.let(::DirectionalBlock)
    private val storage: StorageMechanic? = section.getConfigurationSection("storage")?.let(::StorageMechanic)

    override fun createBlockData() = NoteMechanicHelpers.modernBlockData(customVariation)

    override val blockData = super.blockData as? NoteBlock

    fun isStorage() = storage != null

    fun storage() = directional?.parentMechanic?.storage ?: storage

    val isLog: Boolean = (directional?.parentMechanic?.isLog ?: logStripping) != null

    fun log() = directional?.parentMechanic?.logStripping ?: logStripping

    fun isFalling() = directional?.parentMechanic?.isFalling ?: isFalling

    fun canIgnite() = directional?.parentMechanic?.canIgnite ?: canIgnite

    fun isBeaconBaseBlock() = directional?.parentMechanic?.beaconBaseBlock ?: beaconBaseBlock

    val isDirectional = directional != null

    override val breakable: BreakableMechanic = directional?.parentMechanic?.breakable ?: super.breakable

    override fun hasLight(): Boolean {
        return directional?.parentMechanic?.hasLight() ?: super.hasLight()
    }

    override val blockSounds: BlockSounds? get() {
        return directional?.parentMechanic?.blockSounds ?: super.blockSounds
    }

    val isInteractable: Boolean
        get() = hasClickActions() || isStorage()

    companion object {
        val FALLING_KEY = NamespacedKey.fromString("nexo:falling_custom_block")!!
    }
}
