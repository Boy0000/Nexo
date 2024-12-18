package com.nexomc.nexo.utils.actions.impl.message

import com.nexomc.nexo.NexoPlugin
import me.gabytm.util.actions.actions.Action
import me.gabytm.util.actions.actions.ActionMeta
import me.gabytm.util.actions.actions.Context
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.entity.Player

class MessageAction(meta: ActionMeta<Player?>) : Action<Player>(meta) {
    override fun run(player: Player, context: Context<Player>) {
        val message = LegacyComponentSerializer.legacySection().deserialize(meta.getParsedData(player, context))
        NexoPlugin.instance().audience().player(player).sendMessage(message)
    }

    companion object {
        const val IDENTIFIER = "message"
    }
}
