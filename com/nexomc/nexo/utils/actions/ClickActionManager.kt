package com.nexomc.nexo.utils.actions

import com.nexomc.nexo.utils.PluginUtils
import com.nexomc.nexo.utils.actions.impl.command.ConsoleAction
import com.nexomc.nexo.utils.actions.impl.command.PlayerAction
import com.nexomc.nexo.utils.actions.impl.message.ActionBarAction
import com.nexomc.nexo.utils.actions.impl.message.MessageAction
import com.nexomc.nexo.utils.actions.impl.other.SoundAction
import me.gabytm.util.actions.actions.ActionMeta
import me.gabytm.util.actions.placeholders.PlaceholderProvider
import me.gabytm.util.actions.spigot.actions.SpigotActionManager
import me.gabytm.util.actions.spigot.placeholders.PlaceholderAPIProvider
import org.apache.commons.lang3.StringUtils
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin

class ClickActionManager(plugin: JavaPlugin) : SpigotActionManager(plugin) {
    init {
        registerDefaults(Player::class.java)
        getComponentParser().registerDefaults(Player::class.java)

        if (PluginUtils.isEnabled("PlaceholderAPI")) getPlaceholderManager().register(PlaceholderAPIProvider())
        getPlaceholderManager().register(PlayerNamePlaceholderProvider())

        register(Player::class.java, ConsoleAction.IDENTIFIER) { meta: ActionMeta<Player?> -> ConsoleAction(meta) }
        register(Player::class.java, PlayerAction.IDENTIFIER) { meta: ActionMeta<Player?> -> PlayerAction(meta) }


        register(Player::class.java, ActionBarAction.IDENTIFIER) { meta: ActionMeta<Player?> -> ActionBarAction(meta) }
        register(Player::class.java, MessageAction.IDENTIFIER) { meta: ActionMeta<Player?> -> MessageAction(meta) }
        register(Player::class.java, SoundAction.IDENTIFIER) { meta: ActionMeta<Player?> -> SoundAction(meta) }
    }

    private class PlayerNamePlaceholderProvider : PlaceholderProvider<Player> {
        override fun getType() = Player::class.java

        override fun replace(player: Player, input: String): String = StringUtils.replace(input, "<player>", player.name)
    }
}
