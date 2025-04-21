package com.nexomc.nexo.compatibilities.blocklocker

import com.nexomc.nexo.api.NexoBlocks
import com.nexomc.nexo.compatibilities.CompatibilityProvider
import com.nexomc.nexo.mechanics.furniture.BlockLocation
import com.nexomc.nexo.mechanics.furniture.IFurniturePacketManager
import nl.rutgerkok.blocklocker.BlockLockerAPIv2
import nl.rutgerkok.blocklocker.ProtectableBlocksSettings
import nl.rutgerkok.blocklocker.ProtectionType
import nl.rutgerkok.blocklocker.impl.BlockLockerPluginImpl
import org.bukkit.block.Block

class BlockLockerCompatibility : CompatibilityProvider<BlockLockerPluginImpl>(), ProtectableBlocksSettings {
    init {
        BlockLockerAPIv2.getPlugin().chestSettings.extraProtectables.removeIf { it is BlockLockerCompatibility }
        BlockLockerAPIv2.getPlugin().chestSettings.extraProtectables += this
    }

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
