package com.nexomc.nexo.items

import com.mineinabyss.idofront.util.toColor
import com.nexomc.nexo.api.NexoItems
import com.nexomc.nexo.compatibilities.ecoitems.WrappedEcoItem
import com.nexomc.nexo.compatibilities.mmoitems.WrappedMMOItem
import com.nexomc.nexo.compatibilities.mythiccrucible.WrappedCrucibleItem
import com.nexomc.nexo.configs.Settings
import com.nexomc.nexo.mechanics.MechanicsManager
import com.nexomc.nexo.utils.AdventureUtils
import com.nexomc.nexo.utils.AdventureUtils.setDefaultStyle
import com.nexomc.nexo.utils.NexoYaml.Companion.copyConfigurationSection
import com.nexomc.nexo.utils.PotionUtils.getEffectType
import com.nexomc.nexo.utils.VersionUtil
import com.nexomc.nexo.utils.logs.Logs
import com.nexomc.nexo.utils.printOnFailure
import com.nexomc.nexo.utils.safeCast
import com.nexomc.nexo.utils.wrappers.AttributeWrapper.fromString
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.format.TextDecoration
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
    private var ecoItem: WrappedEcoItem? = null
    private var templateItem: ItemParser? = null
    var isConfigUpdated = false
        private set

    init {

        if (section.isString("template")) templateItem = ItemTemplate.parserTemplate(section.getString("template")!!)

        val crucibleSection = section.getConfigurationSection("crucible")
        val mmoSection = section.getConfigurationSection("mmoitem")
        val ecoItemSection = section.getConfigurationSection("ecoitem")
        when {
            crucibleSection != null -> crucibleItem = WrappedCrucibleItem(crucibleSection)
            section.isString("crucible_id") -> crucibleItem = WrappedCrucibleItem(section.getString("crucible_id"))
            ecoItemSection != null -> ecoItem = WrappedEcoItem(ecoItemSection)
            section.isString("ecoitem_id") -> ecoItem = WrappedEcoItem(section.getString("ecoitem_id"))
            mmoSection != null -> mmoItem = WrappedMMOItem(mmoSection)
        }

        type = section.getString("material")?.let { material ->
            Material.matchMaterial(material).also {
                if (it == null) Logs.logWarn("$itemId is using invalid material $material, defaulting to PAPER...")
            }
        } ?: templateItem?.type ?: Material.PAPER

        nexoMeta = templateItem?.nexoMeta?.copy() ?: NexoMeta()
        mergeWithTemplateSection().getConfigurationSection("Pack")?.also {
            nexoMeta.packInfo(it)
            nexoMeta.customModelData?.let { CUSTOM_MODEL_DATAS_BY_ID[itemId] = CustomModelData(type, nexoMeta, it) }
        }
    }

    val usesMMOItems: Boolean get() {
        return crucibleItem == null && ecoItem == null && mmoItem != null && mmoItem?.build() != null
    }

    val usesCrucibleItems: Boolean get() {
        return mmoItem == null && ecoItem == null && crucibleItem != null && crucibleItem?.build() != null
    }

    val usesEcoItems: Boolean get() {
        return mmoItem == null && crucibleItem == null && ecoItem != null && ecoItem?.build() != null
    }

    val usesTemplate: Boolean get() {
        return templateItem != null
    }

    fun buildItem(): ItemBuilder {
        val item = crucibleItem?.let(::ItemBuilder) ?: mmoItem?.let(::ItemBuilder) ?: ecoItem?.let(::ItemBuilder) ?: ItemBuilder(type)
        return applyConfig(templateItem?.applyConfig(item) ?: item)
    }

    private fun applyConfig(item: ItemBuilder): ItemBuilder {
        section.getString("itemname", section.getString("displayname"))?.let(AdventureUtils.MINI_MESSAGE::deserialize)?.let {
            if (VersionUtil.atleast("1.20.5")) {
                if ("displayname" in section) isConfigUpdated = true
                item.itemName(it)
            } else item.displayName(it)
        }

        section.getString("customname")?.let(AdventureUtils.MINI_MESSAGE::deserialize)?.let { customName ->
            if (VersionUtil.below("1.20.5")) isConfigUpdated = true
            item.displayName(customName)
        }

        if ("lore" in section) item.lore(section.getStringList("lore").map { AdventureUtils.MINI_MESSAGE.deserialize(it).setDefaultStyle() })
        if ("unbreakable" in section) item.setUnbreakable(section.getBoolean("unbreakable", false))
        if ("color" in section) item.setColor(section.getString("color", "#FFFFFF")!!.toColor())
        if ("trim_pattern" in section) item.setTrimPattern(Key.key(section.getString("trim_pattern", "")!!))

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
        val section = mergeWithTemplateSection() ?: return

        if ("ItemFlags" in section) for (itemFlag: String in section.getStringList("ItemFlags"))
            item.addItemFlags(ItemFlag.valueOf(itemFlag))

        section.getList("PotionEffects")?.filterIsInstance<LinkedHashMap<String, Any>>()?.forEach { serializedEffect ->
            val effect = getEffectType(serializedEffect["type"] as? String) ?: return@forEach
            val duration = serializedEffect["duration"] as? Int ?: 60
            val amplifier = serializedEffect["amplifier"] as? Int ?: 0
            val ambient = serializedEffect["ambient"] as? Boolean ?: false
            val particles = serializedEffect["particles"] as? Boolean ?: true
            val icon = serializedEffect["icon"] as? Boolean ?: true
            item.addPotionEffect(PotionEffect(effect, duration, amplifier, ambient, particles, icon))
        }

        runCatching {
            section.getList("PersistentData")?.filterIsInstance<LinkedHashMap<String, Any>>()?.forEach { attributeJson ->
                val keyContent = (attributeJson["key"] as String).split(":")
                val persistentDataType = PersistentDataType::class.java.getDeclaredField(attributeJson["type"] as String)
                    .get(null).safeCast<PersistentDataType<Any, Any?>>() ?: return@forEach

                item.addCustomTag(
                    NamespacedKey(keyContent[0], keyContent[1]),
                    persistentDataType,
                    attributeJson["value"]
                )
            }
        }.printOnFailure(true)

        section.getList("AttributeModifiers")?.filterIsInstance<LinkedHashMap<String, Any>>()?.forEach { attributeJson ->
            attributeJson.putIfAbsent("uuid", UUID.randomUUID().toString())
            attributeJson.putIfAbsent("name", "nexo:modifier")
            attributeJson.putIfAbsent("key", "nexo:modifier")
            val attributeModifier = AttributeModifier.deserialize(attributeJson)
            val attribute = fromString((attributeJson["attribute"] as String)) ?: return@forEach
            item.addAttributeModifiers(attribute, attributeModifier)
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

        mechanicsSection?.getKeys(false)?.forEach { mechanicID: String ->
            val factory = MechanicsManager.getMechanicFactory(mechanicID) ?: return@forEach

            val mechanicSection = mechanicsSection.getConfigurationSection(mechanicID) ?: return@forEach
            val mechanic = factory.parse(mechanicSection) ?: return@forEach
            for (itemModifier in mechanic.itemModifiers) itemModifier.apply(item)
        }

        if (!nexoMeta.containsPackInfo) return
        val customModelData = when {
            section.name in CUSTOM_MODEL_DATAS_BY_ID -> CUSTOM_MODEL_DATAS_BY_ID[section.name]?.customModelData
            !item.hasItemModel() -> {
                CustomModelData.generateId(nexoMeta.modelKey!!, type).also {
                    isConfigUpdated = true
                    if (!Settings.DISABLE_AUTOMATIC_MODEL_DATA.toBool())
                        section.getConfigurationSection("Pack")?.set("custom_model_data", it)
                }
            }
            else -> null
        } ?: return

        item.customModelData(customModelData)
        nexoMeta.customModelData = customModelData
    }

    private fun mergeWithTemplateSection(): ConfigurationSection {
        if (templateItem == null) return section

        return YamlConfiguration().createSection(section.name).also {
            copyConfigurationSection(templateItem!!.section, it)
            copyConfigurationSection(section, it)
        }
    }

    companion object {
        val CUSTOM_MODEL_DATAS_BY_ID = mutableMapOf<String, CustomModelData>()
    }
}
