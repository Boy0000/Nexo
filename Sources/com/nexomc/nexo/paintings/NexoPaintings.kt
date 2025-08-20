package com.nexomc.nexo.paintings

import com.nexomc.nexo.utils.childSections
import com.nexomc.nexo.utils.getKey
import io.papermc.paper.plugin.bootstrap.BootstrapContext
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents
import io.papermc.paper.registry.RegistryKey
import io.papermc.paper.registry.TypedKey
import io.papermc.paper.registry.event.RegistryEvents
import io.papermc.paper.registry.keys.PaintingVariantKeys
import io.papermc.paper.registry.tag.TagKey
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import org.bukkit.Art
import org.bukkit.configuration.file.YamlConfiguration
import kotlin.io.path.notExists

object NexoPaintings {
    @JvmStatic
    fun registerCustomPaintings(context: BootstrapContext) {
        val randomPlacePaintings = mutableListOf<TypedKey<Art>>()

        runCatching {
            if (context.dataDirectory.notExists()) return@runCatching
            val paintingFile = context.dataDirectory.resolve("paintings.yml").toFile().apply { createNewFile() }
            val paintingsYaml = runCatching { YamlConfiguration.loadConfiguration(paintingFile) }.getOrNull() ?: return@runCatching
            val paintings = paintingsYaml.getConfigurationSection("paintings") ?: return

            context.lifecycleManager.registerEventHandler(RegistryEvents.PAINTING_VARIANT.freeze().newHandler { handler ->
                paintings.childSections().forEach { keyId, section ->
                    val key = Key.key(keyId)
                    val author = section.getRichMessage("author") ?: Component.text("boy0000")
                    val title = section.getRichMessage("title") ?: Component.text(keyId)
                    val assetId = section.getKey("asset_id") ?: key
                    val variantKey = PaintingVariantKeys.create(key)

                    if (section.getBoolean("random_place")) randomPlacePaintings.add(TypedKey.create(RegistryKey.PAINTING_VARIANT, key))

                    runCatching {
                        handler.registry().register(variantKey) { builder ->
                            builder.author(author).title(title).assetId(assetId)
                                .width(section.getInt("width").coerceIn(1..16))
                                .height(section.getInt("height").coerceIn(1..16))
                        }
                    }.onFailure {
                        context.logger.warn("Failed to register Custom Painting $key", it)
                    }
                }
            })
        }.onFailure {
            context.logger.error("Failed to load paintings...")
        }

        runCatching {
            val paintingKey = TagKey.create(RegistryKey.PAINTING_VARIANT, Key.key("placeable"))
            context.lifecycleManager.registerEventHandler(LifecycleEvents.TAGS.postFlatten(RegistryKey.PAINTING_VARIANT)) { event ->
                val placeablePaintings = event.registrar().getTag(paintingKey).plus(randomPlacePaintings)
                event.registrar().setTag(paintingKey, placeablePaintings)
            }
        }.onFailure {
            context.logger.error("Failed to load paintings...", it)
        }
    }
}