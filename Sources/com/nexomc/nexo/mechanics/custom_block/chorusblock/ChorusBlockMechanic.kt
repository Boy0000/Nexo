package com.nexomc.nexo.mechanics.custom_block.chorusblock

import com.nexomc.nexo.mechanics.custom_block.CustomBlockMechanic
import com.nexomc.nexo.mechanics.storage.StorageMechanic
import org.bukkit.block.data.MultipleFacing
import org.bukkit.configuration.ConfigurationSection

class ChorusBlockMechanic(factory: ChorusBlockFactory, section: ConfigurationSection) : CustomBlockMechanic(factory, section) {
    val isFalling: Boolean = section.getBoolean("is_falling", false)
    val storage: StorageMechanic? = section.getConfigurationSection("storage")?.let(::StorageMechanic)

    override val blockData = super.blockData as? MultipleFacing

    override fun createBlockData() = ChorusMechanicHelpers.blockData(customVariation)
}