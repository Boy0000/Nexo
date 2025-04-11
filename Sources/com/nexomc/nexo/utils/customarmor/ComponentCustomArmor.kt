package com.nexomc.nexo.utils.customarmor

import com.nexomc.nexo.api.NexoItems
import com.nexomc.nexo.configs.Settings
import com.nexomc.nexo.items.ItemBuilder
import com.nexomc.nexo.items.NexoMeta
import com.nexomc.nexo.utils.KeyUtils.appendSuffix
import com.nexomc.nexo.utils.Quadruple
import com.nexomc.nexo.utils.logs.Logs
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet
import java.util.Collections
import net.kyori.adventure.key.Key
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import team.unnamed.creative.ResourcePack
import team.unnamed.creative.equipment.Equipment
import team.unnamed.creative.equipment.EquipmentLayer
import team.unnamed.creative.equipment.EquipmentLayerType
import team.unnamed.creative.metadata.overlays.OverlayEntry
import team.unnamed.creative.metadata.overlays.OverlaysMeta
import team.unnamed.creative.metadata.pack.PackFormat
import team.unnamed.creative.overlay.Overlay
import team.unnamed.creative.texture.Texture

class ComponentCustomArmor(private val resourcePack: ResourcePack) {
    fun generatePackFiles() {
        val armorPrefixes = NexoItems.entries().entries.mapNotNullTo(ObjectLinkedOpenHashSet()) { (itemId, builder) ->
            val prefix = itemId.replace(CustomArmorType.itemIdRegex, "$1")
            // If there is no CustomArmor data at all, we skip
            if (builder.nexoMeta?.customArmorTextures == null) return@mapNotNullTo null
            // If there is no defined Equippable-model, return prefix
            val modelKey = builder.equippable?.model?.key() ?: return@mapNotNullTo prefix
            // If there is a model, and it is in the pack, we do not need to generate anything
            if (resourcePack.equipment(modelKey) != null) return@mapNotNullTo null
            if (resourcePack.unknownFile("assets/${modelKey.namespace()}/equipment/${modelKey.value()}") != null) return@mapNotNullTo null
            if (resourcePack.unknownFile("assets/${modelKey.namespace()}/models/equipment/${modelKey.value()}") != null) return@mapNotNullTo null
            else return@mapNotNullTo prefix
        }
        writeArmorModels(armorPrefixes)
        parseNexoArmorItems(armorPrefixes)
        copyEquipmentTextures()
    }

    private fun writeArmorModels(armorPrefixes: Set<String>) {
        armorPrefixes.forEach { armorPrefix ->
            val key = Key.key("nexo", armorPrefix)
            val elytraKey = key.appendSuffix("_elytra")

            val equipmentLayer = Collections.singletonList(EquipmentLayer.layer(key))
            val equipment = Equipment.equipment(key, EquipmentLayerType.entries.take(6).minus(EquipmentLayerType.WINGS).associateWith { equipmentLayer })
            val elytraEquipment = Equipment.equipment(elytraKey, mapOf(EquipmentLayerType.WINGS to equipmentLayer))
            val saddleEquipment = Equipment.equipment(key, EquipmentLayerType.entries.dropLast(1).associateWith { equipmentLayer })
            val harnessEquipment = Equipment.equipment(key, EquipmentLayerType.entries.associateWith { equipmentLayer })

            if (resourcePack.equipment(key) == null) resourcePack.equipment(equipment)
            if (resourcePack.equipment(elytraKey) == null) resourcePack.equipment(elytraEquipment)

            val overlays = resourcePack.overlaysMeta()?.entries() ?: mutableListOf()
            // Add overlay for saddle-equipment layers and limit to 1.21.5+ clients
            resourcePack.overlay(Overlay.overlay("nexo_1_21_5").apply { equipment(saddleEquipment) })
            overlays += OverlayEntry.of(PackFormat.format(55, 55, 99), "nexo_1_21_5")
            // Add overlay for harness-equipment layers and limit to 1.21.6+ clients
            resourcePack.overlay(Overlay.overlay("nexo_1_21_6").apply { equipment(harnessEquipment) })
            overlays += OverlayEntry.of(PackFormat.format(56, 56, 99), "nexo_1_21_6")
            resourcePack.overlaysMeta(OverlaysMeta.of(overlays))
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
                customArmor.camelSaddle -> "camel_saddle" to "camel_saddle"
                customArmor.donkeySaddle -> "donkey_saddle" to "donkey_saddle"
                customArmor.horseSaddle -> "horse_saddle" to "horse_saddle"
                customArmor.muleSaddle -> "mule_saddle" to "mule_saddle"
                customArmor.pigSaddle -> "pig_saddle" to "pig_saddle"
                customArmor.skeletonHorseSaddle -> "skeleton_horse_saddle" to "skeleton_horse_saddle"
                customArmor.striderSaddle -> "strider_saddle" to "strider_saddle"
                customArmor.zombieHorseSaddle -> "zombie_horse_saddle" to "zombie_horse_saddle"
                customArmor.harness -> "harness" to "happy_ghast_body"
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
            val modelKey = NamespacedKey.fromString("nexo:$armorPrefix$armorSuffix")!!

            // If an equippable model is defined, and it is in the resourcepack, return
            item.equippable?.model?.also { model ->
                if (resourcePack.equipment(model.key()) != null) return@forEach
                if (resourcePack.unknownFile("assets/${model.namespace}/models/equipment/${model.key}.json") != null) return@forEach
                if (resourcePack.unknownFile("assets/${model.namespace}/equipment/${model.key}.json") != null) return@forEach
            }

            // If we should not auto-assign properties return
            if (!Settings.CUSTOM_ARMOR_ASSIGN.toBool()) {
                // Warn if model does not match the automatic format as this might
                // not have been generated by Nexo just yet.
                if (item.equippable?.model != modelKey) {
                    Logs.logWarn("Item $itemId does not have an equippable-component configured properly.")
                    Logs.logWarn("Nexo has been configured to use Components for custom-armor due to ${Settings.CUSTOM_ARMOR_TYPE.path} setting")
                    Logs.logWarn("Custom Armor will not work unless an equippable-component is set.", true)
                }
                return@forEach
            }

            val vanillaComponent = ItemStack(item.type).itemMeta.equippable.apply { this.slot = EquipmentSlot.HAND }
            val component = (item.equippable?.takeIf { it.model != modelKey } ?: vanillaComponent) ?: return@forEach
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
            "CAMEL_SADDLE", "DONKEY_SADDLE", "HORSE_SADDLE", "MULE_SADDLE", "PIG_SADDLE", "SKELETON_HORSE_SADDLE", "STRIDER_SADDLE", "ZOMBIE_HORSE_SADDLE" -> EquipmentSlot.BODY
            "HARNESS" -> EquipmentSlot.BODY
            else -> null
        }
    }
}
