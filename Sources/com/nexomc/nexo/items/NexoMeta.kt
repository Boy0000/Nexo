package com.nexomc.nexo.items

import com.nexomc.nexo.utils.KeyUtils
import com.nexomc.nexo.utils.KeyUtils.dropExtension
import net.kyori.adventure.key.Key
import org.bukkit.configuration.ConfigurationSection
import team.unnamed.creative.model.ModelTexture
import team.unnamed.creative.model.ModelTextures
import java.util.function.Consumer

data class NexoMeta(
    var customModelData: Int? = null,
    var modelKey: Key? = null,
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
    var generateModel: Boolean = false
) {

    lateinit var parentModel: Key

    fun packInfo(section: ConfigurationSection) {
        this.containsPackInfo = true
        this.modelKey = readModelName(section, "model")
        this.blockingModel = readModelName(section, "blocking_model")
        this.castModel = readModelName(section, "cast_model")
        this.chargedModel = readModelName(section, "charged_model")
        this.fireworkModel = readModelName(section, "firework_model")
        this.pullingModels = section.getStringList("pulling_models").map(KeyUtils::dropExtension)
        this.damagedModels = section.getStringList("damaged_models").map(KeyUtils::dropExtension)

        // By adding the textures to pullingModels aswell,
        // we can use the same code for both pullingModels
        // and pullingTextures to add to the base-bow file predicates
        if (pullingModels.isEmpty()) pullingModels =
            section.getStringList("pulling_textures").map(KeyUtils::dropExtension)
        if (damagedModels.isEmpty()) damagedModels =
            section.getStringList("damaged_textures").map(KeyUtils::dropExtension)

        if (chargedModel == null) chargedModel = dropExtension(section.getString("charged_texture", "")!!)
        if (fireworkModel == null) fireworkModel = dropExtension(section.getString("firework_texture", "")!!)
        if (castModel == null) castModel = dropExtension(section.getString("cast_texture", "")!!)
        if (blockingModel == null) blockingModel = dropExtension(section.getString("blocking_texture", "")!!)

        val textureSection = section.getConfigurationSection("textures")
        when {
            textureSection != null -> {
                val texturesSection = checkNotNull(section.getConfigurationSection("textures"))
                val variables = HashMap<String, ModelTexture>()
                texturesSection.getKeys(false).forEach { key: String ->
                    variables[key] = ModelTexture.ofKey(dropExtension(texturesSection.getString(key)!!))
                }
                this.textureVariables = variables
            }

            section.isList("textures") -> this.textureLayers =
                section.getStringList("textures").map(KeyUtils::dropExtension).map(ModelTexture::ofKey)

            section.isString("textures") -> this.textureLayers =
                listOf(ModelTexture.ofKey(dropExtension(section.getString("textures", "")!!)))

            section.isString("texture") -> this.textureLayers =
                listOf(ModelTexture.ofKey(dropExtension(section.getString("texture", "")!!)))
        }

        this.modelTextures = ModelTextures.builder()
            .particle(textureVariables["particle"])
            .variables(textureVariables)
            .layers(textureLayers)
            .build()

        this.parentModel = Key.key(section.getString("parent_model", "item/generated")!!)
        this.generateModel = section.getString("model") == null

        this.customModelData = section.getInt("custom_model_data").takeUnless { it == 0 }
    }

    // this might not be a very good function name
    private fun readModelName(configSection: ConfigurationSection, configString: String): Key? {
        val modelName = configSection.getString(configString)
        val parent = configSection.parent!!.name

        return when {
            modelName == null && configString == "model" && Key.parseable(parent) -> Key.key(parent)
            Key.parseable(modelName) -> dropExtension(modelName!!)
            //else -> KeyUtils.MALFORMED_KEY_PLACEHOLDER
            else -> null
        }
    }

    fun hasBlockingModel() = blockingModel?.value()?.isNotEmpty() == true

    fun hasCastModel() = castModel?.value()?.isNotEmpty() == true

    fun hasChargedModel() = chargedModel?.value()?.isNotEmpty() == true

    fun hasFireworkModel() = fireworkModel?.value()?.isNotEmpty() == true
}

