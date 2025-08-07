package com.nexomc.nexo.api

import com.nexomc.nexo.NexoPlugin
import com.nexomc.nexo.api.events.NexoItemsLoadedEvent
import com.nexomc.nexo.compatibilities.mythiccrucible.WrappedCrucibleItem
import com.nexomc.nexo.items.CustomModelData
import com.nexomc.nexo.items.ItemBuilder
import com.nexomc.nexo.items.ItemParser
import com.nexomc.nexo.mechanics.MechanicsManager
import com.nexomc.nexo.utils.EventUtils.call
import com.nexomc.nexo.utils.ItemUtils
import com.nexomc.nexo.utils.ItemUtils.persistentDataView
import com.nexomc.nexo.utils.NexoYaml
import com.nexomc.nexo.utils.VersionUtil
import com.nexomc.nexo.utils.filterFast
import com.nexomc.nexo.utils.flatMapFast
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet
import net.Indyuce.mmoitems.MMOItems
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import java.io.File
import java.util.*

object NexoItems {
    val ITEM_ID = NamespacedKey(NexoPlugin.instance(), "id")
    private var itemMap = Object2ObjectLinkedOpenHashMap<File, Object2ObjectLinkedOpenHashMap<String, ItemBuilder>>()
    private var itemNames = ObjectLinkedOpenHashSet<String>()
    private var items = ObjectLinkedOpenHashSet<ItemBuilder>()
    private var unexcludedItems = Object2ObjectLinkedOpenHashMap<File, ObjectArrayList<ItemBuilder>>()
    private var entries = Object2ObjectLinkedOpenHashMap<String, ItemBuilder>()

    @JvmStatic
    fun loadItems() {
        // Clear existing data instead of reassigning new maps
        ItemParser.CUSTOM_MODEL_DATAS_BY_ID.clear()
        CustomModelData.DATAS.clear()
        NexoPlugin.instance().configsManager().assignAllUsedCustomModelDatas()
        NexoPlugin.instance().configsManager().parseAllItemTemplates()

        // Mutate existing maps instead of reassigning
        itemMap.clear()
        itemMap += NexoPlugin.instance().configsManager().parseItemConfig()

        itemNames.clear()
        itemNames += itemMap.values.asSequence().flatMap { it.keys.asSequence() }

        items.clear()
        items += itemMap.values.asSequence().flatMap { it.values.asSequence() }

        entries.clear()
        entries += itemMap.values.asSequence().flatMap { it.entries.asSequence() }.associate { it.key to it.value }

        unexcludedItems.clear()
        unexcludedItems += itemMap.asSequence().mapNotNull { (file, map) ->
            val filtered = map.values.filterFast { it.nexoMeta?.excludedFromInventory == false }
            if (filtered.isNotEmpty()) file to filtered else null
        }

        ensureComponentDataHandled()

        NexoItemsLoadedEvent().call()
    }


    val itemConfigCache: MutableMap<String, Pair<File, ConfigurationSection>?> = Object2ObjectLinkedOpenHashMap()
    fun reloadItem(itemId: String) {
        val (file, config) = itemConfigCache.getOrPut(itemId) {
            itemMap.entries.find { it.value.containsKey(itemId) }?.let {
                it.key to NexoYaml.loadConfiguration(it.key).getConfigurationSection(itemId)!!
            }
        } ?: return
        itemMap[file]?.put(itemId, ItemParser(config).buildItem())
    }

    @JvmStatic
    fun idFromItem(item: ItemBuilder): String? = item.customTag(ITEM_ID, PersistentDataType.STRING)

    //TODO Swap this to item?.persistentDataContainer when dropping <1.21.1
    @JvmStatic
    fun idFromItem(item: ItemStack?): String? = item?.persistentDataView?.get(ITEM_ID, PersistentDataType.STRING)

    @JvmStatic
    fun exists(itemId: String?): Boolean = itemId in itemNames

    @JvmStatic
    fun exists(itemStack: ItemStack?): Boolean = idFromItem(itemStack) in itemNames

    @JvmStatic
    fun itemFromId(id: String?): ItemBuilder? = entries[id]

    @JvmStatic
    fun optionalItemFromId(id: String?): Optional<ItemBuilder> = Optional.ofNullable(itemFromId(id))

    @JvmStatic
    fun builderFromItem(item: ItemStack?): ItemBuilder? = itemFromId(idFromItem(item))

    @JvmStatic
    fun unexcludedItems(file: File): List<ItemBuilder> = unexcludedItems[file] ?: listOf()

    @JvmStatic
    fun hasMechanic(itemID: String?, mechanicID: String?) = MechanicsManager.mechanicFactory(mechanicID)?.getMechanic(itemID) != null

    @JvmStatic
    fun itemMap(): Map<File, Map<String, ItemBuilder>> = itemMap

    @JvmStatic
    fun entries(): Map<String, ItemBuilder> = entries

    @JvmStatic
    fun items(): Set<ItemBuilder> = items

    @JvmStatic
    fun itemNames(): Set<String> = itemNames

    @JvmStatic
    fun unexcludedItemNames(): Array<String> = itemMap.values.flatMapFast { it.filterFast { it.value.nexoMeta?.excludedFromCommands != true }.keys }.toTypedArray()

    /**
     * Primarily for handling data that requires NexoItems's<br></br>
     * For example FoodComponent#getUsingConvertsTo
     */
    private fun ensureComponentDataHandled() {
        if (VersionUtil.matchesServer("1.21.1")) itemMap.forEach { (file, subMap) ->
            val config = NexoYaml.loadConfiguration(file)
            subMap.forEach submap@{ (itemId, value) ->
                val itemBuilder = value ?: return@submap
                val foodComponent = itemBuilder.foodComponent ?: return@submap

                val section = config.getConfigurationSection("$itemId.Components.food.replacement") ?: return@submap
                val replacementItem = parseFoodComponentReplacement(section)
                ItemUtils.setUsingConvertsTo(foodComponent, replacementItem)
                itemBuilder.setFoodComponent(foodComponent).regenerateItem()
            }
        }
    }

    private fun parseFoodComponentReplacement(section: ConfigurationSection): ItemStack? {
        return when {
            section.contains("minecraft_type") ->
                Material.getMaterial(section.getString("minecraft_type")!!)?.let(::ItemStack)
            section.contains("nexo_item") -> itemFromId(section.getString("nexo_item"))?.build()
            section.contains("crucible_item") -> WrappedCrucibleItem(section.getString("crucible_item")).build()
            section.contains("mmoitems_id") && section.contains("mmoitems_type") ->
                MMOItems.plugin.getItem(section.getString("mmoitems_type"), section.getString("mmoitems_id"))
            section.contains("minecraft_item") -> section.getItemStack("minecraft_item")
            else -> null
        }
    }

}
