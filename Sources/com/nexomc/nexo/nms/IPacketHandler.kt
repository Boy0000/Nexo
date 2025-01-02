package com.nexomc.nexo.nms

import org.bukkit.entity.Player

interface IPacketHandler {
    class EmptyPacketHandler : IPacketHandler

    fun inject(player: Player) {}
    fun uninject(player: Player) {}

    companion object {
        const val CHANNEL_NAME = "nexo_channel_handler"
    }
}
