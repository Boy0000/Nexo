package com.nexomc.nexo.mechanics.misc.custom

import com.nexomc.nexo.mechanics.Mechanic
import com.nexomc.nexo.mechanics.MechanicFactory
import com.nexomc.nexo.mechanics.misc.custom.fields.CustomEvent
import com.nexomc.nexo.mechanics.misc.custom.listeners.CustomListener
import com.nexomc.nexo.utils.actions.ClickAction.Companion.from
import org.bukkit.configuration.ConfigurationSection
import kotlin.collections.set

class CustomMechanic(factory: MechanicFactory, section: ConfigurationSection) : Mechanic(factory, section) {
    init {
        section.getKeys(false).forEach { subMechanicName ->
            val subsection = section.getConfigurationSection(subMechanicName) ?: return@forEach
            val key = subsection.currentPath ?: return@forEach

            LOADED_VARIANTS[key]?.apply(CustomListener::unregister)

            val clickAction = from(subsection) ?: return@forEach

            val listener = CustomEvent(
                subsection.getString("event", "")!!,
                subsection.getBoolean("one_usage", false)
            ).getListener(itemID, subsection.getLong("cooldown"), clickAction).apply(CustomListener::register)

            LOADED_VARIANTS[key] = listener
        }
    }

    companion object {
        private val LOADED_VARIANTS = mutableMapOf<String, CustomListener>()
    }
}
