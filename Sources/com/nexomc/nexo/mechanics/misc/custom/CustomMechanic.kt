package com.nexomc.nexo.mechanics.misc.custom

import com.nexomc.nexo.mechanics.Mechanic
import com.nexomc.nexo.mechanics.MechanicFactory
import com.nexomc.nexo.mechanics.misc.custom.fields.CustomEvent
import com.nexomc.nexo.mechanics.misc.custom.listeners.CustomListener
import com.nexomc.nexo.utils.actions.ClickAction.Companion.from
import com.nexomc.nexo.utils.childSections
import org.bukkit.configuration.ConfigurationSection

class CustomMechanic(factory: CustomMechanicFactory, section: ConfigurationSection) : Mechanic(factory, section) {
    init {
        section.childSections().forEach { _, subSection ->
            val key = subSection.currentPath ?: return@forEach
            LOADED_VARIANTS[key]?.apply(CustomListener::unregister)

            val clickAction = from(subSection) ?: return@forEach

            val listener = CustomEvent(
                subSection.getString("event", "")!!,
                subSection.getBoolean("one_usage", false)
            ).getListener(itemID, subSection.getLong("cooldown"), clickAction).apply(CustomListener::register)

            LOADED_VARIANTS[key] = listener
        }
    }

    companion object {
        private val LOADED_VARIANTS = mutableMapOf<String, CustomListener>()
    }
}
