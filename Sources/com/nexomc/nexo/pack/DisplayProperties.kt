package com.nexomc.nexo.pack

import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap
import org.bukkit.Material
import team.unnamed.creative.base.Vector3Float
import team.unnamed.creative.model.ItemTransform

object DisplayProperties {
    var SHIELD = Object2ObjectLinkedOpenHashMap<ItemTransform.Type, ItemTransform>()
    var BOW: Object2ObjectLinkedOpenHashMap<ItemTransform.Type, ItemTransform>
    var CROSSBOW: Object2ObjectLinkedOpenHashMap<ItemTransform.Type, ItemTransform>
    var CHEST: Object2ObjectLinkedOpenHashMap<ItemTransform.Type, ItemTransform>
    var LARGE_AMETHYST_BUD: Object2ObjectLinkedOpenHashMap<ItemTransform.Type, ItemTransform>
    var SMALL_DRIPLEAF: Object2ObjectLinkedOpenHashMap<ItemTransform.Type, ItemTransform>
    var BIG_DRIPLEAF: Object2ObjectLinkedOpenHashMap<ItemTransform.Type, ItemTransform>
    var HANGING_ROOTS: Object2ObjectLinkedOpenHashMap<ItemTransform.Type, ItemTransform>
    var DRAGON_HEAD: Object2ObjectLinkedOpenHashMap<ItemTransform.Type, ItemTransform>
    var CONDUIT: Object2ObjectLinkedOpenHashMap<ItemTransform.Type, ItemTransform>
    var DECORATED_POT: Object2ObjectLinkedOpenHashMap<ItemTransform.Type, ItemTransform>

    fun fromMaterial(material: Material): Object2ObjectLinkedOpenHashMap<ItemTransform.Type, ItemTransform> {
        return when (material) {
            Material.SHIELD -> SHIELD
            Material.BOW -> BOW
            Material.CROSSBOW -> CROSSBOW
            Material.CHEST -> CHEST
            Material.LARGE_AMETHYST_BUD -> LARGE_AMETHYST_BUD
            Material.SMALL_DRIPLEAF -> SMALL_DRIPLEAF
            Material.BIG_DRIPLEAF -> BIG_DRIPLEAF
            Material.HANGING_ROOTS -> HANGING_ROOTS
            Material.DRAGON_HEAD -> DRAGON_HEAD
            Material.CONDUIT -> CONDUIT
            Material.DECORATED_POT -> DECORATED_POT
            else -> Object2ObjectLinkedOpenHashMap()
        }
    }

    init {
        SHIELD[ItemTransform.Type.THIRDPERSON_LEFTHAND] = ItemTransform.transform(
            Vector3Float(0f, 90f, 0f), Vector3Float(10f, 6f, 12f), Vector3Float(1f, 1f, 1f)
        )
        SHIELD[ItemTransform.Type.THIRDPERSON_RIGHTHAND] = ItemTransform.transform(
            Vector3Float(0f, 90f, 0f), Vector3Float(10f, 6f, -4f), Vector3Float(1f, 1f, 1f)
        )
        SHIELD[ItemTransform.Type.FIRSTPERSON_LEFTHAND] = ItemTransform.transform(
            Vector3Float(0f, 180f, 5f), Vector3Float(10f, 0f, -10f), Vector3Float(1.25f, 1.25f, 1.25f)
        )
        SHIELD[ItemTransform.Type.FIRSTPERSON_RIGHTHAND] = ItemTransform.transform(
            Vector3Float(0f, 180f, 5f), Vector3Float(-10f, 2f, -10f), Vector3Float(1.25f, 1.25f, 1.25f)
        )
        SHIELD[ItemTransform.Type.GUI] = ItemTransform.transform(
            Vector3Float(15f, -25f, -5f), Vector3Float(2f, 3f, 0f), Vector3Float(0.65f, 0.65f, 0.65f)
        )
        SHIELD[ItemTransform.Type.FIXED] = ItemTransform.transform(
            Vector3Float(0f, 180f, 0f), Vector3Float(-4.5f, 4.5f, -5f), Vector3Float(0.55f, 0.55f, 0.55f)
        )
        SHIELD[ItemTransform.Type.GROUND] = ItemTransform.transform(
            Vector3Float(0f, 0f, 0f), Vector3Float(2f, 4f, 2f), Vector3Float(0.25f, 0.25f, 0.25f)
        )

        BOW = Object2ObjectLinkedOpenHashMap()
        BOW[ItemTransform.Type.THIRDPERSON_LEFTHAND] = ItemTransform.transform(
            Vector3Float(-80f, -280f, 40f), Vector3Float(-1f, -2f, 2.5f), Vector3Float(0.9f, 0.9f, 0.9f)
        )
        BOW[ItemTransform.Type.THIRDPERSON_RIGHTHAND] = ItemTransform.transform(
            Vector3Float(-80f, 260f, -40f), Vector3Float(-1f, -2f, 2.5f), Vector3Float(0.9f, 0.9f, 0.9f)
        )
        BOW[ItemTransform.Type.FIRSTPERSON_LEFTHAND] = ItemTransform.transform(
            Vector3Float(0f, 90f, -25f), Vector3Float(1.13f, 3.2f, 1.13f), Vector3Float(0.68f, 0.68f, 0.68f)
        )
        BOW[ItemTransform.Type.FIRSTPERSON_RIGHTHAND] = ItemTransform.transform(
            Vector3Float(0f, -90f, 25f), Vector3Float(1.13f, 3.2f, 1.13f), Vector3Float(0.68f, 0.68f, 0.68f)
        )

        CROSSBOW = Object2ObjectLinkedOpenHashMap()
        CROSSBOW[ItemTransform.Type.THIRDPERSON_LEFTHAND] = ItemTransform.transform(
            Vector3Float(-90f, 0f, 30f), Vector3Float(2f, 0.1f, -3f), Vector3Float(0.9f, 0.9f, 0.9f)
        )
        CROSSBOW[ItemTransform.Type.THIRDPERSON_RIGHTHAND] = ItemTransform.transform(
            Vector3Float(-90f, 0f, -60f), Vector3Float(2f, 0.1f, -3f), Vector3Float(0.9f, 0.9f, 0.9f)
        )
        CROSSBOW[ItemTransform.Type.FIRSTPERSON_LEFTHAND] = ItemTransform.transform(
            Vector3Float(-90f, 0f, 35f), Vector3Float(1.13f, 3.2f, 1.13f), Vector3Float(0.68f, 0.68f, 0.68f)
        )
        CROSSBOW[ItemTransform.Type.FIRSTPERSON_RIGHTHAND] = ItemTransform.transform(
            Vector3Float(-90f, 0f, -55f), Vector3Float(1.13f, 3.2f, 1.13f), Vector3Float(0.68f, 0.68f, 0.68f)
        )

        CHEST = Object2ObjectLinkedOpenHashMap()
        CHEST[ItemTransform.Type.GUI] = ItemTransform.transform(
            Vector3Float(30f, 45f, 0f), Vector3Float(0f, 0f, 0f), Vector3Float(0.625f, 0.625f, 0.625f)
        )
        CHEST[ItemTransform.Type.GROUND] = ItemTransform.transform(
            Vector3Float(0f, 0f, 0f), Vector3Float(0f, 3f, 0f), Vector3Float(0.25f, 0.25f, 0.25f)
        )
        CHEST[ItemTransform.Type.HEAD] = ItemTransform.transform(
            Vector3Float(0f, 180f, 0f), Vector3Float(0f, 0f, 0f), Vector3Float(1f, 1f, 1f)
        )
        CHEST[ItemTransform.Type.FIXED] = ItemTransform.transform(
            Vector3Float(0f, 180f, 0f), Vector3Float(0f, 0f, 0f), Vector3Float(0.5f, 0.5f, 0.5f)
        )
        CHEST[ItemTransform.Type.THIRDPERSON_RIGHTHAND] = ItemTransform.transform(
            Vector3Float(75f, 315f, 0f), Vector3Float(0f, 2.5f, 0f), Vector3Float(0.375f, 0.375f, 0.375f)
        )
        CHEST[ItemTransform.Type.FIRSTPERSON_RIGHTHAND] = ItemTransform.transform(
            Vector3Float(0f, 315f, 0f), Vector3Float(0f, 0f, 0f), Vector3Float(0.4f, 0.4f, 0.4f)
        )

        LARGE_AMETHYST_BUD = Object2ObjectLinkedOpenHashMap()
        LARGE_AMETHYST_BUD[ItemTransform.Type.FIXED] = ItemTransform.transform(
            Vector3Float(0f, 0f, 0f), Vector3Float(0f, 4f, 0f), Vector3Float(1f, 1f, 1f)
        )

        SMALL_DRIPLEAF = Object2ObjectLinkedOpenHashMap()
        SMALL_DRIPLEAF[ItemTransform.Type.THIRDPERSON_RIGHTHAND] = ItemTransform.transform(
            Vector3Float(0f, 0f, 0f), Vector3Float(0f, 4f, 1f), Vector3Float(0.55f, 0.55f, 0.55f)
        )
        SMALL_DRIPLEAF[ItemTransform.Type.FIRSTPERSON_RIGHTHAND] = ItemTransform.transform(
            Vector3Float(0f, 45f, 0f), Vector3Float(0f, 3.2f, 0f), Vector3Float(0.4f, 0.4f, 0.4f)
        )

        BIG_DRIPLEAF = Object2ObjectLinkedOpenHashMap()
        BIG_DRIPLEAF[ItemTransform.Type.GUI] = ItemTransform.transform(
            Vector3Float(30f, 225f, 0f), Vector3Float(0f, -2f, 0f), Vector3Float(0.625f, 0.625f, 0.625f)
        )
        BIG_DRIPLEAF[ItemTransform.Type.FIXED] = ItemTransform.transform(
            Vector3Float(0f, 0f, 0f), Vector3Float(0f, 0f, -1f), Vector3Float(0.5f, 0.5f, 0.5f)
        )
        BIG_DRIPLEAF[ItemTransform.Type.THIRDPERSON_RIGHTHAND] = ItemTransform.transform(
            Vector3Float(0f, 0f, 0f), Vector3Float(0f, 1f, 0f), Vector3Float(0.55f, 0.55f, 0.55f)
        )
        BIG_DRIPLEAF[ItemTransform.Type.FIRSTPERSON_RIGHTHAND] = ItemTransform.transform(
            Vector3Float(0f, 0f, 0f), Vector3Float(1.13f, 0f, 1.13f), Vector3Float(0.68f, 0.68f, 0.68f)
        )

        HANGING_ROOTS = Object2ObjectLinkedOpenHashMap()
        HANGING_ROOTS[ItemTransform.Type.THIRDPERSON_RIGHTHAND] = ItemTransform.transform(
            Vector3Float(0f, 0f, 0f), Vector3Float(0f, 0f, 1f), Vector3Float(0.55f, 0.55f, 0.55f)
        )
        HANGING_ROOTS[ItemTransform.Type.FIRSTPERSON_RIGHTHAND] = ItemTransform.transform(
            Vector3Float(0f, -90f, 25f), Vector3Float(1.13f, 0f, 1.13f), Vector3Float(0.68f, 0.68f, 0.68f)
        )

        DRAGON_HEAD = Object2ObjectLinkedOpenHashMap()
        DRAGON_HEAD[ItemTransform.Type.GUI] = ItemTransform.transform(
            Vector3Float(30f, 45f, 0f), Vector3Float(-2f, 2f, 0f), Vector3Float(0.6f, 0.6f, 0.6f)
        )
        DRAGON_HEAD[ItemTransform.Type.THIRDPERSON_RIGHTHAND] = ItemTransform.transform(
            Vector3Float(0f, 180f, 0f), Vector3Float(0f, -1f, 2f), Vector3Float(0.5f, 0.5f, 0.5f)
        )

        CONDUIT = Object2ObjectLinkedOpenHashMap()
        CONDUIT[ItemTransform.Type.GUI] = ItemTransform.transform(
            Vector3Float(30f, 45f, 0f), Vector3Float(0f, 0f, 0f), Vector3Float(1f, 1f, 1f)
        )
        CONDUIT[ItemTransform.Type.GROUND] = ItemTransform.transform(
            Vector3Float(0f, 0f, 0f), Vector3Float(0f, 3f, 0f), Vector3Float(0.5f, 0.5f, 0.5f)
        )
        CONDUIT[ItemTransform.Type.HEAD] = ItemTransform.transform(
            Vector3Float(0f, 180f, 0f), Vector3Float(0f, 0f, 0f), Vector3Float(1f, 1f, 1f)
        )
        CONDUIT[ItemTransform.Type.FIXED] = ItemTransform.transform(
            Vector3Float(0f, 180f, 0f), Vector3Float(0f, 0f, 0f), Vector3Float(1f, 1f, 1f)
        )
        CONDUIT[ItemTransform.Type.THIRDPERSON_RIGHTHAND] = ItemTransform.transform(
            Vector3Float(75f, 315f, 0f), Vector3Float(0f, 2.5f, 0f), Vector3Float(0.5f, 0.5f, 0.5f)
        )
        CONDUIT[ItemTransform.Type.FIRSTPERSON_RIGHTHAND] = ItemTransform.transform(
            Vector3Float(0f, 315f, 0f), Vector3Float(0f, 0f, 0f), Vector3Float(0.8f, 0.8f, 0.8f)
        )

        DECORATED_POT = Object2ObjectLinkedOpenHashMap()
        DECORATED_POT[ItemTransform.Type.THIRDPERSON_RIGHTHAND] = ItemTransform.transform(
            Vector3Float(0f, 90f, 0f), Vector3Float(0f, 2f, 0.5f), Vector3Float(0.375f, 0.375f, 0.375f)
        )
        DECORATED_POT[ItemTransform.Type.FIRSTPERSON_RIGHTHAND] = ItemTransform.transform(
            Vector3Float(0f, 90f, 0f), Vector3Float(0f, 0f, 0f), Vector3Float(0.375f, 0.375f, 0.375f)
        )
        DECORATED_POT[ItemTransform.Type.GUI] = ItemTransform.transform(
            Vector3Float(30f, 45f, 0f), Vector3Float(0f, 0f, 0f), Vector3Float(0.6f, 0.6f, 0.6f)
        )
        DECORATED_POT[ItemTransform.Type.GROUND] = ItemTransform.transform(
            Vector3Float(0f, 0f, 0f), Vector3Float(0f, 1f, 0f), Vector3Float(0.25f, 0.25f, 0.25f)
        )
        DECORATED_POT[ItemTransform.Type.HEAD] = ItemTransform.transform(
            Vector3Float(0f, 180f, 0f), Vector3Float(0f, 16f, 0f), Vector3Float(1.5f, 1.5f, 1.5f)
        )
        DECORATED_POT[ItemTransform.Type.FIXED] = ItemTransform.transform(
            Vector3Float(0f, 180f, 0f), Vector3Float(0f, 0f, 0f), Vector3Float(0.5f, 0.5f, 0.5f)
        )
    }
}
