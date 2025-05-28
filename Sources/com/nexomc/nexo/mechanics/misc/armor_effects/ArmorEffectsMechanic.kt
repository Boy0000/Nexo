package com.nexomc.nexo.mechanics.misc.armor_effects

import com.nexomc.nexo.api.NexoItems
import com.nexomc.nexo.mechanics.Mechanic
import com.nexomc.nexo.mechanics.MechanicFactory
import com.nexomc.nexo.utils.PotionUtils.getEffectType
import com.nexomc.nexo.utils.customarmor.CustomArmorType
import com.nexomc.nexo.utils.logs.Logs
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffect

class ArmorEffectsMechanic(factory: MechanicFactory, section: ConfigurationSection) : Mechanic(factory, section) {
    val armorEffects = ObjectOpenHashSet<ArmorEffect>()

    init {
        section.getKeys(false).forEach { effect ->
            section.getConfigurationSection(effect)?.let(::registersEffectFromSection)
        }
    }

    fun registersEffectFromSection(section: ConfigurationSection) {
        val effectType = getEffectType(section.name.lowercase()) ?: return Logs.logError("Invalid potion effect: ${section.name}, in ${section.currentPath!!.substringBefore(".")}!")

        val duration = section.getInt("duration", ArmorEffectsFactory.instance().delay)
        val amplifier = section.getInt("amplifier", 0)
        val ambient = section.getBoolean("ambient", false)
        val particles = section.getBoolean("particles", true)
        val icon = section.getBoolean("icon", true)

        val requiresFullSet = section.getBoolean("requires_full_set", false)
        val resetOnUnequip = section.getBoolean("reset_on_unequip", false)
        val potionEffect = PotionEffect(effectType, duration, amplifier, ambient, particles, icon)
        armorEffects += ArmorEffect(potionEffect, requiresFullSet, resetOnUnequip)
    }

    companion object {
        private val ARMOR_SLOTS = setOf(36, 37, 38, 39)
        fun addEffects(player: Player) {
            ARMOR_SLOTS.forEach { armorSlot ->
                val armorPiece = player.inventory.getItem(armorSlot) ?: return@forEach
                val mechanic = ArmorEffectsFactory.instance().getMechanic(armorPiece) ?: return@forEach

                val usingFullSet = ARMOR_SLOTS.all { slot ->
                    slot == armorSlot || player.inventory.getItem(slot)?.takeIf {
                        armorNameFromId(NexoItems.idFromItem(it)) == armorNameFromId(mechanic.itemID)
                    } != null
                }
                mechanic.armorEffects.forEach {
                    if (!it.requiresFullSet || usingFullSet) player.addPotionEffect(it)
                }
            }
        }

        fun removeEffects(player: Player, oldItem: ItemStack) {
            val mechanic = ArmorEffectsFactory.instance().getMechanic(oldItem) ?: return

            mechanic.armorEffects.forEach {
                if (it.resetOnUnequip) player.removePotionEffect(it.effect.type)
            }
        }

        private fun armorNameFromId(itemId: String?) = itemId?.replace(CustomArmorType.itemIdRegex, "")
    }
}
