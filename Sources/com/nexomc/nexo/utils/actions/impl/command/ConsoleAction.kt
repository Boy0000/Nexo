package com.nexomc.nexo.utils.actions.impl.command

import com.nexomc.nexo.NexoPlugin
import me.gabytm.util.actions.actions.Action
import me.gabytm.util.actions.actions.ActionMeta
import me.gabytm.util.actions.actions.Context
import org.bukkit.Bukkit
import org.bukkit.entity.Player

class ConsoleAction(meta: ActionMeta<Player?>) : Action<Player>(meta) {
    override fun run(player: Player, context: Context<Player>) {
        Bukkit.getServer().globalRegionScheduler.run(NexoPlugin.instance()) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), meta.getParsedData(player, context))
        }
    }

    companion object {
        const val IDENTIFIER = "console"
    }
}
