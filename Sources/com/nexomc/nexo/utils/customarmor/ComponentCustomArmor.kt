package com.nexomc.nexo.utils.customarmor

import com.destroystokyo.paper.MaterialSetTag
import com.nexomc.nexo.api.NexoItems
import com.nexomc.nexo.configs.Settings
import com.nexomc.nexo.items.ItemBuilder
import com.nexomc.nexo.items.NexoMeta
import com.nexomc.nexo.utils.JsonBuilder
import com.nexomc.nexo.utils.JsonBuilder.plus
import com.nexomc.nexo.utils.Quadruple
import com.nexomc.nexo.utils.logs.Logs
import com.nexomc.nexo.utils.toWritable
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet
import net.kyori.adventure.key.Key
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.EntityType
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import team.unnamed.creative.ResourcePack
import team.unnamed.creative.texture.Texture

class ComponentCustomArmor(private val resourcePack: ResourcePack) {
    fun generatePackFiles() {
        val armorPrefixes = NexoItems.entries().entries.mapNotNullTo(ObjectLinkedOpenHashSet()) { (itemId, builder) ->
            itemId.substringBeforeLast("_").takeUnless { builder.nexoMeta?.customArmorTextures == null }
        }
        writeArmorModels(armorPrefixes)
        parseNexoArmorItems(armorPrefixes)
        copyEquipmentTextures()
    }

    private fun writeArmorModels(armorPrefixes: Set<String>) {
        armorPrefixes.forEach { armorPrefix ->
            val armorModelArray = JsonBuilder.jsonArray.plus(JsonBuilder.jsonObject.plus("texture", "nexo:$armorPrefix"))
            val layers = JsonBuilder.jsonObject.plus("humanoid", armorModelArray).plus("humanoid_leggings", armorModelArray).plus("horse_body", armorModelArray)
            val wings = JsonBuilder.jsonObject.plus("wings", armorModelArray)
            val wolf = JsonBuilder.jsonObject.plus("wolf_body", armorModelArray)
            val llama = JsonBuilder.jsonObject.plus("llama_body", armorModelArray)
            val equipmentModel = JsonBuilder.jsonObject.plus("layers", layers)

            //1.21.3
            "assets/nexo/models/equipment/$armorPrefix.json".takeIf { it !in resourcePack.unknownFiles() }?.run {
                resourcePack.unknownFile(this, equipmentModel.toWritable())
            }
            "assets/nexo/models/equipment/${armorPrefix}_elytra.json".takeIf { it !in resourcePack.unknownFiles() }?.run {
                resourcePack.unknownFile(this, JsonBuilder.jsonObject.plus("layers", wings).toWritable())
            }
            "assets/nexo/models/equipment/${armorPrefix}_wolf.json".takeIf { it !in resourcePack.unknownFiles() }?.run {
                resourcePack.unknownFile(this, JsonBuilder.jsonObject.plus("layers", wolf).toWritable())
            }
            "assets/nexo/models/equipment/${armorPrefix}_llama.json".takeIf { it !in resourcePack.unknownFiles() }?.run {
                resourcePack.unknownFile(this, JsonBuilder.jsonObject.plus("layers", llama).toWritable())
            }

            //1.21.4+
            "assets/nexo/equipment/$armorPrefix.json".takeIf { it !in resourcePack.unknownFiles() }?.run {
                resourcePack.unknownFile(this, equipmentModel.toWritable())
            }
            "assets/nexo/equipment/${armorPrefix}_elytra.json".takeIf { it !in resourcePack.unknownFiles() }?.run {
                resourcePack.unknownFile(this, JsonBuilder.jsonObject.plus("layers", wings).toWritable())
            }
            "assets/nexo/equipment/${armorPrefix}_wolf.json".takeIf { it !in resourcePack.unknownFiles() }?.run {
                resourcePack.unknownFile(this, JsonBuilder.jsonObject.plus("layers", wolf).toWritable())
            }
            "assets/nexo/equipment/${armorPrefix}_llama.json".takeIf { it !in resourcePack.unknownFiles() }?.run {
                resourcePack.unknownFile(this, JsonBuilder.jsonObject.plus("layers", llama).toWritable())
            }
        }
    }

    private val Map.Entry<String, ItemBuilder>.armorProperties: Quadruple<String, String, ItemBuilder, NexoMeta.CustomArmorTextures>? get() =
        value.nexoMeta?.customArmorTextures?.let { Quadruple(key.substringBeforeLast("_"), key, value, it) }

    private fun copyEquipmentTextures() {
        NexoItems.entries().entries.mapNotNull { it.armorProperties }.distinctBy { it.fourth.fromItem(it.third) }.forEach { (prefix, itemId, item, customArmor) ->
            val customArmorKey = customArmor.fromItem(item) ?: return@forEach
            val (fallback, path) = when (customArmorKey) {
                customArmor.layer1 -> "armor_layer_1" to "humanoid"
                customArmor.layer2 -> "armor_layer_2" to "humanoid_leggings"
                customArmor.elytra -> "elytra" to "wings"
                customArmor.wolf -> "wolf" to "wolf_body"
                customArmor.llama -> "llama" to "llama_body"
                customArmor.horse -> "horse" to "horse_body"
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
            val armorPrefix = itemId.substringBeforeLast("_").takeIf(armorPrefixes::contains) ?: return@forEach
            val armorSuffix = when {
                item.type == Material.ELYTRA || item.isGlider == true -> "_elytra"
                item.equippable?.slot == EquipmentSlot.BODY -> when {
                    item.type == Material.WOLF_ARMOR || item.equippable?.allowedEntities?.contains(EntityType.WOLF) == true -> "_wolf"
                    MaterialSetTag.WOOL_CARPETS.isTagged(item.type) || item.equippable?.allowedEntities?.contains(EntityType.LLAMA) == true -> "_llama"
                    item.equippable?.allowedEntities?.contains(EntityType.HORSE) == true -> "_horse"
                    else -> ""
                }
                else -> ""
            }
            val slot = slotFromItem(itemId) ?: return@forEach

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

    private fun slotFromItem(itemId: String): EquipmentSlot? {
        return when (itemId.substringAfterLast("_").uppercase()) {
            "HELMET" -> EquipmentSlot.HEAD
            "CHESTPLATE", "ELYTRA" -> EquipmentSlot.CHEST
            "LEGGINGS" -> EquipmentSlot.LEGS
            "BOOTS" -> EquipmentSlot.FEET
            "WOLF", "LLAMA", "HORSE" -> EquipmentSlot.BODY
            else -> null
        }
    }
}
