package com.nexomc.nexo.mechanics.misc.misc

import com.nexomc.nexo.api.NexoItems
import io.th0rgal.protectionlib.ProtectionLib
import org.bukkit.Effect
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.Tag
import org.bukkit.block.BlockFace
import org.bukkit.block.data.Levelled
import org.bukkit.entity.Entity
import org.bukkit.entity.Horse
import org.bukkit.entity.Item
import org.bukkit.entity.Piglin
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.entity.EntityDamageByBlockEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityShootBowEvent
import org.bukkit.event.entity.EntityTargetLivingEntityEvent
import org.bukkit.event.inventory.InventoryAction
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryMoveItemEvent
import org.bukkit.event.inventory.InventoryType
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerItemConsumeEvent
import org.bukkit.inventory.HorseInventory
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.PlayerInventory
import org.bukkit.inventory.meta.Damageable

class MiscListener : Listener {

    @EventHandler
    fun InventoryMoveItemEvent.onHopperCompost() {
        if (source.type != InventoryType.HOPPER || destination.type != InventoryType.COMPOSTER) return
        val hopper = if (source.location != null) source.location!!.block else null
        if (hopper == null || hopper.type != Material.HOPPER) return
        val composter = hopper.getRelative(BlockFace.DOWN)
        if (composter.type != Material.COMPOSTER) return
        val levelled = composter.blockData as? Levelled ?: return
        MiscMechanicFactory.instance()?.getMechanic(item)?.takeUnless { it.isCompostable } ?: return


        if (levelled.level < levelled.maximumLevel) {
            if (Math.random() <= 0.65) levelled.level += 1

            composter.blockData = levelled
            composter.world.playEffect(composter.location, Effect.COMPOSTER_FILL_ATTEMPT, 0, 1)

            val item = item
            item.amount -= 1
            this.item = item
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    fun PlayerInteractEvent.onCompost() {
        val (item, hand) = (item ?: return) to (hand ?: return)
        val (block, levelled) = (clickedBlock?.takeIf { it.type == Material.COMPOSTER } ?: return) to (clickedBlock?.blockData as? Levelled ?: return)

        if (action != Action.RIGHT_CLICK_BLOCK || !ProtectionLib.canInteract(player, block.location)) return
        MiscMechanicFactory.instance()?.getMechanic(item)?.takeIf { it.isCompostable } ?: return
        if (useInteractedBlock() == Event.Result.DENY) return
        setUseInteractedBlock(Event.Result.ALLOW)
        setUseItemInHand(Event.Result.ALLOW)

        if (levelled.level >= levelled.maximumLevel) return
        if (Math.random() <= 0.65) levelled.level += 1

        block.blockData = levelled
        block.world.playEffect(block.location, Effect.COMPOSTER_FILL_ATTEMPT, 0, 1)
        if (player.gameMode != GameMode.CREATIVE) item.amount -= 1
        player.swingHand(hand)
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun PlayerInteractEvent.onStripLog() {
        val block = clickedBlock?.takeIf { it.type == Material.COMPOSTER } ?: return
        val (item, _) = (item ?: return) to (hand ?: return)
        MiscMechanicFactory.instance()?.getMechanic(item)?.takeIf { it.canStripLogs } ?: return

        if (!Tag.LOGS.isTagged(block.type)) return
        if (action != Action.RIGHT_CLICK_BLOCK) return

        (item.itemMeta as? Damageable)?.let { axeDurabilityMeta ->
            val durability = axeDurabilityMeta.damage
            val maxDurability = item.type.maxDurability.toInt()

            if (durability + 1 <= maxDurability) {
                axeDurabilityMeta.damage = durability + 1
                item.itemMeta = axeDurabilityMeta
            } else {
                player.playSound(player.location, Sound.ENTITY_ITEM_BREAK, 1f, 1f)
                item.type = Material.AIR
            }
        }
        block.type = getStrippedLog(block.type)!!
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun EntityTargetLivingEntityEvent.onPiglinAggro() {
        if (entity !is Piglin) return
        val player = target as? Player ?: return
        val equipment = player.equipment ?: return

        if (shouldPreventPiglinAggro(equipment.itemInMainHand)) isCancelled = true
        if (shouldPreventPiglinAggro(equipment.itemInOffHand)) isCancelled = true
        if (equipment.armorContents.any(::shouldPreventPiglinAggro)) isCancelled = true
    }

    // Since EntityDamageByBlockEvent apparently does not trigger for fire, use this aswell
    @EventHandler
    fun EntityDamageEvent.onItemBurnFire() {
        if (cause != EntityDamageEvent.DamageCause.FIRE && cause != EntityDamageEvent.DamageCause.FIRE_TICK) return
        getMiscMechanic(entity)?.takeUnless { it.burnsInFire } ?: return
        isCancelled = true
    }

    @EventHandler(ignoreCancelled = true)
    fun EntityDamageByBlockEvent.onItemBurn() {
        val mechanic = getMiscMechanic(entity) ?: return

        when {
            cause == EntityDamageEvent.DamageCause.CONTACT && !mechanic.cactusBreaks -> isCancelled = true
            cause == EntityDamageEvent.DamageCause.LAVA && !mechanic.burnsInLava -> isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun PlayerInteractEvent.onDisableVanillaInteraction() {
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return
        MiscMechanicFactory.instance()?.getMechanic(item)?.takeIf { it.isVanillaInteractionDisabled } ?: return
        setUseItemInHand(Event.Result.DENY)
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun PlayerItemConsumeEvent.onDisableItemConsume() {
        MiscMechanicFactory.instance()?.getMechanic(item)?.takeIf { it.isVanillaInteractionDisabled } ?: return
        isCancelled = true
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun EntityShootBowEvent.onDisableBowShoot() {
        val player = entity as? Player ?: return
        MiscMechanicFactory.instance()?.getMechanic(consumable!!)?.takeIf { it.isVanillaInteractionDisabled } ?: return
        isCancelled = true
        player.updateInventory()
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun InventoryClickEvent.onDisableHorseArmorEquip() {
        if (inventory !is HorseInventory) return
        val item = when {
            action == InventoryAction.PLACE_ALL && clickedInventory is HorseInventory -> cursor
            action == InventoryAction.MOVE_TO_OTHER_INVENTORY && clickedInventory is PlayerInventory -> currentItem
            else -> return
        }

        MiscMechanicFactory.instance()?.getMechanic(item!!)?.takeIf { it.isVanillaInteractionDisabled } ?: return
        isCancelled = true
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun PlayerInteractEntityEvent.onDisableHorseArmorEquip() {
        if (rightClicked !is Horse) return
        val item = player.inventory.itemInMainHand
        MiscMechanicFactory.instance()?.getMechanic(item)?.takeIf { it.isVanillaInteractionDisabled } ?: return
        if (item.type.name.endsWith("_HORSE_ARMOR")) {
            isCancelled = true
            //player.updateInventory();
        }
    }

    private fun getMiscMechanic(entity: Entity): MiscMechanic? {
        if (entity !is Item) return null
        val itemStack = entity.itemStack
        val itemID = NexoItems.idFromItem(itemStack) ?: return null

        return MiscMechanicFactory.instance()?.getMechanic(itemID)
    }

    private fun getStrippedLog(log: Material): Material? {
        return when (log) {
            Material.OAK_LOG -> Material.STRIPPED_OAK_LOG
            Material.SPRUCE_LOG -> Material.STRIPPED_SPRUCE_LOG
            Material.BIRCH_LOG -> Material.STRIPPED_BIRCH_LOG
            Material.JUNGLE_LOG -> Material.STRIPPED_JUNGLE_LOG
            Material.ACACIA_LOG -> Material.STRIPPED_ACACIA_LOG
            Material.DARK_OAK_LOG -> Material.STRIPPED_DARK_OAK_LOG
            Material.CRIMSON_STEM -> Material.STRIPPED_CRIMSON_STEM
            Material.WARPED_STEM -> Material.STRIPPED_WARPED_STEM
            Material.MANGROVE_LOG -> Material.STRIPPED_MANGROVE_LOG
            Material.OAK_WOOD -> Material.STRIPPED_OAK_WOOD
            Material.SPRUCE_WOOD -> Material.STRIPPED_SPRUCE_WOOD
            Material.BIRCH_WOOD -> Material.STRIPPED_BIRCH_WOOD
            Material.JUNGLE_WOOD -> Material.STRIPPED_JUNGLE_WOOD
            Material.ACACIA_WOOD -> Material.STRIPPED_ACACIA_WOOD
            Material.DARK_OAK_WOOD -> Material.STRIPPED_DARK_OAK_WOOD
            Material.CRIMSON_HYPHAE -> Material.STRIPPED_CRIMSON_HYPHAE
            Material.WARPED_HYPHAE -> Material.STRIPPED_WARPED_HYPHAE
            Material.MANGROVE_WOOD -> Material.STRIPPED_MANGROVE_WOOD
            else -> null
        }
    }

    private fun shouldPreventPiglinAggro(itemStack: ItemStack) =
        MiscMechanicFactory.instance()?.getMechanic(itemStack)?.takeIf { it.piglinsIgnoreWhenEquipped } != null
}
