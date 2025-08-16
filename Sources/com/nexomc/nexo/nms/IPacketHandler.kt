package com.nexomc.nexo.nms

import org.bukkit.entity.Player

interface IPacketHandler {
    class EmptyPacketHandler : IPacketHandler

    fun inject(player: Player) {}
    fun uninject(player: Player) {}

    companion object {
        const val PACKET_HANDLER = "nexo_packet_handler"
        const val FURNITURE_HANDLER = "nexo_furniture_handler"
        const val GLYPH_HANDLER = "nexo_glyph_handler"
    }
}
