package com.nexomc.nexo.pack

import com.nexomc.nexo.api.NexoItems
import com.nexomc.nexo.utils.KeyUtils.dropExtension
import net.kyori.adventure.key.Key
import org.bukkit.Material
import team.unnamed.creative.ResourcePack
import team.unnamed.creative.model.*
import kotlin.math.min
import kotlin.math.round

class PredicateGenerator(private val resourcePack: ResourcePack) {
    /**
     * Generates the base model overrides for the given material
     * This looks up all ItemBuilders using this material and generates the overrides for them
     * This includes CustomModelData, ItemPredicates like pulling, blocking, charged, cast, firework and damage
     * @param material the material to generate the overrides for
     * @return the generated overrides
     */
    fun generateBaseModelOverrides(material: Material): List<ItemOverride> {
        val itemBuilders = LinkedHashSet(NexoItems.items().filter { it.type == material })
        val overrides = DefaultResourcePackExtractor.vanillaResourcePack
            .model(Key.key("item/" + material.toString().lowercase()))?.overrides() ?: mutableListOf()

        itemBuilders.forEach { itemBuilder ->
            val nexoMeta = itemBuilder.nexoMeta?.takeIf { it.containsPackInfo } ?: return@forEach
            val cmdPredicate = ItemPredicate.customModelData(nexoMeta.customModelData ?: return@forEach)
            overrides += ItemOverride.of(nexoMeta.modelKey, cmdPredicate)

            val parentModel = nexoMeta.parentModel
            if (nexoMeta.hasBlockingModel()) addMissingOverrideModel(itemBuilder.type, nexoMeta.blockingModel!!, parentModel)
            if (nexoMeta.hasChargedModel()) addMissingOverrideModel(itemBuilder.type, nexoMeta.chargedModel!!, parentModel)
            if (nexoMeta.hasCastModel()) addMissingOverrideModel(itemBuilder.type, nexoMeta.castModel!!, parentModel)
            if (nexoMeta.hasFireworkModel()) addMissingOverrideModel(itemBuilder.type, nexoMeta.fireworkModel!!, parentModel)
            for (pullingKey in nexoMeta.pullingModels) addMissingOverrideModel(itemBuilder.type, pullingKey, parentModel)
            for (damagedKey in nexoMeta.damagedModels) addMissingOverrideModel(itemBuilder.type, damagedKey, parentModel)

            if (nexoMeta.hasBlockingModel()) overrides.add(ItemOverride.of(nexoMeta.blockingModel, ItemPredicate.blocking(), cmdPredicate))
            if (nexoMeta.hasChargedModel()) overrides.add(ItemOverride.of(nexoMeta.chargedModel, ItemPredicate.charged(), cmdPredicate))
            if (nexoMeta.hasCastModel()) overrides.add(ItemOverride.of(nexoMeta.castModel, ItemPredicate.cast(), cmdPredicate))
            if (nexoMeta.hasFireworkModel()) overrides.add(ItemOverride.of(nexoMeta.fireworkModel, ItemPredicate.firework(), cmdPredicate))

            nexoMeta.pullingModels.forEachIndexed { i, model ->
                val pull = if (i == 0) 0f else min(roundPredicate(i.plus(1f).div(nexoMeta.pullingModels.size).toDouble()), 0.9f)
                overrides += ItemOverride.of(model, ItemPredicate.pulling(), ItemPredicate.pull(pull), cmdPredicate)
            }

            nexoMeta.damagedModels.drop(1).mapIndexed { i, key -> i.plus(1) to key }.forEach { (i, model) ->
                val damage = min(roundPredicate(i.toFloat().div(nexoMeta.damagedModels.size).toDouble()), 0.99f)
                overrides += ItemOverride.of(model, ItemPredicate.pulling(), ItemPredicate.damage(damage), cmdPredicate)
            }
        }

        return overrides
    }

    private fun roundPredicate(value: Double, step: Float = 0.05f): Float {
        var roundedValue = (round(value / step) * step).toFloat()
        val remainder = (value % step).toFloat()

        if (remainder > step / 2) roundedValue += step
        return String.format("%.2f", roundedValue).replace(",", ".").toFloat()
    }

    private fun addMissingOverrideModel(material: Material, modelKey: Key, parentKey: Key) {
        resourcePack.model(
            resourcePack.model(modelKey) ?: Model.model().key(modelKey).parent(parentKey).display(DisplayProperties.fromMaterial(material))
                .textures(ModelTextures.builder().layers(ModelTexture.ofKey(dropExtension(modelKey))).build()).build()
        )
    }
}

