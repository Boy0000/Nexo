package com.nexomc.nexo.utils.actions.impl.message

import me.gabytm.util.actions.actions.Action
import me.gabytm.util.actions.actions.ActionMeta
import me.gabytm.util.actions.actions.Context
import org.bukkit.entity.Player

class MessageAction(meta: ActionMeta<Player?>) : Action<Player>(meta) {
    override fun run(player: Player, context: Context<Player>) {
        player.sendRichMessage(meta.getParsedData(player, context))
    }

    companion object {
        const val IDENTIFIER = "message"
    }
}
