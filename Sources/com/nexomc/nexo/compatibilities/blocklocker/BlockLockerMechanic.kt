package com.nexomc.nexo.compatibilities.blocklocker

import com.nexomc.nexo.utils.EnumUtils.toEnumOrElse
import com.nexomc.nexo.utils.logs.Logs
import nl.rutgerkok.blocklocker.ProtectionType
import org.bukkit.configuration.ConfigurationSection

class BlockLockerMechanic(section: ConfigurationSection) {
    val canProtect: Boolean = section.getBoolean("can_protect", true)
    val protectionType: ProtectionType =
        section.getString("protection_type")?.toEnumOrElse(ProtectionType::class.java) {
            Logs.logError("Invalid protection type $it for BlockLocker mechanic in item ${section.parent?.parent.toString()}, defaulting to CONTAINER")
            ProtectionType.CONTAINER
        } ?: ProtectionType.CONTAINER
}
