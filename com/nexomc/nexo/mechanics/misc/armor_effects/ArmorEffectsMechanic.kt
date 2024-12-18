package com.nexomc.nexo.mechanics.misc.armor_effects

import com.nexomc.nexo.api.NexoItems
import com.nexomc.nexo.mechanics.Mechanic
import com.nexomc.nexo.mechanics.MechanicFactory
import com.nexomc.nexo.utils.PotionUtils.getEffectType
import com.nexomc.nexo.utils.logs.Logs
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffect

class ArmorEffectsMechanic(mechanicFactory: MechanicFactory?, section: ConfigurationSection) :
    Mechanic(mechanicFactory, section) {
    val armorEffects = mutableSetOf<ArmorEffect>()

    init {
        section.getKeys(false).forEach { effect ->
            section.getConfigurationSection(effect)?.let(::registersEffectFromSection)
        }
    }

    fun registersEffectFromSection(section: ConfigurationSection) {
        val type = section.name.lowercase()
        val effectType = getEffectType(type) ?: return Logs.logError("Invalid potion effect: ${section.name}, in ${section.currentPath!!.substringBefore(".")}!")

        val duration = section.getInt("duration", ArmorEffectsFactory.instance().delay)
        val amplifier = section.getInt("amplifier", 0)
        val ambient = section.getBoolean("ambient", false)
        val particles = section.getBoolean("particles", true)
        val icon = section.getBoolean("icon", true)

        val requiresFullSet = section.getBoolean("requires_full_set", false)
        val potionEffect = PotionEffect(effectType, duration, amplifier, ambient, particles, icon)
        armorEffects += ArmorEffect(potionEffect, requiresFullSet)
    }

    companion object {
        private val ARMOR_SLOTS = setOf(36, 37, 38, 39)
        fun addEffects(player: Player) {
            ARMOR_SLOTS.forEach { armorSlot ->
                val armorPiece = player.inventory.getItem(armorSlot)
                val mechanic = ArmorEffectsFactory.instance().getMechanic(armorPiece) ?: return@forEach

                val finalArmorEffects = mechanic.armorEffects.mapNotNull { armorEffect ->
                    when {
                        armorEffect.requiresFullSet -> armorEffect.effect.takeIf {
                            ARMOR_SLOTS.filterNot(armorSlot::equals).all { slot ->
                                player.inventory.getItem(slot)?.takeIf { isSameArmorType(armorPiece, it) } != null
                            }
                        }
                        else -> armorEffect.effect
                    }
                }

                player.addPotionEffects(finalArmorEffects)
            }
        }

        private fun isSameArmorType(firstItem: ItemStack?, secondItem: ItemStack) =
            armorNameFromId(NexoItems.idFromItem(firstItem)) == armorNameFromId(NexoItems.idFromItem(secondItem))

        private fun armorNameFromId(itemId: String?) = itemId?.substringBeforeLast("_")
    }
}
