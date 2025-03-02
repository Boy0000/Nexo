package com.nexomc.nexo.mechanics.farming.harvesting

import com.nexomc.nexo.api.NexoItems
import com.nexomc.nexo.utils.EventUtils.call
import com.nexomc.nexo.utils.ItemUtils
import com.nexomc.nexo.utils.to
import io.th0rgal.protectionlib.ProtectionLib
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.data.Ageable
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerItemDamageEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.Damageable

class HarvestingMechanicListener : Listener {
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    fun PlayerInteractEvent.onPlayerInteract() {
        val location = clickedBlock?.location ?: return
        val (item, itemID) = player.inventory.itemInMainHand.let { it to NexoItems.idFromItem(it) }
        val mechanic = HarvestingMechanicFactory.instance()?.getMechanic(itemID) ?: return

        if (hand != EquipmentSlot.HAND || action != Action.RIGHT_CLICK_BLOCK) return

        mechanic.getTimer(player).let { it.takeIf { it.isFinished }?.reset() ?: return it.sendToPlayer(player) }

        var durabilityDamage = 0
        getNearbyBlocks(location, mechanic.radius, mechanic.height).forEach { block: Block ->
            val ageable = block.blockData as? Ageable ?: return@forEach

            if (ageable.age == ageable.maximumAge && ProtectionLib.canBreak(player, block.location) && ProtectionLib.canBuild(player, block.location)) {
                ageable.age = 0
                block.blockData = ageable
                val (sound, volume, pitch) = block.blockData.soundGroup.let { it.breakSound to it.volume to it.pitch }
                block.world.playSound(block.location, sound, volume, pitch)
                when (block.type) {
                    Material.WHEAT -> ItemUtils.itemStacks(Material.WHEAT, Material.WHEAT_SEEDS)
                    Material.BEETROOTS -> ItemUtils.itemStacks(Material.BEETROOT, Material.BEETROOT_SEEDS)
                    else -> block.drops
                }.forEach { itemStack -> giveItem(player, itemStack, location) }
                durabilityDamage++
            }
        }

        if (mechanic.lowerItemDurability && item.itemMeta is Damageable && durabilityDamage > 0)
            PlayerItemDamageEvent(player, item, durabilityDamage).call()
    }

    private fun giveItem(player: Player, item: ItemStack, location: Location) {
        if (player.inventory.firstEmpty() != -1) {
            player.inventory.addItem(item).entries.forEach { itemStack ->
                player.world.dropItem(player.location, itemStack.value)
            }
        } else player.world.dropItemNaturally(location.toCenterLocation(), item)
    }

    private fun getNearbyBlocks(location: Location, radius: Int, height: Int): List<Block> {
        val blocks = mutableListOf<Block>()

        var x = location.blockX - radius.floorDiv(2)
        while (x <= location.blockX + radius.floorDiv(2)) {
            var y = location.blockY - height.floorDiv(2)
            while (y <= location.blockY + height.floorDiv(2)) {
                var z = location.blockZ - radius.floorDiv(2)
                while (z <= location.blockZ + radius.floorDiv(2)) {
                    blocks += location.world.getBlockAt(x, y, z)
                    z++
                }
                y++
            }
            x++
        }
        return blocks
    }
}
