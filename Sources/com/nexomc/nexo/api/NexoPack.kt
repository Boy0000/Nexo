package com.nexomc.nexo.api

import com.nexomc.nexo.NexoPlugin
import com.nexomc.nexo.pack.creative.NexoPackReader
import com.nexomc.nexo.utils.logs.Logs
import org.bukkit.entity.Player
import team.unnamed.creative.ResourcePack
import team.unnamed.creative.blockstate.BlockState
import team.unnamed.creative.font.Font
import team.unnamed.creative.item.*
import team.unnamed.creative.lang.Language
import team.unnamed.creative.metadata.Metadata
import team.unnamed.creative.metadata.overlays.OverlayEntry
import team.unnamed.creative.metadata.overlays.OverlaysMeta
import team.unnamed.creative.metadata.sodium.SodiumMeta
import team.unnamed.creative.model.Model
import team.unnamed.creative.overlay.Overlay
import team.unnamed.creative.overlay.ResourceContainer
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
        mergePack(resourcePack(), NexoPackReader.INSTANCE.readFromZipFile(zipFile))
    }

    @JvmStatic
    fun mergePackFromDirectory(directory: File) {
        if (!directory.exists() || !directory.isDirectory) return
        mergePack(resourcePack(), NexoPackReader.INSTANCE.readFromDirectory(directory))
    }

    @JvmStatic
    fun overwritePack(resourcePack: ResourcePack, overwritePack: ResourcePack) {
        clearPack(resourcePack)
        mergePack(resourcePack, overwritePack)
    }

    @JvmStatic
    fun clearPack(resourcePack: ResourcePack) {
        resourcePack.icon(null)
        resourcePack.metadata(Metadata.empty())
        resourcePack.models().map { it.key() }.forEach(resourcePack::removeModel)
        resourcePack.textures().map { it.key() }.forEach(resourcePack::removeTexture)
        resourcePack.atlases().map { it.key() }.forEach(resourcePack::removeAtlas)
        resourcePack.languages().map { it.key() }.forEach(resourcePack::removeLanguage)
        resourcePack.blockStates().map { it.key() }.forEach(resourcePack::removeBlockState)
        resourcePack.fonts().map { it.key() }.forEach(resourcePack::removeFont)
        resourcePack.sounds().map { it.key() }.forEach(resourcePack::removeSound)
        resourcePack.soundRegistries().map { it.namespace() }.forEach(resourcePack::removeSoundRegistry)
        resourcePack.unknownFiles().map { it.key }.forEach(resourcePack::removeUnknownFile)
        resourcePack.items().map { it.key() }.forEach(resourcePack::removeItem)
        resourcePack.equipment().map { it.key() }.forEach(resourcePack::removeEquipment)
    }

    @JvmStatic
    fun mergePack(resourcePack: ResourcePack, importedPack: ResourcePack) {
        (importedPack.packMeta() ?: resourcePack.packMeta())?.apply(resourcePack::packMeta)
        (importedPack.icon() ?: resourcePack.icon())?.apply(resourcePack::icon)

        resourcePack.overlays().plus(importedPack.overlays()).groupBy { it.directory() }.forEach { (directory, overlays) ->
            val newOverlay = Overlay.overlay(directory)
            overlays.forEach { overlay -> mergeContainers(newOverlay, overlay) }
            resourcePack.overlay(newOverlay)
        }

        (resourcePack.overlaysMeta()?.entries() ?: mutableListOf<OverlayEntry>())
            .plus(importedPack.overlaysMeta()?.entries() ?: mutableListOf<OverlayEntry>())
            .also { resourcePack.overlaysMeta(OverlaysMeta.of(it)) }

        (resourcePack.sodiumMeta()?.ignoredShaders() ?: mutableListOf<String>())
            .plus(importedPack.sodiumMeta()?.ignoredShaders() ?: mutableListOf<String>())
            .also { resourcePack.sodiumMeta(SodiumMeta.sodium(it)) }

        mergeContainers(resourcePack, importedPack)
    }

    private fun mergeContainers(container: ResourceContainer, importedContainer: ResourceContainer) {
        importedContainer.textures().forEach(container::texture)
        importedContainer.sounds().forEach(container::sound)
        importedContainer.unknownFiles().forEach(container::unknownFile)

        importedContainer.equipment().forEach { equipment ->
            val oldEquipment = container.equipment(equipment.key()) ?: return@forEach container.equipment(equipment)
            val layersByType = LinkedHashMap(oldEquipment.layers())
            equipment.layers().forEach { (type, layers) ->
                layersByType.compute(type) { _, oldLayers ->
                    return@compute (oldLayers ?: listOf()).plus(layers)
                }
            }

            container.equipment(oldEquipment.layers(layersByType))
        }

        importedContainer.items().forEach { item ->
            val oldItem = container.item(item.key()) ?: return@forEach container.item(item)
            val handSwap = if (oldItem.handAnimationOnSwap()) item.handAnimationOnSwap() else oldItem.handAnimationOnSwap()

            fun mergeItemModels(oldItem: ItemModel, newItem: ItemModel): ItemModel {
                return when (newItem) {
                    is ReferenceItemModel -> ItemModel.reference(newItem.model(), newItem.tints().plus((oldItem as? ReferenceItemModel)?.tints() ?: listOf()))
                    is CompositeItemModel -> ItemModel.composite(newItem.models().plus((oldItem as? CompositeItemModel)?.models() ?: listOf()))
                    is SelectItemModel -> newItem.toBuilder().addCases((oldItem as? SelectItemModel)?.cases() ?: listOf()).build()
                    is RangeDispatchItemModel -> newItem.toBuilder().addEntries((oldItem as? RangeDispatchItemModel)?.entries() ?: listOf()).build()
                    is ConditionItemModel -> {
                        val oldCondition = (oldItem as? ConditionItemModel)?.takeIf { it.condition() == newItem.condition() }
                        val mergedTrue = oldCondition?.onTrue()?.let { mergeItemModels(it, newItem.onTrue()) } ?: newItem.onTrue()
                        val mergedFalse = oldCondition?.onFalse()?.let { mergeItemModels(it, newItem.onFalse()) } ?: newItem.onFalse()
                        ItemModel.conditional(newItem.condition(), mergedTrue, mergedFalse)
                    }
                    else -> item.model()
                }
            }

            when {
                oldItem.model() is ReferenceItemModel -> item.model()
                oldItem.model() is EmptyItemModel -> item.model()
                oldItem.model().javaClass == item.model().javaClass -> mergeItemModels(oldItem.model(), item.model())
                else -> {
                    Logs.logError("Failed to merge ItemModels ${item.key().asString()}, ")
                    Logs.logWarn("Existing ItemModel of incompatible type ${oldItem.model().javaClass.simpleName}, keeping old ItemModel...")
                    null
                }
            }?.let { Item.item(item.key(), it, handSwap) }?.addTo(container)
        }

        importedContainer.models().forEach { model: Model ->
            model.toBuilder().apply {
                container.model(model.key())?.overrides()?.forEach(::addOverride)
            }.build().addTo(container)
        }

        importedContainer.fonts().forEach { font: Font ->
            font.toBuilder().apply {
                container.font(font.key())?.providers()?.forEach(::addProvider)
            }.build().addTo(container)
        }

        importedContainer.soundRegistries().forEach { soundRegistry: SoundRegistry ->
            val baseRegistry = container.soundRegistry(soundRegistry.namespace())
            if (baseRegistry != null) {
                val mergedEvents = LinkedHashSet(baseRegistry.sounds())
                mergedEvents.addAll(soundRegistry.sounds())
                SoundRegistry.soundRegistry(baseRegistry.namespace(), mergedEvents).addTo(container)
            } else soundRegistry.addTo(container)
        }

        importedContainer.atlases().forEach { atlas ->
            atlas.toBuilder().apply {
                container.atlas(atlas.key())?.sources()?.forEach(::addSource)
            }.build().addTo(container)
        }

        importedContainer.languages().forEach { language: Language ->
            container.language(language.key())?.let { base: Language ->
                base.translations().entries.forEach { (key, value) ->
                    language.translations().putIfAbsent(key, value)
                }
            }
            language.addTo(container)
        }

        importedContainer.blockStates().forEach { blockState: BlockState ->
            container.blockState(blockState.key())?.let { base: BlockState ->
                blockState.multipart().addAll(base.multipart())
                blockState.variants().putAll(base.variants())
            }
            blockState.addTo(container)
        }
    }
}
