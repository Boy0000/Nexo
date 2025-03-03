package com.nexomc.nexo.mechanics.breakable

import com.nexomc.nexo.api.NexoItems
import com.nexomc.nexo.mechanics.furniture.FurnitureFactory
import com.nexomc.nexo.utils.breaker.ToolTypeSpeedModifier
import com.nexomc.nexo.utils.drops.Drop
import com.nexomc.nexo.utils.drops.Loot
import com.nexomc.nexo.utils.wrappers.AttributeWrapper
import com.nexomc.nexo.utils.wrappers.EnchantmentWrapper
import com.nexomc.nexo.utils.wrappers.PotionEffectTypeWrapper
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.Player
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

class BreakableMechanic(section: ConfigurationSection) {
    val hardness: Double = section.getInt("hardness", 1).toDouble()
    //val treatAs: Material = section.getString("treat_as")?.let(Material::matchMaterial) ?: Material.STONE
    val drop: Drop
    private val itemId: String = section.parent!!.parent!!.name

    init {
        val dropSection = section.getConfigurationSection("drop")
        drop = when {
            dropSection != null -> Drop.createDrop(FurnitureFactory.instance()!!.toolTypes, dropSection, itemId)
            else -> Drop(mutableListOf(Loot(NexoItems.itemFromId(itemId)?.build(), 1.0)), false, false, itemId)
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

        //TODO Revisit at a later time
        /*var multiplier = when {
            VersionUtil.atleast("1.21.1") -> itemInMainHand.itemMeta?.tool?.let { tool ->
                tool.rules.firstOrNull { rule -> treatAs in rule.blocks }?.speed ?: tool.defaultMiningSpeed
            } ?: 1.0f
            else -> ToolTypeSpeedModifier.VANILLA
                .filter { itemInMainHand.type in it.toolTypes() }
                .minByOrNull { it.speedModifier() }?.takeIf { drop.isToolEnough(itemInMainHand) }
                ?.speedModifier() ?: ToolTypeSpeedModifier.EMPTY.speedModifier()
        }*/
        var multiplier = ToolTypeSpeedModifier.VANILLA
            .filter { itemInMainHand.type in it.toolTypes() }
            .minByOrNull { it.speedModifier() }?.takeIf { drop.isToolEnough(itemInMainHand) }
            ?.speedModifier() ?: ToolTypeSpeedModifier.EMPTY.speedModifier()

        val miningEff = AttributeWrapper.MINING_EFFICIENCY?.let(player::getAttribute)?.value?.toFloat()
        val effEnchant = itemInMainHand.getEnchantmentLevel(EnchantmentWrapper.EFFICIENCY)
        if (multiplier > 1.0f) multiplier += miningEff ?: (effEnchant.toDouble().pow(2.0) + 1).toFloat()


        val haste = player.getPotionEffect(PotionEffectTypeWrapper.HASTE)
        val conduitPower = player.getPotionEffect(PotionEffectTypeWrapper.CONDUIT_POWER)
        val amplifier = max(haste?.amplifier ?: 0, conduitPower?.amplifier ?: 0)
        if (amplifier > 0) multiplier *= 0.2f * amplifier + 1

        // Whilst the player has this when they start digging, period is calculated before it is applied
        val miningFatigue = player.getPotionEffect(PotionEffectTypeWrapper.MINING_FATIGUE)
        if (miningFatigue != null) multiplier *= 0.3.pow(min(miningFatigue.amplifier.toDouble(), 4.0)).toFloat()

        // 1.20.5+ speed-modifier attribute
        multiplier *= AttributeWrapper.BLOCK_BREAK_SPEED?.let(player::getAttribute)?.value?.toFloat() ?: 1f

        if (player.isUnderWater) {
            val submerged = AttributeWrapper.SUBMERGED_MINING_SPEED?.let(player::getAttribute)?.value?.toFloat()
            val aquaAffinity = player.equipment?.helmet?.containsEnchantment(EnchantmentWrapper.AQUA_AFFINITY) == true
            multiplier *= submerged ?: if (aquaAffinity) 1.0f else 0.2f
        }

        if (!player.isOnGround) multiplier /= 5f

        return multiplier.toDouble()
    }
}
