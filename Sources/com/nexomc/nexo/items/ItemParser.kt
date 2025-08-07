package com.nexomc.nexo.items

import com.jeff_media.morepersistentdatatypes.DataType
import com.nexomc.nexo.api.NexoItems
import com.nexomc.nexo.commands.toColor
import com.nexomc.nexo.compatibilities.mmoitems.WrappedMMOItem
import com.nexomc.nexo.compatibilities.mythiccrucible.WrappedCrucibleItem
import com.nexomc.nexo.configs.Settings
import com.nexomc.nexo.mechanics.MechanicsManager
import com.nexomc.nexo.mechanics.trident.TridentFactory
import com.nexomc.nexo.utils.AdventureUtils.setDefaultStyle
import com.nexomc.nexo.utils.NexoYaml.Companion.copyConfigurationSection
import com.nexomc.nexo.utils.PotionUtils
import com.nexomc.nexo.utils.VersionUtil
import com.nexomc.nexo.utils.childSections
import com.nexomc.nexo.utils.deserialize
import com.nexomc.nexo.utils.getKey
import com.nexomc.nexo.utils.getLinkedMapList
import com.nexomc.nexo.utils.getStringListOrNull
import com.nexomc.nexo.utils.getStringOrNull
import com.nexomc.nexo.utils.logs.Logs
import com.nexomc.nexo.utils.printOnFailure
import com.nexomc.nexo.utils.safeCast
import com.nexomc.nexo.utils.toMap
import com.nexomc.nexo.utils.wrappers.AttributeWrapper.fromString
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.attribute.AttributeModifier
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.enchantments.EnchantmentWrapper
import org.bukkit.inventory.ItemFlag
import org.bukkit.persistence.PersistentDataType
import org.bukkit.potion.PotionEffect
import java.util.*

class ItemParser(private val section: ConfigurationSection) {
    private val nexoMeta: NexoMeta
    private val itemId: String = section.name
    private val type: Material
    private var mmoItem: WrappedMMOItem? = null
    private var crucibleItem: WrappedCrucibleItem? = null
    private var templateItem: ItemParser? = null
    var isConfigUpdated = false
        private set

    init {

        templateItem = section.getStringOrNull("template")?.let(ItemTemplate::parserTemplate)
            ?: section.getStringListOrNull("templates")?.let(ItemTemplate::parserTemplates)

        section.getConfigurationSection("crucible")?.also { crucibleItem = WrappedCrucibleItem(it) }
            ?: section.getConfigurationSection("mmoitem")?.also { mmoItem = WrappedMMOItem(it) }
            ?: (section.getStringOrNull("crucible_id") ?: section.getStringOrNull("crucible"))?.also { crucibleItem = WrappedCrucibleItem(it) }

        type = section.getString("material")?.let { material ->
            Material.matchMaterial(material).also {
                if (it == null) Logs.logWarn("$itemId is using invalid material $material, defaulting to PAPER...")
            }
        } ?: templateItem?.type ?: Material.PAPER

        nexoMeta = templateItem?.nexoMeta?.copy() ?: NexoMeta()
        mergeWithTemplateSection().getConfigurationSection("Pack")?.also {
            nexoMeta.packInfo(it)
            nexoMeta.customModelData?.also { CUSTOM_MODEL_DATAS_BY_ID[itemId] = CustomModelData(type, nexoMeta, it) }
        }
    }

    fun buildItem(): ItemBuilder {
        val item = crucibleItem?.let(::ItemBuilder) ?: mmoItem?.let(::ItemBuilder) ?: ItemBuilder(type)
        return applyConfig(templateItem?.applyConfig(item) ?: item)
    }

    private fun applyConfig(item: ItemBuilder): ItemBuilder {
        (section.getStringOrNull("itemname") ?: section.getString("displayname"))?.deserialize()?.let {
            if (VersionUtil.atleast("1.20.5")) {
                if ("displayname" in section) isConfigUpdated = true
                item.itemName(it)
            } else item.displayName(it)
        }

        section.getStringOrNull("customname")?.deserialize()?.let { customName ->
            if (VersionUtil.below("1.20.5")) isConfigUpdated = true
            item.displayName(customName)
        }

        section.getStringListOrNull("lore")?.map { it.deserialize().setDefaultStyle() }?.let(item::lore)
        section.getStringOrNull("color")?.toColor()?.let(item::setColor)
        section.getKey("trim_pattern")?.let(item::setTrimPattern)
        if ("unbreakable" in section) item.setUnbreakable(section.getBoolean("unbreakable", false))

        ComponentParser(section, item).parseComponents()
        parseMiscOptions(item)
        parseVanillaSections(item)
        parseNexoSection(item)
        item.nexoMeta(nexoMeta)
        return item
    }

    private fun parseMiscOptions(item: ItemBuilder) {
        if (section.getBoolean("injectId", true)) item.customTag(NexoItems.ITEM_ID, PersistentDataType.STRING, itemId)

        nexoMeta.noUpdate = section.getBoolean("no_auto_update", false)
        nexoMeta.disableEnchanting = section.getBoolean("disable_enchanting", false)
        nexoMeta.excludedFromInventory = section.getBoolean("excludeFromInventory", false)
        nexoMeta.excludedFromCommands = section.getBoolean("excludeFromCommands", false)
    }

    @Suppress("DEPRECATION")
    private fun parseVanillaSections(item: ItemBuilder) {
        val section = mergeWithTemplateSection()

        item.addItemFlags(*section.getStringList("ItemFlags").mapNotNull { runCatching { ItemFlag.valueOf(it) }.getOrNull() }.toTypedArray())

        section.getLinkedMapList("PotionEffects").forEach {
            PotionUtils.getEffectType(it["type"].safeCast())?.also { v -> it["effect"] = v.key.key }
            item.addPotionEffect(PotionEffect(it))
        }

        runCatching {
            val persistentData = section.getLinkedMapList("PersistentData", listOfNotNull(section.getConfigurationSection("PersistentData")?.toMap()?.toMap(linkedMapOf())))
            persistentData.forEach { attributeJson ->
                val key = NamespacedKey.fromString(attributeJson["key"] as String)!!

                // Resolve the PersistentDataType using reflection or a registry
                val dataTypeField = DataType::class.java.getDeclaredField(attributeJson["type"] as String)
                val dataType = dataTypeField.get(null).safeCast<PersistentDataType<Any, Any>>() ?: return@forEach
                val value = attributeJson["value"] ?: return@forEach

                item.customTag(key, dataType, value)
            }

        }.printOnFailure(true)

        section.getLinkedMapList("AttributeModifiers").forEach { attributes ->
            attributes.putIfAbsent("uuid", UUID.randomUUID().toString())
            attributes.putIfAbsent("name", "nexo:modifier")
            attributes.putIfAbsent("key", "nexo:modifier")
            val attribute = fromString((attributes["attribute"] as String)) ?: return@forEach
            item.addAttributeModifiers(attribute, AttributeModifier.deserialize(attributes))
        }

        section.getConfigurationSection("Enchantments")?.getKeys(false)?.forEach { enchant: String ->
            item.addEnchant(
                EnchantmentWrapper.getByKey(NamespacedKey.minecraft(enchant)) ?: return@forEach,
                section.getConfigurationSection("Enchantments")!!.getInt(enchant)
            )
        }
    }

    private fun parseNexoSection(item: ItemBuilder) {
        val mechanicsSection = mergeWithTemplateSection().getConfigurationSection("Mechanics")

        // Add trident mechanic by default if type is trident
        if (item.type == Material.TRIDENT) TridentFactory.instance()?.parse(section)

        mechanicsSection?.childSections()?.forEach { factoryId, section ->
            val mechanic = MechanicsManager.mechanicFactory(factoryId)?.parse(section) ?: return@forEach
            for (modifier in mechanic.itemModifiers) modifier.apply(item)
        }

        if (!nexoMeta.containsPackInfo) return
        val customModelData = CUSTOM_MODEL_DATAS_BY_ID[section.name]?.customModelData
            ?: nexoMeta.takeIf { !item.hasItemModel() && !item.hasCustomModelDataComponent() }?.model?.let {
                CustomModelData.generateId(it, type).also { cmd ->
                    isConfigUpdated = true
                    if (!Settings.DISABLE_AUTOMATIC_MODEL_DATA.toBool())
                        section.getConfigurationSection("Pack")?.set("custom_model_data", cmd)
                }
            } ?: return

        item.customModelData(customModelData)
        nexoMeta.customModelData = customModelData
    }

    private fun mergeWithTemplateSection(): ConfigurationSection {
        val templateSection = templateItem?.section ?: return section

        return YamlConfiguration().createSection(section.name).also {
            copyConfigurationSection(templateSection, it)
            copyConfigurationSection(section, it)
        }
    }

    companion object {
        val CUSTOM_MODEL_DATAS_BY_ID = mutableMapOf<String, CustomModelData>()
    }
}
