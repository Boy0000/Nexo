package com.nexomc.nexo.utils.actions.impl.message

import com.nexomc.nexo.utils.deserialize
import me.gabytm.util.actions.actions.Action
import me.gabytm.util.actions.actions.ActionMeta
import me.gabytm.util.actions.actions.Context
import org.bukkit.entity.Player

class ActionBarAction(meta: ActionMeta<Player?>) : Action<Player>(meta) {
    override fun run(player: Player, context: Context<Player>) {
        player.sendActionBar(meta.getParsedData(player, context).deserialize())
    }

    companion object {
        const val IDENTIFIER = "actionbar"
    }
}
