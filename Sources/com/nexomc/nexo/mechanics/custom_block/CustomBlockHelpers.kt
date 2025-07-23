package com.nexomc.nexo.mechanics.custom_block

import com.nexomc.nexo.api.NexoBlocks
import com.nexomc.nexo.api.events.custom_block.NexoBlockPlaceEvent
import com.nexomc.nexo.api.events.custom_block.chorusblock.NexoChorusBlockPlaceEvent
import com.nexomc.nexo.api.events.custom_block.noteblock.NexoNoteBlockPlaceEvent
import com.nexomc.nexo.api.events.custom_block.stringblock.NexoStringBlockPlaceEvent
import com.nexomc.nexo.mechanics.custom_block.chorusblock.ChorusBlockMechanic
import com.nexomc.nexo.mechanics.custom_block.noteblock.NoteBlockMechanic
import com.nexomc.nexo.mechanics.custom_block.noteblock.NoteMechanicHelpers
import com.nexomc.nexo.mechanics.custom_block.stringblock.StringBlockMechanic
import com.nexomc.nexo.nms.NMSHandlers
import com.nexomc.nexo.utils.BlockHelpers
import com.nexomc.nexo.utils.BlockHelpers.isReplaceable
import com.nexomc.nexo.utils.BlockHelpers.isStandingInside
import com.nexomc.nexo.utils.BlockHelpers.toCenterBlockLocation
import com.nexomc.nexo.utils.EventUtils.call
import com.nexomc.nexo.utils.InteractionResult
import com.nexomc.nexo.utils.drops.Drop
import com.nexomc.protectionlib.ProtectionLib
import org.bukkit.GameEvent
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.Tag
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.block.Sign
import org.bukkit.block.data.BlockData
import org.bukkit.block.sign.Side
import org.bukkit.entity.FallingBlock
import org.bukkit.entity.Player
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

object CustomBlockHelpers {
    fun makePlayerPlaceBlock(
        player: Player, hand: EquipmentSlot, item: ItemStack,
        placedAgainst: Block, face: BlockFace, newMechanic: CustomBlockMechanic?, newData: BlockData?
    ) {
        val target: Block
        val itemMaterial = item.type
        val worldHeightRange = placedAgainst.world.let { it.minHeight..it.maxHeight }

        if (isReplaceable(placedAgainst)) target = placedAgainst
        else {
            target = placedAgainst.getRelative(face)

            if (newMechanic != null && !isReplaceable(target)) return
            else if (Tag.DOORS.isTagged(target.type)) return
        }

        val (blockBelow, blockAbove) = target.getRelative(BlockFace.DOWN) to target.getRelative(BlockFace.UP)
        val oldData = target.blockData
        var result: InteractionResult? = null
        if (newMechanic == null) {
            //TODO Fix boats, currently Item#use in BoatItem calls PlayerInteractEvent
            // thus causing a StackOverflow, find a workaround
            if (Tag.ITEMS_BOATS.isTagged(itemMaterial)) return
            result = NMSHandlers.handler().correctBlockStates(player, hand, item, target.getRelative(face.oppositeFace), face)
            (target.state as? Sign)?.takeIf { it.type != oldData.material }?.let {
                player.openSign(it, Side.FRONT)
            }
        }

        when {
            newData != null -> if (result == null) target.setBlockData(newData, false)
            else target.setBlockData(target.blockData, false)
        }

        val blockPlaceEvent = BlockPlaceEvent(target, target.state, placedAgainst, item, player, true, hand)

        if (!ProtectionLib.canBuild(player, target.location)) blockPlaceEvent.isCancelled = true
        if (target.y !in worldHeightRange) blockPlaceEvent.isCancelled = true

        when {
            newMechanic != null -> {
                if (newMechanic !is StringBlockMechanic && isStandingInside(player, target))
                    blockPlaceEvent.isCancelled = true
            }
            else -> {
                if (!itemMaterial.isBlock && itemMaterial != Material.FLINT_AND_STEEL && itemMaterial != Material.FIRE_CHARGE && itemMaterial != Material.STRING) return
                if (target.blockData == oldData) blockPlaceEvent.isCancelled = true
                if (result == null) blockPlaceEvent.isCancelled = true
            }
        }

        // Handling placing against noteblock
        NexoBlocks.noteBlockMechanic(placedAgainst.blockData)?.let {
            if (!player.isSneaking && (it.isStorage() || it.hasClickActions())) blockPlaceEvent.isCancelled = true
        }

        if (newMechanic is StringBlockMechanic && newMechanic.isTall) when {
            blockAbove.type !in BlockHelpers.REPLACEABLE_BLOCKS -> blockPlaceEvent.isCancelled = true
            blockAbove.y !in worldHeightRange -> blockPlaceEvent.isCancelled = true
            else -> blockAbove.type = Material.TRIPWIRE
        }

        // Call the event and check if it is cancelled, if so reset BlockData
        if (!blockPlaceEvent.call() || !blockPlaceEvent.canBuild()) {
            target.setBlockData(oldData, false)
            return
        }

        if (newMechanic != null) {
            NexoBlocks.place(newMechanic.itemID, target.location)
            val customBlockPlaceEvent = when (newMechanic) {
                is NoteBlockMechanic -> NexoNoteBlockPlaceEvent(newMechanic, target, player, item, hand)
                is StringBlockMechanic -> NexoStringBlockPlaceEvent(newMechanic, target, player, item, hand)
                is ChorusBlockMechanic -> NexoChorusBlockPlaceEvent(newMechanic, target, player, item, hand)
                else -> NexoBlockPlaceEvent(newMechanic, target, player, item, hand)
            }

            if (!customBlockPlaceEvent.call()) return target.setBlockData(oldData, false)

            // Handle Falling NoteBlock-Mechanic blocks
            when {
                newMechanic is NoteBlockMechanic -> {
                    when {
                        newMechanic.isFalling() && blockBelow.type.isAir() -> {
                            val fallingLocation = toCenterBlockLocation(target.location)
                            NexoBlocks.remove(target.location, overrideDrop = Drop.emptyDrop())
                            if (fallingLocation.getNearbyEntitiesByType(FallingBlock::class.java, 0.25).isEmpty()) {
                                val falling = target.world.spawn(fallingLocation, FallingBlock::class.java)
                                falling.blockData = newData!!
                                falling.persistentDataContainer.set(NoteBlockMechanic.FALLING_KEY, PersistentDataType.BYTE, 1)
                            }
                            NoteMechanicHelpers.handleFallingNexoBlockAbove(target)
                        }
                        else -> {
                            target.type = Material.AIR
                            target.setBlockData(newData!!, true)
                        }
                    }
                }
                newData != null -> {
                    target.type = Material.AIR
                    target.setBlockData(newData, true)
                }
            }

            if (player.gameMode != GameMode.CREATIVE) item.amount -= 1

            player.swingHand(hand)
        }
        target.world.sendGameEvent(player, GameEvent.BLOCK_PLACE, target.location.toVector())
    }
}
