package com.nexomc.nexo.pack.server

import org.bukkit.entity.Player

class EmptyServer : NexoPackServer {
    override fun sendPack(player: Player) {
    }

    override fun packUrl() = ""
}
