package com.nexomc.nexo.mechanics.custom_block.stringblock.sapling

import com.nexomc.nexo.api.NexoBlocks
import com.nexomc.nexo.compatibilities.worldedit.WrappedWorldEdit
import com.nexomc.nexo.utils.BlockHelpers.persistentDataContainer
import com.nexomc.nexo.utils.wrappers.ParticleWrapper
import org.bukkit.Effect
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.persistence.PersistentDataType

class SaplingListener : Listener {
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun PlayerInteractEvent.onBoneMeal() {
        val block = clickedBlock?.takeIf { it.type == Material.TRIPWIRE } ?: return
        val item = item?.takeIf { it.type == Material.BONE_MEAL } ?: return
        val mechanic = NexoBlocks.stringMechanic(block)?.takeIf { it.isSapling() } ?: return
        val sapling = mechanic.sapling()?.takeIf { it.hasSchematic() } ?: return

        if (action != Action.RIGHT_CLICK_BLOCK || hand != EquipmentSlot.HAND) return
        if (sapling.requiresLight() && sapling.minLightLevel > block.lightLevel) return
        if (sapling.requiresWaterSource && sapling.isUnderWater(block)) return
        if (!sapling.canGrowFromBoneMeal || !WrappedWorldEdit.loaded) return

        val selectedSchematic = sapling.selectSchematic() ?: return
        if (!sapling.replaceBlocks && WrappedWorldEdit.blocksInSchematic(block.location, selectedSchematic).isNotEmpty()) return

        if (player.gameMode != GameMode.CREATIVE) item.amount -= 1
        block.world.playEffect(block.location, Effect.BONE_MEAL_USE, 3)

        val pdc = block.persistentDataContainer
        val growthTimeRemains = pdc.getOrDefault(SaplingMechanic.SAPLING_KEY, PersistentDataType.INTEGER, 0) - sapling.boneMealGrowthSpeedup
        block.world.spawnParticle(ParticleWrapper.HAPPY_VILLAGER, block.location, 10, 0.5, 0.5, 0.5)
        player.swingMainHand()
        if (growthTimeRemains <= 0) {
            block.setType(Material.AIR, false)
            if (sapling.hasGrowSound()) player.playSound(block.location, sapling.growSound!!, 1.0f, 0.8f)
            WrappedWorldEdit.pasteSchematic(block.location, selectedSchematic, sapling.replaceBlocks, sapling.copyBiomes, sapling.copyEntities)
        } else pdc.set(SaplingMechanic.SAPLING_KEY, PersistentDataType.INTEGER, growthTimeRemains)
    }
}
