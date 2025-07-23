package com.nexomc.nexo

import com.nexomc.nexo.dialog.NexoDialogs.registerDialogs
import com.nexomc.nexo.jukebox_songs.NexoJukeboxSong.registerJukeboxSongs
import com.nexomc.nexo.paintings.NexoPaintings.registerCustomPaintings
import com.nexomc.nexo.tags.NexoTags.registerTags
import com.nexomc.nexo.utils.VersionUtil.atleast
import io.papermc.paper.plugin.bootstrap.BootstrapContext
import io.papermc.paper.plugin.bootstrap.PluginBootstrap

class NexoBootstrap : PluginBootstrap {

    companion object {
        var bootsStrung: Boolean = false
    }

    override fun bootstrap(context: BootstrapContext) {
        runCatching {
            if (atleast("1.21.1")) registerTags(context)
            if (atleast("1.21.3")) registerCustomPaintings(context)
            if (atleast("1.21.6")) registerJukeboxSongs(context)
            if (atleast("1.21.7")) registerDialogs(context)
            bootsStrung = true
        }.onFailure {
            context.logger.error("Failed to bootstrap Nexo: ${it.message}")
        }
    }
}