package com.nexomc.nexo.utils.customarmor

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.nexomc.nexo.api.NexoItems
import com.nexomc.nexo.configs.Settings
import com.nexomc.nexo.utils.logs.Logs
import net.kyori.adventure.key.Key
import org.bukkit.NamespacedKey
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import team.unnamed.creative.ResourcePack
import team.unnamed.creative.base.Writable
import team.unnamed.creative.texture.Texture

object ComponentCustomArmor {
    fun generatePackFiles(resourcePack: ResourcePack) {
        val armorPrefixes = NexoItems.entries().mapNotNullTo(LinkedHashSet()) { (itemId, builder) ->
            itemId.substringBeforeLast("_").takeUnless { builder.nexoMeta?.customArmorTextures == null }
        }
        writeArmorModels(resourcePack, armorPrefixes)
        copyArmorLayerTextures(resourcePack)
        parseNexoArmorItems(armorPrefixes)
    }

    private fun writeArmorModels(resourcePack: ResourcePack, armorPrefixes: Set<String>) {
        armorPrefixes.forEach { armorprefix ->
            val armorModelArray = JsonArray().apply {
                add(JsonObject().apply {
                    addProperty("texture", "nexo:$armorprefix")
                })
            }

            val equipmentModel = JsonObject().apply {
                add("layers", JsonObject().apply {
                    add("humanoid", armorModelArray)
                    add("humanoid_leggings", armorModelArray)
                })
            }

            for (path in setOf("assets/nexo/models/equipment/$armorprefix.json", "assets/nexo/equipment/$armorprefix.json")) {
                if (path in resourcePack.unknownFiles()) return@forEach
                resourcePack.unknownFile(path, Writable.stringUtf8(equipmentModel.toString()))
            }
        }
    }

    private fun copyArmorLayerTextures(resourcePack: ResourcePack) {
        NexoItems.entries().forEach { (itemId, item) ->
            val customArmor = item.nexoMeta?.customArmorTextures ?: return@forEach
            val armorPrefix = itemId.substringBeforeLast("_")
            val layer1 = resourcePack.texture(customArmor.layer1)
                ?: resourcePack.textures().firstOrNull { it.key().value().substringAfterLast("/") == armorPrefix.plus("_armor_layer_1.png") }
                ?: return@forEach Logs.logWarn("Failed to fetch ${customArmor.layer1.asString()} used by $itemId")
            val layer2 = resourcePack.texture(customArmor.layer2)
                ?: resourcePack.textures().firstOrNull { it.key().value().substringAfterLast("/") == armorPrefix.plus("_armor_layer_2.png") }
                ?: return@forEach Logs.logWarn("Failed to fetch ${customArmor.layer2.asString()} used by $itemId")

            resourcePack.removeTexture(customArmor.layer1)
            resourcePack.removeTexture(customArmor.layer2)

            resourcePack.texture(Key.key("nexo:entity/equipment/humanoid/$armorPrefix.png"), layer1.data())
            resourcePack.texture(Key.key("nexo:entity/equipment/humanoid_leggings/$armorPrefix.png"), layer2.data())
        }
    }

    private fun parseNexoArmorItems(armorPrefixes: Set<String>) {
        NexoItems.entries().forEach { (itemId, itemBuilder) ->
            val armorPrefix = itemId.substringBeforeLast("_").takeIf(armorPrefixes::contains) ?: return@forEach
            val slot = slotFromItem(itemId)

            if (slot == null) {
                if (!Settings.CUSTOM_ARMOR_ASSIGN.toBool()) {
                    Logs.logWarn("Item $itemId does not have an equippable-component configured properly.")
                    Logs.logWarn("Nexo has been configured to use Components for custom-armor due to ${Settings.CUSTOM_ARMOR_TYPE.path} setting")
                    Logs.logWarn("Custom Armor will not work unless an equippable-component is set.", true)
                }
                return@forEach
            }

            val modelKey = NamespacedKey.fromString("nexo:$armorPrefix")!!
            val vanillaComponent = ItemStack(itemBuilder.type).itemMeta.equippable.apply { this.slot = EquipmentSlot.HAND }
            val component = (itemBuilder.equippable ?: vanillaComponent).takeIf { it.model != modelKey } ?: return@forEach
            if (itemBuilder.nexoMeta?.generateModel != false) component.model = modelKey
            component.slot = slot
            if (component == itemBuilder.equippable) return@forEach
            itemBuilder.setEquippableComponent(component)
            itemBuilder.save()
            Logs.logWarn("Item $itemId does not have an equippable-component set.")
            Logs.logInfo("Configured Components.equippable.model to $modelKey for $itemId")
        }
    }

    private fun slotFromItem(itemId: String): EquipmentSlot? {
        return when (itemId.substringAfterLast("_").uppercase()) {
            "HELMET" -> EquipmentSlot.HEAD
            "CHESTPLATE" -> EquipmentSlot.CHEST
            "LEGGINGS" -> EquipmentSlot.LEGS
            "BOOTS" -> EquipmentSlot.FEET
            else -> null
        }
    }

    private fun armorPrefix(texture: Texture): String {
        val textureKey = texture.key().value()
        return when {
            textureKey.endsWith("_armor_layer_1.png") -> textureKey.substringBefore("_armor_layer_1.png")
            textureKey.endsWith("_armor_layer_2.png") -> textureKey.substringBefore("_armor_layer_2.png")
            else -> ""
        }.substringAfterLast("/")
    }
}
