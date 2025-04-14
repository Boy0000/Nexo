package com.nexomc.nexo.configs

import com.nexomc.nexo.utils.KeyUtils
import com.nexomc.nexo.utils.jukebox_playable.JukeboxPlayable
import com.nexomc.nexo.utils.logs.Logs
import com.nexomc.nexo.utils.safeCast
import org.bukkit.configuration.file.YamlConfiguration
import team.unnamed.creative.sound.SoundEntry
import team.unnamed.creative.sound.SoundEvent
import team.unnamed.creative.sound.SoundRegistry

class SoundManager(soundConfig: YamlConfiguration) {
    val jukeboxPlayables = mutableListOf<JukeboxPlayable>()
    private val generateSounds: Boolean = soundConfig.getBoolean("settings.generate_sounds")
    private val customSoundRegistries: LinkedHashSet<SoundRegistry> = soundConfig.getMapList("sounds").safeCast<List<Map<String, Any>>>()?.takeIf { generateSounds }?.let { parseCustomSounds(it) } ?: linkedSetOf()

    private fun parseCustomSounds(sounds: List<Map<String, Any>>): LinkedHashSet<SoundRegistry> {
        return linkedSetOf<SoundRegistry>().apply {
            sounds.mapNotNull { soundEntry ->
                val soundId = soundEntry["id"].safeCast<String>()?.let(KeyUtils::parseKey) ?: run {
                    Logs.logWarn("Skipping sound entry with missing ID")
                    return@mapNotNull null
                }
                val soundKeys = (soundEntry["sound"].safeCast<String>()?.let { listOf(it) }
                    ?: soundEntry["sounds"].safeCast<List<String>>() ?: emptyList()).map {
                        KeyUtils.parseKey(it.removeSuffix(".ogg"))
                }

                val soundEntries = when (val type = (soundEntry["type"].safeCast<String>())?.uppercase() ?: "FILE") {
                    "FILE" -> soundKeys.map { soundKey ->
                        SoundEntry.soundEntry()
                            .key(soundKey).stream(soundEntry["stream"]?.toString()?.toBooleanStrictOrNull() ?: false)
                            .volume(soundEntry["volume"]?.toString()?.toFloatOrNull()?.coerceAtLeast(0f) ?: 1.0f)
                            .pitch(soundEntry["pitch"]?.toString()?.toFloatOrNull()?.coerceAtLeast(0f) ?: 1.0f)
                            .weight(soundEntry["weight"].safeCast<Int>() ?: 1)
                            .preload(soundEntry["preload"]?.toString()?.toBooleanStrictOrNull() ?: false)
                            .attenuationDistance(soundEntry["attenuation_distance"]?.toString()?.toIntOrNull() ?: 16)
                            .build()
                    }
                    "EVENT" -> when(val referenceId = soundEntry["reference_id"].safeCast<String>()?.let(KeyUtils::parseKey)) {
                        null -> {
                            Logs.logWarn("Skipping EVENT type sound entry without reference ID for $soundId")
                            return@mapNotNull null
                        }
                        else -> listOf(SoundEntry.soundEntry().key(referenceId).build())
                    }
                    else -> {
                        Logs.logWarn("Skipping unknown sound type $type for $soundId")
                        return@mapNotNull null
                    }
                }

                soundEntry["jukebox_playable"].safeCast<Map<String, Any>>()?.also { jukeboxPlayable ->
                    jukeboxPlayables += JukeboxPlayable(jukeboxPlayable, soundId)
                }
                SoundEvent.soundEvent()
                    .key(soundId).sounds(soundEntries)
                    .replace(soundEntry["replace"].safeCast<Boolean>() ?: SoundEvent.DEFAULT_REPLACE)
                    .subtitle(soundEntry["subtitle"]?.toString())
                    .build()
            }.groupBy { it.key().namespace() }.forEach { (namespace, soundEvents) ->
                this += SoundRegistry.soundRegistry(namespace, soundEvents)
            }
        }
    }


    fun customSoundRegistries() = LinkedHashSet(customSoundRegistries)
}
