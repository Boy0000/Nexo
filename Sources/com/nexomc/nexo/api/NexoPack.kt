package com.nexomc.nexo.api

import com.nexomc.nexo.NexoPlugin
import com.nexomc.nexo.pack.PackGenerator
import com.nexomc.nexo.utils.plusFast
import org.bukkit.entity.Player
import team.unnamed.creative.ResourcePack
import team.unnamed.creative.blockstate.BlockState
import team.unnamed.creative.font.Font
import team.unnamed.creative.lang.Language
import team.unnamed.creative.metadata.overlays.OverlayEntry
import team.unnamed.creative.metadata.overlays.OverlaysMeta
import team.unnamed.creative.metadata.sodium.SodiumMeta
import team.unnamed.creative.model.Model
import team.unnamed.creative.sound.SoundRegistry
import java.io.File

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
    fun mergePack(resourcePack: ResourcePack, importedPack: ResourcePack) {
        importedPack.textures().forEach(resourcePack::texture)
        importedPack.sounds().forEach(resourcePack::sound)
        importedPack.unknownFiles().forEach(resourcePack::unknownFile)

        (importedPack.packMeta() ?: resourcePack.packMeta())?.apply(resourcePack::packMeta)
        (importedPack.icon() ?: resourcePack.icon())?.apply(resourcePack::icon)
        resourcePack.overlaysMeta(OverlaysMeta.of(listOf<OverlayEntry>().plus(resourcePack.overlaysMeta()?.entries()).plus(importedPack.overlaysMeta()?.entries()).filterIsInstance<OverlayEntry>()))
        resourcePack.sodiumMeta(SodiumMeta.of(listOf<String>().plus(resourcePack.sodiumMeta()?.ignoredShaders()).plus(importedPack.sodiumMeta()?.ignoredShaders()).filterIsInstance<String>()))

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
