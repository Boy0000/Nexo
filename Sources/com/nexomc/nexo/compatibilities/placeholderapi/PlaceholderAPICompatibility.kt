package com.nexomc.nexo.compatibilities.placeholderapi

import com.nexomc.nexo.NexoPlugin
import com.nexomc.nexo.compatibilities.CompatibilityProvider
import me.clip.placeholderapi.PlaceholderAPIPlugin

class PlaceholderAPICompatibility : CompatibilityProvider<PlaceholderAPIPlugin>() {
    private val expansion = NexoExpansion(NexoPlugin.instance())

    init {
        expansion.register()
    }

    override fun disable() {
        super.disable()
        expansion.unregister()
    }
}
