package com.nexomc.nexo.mechanics.furniture.connectable

import com.jeff_media.morepersistentdatatypes.DataType
import com.nexomc.nexo.NexoPlugin
import com.nexomc.nexo.api.NexoFurniture
import com.nexomc.nexo.api.NexoItems
import com.nexomc.nexo.items.ItemBuilder
import com.nexomc.nexo.mechanics.furniture.IFurniturePacketManager
import com.nexomc.nexo.utils.KeyUtils.appendSuffix
import com.nexomc.nexo.utils.SchedulerUtils
import com.nexomc.nexo.utils.VersionUtil
import com.nexomc.nexo.utils.blockLocation
import com.nexomc.nexo.utils.getEnum
import com.nexomc.nexo.utils.getKey
import com.nexomc.nexo.utils.logs.Logs
import com.nexomc.nexo.utils.minus
import com.nexomc.nexo.utils.plus
import com.nexomc.nexo.utils.rootId
import com.nexomc.nexo.utils.rootSection
import com.nexomc.nexo.utils.ticks
import net.kyori.adventure.key.Key
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.block.BlockFace
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.Entity
import org.bukkit.entity.ItemDisplay
import kotlin.time.Duration

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
            ConnectableItemType.ITEM_MODEL -> ItemBuilder(Material.PAPER).setItemModel(default)
            else -> NexoItems.itemFromId(default?.value()) ?: ItemBuilder(Material.PAPER)
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
                typeKey != null -> ItemBuilder(Material.PAPER).setItemModel(typeKey)
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

    fun scheduleUpdateState(baseEntity: ItemDisplay, delay: Duration = 1.ticks) {
        SchedulerUtils.launchDelayed(baseEntity, delay) { updateState(baseEntity) }
    }

    fun updateState(baseEntity: ItemDisplay) {
        val type = determineConnectableShape(baseEntity)
        baseEntity.persistentDataContainer.set(CONNECTABLE_KEY, ConnectType.dataType, type)
        IFurniturePacketManager.furnitureBaseMap[baseEntity.uniqueId]?.refreshItem(baseEntity)
    }

    fun updateSurrounding(baseEntity: ItemDisplay) {
        SchedulerUtils.launchDelayed(2.ticks) {
            val (leftLoc, rightLoc) = baseEntity.blockLocation.minus(1) to baseEntity.blockLocation.plus(1)
            val (aheadLoc, behindLoc) = baseEntity.blockLocation.minus(z = 1) to baseEntity.blockLocation.plus(z = 1)
            IFurniturePacketManager.baseEntityFromHitbox(leftLoc)?.takeIf { it.isValid }?.let(::scheduleUpdateState)
            IFurniturePacketManager.baseEntityFromHitbox(rightLoc)?.takeIf { it.isValid }?.let(::scheduleUpdateState)
            IFurniturePacketManager.baseEntityFromHitbox(aheadLoc)?.takeIf { it.isValid }?.let(::scheduleUpdateState)
            IFurniturePacketManager.baseEntityFromHitbox(behindLoc)?.takeIf { it.isValid }?.let(::scheduleUpdateState)
        }
    }

    private fun determineConnectableShape(baseEntity: ItemDisplay): ConnectType {
        var currentType = baseEntity.persistentDataContainer.get(CONNECTABLE_KEY, ConnectType.dataType)
        val mechanic = NexoFurniture.furnitureMechanic(baseEntity) ?: return ConnectType.DEFAULT
        val furnitureCheck = { entity: Entity -> entity.isValid && NexoFurniture.furnitureMechanic(entity) == mechanic }

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

        val leftBlock = baseEntity.location.add(leftFacing.direction)
        val rightBlock = baseEntity.location.add(leftFacing.oppositeFace.direction)
        val aheadBlock = baseEntity.location.add(aheadFacing.direction)
        val behindBlock = baseEntity.location.add(behindFacing.direction)

        val leftEntity = IFurniturePacketManager.baseEntityFromHitbox(leftBlock)?.takeIf(furnitureCheck)
        val rightEntity = IFurniturePacketManager.baseEntityFromHitbox(rightBlock)?.takeIf(furnitureCheck)
        val aheadEntity = IFurniturePacketManager.baseEntityFromHitbox(aheadBlock)?.takeIf(furnitureCheck)
        val behindEntity = IFurniturePacketManager.baseEntityFromHitbox(behindBlock)?.takeIf(furnitureCheck)

        val leftState = leftEntity?.let(ConnectType::fromEntity)
        val rightState = rightEntity?.let(ConnectType::fromEntity)
        val aheadState = aheadEntity?.let(ConnectType::fromEntity)
        val behindState = behindEntity?.let(ConnectType::fromEntity)

        if (leftState != null) when (leftEntity.facing) {
            leftEntity.facing -> currentType = ConnectType.RIGHT_END
            else -> scheduleUpdateState(baseEntity)
        }
        if (rightState != null) when (rightEntity.facing) {
            rightEntity.facing -> currentType = ConnectType.LEFT_END
            else -> scheduleUpdateState(baseEntity)
        }
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
            fun fromEntity(entity: Entity): ConnectType? = entity.persistentDataContainer.get(CONNECTABLE_KEY, dataType)
        }
    }
}