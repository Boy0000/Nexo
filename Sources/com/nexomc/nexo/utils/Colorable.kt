package com.nexomc.nexo.utils

import com.nexomc.nexo.api.NexoItems
import com.nexomc.nexo.nms.NMSHandlers
import org.bukkit.Color
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.*

interface Colorable {
    var color: Color?
}

fun ItemStack.asColorable(): Colorable? {
    return when {
        itemMeta.isColorable() -> NMSHandlers.handler().itemUtils().asColorable(this)
        NexoItems.builderFromItem(this)?.nexoMeta?.dyeableModel != null -> NMSHandlers.handler().itemUtils().asColorable(this)
        else -> null
    }
}

fun ItemMeta?.isColorable(): Boolean {
    return this is LeatherArmorMeta || this is PotionMeta || this is MapMeta || this is FireworkEffectMeta
}
