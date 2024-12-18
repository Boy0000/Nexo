package com.nexomc.nexo.utils.customarmor

import com.nexomc.nexo.configs.Settings
import com.nexomc.nexo.utils.VersionUtil
import com.nexomc.nexo.utils.logs.Logs
import java.util.*

enum class CustomArmorType {
    NONE, TRIMS, COMPONENT;

    companion object {

        val itemIdRegex = Regex(".*_(helmet|chestplate|leggings|boots)")

        @JvmStatic
        val setting: CustomArmorType = fromString(Settings.CUSTOM_ARMOR_TYPE.toString())

        fun fromString(type: String): CustomArmorType {
            return runCatching {
                val customArmorType = valueOf(type.uppercase(Locale.getDefault()))
                if (!VersionUtil.atleast("1.21.2") && customArmorType == COMPONENT) {
                    Logs.logError("Component based custom armor is only supported in 1.21.2 and above.")
                    throw IllegalArgumentException()
                } else if (!VersionUtil.atleast("1.20") && customArmorType == TRIMS) {
                    Logs.logError("Trim based custom armor is only supported in 1.20 and above.")
                    throw IllegalArgumentException()
                }
                customArmorType
            }.getOrElse {
                Logs.logError("Invalid custom armor type: $type")
                Logs.logError("Defaulting to NONE.")
                NONE
            }
        }
    }
}
