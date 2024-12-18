package com.nexomc.nexo.mechanics.furniture.compatibility

import com.nexomc.nexo.mechanics.furniture.BlockLocation
import com.nexomc.nexo.mechanics.furniture.IFurniturePacketManager
import com.nexomc.nexo.utils.logs.Logs
import me.vagdedes.spartan.api.PlayerViolationEvent
import me.vagdedes.spartan.system.Enums.HackType
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener

class SpartanCompatibility : Listener {

    init {
        Logs.logSuccess("Initializing Spartan-AntiCheat Hook!")
    }

    private val furnitureHackTypes = setOf(HackType.Exploits, HackType.IrregularMovements)

    @EventHandler
    fun PlayerViolationEvent.onFurnitureHitbox() {
        if (hackType !in furnitureHackTypes) return

        if (IFurniturePacketManager.standingOnFurniture(player)) isCancelled = true
    }
}