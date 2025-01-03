package com.nexomc.nexo.pack

import com.google.gson.JsonParser
import com.nexomc.nexo.pack.PackGenerator.Companion.externalPacks
import com.nexomc.nexo.pack.creative.NexoPackReader
import com.nexomc.nexo.utils.*
import com.nexomc.nexo.utils.JsonBuilder.array
import com.nexomc.nexo.utils.JsonBuilder.`object`
import com.nexomc.nexo.utils.JsonBuilder.plus
import com.nexomc.nexo.utils.JsonBuilder.primitive
import com.nexomc.nexo.utils.JsonBuilder.toJsonArray
import com.nexomc.nexo.utils.logs.Logs
import com.willfp.eco.core.lookup.matches
import team.unnamed.creative.ResourcePack
import team.unnamed.creative.base.Writable
import team.unnamed.creative.model.ItemOverride
import team.unnamed.creative.model.ItemPredicate

// Patch for handling CustomModelData for 1.21.4+ until creative updated
class ModernVersionPatcher(val resourcePack: ResourcePack) {
    private val overlayItemModelRegex = ".+/assets/minecraft/items/.*\\.json".toRegex()

    private fun mergeItemModels(firstModel: Writable, secondModel: Writable): Writable {
        // Parse JSON objects
        val firstJson = firstModel.toJsonObject() ?: return secondModel
        val secondJson = secondModel.toJsonObject() ?: return firstModel
        if (firstJson == secondJson) return secondModel

        // Extract "entries" array from the first model
        val firstEntries = firstJson.`object`("model")?.array("entries")

        // If "entries" exist in the base model, merge with overlay
        if (firstEntries != null) {
            val secondModelJson = secondJson.`object`("model") ?: return firstModel
            val updatedEntries = secondModelJson.array("entries")?.plus(firstEntries) ?: firstEntries

            // Add merged entries back into the second model JSON
            secondModelJson.add("entries", updatedEntries)
            return Writable.stringUtf8(secondJson.toString())
        }

        // No "entries" in the base model, replace with overlay
        return secondModel
    }

    fun patchPack() {
        val itemModels = DefaultResourcePackExtractor.vanillaResourcePack.unknownFiles()
            .plus(resourcePack.unknownFiles())
            .filterKeys { it.startsWith("assets/minecraft/items") }
            .toMutableMap()

        resourcePack.models().filter { DefaultResourcePackExtractor.vanillaResourcePack.model(it.key()) != null }
            .associate { it.key().value().removePrefix("item/").appendIfMissing(".json") to it.overrides() }
            .forEach { (model, overrides) ->
                val existingItemModel = itemModels["assets/minecraft/items/$model"]?.let { JsonParser.parseString(it.toUTF8String()).asJsonObject }
                // If not standard (shield etc.) we need to traverse the tree
                val finalNewItemModel = existingItemModel?.takeUnless { isStandardItemModel("assets/minecraft/items/$model", it) }?.also { existingItemModel ->
                    // More complex item-models, like shield etc
                    val baseItemModel = existingItemModel.`object`("model") ?: return@also

                    runCatching {
                        if ("on_false" in baseItemModel.keySet()) handleOnBoolean(false, baseItemModel, overrides)
                        if ("on_true" in baseItemModel.keySet()) handleOnBoolean(true, baseItemModel, overrides)
                        if ("tints" in baseItemModel.keySet()) handleTints(existingItemModel, baseItemModel, overrides)
                        if ("cases" in baseItemModel.keySet()) handleCases(existingItemModel, baseItemModel, overrides)
                    }.onFailure {
                        it.printStackTrace()
                        Logs.logError(model)
                        Logs.logWarn(overrides.joinToString("\n") { it.toString() })
                    }

                } ?: JsonBuilder.jsonObject.plus("model", modelObject(existingItemModel?.`object`("model"), overrides, model))
                resourcePack.unknownFile("assets/minecraft/items/$model", Writable.stringUtf8(finalNewItemModel.toString()))
            }

        // Merge any ItemModel in an overlay into the base one
        resourcePack.unknownFiles()
            .filterKeys { it.matches(overlayItemModelRegex) }
            .mapKeys { it.key.substringAfter("/").prependIfMissing("assets/") }
            .forEach { (key, overlayWritable) ->
                resourcePack.unknownFile(key)?.toJsonObject()
                    ?.takeUnless { !isStandardItemModel(key, it) }
                    ?.also { resourcePack.unknownFile(key, overlayWritable) }
                    ?: mergeItemModels(resourcePack.unknownFile(key) ?: overlayWritable, overlayWritable).also {
                        resourcePack.unknownFile(key, it)
                    }
            }

        // Remove all overlay ItemModels
        resourcePack.unknownFiles()
            .filterKeys { it.matches(overlayItemModelRegex) }
            .keys.forEach(resourcePack::removeUnknownFile)
    }

    private fun handleCases(existingItemModel: JsonObject, baseItemModel: JsonObject, overrides: MutableList<ItemOverride>) {
        JsonBuilder.jsonObject.plus("type", "minecraft:range_dispatch")
            .plus("property", "minecraft:custom_model_data")
            .plus(
                "entries", JsonBuilder.jsonArray
                    .plus(JsonBuilder.jsonObject.plus("model", baseItemModel).plus("threshold", 0f))
                    .plus(overrides.mapNotNull {
                        val cmd = it.predicate().customModelData ?: return@mapNotNull null
                        JsonBuilder.jsonObject.plus("threshold", cmd).plus(
                            "model", JsonBuilder.jsonObject
                                .plus("model", it.model().asString())
                                .plus("type", "minecraft:model")
                        )
                    }.toJsonArray())
            ).let { existingItemModel.plus("model", it) }
    }

    private fun handleTints(existingItemModel: JsonObject, baseItemModel: JsonObject, overrides: MutableList<ItemOverride>) {
        val defaultTints = baseItemModel.array("tints")?.deepCopy() ?: return

        JsonBuilder.jsonObject.plus("type", "minecraft:range_dispatch")
            .plus("property", "minecraft:custom_model_data")
            .plus(
                "entries", JsonBuilder.jsonArray
                    .plus(JsonBuilder.jsonObject.plus("model", baseItemModel).plus("threshold", 0f))
                    .plus(overrides.mapNotNull {
                        val cmd = it.predicate().customModelData ?: return@mapNotNull null
                        JsonBuilder.jsonObject.plus("threshold", cmd).plus(
                            "model", JsonBuilder.jsonObject
                                .plus("model", it.model().asString())
                                .plus("type", "minecraft:model")
                                .plus("tints", defaultTints)
                        )
                    }.toJsonArray())
            ).let { existingItemModel.plus("model", it) }
    }

    private fun handleOnBoolean(boolean: Boolean, baseItemModel: JsonObject, overrides: MutableList<ItemOverride>) {
        val defaultObject = baseItemModel.`object`("on_$boolean")?.deepCopy() ?: return
        val wantedOverrides = overrides.groupBy { it.predicate().customModelData }
            .let { e -> e.values.map { if (boolean) it.last() else it.first() } }
            .filter { (it.predicate().customModelData ?: 0) != 0 }

        JsonBuilder.jsonObject.plus("type", "minecraft:range_dispatch")
            .plus("property", "minecraft:custom_model_data")
            .plus(
                "entries", JsonBuilder.jsonArray
                    .plus(JsonBuilder.jsonObject.plus("model", defaultObject).plus("threshold", 0f))
                    .plus(wantedOverrides.mapNotNull {
                        val cmd = it.predicate().customModelData ?: return@mapNotNull null
                        JsonBuilder.jsonObject.plus("threshold", cmd).plus(
                            "model", JsonBuilder.jsonObject
                                .plus("model", it.model().asString())
                                .plus("type", "minecraft:model")
                        )
                    }.toJsonArray())
            ).let { baseItemModel.plus("on_$boolean", it) }
    }

    private fun isStandardItemModel(key: String, itemModel: JsonObject): Boolean {
        return runCatching {
            NexoPackReader().readFile(externalPacks.listFiles()!!.first { it.name.startsWith("RequiredPack_") })
        }.getOrDefault(DefaultResourcePackExtractor.vanillaResourcePack).unknownFiles()
            .filterKeys { it.startsWith("assets/minecraft/items") }[key]?.toJsonObject()?.equals(itemModel) ?: false
    }

    private fun modelObject(baseItemModel: JsonObject?, overrides: List<ItemOverride>, model: String? = null): JsonObject = JsonBuilder.jsonObject
        .plus("type", "minecraft:range_dispatch")
        .plus("property", "minecraft:custom_model_data")
        .plus("entries", modelEntries(overrides))
        .plus("scale", 1f)
        .apply {
            if (model != null) plus("fallback", baseItemModel ?: JsonBuilder.jsonObject
                .plus("type", "minecraft:model")
                .plus("model", "item/${model.removeSuffix(".json")}")
            )
        }

    private fun modelEntries(overrides: List<ItemOverride>) = overrides.mapNotNull {
        JsonBuilder.jsonObject.plus("threshold", it.predicate().customModelData ?: return@mapNotNull null).plus(
            "model", JsonBuilder.jsonObject
                .plus("model", it.model().asString())
                .plus("type", "minecraft:model")
        )
    }.toJsonArray()

    private val List<ItemPredicate>.customModelData: Float?
        get() = firstOrNull { it.name() == "custom_model_data" }?.value().toString().toFloatOrNull()
}