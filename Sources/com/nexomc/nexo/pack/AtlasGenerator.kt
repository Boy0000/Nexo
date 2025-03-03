package com.nexomc.nexo.pack

import com.nexomc.nexo.utils.mapNotNullFast
import net.kyori.adventure.key.Key
import team.unnamed.creative.ResourcePack
import team.unnamed.creative.atlas.Atlas
import team.unnamed.creative.atlas.AtlasSource
import team.unnamed.creative.atlas.SingleAtlasSource
import team.unnamed.creative.model.ModelTexture

object AtlasGenerator {
    fun generateAtlasFile(resourcePack: ResourcePack) {
        val sources = mutableListOf<AtlasSource>()
        resourcePack.models().forEach { model ->
            addKey(model.textures().layers().mapNotNullFast(ModelTexture::key), sources)
            addKey(model.textures().variables().values.mapNotNullFast(ModelTexture::key), sources)

            model.textures().particle()?.key()?.let { addKey(listOf(it), sources) }
        }
        sources.sortBy { (it as? SingleAtlasSource)?.resource() }

        val atlas = resourcePack.atlas(Atlas.BLOCKS)?.let {
            it.toBuilder().sources(it.sources().plus(sources)).build()
        } ?: Atlas.atlas(Atlas.BLOCKS, sources)

        atlas.addTo(resourcePack)
    }

    private fun addKey(keys: List<Key>, sources: MutableList<AtlasSource>) {
        keys.forEach { key ->
            if (DefaultResourcePackExtractor.vanillaResourcePack.texture(key) != null) return
            sources.add(AtlasSource.single(key))
        }
    }
}
