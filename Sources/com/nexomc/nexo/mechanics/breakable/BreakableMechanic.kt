package com.nexomc.nexo.mechanics.breakable

import com.nexomc.nexo.mechanics.furniture.FurnitureFactory
import com.nexomc.nexo.utils.breaker.ToolTypeSpeedModifier
import com.nexomc.nexo.utils.drops.Drop
import com.nexomc.nexo.utils.drops.Loot
import com.nexomc.nexo.utils.wrappers.AttributeWrapper
import com.nexomc.nexo.utils.wrappers.EnchantmentWrapper
import com.nexomc.nexo.utils.wrappers.PotionEffectTypeWrapper
import org.bukkit.attribute.AttributeInstance
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.Player
import java.util.*
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.ceil
import kotlin.math.min
import kotlin.math.pow

class BreakableMechanic(section: ConfigurationSection) {
    val hardness: Double = section.getInt("hardness", 1).toDouble()
    val drop: Drop
    private val itemId: String = section.parent!!.parent!!.name

    init {
        val dropSection = section.getConfigurationSection("drop")
        drop = when {
            dropSection != null -> Drop.createDrop(FurnitureFactory.instance()!!.toolTypes, dropSection, itemId)
            else -> Drop(ArrayList<Loot>(), false, false, itemId)
        }
    }

    /**
     * Calculates the time it should take for a Player to break this CustomBlock / Furniture
     *
     * @param player The Player breaking the block
     * @return Time in ticks it takes for player to break this block / furniture
     */
    fun breakTime(player: Player): Int {
        val damage = speedMultiplier(player) / hardness / 30
        return if (damage > 1) 0 else ceil(1 / damage).toInt()
    }

    fun speedMultiplier(player: Player): Double {
        val itemInMainHand = player.inventory.itemInMainHand
        val speedMultiplier = AtomicReference(1f)

        ToolTypeSpeedModifier.VANILLA.stream()
            .filter { t: ToolTypeSpeedModifier -> t.toolTypes().contains(itemInMainHand.type) }
            .min(Comparator.comparingDouble { it.speedModifier().toDouble() })
            .ifPresentOrElse(
                {
                    speedMultiplier.set(
                        if (drop.isToolEnough(itemInMainHand)) it.speedModifier()
                        else ToolTypeSpeedModifier.EMPTY.speedModifier()
                    )
                },
                { speedMultiplier.set(ToolTypeSpeedModifier.EMPTY.speedModifier()) }
            )

        var multiplier = speedMultiplier.get()

        val efficiencyLevel = itemInMainHand.getEnchantmentLevel(EnchantmentWrapper.EFFICIENCY)
        if (multiplier > 1.0f && efficiencyLevel != 0) multiplier += (efficiencyLevel.toDouble().pow(2.0) + 1).toFloat()

        val haste = player.getPotionEffect(PotionEffectTypeWrapper.HASTE)
        if (haste != null) multiplier *= 1.0f + (haste.amplifier + 1).toFloat() * 0.2f

        // Whilst the player has this when they start digging, period is calculated before it is applied
        val miningFatigue = player.getPotionEffect(PotionEffectTypeWrapper.MINING_FATIGUE)
        if (miningFatigue != null) multiplier *= 0.3.pow(min(miningFatigue.amplifier.toDouble(), 4.0)).toFloat()

        // 1.20.5+ speed-modifier attribute
        val miningSpeedModifier = Optional.ofNullable(
            AttributeWrapper.BLOCK_BREAK_SPEED?.let(player::getAttribute)
        ).map(AttributeInstance::getBaseValue).orElse(1.0).toFloat()
        multiplier *= miningSpeedModifier

        if (player.isUnderWater && (player.equipment.helmet)?.containsEnchantment(EnchantmentWrapper.AQUA_AFFINITY) == true)
            multiplier /= 5f

        if (!player.isOnGround) multiplier /= 5f

        return multiplier.toDouble()
    }
}
