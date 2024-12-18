package com.nexomc.nexo.api

import com.nexomc.nexo.NexoPlugin
import com.nexomc.nexo.api.events.NexoItemsLoadedEvent
import com.nexomc.nexo.items.CustomModelData
import com.nexomc.nexo.items.ItemBuilder
import com.nexomc.nexo.items.ItemParser
import com.nexomc.nexo.mechanics.MechanicsManager
import com.nexomc.nexo.utils.EventUtils.call
import com.nexomc.nexo.utils.NexoYaml
import org.bukkit.NamespacedKey
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import java.io.File
import java.util.*

object NexoItems {
    val ITEM_ID = NamespacedKey(NexoPlugin.instance(), "id")
    private var itemMap = LinkedHashMap<File, LinkedHashMap<String, ItemBuilder>>()
    private var items = LinkedHashSet<String>()

    @JvmStatic
    fun loadItems() {
        ItemParser.CUSTOM_MODEL_DATAS_BY_ID.clear()
        CustomModelData.DATAS.clear()
        NexoPlugin.instance().configsManager().assignAllUsedCustomModelDatas()
        NexoPlugin.instance().configsManager().parseAllItemTemplates()
        itemMap = NexoPlugin.instance().configsManager().parseItemConfig()
        items = linkedSetOf()
        for (subMap in itemMap.values) items += subMap.keys

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
    fun unexcludedItems(file: File) = itemMap[file]?.values?.filter { it.nexoMeta?.excludedFromInventory == false } ?: listOf()

    @JvmStatic
    fun hasMechanic(itemID: String?, mechanicID: String?) = MechanicsManager.getMechanicFactory(mechanicID)?.getMechanic(itemID) != null

    @JvmStatic
    fun itemMap() = itemMap

    @JvmStatic
    fun entriesAsMap(): Map<String, ItemBuilder> = itemMap.values.flatMap { it.entries }.associate { it.key to it.value }

    @JvmStatic
    fun entries(): Set<Map.Entry<String, ItemBuilder>> = itemMap.values.flatMap { it.entries }.toSet()

    @JvmStatic
    fun items(): Set<ItemBuilder> = itemMap.values.flatMap { it.values }.toSet()

    @JvmStatic
    fun names(): Set<String> = itemMap.values.flatMap { it.keys }.toSet()

    @JvmStatic
    fun itemNames(): Array<String> = itemMap.values.flatMap { it.filterValues { it.nexoMeta?.excludedFromCommands != true }.keys }.toTypedArray()
}
