package com.nexomc.nexo.utils.drops

import com.nexomc.nexo.api.NexoItems
import com.nexomc.nexo.mechanics.furniture.FurnitureHelpers
import com.nexomc.nexo.mechanics.furniture.FurnitureMechanic
import com.nexomc.nexo.mechanics.misc.itemtype.ItemTypeMechanicFactory
import com.nexomc.nexo.utils.BlockHelpers.isLoaded
import com.nexomc.nexo.utils.deserialize
import com.nexomc.nexo.utils.randomOrMin
import com.nexomc.nexo.utils.safeCast
import com.nexomc.nexo.utils.wrappers.EnchantmentWrapper
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.ItemDisplay
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.persistence.PersistentDataType

class Drop(
    private var hierarchy: List<String>? = null,
    private val loots: MutableList<Loot>,
    val isSilktouch: Boolean,
    val isFortune: Boolean,
    val sourceID: String,
    private var minimalType: String? = null,
    private var bestTool: String? = null,
) {

    val isEmpty get() = loots.isEmpty() && !isSilktouch
    val explosionDrops: Drop get() = Drop(loots.filter { it.inExplosion }.toMutableList(), false, false, sourceID)

    constructor(loots: MutableList<Loot>, silktouch: Boolean, fortune: Boolean, sourceID: String) :
            this(loots = loots, isSilktouch = silktouch, isFortune = fortune, sourceID = sourceID)

    fun getItemType(itemInHand: ItemStack): String? {
        val itemID = NexoItems.idFromItem(itemInHand)
        val factory = ItemTypeMechanicFactory.get()
        when {
            factory.isNotImplementedIn(itemID) -> {
                val content = itemInHand.type.toString().split("_".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                return if (content.size >= 2) content[0] else ""
            }
            else -> return factory.getMechanic(itemID)?.itemType
        }
    }

    fun canDrop(itemInHand: ItemStack?): Boolean {
        return minimalType.isNullOrEmpty() || isToolEnough(itemInHand) && isTypeEnough(itemInHand)
    }

    fun isTypeEnough(itemInHand: ItemStack?): Boolean {
        if (!minimalType.isNullOrEmpty()) {
            val itemType = if (itemInHand == null) "" else getItemType(itemInHand)
            return itemType!!.isNotEmpty() && itemType in hierarchy!!
                    && (hierarchy!!.indexOf(itemType) >= hierarchy!!.indexOf(minimalType))
        } else return true
    }

    fun isToolEnough(itemInHand: ItemStack?): Boolean {
        if (!bestTool.isNullOrEmpty()) {
            val itemID = NexoItems.idFromItem(itemInHand)
            val type = (itemInHand?.type ?: Material.AIR).name
            return when (bestTool) {
                itemID, type -> true
                else -> type.endsWith("_${bestTool!!.uppercase()}")
            }
        } else return true
    }

    fun getDiff(item: ItemStack) =
        if (minimalType == null) 0 else hierarchy!!.indexOf(getItemType(item)) - hierarchy!!.indexOf(minimalType)

    fun sourceId() = sourceID
    fun minimalType() = minimalType
    fun bestTool() = bestTool
    fun hierarchy() = hierarchy
    fun loots() = loots

    fun loots(loots: List<Loot>): Drop {
        this.loots.clear()
        this.loots.addAll(loots)
        return this
    }

    fun spawns(location: Location, itemInHand: ItemStack): List<DroppedLoot> {
        if (!canDrop(itemInHand) || !location.isLoaded) return listOf()
        val baseItem = NexoItems.itemFromId(sourceID)?.build()

        if (baseItem != null && isSilktouch && itemInHand.itemMeta?.hasEnchant(EnchantmentWrapper.SILK_TOUCH) == true) {
            location.world.dropItemNaturally(location.toCenterLocation(), baseItem)
            return listOf(DroppedLoot(Loot(sourceID, baseItem, 1.0, 1..1), 1))
        } else return dropLoot(loots, location.toCenterLocation(), fortuneMultiplier(itemInHand))
    }

    fun furnitureSpawns(baseEntity: ItemDisplay, itemInHand: ItemStack) {
        val baseItem = NexoItems.itemFromId(sourceID)?.build()
        val location = baseEntity.location.toCenterLocation().takeIf { it.isWorldLoaded } ?: return
        val furnitureItem = FurnitureHelpers.furnitureItem(baseEntity) ?: NexoItems.itemFromId(sourceID)?.build() ?: return
        furnitureItem.editMeta { itemMeta: ItemMeta ->
            baseItem?.itemMeta?.takeIf(ItemMeta::hasDisplayName)?.let { itemMeta.displayName(it.displayName()) }
            baseEntity.persistentDataContainer.get(FurnitureMechanic.DISPLAY_NAME_KEY, PersistentDataType.STRING)?.also { itemMeta.displayName(it.deserialize()) }
        }

        if (!canDrop(itemInHand)) return

        when {
            baseItem != null && isSilktouch && itemInHand.itemMeta?.hasEnchant(EnchantmentWrapper.SILK_TOUCH) == true ->
                location.world.dropItemNaturally(location, baseItem)
            else -> {
                dropLoot(loots.filter { it.itemStack() != baseItem }, location, fortuneMultiplier(itemInHand))
                dropLoot(loots.filter { it.itemStack() == baseItem }.map { Loot(sourceID, furnitureItem, it.probability, it.amount) }, location, fortuneMultiplier(itemInHand))
            }
        }
    }

    fun fortuneMultiplier(itemInHand: ItemStack) =
        (1..(itemInHand.itemMeta?.takeIf { isFortune }?.getEnchantLevel(EnchantmentWrapper.FORTUNE)?.plus(1) ?: 1)).randomOrMin()

    fun dropLoot(loots: List<Loot>, location: Location, fortuneMultiplier: Int) = loots.mapNotNull {
        it.dropNaturally(location, fortuneMultiplier).takeIf { it > 0 }?.let { amount -> DroppedLoot(it, amount) }
    }

    /**
     * Get the loots that will drop based on a given Player
     * @param player the player that triggered this drop
     * @return the loots that will drop
     */
    fun lootToDrop(player: Player): List<Loot> {
        val itemInHand = player.inventory.itemInMainHand

        return loots.filter { loot -> canDrop(itemInHand) && Math.random() > loot.probability }
    }

    companion object {
        @JvmStatic
        fun createDrop(toolTypes: List<String>?, dropSection: ConfigurationSection, sourceID: String): Drop {
            val loots = (dropSection.getList("loots").safeCast<List<LinkedHashMap<String, Any>>>())?.map { Loot(it, sourceID) }?.toMutableList() ?: mutableListOf()

            return Drop(
                toolTypes, loots, dropSection.getBoolean("silktouch"),
                dropSection.getBoolean("fortune"), sourceID,
                dropSection.getString("minimal_type", ""), dropSection.getString("best_tool", "")
            )
        }

        @JvmStatic
        fun emptyDrop() = Drop(ArrayList(), false, false, "")

        @JvmStatic
        fun emptyDrop(loots: MutableList<Loot>) = Drop(loots, false, false, "")

        @JvmStatic
        fun clone(drop: Drop, newLoots: MutableList<Loot>) = Drop(
            drop.hierarchy,
            newLoots,
            drop.isSilktouch,
            drop.isFortune,
            drop.sourceID,
            drop.minimalType,
            drop.bestTool
        )
    }
}
