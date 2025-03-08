package com.nexomc.nexo.pack

import com.google.gson.JsonPrimitive
import com.nexomc.nexo.NexoPlugin
import com.nexomc.nexo.configs.Settings
import com.nexomc.nexo.utils.FileUtils
import com.nexomc.nexo.utils.JsonBuilder.array
import com.nexomc.nexo.utils.JsonBuilder.`object`
import com.nexomc.nexo.utils.JsonBuilder.plus
import com.nexomc.nexo.utils.JsonBuilder.toJsonArray
import com.nexomc.nexo.utils.JsonObject
import com.nexomc.nexo.utils.KeyUtils.appendSuffix
import com.nexomc.nexo.utils.KeyUtils.removeSuffix
import com.nexomc.nexo.utils.associateFastWith
import com.nexomc.nexo.utils.ensureCast
import com.nexomc.nexo.utils.filterFast
import com.nexomc.nexo.utils.flatMapFast
import com.nexomc.nexo.utils.logs.Logs
import com.nexomc.nexo.utils.mapNotNullFast
import com.nexomc.nexo.utils.plusFast
import com.nexomc.nexo.utils.printOnFailure
import com.nexomc.nexo.utils.resolve
import com.nexomc.nexo.utils.toJsonObject
import com.nexomc.nexo.utils.toWritable
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet
import java.io.File
import java.util.UUID
import net.kyori.adventure.key.Key
import team.unnamed.creative.ResourcePack
import team.unnamed.creative.atlas.AtlasSource
import team.unnamed.creative.atlas.SingleAtlasSource
import team.unnamed.creative.blockstate.BlockState
import team.unnamed.creative.blockstate.MultiVariant
import team.unnamed.creative.blockstate.Selector
import team.unnamed.creative.blockstate.Variant
import team.unnamed.creative.font.BitMapFontProvider
import team.unnamed.creative.model.ItemOverride
import team.unnamed.creative.model.Model
import team.unnamed.creative.model.ModelTexture
import team.unnamed.creative.model.ModelTextures
import team.unnamed.creative.sound.Sound
import team.unnamed.creative.sound.SoundRegistry
import team.unnamed.creative.texture.Texture

class PackObfuscator(private val resourcePack: ResourcePack) {
    val obfuscationType: PackObfuscationType = Settings.PACK_OBFUSCATION_TYPE.toEnumOrGet(PackObfuscationType::class.java) {
        Logs.logError("Invalid PackObfuscation type: $it, defaulting to ${PackObfuscationType.SIMPLE}", true)
        Logs.logError("Valid options are: ${PackObfuscationType.entries.joinToString()}", true)
        PackObfuscationType.SIMPLE
    }
    private lateinit var obfCachedPack: File
    private val cache = Settings.PACK_CACHE_OBFUSCATION.toBool()

    private class ObfuscatedModel(val originalModel: Model, val obfuscatedModel: Model) {
        fun find(key: Key) = originalModel.takeIf { it.key() == key } ?: obfuscatedModel.takeIf { it.key() == key }
    }

    private class ObfuscatedTexture(
        var originalTexture: Texture,
        var obfuscatedTexture: Texture
    ) {

        init {
            originalTexture = originalTexture.toBuilder().key(originalTexture.key().appendSuffix(".png")).build()
            obfuscatedTexture = obfuscatedTexture.toBuilder().key(obfuscatedTexture.key().appendSuffix(".png")).build()
        }

        fun find(key: Key) = originalTexture.takeIf { it.key() == key } ?: obfuscatedTexture.takeIf { it.key() == key }
    }

    private class ObfuscatedSound(val originalSound: Sound, val obfuscatedSound: Sound) {
        fun find(key: Key) = originalSound.takeIf { it.key() == key } ?: obfuscatedSound.takeIf { it.key() == key }
    }

    val skippedKeys = ObjectOpenHashSet<Key>()
    private val obfuscatedModels = ObjectOpenHashSet<ObfuscatedModel>()
    private val obfuscatedTextures = ObjectOpenHashSet<ObfuscatedTexture>()
    private val obfuscatedSounds = ObjectOpenHashSet<ObfuscatedSound>()
    private val obfuscatedNamespaceCache = Object2ObjectOpenHashMap<String, String>()

    private fun ObjectOpenHashSet<ObfuscatedModel>.findObf(key: Key) = find { it.find(key) != null }?.obfuscatedModel
    private fun ObjectOpenHashSet<ObfuscatedTexture>.findObf(key: Key) = key.appendSuffix(".png").let { k -> firstOrNull { it.find(k) != null }?.obfuscatedTexture }
    private fun ObjectOpenHashSet<ObfuscatedSound>.findObf(key: Key) = find { it.find(key) != null }?.obfuscatedSound

    enum class PackObfuscationType {
        SIMPLE, FULL, NONE;

        val isNone: Boolean
            get() = this == NONE
    }

    fun obfuscatePack(hash: String): ResourcePack {
        if (obfuscationType.isNone) return resourcePack

        this.obfCachedPack = NexoPlugin.instance().dataFolder.resolve("pack", ".deobfCachedPacks", "$hash.zip")
        FileUtils.setHidden(obfCachedPack.apply { parentFile.mkdirs() }.parentFile.toPath())

        if (!cache || !obfCachedPack.exists()) {
            Logs.logInfo("Obfuscating NexoPack...")
            obfuscatedModels.clear()

            obfuscateModels()
            obfuscateFonts()
            obfuscateTextures()
            obfuscateSounds()

            if (cache) {
                PackGenerator.packWriter.writeToZipFile(obfCachedPack, resourcePack)
                Logs.logInfo("Caching obfuscated ResourcePack...")
            }
        }

        obfCachedPack.parentFile.listFiles()?.asSequence()
            ?.filter { it.name != obfCachedPack.name }
            ?.forEach { runCatching { it.deleteRecursively() } }

        return if (cache) PackGenerator.packReader.readFromZipFile(obfCachedPack) else resourcePack
    }

    private fun obfuscateModels() {
        resourcePack.models().filterNot { it.key().value().startsWith("equipment/") }.forEach(::obfuscateModel)

        // Remove the original model and add the obfuscated one
        // If the original was marked to be skipped, still use the obfuscated but change the model-key to keep obf textures...
        obfuscatedModels.forEach {
            resourcePack.removeModel(it.originalModel.key())
            if (it.originalModel.key() !in skippedKeys) it.obfuscatedModel.addTo(resourcePack)
            else it.obfuscatedModel.toBuilder().key(it.originalModel.key()).build().addTo(resourcePack)
        }

        obfuscateBlockStates()
        obfuscateItemModels()
    }

    private fun obfuscateItemModels() {

        fun obfuscateModelKey(jsonObject: JsonObject?) {
            jsonObject?.get("model")?.takeIf { it.isJsonPrimitive }?.asString?.let(Key::key)?.let { obfuscatedModels.findObf(it) }?.key()?.asString()?.let {
                jsonObject.plus("model", it)
            }
            jsonObject?.get("base")?.takeIf { it.isJsonPrimitive }?.asString?.let(Key::key)?.let { obfuscatedModels.findObf(it) }?.key()?.asString()?.let {
                jsonObject.plus("base", it)
            }
            jsonObject?.get("models")?.takeIf { it.isJsonArray }?.asJsonArray?.map { JsonPrimitive(obfuscatedModels.findObf(Key.key(it.asString))?.key()?.asString() ?: it.asString) }?.let {
                jsonObject.add("models", it.toJsonArray())
            }
        }

        fun obfuscateItemModel(obj: JsonObject) {
            val modelObj = obj.`object`("model") ?: return

            obfuscateModelKey(modelObj)
            obfuscateModelKey(obj.`object`("fallback"))

            modelObj.array("entries")?.forEach { it.asJsonObject?.let(::obfuscateItemModel) }
            modelObj.array("cases")?.forEach { it.asJsonObject?.let(::obfuscateItemModel) }

            modelObj.`object`("on_false")?.let { onFalse ->
                obfuscateModelKey(onFalse)
                onFalse.array("entries")?.forEach { it.asJsonObject?.let(::obfuscateItemModel) }
                onFalse.array("cases")?.forEach { it.asJsonObject?.let(::obfuscateItemModel) }
                obfuscateModelKey(onFalse.`object`("fallback"))
            }
            modelObj.`object`("on_true")?.let { onTrue ->
                onTrue.array("entries")?.forEach { it.asJsonObject?.let(::obfuscateItemModel) }
                onTrue.array("cases")?.forEach { it.asJsonObject?.let(::obfuscateItemModel) }
                obfuscateModelKey(onTrue.`object`("fallback"))
            }
        }

        resourcePack.unknownFiles().filterFast { it.key.startsWith("assets/minecraft/items/") }.forEach { (key, writable) ->
            runCatching {
                val itemModelObject = writable.toJsonObject() ?: return@forEach
                if (ModernVersionPatcher.isStandardItemModel(key, itemModelObject)) return@forEach
                obfuscateItemModel(itemModelObject)

                resourcePack.unknownFile(key, itemModelObject.toWritable())
            }.printOnFailure(true)
        }
    }

    private fun obfuscateBlockStates() {
        resourcePack.blockStates().filterNotNull().forEach { blockState ->
            val multiparts = blockState.multipart().map {
                Selector.of(it.condition(), MultiVariant.of(it.variant().variants().map { v -> v.obfuscateVariant() }))
            }

            val variants = blockState.variants().map {
                it.key to MultiVariant.of(it.value.variants().map { v -> v.obfuscateVariant() })
            }.toMap()

            BlockState.of(blockState.key(), variants, multiparts).addTo(resourcePack)
        }
    }

    private fun obfuscateFonts() {
        resourcePack.fonts().filterNotNull().map { font ->
            font.providers(font.providers().filterNotNull().map { provider ->
                when (provider) {
                    is BitMapFontProvider ->
                        provider.toBuilder().file(obfuscateFontTexture(provider)?.key() ?: provider.file()).build()
                    else -> provider
                }
            })
        }.forEach(resourcePack::font)
    }

    private fun obfuscateTextures() {
        obfuscatedTextures.forEach {
            resourcePack.removeTexture(it.originalTexture.key())
            resourcePack.texture(it.obfuscatedTexture)
            resourcePack.texture(it.originalTexture.key().emissiveKey())?.also { e ->
                resourcePack.removeTexture(e.key())
                resourcePack.texture(e.toBuilder().key(it.obfuscatedTexture.key().emissiveKey()).build())
            }
        }

        obfuscateAtlases()
    }

    private fun obfuscateAtlases() {
        resourcePack.atlases().map { atlas ->
            val obfSources = atlas.sources().filter { it !is SingleAtlasSource }.plus(
                atlas.sources().filterIsInstance<SingleAtlasSource>().map { s ->
                    obfuscatedTextures.findObf(s.resource())?.let {
                        AtlasSource.single(it.key().removeSuffix(".png"))
                    } ?: s
                }
            )

            atlas.toBuilder().sources(obfSources).build()
        }.forEach(resourcePack::atlas)
    }

    private fun obfuscateSounds() {
        resourcePack.sounds().map { sound ->
            if (sound.key() in DefaultResourcePackExtractor.vanillaSounds) return@map sound
            Sound.sound(sound.key().obfuscateKey(), sound.data()).also {
                obfuscatedSounds += ObfuscatedSound(sound, it)
            }
        }.forEach(resourcePack::sound)

        resourcePack.soundRegistries().map soundRegistries@{ soundRegistry ->
            SoundRegistry.soundRegistry(
                soundRegistry.namespace(),
                soundRegistry.sounds().map soundEvents@{ soundEvent ->
                    soundEvent.toBuilder().sounds(soundEvent.sounds().map soundEntries@{ soundEntry ->
                        obfuscatedSounds.findObf(soundEntry.key())?.let {
                            soundEntry.toBuilder().key(it.key()).build()
                        } ?: soundEntry
                    }).build()
                }).addTo(resourcePack)
        }

        obfuscatedSounds.forEach {
            resourcePack.removeSound(it.originalSound.key())
            it.obfuscatedSound.addTo(resourcePack)
        }
    }


    private fun obfuscateModel(model: Model) = obfuscatedModels.findObf(model.key())
        ?: model.obfuscateModelTextures()
            .obfuscateOverrides()
            .also { obfuscatedModels += ObfuscatedModel(model, it) }
            .obfuscateParentModel()
            .also { obfuscatedModels.removeIf { it.originalModel.key() == model.key() } }
            .also { obfuscatedModels += ObfuscatedModel(model, it) }

    private fun Model.obfuscateModelTextures(): Model {
        obfuscatedModels.findObf(key())?.let { return it }
        if (DefaultResourcePackExtractor.vanillaResourcePack.model(key()) != null) return this

        val layers = textures().layers().filter { it.key() != null }.map { modelTexture ->
            obfuscateModelTexture(modelTexture)?.key()?.removeSuffix(".png")?.let(ModelTexture::ofKey) ?: modelTexture
        }
        val variables = textures().variables().map { variable ->
            variable.key to (obfuscateModelTexture(variable.value)?.key()?.removeSuffix(".png")?.let(ModelTexture::ofKey) ?: variable.value)
        }.toMap()

        val particle = textures().particle()
            ?.let { p -> obfuscateModelTexture(p)?.key()?.removeSuffix(".png")?.let { ModelTexture.ofKey(it) } ?: p }
        val modelTextures = ModelTextures.builder().layers(layers).variables(variables).particle(particle).build()
        return this.toBuilder().textures(modelTextures).build()
    }

    private fun Variant.obfuscateVariant(): Variant {
        return Variant.builder()
            .model(obfuscatedModels.findObf(model())?.key() ?: model())
            .uvLock(uvLock()).weight(weight()).x(x()).y(y()).build()
    }

    private fun Model.obfuscateParentModel(): Model {
        val parent = parent() ?: return this
        return toBuilder().parent(
            obfuscatedModels.findObf(parent)?.key()
                ?: DefaultResourcePackExtractor.vanillaResourcePack.model(parent)?.let { return this }
                ?: resourcePack.takeUnless { parent == key() }?.model(parent)?.let(::obfuscateModel)?.key()
                ?: parent
        ).build()
    }

    private fun Model.obfuscateOverrides(): Model = obfuscatedModels.findObf(key())
        ?: toBuilder().overrides(overrides().filterNotNull().map { override ->
            val overrideKey = override.model()
            val modelKey = obfuscatedModels.findObf(overrideKey)?.key()
                ?: DefaultResourcePackExtractor.vanillaResourcePack.model(overrideKey)?.let { overrideKey }
                ?: resourcePack.takeUnless { overrideKey == this.key() }?.model(overrideKey)?.let(::obfuscateModel)?.key()
                ?: overrideKey

            return@map ItemOverride.of(modelKey, override.predicate())
        }
        ).key(key().takeUnless { DefaultResourcePackExtractor.vanillaResourcePack.model(it) != null }?.obfuscateKey() ?: key())
            .build()

    private fun Texture.obfuscate() =
        this.toBuilder().key(this.key().obfuscateKey().appendSuffix(".png")).build()
            .also { obfuscatedTextures += ObfuscatedTexture(this@obfuscate, it) }

    private fun obfuscateModelTexture(modelTexture: ModelTexture): Texture? {
        val keyPng = modelTexture.key()?.appendSuffix(".png") ?: return null
        return obfuscatedTextures.findObf(keyPng)
            ?: vanillaModelTextures[keyPng]
            ?: resourcePack.texture(keyPng)?.obfuscate()
    }

    private fun obfuscateFontTexture(provider: BitMapFontProvider): Texture? {
        val key = provider.file()
        return obfuscatedTextures.findObf(key)
            ?: DefaultResourcePackExtractor.vanillaResourcePack.texture(key)
            ?: resourcePack.texture(key)?.obfuscate()
    }

    private fun Key.obfuscateKey() = when (obfuscationType) {
        PackObfuscationType.NONE -> this
        PackObfuscationType.FULL -> Key.key(obfuscatedNamespaceCache.getOrPut(namespace()) {
            UUID.randomUUID().toString()
        }, UUID.randomUUID().toString())

        PackObfuscationType.SIMPLE -> Key.key(this.namespace(), UUID.randomUUID().toString())
    }

    private fun Key.emissiveKey() = removeSuffix(".png").appendSuffix("_e.png")

    private val vanillaModelTextures by lazy {
        resourcePack.models().filterFast { DefaultResourcePackExtractor.vanillaResourcePack.model(it.key()) != null }
            .plus(DefaultResourcePackExtractor.vanillaResourcePack.models())
            .distinctBy { it.key().asString() }
            .flatMapFast { it.textures().layers().plusFast(it.textures().variables().values).plus(it.textures().particle()) }
            .mapNotNullFast { it?.key()?.appendSuffix(".png") }
            .associateFastWith { resourcePack.texture(it) ?: DefaultResourcePackExtractor.vanillaResourcePack.texture(it) }
            .filterFast { it.value != null }.ensureCast<Object2ObjectOpenHashMap<Key, Texture>>()
    }


}
