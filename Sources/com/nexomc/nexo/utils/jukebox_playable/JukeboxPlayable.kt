package com.nexomc.nexo.utils.jukebox_playable

import com.google.gson.JsonObject
import com.nexomc.nexo.utils.AdventureUtils
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import org.bukkit.configuration.ConfigurationSection
import team.unnamed.creative.sound.SoundEvent

class JukeboxPlayable(
    val comparatorOutput: Int,
    val range: Int?,
    val lengthInSeconds: Float,
    val description: Component,
    val soundId: Key,
) {

    constructor(jukeboxSection: ConfigurationSection, soundId: Key) : this(
        jukeboxSection.getInt("comparator_output", 15),
        jukeboxSection.getInt("range").takeIf { it > 0 },
        jukeboxSection.getDouble("length_in_seconds").toFloat().coerceAtLeast(1f),
        AdventureUtils.MINI_MESSAGE.deserialize(jukeboxSection.getString("description", "").toString()),
        soundId
    )

    constructor(jukeboxMap: Map<String, Any>, soundId: Key) : this(
        jukeboxMap["comparator_output"]?.toString()?.toIntOrNull() ?: 15,
        jukeboxMap["range"]?.toString()?.toIntOrNull()?.takeIf { it > 0 },
        jukeboxMap["length_in_seconds"]?.toString()?.toFloatOrNull()?.coerceAtLeast(1f) ?: 1f,
        AdventureUtils.MINI_MESSAGE.deserialize(jukeboxMap["description"].toString()),
        soundId
    )

    val jukeboxJson = JsonObject().apply {
        add("sound_event", JsonObject().apply {
            addProperty("sound_id", soundId.asString())
            range?.also { addProperty("range", it) }
        })
        add("description", JsonObject().apply {
            addProperty("text", AdventureUtils.LEGACY_SERIALIZER.serialize(description))
        })
        addProperty("length_in_seconds", lengthInSeconds.coerceAtLeast(1f))
        addProperty("comparator_output", comparatorOutput.coerceIn(0..15))
    }.toString()
}