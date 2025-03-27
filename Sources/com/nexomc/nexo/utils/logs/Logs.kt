package com.nexomc.nexo.utils.logs

import com.nexomc.nexo.configs.Settings
import com.nexomc.nexo.utils.AdventureUtils
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.ChatColor

object Logs {
    @JvmStatic
    fun logInfo(message: String) {
        if (message.isNotEmpty()) logInfo(message, false)
    }

    @JvmStatic
    fun logInfo(message: String, newline: Boolean) {
        val info = AdventureUtils.MINI_MESSAGE.deserialize("<prefix><#529ced>$message</#529ced>")
        Bukkit.getConsoleSender().sendMessage(if (newline) info.append(Component.newline()) else info)
    }

    @JvmStatic
    @JvmOverloads
    fun logSuccess(message: String, newline: Boolean = false) {
        val success = AdventureUtils.MINI_MESSAGE.deserialize("<prefix><#55ffa4>$message</#55ffa4>")
        Bukkit.getConsoleSender().sendMessage(if (newline) success.append(Component.newline()) else success)
    }

    @JvmStatic
    @JvmOverloads
    fun logError(message: String, newline: Boolean = false) {
        val error = AdventureUtils.MINI_MESSAGE.deserialize("<prefix><#e73f34>$message</#e73f34>")
        Bukkit.getConsoleSender().sendMessage(if (newline) error.append(Component.newline()) else error)
    }

    @JvmStatic
    @JvmOverloads
    fun logWarn(message: String, newline: Boolean = false) {
        val warning = AdventureUtils.MINI_MESSAGE.deserialize("<prefix><#f9f178>$message</#f9f178>")
        Bukkit.getConsoleSender().sendMessage(if (newline) warning.append(Component.newline()) else warning)
    }

    @JvmStatic
    fun newline() {
        Bukkit.getConsoleSender().sendMessage(Component.empty())
    }

    fun debug(`object`: Any) {
        if (Settings.DEBUG.toBool()) Bukkit.broadcastMessage(`object`.toString())
    }

    fun debug(`object`: Any, prefix: String) {
        if (Settings.DEBUG.toBool()) Bukkit.broadcastMessage(prefix + `object`)
    }

    private val debugColors =
        listOf(ChatColor.GREEN, ChatColor.BLUE, ChatColor.RED, ChatColor.GOLD, ChatColor.LIGHT_PURPLE)

    fun debug(vararg objects: Any?) {
        if (!Settings.DEBUG.toBool()) return
        val objectList = listOf(*objects)

        buildString {
            for (i in objectList.indices) {
                append(debugColors[i % debugColors.size].toString()).append(objectList[i])
                if (i + 1 != objectList.size) append(ChatColor.WHITE.toString() + " | ")
            }

        }.let(Bukkit::broadcastMessage)
    }

    fun debug(prefix: String?, vararg objects: Any?) {
        if (!Settings.DEBUG.toBool()) return
        val objectList = listOf(*objects)

        buildString {
            append(prefix)
            objectList.indices.forEach { i ->
                append(debugColors[i % debugColors.size].toString()).append(objectList[i])
                if (i + 1 != objectList.size) append(ChatColor.WHITE.toString() + " | ")
            }

            append(prefix)
        }.let(Bukkit::broadcastMessage)
    }

    fun <T> T.debugVal() = this.apply {
        if (Settings.DEBUG.toBool()) Bukkit.broadcastMessage(this.toString())
    }

    fun <T> T.debugVal(prefix: String) = this.apply {
        if (Settings.DEBUG.toBool()) Bukkit.broadcastMessage(prefix + this)
    }

    fun debug(component: Component) {
        if (Settings.DEBUG.toBool()) Bukkit.getConsoleSender().sendMessage(component)
    }
}
