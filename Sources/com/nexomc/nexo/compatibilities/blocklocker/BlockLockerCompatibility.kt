package com.nexomc.nexo.compatibilities.blocklocker

import com.nexomc.nexo.compatibilities.CompatibilityProvider
import nl.rutgerkok.blocklocker.BlockLockerAPIv2
import nl.rutgerkok.blocklocker.impl.BlockLockerPluginImpl

class BlockLockerCompatibility : CompatibilityProvider<BlockLockerPluginImpl>() {
    init {
        BlockLockerAPIv2.getPlugin().chestSettings.extraProtectables.removeIf { it is BlockLockerProtection }
        BlockLockerAPIv2.getPlugin().chestSettings.extraProtectables += BlockLockerProtection()
    }
}
