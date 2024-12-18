package com.nexomc.nexo.mechanics.furniture

import com.mineinabyss.idofront.util.toColor
import com.nexomc.nexo.utils.EnumUtils.toEnumOrElse
import com.nexomc.nexo.utils.VectorUtils.quaternionfFromString
import com.nexomc.nexo.utils.VectorUtils.vector3fFromString
import com.nexomc.nexo.utils.logs.Logs
import org.bukkit.Color
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.Display
import org.bukkit.entity.Display.Billboard
import org.bukkit.entity.Display.Brightness
import org.bukkit.entity.Entity
import org.bukkit.entity.ItemDisplay
import org.bukkit.entity.ItemDisplay.ItemDisplayTransform
import org.joml.Quaternionf
import org.joml.Vector3f
import java.util.*

class FurnitureProperties() {
    private var glowColor: Color?
    private var viewRange: Float?
    private var brightness: Brightness?
    private var displayTransform: ItemDisplayTransform
    private var trackingRotation: Billboard?
    private var shadowStrength: Float?
    private var shadowRadius: Float?
    private var displayWidth = 0f
    private var displayHeight = 0f
    private var scale: Vector3f
    private var translation: Vector3f
    private var leftRotation: Quaternionf
    private var rightRotation: Quaternionf

    constructor(configSection: ConfigurationSection?) : this() {
        if (configSection == null) return
        val itemID = configSection.parent!!.parent!!.parent!!.name
        glowColor = configSection.getString("glow_color", "")!!.toColor()
        viewRange = configSection.getDouble("view_range").toFloat().takeUnless { it == 0f }
        shadowStrength = configSection.getDouble("shadow_strength").toFloat().takeUnless { it == 0f }
        shadowRadius = configSection.getDouble("shadow_radius").toFloat().takeUnless { it == 0f }
        displayWidth = configSection.getDouble("display_width", 0.0).toFloat()
        displayHeight = configSection.getDouble("display_height", 0.0).toFloat()

        displayTransform = configSection.getString("display_transform")?.toEnumOrElse(ItemDisplayTransform::class.java) { transform ->
            Logs.logError("Use of illegal ItemDisplayTransform <i>$transform</i> in furniture <gold>$itemID")
            Logs.logWarn("Allowed ones are: <gold>${ItemDisplayTransform.entries.joinToString { it.name }}")
            Logs.logWarn("Setting transform to <i>${ItemDisplayTransform.NONE}</i> for furniture: <gold>$itemID")
            ItemDisplayTransform.NONE
        } ?: ItemDisplayTransform.NONE

        val isFixed: Boolean = displayTransform == ItemDisplayTransform.FIXED
        scale = configSection.getString("scale")?.let { vector3fFromString(it, if (isFixed) 0.5f else 1f) } ?: if (isFixed) Vector3f(0.5f, 0.5f, 0.5f) else Vector3f(1f,1f,1f)
        translation = configSection.getString("translation")?.let { vector3fFromString(it, 0f) } ?: Vector3f()
        leftRotation = configSection.getString("left_rotation")?.let { quaternionfFromString(it, 0f) } ?: Quaternionf()
        rightRotation = configSection.getString("right_rotation")?.let { quaternionfFromString(it, 0f) } ?: Quaternionf()

        trackingRotation = configSection.getString("tracking_rotation")?.toEnumOrElse(Billboard::class.java) { tracking ->
            Logs.logError("Use of illegal tracking-rotation $tracking in $itemID furniture.")
            Logs.logError("Allowed ones are: ${Billboard.entries.joinToString { it.name }}")
            Logs.logWarn("Set tracking-rotation to ${Billboard.FIXED} for $itemID")
            Billboard.FIXED
        } ?: Billboard.FIXED

        brightness = configSection.getConfigurationSection("brightness")?.let { Brightness(it.getInt("block_light"), it.getInt("sky_light")) }
    }

    init {
        this.displayTransform = ItemDisplayTransform.NONE
        this.scale = Vector3f(1f, 1f, 1f)
        this.translation = Vector3f()
        this.leftRotation = Quaternionf()
        this.rightRotation = Quaternionf()
        this.shadowRadius = null
        this.shadowStrength = null
        this.brightness = null
        this.trackingRotation = null
        this.viewRange = null
        this.glowColor = null
    }

    fun glowColor(): Optional<Color> {
        return Optional.ofNullable(glowColor)
    }

    fun viewRange(): Optional<Float> {
        return Optional.ofNullable(viewRange)
    }


    fun brightness(): Optional<Display.Brightness> {
        return Optional.ofNullable(brightness)
    }

    fun displayTransform(): ItemDisplayTransform {
        return displayTransform
    }

    val isFixedTransform: Boolean
        get() = displayTransform == ItemDisplayTransform.FIXED

    val isNoneTransform: Boolean
        get() = displayTransform == ItemDisplayTransform.NONE


    fun trackingRotation(): Optional<Billboard> {
        return Optional.ofNullable(trackingRotation)
    }

    fun shadowStrength(): Optional<Float> {
        return Optional.ofNullable(shadowStrength)
    }

    fun shadowRadius(): Optional<Float> {
        return Optional.ofNullable(shadowRadius)
    }

    fun displayWidth(): Float {
        return displayWidth
    }

    fun displayHeight(): Float {
        return displayHeight
    }

    fun scale(): Vector3f {
        return scale
    }

    fun translation(): Vector3f {
        return translation
    }

    fun leftRotation(): Quaternionf {
        return leftRotation
    }

    fun rightRotation(): Quaternionf {
        return rightRotation
    }


    fun ensureSameDisplayProperties(entity: Entity): Boolean {
        if (entity !is ItemDisplay) return false
        entity.itemDisplayTransform = displayTransform
        entity.billboard = trackingRotation ?: Billboard.FIXED
        entity.brightness = brightness ?: Brightness(0, 0)
        entity.shadowRadius = shadowRadius ?: 0f
        entity.shadowStrength = shadowStrength ?: 0f
        entity.viewRange = viewRange ?: 0f
        entity.transformation.scale.set(scale)
        entity.transformation.translation.set(Objects.requireNonNullElse(translation, Vector3f()))

        return true
    }
}
