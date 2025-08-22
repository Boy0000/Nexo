@file:Suppress("removal")

package com.nexomc.nexo.items

import com.nexomc.nexo.NexoPlugin
import com.nexomc.nexo.api.NexoItems
import com.nexomc.nexo.commands.toColor
import com.nexomc.nexo.compatibilities.mythiccrucible.WrappedCrucibleItem
import com.nexomc.nexo.configs.Settings
import com.nexomc.nexo.nms.NMSHandlers
import com.nexomc.nexo.utils.VersionUtil
import com.nexomc.nexo.utils.getEnum
import com.nexomc.nexo.utils.getEnumList
import com.nexomc.nexo.utils.getFloat
import com.nexomc.nexo.utils.getKey
import com.nexomc.nexo.utils.getKeyListOrNull
import com.nexomc.nexo.utils.getNamespacedKey
import com.nexomc.nexo.utils.getNamespacedKeyList
import com.nexomc.nexo.utils.logs.Logs
import com.nexomc.nexo.utils.sectionList
import io.papermc.paper.datacomponent.item.TooltipDisplay
import io.papermc.paper.registry.RegistryAccess
import io.papermc.paper.registry.RegistryKey
import net.Indyuce.mmoitems.MMOItems
import net.kyori.adventure.key.Key
import org.apache.commons.lang3.EnumUtils
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.Registry
import org.bukkit.Tag
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.damage.DamageType
import org.bukkit.entity.EntityType
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemRarity
import org.bukkit.inventory.ItemStack
import org.bukkit.tag.DamageTypeTags

@Suppress("UnstableApiUsage", "removal")
class ComponentParser(section: ConfigurationSection, private val itemBuilder: ItemBuilder) {
    private val componentSection: ConfigurationSection? = section.getConfigurationSection("Components")
    private val itemId: String = section.name

    fun parseComponents() {
        if (componentSection == null || VersionUtil.below("1.20.5")) return

        componentSection.getConfigurationSection("custom_data")?.let { c -> c.getKeys(false).associateWith(c::get) }
            ?.let(itemBuilder.customDataMap::putAll)

        if ("max_stack_size" in componentSection)
            itemBuilder.maxStackSize(componentSection.getInt("max_stack_size").coerceIn(1..99))

        if ("enchantment_glint_override" in componentSection)
            itemBuilder.setEnchantmentGlintOverride(componentSection.getBoolean("enchantment_glint_override"))

        if ("durability" in componentSection) {
            itemBuilder.isDamagedOnBlockBreak = componentSection.getBoolean("durability.damage_block_break")
            itemBuilder.isDamagedOnEntityHit = componentSection.getBoolean("durability.damage_entity_hit")
            itemBuilder.setDurability(componentSection.getInt("durability.value").coerceAtLeast(componentSection.getInt("durability", 1)))
        }
        if ("rarity" in componentSection) itemBuilder.setRarity(componentSection.getEnum("rarity", ItemRarity::class.java))
        if ("fire_resistant" in componentSection) itemBuilder.setFireResistant(componentSection.getBoolean("fire_resistant"))
        if ("hide_tooltip" in componentSection) itemBuilder.setHideToolTip(componentSection.getBoolean("hide_tooltip"))

        componentSection.getConfigurationSection("food")?.let { food: ConfigurationSection ->
            NMSHandlers.handler().itemUtils().foodComponent(itemBuilder, food)
        }
        parseToolComponent()

        if (VersionUtil.below("1.21")) return

        itemBuilder.setPaintingVariant(componentSection.getKey("painting_variant"))

        componentSection.getConfigurationSection("jukebox_playable")?.let { jukeboxSection ->
            ItemStack(itemBuilder.type).itemMeta.jukeboxPlayable.also {
                it.isShowInTooltip = jukeboxSection.getBoolean("show_in_tooltip")
                it.songKey = jukeboxSection.getNamespacedKey("song_key") ?: return@also
            }.let(itemBuilder::setJukeboxPlayable)
        }

        if (VersionUtil.below("1.21.2")) return
        componentSection.getConfigurationSection("death_protection")?.let { deathProtection ->
            NMSHandlers.handler().itemUtils().deathProtectionComponent(itemBuilder, deathProtection)
        }
        parseEquippableComponent()

        componentSection.getConfigurationSection("use_cooldown")?.let { cooldownSection: ConfigurationSection ->
                ItemStack(Material.PAPER).itemMeta.useCooldown.also {
                    it.cooldownGroup = cooldownSection.getNamespacedKey("group", "nexo:${NexoItems.idFromItem(itemBuilder)}")
                    it.cooldownSeconds = cooldownSection.getDouble("seconds", 1.0).coerceAtLeast(0.0).toFloat()
                }.apply(itemBuilder::setUseCooldownComponent)
            }

        componentSection.getConfigurationSection("use_remainder")?.let { parseUseRemainderComponent(itemBuilder, it) }
        componentSection.getNamespacedKey("damage_resistant")?.also { damageResistantKey ->
            itemBuilder.setDamageResistant(Bukkit.getTag(DamageTypeTags.REGISTRY_DAMAGE_TYPES, damageResistantKey, DamageType::class.java))
        }

        componentSection.getNamespacedKey("tooltip_style")?.apply(itemBuilder::setTooltipStyle)

        val itemModel = componentSection.getKey("item_model")
            ?: Key.key("nexo:$itemId").takeIf { Settings.PACK_PREFER_ITEMMODELS.toBool() }
        if (itemModel != null && VersionUtil.below("1.21.4"))
            NexoPlugin.instance().packGenerator().packObfuscator().skippedKeys += itemModel.key()
        itemModel?.also(itemBuilder::setItemModel)

        if ("enchantable" in componentSection) itemBuilder.setEnchantable(componentSection.getInt("enchantable"))
        if ("glider" in componentSection) itemBuilder.setGlider(componentSection.getBoolean("glider"))

        componentSection.getConfigurationSection("consumable")?.let { consumableSection ->
            NMSHandlers.handler().itemUtils().consumableComponent(itemBuilder, consumableSection)
        }

        val repairableWith = componentSection.getKeyListOrNull("repairable") ?: listOf(componentSection.getKey("repairable"))
        NMSHandlers.handler().itemUtils().repairableComponent(itemBuilder, repairableWith.filterNotNull())

        if (VersionUtil.below("1.21.4")) return
        componentSection.getConfigurationSection("custom_model_data")?.let { cmdSection ->
            ItemStack(itemBuilder.type).itemMeta.customModelDataComponent.also { cmdComponent ->
                cmdComponent.colors = cmdSection.getStringList("colors").mapNotNull { it.toColor() }
                cmdComponent.floats = cmdSection.getStringList("floats").mapNotNull { it.toFloatOrNull() }
                cmdComponent.strings = cmdSection.getStringList("strings").filterNotNull()
                cmdComponent.flags = cmdSection.getStringList("flags").mapNotNull { it.toBooleanStrictOrNull() }
            }.also(itemBuilder::setCustomModelDataComponent)
        }

        if (VersionUtil.below("1.21.5")) return
        componentSection.getKeyListOrNull("tooltip_display")?.let { displayList ->
            if (itemBuilder.hideToolTip == true) {
                TooltipDisplay.tooltipDisplay().hideTooltip(true).build()
            } else {
                val registry = RegistryAccess.registryAccess().getRegistry(RegistryKey.DATA_COMPONENT_TYPE)
                TooltipDisplay.tooltipDisplay().addHiddenComponents(*displayList.mapNotNull(registry::get).toTypedArray()).build()
            }
        }.also(itemBuilder::setTooltipDisplay)
    }

    private fun parseUseRemainderComponent(item: ItemBuilder, remainderSection: ConfigurationSection) {
        val amount = remainderSection.getInt("amount", 1)

        val result = when {
            "nexo_item" in remainderSection ->
                NexoItems.itemFromId(remainderSection.getString("nexo_item"))?.build()?.let(ItemUpdater::updateItem)
            "crucible_item" in remainderSection -> WrappedCrucibleItem(remainderSection.getString("crucible_item")).build()
            "mmoitems_id" in remainderSection && remainderSection.isString("mmoitems_type") ->
                MMOItems.plugin.getItem(remainderSection.getString("mmoitems_type"), remainderSection.getString("mmoitems_id"))

            "minecraft_type" in remainderSection ->
                ItemStack(Material.getMaterial(remainderSection.getString("minecraft_type", "") ?: return) ?: return)
            else -> remainderSection.getItemStack("minecraft_item")
        }

        result?.amount = amount
        item.setUseRemainder(result)
    }

    private fun parseEquippableComponent() {
        val equippableSection = componentSection?.getConfigurationSection("equippable") ?: return
        val equippableComponent = ItemStack(itemBuilder.type).itemMeta.equippable

        val slot = equippableSection.getString("slot")
        runCatching {
            equippableComponent.slot = EquipmentSlot.valueOf(slot!!)
        }.onFailure {
            Logs.logWarn("Error parsing equippable-component in $itemId...")
            Logs.logWarn("Invalid \"slot\"-value $slot")
            Logs.logWarn("Valid values are: ${EquipmentSlot.entries.joinToString()}")
        }.getOrNull() ?: return

        val entityTypes = equippableSection.getStringList("allowed_entity_types").mapNotNull { EnumUtils.getEnum(EntityType::class.java, it) }
        if ("allowed_entity_types" in equippableSection) equippableComponent.allowedEntities = entityTypes.ifEmpty { null }
        if ("damage_on_hurt" in equippableSection) equippableComponent.isDamageOnHurt = equippableSection.getBoolean("damage_on_hurt", true)
        if ("dispensable" in equippableSection) equippableComponent.isDispensable = equippableSection.getBoolean("dispensable", true)
        if ("swappable" in equippableSection) equippableComponent.isSwappable = equippableSection.getBoolean("swappable", true)

        equippableSection.getNamespacedKey("model")?.apply(equippableComponent::setModel)
        equippableSection.getNamespacedKey("camera_overlay")?.apply(equippableComponent::setCameraOverlay)
        equippableSection.getKey("equip_sound")?.let(Registry.SOUNDS::get)?.apply(equippableComponent::setEquipSound)

        itemBuilder.setEquippableComponent(equippableComponent)
    }

    private fun parseToolComponent() {
        val toolSection = componentSection?.getConfigurationSection("tool") ?: return
        val toolComponent = ItemStack(Material.PAPER).itemMeta.tool
        toolComponent.damagePerBlock = toolSection.getInt("damage_per_block", 1).coerceAtLeast(0)
        toolComponent.defaultMiningSpeed = toolSection.getDouble("default_mining_speed", 1.0).toFloat().coerceAtLeast(0f)

        toolSection.sectionList("rules").forEach { section ->
            val speed = section.getFloat("speed", 1f)
            val correctForDrops = section.getBoolean("correct_for_drops")

            val materials = section.getEnumList("materials", Material::class.java).filter(Material::isBlock).toMutableList()
            val material = section.getEnum("material", Material::class.java)?.takeIf(Material::isBlock)
            if (material != null) materials.add(material)

            val tags = section.getNamespacedKeyList("tags").mapNotNull {
                Bukkit.getTag(Tag.REGISTRY_BLOCKS, it, Material::class.java)
            }.toMutableList()
            val tag = section.getNamespacedKey("tag")?.let { Bukkit.getTag(Tag.REGISTRY_BLOCKS, it, Material::class.java) }
            if (tag != null) tags += tag

            if (materials.isNotEmpty()) toolComponent.addRule(materials, speed, correctForDrops)
            for (tag in tags) toolComponent.addRule(tag, speed, correctForDrops)
        }

        itemBuilder.setToolComponent(toolComponent)
    }
}
