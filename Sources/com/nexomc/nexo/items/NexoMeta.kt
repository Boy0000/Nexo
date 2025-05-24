package com.nexomc.nexo.items

import com.nexomc.nexo.utils.KeyUtils
import com.nexomc.nexo.utils.KeyUtils.dropExtension
import com.nexomc.nexo.utils.appendIfMissing
import com.nexomc.nexo.utils.customarmor.CustomArmorType
import com.nexomc.nexo.utils.getKey
import com.nexomc.nexo.utils.getStringListOrNull
import com.nexomc.nexo.utils.printOnFailure
import com.nexomc.nexo.utils.rootId
import net.kyori.adventure.key.Key
import org.bukkit.Material
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.EntityType
import org.bukkit.inventory.EquipmentSlot
import team.unnamed.creative.model.Model
import team.unnamed.creative.model.ModelTexture
import team.unnamed.creative.model.ModelTextures

data class NexoMeta(
    var customModelData: Int? = null,
    var model: Key? = null,
    var parentModel: Key = Model.ITEM_GENERATED,
    var dyeableModel: Key? = null,
    var brokenModel: Key? = null,
    var blockingModel: Key? = null,
    var pullingModels: List<Key> = listOf(),
    var chargedModel: Key? = null,
    var fireworkModel: Key? = null,
    var castModel: Key? = null,
    var damagedModels: List<Key> = listOf(),
    var textureLayers: List<ModelTexture> = listOf(),
    var textureVariables: Map<String, ModelTexture> = mapOf(),
    var modelTextures: ModelTextures? = null,
    var containsPackInfo: Boolean = false,
    var excludedFromInventory: Boolean = false,
    var excludedFromCommands: Boolean = false,
    var noUpdate: Boolean = false,
    var disableEnchanting: Boolean = false,
    var generateModel: Boolean = false,
    var customArmorTextures: CustomArmorTextures? = null,
) {

    data class CustomArmorTextures(
        val layer1: Key?,
        val layer2: Key?,
        val elytra: Key?,
        val wolfArmor: Key?,
        val llamaArmor: Key?,
        val horseArmor: Key?,
        val camelSaddle: Key?,
        val donkeySaddle: Key?,
        val horseSaddle: Key?,
        val muleSaddle: Key?,
        val pigSaddle: Key?,
        val skeletonHorseSaddle: Key?,
        val striderSaddle: Key?,
        val zombieHorseSaddle: Key?,
        val harness: Key?,
    ) {

        constructor(armorPrefix: String) : this(
            Key.key("${armorPrefix}_armor_layer_1.png"),
            Key.key("${armorPrefix}_armor_layer_2.png"),
            Key.key("${armorPrefix}_elytra.png"),
            Key.key("${armorPrefix}_wolf_armor.png"),
            Key.key("${armorPrefix}_llama_armor.png"),
            Key.key("${armorPrefix}_horse_armor.png"),
            Key.key("${armorPrefix}_camel_saddle.png"),
            Key.key("${armorPrefix}_donkey_saddle.png"),
            Key.key("${armorPrefix}_horse_saddle.png"),
            Key.key("${armorPrefix}_mule_saddle.png"),
            Key.key("${armorPrefix}_pig_saddle.png"),
            Key.key("${armorPrefix}_skeleton_horse_saddle.png"),
            Key.key("${armorPrefix}_strider_saddle.png"),
            Key.key("${armorPrefix}_zombie_horse_saddle.png"),
            Key.key("${armorPrefix}_harness.png"),
        )

        constructor(section: ConfigurationSection) : this(
            Key.key(section.getString("layer1", "")?.appendIfMissing(".png")!!),
            Key.key(section.getString("layer2", "")?.appendIfMissing(".png")!!),
            Key.key(section.getString("elytra", "")?.appendIfMissing(".png")!!),
            Key.key(section.getString("wolf_armor", "")?.appendIfMissing(".png")!!),
            Key.key(section.getString("llama_armor", "")?.appendIfMissing(".png")!!),
            Key.key(section.getString("horse_armor", "")?.appendIfMissing(".png")!!),
            Key.key(section.getString("camel_saddle", "")?.appendIfMissing(".png")!!),
            Key.key(section.getString("donkey_saddle", "")?.appendIfMissing(".png")!!),
            Key.key(section.getString("horse_saddle", "")?.appendIfMissing(".png")!!),
            Key.key(section.getString("mule_saddle", "")?.appendIfMissing(".png")!!),
            Key.key(section.getString("pig_saddle", "")?.appendIfMissing(".png")!!),
            Key.key(section.getString("skeleton_horse_saddle", "")?.appendIfMissing(".png")!!),
            Key.key(section.getString("strider_saddle", "")?.appendIfMissing(".png")!!),
            Key.key(section.getString("zombie_horse_saddle", "")?.appendIfMissing(".png")!!),
            Key.key(section.getString("harness", "")?.appendIfMissing(".png")!!),
        )

        fun fromItem(item: ItemBuilder, itemId: String): Key? {
            val allowedEntities = item.equippable?.allowedEntities ?: listOf()
            val slot = item.equippable?.slot
            return when {
                itemId.endsWith("_elytra") || item.type == Material.ELYTRA || item.isGlider == true -> elytra
                slot == EquipmentSlot.BODY -> when {
                    item.type == Material.WOLF_ARMOR || itemId.endsWith("_wolf_armor") || EntityType.WOLF in allowedEntities -> wolfArmor
                    itemId.endsWith("_llama_armor") || EntityType.LLAMA in allowedEntities -> llamaArmor
                    itemId.endsWith("_horse_armor") || EntityType.HORSE in allowedEntities -> horseArmor
                    else -> null
                }

                item.type == Material.SADDLE -> when {
                    itemId.endsWith("_camel_saddle") || EntityType.CAMEL in allowedEntities -> camelSaddle
                    itemId.endsWith("_donkey_saddle") || EntityType.DONKEY in allowedEntities -> donkeySaddle
                    itemId.endsWith("_horse_saddle") || EntityType.HORSE in allowedEntities -> horseSaddle
                    itemId.endsWith("_mule_saddle") || EntityType.MULE in allowedEntities -> muleSaddle
                    itemId.endsWith("_pig_saddle") || EntityType.PIG in allowedEntities -> pigSaddle
                    itemId.endsWith("_skeleton_horse_saddle") || EntityType.SKELETON_HORSE in allowedEntities -> skeletonHorseSaddle
                    itemId.endsWith("_strider_saddle") || EntityType.STRIDER in allowedEntities -> striderSaddle
                    itemId.endsWith("_zombie_horse_saddle") || EntityType.ZOMBIE_HORSE in allowedEntities -> zombieHorseSaddle
                    else -> null
                }

                item.type.name == "HAPPY_GHAST_HARNESS" && (itemId.endsWith("_harness") || allowedEntities.any { it.name == "HAPPY_GHAST" }) -> harness

                slot == EquipmentSlot.HEAD || slot == EquipmentSlot.CHEST -> layer1
                slot == EquipmentSlot.LEGS || slot == EquipmentSlot.FEET -> layer2
                else -> null
            }
        }
    }

    fun packInfo(packSection: ConfigurationSection) {
        this.containsPackInfo = true
        this.parentModel = packSection.getKey("parent_model", Model.ITEM_GENERATED)
        this.model = parseModelKey(packSection, "model")
        this.dyeableModel = parseModelKey(packSection, "dyeable_model") ?: packSection.getKey("dyeable_texture")?.dropExtension()
        this.brokenModel = parseModelKey(packSection, "broken_model") ?: packSection.getKey("broken_texture")?.dropExtension()
        this.blockingModel = parseModelKey(packSection, "blocking_model") ?: packSection.getKey("blocking_texture")?.dropExtension()
        this.castModel = parseModelKey(packSection, "cast_model") ?: packSection.getKey("cast_texture")?.dropExtension()
        this.chargedModel = parseModelKey(packSection, "charged_model") ?: packSection.getKey("charged_texture")?.dropExtension()
        this.fireworkModel = parseModelKey(packSection, "firework_model") ?: packSection.getKey("firework_texture")?.dropExtension()
        this.pullingModels = (packSection.getStringListOrNull("pulling_models") ?: packSection.getStringList("pulling_textures")).map(KeyUtils::dropExtension)
        this.damagedModels = (packSection.getStringListOrNull("damaged_models") ?: packSection.getStringList("damaged_textures")).map(KeyUtils::dropExtension)

        val textureSection = packSection.getConfigurationSection("textures")
        when {
            textureSection != null ->
                this.textureVariables = textureSection.getKeys(false).associateWith { ModelTexture.ofKey(textureSection.getKey(it)) }

            packSection.isList("textures") ->
                this.textureLayers = packSection.getStringList("textures").map(KeyUtils::dropExtension).map(ModelTexture::ofKey)

            packSection.isString("textures") ->
                this.textureLayers = listOf(ModelTexture.ofKey(packSection.getKey("textures")?.dropExtension() ?: KeyUtils.MALFORMED_KEY_PLACEHOLDER))

            packSection.isString("texture") ->
                this.textureLayers = listOf(ModelTexture.ofKey(packSection.getKey("texture")?.dropExtension() ?: KeyUtils.MALFORMED_KEY_PLACEHOLDER))
        }

        this.modelTextures = ModelTextures.builder()
            .particle(textureVariables["particle"] ?: textureLayers.firstOrNull())
            .variables(textureVariables)
            .layers(textureLayers)
            .build()

        this.generateModel = packSection.getString("model") == null && (textureLayers.isNotEmpty() || textureVariables.isNotEmpty())
        this.customModelData = packSection.getInt("custom_model_data").takeUnless { it == 0 }
        this.customArmorTextures = runCatching { packSection.getConfigurationSection("CustomArmor")?.let(::CustomArmorTextures) }.printOnFailure().getOrNull() ?: let {
            if (!generateModel) return@let null
            val itemId = packSection.parent!!.name
            itemId.replace(CustomArmorType.itemIdRegex, "$1").takeUnless { it == itemId || it.isBlank() }?.let(::CustomArmorTextures)
        }
    }

    private fun parseModelKey(configSection: ConfigurationSection, configString: String): Key? {
        val modelName = configSection.getString(configString)
        val parent = configSection.rootId.lowercase().replace(" ", "_")

        return when {
            modelName == null && configString == "model" && Key.parseable(parent) -> Key.key(parent)
            Key.parseable(modelName) -> dropExtension(modelName!!)
            //else -> KeyUtils.MALFORMED_KEY_PLACEHOLDER
            else -> null
        }
    }

    //fun model(): Key = runCatching { modelKey }.getOrDefault(KeyUtils.MALFORMED_KEY_PLACEHOLDER)

    fun hasBlockingModel() = blockingModel?.value()?.isNotEmpty() == true

    fun hasCastModel() = castModel?.value()?.isNotEmpty() == true

    fun hasChargedModel() = chargedModel?.value()?.isNotEmpty() == true

    fun hasFireworkModel() = fireworkModel?.value()?.isNotEmpty() == true
}

