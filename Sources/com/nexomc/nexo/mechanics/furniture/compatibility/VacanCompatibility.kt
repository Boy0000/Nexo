package com.nexomc.nexo.mechanics.furniture.compatibility

import ai.idealistic.vacan.abstraction.check.CheckEnums
import ai.idealistic.vacan.api.PlayerViolationEvent
import com.nexomc.nexo.mechanics.furniture.IFurniturePacketManager
import com.nexomc.nexo.utils.logs.Logs
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener

class VacanCompatibility : Listener {

    init {
        Logs.logSuccess("Initializing Vacan-AntiCheat Hook!")
    }

    private val furnitureHackTypes = setOf(CheckEnums.HackType.EXPLOITS, CheckEnums.HackType.IRREGULAR_MOVEMENTS)

    @EventHandler
    fun PlayerViolationEvent.onFurnitureHitbox() {
        if (hackType !in furnitureHackTypes) return

        if (IFurniturePacketManager.standingOnFurniture(player)) isCancelled = true
    }
}