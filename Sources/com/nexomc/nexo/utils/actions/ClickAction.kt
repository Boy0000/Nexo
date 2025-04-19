package com.nexomc.nexo.utils.actions

import com.nexomc.nexo.NexoPlugin
import com.nexomc.nexo.utils.getLinkedMapList
import com.nexomc.nexo.utils.printOnFailure
import com.nexomc.nexo.utils.safeCast
import me.gabytm.util.actions.actions.Action
import org.bukkit.Bukkit
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.Player
import org.springframework.expression.spel.standard.SpelExpressionParser
import org.springframework.expression.spel.support.StandardEvaluationContext

class ClickAction private constructor(private val conditions: List<String>, private val actions: List<Action<Player>>) {
    fun canRun(player: Player?): Boolean {
        if (conditions.isEmpty()) return true
        if (actions.isEmpty()) return false

        val context = StandardEvaluationContext(player).apply {
            setVariable("player", player)
            setVariable("server", Bukkit.getServer())
        }

        return conditions.all { condition ->
            runCatching {
                PARSER.parseExpression(condition).getValue(context, Boolean::class.java) ?: return false
            }.printOnFailure(true).getOrDefault(true)
        }
    }

    fun performActions(player: Player) {
        NexoPlugin.instance().clickActionManager().run(player, actions, false)
    }

    companion object {
        private val PARSER = SpelExpressionParser()

        private fun from(config: Map<String, Any>): ClickAction? {
            val conditions = config.getOrDefault("conditions", emptyList<Any>()).safeCast<List<String>>() ?: return null

            val actions = NexoPlugin.instance().clickActionManager()
                .parse(Player::class.java, config.getOrDefault("actions", emptyList<Any>()).safeCast<List<String>>() ?: return null)

            // If the action doesn't have any actions, return null
            return if (actions.isEmpty()) null else ClickAction(conditions, actions)
        }

        @JvmStatic
        fun from(config: ConfigurationSection): ClickAction? {
            val conditions = config.getStringList("conditions")
            val actions = NexoPlugin.instance().clickActionManager().parse(Player::class.java, config.getStringList("actions"))

            // If the action doesn't have any actions, return null
            if (actions.isEmpty()) return null

            return ClickAction(conditions, actions)
        }

        @JvmStatic
        fun parseList(section: ConfigurationSection): List<ClickAction> {
            val list = section.getLinkedMapList("clickActions")

            return list.mapNotNullTo(ArrayList(list.size)) { from(it) }
        }
    }
}
