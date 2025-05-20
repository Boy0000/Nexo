package com.nexomc.nexo.fonts

import com.nexomc.nexo.configs.Message
import com.nexomc.nexo.configs.Settings
import com.nexomc.nexo.nms.GlyphHandlers
import com.nexomc.nexo.nms.NMSHandlers
import com.nexomc.nexo.utils.AdventureUtils
import com.nexomc.nexo.utils.AdventureUtils.STANDARD_MINI_MESSAGE
import com.nexomc.nexo.utils.AdventureUtils.parseLegacy
import com.nexomc.nexo.utils.AdventureUtils.parseLegacyThroughMiniMessage
import com.nexomc.nexo.utils.AdventureUtils.tagResolver
import io.papermc.paper.event.player.AsyncChatDecorateEvent
import net.kyori.adventure.inventory.Book
import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.event.Event
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.SignChangeEvent
import org.bukkit.event.player.PlayerEditBookEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.meta.BookMeta

class FontListener(private val manager: FontManager) : Listener {

    @EventHandler
    fun AsyncChatDecorateEvent.onDecorate() {
        val message = GlyphHandlers.escapePlaceholders(result(), player())
        result(GlyphHandlers.escapeGlyphs(message, player()))
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun PlayerJoinEvent.onJoin() {
        NMSHandlers.handler().packetHandler().inject(player)
    }

    @EventHandler
    fun PlayerQuitEvent.onQuit() {
        NMSHandlers.handler().packetHandler().uninject(player)
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun PlayerEditBookEvent.onBookGlyph() {
        if (!Settings.FORMAT_BOOKS.toBool()) return

        val meta = newBookMeta
        meta.pages.forEach { page: String ->
            val i = meta.pages.indexOf(page) + 1
            if (i == 0) return@forEach
            manager.unicodeGlyphMap.keys.forEach { character ->
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
                val unicode = glyph.unicodes.joinToString()
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
        player.openBook(book)
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
                line = line.replace(glyphId, ChatColor.WHITE.toString() + glyph.formattedUnicodes + ChatColor.BLACK)
                line = glyph.unicodes.fold(line) { line, unicode -> line.replace(unicode, ChatColor.WHITE.toString() + unicode + ChatColor.BLACK) }
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
}
