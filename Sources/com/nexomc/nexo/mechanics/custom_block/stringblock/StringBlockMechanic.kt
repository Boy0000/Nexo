package com.nexomc.nexo.mechanics.custom_block.stringblock

import com.nexomc.nexo.mechanics.MechanicFactory
import com.nexomc.nexo.mechanics.custom_block.CustomBlockMechanic
import com.nexomc.nexo.mechanics.custom_block.stringblock.sapling.SaplingMechanic
import org.bukkit.block.data.type.Tripwire
import org.bukkit.configuration.ConfigurationSection

class StringBlockMechanic(factory: StringBlockMechanicFactory, section: ConfigurationSection) : CustomBlockMechanic(factory, section) {
    private val randomPlace: List<String> = section.getStringList("random_place")
    private val sapling: SaplingMechanic? = section.getConfigurationSection("sapling")?.let { SaplingMechanic(itemID, it) }
    // Creates an instance of CustomBlockMechanic and applies the below
    val isTall: Boolean = section.getBoolean("is_tall")
    val isPlaceableOnWater: Boolean = section.getBoolean("placeable_on_water")

    override val blockData = super.blockData as? Tripwire

    override fun createBlockData() = StringMechanicHelpers.modernBlockData(customVariation)

    fun isSapling(): Boolean {
        return sapling != null
    }

    fun sapling(): SaplingMechanic? {
        return sapling
    }

    fun hasRandomPlace(): Boolean {
        return randomPlace.isNotEmpty()
    }

    fun randomPlace(): List<String> {
        return randomPlace
    }
}
