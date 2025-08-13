package com.nexomc.nexo.mechanics.furniture.states

import com.nexomc.nexo.api.NexoItems
import com.nexomc.nexo.mechanics.furniture.FurnitureMechanic
import com.nexomc.nexo.mechanics.furniture.IFurniturePacketManager
import com.nexomc.nexo.utils.associateFastLinked
import com.nexomc.nexo.utils.childSections
import com.nexomc.nexo.utils.getKey
import com.nexomc.nexo.utils.getStringOrNull
import com.nexomc.nexo.utils.indexOfOrNull
import com.nexomc.nexo.utils.rootId
import net.kyori.adventure.key.Key
import org.bukkit.NamespacedKey
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.ItemDisplay
import org.bukkit.persistence.PersistentDataType

data class FurnitureStates(val creativeModeOnly: Boolean, val states: Map<String, FurnitureState>) {
    constructor(section: ConfigurationSection) : this(
        section.getBoolean("creative_mode_only", true),
        section.childSections().entries.associateFastLinked { it.key to FurnitureState(it.value) }
    )

    companion object {
        val STATE_KEY = NamespacedKey.fromString("nexo:furniture_state")!!

        fun currentState(baseEntity: ItemDisplay, mechanic: FurnitureMechanic): FurnitureState? {
            if (mechanic.states?.states.isNullOrEmpty()) return null

            val stateId = baseEntity.persistentDataContainer.get(STATE_KEY, PersistentDataType.STRING) ?: ""
            return mechanic.states.states[stateId] ?: mechanic.states.states.entries.firstOrNull()?.value?.apply {
                baseEntity.persistentDataContainer.set(STATE_KEY, PersistentDataType.STRING, id)
            }
        }

        fun cycleState(baseEntity: ItemDisplay, mechanic: FurnitureMechanic) {
            val states = mechanic.states?.states?.takeIf { it.isNotEmpty() }?.entries ?: return
            val currentStateId = baseEntity.persistentDataContainer.get(STATE_KEY, PersistentDataType.STRING) ?: ""
            val currentState = states.indexOfOrNull(currentStateId)?.inc()?.coerceAtMost(states.size - 1)
                ?.let(states::elementAtOrNull) ?: states.firstOrNull()?.value ?: return

            baseEntity.persistentDataContainer.set(STATE_KEY, PersistentDataType.STRING, currentStateId)
            IFurniturePacketManager.furnitureBaseMap[baseEntity.uniqueId]?.refreshItem(baseEntity)
        }
    }
}

data class FurnitureState(val id: String, val itemModel: Key?, val item: String?) {

    constructor(section: ConfigurationSection) : this(
        section.name,
        section.getKey("item_model"),
        section.getStringOrNull("nexo_item")
    ) {
        require(itemModel != null || NexoItems.exists(item)) { "FurnitureState requires either an ItemModel or a NexoItem to be specified, missing in ${section.rootId}" }
    }
}