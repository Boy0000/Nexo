package com.nexomc.nexo.dialog

import com.nexomc.nexo.items.ItemParser
import com.nexomc.nexo.utils.NexoYaml
import com.nexomc.nexo.utils.childSections
import com.nexomc.nexo.utils.listYamlFiles
import io.papermc.paper.plugin.bootstrap.BootstrapContext
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import org.bukkit.Material
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.inventory.ItemStack

data class NexoDialogItem(val bodySection: ConfigurationSection) {

    fun buildItem(): ItemStack {
        return when {
            bodySection.isItemStack("item") -> bodySection.getItemStack("item")
            bodySection.isString("nexoItem") -> {
                val itemId = bodySection.getString("nexoItem")!!
                val config = itemConfigs[itemId] ?: return ItemStack(Material.PAPER)
                return ItemParser(config).buildItem().build()
            }
            else -> ItemStack.of(Material.PAPER)
        } ?: ItemStack.of(Material.PAPER)
    }

    companion object {
        private val itemConfigs: MutableMap<String, ConfigurationSection> = Object2ObjectOpenHashMap()

        fun registerItemConfigs(context: BootstrapContext) {
            itemConfigs.clear()
            context.dataDirectory.resolve("items").toFile().listYamlFiles(true).forEach { file ->
                itemConfigs.putAll(NexoYaml.loadConfiguration(file).childSections())
            }
        }
    }
}