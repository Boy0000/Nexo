package com.nexomc.nexo.utils.breaker

import com.nexomc.nexo.mechanics.breakable.BreakableMechanic
import com.nexomc.nexo.mechanics.custom_block.CustomBlockMechanic
import com.nexomc.nexo.mechanics.furniture.FurnitureMechanic
import com.nexomc.nexo.utils.wrappers.AttributeWrapper
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.attribute.AttributeModifier
import org.bukkit.block.Block
import org.bukkit.entity.ItemDisplay
import org.bukkit.entity.Player
import org.bukkit.inventory.EquipmentSlot
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.set

class ModernBreakerManager(private val modifierMap: ConcurrentHashMap<UUID, AttributeModifier>) : BreakerManager {

    override fun startFurnitureBreak(
        player: Player,
        baseEntity: ItemDisplay,
        mechanic: FurnitureMechanic,
        block: Block
    ) {
        //TODO See if this can be handled even with packet-barriers
    }

    override fun startBlockBreak(player: Player, block: Block, mechanic: CustomBlockMechanic) {
        removeTransientModifier(player)
        if (player.gameMode == GameMode.CREATIVE) return

        addTransientModifier(player, createBreakingModifier(player, block, mechanic.breakable))
    }

    override fun stopBlockBreak(player: Player) {
        removeTransientModifier(player)
    }

    private fun createBreakingModifier(player: Player, block: Block, breakable: BreakableMechanic): AttributeModifier {
        return AttributeModifier.deserialize(
            mapOf<String, Any>(
                "slot" to EquipmentSlot.HAND,
                "uuid" to UUID.nameUUIDFromBytes(block.toString().toByteArray()).toString(),
                "name" to "nexo:custom_break_speed",
                "operation" to AttributeModifier.Operation.MULTIPLY_SCALAR_1,
                "amount" to (defaultBlockHardness(block) / breakable.hardness * breakable.speedMultiplier(player)) - 1
            )
        )
    }

    private fun defaultBlockHardness(block: Block): Double {
        return when (block.type) {
            Material.NOTE_BLOCK -> 0.8
            Material.TRIPWIRE -> 1.0
            else -> 1.0
        }
    }

    private fun addTransientModifier(player: Player, modifier: AttributeModifier) {
        removeTransientModifier(player)
        modifierMap[player.uniqueId] = modifier
        player.getAttribute(AttributeWrapper.BLOCK_BREAK_SPEED!!)?.addTransientModifier(modifier)
    }

    private fun removeTransientModifier(player: Player) {
        val modifier = modifierMap[player.uniqueId] ?: return
        val instance = player.getAttribute(AttributeWrapper.BLOCK_BREAK_SPEED!!) ?: return
        instance.removeModifier(modifier)
    }
}
