package com.nexomc.nexo.utils.jukebox_playable

import com.nexomc.nexo.NexoPlugin
import com.nexomc.nexo.utils.NexoDatapack
import com.nexomc.nexo.utils.VersionUtil
import com.nexomc.nexo.utils.logs.Logs

class JukeboxPlayableDatapack : NexoDatapack("nexo_jukebox_playables", "Datapack for Custom Music Discs") {

    fun createDatapack() {
        if (VersionUtil.atleast("1.21.6")) return
        writeMCMeta()

        NexoPlugin.instance().soundManager().jukeboxPlayables.ifEmpty { null }?.forEach { jukeboxPlayable ->
            val (namespace, key) = jukeboxPlayable.soundId.let { it.namespace() to it.value() }
            datapackFile.resolve("data/$namespace/jukebox_song/$key.json").apply {
                parentFile.mkdirs()
            }.writeText(jukeboxPlayable.jukeboxJson.toString())
        } ?: return

        when {
            isFirstInstall -> {
                Logs.logError("Nexos's Custom Music Discs datapack could not be found...")
                Logs.logWarn("The first time you add a music-disc you need to restart your server so that the DataPack is enabled...")
                Logs.logWarn("Custom Music Discs will not work, please restart your server once!", true)
            }
            !datapackEnabled -> {
                Logs.logError("Nexos's Custom Music Discs datapack is not enabled...")
                Logs.logWarn("Custom Music Discs will not work, please restart your server!", true)
            }
        }

        enableDatapack(true)
    }
}