package com.nexomc.nexo.utils.customarmor

import com.nexomc.nexo.api.NexoItems
import com.nexomc.nexo.configs.Settings
import com.nexomc.nexo.items.ItemBuilder
import com.nexomc.nexo.items.NexoMeta
import com.nexomc.nexo.utils.KeyUtils.appendSuffix
import com.nexomc.nexo.utils.Quadruple
import com.nexomc.nexo.utils.logs.Logs
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet
import net.kyori.adventure.key.Key
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import team.unnamed.creative.ResourcePack
import team.unnamed.creative.equipment.Equipment
import team.unnamed.creative.equipment.EquipmentLayer
import team.unnamed.creative.texture.Texture

class ComponentCustomArmor(private val resourcePack: ResourcePack) {
    fun generatePackFiles() {
        val armorPrefixes = NexoItems.entries().entries.mapNotNullTo(ObjectLinkedOpenHashSet()) { (itemId, builder) ->
            itemId.replace(CustomArmorType.itemIdRegex, "$1").takeUnless { builder.nexoMeta?.customArmorTextures == null }
        }
        writeArmorModels(armorPrefixes)
        parseNexoArmorItems(armorPrefixes)
        copyEquipmentTextures()
    }

    private fun writeArmorModels(armorPrefixes: Set<String>) {
        armorPrefixes.forEach { armorPrefix ->
            val key = Key.key("nexo", armorPrefix)
            val equipmentLayer = EquipmentLayer.layer(key)
            resourcePack.equipment(
                Equipment.equipment().key(key)
                    .addHumanoidLayer(equipmentLayer)
                    .addHumanoidLeggingsLayer(equipmentLayer)
                    .addWolfBodyLayer(equipmentLayer)
                    .addHorseBodyLayer(equipmentLayer)
                    .addLlamaBodyLayer(equipmentLayer).build()
            )
            resourcePack.equipment(Equipment.equipment().key(key.appendSuffix("_elytra")).addWingsLayer(equipmentLayer).build())
        }
    }

    private val Map.Entry<String, ItemBuilder>.armorProperties: Quadruple<String, String, ItemBuilder, NexoMeta.CustomArmorTextures>?
        get() = value.nexoMeta?.customArmorTextures?.let { Quadruple(key.replace(CustomArmorType.itemIdRegex, "$1"), key, value, it) }

    private fun copyEquipmentTextures() {
        NexoItems.entries().entries.mapNotNull { it.armorProperties }.distinctBy { it.fourth.fromItem(it.third, it.second) }.forEach { (prefix, itemId, item, customArmor) ->
            val customArmorKey = customArmor.fromItem(item, itemId) ?: return@forEach
            val (fallback, path) = when (customArmorKey) {
                customArmor.layer1 -> "armor_layer_1" to "humanoid"
                customArmor.layer2 -> "armor_layer_2" to "humanoid_leggings"
                customArmor.elytra -> "elytra" to "wings"
                customArmor.wolfArmor -> "wolf_armor" to "wolf_body"
                customArmor.llamaArmor -> "llama_armor" to "llama_body"
                customArmor.horseArmor -> "horse_armor" to "horse_body"
                else -> return@forEach
            }
            val texture = fetchTexture(customArmorKey, "${prefix}_${fallback}.png", itemId) ?: return@forEach

            resourcePack.removeTexture(customArmorKey)
            resourcePack.texture(Key.key("nexo:entity/equipment/$path/$prefix.png"), texture.data())
        }
    }

    private fun fetchTexture(texture: Key, fallbackName: String, itemId: String): Texture? {
        return resourcePack.texture(texture)
            ?: resourcePack.textures().find { it.key().value().substringAfterLast("/") == fallbackName }
            ?: run { Logs.logWarn("Failed to fetch ${texture.asString()} used by $itemId"); null }
    }

    private fun parseNexoArmorItems(armorPrefixes: Set<String>) {
        NexoItems.entries().forEach { (itemId, item) ->
            val armorPrefix = itemId.replace(CustomArmorType.itemIdRegex, "$1").takeIf(armorPrefixes::contains) ?: return@forEach
            val armorSuffix = if (item.type == Material.ELYTRA || item.isGlider == true) "_elytra" else ""
            val slot = slotFromItem(itemId.removePrefix("${armorPrefix}_").uppercase()) ?: return@forEach

            if (!Settings.CUSTOM_ARMOR_ASSIGN.toBool()) {
                Logs.logWarn("Item $itemId does not have an equippable-component configured properly.")
                Logs.logWarn("Nexo has been configured to use Components for custom-armor due to ${Settings.CUSTOM_ARMOR_TYPE.path} setting")
                Logs.logWarn("Custom Armor will not work unless an equippable-component is set.", true)
                return@forEach
            }

            val modelKey = NamespacedKey.fromString("nexo:$armorPrefix$armorSuffix")!!
            val vanillaComponent = ItemStack(item.type).itemMeta.equippable.apply { this.slot = EquipmentSlot.HAND }
            val component = (item.equippable ?: vanillaComponent).takeIf { it.model != modelKey } ?: return@forEach
            if (item.nexoMeta?.generateModel != false) component.model = modelKey
            component.slot = slot
            if (component == item.equippable) return@forEach
            item.setEquippableComponent(component)
            item.save()
            Logs.logWarn("Item $itemId does not have an equippable-component set.")
            Logs.logInfo("Configured Components.equippable.model to $modelKey for $itemId")
        }
    }

    private fun slotFromItem(suffix: String): EquipmentSlot? {
        return when (suffix) {
            "HELMET" -> EquipmentSlot.HEAD
            "CHESTPLATE", "ELYTRA" -> EquipmentSlot.CHEST
            "LEGGINGS" -> EquipmentSlot.LEGS
            "BOOTS" -> EquipmentSlot.FEET
            "WOLF_ARMOR", "LLAMA_ARMOR", "HORSE_ARMOR" -> EquipmentSlot.BODY
            else -> null
        }
    }
}
