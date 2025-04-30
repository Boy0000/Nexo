package com.nexomc.nexo.utils

import com.nexomc.nexo.nms.NMSHandlers
import org.bukkit.Color
import org.bukkit.inventory.ItemStack

interface Colorable {
    var color: Color?
}

fun ItemStack.asColorable(): Colorable? {
    return NMSHandlers.handler().asColorable(this)
}