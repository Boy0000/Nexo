package com.nexomc.nexo.utils.customarmor

import com.nexomc.nexo.NexoPlugin
import com.nexomc.nexo.api.NexoItems
import com.nexomc.nexo.configs.Settings
import com.nexomc.nexo.utils.logs.Logs
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.nexomc.nexo.utils.VersionUtil
import net.kyori.adventure.key.Key
import org.bukkit.NamespacedKey
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import team.unnamed.creative.ResourcePack
import team.unnamed.creative.base.Writable
import team.unnamed.creative.texture.Texture

object ComponentCustomArmor {
    fun generatePackFiles(resourcePack: ResourcePack) {
        val armorPrefixes = armorPrefixes(resourcePack)
        writeArmorModels(resourcePack, armorPrefixes)
        copyArmorLayerTextures(resourcePack)
        parseNexoArmorItems(armorPrefixes)
    }

    private fun writeArmorModels(resourcePack: ResourcePack, armorPrefixes: Set<String?>) {
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
        listOf(resourcePack.textures()).flatten().forEach { texture ->
            val armorFolder = if (texture.key().asString().endsWith("_armor_layer_1.png")) "humanoid" else "humanoid_leggings"
            val armorPrefix = armorPrefix(texture)
            if (armorPrefix.isEmpty()) return@forEach

            resourcePack.removeTexture(texture.key())
            resourcePack.texture(Key.key("nexo:entity/equipment/$armorFolder/$armorPrefix.png"), texture.data())
        }
    }

    private fun parseNexoArmorItems(armorPrefixes: Set<String>) {
        NexoItems.entries().forEach { (itemId, itemBuilder) ->
            val armorPrefix = itemId.substringBeforeLast("_").takeIf(armorPrefixes::contains) ?: return@forEach
            val slot = slotFromItem(itemId) ?: return@forEach

            if (!Settings.CUSTOM_ARMOR_ASSIGN.toBool()) {
                Logs.logWarn("Item $itemId does not have an equippable-component configured properly.")
                Logs.logWarn("Nexo has been configured to use Components for custom-armor due to ${Settings.CUSTOM_ARMOR_TYPE.path} setting")
                Logs.logWarn("Custom Armor will not work unless an equippable-component is set.", true)
            } else {
                val modelKey = NamespacedKey.fromString(armorPrefix, NexoPlugin.instance())!!
                val component = (itemBuilder.equippable ?: ItemStack(itemBuilder.type).itemMeta.equippable).takeIf { it.model != modelKey } ?: return@forEach
                component.model = modelKey
                component.slot = slot
                itemBuilder.setEquippableComponent(component)

                itemBuilder.save()
                Logs.logWarn("Item $itemId does not have an equippable-component set.")
                Logs.logInfo("Configured Components.equippable.model to $modelKey for $itemId")
            }
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

    private fun armorPrefixes(resourcePack: ResourcePack) =
        LinkedHashSet(resourcePack.textures().map(::armorPrefix).filter(String::isNotBlank))

    private fun armorPrefix(texture: Texture): String {
        val textureKey = texture.key().value()
        return when {
            textureKey.endsWith("_armor_layer_1.png") -> textureKey.substringBefore("_armor_layer_1.png")
            textureKey.endsWith("_armor_layer_2.png") -> textureKey.substringBefore("_armor_layer_2.png")
            else -> ""
        }.substringAfterLast("/")
    }
}
