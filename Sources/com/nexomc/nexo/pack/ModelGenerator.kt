package com.nexomc.nexo.pack

import com.nexomc.nexo.api.NexoItems
import com.nexomc.nexo.items.ItemBuilder
import com.nexomc.nexo.items.NexoMeta
import com.nexomc.nexo.utils.KeyUtils.appendSuffix
import com.nexomc.nexo.utils.groupByFast
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import net.kyori.adventure.key.Key
import org.bukkit.Color
import team.unnamed.creative.ResourcePack
import team.unnamed.creative.item.Item
import team.unnamed.creative.item.ItemModel
import team.unnamed.creative.item.property.ItemBooleanProperty
import team.unnamed.creative.item.tint.TintSource
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
                val itemId = NexoItems.idFromItem(item)?.lowercase() ?: return@forEach
                val itemKey = item.itemModel?.key() ?: Key.key("nexo", itemId)
                val reference = resourcePack.model(itemKey) ?: item.nexoMeta?.model?.let(resourcePack::model) ?: return@forEach

                val dyeableModel = item.nexoMeta?.dyeableModel?.let {
                    resourcePack.model(it) ?: reference.toBuilder()
                        .textures(ModelTextures.builder().layers(ModelTexture.ofKey(it.key())).build()).build()
                        .apply(resourcePack::model)
                } ?: resourcePack.model(itemKey.appendSuffix("_dyeable"))

                val throwingModel = item.nexoMeta?.throwingModel?.let {
                    resourcePack.model(it) ?: reference.toBuilder()
                        .textures(ModelTextures.builder().layers(ModelTexture.ofKey(it.key())).build()).build()
                        .apply(resourcePack::model)
                } ?: resourcePack.model(itemKey.appendSuffix("_throwing"))

                val handSwapAnimation = item.nexoMeta?.handSwapAnimation.takeUnless { it == Item.DEFAULT_HAND_ANIMATION_ON_SWAP }
                val oversizedInGui = item.nexoMeta?.oversizedInGui.takeUnless { it == Item.DEFAULT_OVERSIZED_IN_GUI }
                val itemModel = when {
                    resourcePack.item(itemKey) != null -> return@forEach
                    dyeableModel != null -> {
                        val falseModel = ItemModel.reference(reference.key())
                        val trueModel = ItemModel.reference(dyeableModel.key(), TintSource.dye(item.color?.asRGB() ?: Color.WHITE.asRGB()))
                        ItemModel.conditional(ItemBooleanProperty.hasComponent("minecraft:dyed_color"), trueModel, falseModel)
                    }
                    item.color != null -> ItemModel.reference(reference.key(), TintSource.dye(item.color!!.asRGB()))
                    throwingModel != null -> {
                        val falseModel = ItemModel.reference(reference.key())
                        val trueModel = ItemModel.reference(throwingModel.key())
                        ItemModel.conditional(ItemBooleanProperty.usingItem(), trueModel, falseModel)
                    }
                    oversizedInGui != null || handSwapAnimation != null ->
                        ItemModel.reference(item.nexoMeta?.model ?: Key.key("nexo:$itemId"))
                    else -> return@forEach
                }

                resourcePack.item(
                    Item.item(
                        itemKey, itemModel,
                        handSwapAnimation ?: Item.DEFAULT_HAND_ANIMATION_ON_SWAP,
                        oversizedInGui ?: Item.DEFAULT_OVERSIZED_IN_GUI
                    )
                )
            }
        }
    }

    private fun addOverrides(key: Key, overrides: List<ItemOverride>) {
        val model = (resourcePack.model(key) ?: VanillaResourcePack.resourcePack.model(key)) ?: return
        val parent = model.parent() ?: VanillaResourcePack.resourcePack.model(key)?.parent() ?: Model.BUILT_IN_ENTITY
        model.toBuilder().parent(parent).overrides(model.overrides().plus(overrides)).build().addTo(resourcePack)
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
