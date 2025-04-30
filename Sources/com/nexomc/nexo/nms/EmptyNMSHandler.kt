package com.nexomc.nexo.nms

import com.nexomc.nexo.nms.IPacketHandler.EmptyPacketHandler
import com.nexomc.nexo.utils.Colorable
import com.nexomc.nexo.utils.wrappers.PotionEffectTypeWrapper
import org.bukkit.Color
import org.bukkit.FireworkEffect
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.entity.Player
import org.bukkit.event.Listener
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.FireworkEffectMeta
import org.bukkit.inventory.meta.LeatherArmorMeta
import org.bukkit.inventory.meta.MapMeta
import org.bukkit.inventory.meta.PotionMeta
import org.bukkit.potion.PotionEffect

class EmptyNMSHandler(override val pluginConverter: IPluginConverter = IPluginConverter.EmptyPluginConverter()) : NMSHandler {
    override val packDispatchListener: Listener = object : Listener {}
    override fun packetHandler(): IPacketHandler = EmptyPacketHandler()

    override fun noteblockUpdatesDisabled(): Boolean = false
    override fun tripwireUpdatesDisabled(): Boolean = false
    override fun chorusplantUpdateDisabled(): Boolean = false

    override fun copyItemNBTTags(oldItem: ItemStack, newItem: ItemStack) = newItem

    override fun correctBlockStates(player: Player, slot: EquipmentSlot, itemStack: ItemStack, target: Block, blockFace: BlockFace) = null

    override fun applyMiningEffect(player: Player) {
        player.addPotionEffect(PotionEffect(PotionEffectTypeWrapper.MINING_FATIGUE, -1, Int.MAX_VALUE, false, false, false))
    }

    override fun removeMiningEffect(player: Player) {
        player.removePotionEffect(PotionEffectTypeWrapper.MINING_FATIGUE)
    }

    override fun noteBlockInstrument(block: Block): String {
        return "block.note_block.harp"
    }

    override fun resourcepackFormat() = 55
    override fun datapackFormat() = 71

    override fun asColorable(itemStack: ItemStack): Colorable? {
        return when (val meta = itemStack.itemMeta) {
            is LeatherArmorMeta -> object : Colorable {
                override var color: Color?
                    get() = meta.color
                    set(value) {
                        meta.setColor(value)
                        itemStack.setItemMeta(meta)
                    }
            }

            is PotionMeta -> object : Colorable {
                override var color: Color?
                    get() = meta.color
                    set(value) {
                        meta.color = value
                        itemStack.setItemMeta(meta)
                    }
            }

            is MapMeta -> object : Colorable {
                override var color: Color?
                    get() = meta.color
                    set(value) {
                        meta.color = value
                        itemStack.setItemMeta(meta)
                    }
            }

            is FireworkEffectMeta -> object : Colorable {
                override var color: Color?
                    get() = meta.effect?.colors?.firstOrNull()
                    set(value) {
                        meta.effect = FireworkEffect.builder()
                            .withColor(setOf(value ?: meta.effect?.colors ?: listOf(Color.GRAY)))
                            .with(meta.effect?.type ?: FireworkEffect.Type.BALL)
                            .withFade(meta.effect?.fadeColors ?: emptyList<Color>())
                            .trail(meta.effect?.hasTrail() ?: false)
                            .flicker(meta.effect?.hasFlicker() ?: false)
                            .build()
                        itemStack.setItemMeta(meta)
                    }
            }

            else -> null
        }
    }
}
