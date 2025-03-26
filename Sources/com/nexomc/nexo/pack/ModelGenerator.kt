package com.nexomc.nexo.pack

import com.nexomc.nexo.api.NexoItems
import com.nexomc.nexo.items.ItemBuilder
import com.nexomc.nexo.items.NexoMeta
import com.nexomc.nexo.utils.groupByFast
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import net.kyori.adventure.key.Key
import team.unnamed.creative.ResourcePack
import team.unnamed.creative.item.Item
import team.unnamed.creative.item.ItemModel
import team.unnamed.creative.model.ItemOverride
import team.unnamed.creative.model.Model
import team.unnamed.creative.model.ModelTexture
import team.unnamed.creative.model.ModelTextures

class ModelGenerator(private val resourcePack: ResourcePack) {
    private val predicateGenerator = PredicateGenerator(resourcePack)

    fun generateModels() {
        NexoItems.items().groupByFast(ItemBuilder::type).forEach { (type, items) ->
            val overrides = predicateGenerator.generateBaseModelOverrides(type, items)

            // Add overrides to base-models unless manually provided
            addOverrides(Key.key("item/${type.toString().lowercase()}"), overrides)
            addOverrides(Key.key("block/${type.toString().lowercase()}"), overrides)

            //generateItemModels
            items.forEach { item ->
                item.nexoMeta?.takeIf { it.containsPackInfo && it.generateModel }?.let { nexoMeta ->
                    val builder = generateModelBuilder(nexoMeta) ?: return@let

                    // If using a parent-model with display-properties, use instead of getting defaults
                    val display = resourcePack.model(nexoMeta.parentModel)?.display()?.takeUnless { it.isEmpty() }?.let(::Object2ObjectLinkedOpenHashMap) ?: DisplayProperties.fromMaterial(type)
                    val overrides = resourcePack.model(nexoMeta.model ?: return@let)?.overrides()?.let(::ObjectArrayList) ?: ObjectArrayList()

                    val model = builder.overrides(overrides).display(display).build()
                    model.addTo(resourcePack)
                }

                //ItemModels
                val itemKey = item.itemModel?.key() ?: return@forEach
                val reference = resourcePack.model(itemKey) ?: item.nexoMeta?.model?.let(resourcePack::model) ?: return@forEach
                resourcePack.item(itemKey) ?: resourcePack.item(Item.item(itemKey, ItemModel.reference(reference.key())))
            }
        }
    }

    private fun addOverrides(key: Key, overrides: List<ItemOverride>) {
        val model = (resourcePack.model(key) ?: VanillaResourcePack.resourcePack.model(key)) ?: return
        model.apply { overrides().addAll(overrides) }.addTo(resourcePack)
    }

    private fun generateModelBuilder(nexoMeta: NexoMeta): Model.Builder? {
        if (nexoMeta.model == null) return null
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
        return Model.model().key(nexoMeta.model ?: return null).parent(nexoMeta.parentModel).textures(textures.build())
    }
}
