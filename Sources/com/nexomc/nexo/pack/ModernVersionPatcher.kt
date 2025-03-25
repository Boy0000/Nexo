package com.nexomc.nexo.pack

import com.nexomc.nexo.pack.PackGenerator.Companion.externalPacks
import com.nexomc.nexo.pack.creative.NexoPackReader
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import net.kyori.adventure.key.Key
import team.unnamed.creative.ResourcePack
import team.unnamed.creative.item.ConditionItemModel
import team.unnamed.creative.item.EmptyItemModel
import team.unnamed.creative.item.Item
import team.unnamed.creative.item.ItemModel
import team.unnamed.creative.item.RangeDispatchItemModel
import team.unnamed.creative.item.ReferenceItemModel
import team.unnamed.creative.item.SelectItemModel
import team.unnamed.creative.item.SpecialItemModel
import team.unnamed.creative.item.property.ItemNumericProperty
import team.unnamed.creative.item.property.ItemStringProperty
import team.unnamed.creative.model.ItemOverride
import team.unnamed.creative.model.ItemPredicate

object ModernVersionPatcher {

    fun convertResources(resourcePack: ResourcePack) {
        resourcePack.models().associateBy { Key.key(it.key().asString().replace("block/", "").replace("item/", "")) }.forEach { (itemKey, model) ->
            val overrides = model.overrides()
            val standardItem = standardItemModels[itemKey]
            val finalNewItemModel = standardItem?.let { existingItemModel ->
                val baseItemModel = existingItemModel.model().takeUnless { it.isSimpleItemModel } ?: return@let null

                when (baseItemModel) {
                    is RangeDispatchItemModel -> ItemModel.rangeDispatch(ItemNumericProperty.customModelData(), 1f, overrides.mapNotNull {
                        RangeDispatchItemModel.Entry.entry(it.predicate().customModelData ?: return@mapNotNull null, ItemModel.reference(it.model()))
                    }, baseItemModel)

                    is SelectItemModel -> ItemModel.select().property(ItemStringProperty.customModelData()).fallback(baseItemModel).addCases(overrides.mapNotNull {
                        SelectItemModel.Case._case(ItemModel.reference(it.model()), it.predicate().customModelData?.toString() ?: return@mapNotNull null)
                    }).build()

                    is ConditionItemModel -> {
                        val (trueOverrides, falseOverrides) = overrides.groupBy { it.predicate().customModelData?.takeUnless { it == 0f } }.let {
                            it.values.mapNotNull { it.last() } to it.values.mapNotNull { it.first() }
                        }

                        val onTrue = ItemModel.rangeDispatch(ItemNumericProperty.customModelData(), 1f, trueOverrides.mapNotNull {
                            RangeDispatchItemModel.Entry.entry(it.predicate().customModelData ?: return@mapNotNull null, ItemModel.reference(it.model()))
                        }, baseItemModel.onTrue())

                        val onFalse = ItemModel.rangeDispatch(ItemNumericProperty.customModelData(), 1f, falseOverrides.mapNotNull {
                            RangeDispatchItemModel.Entry.entry(it.predicate().customModelData ?: return@mapNotNull null, ItemModel.reference(it.model()))
                        }, baseItemModel.onFalse())

                        ItemModel.conditional(baseItemModel.condition(), onTrue, onFalse)
                    }

                    is ReferenceItemModel -> ItemModel.rangeDispatch().fallback(baseItemModel).property(ItemNumericProperty.customModelData()).also { builder ->
                        builder.addEntries(overrides.mapNotNull { override ->
                            val cmd = override.predicate().customModelData ?: return@mapNotNull null
                            RangeDispatchItemModel.Entry.entry(cmd, ItemModel.reference(override.model(), baseItemModel.tints()))
                        })
                    }.build()

                    is SpecialItemModel -> {
                        val defaultDisplay = DisplayProperties.fromModel(model)
                        val newBase = ItemModel.special(baseItemModel.render(), when {
                            model.display() != defaultDisplay -> model.key()
                            else -> baseItemModel.base()
                        })

                        if (overrides.isNotEmpty()) ItemModel.rangeDispatch().fallback(newBase).property(ItemNumericProperty.customModelData()).apply {
                            addEntries(overrides.mapNotNull { override ->
                                val cmd = override.predicate().customModelData ?: return@mapNotNull null
                                RangeDispatchItemModel.Entry.entry(cmd, ItemModel.special(baseItemModel.render(), override.model()))
                            })
                        }.build()
                        else newBase
                    }
                    else -> baseItemModel
                }
            } ?: modelObject(standardItem?.model(), overrides, itemKey)

            resourcePack.item(Item.item(itemKey, finalNewItemModel))
        }
    }

    private fun modelObject(baseItemModel: ItemModel?, overrides: List<ItemOverride>, modelKey: Key?): ItemModel {
        return ItemModel.rangeDispatch().property(ItemNumericProperty.customModelData()).apply {
            if (modelKey != null) fallback(baseItemModel ?: ItemModel.reference(modelKey))
        }.addEntries(modelEntries(overrides)).build()
    }

    private fun modelEntries(overrides: List<ItemOverride>) = overrides.mapNotNull { override ->
        RangeDispatchItemModel.Entry.entry(override.predicate().customModelData ?: return@mapNotNull null, ItemModel.reference(override.model()))
    }

    private val List<ItemPredicate>.customModelData: Float?
        get() = firstOrNull { it.name() == "custom_model_data" }?.value().toString().toFloatOrNull()
    private val List<ItemPredicate>.pull: Float?
        get() = firstOrNull { it.name() == "pull" }?.value().toString().toFloatOrNull()
    private val List<ItemPredicate>.charged: Double?
        get() = firstOrNull { it.name() == "charged" }?.value().toString().toDoubleOrNull()
    private val List<ItemPredicate>.firework: Double?
        get() = firstOrNull { it.name() == "firework" }?.value().toString().toDoubleOrNull()

    val standardItemModels by lazy {
        runCatching {
            NexoPackReader.INSTANCE.readFile(externalPacks.listFiles()!!.first { it.name.startsWith("RequiredPack_") })
        }.getOrDefault(ResourcePack.resourcePack()).items().plus(DefaultResourcePackExtractor.vanillaResourcePack.items())
            .associateByTo(Object2ObjectOpenHashMap()) { it.key() }
    }

    val ItemModel.isSimpleItemModel: Boolean get() {
        return (this as? ReferenceItemModel)?.tints()?.isEmpty() == true || this is EmptyItemModel
    }
}