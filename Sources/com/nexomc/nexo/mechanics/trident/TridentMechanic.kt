package com.nexomc.nexo.mechanics.trident

import com.nexomc.nexo.api.NexoItems
import com.nexomc.nexo.mechanics.Mechanic
import com.nexomc.nexo.mechanics.MechanicFactory
import com.nexomc.nexo.mechanics.furniture.FurnitureTransform
import com.nexomc.nexo.utils.*
import io.papermc.paper.datacomponent.DataComponentTypes
import net.kyori.adventure.key.Key
import org.bukkit.Material
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.inventory.ItemStack
import org.joml.Vector2f

class TridentMechanic(factory: MechanicFactory, section: ConfigurationSection) : Mechanic(factory, section) {

    val itemModel: Key? = section.getKey("thrown_item_model") ?: section.rootSection.getKey("Components.item_model")
    val model: String? = section.getString("thrown_item", section.rootId)
    val transform = section.getEnum("display_transform", FurnitureTransform::class.java) ?: FurnitureTransform.NONE
    val rotation: Vector2f = section.getVector2f("rotation")

    val itemStack by lazy {
        if (itemModel != null) ItemStack.of(Material.PAPER).apply { setData(DataComponentTypes.ITEM_MODEL, itemModel) }
        else NexoItems.itemFromId(model)?.build()
    }
}