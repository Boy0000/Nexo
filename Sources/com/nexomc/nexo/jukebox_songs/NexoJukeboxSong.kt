package com.nexomc.nexo.jukebox_songs

import com.nexomc.nexo.utils.getKey
import com.nexomc.nexo.utils.jukebox_playable.JukeboxPlayable
import com.nexomc.nexo.utils.sectionList
import io.papermc.paper.plugin.bootstrap.BootstrapContext
import io.papermc.paper.registry.event.RegistryEvents
import io.papermc.paper.registry.keys.JukeboxSongKeys
import org.bukkit.configuration.file.YamlConfiguration

object NexoJukeboxSong {
    @JvmStatic
    fun registerJukeboxSongs(context: BootstrapContext) {
        runCatching {
            val soundsFile = context.dataDirectory.resolve("sounds.yml").toFile().apply { createNewFile() }
            val soundsYaml = runCatching { YamlConfiguration.loadConfiguration(soundsFile) }.getOrNull() ?: return@runCatching
            val sounds = soundsYaml.sectionList("sounds").ifEmpty { return }

            context.lifecycleManager.registerEventHandler(RegistryEvents.JUKEBOX_SONG.compose().newHandler { handler ->
                sounds.forEach { section ->
                    val key = section.getKey("id") ?: return@forEach
                    val jukeboxSection = section.getConfigurationSection("jukebox_playable") ?: return@forEach
                    val jukeboxPlayable = JukeboxPlayable(jukeboxSection, key)

                    handler.registry().register(JukeboxSongKeys.create(key)) { builder ->
                        builder.description(jukeboxPlayable.description)
                            .comparatorOutput(jukeboxPlayable.comparatorOutput)
                            .lengthInSeconds(jukeboxPlayable.lengthInSeconds)
                            .soundEvent { it.empty().location(key).fixedRange(jukeboxPlayable.range?.toFloat()) }
                    }
                }
            })
        }
    }
}