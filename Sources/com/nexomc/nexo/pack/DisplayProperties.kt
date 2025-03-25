package com.nexomc.nexo.pack

import net.kyori.adventure.key.Key
import org.bukkit.Material
import team.unnamed.creative.model.ItemTransform
import team.unnamed.creative.model.Model

object DisplayProperties {
    fun fromMaterial(material: Material): Map<ItemTransform.Type, ItemTransform> {
        return DefaultResourcePackExtractor.vanillaResourcePack.model(Key.key("item/${material.name.lowercase()}"))?.display() ?: mapOf()
    }

    fun fromModel(model: Model): Map<ItemTransform.Type, ItemTransform> {
        return DefaultResourcePackExtractor.vanillaResourcePack.model(model.key())?.display() ?: mapOf()
    }
}
