package com.nexomc.nexo.mechanics.custom_block.chorusblock

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.block.BlockFace
import org.bukkit.block.data.MultipleFacing

object ChorusMechanicHelpers {

    fun blockData(customVariation: Int): MultipleFacing {
        return (Bukkit.createBlockData(Material.CHORUS_PLANT) as MultipleFacing).also { chorus ->
            chorus.setFace(BlockFace.NORTH, (customVariation and 0x1) != 0)
            chorus.setFace(BlockFace.SOUTH, (customVariation and 0x2) != 0)
            chorus.setFace(BlockFace.EAST, (customVariation and 0x4) != 0)
            chorus.setFace(BlockFace.WEST, (customVariation and 0x8) != 0)
            chorus.setFace(BlockFace.UP, (customVariation and 0x10) != 0)
            chorus.setFace(BlockFace.DOWN, (customVariation and 0x20) != 0)
        }
    }
}