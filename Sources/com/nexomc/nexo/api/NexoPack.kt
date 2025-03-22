package com.nexomc.nexo.api

import com.nexomc.nexo.NexoPlugin
import com.nexomc.nexo.pack.PackGenerator
import java.io.File
import org.bukkit.entity.Player
import team.unnamed.creative.ResourcePack
import team.unnamed.creative.blockstate.BlockState
import team.unnamed.creative.font.Font
import team.unnamed.creative.lang.Language
import team.unnamed.creative.metadata.Metadata
import team.unnamed.creative.metadata.overlays.OverlayEntry
import team.unnamed.creative.metadata.overlays.OverlaysMeta
import team.unnamed.creative.metadata.sodium.SodiumMeta
import team.unnamed.creative.model.Model
import team.unnamed.creative.sound.SoundRegistry

object NexoPack {
    @JvmStatic
    fun sendPack(player: Player) {
        NexoPlugin.instance().packServer().sendPack(player)
    }

    @JvmStatic
    fun resourcePack() = NexoPlugin.instance().packGenerator().resourcePack()

    @JvmStatic
    fun builtResourcePack() = NexoPlugin.instance().packGenerator().builtPack()

    @JvmStatic
    fun mergePackFromZip(zipFile: File) {
        if (!zipFile.exists()) return
        mergePack(resourcePack(), PackGenerator.packReader.readFromZipFile(zipFile))
    }

    @JvmStatic
    fun mergePackFromDirectory(directory: File) {
        if (!directory.exists() || !directory.isDirectory) return
        mergePack(resourcePack(), PackGenerator.packReader.readFromDirectory(directory))
    }

    @JvmStatic
    fun overwritePack(resourcePack: ResourcePack, overwritePack: ResourcePack) {
        clearPack(resourcePack)
        mergePack(resourcePack, overwritePack)
    }

    @JvmStatic
    fun clearPack(resourcePack: ResourcePack) {
        resourcePack.icon(null)
        resourcePack.overlaysMeta(OverlaysMeta.of())
        resourcePack.metadata(Metadata.empty())
        resourcePack.models().toList().forEach { resourcePack.removeModel(it.key()) }
        resourcePack.textures().toList().forEach { resourcePack.removeTexture(it.key()) }
        resourcePack.atlases().toList().forEach { resourcePack.removeAtlas(it.key()) }
        resourcePack.languages().toList().forEach { resourcePack.removeLanguage(it.key()) }
        resourcePack.blockStates().toList().forEach { resourcePack.removeBlockState(it.key()) }
        resourcePack.fonts().toList().forEach { resourcePack.removeFont(it.key()) }
        resourcePack.sounds().toList().forEach { resourcePack.removeSound(it.key()) }
        resourcePack.soundRegistries().toList().forEach { resourcePack.removeSoundRegistry(it.namespace()) }
        resourcePack.unknownFiles().toMap().forEach { resourcePack.removeUnknownFile(it.key) }
    }

    @JvmStatic
    fun mergePack(resourcePack: ResourcePack, importedPack: ResourcePack) {
        importedPack.textures().forEach(resourcePack::texture)
        importedPack.sounds().forEach(resourcePack::sound)
        importedPack.unknownFiles().forEach(resourcePack::unknownFile)

        (importedPack.packMeta() ?: resourcePack.packMeta())?.apply(resourcePack::packMeta)
        (importedPack.icon() ?: resourcePack.icon())?.apply(resourcePack::icon)

        (resourcePack.overlaysMeta()?.entries() ?: mutableListOf<OverlayEntry>())
            .plus(importedPack.overlaysMeta()?.entries() ?: mutableListOf<OverlayEntry>())
            .also { resourcePack.overlaysMeta(OverlaysMeta.of(it)) }

        (resourcePack.sodiumMeta()?.ignoredShaders() ?: mutableListOf<String>())
            .plus(importedPack.sodiumMeta()?.ignoredShaders() ?: mutableListOf<String>())
            .also { resourcePack.sodiumMeta(SodiumMeta.of(it)) }

        importedPack.models().forEach { model: Model ->
            model.toBuilder().apply {
                resourcePack.model(model.key())?.overrides()?.forEach(::addOverride)
            }.build().addTo(resourcePack)
        }

        importedPack.fonts().forEach { font: Font ->
            font.toBuilder().apply {
                resourcePack.font(font.key())?.providers()?.forEach(::addProvider)
            }.build().addTo(resourcePack)
        }

        importedPack.soundRegistries().forEach { soundRegistry: SoundRegistry ->
            val baseRegistry = resourcePack.soundRegistry(soundRegistry.namespace())
            if (baseRegistry != null) {
                val mergedEvents = LinkedHashSet(baseRegistry.sounds())
                mergedEvents.addAll(soundRegistry.sounds())
                SoundRegistry.soundRegistry(baseRegistry.namespace(), mergedEvents).addTo(resourcePack)
            } else soundRegistry.addTo(resourcePack)
        }

        importedPack.atlases().forEach { atlas ->
            atlas.toBuilder().apply {
                resourcePack.atlas(atlas.key())?.sources()?.forEach(::addSource)
            }.build().addTo(resourcePack)
        }

        importedPack.languages().forEach { language: Language ->
            resourcePack.language(language.key())?.let { base: Language ->
                base.translations().entries.forEach { (key, value) ->
                    language.translations().putIfAbsent(key, value)
                }
            }
            language.addTo(resourcePack)
        }

        importedPack.blockStates().forEach { blockState: BlockState ->
            resourcePack.blockState(blockState.key())?.let { base: BlockState ->
                blockState.multipart().addAll(base.multipart())
                blockState.variants().putAll(base.variants())
            }
            blockState.addTo(resourcePack)
        }
    }
}
