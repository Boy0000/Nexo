package com.nexomc.nexo.mechanics.furniture

import com.nexomc.nexo.commands.toColor
import com.nexomc.nexo.utils.EnumUtils.toEnumOrElse
import com.nexomc.nexo.utils.VectorUtils.quaternionfFromString
import com.nexomc.nexo.utils.VectorUtils.vector3fFromString
import com.nexomc.nexo.utils.logs.Logs
import com.nexomc.nexo.utils.rootId
import org.bukkit.Color
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.Display.Billboard
import org.bukkit.entity.Display.Brightness
import org.bukkit.entity.ItemDisplay.ItemDisplayTransform
import org.joml.Quaternionf
import org.joml.Vector3f

class FurnitureProperties(
    val glowColor: Color? = null,
    val viewRange: Float? = null,
    val brightness: Brightness? = null,
    val displayTransform: ItemDisplayTransform = ItemDisplayTransform.NONE,
    _scale: Vector3f = Vector3f(1f,1f,1f),
    val trackingRotation: Billboard? = null,
    val shadowStrength: Float? = null,
    val shadowRadius: Float? = null,
    val displayWidth: Float = 0f,
    val displayHeight: Float = 0f,
    val translation: Vector3f = Vector3f(),
    val leftRotation: Quaternionf = Quaternionf(),
    val rightRotation: Quaternionf = Quaternionf(),
) {

    var scale = _scale
        private set

    constructor(configSection: ConfigurationSection) : this(
            glowColor = configSection.getString("glow_color", "")!!.toColor(),
                    viewRange = configSection.getDouble("view_range").toFloat().takeUnless { it == 0f },
                    shadowStrength = configSection.getDouble("shadow_strength").toFloat().takeUnless { it == 0f },
                    shadowRadius = configSection.getDouble("shadow_radius").toFloat().takeUnless { it == 0f },
                    displayWidth = configSection.getDouble("display_width", 0.0).toFloat(),
        displayHeight = configSection.getDouble("display_height", 0.0).toFloat(),

        displayTransform = configSection.getString("display_transform")?.toEnumOrElse(ItemDisplayTransform::class.java) { transform ->
            val itemID = configSection.rootId
            Logs.logError("Use of illegal ItemDisplayTransform <i>$transform</i> in furniture <gold>$itemID")
            Logs.logWarn("Allowed ones are: <gold>${ItemDisplayTransform.entries.joinToString { it.name }}")
            Logs.logWarn("Setting transform to <i>${ItemDisplayTransform.NONE}</i> for furniture: <gold>$itemID")
            ItemDisplayTransform.NONE
        } ?: ItemDisplayTransform.NONE,

        translation = configSection.getString("translation")?.let { vector3fFromString(it, 0f) } ?: Vector3f(),
        leftRotation = configSection.getString("left_rotation")?.let { quaternionfFromString(it, 0f) } ?: Quaternionf(),
        rightRotation = configSection.getString("right_rotation")?.let { quaternionfFromString(it, 0f) } ?: Quaternionf(),

        trackingRotation = configSection.getString("tracking_rotation")?.toEnumOrElse(Billboard::class.java) { tracking ->
            val itemID = configSection.rootId
            Logs.logError("Use of illegal tracking-rotation $tracking in $itemID furniture.")
            Logs.logError("Allowed ones are: ${Billboard.entries.joinToString { it.name }}")
            Logs.logWarn("Set tracking-rotation to ${Billboard.FIXED} for $itemID")
            Billboard.FIXED
        } ?: Billboard.FIXED,

        brightness = configSection.getConfigurationSection("brightness")?.let { Brightness(it.getInt("block_light"), it.getInt("sky_light")) }
    ) {
        scale = configSection.getString("scale")?.let { vector3fFromString(it, if (isFixedTransform) 0.5f else 1f) } ?: if (isFixedTransform) Vector3f(0.5f, 0.5f, 0.5f) else Vector3f(1f,1f,1f)
    }

    val isFixedTransform: Boolean
        get() = displayTransform == ItemDisplayTransform.FIXED

    val isNoneTransform: Boolean
        get() = displayTransform == ItemDisplayTransform.NONE
}
