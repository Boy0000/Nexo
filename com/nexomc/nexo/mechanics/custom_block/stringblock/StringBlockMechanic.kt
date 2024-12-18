package com.nexomc.nexo.mechanics.custom_block.stringblock

import com.nexomc.nexo.configs.Settings
import com.nexomc.nexo.mechanics.MechanicFactory
import com.nexomc.nexo.mechanics.custom_block.CustomBlockMechanic
import com.nexomc.nexo.mechanics.custom_block.stringblock.sapling.SaplingMechanic
import org.bukkit.block.BlockFace
import org.bukkit.block.data.type.Tripwire
import org.bukkit.configuration.ConfigurationSection

class StringBlockMechanic(mechanicFactory: MechanicFactory?, section: ConfigurationSection) :
    CustomBlockMechanic(mechanicFactory, section) {
    private val randomPlace: List<String>
    private val sapling: SaplingMechanic?
    val isTall: Boolean
    val isPlaceableOnWater: Boolean


    init {
        // Creates an instance of CustomBlockMechanic and applies the below
        isTall = section.getBoolean("is_tall")
        isPlaceableOnWater = section.getBoolean("placeable_on_water")
        randomPlace = section.getStringList("random_place")

        val saplingSection = section.getConfigurationSection("sapling")
        sapling = if (saplingSection != null) SaplingMechanic(itemID, saplingSection) else null
    }

    override val blockData = super.blockData as? Tripwire

    private val BLOCK_FACES = arrayOf(BlockFace.EAST, BlockFace.WEST, BlockFace.SOUTH, BlockFace.NORTH)
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
