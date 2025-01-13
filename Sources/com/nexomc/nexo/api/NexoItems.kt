package com.nexomc.nexo.api

import com.nexomc.nexo.NexoPlugin
import com.nexomc.nexo.api.events.NexoItemsLoadedEvent
import com.nexomc.nexo.compatibilities.ecoitems.WrappedEcoItem
import com.nexomc.nexo.compatibilities.mythiccrucible.WrappedCrucibleItem
import com.nexomc.nexo.items.CustomModelData
import com.nexomc.nexo.items.ItemBuilder
import com.nexomc.nexo.items.ItemParser
import com.nexomc.nexo.items.ItemUpdater
import com.nexomc.nexo.mechanics.MechanicsManager
import com.nexomc.nexo.utils.*
import com.nexomc.nexo.utils.EventUtils.call
import com.nexomc.nexo.utils.logs.Logs
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet
import net.Indyuce.mmoitems.MMOItems
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.components.FoodComponent
import org.bukkit.persistence.PersistentDataType
import java.io.File
import java.util.*

object NexoItems {
    val ITEM_ID = NamespacedKey(NexoPlugin.instance(), "id")
    private var itemMap = Object2ObjectLinkedOpenHashMap<File, Object2ObjectLinkedOpenHashMap<String, ItemBuilder>>()
    private var items = ObjectLinkedOpenHashSet<String>()

    @JvmStatic
    fun loadItems() {
        ItemParser.CUSTOM_MODEL_DATAS_BY_ID.clear()
        CustomModelData.DATAS.clear()
        NexoPlugin.instance().configsManager().assignAllUsedCustomModelDatas()
        NexoPlugin.instance().configsManager().parseAllItemTemplates()
        itemMap = NexoPlugin.instance().configsManager().parseItemConfig()
        items = ObjectLinkedOpenHashSet()
        for (subMap in itemMap.values) items += subMap.keys

        ensureComponentDataHandled()

        NexoItemsLoadedEvent().call()
    }

    val itemConfigCache: MutableMap<String, Pair<File, ConfigurationSection>?> = mutableMapOf()
    fun reloadItem(itemId: String) {
        itemConfigCache.computeIfAbsent(itemId) {
            itemMap.entries.firstOrNull { itemId in it.value.keys }?.let {
                it.key to NexoYaml.loadConfiguration(it.key).getConfigurationSection(itemId)!!
            }
        }?.let { (file, config) ->
            itemMap[file]?.put(itemId, ItemParser(config).buildItem())
        }
    }

    @JvmStatic
    fun idFromItem(item: ItemBuilder) = item.customTag(ITEM_ID, PersistentDataType.STRING)

    @JvmStatic
    fun idFromItem(item: ItemStack?) = item?.itemMeta?.persistentDataContainer?.get(ITEM_ID, PersistentDataType.STRING)

    @JvmStatic
    fun exists(itemId: String?) = itemId in items

    @JvmStatic
    fun exists(itemStack: ItemStack?) = idFromItem(itemStack) in items

    @JvmStatic
    fun optionalItemFromId(id: String?) = Optional.ofNullable(entries().firstOrNull { it.key == id }?.value)

    @JvmStatic
    fun itemFromId(id: String?): ItemBuilder? = optionalItemFromId(id).orElse(null)

    @JvmStatic
    fun builderFromItem(item: ItemStack?): ItemBuilder? = itemFromId(idFromItem(item))

    @JvmStatic
    fun unexcludedItems(file: File) = itemMap[file]?.values?.filterFast { it.nexoMeta?.excludedFromInventory == false } ?: listOf()

    @JvmStatic
    fun hasMechanic(itemID: String?, mechanicID: String?) = MechanicsManager.getMechanicFactory(mechanicID)?.getMechanic(itemID) != null

    @JvmStatic
    fun itemMap(): Map<File, Map<String, ItemBuilder>> = itemMap

    @JvmStatic
    fun entriesAsMap(): Map<String, ItemBuilder> = itemMap.values.flatMapFast { it.entries }.associateFast { it.key to it.value }

    @JvmStatic
    fun entries(): Set<Map.Entry<String, ItemBuilder>> = itemMap.values.flatMapSetFast { it.entries }

    @JvmStatic
    fun items(): Set<ItemBuilder> = itemMap.values.flatMapSetFast { it.values }

    @JvmStatic
    fun names(): Set<String> = itemMap.values.flatMapSetFast { it.keys }

    @JvmStatic
    fun itemNames(): Array<String> = itemMap.values.flatMapFast { it.filterFast { it.value.nexoMeta?.excludedFromCommands != true }.keys }.toTypedArray()

    /**
     * Primarily for handling data that requires NexoItems's<br></br>
     * For example FoodComponent#getUsingConvertsTo
     */
    private fun ensureComponentDataHandled() {
        if (VersionUtil.matchesServer("1.21.1")) itemMap.forEach { (file, subMap) ->
            subMap.forEach submap@{ (itemId, value) ->
                val itemBuilder = value ?: return@submap
                val foodComponent = itemBuilder.foodComponent ?: return@submap

                val section = NexoYaml.loadConfiguration(file).getConfigurationSection("$itemId.Components.food.replacement") ?: return@submap
                val replacementItem = parseFoodComponentReplacement(section)
                ItemUtils.setUsingConvertsTo(foodComponent, replacementItem)
                itemBuilder.setFoodComponent(foodComponent).regenerateItem()
            }
        }
    }

    private fun parseFoodComponentReplacement(section: ConfigurationSection): ItemStack? {
        val replacementItem = when {
            section.isString("minecraft_type") -> {
                val material = Material.getMaterial(section.getString("minecraft_type")!!)
                if (material == null) Logs.logError("Invalid material: ${section.getString("minecraft_type")}")
                material?.let(::ItemStack)
            }
            section.isString("nexo_item") -> itemFromId(section.getString("nexo_item"))?.build()
            section.isString("crucible_item") ->  WrappedCrucibleItem(section.getString("crucible_item")).build()
            section.isString("mmoitems_id") && section.isString("mmoitems_type") ->
                MMOItems.plugin.getItem(section.getString("mmoitems_type"), section.getString("mmoitems_id"))
            section.isString("ecoitem_id") -> WrappedEcoItem(section.getString("ecoitem_id")).build()
            section.isItemStack("minecraft_item") -> section.getItemStack("minecraft_item")
            else -> null
        }

        return replacementItem
    }
}
