package com.nexomc.nexo

import com.nexomc.nexo.dialog.NexoDialogs.registerDialogs
import com.nexomc.nexo.glyphs.GlyphTag
import com.nexomc.nexo.glyphs.ShiftTag
import com.nexomc.nexo.jukebox_songs.NexoJukeboxSong.registerJukeboxSongs
import com.nexomc.nexo.paintings.NexoPaintings.registerCustomPaintings
import com.nexomc.nexo.tags.NexoTags.registerTags
import com.nexomc.nexo.utils.VersionUtil
import io.papermc.paper.plugin.bootstrap.BootstrapContext
import io.papermc.paper.plugin.bootstrap.PluginBootstrap
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver

class NexoBootstrap : PluginBootstrap {

    companion object {
        var bootsStrung: Boolean = false
    }

    override fun bootstrap(context: BootstrapContext) {
        runCatching {
            if (VersionUtil.atleast("1.21.1")) registerTags(context)
            if (VersionUtil.atleast("1.21.3")) registerCustomPaintings(context)
            if (VersionUtil.atleast("1.21.6")) registerJukeboxSongs(context)
            if (VersionUtil.atleast("1.21.7")) registerDialogs(context)

            //injectNexoTagResolvers(context)
            bootsStrung = true
        }.onFailure {
            context.logger.error("Failed to bootstrap Nexo: ${it.message}")
            it.printStackTrace()
        }
    }

    private fun injectNexoTagResolvers(context: BootstrapContext) {
        runCatching {
            val instancesClass = Class.forName("net.kyori.adventure.text.minimessage.MiniMessageImpl\$Instances")
            val miniMessageImpl = instancesClass.getDeclaredField("INSTANCE").apply { isAccessible = true }.get(null) // MiniMessageImpl
            val parser = miniMessageImpl.javaClass.getDeclaredField("parser").apply { isAccessible = true }.get(miniMessageImpl)

            val resolverField = parser.javaClass.getDeclaredField("tagResolver").apply { isAccessible = true }
            val oldResolver = resolverField.get(parser) as TagResolver
            val combinedResolver = TagResolver.resolver(oldResolver, GlyphTag.RESOLVER, ShiftTag.RESOLVER)
            resolverField.set(parser, combinedResolver)
        }.onFailure {
            context.logger.error("Failed to edit MiniMessage resolver: ${it.message}")
            it.printStackTrace()
        }
    }
}