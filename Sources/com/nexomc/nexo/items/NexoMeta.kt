package com.nexomc.nexo.items

import com.nexomc.nexo.utils.KeyUtils
import com.nexomc.nexo.utils.KeyUtils.dropExtension
import com.nexomc.nexo.utils.appendIfMissing
import com.nexomc.nexo.utils.customarmor.CustomArmorType
import com.nexomc.nexo.utils.getKey
import com.nexomc.nexo.utils.getStringListOrNull
import com.nexomc.nexo.utils.printOnFailure
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

    data class CustomArmorTextures(val layer1: Key?, val layer2: Key?, val elytra: Key?, val wolfArmor: Key?, val llamaArmor: Key?, val horseArmor: Key?) {

        constructor(armorPrefix: String) : this(
            Key.key("${armorPrefix}_armor_layer_1.png"),
            Key.key("${armorPrefix}_armor_layer_2.png"),
            Key.key("${armorPrefix}_elytra.png"),
            Key.key("${armorPrefix}_wolf_armor.png"),
            Key.key("${armorPrefix}_llama_armor.png"),
            Key.key("${armorPrefix}_horse_armor.png")
        )

        constructor(section: ConfigurationSection) : this(
            Key.key(section.getString("layer1", "")?.appendIfMissing(".png")!!),
            Key.key(section.getString("layer2", "")?.appendIfMissing(".png")!!),
            Key.key(section.getString("elytra", "")?.appendIfMissing(".png")!!),
            Key.key(section.getString("wolf_armor", "")?.appendIfMissing(".png")!!),
            Key.key(section.getString("llama_armor", "")?.appendIfMissing(".png")!!),
            Key.key(section.getString("horse_armor", "")?.appendIfMissing(".png")!!),
        )

        fun fromItem(item: ItemBuilder, itemId: String): Key? {
            return when {
                item.type == Material.ELYTRA || item.isGlider == true -> elytra
                item.equippable?.slot == EquipmentSlot.BODY -> when {
                    itemId.endsWith("_wolf_armor") || item.type == Material.WOLF_ARMOR || item.equippable?.allowedEntities?.contains(EntityType.WOLF) == true -> wolfArmor
                    itemId.endsWith("_llama_armor") || item.equippable?.allowedEntities?.contains(EntityType.LLAMA) == true -> llamaArmor
                    itemId.endsWith("_horse_armor") || item.equippable?.allowedEntities?.contains(EntityType.HORSE) == true -> horseArmor
                    else -> null
                }
                item.equippable?.slot == EquipmentSlot.HEAD || item.equippable?.slot == EquipmentSlot.CHEST -> layer1
                item.equippable?.slot == EquipmentSlot.LEGS || item.equippable?.slot == EquipmentSlot.FEET -> layer2
                else -> null
            }
        }
    }

    lateinit var parentModel: Key

    fun packInfo(packSection: ConfigurationSection) {
        this.containsPackInfo = true
        this.model = parseModelKey(packSection, "model")
        this.blockingModel = parseModelKey(packSection, "blocking_model")
        this.castModel = parseModelKey(packSection, "cast_model")
        this.chargedModel = parseModelKey(packSection, "charged_model")
        this.fireworkModel = parseModelKey(packSection, "firework_model")
        this.pullingModels = (packSection.getStringListOrNull("pulling_models") ?: packSection.getStringList("pulling_textures")).map(KeyUtils::dropExtension)
        this.damagedModels = (packSection.getStringListOrNull("damaged_models") ?: packSection.getStringList("damaged_textures")).map(KeyUtils::dropExtension)

        chargedModel = chargedModel ?: packSection.getKey("charged_texture")?.dropExtension()
        fireworkModel = fireworkModel ?: packSection.getKey("firework_texture")?.dropExtension()
        castModel = castModel ?: packSection.getKey("cast_texture")?.dropExtension()
        blockingModel = blockingModel ?: packSection.getKey("blocking_texture")?.dropExtension()

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

        this.customArmorTextures = runCatching { packSection.getConfigurationSection("CustomArmor")?.let(::CustomArmorTextures) }.printOnFailure().getOrNull() ?: let {
            val itemId = packSection.parent!!.name
            itemId.replace(CustomArmorType.itemIdRegex, "$1").takeUnless { it == itemId || it.isBlank() }?.let(::CustomArmorTextures)
        }

        this.parentModel = packSection.getKey("parent_model", Model.ITEM_GENERATED)
        this.generateModel = packSection.getString("model") == null && (textureLayers.isNotEmpty() || textureVariables.isNotEmpty())

        this.customModelData = packSection.getInt("custom_model_data").takeUnless { it == 0 }
    }

    private fun parseModelKey(configSection: ConfigurationSection, configString: String): Key? {
        val modelName = configSection.getString(configString)
        val parent = configSection.parent!!.name.lowercase().replace(" ", "_")

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

