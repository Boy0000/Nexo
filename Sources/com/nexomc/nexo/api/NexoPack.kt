package com.nexomc.nexo.api

import com.nexomc.nexo.NexoPlugin
import com.nexomc.nexo.pack.creative.NexoPackReader
import com.nexomc.nexo.utils.logs.Logs
import java.io.File
import org.bukkit.entity.Player
import team.unnamed.creative.ResourcePack
import team.unnamed.creative.blockstate.BlockState
import team.unnamed.creative.font.Font
import team.unnamed.creative.item.CompositeItemModel
import team.unnamed.creative.item.ConditionItemModel
import team.unnamed.creative.item.EmptyItemModel
import team.unnamed.creative.item.Item
import team.unnamed.creative.item.ItemModel
import team.unnamed.creative.item.RangeDispatchItemModel
import team.unnamed.creative.item.ReferenceItemModel
import team.unnamed.creative.item.SelectItemModel
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
        resourcePack.models().toList().forEach { resourcePack.removeModel(it.key()) }
        resourcePack.textures().toList().forEach { resourcePack.removeTexture(it.key()) }
        resourcePack.atlases().toList().forEach { resourcePack.removeAtlas(it.key()) }
        resourcePack.languages().toList().forEach { resourcePack.removeLanguage(it.key()) }
        resourcePack.blockStates().toList().forEach { resourcePack.removeBlockState(it.key()) }
        resourcePack.fonts().toList().forEach { resourcePack.removeFont(it.key()) }
        resourcePack.sounds().toList().forEach { resourcePack.removeSound(it.key()) }
        resourcePack.soundRegistries().toList().forEach { resourcePack.removeSoundRegistry(it.namespace()) }
        resourcePack.unknownFiles().toList().forEach { resourcePack.removeUnknownFile(it.first) }
        resourcePack.items().toList().forEach { resourcePack.removeItem(it.key()) }
        resourcePack.equipment().toList().forEach { resourcePack.removeEquipment(it.key()) }
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

        importedPack.equipment().forEach { equipment ->
            val oldEquipment = resourcePack.equipment(equipment.key()) ?: return@forEach resourcePack.equipment(equipment)
            val layersByType = LinkedHashMap(oldEquipment.layers())
            equipment.layers().forEach { (type, layers) ->
                layersByType.compute(type) { _, oldLayers ->
                    return@compute (oldLayers ?: listOf()).plus(layers)
                }
            }

            resourcePack.equipment(oldEquipment.layers(layersByType))
        }

        importedPack.items().forEach { item ->
            val oldItem = resourcePack.item(item.key()) ?: return@forEach resourcePack.item(item)

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
            }?.let { Item.item(item.key(), it, item.handAnimationOnSwap()) }?.addTo(resourcePack)
        }

        (resourcePack.sodiumMeta()?.ignoredShaders() ?: mutableListOf<String>())
            .plus(importedPack.sodiumMeta()?.ignoredShaders() ?: mutableListOf<String>())
            .also { resourcePack.sodiumMeta(SodiumMeta.sodium(it)) }

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
