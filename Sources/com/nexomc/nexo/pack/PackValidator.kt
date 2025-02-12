package com.nexomc.nexo.pack

import com.nexomc.nexo.configs.Settings
import com.nexomc.nexo.fonts.Glyph
import com.nexomc.nexo.utils.*
import com.nexomc.nexo.utils.KeyUtils.appendSuffix
import com.nexomc.nexo.utils.KeyUtils.removeSuffix
import com.nexomc.nexo.utils.appendIfMissing
import com.nexomc.nexo.utils.logs.Logs
import net.kyori.adventure.key.Key
import team.unnamed.creative.ResourcePack
import team.unnamed.creative.atlas.Atlas
import team.unnamed.creative.atlas.PalettedPermutationsAtlasSource
import team.unnamed.creative.atlas.SingleAtlasSource
import team.unnamed.creative.font.BitMapFontProvider
import team.unnamed.creative.texture.Texture
import java.awt.Dimension
import javax.imageio.ImageIO
import javax.imageio.stream.MemoryCacheImageInputStream
import kotlin.math.max

class PackValidator(val resourcePack: ResourcePack) {
    private val requiredTexture = resourcePack.texture(Glyph.REQUIRED_GLYPH)

    private fun Key.appendPng() = Key.key(this.asString().appendIfMissing(".png"))
    val invalidTextures = mutableListOf<Key>()

    fun validatePack() {
        invalidTextures.clear()
        Logs.logInfo("Validating ResourcePack files...")
        val palettedPermutations = resourcePack.atlas(Atlas.BLOCKS)?.sources()?.filterFastIsInstance<PalettedPermutationsAtlasSource>()?.flatMapFast { source ->
            source.textures().mapFast { it.appendPng() }.flatMapFast textures@{ texture ->
                if (resourcePack.texture(texture) == null && DefaultResourcePackExtractor.vanillaResourcePack.texture(texture) == null) {
                    logMissingTexture("Atlas", Atlas.BLOCKS.key(), texture)
                    return@textures emptyList()
                }
                source.permutations().keys.map { permutation ->
                    texture.key().removeSuffix(".png").appendSuffix("_$permutation").appendPng()
                }
            }
        } ?: emptyList()

        if (Settings.PACK_VALIDATE_MODELS.toBool()) resourcePack.models().forEach { model ->
            model.textures().layers().forEach layers@{
                val key = it.key()?.appendPng() ?: return@layers
                if (key in palettedPermutations) return@layers
                if (DefaultResourcePackExtractor.vanillaResourcePack.texture(key) != null) return@layers
                resourcePack.texture(key)?.also { t -> validateTextureSize(t, 512) } ?: logMissingTexture("Model", model.key(), key)
            }

            model.textures().variables().entries.forEach variables@{
                val key = it.value.key()?.appendPng() ?: return@variables
                if (key in palettedPermutations) return@variables
                if (DefaultResourcePackExtractor.vanillaResourcePack.texture(key) != null) return@variables
                resourcePack.texture(key)?.also { t -> validateTextureSize(t, 512) } ?: logMissingTexture("Model", model.key(), key)
            }

            model.textures().particle()?.also {
                val key = it.key()?.appendPng() ?: return@also
                if (DefaultResourcePackExtractor.vanillaResourcePack.texture(key) != null) return@also
                if (key in palettedPermutations) return@also
                resourcePack.texture(key)?.also { t -> validateTextureSize(t, 512) } ?: logMissingTexture("Model", model.key(), key)
            }
        }

        if (Settings.PACK_VALIDATE_FONTS.toBool()) resourcePack.fonts().map { font ->
            font.providers().map providers@{ provider ->
                when (provider) {
                    is BitMapFontProvider -> {
                        val key = provider.file().appendPng()
                        if (key in palettedPermutations || provider.characters().size > 1) return@providers provider
                        if (DefaultResourcePackExtractor.vanillaResourcePack.texture(key) != null) return@providers provider
                        resourcePack.texture(key)?.also { validateTextureSize(it, 256) }?.let { return@providers provider }

                        logMissingTexture("Font", font.key(), key)
                        Logs.logWarn("It has been temporarily replaced with a placeholder-image to not break the pack")
                        provider.file(Glyph.REQUIRED_GLYPH)
                    }
                    else -> provider
                }
            }.let(font::providers)
        }.forEach { it.addTo(resourcePack) }

        if (Settings.PACK_VALIDATE_ATLAS.toBool()) resourcePack.atlas(Atlas.BLOCKS)?.let { atlas ->
            atlas.sources().filterIsInstance<SingleAtlasSource>().forEach { source ->
                val key = source.resource().appendPng()
                if (DefaultResourcePackExtractor.vanillaResourcePack.texture(key) != null) return@forEach
                if (key in palettedPermutations) return@forEach
                if (resourcePack.texture(key) != null) return@forEach
                logMissingTexture("Atlas", Atlas.BLOCKS.key(), key)
            }
        }
    }

    private val textureDimensionCache: MutableMap<Key, Dimension> = mutableMapOf()
    private fun Texture.dimensions(): Dimension? {
        if (this.key() in textureDimensionCache) return null

        MemoryCacheImageInputStream(data().toByteArray().inputStream()).use { input ->
            val readers = ImageIO.getImageReaders(input)
            if (readers.hasNext()) {
                val reader = readers.next()
                reader.input = input
                return Dimension(reader.getWidth(0), reader.getHeight(0)).also {
                    textureDimensionCache[this.key()] = it
                }
            }
        }
        return null
    }

    private val uvTextures by lazy {
        resourcePack.models().associateFast { it.textures() to it.elements() }.mapNotNull { (textures, elements) ->
            elements.flatMapFast { it.faces().values }.filterFast { it.uv() != null }.mapNotNull { face ->
                textures.variables()[face.texture().removePrefix("#")]?.key()
            }
        }.flatten().toSet()
    }

    private fun validateTextureSize(texture: Texture, maxResolution: Int) {
        runCatching {
            if (texture.key().removeSuffix(".png") in uvTextures) return
            if (texture.hasMetadata() || resourcePack.unknownFile("assets/${texture.key().namespace()}/textures/${texture.key().value()}.mcmeta") != null) return
            val dimensions = texture.dimensions() ?: return

            if (max(dimensions.width, dimensions.height) > maxResolution) {
                Logs.logError("Texture <#E24D47><i>${texture.key()}</i></#E24D47> is above allowed ${maxResolution}x$maxResolution resolution...")
                Logs.logWarn("It has been temporarily replaced with a placeholder-image to not break the pack")
                texture.toBuilder().data(requiredTexture?.data() ?: texture.data()).build()
            }
        }.printOnFailure(true)
    }

    private fun logMissingTexture(prefix: String, parentKey: Key, key: Key) {
        if (key in invalidTextures) return
        invalidTextures += key
        Logs.logError("$prefix <#E24D47><i>$parentKey</i></#E24D47> is trying to use texture <#E24D47><i>$key</i></#E24D47>, but it does not exist within Nexo's ResourcePacks")
    }
}