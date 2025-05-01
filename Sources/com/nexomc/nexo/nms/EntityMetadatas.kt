package com.nexomc.nexo.nms

import org.joml.Quaternionf
import org.joml.Vector3f

object EntityMetadatas {

    const val ENTITY_SHARED_DATA = 0
    const val ENTITY_CUSTOM_NAME = 2
    const val ENTITY_CUSTOM_NAME_VISIBLE = 3
    const val ENTITY_SILENT = 4
    const val MOB_SHARED_DATA = 15

    const val SHULKER_ATTACHED_FACE = 16
    const val SHULKER_SHIELD_HEIGHT = 17
    const val SHULKER_COLOR = 18

    const val DISPLAY_INTERPOLATION_DELAY = 8
    const val DISPLAY_TRANSFORMATION_INTERPOLATION_DURATION = 9
    const val DISPLAY_POS_INTERPOLATION_DURATION = 10
    const val DISPLAY_TRANSLATION = 11
    const val DISPLAY_SCALE_ID = 12
    const val DISPLAY_QUATERNION_LEFT = 13
    const val DISPLAY_QUATERNION_RIGHT = 14
    const val DISPLAY_BILLBOARD_ID = 15
    const val DISPLAY_BRIGHTNESS_ID = 16
    const val DISPLAY_VIEW_RANGE_ID = 17
    const val DISPLAY_SHADOW_RADIUS_ID = 18
    const val DISPLAY_SHADOW_STRENGTH_ID = 19
    const val DISPLAY_WIDTH_ID = 20
    const val DISPLAY_HEIGHT_ID = 21
    const val DISPLAY_GLOW_ID = 22

    const val ITEM_DISPLAY_ITEM_ID = 23
    const val ITEM_DISPLAY_TRANSFORM_ID = 24

    const val INTERACTION_WIDTH_ID = 8
    const val INTERACTION_HEIGHT_ID = 9

    val DEFAULT_TRANSLATION = Vector3f(0f,0f,0f)
    val DEFAULT_SCALE = Vector3f(1f,1f,1f)
    val DEFAULT_ROTATION = Quaternionf()
}