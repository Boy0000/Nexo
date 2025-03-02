package com.nexomc.nexo.fonts

import com.nexomc.nexo.NexoPlugin
import com.nexomc.nexo.api.NexoItems
import com.nexomc.nexo.configs.Message
import com.nexomc.nexo.configs.Settings
import com.nexomc.nexo.items.ItemBuilder
import com.nexomc.nexo.nms.GlyphHandlers
import com.nexomc.nexo.nms.NMSHandlers
import com.nexomc.nexo.utils.*
import com.nexomc.nexo.utils.AdventureUtils.STANDARD_MINI_MESSAGE
import com.nexomc.nexo.utils.AdventureUtils.parseLegacy
import com.nexomc.nexo.utils.AdventureUtils.parseLegacyThroughMiniMessage
import com.nexomc.nexo.utils.AdventureUtils.tagResolver
import com.nexomc.nexo.utils.logs.Logs
import io.papermc.paper.event.player.AsyncChatDecorateEvent
import net.kyori.adventure.inventory.Book
import net.kyori.adventure.text.TextComponent
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.*
import org.bukkit.event.block.Action
import org.bukkit.event.block.SignChangeEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.*
import org.bukkit.inventory.AnvilInventory
import org.bukkit.inventory.meta.BookMeta
import org.bukkit.persistence.PersistentDataType

class FontListener(private val manager: FontManager) : Listener {
    private val paperChatHandler: PaperChatHandler = PaperChatHandler()
    private val spigotChatHandler: SpigotChatHandler = SpigotChatHandler()

    enum class ChatHandler {
        LEGACY, MODERN;

        companion object {
            val isLegacy: Boolean get() = get() == LEGACY

            val isModern: Boolean get() = get() == MODERN

            fun get(): ChatHandler {
                return Settings.CHAT_HANDLER.toEnumOrGet(ChatHandler::class.java) { handler: String ->
                    Logs.logError("Invalid chat-handler $handler defined in settings.yml, defaulting to $MODERN", false)
                    Logs.logError("Valid options are: ${entries.toTypedArray().contentToString()}", true)
                    MODERN
                }
            }
        }
    }

    fun registerChatHandlers() {
        Bukkit.getPluginManager().registerEvents(paperChatHandler, NexoPlugin.instance())
        Bukkit.getPluginManager().registerEvents(spigotChatHandler, NexoPlugin.instance())
    }

    fun unregisterChatHandlers() {
        HandlerList.unregisterAll(paperChatHandler)
        HandlerList.unregisterAll(spigotChatHandler)
    }

    @EventHandler
    fun PlayerJoinEvent.onJoin() {
        if (Settings.FORMAT_PACKETS.toBool()) NMSHandlers.handler().packetHandler().inject(player)
    }

    @EventHandler
    fun PlayerQuitEvent.onQuit() {
        if (Settings.FORMAT_PACKETS.toBool()) NMSHandlers.handler().packetHandler().uninject(player)
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun PlayerEditBookEvent.onBookGlyph() {
        if (!Settings.FORMAT_BOOKS.toBool()) return

        val meta = newBookMeta
        meta.pages.forEach { page: String ->
            val i = meta.pages.indexOf(page) + 1
            if (i == 0) return@forEach
            manager.unicodeGlyphMap.keys.forEach { character: Char? ->
                if (character.toString() !in page) return@forEach

                val glyph = manager.glyphFromName(manager.unicodeGlyphMap[character])
                if (glyph.hasPermission(player)) return@forEach

                Message.NO_PERMISSION.send(player, tagResolver("permission", glyph.permission))
                isCancelled = true
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun PlayerInteractEvent.onBookGlyph() {
        val (item, meta) = (item ?: return) to (item?.itemMeta as? BookMeta ?: return)

        if (!Settings.FORMAT_BOOKS.toBool() || action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return
        if (item.type != Material.WRITTEN_BOOK || useInteractedBlock() == Event.Result.ALLOW) return

        meta.pages.forEach { page: String ->
            var page = page
            val i = meta.pages.indexOf(page) + 1
            if (i == 0) return@forEach

            manager.placeholderGlyphMap.entries.forEach { (glyphId, glyph) ->
                val unicode = java.lang.String.valueOf(glyph.character())
                if (glyph.hasPermission(player)) page =
                    page.replace(glyphId, ChatColor.WHITE.toString() + unicode + ChatColor.BLACK)
                        .replace(unicode, ChatColor.WHITE.toString() + unicode + ChatColor.BLACK)
                meta.setPage(i, parseLegacy(page))
            }
        }

        val pages = meta.pages().map(AdventureUtils.MINI_MESSAGE::serialize)
        val tagResolver = GlyphTag.getResolverForPlayer(player)

        val book = Book.builder()
            .title(AdventureUtils.MINI_MESSAGE.deserialize(meta.title ?: ""))
            .author(AdventureUtils.MINI_MESSAGE.deserialize(meta.author ?: ""))
            .pages(pages.map { STANDARD_MINI_MESSAGE.deserialize(it, tagResolver) }).build()

        // Open fake book and deny opening of original book to avoid needing to format the original book
        setUseItemInHand(Event.Result.DENY)
        NexoPlugin.instance().audience().player(player).openBook(book)
    }

    @EventHandler(ignoreCancelled = true)
    fun SignChangeEvent.onSignGlyph() {
        if (!Settings.FORMAT_SIGNS.toBool()) return

        lines.forEachIndexed { index, line ->
            var line = parseLegacyThroughMiniMessage(line)

            manager.unicodeGlyphMap.keys.filter { it in line }.forEach { character: Char ->
                val glyph = manager.glyphFromName(manager.unicodeGlyphMap[character]).takeUnless { it.hasPermission(player) } ?: return@forEach
                Message.NO_PERMISSION.send(player, tagResolver("permission", glyph.permission))
                isCancelled = true
            }

            manager.placeholderGlyphMap.filterValues { it.hasPermission(player) }.entries.forEach { (glyphId, glyph) ->
                line = line
                    .replace(glyphId, ChatColor.WHITE.toString() + glyph.character() + ChatColor.BLACK)
                    .replace(glyph.character(), ChatColor.WHITE.toString() + glyph.character() + ChatColor.BLACK)
            }

            setLine(index, parseLegacy(line))
        }
    }

    /*@EventHandler
    fun InventoryClickEvent.onPlayerRename() {
        val clickedInv = clickedInventory as? AnvilInventory ?: return
        if (!Settings.FORMAT_ANVIL.toBool() || slot != 2) return

        val player = whoClicked as Player
        var displayName = clickedInv.renameText
        val inputItem = clickedInv.getItem(0)
        val resultItem = clickedInv.getItem(2)
        if (resultItem == null || !NexoItems.exists(inputItem)) return

        if (displayName != null) {
            displayName = parseLegacyThroughMiniMessage(displayName)
            manager.unicodeGlyphMap.keys.forEach { character: Char ->
                if (character.toString() !in displayName!!) return@forEach
                val glyph = manager.glyphFromName(manager.unicodeGlyphMap[character])
                if (glyph.hasPermission(player)) return@forEach

                val required = manager.glyphFromName("required")
                val replacement = if (required.hasPermission(player)) required.character() else ""
                Message.NO_PERMISSION.send(player, tagResolver("permission", glyph.permission))
                displayName = displayName?.replace(character.toString(), replacement)
            }

            manager.placeholderGlyphMap.entries.forEach { (glyphId, glyph) ->
                if (glyph.hasPermission(player)) displayName = displayName?.replace(glyphId, glyph.character())
            }
        }

        // Since getRenameText is in PlainText, check if the displayName is the same as the rename text with all tags stripped
        // If so retain the displayName of inputItem. This also fixes enchantments breaking names
        // If the displayName is null, reset it to the "original" name
        val strippedDownInputDisplay = AdventureUtils.MINI_MESSAGE.stripTags(parseLegacy(inputItem!!.itemMeta.displayName))
        if (VersionUtil.below("1.20.5") && ((displayName.isNullOrEmpty() && NexoItems.exists(inputItem)) || strippedDownInputDisplay == displayName)) {
            displayName = inputItem.itemMeta.persistentDataContainer.get(ItemBuilder.ORIGINAL_NAME_KEY, PersistentDataType.STRING)
        }

        resultItem.editMeta { it.displayName(displayName?.deserialize()) }
    }*/

    @EventHandler
    fun PlayerJoinEvent.onPlayerJoin() {
        manager.sendGlyphTabCompletion(player)
    }

    @EventHandler
    fun PlayerQuitEvent.onPlayerQuit() {
        manager.clearGlyphTabCompletions(player)
    }

    inner class SpigotChatHandler : Listener {
        @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
        fun AsyncPlayerChatEvent.onPlayerChat() {
            if (!Settings.FORMAT_CHAT.toBool() || !ChatHandler.isLegacy) return

            format = format
            message = GlyphHandlers.escapeGlyphs(message.deserialize(), player).serialize()
        }

        /**
         * Formats a string with glyphs and placeholders
         *
         * @param string The string to format
         * @param player The player to check permissions for, if null it parses the string without checking permissions
         * @return The formatted string, or null if the player doesn't have permission for a glyph
         */
        private fun format(string: String, player: Player?): String? {
            var component = AdventureUtils.MINI_MESSAGE_PLAYER(player).deserialize(string) as TextComponent
            if (player != null) manager.unicodeGlyphMap.keys.forEach { character: Char ->
                if (character !in component.content()) return@forEach
                val glyph = manager.glyphFromName(manager.unicodeGlyphMap[character])
                if (!glyph.hasPermission(player)) {
                    Message.NO_PERMISSION.send(player, tagResolver("permission", glyph.permission))
                    return null
                }
            }

            manager.placeholderGlyphMap.values.forEach { glyph ->
                if (player != null && !glyph.hasPermission(player)) return@forEach
                component = component.replaceText(glyph.placeholderReplacementConfig!!) as TextComponent
            }

            return AdventureUtils.LEGACY_SERIALIZER.serialize(component)
        }
    }


    inner class PaperChatHandler : Listener {
        @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
        fun AsyncChatDecorateEvent.onPlayerChat() {
            val player = player() ?: return
            if (!Settings.FORMAT_CHAT.toBool() || !ChatHandler.isModern) return
            result(GlyphHandlers.escapePlaceholders(GlyphHandlers.escapeGlyphs(result(), player), player))
        }
    }
}
