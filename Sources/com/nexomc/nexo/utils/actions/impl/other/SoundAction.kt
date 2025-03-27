package com.nexomc.nexo.utils.actions.impl.other

import com.nexomc.nexo.utils.printOnFailure
import me.gabytm.util.actions.actions.Action
import me.gabytm.util.actions.actions.ActionMeta
import me.gabytm.util.actions.actions.Context
import net.kyori.adventure.key.Key
import net.kyori.adventure.sound.Sound
import org.bukkit.entity.Player

class SoundAction(meta: ActionMeta<Player?>) : Action<Player>(meta) {
    private val source = getMeta().getProperty("source", Sound.Source.MASTER) { Sound.Source.NAMES.value(it.lowercase()) }
    private val volume = getMeta().getProperty("volume", 1f) { it.toFloatOrNull() }
    private val pitch = getMeta().getProperty("pitch", 1f) { it.toFloatOrNull() }

    override fun run(player: Player, context: Context<Player>) {
        val parsed = meta.getParsedData(player, context)

        runCatching {
            player.playSound(Sound.sound(Key.key(parsed), source, volume, pitch))
        }.printOnFailure()
    }

    companion object {
        const val IDENTIFIER = "sound"
    }
}
