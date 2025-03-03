package com.nexomc.nexo.utils.customarmor

import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent
import com.nexomc.nexo.api.NexoItems
import com.nexomc.nexo.configs.Settings
import com.nexomc.nexo.utils.logs.Logs
import net.kyori.adventure.key.Key
import org.bukkit.NamespacedKey
import org.bukkit.Registry
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.PrepareSmithingEvent
import org.bukkit.event.player.PlayerArmorStandManipulateEvent
import org.bukkit.event.player.PlayerAttemptPickupItemEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ArmorMeta
import org.bukkit.inventory.meta.trim.ArmorTrim
import org.bukkit.inventory.meta.trim.TrimMaterial

class CustomArmorListener : Listener {

    @EventHandler
    fun PlayerArmorChangeEvent.onPlayerEquipVanilla() {
        setVanillaArmorTrim(oldItem)
        setVanillaArmorTrim(newItem)
    }

    @EventHandler
    fun PlayerAttemptPickupItemEvent.onPlayerPickup() {
        this.item.itemStack = item.itemStack.apply(::setVanillaArmorTrim)
    }

    @EventHandler
    fun PrepareSmithingEvent.onTrimCustomArmor() {
        val armorMeta = inventory.inputEquipment?.takeIf { it.hasItemMeta() && NexoItems.exists(it) }?.itemMeta as? ArmorMeta ?: return
        if (CustomArmorType.setting == CustomArmorType.NONE || armorMeta.trim?.pattern?.key()?.namespace() != "nexo") return
        result = null
    }

    @EventHandler
    fun PlayerJoinEvent.updateVanillaArmor() {
        for (item in player.inventory.contents) setVanillaArmorTrim(item)
    }

    @EventHandler
    fun PlayerArmorStandManipulateEvent.onAmorStandEquip() {
        setVanillaArmorTrim(playerItem)
        setVanillaArmorTrim(armorStandItem)
    }

    private fun setVanillaArmorTrim(itemStack: ItemStack?) {
        val armorMeta = itemStack?.itemMeta as? ArmorMeta ?: return
        if (CustomArmorType.setting != CustomArmorType.TRIMS) return
        if (!itemStack.type.name.startsWith("CHAINMAIL")) return
        if (armorMeta.hasTrim() && armorMeta.trim?.pattern?.key()?.namespace() == "nexo") return

        val vanillaPatternKey = Key.key("minecraft", "chainmail")
        val vanillaPattern = Registry.TRIM_PATTERN[NamespacedKey.fromString(vanillaPatternKey.asString())!!]
        when {
            vanillaPattern != null && (!armorMeta.hasItemFlag(ItemFlag.HIDE_ARMOR_TRIM) || !armorMeta.hasTrim() || (armorMeta.trim?.pattern?.key() != vanillaPatternKey)) -> {
                armorMeta.trim = ArmorTrim(TrimMaterial.REDSTONE, vanillaPattern)
                armorMeta.addItemFlags(ItemFlag.HIDE_ARMOR_TRIM)
                itemStack.itemMeta = armorMeta
            }
            vanillaPattern == null && Settings.DEBUG.toBool() ->
                Logs.logWarn("Vanilla trim-pattern not found for " + itemStack.type.name + ": " + vanillaPatternKey.asString())
        }
    }
}
