package com.nexomc.nexo.mechanics.furniture.connectable

import com.jeff_media.morepersistentdatatypes.DataType
import com.nexomc.nexo.NexoPlugin
import com.nexomc.nexo.api.NexoFurniture
import com.nexomc.nexo.api.NexoItems
import com.nexomc.nexo.items.ItemBuilder
import com.nexomc.nexo.mechanics.furniture.BlockLocation
import com.nexomc.nexo.mechanics.furniture.IFurniturePacketManager
import com.nexomc.nexo.utils.*
import com.nexomc.nexo.utils.KeyUtils.appendSuffix
import com.nexomc.nexo.utils.logs.Logs
import net.kyori.adventure.key.Key
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.block.BlockFace
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.ItemDisplay

enum class ConnectableItemType {
    ITEM_MODEL, ITEM;

    fun validate(itemId: String): ConnectableItemType {
        if (this == ITEM_MODEL) {
            if (VersionUtil.atleast("1.21.2")) return ITEM_MODEL
            else {
                Logs.logWarn("Item $itemId attempted to use ITEM_MODEL Connectable-type, but this requires a 1.21.2+ server")
                Logs.logWarn("Defaulting to using ITEM method")
            }
        }

        return ITEM
    }

    companion object {
        val defaultType by lazy { if (VersionUtil.atleast("1.21.2")) ITEM_MODEL else ITEM }
    }
}

data class ConnectableMechanic(
    val type: ConnectableItemType,
    val default: Key?,
    val straight: Key?,
    val left: Key?,
    val right: Key?,
    val inner: Key?,
    val outer: Key?,
) {

    private val defaultItem by lazy {
        when (type) {
            ConnectableItemType.ITEM_MODEL -> ItemBuilder(Material.LEATHER_HORSE_ARMOR).setItemModel(default)
            else -> NexoItems.itemFromId(default?.value()) ?: ItemBuilder(Material.BARRIER)
        }
    }
    private val straightItem by lazy { buildDisplayItem(straight, "straight") }
    private val leftEndItem by lazy { buildDisplayItem(left, "left") }
    private val rightEndItem by lazy { buildDisplayItem(right, "right") }
    private val innerCornerItem by lazy { buildDisplayItem(inner, "inner") }
    private val outerCornerItem by lazy { buildDisplayItem(outer, "outer") }

    private fun buildDisplayItem(typeKey: Key?, suffix: String): ItemBuilder {
        return when (type) {
            ConnectableItemType.ITEM_MODEL -> when {
                typeKey != null -> ItemBuilder(Material.LEATHER_HORSE_ARMOR).setItemModel(typeKey)
                else -> defaultItem.clone().setBlockStates(mapOf(BLOCKSTATE_KEY to suffix))
            }
            else -> NexoItems.itemFromId(typeKey?.value()) ?: defaultItem
        }
    }

    constructor(section: ConfigurationSection) : this(
        type = section.getEnum("type", ConnectableItemType::class.java)?.validate(section.rootId) ?: ConnectableItemType.defaultType,

        default = section.getKey("default") ?: section.rootSection.getKey("Components.item_model") ?: section.rootSection.getKey("Pack.model"),
        straight = section.getKey("straight") ?: section.getKey("default")?.appendSuffix("_straight"),
        left = section.getKey("left") ?: section.getKey("default")?.appendSuffix("_left"),
        right = section.getKey("right") ?: section.getKey("default")?.appendSuffix("_right"),
        inner = section.getKey("inner") ?: section.getKey("default")?.appendSuffix("_inner"),
        outer = section.getKey("outer") ?: section.getKey("default")?.appendSuffix("_outer"),
    )

    fun updateState(baseEntity: ItemDisplay) {
        val type = determineConnectableShape(baseEntity)
        baseEntity.persistentDataContainer.set(CONNECTABLE_KEY, ConnectType.dataType, type)
        IFurniturePacketManager.furnitureBaseMap[baseEntity.uniqueId]?.refreshItem(baseEntity)
    }

    fun updateSurrounding(baseEntity: ItemDisplay) {
        SchedulerUtils.runTaskLater(2L) {
            val world = baseEntity.world
            val (leftLoc, rightLoc) = BlockLocation(baseEntity.location).add(-1, 0, 0) to BlockLocation(baseEntity.location).add(1, 0, 0)
            val (aheadLoc, behindLoc) = BlockLocation(baseEntity.location).add(0, 0, -1) to BlockLocation(baseEntity.location).add(0, 0, 1)
            IFurniturePacketManager.baseEntityFromHitbox(leftLoc, world)?.takeIf { it.isValid }?.let(::updateState)
            IFurniturePacketManager.baseEntityFromHitbox(rightLoc, world)?.takeIf { it.isValid }?.let(::updateState)
            IFurniturePacketManager.baseEntityFromHitbox(aheadLoc, world)?.takeIf { it.isValid }?.let(::updateState)
            IFurniturePacketManager.baseEntityFromHitbox(behindLoc, world)?.takeIf { it.isValid }?.let(::updateState)
        }
    }

    private fun determineConnectableShape(baseEntity: ItemDisplay): ConnectType {
        var currentType = baseEntity.persistentDataContainer.get(CONNECTABLE_KEY, ConnectType.dataType)
        val mechanic = NexoFurniture.furnitureMechanic(baseEntity) ?: return ConnectType.DEFAULT
        val world = baseEntity.world

        if (currentType?.isCornerRotated == true) baseEntity.setRotation(baseEntity.yaw - 90f, baseEntity.pitch)

        val aheadFacing = baseEntity.facing
        val behindFacing = aheadFacing.oppositeFace
        val leftFacing = when (aheadFacing) { // Left but we use it for a block relative so opposite
            BlockFace.NORTH -> BlockFace.WEST
            BlockFace.EAST -> BlockFace.NORTH
            BlockFace.SOUTH -> BlockFace.EAST
            BlockFace.WEST -> BlockFace.SOUTH
            else -> BlockFace.WEST
        }.oppositeFace

        val leftBlock = BlockLocation(baseEntity.location.add(leftFacing.direction))
        val rightBlock = BlockLocation(baseEntity.location.add(leftFacing.oppositeFace.direction))
        val aheadBlock = BlockLocation(baseEntity.location.add(aheadFacing.direction))
        val behindBlock = BlockLocation(baseEntity.location.add(behindFacing.direction))

        val leftEntity = IFurniturePacketManager.baseEntityFromHitbox(leftBlock, world)?.takeIf { it.isValid && NexoFurniture.furnitureMechanic(it) == mechanic }
        val rightEntity = IFurniturePacketManager.baseEntityFromHitbox(rightBlock, world)?.takeIf { it.isValid && NexoFurniture.furnitureMechanic(it) == mechanic }
        val aheadEntity = IFurniturePacketManager.baseEntityFromHitbox(aheadBlock, world)?.takeIf { it.isValid && NexoFurniture.furnitureMechanic(it) == mechanic }
        val behindEntity = IFurniturePacketManager.baseEntityFromHitbox(behindBlock, world)?.takeIf { it.isValid && NexoFurniture.furnitureMechanic(it) == mechanic }

        val leftState = leftEntity?.persistentDataContainer?.get(CONNECTABLE_KEY, ConnectType.dataType)
        val rightState = rightEntity?.persistentDataContainer?.get(CONNECTABLE_KEY, ConnectType.dataType)
        val aheadState = aheadEntity?.persistentDataContainer?.get(CONNECTABLE_KEY, ConnectType.dataType)
        val behindState = behindEntity?.persistentDataContainer?.get(CONNECTABLE_KEY, ConnectType.dataType)

        if (leftState != null && leftEntity.facing == aheadFacing) currentType = ConnectType.RIGHT_END
        if (rightState != null && rightEntity.facing == aheadFacing) currentType = ConnectType.LEFT_END
        if (aheadState?.isCorner == false && aheadFacing != aheadEntity.facing) {
            if (rightState != null) currentType = ConnectType.INNER_CORNER
            if (leftState != null) currentType = ConnectType.INNER_CORNER_ROTATED
        } else if (behindState?.isCorner == false && behindFacing != behindEntity.facing) {
            currentType = when {
                rightState != null -> ConnectType.OUTER_CORNER_ROTATED
                leftState == null && behindEntity.facing == leftFacing -> ConnectType.OUTER_CORNER_ROTATED
                else -> ConnectType.OUTER_CORNER
            }
        }

        if (leftState != null && rightState != null && (leftEntity.facing == rightEntity.facing || leftState.isCorner || rightState.isCorner))
            currentType = ConnectType.STRAIGHT
        if (leftState == null && rightState == null && (behindState == null || behindFacing == behindEntity.facing || behindFacing == behindEntity.facing.oppositeFace))
            currentType = ConnectType.DEFAULT

        if (currentType?.isCornerRotated == true) baseEntity.setRotation(baseEntity.yaw + 90f, baseEntity.pitch)

        return currentType ?: ConnectType.DEFAULT
    }

    fun displayedItem(baseEntity: ItemDisplay): ItemBuilder {
        val item = when (baseEntity.persistentDataContainer.getOrDefault(CONNECTABLE_KEY, ConnectType.dataType, ConnectType.DEFAULT)) {
            ConnectType.STRAIGHT -> straightItem
            ConnectType.LEFT_END -> leftEndItem
            ConnectType.RIGHT_END -> rightEndItem
            ConnectType.INNER_CORNER, ConnectType.INNER_CORNER_ROTATED -> innerCornerItem
            ConnectType.OUTER_CORNER, ConnectType.OUTER_CORNER_ROTATED -> outerCornerItem
            else -> defaultItem
        }

        return item
    }

    companion object {
        val CONNECTABLE_KEY = NamespacedKey(NexoPlugin.instance(), BLOCKSTATE_KEY)
        private const val BLOCKSTATE_KEY = "connectable"
    }

    enum class ConnectType {
        DEFAULT, STRAIGHT, LEFT_END, RIGHT_END, OUTER_CORNER, OUTER_CORNER_ROTATED, INNER_CORNER, INNER_CORNER_ROTATED;

        val isCorner get() = this == OUTER_CORNER || this == INNER_CORNER || this == OUTER_CORNER_ROTATED || this == INNER_CORNER_ROTATED
        val isCornerRotated get() = this == OUTER_CORNER_ROTATED || this == INNER_CORNER_ROTATED

        companion object {
            val dataType = DataType.asEnum(ConnectType::class.java)
        }
    }
}