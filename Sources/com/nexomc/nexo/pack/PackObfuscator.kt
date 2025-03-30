package com.nexomc.nexo.pack

import com.nexomc.nexo.NexoPlugin
import com.nexomc.nexo.api.NexoPack
import com.nexomc.nexo.configs.Settings
import com.nexomc.nexo.pack.creative.NexoPackReader
import com.nexomc.nexo.pack.creative.NexoPackWriter
import com.nexomc.nexo.utils.FileUtils
import com.nexomc.nexo.utils.KeyUtils.appendSuffix
import com.nexomc.nexo.utils.KeyUtils.removeSuffix
import com.nexomc.nexo.utils.filterFast
import com.nexomc.nexo.utils.flatMapFastNotNull
import com.nexomc.nexo.utils.logs.Logs
import com.nexomc.nexo.utils.mapNotNullFast
import com.nexomc.nexo.utils.resolve
import com.nexomc.nexo.utils.toFastMap
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
import team.unnamed.creative.item.CompositeItemModel
import team.unnamed.creative.item.ConditionItemModel
import team.unnamed.creative.item.Item
import team.unnamed.creative.item.ItemModel
import team.unnamed.creative.item.RangeDispatchItemModel
import team.unnamed.creative.item.ReferenceItemModel
import team.unnamed.creative.item.SelectItemModel
import team.unnamed.creative.item.SpecialItemModel
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

    private class ObfuscatedTexture(var originalTexture: Texture, var obfuscatedTexture: Texture) {

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

    fun obfuscatePack(hash: String) {
        if (obfuscationType.isNone) return

        this.obfCachedPack = NexoPlugin.instance().dataFolder.resolve("pack", ".deobfCachedPacks", "$hash.zip")
        FileUtils.setHidden(obfCachedPack.apply { parentFile.mkdirs() }.parentFile.toPath())

        obfCachedPack.parentFile.listFiles()?.asSequence()
            ?.filter { it.name != obfCachedPack.name }
            ?.forEach { runCatching { it.deleteRecursively() } }

        if (!cache || !obfCachedPack.exists()) {
            Logs.logInfo("Obfuscating NexoPack...")
            obfuscatedModels.clear()

            obfuscateModels()
            obfuscateFonts()
            obfuscateTextures()
            obfuscateSounds()

            if (cache) {
                NexoPackWriter.INSTANCE.writeToZipFile(obfCachedPack, resourcePack)
                Logs.logInfo("Caching obfuscated ResourcePack...")
            }
        }

        if (cache) NexoPack.overwritePack(resourcePack, NexoPackReader.INSTANCE.readFromZipFile(obfCachedPack))
    }

    private fun obfuscateModels() {
        resourcePack.models().forEach(::obfuscateModel)

        // Remove the original model and add the obfuscated one
        // If the original was marked to be skipped, still use the obfuscated but change the model-key to keep obf textures...
        obfuscatedModels.forEach {
            resourcePack.removeModel(it.originalModel.key())
            if (it.originalModel.key() !in skippedKeys) it.obfuscatedModel.addTo(resourcePack)
            else it.obfuscatedModel.toBuilder().key(it.originalModel.key()).build().addTo(resourcePack)
        }

        obfuscateBlockStates()
        obfuscateItems()
    }

    private fun obfuscateItems() {
        fun obfuscateItemModel(itemModel: ItemModel): ItemModel {
            return when (itemModel) {
                is CompositeItemModel -> ItemModel.composite(itemModel.models().map(::obfuscateItemModel))
                is RangeDispatchItemModel -> ItemModel.rangeDispatch(itemModel.property(), itemModel.scale(), itemModel.entries().map { RangeDispatchItemModel.Entry.entry(it.threshold(), obfuscateItemModel(it.model())) }, itemModel.fallback())
                is ConditionItemModel -> ItemModel.conditional(itemModel.condition(), obfuscateItemModel(itemModel.onTrue()), obfuscateItemModel(itemModel.onFalse()))
                is SelectItemModel -> ItemModel.select(itemModel.property(), itemModel.cases().map { SelectItemModel.Case._case(obfuscateItemModel(it.model()), it.`when`()) }, itemModel.fallback())
                is SpecialItemModel -> ItemModel.special(itemModel.render(), obfuscatedModels.findObf(itemModel.base())?.key() ?: itemModel.base())
                is ReferenceItemModel -> ItemModel.reference(obfuscatedModels.findObf(itemModel.model())?.key() ?: itemModel.model(), itemModel.tints())
                else -> itemModel
            }
        }

        resourcePack.items().toList().forEach { item ->
            Item.item(item.key(), obfuscateItemModel(item.model()), item.handAnimationOnSwap()).addTo(resourcePack)
        }
    }

    private fun obfuscateBlockStates() {
        resourcePack.blockStates().toList().forEach { blockState ->
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
        resourcePack.fonts().toList().forEach { font ->
            font.providers(font.providers().map { provider ->
                when (provider) {
                    is BitMapFontProvider -> provider.toBuilder().file(obfuscateFontTexture(provider)?.key() ?: provider.file()).build()
                    else -> provider
                }
            }).addTo(resourcePack)
        }
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
        resourcePack.atlases().toList().forEach { atlas ->
            val obfSources = atlas.sources().map { source ->
                val obfSource = (source as? SingleAtlasSource)?.resource()?.let { obfuscatedTextures.findObf(it) } ?: return@map source
                AtlasSource.single(obfSource.key().removeSuffix(".png"))
            }

            atlas.toBuilder().sources(obfSources).build().addTo(resourcePack)
        }
    }

    private fun obfuscateSounds() {
        resourcePack.sounds().toList().forEach { sound ->
            if (sound.key() in VanillaResourcePack.vanillaSounds) return@forEach
            Sound.sound(sound.key().obfuscateKey(), sound.data()).also {
                obfuscatedSounds += ObfuscatedSound(sound, it)
            }.addTo(resourcePack)
        }

        resourcePack.soundRegistries().toList().forEach soundRegistries@{ soundRegistry ->
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
        if (VanillaResourcePack.resourcePack.model(key()) != null) return this

        val layers = textures().layers().mapNotNullFast { modelTexture ->
            if (modelTexture.key() == null) return@mapNotNullFast null
            obfuscateModelTexture(modelTexture)?.key()?.removeSuffix(".png")?.let(ModelTexture::ofKey) ?: modelTexture
        }
        val variables = textures().variables().mapValues { variable ->
            obfuscateModelTexture(variable.value)?.key()?.removeSuffix(".png")?.let(ModelTexture::ofKey) ?: variable.value
        }

        val particle = textures().particle()?.let { p -> obfuscateModelTexture(p)?.key()?.removeSuffix(".png")?.let(ModelTexture::ofKey) ?: p }
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
                ?: VanillaResourcePack.resourcePack.model(parent)?.let { return this }
                ?: resourcePack.takeUnless { parent == key() }?.model(parent)?.let(::obfuscateModel)?.key()
                ?: parent
        ).build()
    }

    private fun Model.obfuscateOverrides(): Model = obfuscatedModels.findObf(key())
        ?: toBuilder().overrides(overrides().map { override ->
            val overrideKey = override.model()
            val modelKey = obfuscatedModels.findObf(overrideKey)?.key()
                ?: VanillaResourcePack.resourcePack.model(overrideKey)?.let { overrideKey }
                ?: resourcePack.takeUnless { overrideKey == this.key() }?.model(overrideKey)?.let(::obfuscateModel)?.key()
                ?: return@map override

            ItemOverride.of(modelKey, override.predicate())
        }).key(key().takeUnless { VanillaResourcePack.resourcePack.model(it) != null }?.obfuscateKey() ?: key()).build()

    private fun Texture.obfuscate() =
        this.toBuilder().key(this.key().obfuscateKey().appendSuffix(".png")).build()
            .also { obfuscatedTextures += ObfuscatedTexture(this@obfuscate, it) }

    private fun obfuscateModelTexture(modelTexture: ModelTexture): Texture? {
        val key = modelTexture.key()?.appendSuffix(".png") ?: return null
        return obfuscatedTextures.findObf(key) ?: vanillaModelTextures[key] ?: resourcePack.texture(key)?.obfuscate()
    }

    private fun obfuscateFontTexture(provider: BitMapFontProvider): Texture? {
        val key = provider.file().appendSuffix(".png")
        return obfuscatedTextures.findObf(key) ?: vanillaFontTextures[key] ?: resourcePack.texture(key)?.obfuscate()
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
        resourcePack.models().filterFast { VanillaResourcePack.resourcePack.model(it.key()) != null }
            .plus(VanillaResourcePack.resourcePack.models())
            .distinctBy { it.key().asString() }
            .flatMapFastNotNull { it.textures().layers() + listOfNotNull(it.textures().particle()) + it.textures().variables().values }
            .mapNotNullFast {
                val key = it.key()?.appendSuffix(".png") ?: return@mapNotNullFast null
                key to (resourcePack.texture(key) ?: VanillaResourcePack.resourcePack.texture(key) ?: return@mapNotNullFast null)
            }.toFastMap()
    }

    private val vanillaFontTextures by lazy {
        resourcePack.fonts().filterFast { VanillaResourcePack.resourcePack.font(it.key()) != null }
            .plus(VanillaResourcePack.resourcePack.fonts())
            .distinctBy { it.key().asString() }
            .flatMapFastNotNull { it.providers().mapNotNullFast { (it as? BitMapFontProvider)?.file()?.appendSuffix(".png") } }
            .mapNotNullFast { it to (resourcePack.texture(it) ?: VanillaResourcePack.resourcePack.texture(it) ?: return@mapNotNullFast null) }
            .toFastMap()
    }
}
