package com.nexomc.nexo.compatibilities.blocklocker

import com.nexomc.nexo.api.NexoBlocks
import com.nexomc.nexo.mechanics.furniture.BlockLocation
import com.nexomc.nexo.mechanics.furniture.IFurniturePacketManager
import nl.rutgerkok.blocklocker.ProtectableBlocksSettings
import nl.rutgerkok.blocklocker.ProtectionType
import org.bukkit.block.Block

class BlockLockerProtection : ProtectableBlocksSettings {

    override fun canProtect(block: Block): Boolean {
        val blocklocker = NexoBlocks.customBlockMechanic(block.blockData)?.blockLocker
            ?: block.takeIf(Block::isEmpty)?.let(::BlockLocation)?.let(IFurniturePacketManager::mechanicFromHitbox)?.blockLocker ?: return false

        return blocklocker.canProtect
    }

    override fun canProtect(type: ProtectionType, block: Block): Boolean {
        val blocklocker = NexoBlocks.customBlockMechanic(block.blockData)?.blockLocker
            ?: block.takeIf(Block::isEmpty)?.let(::BlockLocation)?.let(IFurniturePacketManager::mechanicFromHitbox)?.blockLocker ?: return false

        return blocklocker.canProtect && blocklocker.protectionType == type
    }
}