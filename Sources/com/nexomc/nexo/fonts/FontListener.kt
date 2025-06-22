package com.nexomc.nexo.fonts

import com.nexomc.nexo.NexoPlugin
import com.nexomc.nexo.glyphs.Glyph
import com.nexomc.nexo.nms.GlyphHandlers
import com.nexomc.nexo.nms.NMSHandlers
import com.nexomc.nexo.utils.serialize
import io.papermc.paper.event.player.AsyncChatCommandDecorateEvent
import io.papermc.paper.event.player.AsyncChatDecorateEvent
import me.neznamy.tab.api.TabAPI
import me.neznamy.tab.api.event.player.PlayerLoadEvent
import me.neznamy.tab.api.event.plugin.TabLoadEvent
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.inventory.PrepareAnvilEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.persistence.PersistentDataContainer
import org.bukkit.persistence.PersistentDataType
import team.unnamed.creative.font.Font


class FontListener(private val manager: FontManager) : Listener {

    @EventHandler(priority = EventPriority.LOWEST)
    fun PlayerJoinEvent.onJoin() {
        NMSHandlers.handler().packetHandler().inject(player)
        manager.sendGlyphTabCompletion(player)
    }

    @EventHandler
    fun PlayerQuitEvent.onQuit() {
        NMSHandlers.handler().packetHandler().uninject(player)
    }

    @EventHandler
    fun AsyncChatDecorateEvent.onDecorate() {
        result(GlyphHandlers.escapeGlyphs(result(), player()))
    }

    @EventHandler
    fun AsyncChatCommandDecorateEvent.onPlayerCommandChat() {
        result(GlyphHandlers.escapeGlyphs(result(), player()))
    }

    // Adds PDC entry of the original PlainText so that packet-handler can always ensure it is correctly named
    @EventHandler
    fun PrepareAnvilEvent.onRename() {
        val pdcTask =  { pdc: PersistentDataContainer ->
            if (inventory.renameText.isNullOrEmpty() || result?.itemMeta?.hasDisplayName() != true)
                pdc.remove(Glyph.ORIGINAL_ITEM_RENAME)
            else pdc.set(Glyph.ORIGINAL_ITEM_RENAME, PersistentDataType.STRING, inventory.renameText!!)
        }
        runCatching {
            result?.editPersistentDataContainer(pdcTask)
        }.onFailure {
            result?.itemMeta?.persistentDataContainer?.run(pdcTask)
        }

    }

    /*@EventHandler(ignoreCancelled = true)
    fun SignChangeEvent.onSignGlyph() {
        if (!Settings.FORMAT_SIGNS.toBool()) return

        val state = (block.state as Sign)
        val pdc = state.persistentDataContainer
        val sideLines = lines().map { it.serialize() }
        val frontLines = when (side) {
            Side.FRONT -> sideLines
            else -> pdc.getOrDefault(ORIGINAL_SIGN_FRONT_LINES, CustomDataTypes.STRING_LIST, List(4) {""})
        }
        val backLines = when (side) {
            Side.BACK -> sideLines
            else -> pdc.getOrDefault(ORIGINAL_SIGN_BACK_LINES, CustomDataTypes.STRING_LIST, List(4) {""})
        }

        pdc.set(ORIGINAL_SIGN_FRONT_LINES, CustomDataTypes.STRING_LIST, frontLines)
        pdc.set(ORIGINAL_SIGN_BACK_LINES, CustomDataTypes.STRING_LIST, backLines)
        state.update(true)

        lines().toList().forEachIndexed { index, s ->
            line(index, GlyphHandlers.escapeGlyphs(s ?: return@forEachIndexed, player).transformGlyphs(player.locale()))
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun PlayerOpenSignEvent.onSignEdit() {
        if (cause == PlayerOpenSignEvent.Cause.PLACE) return

        sign.persistentDataContainer.get(
            when (sign.getInteractableSideFor(player)) {
                Side.FRONT -> ORIGINAL_SIGN_FRONT_LINES
                Side.BACK -> ORIGINAL_SIGN_BACK_LINES
            }, DataType.asList(DataType.STRING)
        )?.forEachIndexed { index, s ->
            sign.getSide(side).line(index, s.deserialize())
        }
        sign.update(true)
        isCancelled = true
        SchedulerUtils.runTaskLater(2L) {
            player.openSign(sign, side)
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun PlayerEditBookEvent.onBookEdit() {
        if (!isSigning || !Settings.FORMAT_BOOKS.toBool()) return
        newBookMeta = newBookMeta.apply {
            pages(pages().map {
                GlyphHandlers.unescapeGlyphTags(GlyphHandlers.escapeGlyphs(it, player).transformGlyphs(player.locale()))
            })
        }
    }*/
}
