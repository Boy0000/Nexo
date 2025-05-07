package com.nexomc.nexo.utils.blocksounds

import it.unimi.dsi.fastutil.objects.ObjectArrayList
import net.kyori.adventure.key.Key
import org.bukkit.configuration.ConfigurationSection
import team.unnamed.creative.sound.SoundEntry
import team.unnamed.creative.sound.SoundEvent
import team.unnamed.creative.sound.SoundRegistry

class BlockSounds(section: ConfigurationSection) {
    val placeSound: String? = getSound(section, "place")
    val breakSound: String? = getSound(section, "break")
    val stepSound: String? = getSound(section, "step")
    val hitSound: String? = getSound(section, "hit")
    val fallSound: String? = getSound(section, "fall")

    val placeVolume: Float = getVolume(section, "place", VANILLA_PLACE_VOLUME)
    val breakVolume: Float = getVolume(section, "break", VANILLA_BREAK_VOLUME)
    val stepVolume: Float = getVolume(section, "step", VANILLA_STEP_VOLUME)
    val hitVolume: Float = getVolume(section, "hit", VANILLA_HIT_VOLUME)
    val fallVolume: Float = getVolume(section, "fall", VANILLA_FALL_VOLUME)

    val placePitch: Float = getPitch(section, "place", VANILLA_PLACE_PITCH)
    val breakPitch: Float = getPitch(section, "break", VANILLA_BREAK_PITCH)
    val stepPitch: Float = getPitch(section, "step", VANILLA_STEP_PITCH)
    val hitPitch: Float = getPitch(section, "hit", VANILLA_HIT_PITCH)
    val fallPitch: Float = getPitch(section, "fall", VANILLA_FALL_PITCH)

    private fun getSound(section: ConfigurationSection, key: String): String? {
        return section.getString("${key}_sound") ?: section.getString("${key}.sound")
    }

    private fun getVolume(section: ConfigurationSection, type: String, defaultValue: Float): Float {
        return section.getConfigurationSection(type)?.getDouble("volume", defaultValue.toDouble())?.toFloat() ?: defaultValue
    }

    private fun getPitch(section: ConfigurationSection, type: String, defaultValue: Float): Float {
        return section.getConfigurationSection(type)?.getDouble("pitch", defaultValue.toDouble())?.toFloat() ?: defaultValue
    }

    fun hasPlaceSound(): Boolean {
        return placeSound != null
    }

    fun hasBreakSound(): Boolean {
        return breakSound != null
    }

    fun hasStepSound(): Boolean {
        return stepSound != null
    }

    fun hasHitSound(): Boolean {
        return hitSound != null
    }

    fun hasFallSound(): Boolean {
        return fallSound != null
    }

    companion object {
        const val VANILLA_STONE_PLACE = "nexo:block.stone.place"
        const val VANILLA_STONE_BREAK = "nexo:block.stone.break"
        const val VANILLA_STONE_HIT = "nexo:block.stone.hit"
        const val VANILLA_STONE_STEP = "nexo:block.stone.step"
        const val VANILLA_STONE_FALL = "nexo:block.stone.fall"
        val NEXO_STONE_SOUNDS: List<String>  = ObjectArrayList(listOf(VANILLA_STONE_PLACE, VANILLA_STONE_BREAK, VANILLA_STONE_HIT, VANILLA_STONE_STEP, VANILLA_STONE_FALL))

        const val VANILLA_WOOD_PLACE = "nexo:block.wood.place"
        const val VANILLA_WOOD_BREAK = "nexo:block.wood.break"
        const val VANILLA_WOOD_HIT = "nexo:block.wood.hit"
        const val VANILLA_WOOD_STEP = "nexo:block.wood.step"
        const val VANILLA_WOOD_FALL = "nexo:block.wood.fall"
        val NEXO_WOOD_SOUNDS: List<String> = ObjectArrayList(listOf(VANILLA_WOOD_PLACE, VANILLA_WOOD_BREAK, VANILLA_WOOD_HIT, VANILLA_WOOD_STEP, VANILLA_WOOD_FALL))

        const val VANILLA_PLACE_VOLUME = 1.0f
        const val VANILLA_PLACE_PITCH = 0.8f
        const val VANILLA_BREAK_VOLUME = 1.0f
        const val VANILLA_BREAK_PITCH = 0.8f
        const val VANILLA_HIT_VOLUME = 0.25f
        const val VANILLA_HIT_PITCH = 0.5f
        const val VANILLA_STEP_VOLUME = 0.15f
        const val VANILLA_STEP_PITCH = 1.0f
        const val VANILLA_FALL_VOLUME = 0.5f
        const val VANILLA_FALL_PITCH = 0.75f

        private val STONE_DIG_ENTRIES = listOf(
            SoundEntry.soundEntry().key(Key.key("dig/stone1")).build(),
            SoundEntry.soundEntry().key(Key.key("dig/stone2")).build(),
            SoundEntry.soundEntry().key(Key.key("dig/stone3")).build(),
            SoundEntry.soundEntry().key(Key.key("dig/stone4")).build()
        )
        private val STONE_STEP_ENTRIES = listOf(
            SoundEntry.soundEntry().key(Key.key("step/stone1")).build(),
            SoundEntry.soundEntry().key(Key.key("step/stone2")).build(),
            SoundEntry.soundEntry().key(Key.key("step/stone3")).build(),
            SoundEntry.soundEntry().key(Key.key("step/stone4")).build(),
            SoundEntry.soundEntry().key(Key.key("step/stone5")).build(),
            SoundEntry.soundEntry().key(Key.key("step/stone6")).build()
        )
        private val WOOD_DIG_ENTRIES = listOf(
            SoundEntry.soundEntry().key(Key.key("dig/wood1")).build(),
            SoundEntry.soundEntry().key(Key.key("dig/wood2")).build(),
            SoundEntry.soundEntry().key(Key.key("dig/wood3")).build(),
            SoundEntry.soundEntry().key(Key.key("dig/wood4")).build()
        )
        private val WOOD_STEP_ENTRIES = listOf(
            SoundEntry.soundEntry().key(Key.key("step/wood1")).build(),
            SoundEntry.soundEntry().key(Key.key("step/wood2")).build(),
            SoundEntry.soundEntry().key(Key.key("step/wood3")).build(),
            SoundEntry.soundEntry().key(Key.key("step/wood4")).build(),
            SoundEntry.soundEntry().key(Key.key("step/wood5")).build(),
            SoundEntry.soundEntry().key(Key.key("step/wood6")).build()
        )

        val VANILLA_STONE_SOUNDS: List<String>  = ObjectArrayList(listOf(
            "minecraft:block.stone.place",
            "minecraft:block.stone.break",
            "minecraft:block.stone.hit",
            "minecraft:block.stone.fall",
            "minecraft:block.stone.step"
        ))
        @JvmField
        val VANILLA_STONE_SOUND_REGISTRY = SoundRegistry.soundRegistry("minecraft", VANILLA_STONE_SOUNDS.map {
            SoundEvent.soundEvent(Key.key(it), true, null, ArrayList())
        })

        val VANILLA_WOOD_SOUNDS: List<String>  = ObjectArrayList(listOf(
            "minecraft:block.wood.place",
            "minecraft:block.wood.break",
            "minecraft:block.wood.hit",
            "minecraft:block.wood.fall",
            "minecraft:block.wood.step"
        ))
        @JvmField
        val VANILLA_WOOD_SOUND_REGISTRY = SoundRegistry.soundRegistry("minecraft", VANILLA_WOOD_SOUNDS.map {
            SoundEvent.soundEvent(Key.key(it), true, null, ArrayList())
        })

        @JvmField
        val NEXO_STONE_SOUND_REGISTRY = SoundRegistry.soundRegistry(
            "nexo", listOf(
                SoundEvent.soundEvent(Key.key("nexo:block.stone.place"), false, "subtitles.block.generic.place", STONE_DIG_ENTRIES),
                SoundEvent.soundEvent(Key.key("nexo:block.stone.break"), false, "subtitles.block.generic.break", STONE_DIG_ENTRIES),
                SoundEvent.soundEvent(Key.key("nexo:block.stone.hit"), false, "subtitles.block.generic.hit", STONE_STEP_ENTRIES),
                SoundEvent.soundEvent(Key.key("nexo:block.stone.fall"), false, null, STONE_STEP_ENTRIES),
                SoundEvent.soundEvent(Key.key("nexo:block.stone.step"), false, "subtitles.block.generic.footsteps", STONE_STEP_ENTRIES)
            )
        )

        @JvmField
        val NEXO_WOOD_SOUND_REGISTRY = SoundRegistry.soundRegistry(
            "nexo", listOf(
                SoundEvent.soundEvent(Key.key("nexo:block.wood.place"), false, "subtitles.block.generic.place", WOOD_DIG_ENTRIES),
                SoundEvent.soundEvent(Key.key("nexo:block.wood.break"), false, "subtitles.block.generic.break", WOOD_DIG_ENTRIES),
                SoundEvent.soundEvent(Key.key("nexo:block.wood.hit"), false, "subtitles.block.generic.hit", WOOD_STEP_ENTRIES),
                SoundEvent.soundEvent(Key.key("nexo:block.wood.fall"), false, null, WOOD_STEP_ENTRIES),
                SoundEvent.soundEvent(Key.key("nexo:block.wood.step"), false, "subtitles.block.generic.footsteps", WOOD_STEP_ENTRIES)
            )
        )
    }
}
