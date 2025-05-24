package com.nexomc.nexo.nms

import org.bukkit.entity.Player

interface IPlayerUtils {

    fun applyMiningEffect(player: Player) {}

    fun removeMiningEffect(player: Player) {}

    class EmptyPlayerUtils : IPlayerUtils {
        override fun applyMiningEffect(player: Player) {}
        override fun removeMiningEffect(player: Player) {}
    }
}