package com.nexomc.nexo.pack

import com.nexomc.nexo.api.NexoItems
import com.nexomc.nexo.items.ItemBuilder
import com.nexomc.nexo.items.NexoMeta
import com.nexomc.nexo.utils.mapFastSet
import net.kyori.adventure.key.Key
import org.bukkit.Material
import team.unnamed.creative.ResourcePack
import team.unnamed.creative.model.Model
import team.unnamed.creative.model.ModelTexture
import team.unnamed.creative.model.ModelTextures

class ModelGenerator(private val resourcePack: ResourcePack) {
    private val predicateGenerator = PredicateGenerator(resourcePack)

    fun generateBaseItemModels() {
        // Generate the baseItem model and add all needed overrides
        NexoItems.items().mapFastSet(ItemBuilder::type).forEach { baseMaterial ->
            val baseModelKey = Key.key("item/${baseMaterial.toString().lowercase()}")
            val model = resourcePack.model(baseModelKey) ?: DefaultResourcePackExtractor.vanillaResourcePack.model(baseModelKey) ?: return@forEach

            model.toBuilder().also { builder ->
                predicateGenerator.generateBaseModelOverrides(baseMaterial).forEach { override ->
                    builder.addOverride(override)
                }
            }.build().addTo(resourcePack)
        }
    }

    fun generateItemModels() {
        NexoItems.items().forEach { itemBuilder ->
            val nexoMeta = itemBuilder.nexoMeta ?: return@forEach
            if (!nexoMeta.containsPackInfo || !nexoMeta.generateModel) return@forEach
            val model = generateModelBuilder(itemBuilder.type, nexoMeta)

            resourcePack.model(model.key())?.also {
                model.toBuilder().also { builder ->
                    it.overrides().forEach { override ->
                        builder.addOverride(override!!)
                    }
                }.build().addTo(resourcePack)
            } ?: model.addTo(resourcePack)
        }
    }

    private fun generateModelBuilder(material: Material, nexoMeta: NexoMeta): Model {
        val parent = nexoMeta.parentModel.value()
        val textures = nexoMeta.modelTextures?.toBuilder() ?: ModelTextures.builder()
        val layers = nexoMeta.modelTextures?.layers() ?: listOf()
        val defaultTexture = layers.firstOrNull() ?: ModelTexture.ofKey(Key.key(""))

        if (nexoMeta.modelTextures?.variables().isNullOrEmpty()) {
            textures.layers(listOf())
            when {
                parent == "block/cube" || parent == "block/cube_directional" || parent == "block/cube_mirrored" -> {
                    textures.addVariable("particle", layers.getOrNull(2) ?: defaultTexture)
                    textures.addVariable("down", defaultTexture)
                    textures.addVariable("up", layers.getOrNull(1) ?: defaultTexture)
                    textures.addVariable("north", layers.getOrNull(2) ?: defaultTexture)
                    textures.addVariable("south", layers.getOrNull(3) ?: defaultTexture)
                    textures.addVariable("west", layers.getOrNull(4) ?: defaultTexture)
                    textures.addVariable("east", layers.getOrNull(5) ?: defaultTexture)
                }
                parent == "block/cube_all" || parent == "block/cube_mirrored_all" ->
                    textures.addVariable("all", defaultTexture)
                parent == "block/cross" -> textures.addVariable("cross", defaultTexture)
                parent.startsWith("block/orientable") -> {
                    textures.addVariable("front", defaultTexture)
                    textures.addVariable("side", layers.getOrNull(1) ?: defaultTexture)
                    if (!parent.endsWith("vertical")) textures.addVariable("top", layers.getOrNull(2) ?: defaultTexture)
                    if (parent.endsWith("with_bottom")) textures.addVariable("bottom", layers.getOrNull(3) ?: defaultTexture)
                }
                parent.startsWith("block/cube_column") -> {
                    textures.addVariable("end", defaultTexture)
                    textures.addVariable("side",layers.getOrNull(1) ?: defaultTexture)
                }
                parent == "block/cube_bottom_top" || parent.contains("block/slab") || parent.endsWith("stairs") -> {
                    textures.addVariable("bottom", defaultTexture)
                    textures.addVariable("side", layers.getOrNull(1) ?: defaultTexture)
                    textures.addVariable("top", layers.getOrNull(2) ?: defaultTexture)
                }
                parent == "block/cube_top" -> {
                    textures.addVariable("top", defaultTexture)
                    textures.addVariable("side", layers.getOrNull(1) ?: defaultTexture)
                }
                "block/door_" in parent -> {
                    textures.addVariable("bottom", defaultTexture)
                    textures.addVariable("top", layers.getOrNull(1) ?: defaultTexture)
                }
                "trapdoor" in parent -> textures.addVariable("texture", defaultTexture)
                else -> textures.layers(layers)
            }
        }

        return Model.model()
            .key(nexoMeta.modelKey!!)
            .display(DisplayProperties.fromMaterial(material))
            .parent(nexoMeta.parentModel)
            .textures(textures.build())
            .build()
    }
}
