package com.nexomc.nexo.pack

import com.nexomc.nexo.pack.PackGenerator.Companion.externalPacks
import com.nexomc.nexo.pack.creative.NexoPackReader
import com.nexomc.nexo.utils.groupByFast
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import net.kyori.adventure.key.Key
import team.unnamed.creative.ResourcePack
import team.unnamed.creative.item.*
import team.unnamed.creative.item.property.ItemNumericProperty
import team.unnamed.creative.item.property.ItemStringProperty
import team.unnamed.creative.item.special.HeadSpecialRender
import team.unnamed.creative.model.ItemOverride
import team.unnamed.creative.model.ItemPredicate

object ModernVersionPatcher {

    fun convertResources(resourcePack: ResourcePack) {
        resourcePack.models().associateBy { Key.key(it.key().asString().replace("block/", "").replace("item/", "")) }.forEach { (itemKey, model) ->
            val overrides = model.overrides().ifEmpty { return@forEach }
            val standardItem = resourcePack.item(itemKey) ?: standardItemModels[itemKey]
            val finalNewItemModel = standardItem?.let { existingItemModel ->
                val baseItemModel = existingItemModel.model().takeUnless { it.isSimpleItemModel } ?: return@let null

                when (baseItemModel) {
                    is RangeDispatchItemModel -> {
                        val fallback = baseItemModel.fallback() ?: baseItemModel
                        val entries = baseItemModel.entries().plus(overrides.mapNotNull {
                            val model = when {
                                fallback is SpecialItemModel && fallback.render() is HeadSpecialRender -> ItemModel.special(fallback.render(), it.model())
                                else -> ItemModel.reference(it.model())
                            }
                            RangeDispatchItemModel.Entry.entry(it.predicate().customModelData ?: return@mapNotNull null, model)
                        }).distinctBy { it.threshold() }
                        ItemModel.rangeDispatch(ItemNumericProperty.customModelData(), 1f, entries, fallback)
                    }

                    is SelectItemModel -> ItemModel.rangeDispatch(ItemNumericProperty.customModelData(), 1f, overrides.mapNotNull { override ->
                        val model = when {
                            baseItemModel is SpecialItemModel && baseItemModel.render() is HeadSpecialRender -> ItemModel.special(baseItemModel.render(), override.model())
                            else -> ItemModel.reference(override.model())
                        }
                        RangeDispatchItemModel.Entry.entry(override.predicate().customModelData ?: return@mapNotNull null, model)
                    }, baseItemModel)

                    is ConditionItemModel -> {
                        val (trueOverrides, falseOverrides) = overrides.groupByFast { it.predicate().customModelData?.takeUnless { it == 0f } }.let { grouped ->
                            when {
                                itemKey.asString().endsWith("bow") ->
                                    grouped.values.flatMap { it.filter { p-> p.pulling } } to grouped.values.flatMap { it.filterNot { p -> p.pulling } }

                                itemKey.asString().endsWith("shield") ->
                                    grouped.values.mapNotNull { it.firstOrNull { it.blocking } } to grouped.values.mapNotNull { it.firstOrNull { !it.blocking } }

                                itemKey.asString().endsWith("fishing_rod") ->
                                    grouped.values.mapNotNull { it.firstOrNull { it.cast } } to grouped.values.mapNotNull { it.firstOrNull { !it.cast } }

                                else -> grouped.values.mapNotNull { it.firstOrNull() } to grouped.values.mapNotNull { it.lastOrNull() }
                            }
                        }

                        // If there are any pull-override predicates it is a bow, and we build a RangeDispatchItemModel for the Entry, otherwise a simple ReferenceItemModel
                        val onTrueCmdEntries = trueOverrides.groupBy { it.predicate().customModelData }.mapNotNull { (cmd, overrides) ->
                            val baseOverrideModel = overrides.firstOrNull()?.let { ItemModel.reference(it.model()) } ?: return@mapNotNull null

                            // If the overrides contain any pull, we make a bow-type model
                            val finalModel = overrides.drop(1).mapNotNull {
                                RangeDispatchItemModel.Entry.entry(it.predicate().pull ?: return@mapNotNull null, ItemModel.reference(it.model()))
                            }.takeUnless { it.isEmpty() }?.let { pullingEntries ->
                                val property = if (itemKey.asString().contains("crossbow")) ItemNumericProperty.crossbowPull() else ItemNumericProperty.useDuration()
                                ItemModel.rangeDispatch(property, RangeDispatchItemModel.DEFAULT_SCALE, pullingEntries, baseOverrideModel)
                            } ?: baseOverrideModel

                            RangeDispatchItemModel.Entry.entry(cmd ?: return@mapNotNull null, finalModel)
                        }

                        val onFalseCmdEntries = falseOverrides.groupBy { it.predicate().customModelData }.mapNotNull { (cmd, overrides) ->
                            val baseOverrideModel = overrides.firstOrNull()?.let { ItemModel.reference(it.model()) } ?: return@mapNotNull null
                            val firework = overrides.firstNotNullOfOrNull { it.firework }
                            val charged = overrides.firstNotNullOfOrNull { it.charged }

                            val finalModel = when {
                                charged != null || firework != null -> ItemModel.select().property(ItemStringProperty.chargeType()).fallback(baseOverrideModel).apply {
                                    charged?.apply(::addCase)
                                    firework?.apply(::addCase)
                                }.build()
                                else -> baseOverrideModel
                            }

                            RangeDispatchItemModel.Entry.entry(cmd ?: return@mapNotNull null, finalModel)
                        }

                        val onTrue = ItemModel.rangeDispatch(ItemNumericProperty.customModelData(), RangeDispatchItemModel.DEFAULT_SCALE, onTrueCmdEntries, baseItemModel.onTrue())
                        val onFalse = ItemModel.rangeDispatch(ItemNumericProperty.customModelData(), RangeDispatchItemModel.DEFAULT_SCALE, onFalseCmdEntries, baseItemModel.onFalse())

                        ItemModel.conditional(baseItemModel.condition(), onTrue, onFalse)
                    }

                    is ReferenceItemModel -> {
                        ItemModel.rangeDispatch(ItemNumericProperty.customModelData(), RangeDispatchItemModel.DEFAULT_SCALE, overrides.mapNotNull { override ->
                            val cmd = override.predicate().customModelData ?: return@mapNotNull null
                            RangeDispatchItemModel.Entry.entry(cmd, ItemModel.reference(override.model(), baseItemModel.tints()))
                        }, baseItemModel)
                    }

                    is SpecialItemModel -> {
                        val defaultDisplay = DisplayProperties.fromModel(model)
                        val newBase = ItemModel.special(baseItemModel.render(), when {
                            model.display() != defaultDisplay -> model.key()
                            else -> baseItemModel.base()
                        })

                        if (overrides.isNotEmpty()) ItemModel.rangeDispatch(ItemNumericProperty.customModelData(), RangeDispatchItemModel.DEFAULT_SCALE, overrides.mapNotNull { override ->
                            val cmd = override.predicate().customModelData ?: return@mapNotNull null
                            RangeDispatchItemModel.Entry.entry(cmd, ItemModel.special(baseItemModel.render(), override.model()))
                        }, newBase)
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
    private val ItemOverride.pulling: Boolean
        get() = predicate().any { it.name() == "pulling" }
    private val ItemOverride.charged: SelectItemModel.Case?
        get() = takeIf { it.predicate().any { it.name() == "charged" } }?.model()?.let { SelectItemModel.Case._case(ItemModel.reference(it), "arrow") }
    private val ItemOverride.firework: SelectItemModel.Case?
        get() = takeIf { it.predicate().any { it.name() == "firework" } }?.model()?.let { SelectItemModel.Case._case(ItemModel.reference(it), "rocket") }
    private val ItemOverride.blocking: Boolean
        get() = predicate().any { it.name() == "blocking" }
    private val ItemOverride.cast: Boolean
        get() = predicate().any { it.name() == "cast" }

    val standardItemModels by lazy {
        runCatching {
            NexoPackReader.INSTANCE.readFile(externalPacks.listFiles()!!.first { it.name.startsWith("RequiredPack_") })
        }.getOrDefault(ResourcePack.resourcePack()).items().plus(VanillaResourcePack.resourcePack.items())
            .associateByTo(Object2ObjectOpenHashMap()) { it.key() }
    }

    val ItemModel.isSimpleItemModel: Boolean get() {
        return (this as? ReferenceItemModel)?.tints()?.isEmpty() == true
    }
}