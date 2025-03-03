package com.nexomc.nexo.mechanics.custom_block.stringblock

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.BlockFace
import org.bukkit.block.data.type.Tripwire
import org.bukkit.entity.Player

object StringMechanicHelpers {
    private val BLOCK_FACES = arrayOf(BlockFace.EAST, BlockFace.WEST, BlockFace.SOUTH, BlockFace.NORTH)
    fun legacyBlockData(customVariation: Int): Tripwire {
        return (Bukkit.createBlockData(Material.TRIPWIRE) as Tripwire).also { tripwire ->
            var i = 0
            BLOCK_FACES.forEach { face ->
                tripwire.setFace(face, (customVariation and (0x1 shl i++)) != 0)
            }
            tripwire.isAttached = (customVariation and (0x1 shl i++)) != 0
            tripwire.isDisarmed = (customVariation and (0x1 shl i++)) != 0
            tripwire.isPowered = (customVariation and (0x1 shl i)) != 0
        }
    }

    fun modernBlockData(customVariation: Int): Tripwire {
        return (Bukkit.createBlockData(Material.TRIPWIRE) as Tripwire).also { tripwire ->
            tripwire.setFace(BlockFace.NORTH, (customVariation and 0x1) != 0)
            tripwire.setFace(BlockFace.SOUTH, (customVariation and 0x2) != 0)
            tripwire.setFace(BlockFace.EAST, (customVariation and 0x4) != 0)
            tripwire.setFace(BlockFace.WEST, (customVariation and 0x8) != 0)
            tripwire.isAttached = (customVariation and 0x10) != 0
            tripwire.isDisarmed = (customVariation and 0x20) != 0
            tripwire.isPowered = (customVariation and 0x40) != 0
        }
    }

    fun modernCustomVariation(tripwire: Tripwire): Int {
        var customVariation = 0

        if (tripwire.hasFace(BlockFace.NORTH)) customVariation = customVariation or 0x1
        if (tripwire.hasFace(BlockFace.SOUTH)) customVariation = customVariation or 0x2
        if (tripwire.hasFace(BlockFace.EAST)) customVariation = customVariation or 0x4
        if (tripwire.hasFace(BlockFace.WEST)) customVariation = customVariation or 0x8

        if (tripwire.isAttached) customVariation = customVariation or 0x10
        if (tripwire.isDisarmed) customVariation = customVariation or 0x20
        if (tripwire.isPowered) customVariation = customVariation or 0x40

        return customVariation
    }

    fun fixClientsideUpdate(loc: Location) {
        val players = loc.world.getNearbyEntitiesByType(Player::class.java, loc, 20.0)
        (loc.blockX.minus(10)..loc.blockX.plus(10)).forEach { x ->
            (loc.blockY.minus(4)..loc.blockY.plus(4)).forEach { y ->
                (loc.blockZ.minus(10)..loc.blockZ.plus(10)).forEach z@{ z ->
                    val location = Location(loc.getWorld(), x.toDouble(), y.toDouble(), z.toDouble())
                    val blockData = location.block.blockData.takeIf { it is Tripwire } ?: return@z
                    players.forEach {
                        it.sendBlockChange(location, blockData)
                    }
                }
            }
        }
    }
}
