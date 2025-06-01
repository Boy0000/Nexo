package com.nexomc.nexo.configs

import com.nexomc.nexo.NexoPlugin
import com.nexomc.nexo.utils.AdventureUtils
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender

enum class Message(val path: String) {
    // general
    PREFIX("general.prefix"),
    NO_PERMISSION("general.no_permission"),
    NOT_PLAYER("general.not_player"),
    COOLDOWN("general.cooldown"),
    RELOAD("general.reload"),
    PACK_REGENERATED("general.pack_regenerated"),
    UPDATING_CONFIG("general.updating_config"),
    REMOVING_CONFIG("general.removing_config"),
    UPDATED_ITEMS("general.updated_items"),
    BAD_RECIPE("general.bad_recipe"),
    ITEM_NOT_FOUND("general.item_not_found"),
    PLUGIN_HOOKS("general.plugin_hooks"),
    PLUGIN_UNHOOKS("general.plugin_unhooks"),
    NOT_ENOUGH_SPACE("general.not_enough_space"),
    EXIT_MENU("general.exit_menu"),
    NO_EMOJIS("general.no_emojis"),

    // logs
    PLUGIN_UNLOADED("logs.unloaded"),

    // command
    COMMAND_HELP("command.help"),
    DEBUG_TOGGLE("command.debug.toggle"),
    VERSION("command.version"),

    RECIPE_NO_BUILDER("command.recipe.no_builder"),
    RECIPE_NO_RECIPE("command.recipe.no_recipes"),
    RECIPE_SAVE("command.recipe.save"),

    GIVE_PLAYER("command.give.player"),
    GIVE_PLAYERS("command.give.players"),

    DYE_SUCCESS("command.dye.success"),
    DYE_WRONG_COLOR("command.dye.wrong_color"),
    DYE_FAILED("command.dye.failed"),

    // mechanics
    MECHANICS_JUKEBOX_NOW_PLAYING("mechanics.jukebox_now_playing");


    override fun toString(): String {
        return NexoPlugin.instance().configsManager().messages.getString(path)!!
    }

    fun send(sender: CommandSender?, vararg placeholders: TagResolver) {
        if (sender == null) return
        val lang = NexoPlugin.instance().configsManager().messages.getString(path).takeUnless { it.isNullOrEmpty() } ?: return
        sender.sendMessage(AdventureUtils.MINI_MESSAGE.deserialize(lang, TagResolver.resolver(*placeholders)))
    }

    fun toComponent() = AdventureUtils.MINI_MESSAGE.deserialize(toString())

    fun log(vararg placeholders: TagResolver) {
        send(Bukkit.getConsoleSender(), *placeholders)
    }
}
