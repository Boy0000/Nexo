package com.nexomc.nexo.compatibilities.blocklocker

import com.nexomc.nexo.api.NexoBlocks
import com.nexomc.nexo.api.NexoFurniture
import com.nexomc.nexo.compatibilities.CompatibilityProvider
import nl.rutgerkok.blocklocker.BlockLockerAPIv2
import nl.rutgerkok.blocklocker.ProtectableBlocksSettings
import nl.rutgerkok.blocklocker.ProtectionType
import nl.rutgerkok.blocklocker.impl.BlockLockerPluginImpl
import org.bukkit.block.Block

class BlockLockerCompatibility : CompatibilityProvider<BlockLockerPluginImpl>() {
    init {
        BlockLockerAPIv2.getPlugin().chestSettings.extraProtectables += object : ProtectableBlocksSettings {
            override fun canProtect(block: Block): Boolean {
                val blocklocker = NexoBlocks.customBlockMechanic(block.blockData)?.blockLocker
                    ?: NexoFurniture.furnitureMechanic(block)?.blockLocker

                return blocklocker?.canProtect == true
            }

            override fun canProtect(type: ProtectionType, block: Block): Boolean {
                val blocklocker = NexoBlocks.customBlockMechanic(block.blockData)?.blockLocker
                    ?: NexoFurniture.furnitureMechanic(block)?.blockLocker ?: return false

                return blocklocker.canProtect && blocklocker.protectionType == type
            }
        }
    }
}
