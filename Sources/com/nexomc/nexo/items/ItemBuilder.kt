package com.nexomc.nexo.items

import com.google.common.collect.HashMultimap
import com.google.common.collect.Multimap
import com.jeff_media.morepersistentdatatypes.DataType
import com.nexomc.nexo.NexoPlugin
import com.nexomc.nexo.api.NexoFurniture
import com.nexomc.nexo.api.NexoItems
import com.nexomc.nexo.compatibilities.mmoitems.WrappedMMOItem
import com.nexomc.nexo.compatibilities.mythiccrucible.WrappedCrucibleItem
import com.nexomc.nexo.nms.NMSHandlers
import com.nexomc.nexo.utils.AdventureUtils
import com.nexomc.nexo.utils.AdventureUtils.setDefaultStyle
import com.nexomc.nexo.utils.NexoYaml.Companion.loadConfiguration
import com.nexomc.nexo.utils.VersionUtil
import com.nexomc.nexo.utils.printOnFailure
import com.nexomc.nexo.utils.safeCast
import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.TooltipDisplay
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import org.bukkit.*
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.damage.DamageType
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemRarity
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.*
import org.bukkit.inventory.meta.components.*
import org.bukkit.inventory.meta.trim.ArmorTrim
import org.bukkit.inventory.meta.trim.TrimMaterial
import org.bukkit.inventory.meta.trim.TrimPattern
import org.bukkit.persistence.PersistentDataContainer
import org.bukkit.persistence.PersistentDataType
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionType

@Suppress("UnstableApiUsage")
class ItemBuilder(private val itemStack: ItemStack) {
    val persistentDataMap: MutableMap<PersistentDataSpace<*, *>, Any> = mutableMapOf()
    val persistentDataContainer: PersistentDataContainer
    val enchantments: MutableMap<Enchantment, Int>
    var nexoMeta: NexoMeta? = null; private set
    var type: Material = Material.PAPER; private set
    var amount: Int; private set
    var color: Color? = null; private set
    var trimPattern: Key? = null; private set
    var basePotionType: PotionType? = null; private set
    var customPotionEffects: MutableList<PotionEffect>? = null; private set
    var unbreakable: Boolean; private set
    var itemFlags: MutableSet<ItemFlag> = mutableSetOf(); private set
    var attributeModifiers: Multimap<Attribute, AttributeModifier>? = null; private set
    var customModelData: Int? = null; private set
    var displayName: Component? = null; private set
    var lore: List<Component>? = null; private set
    var finalItemStack: ItemStack? = null; private set

    // 1.20.5+ properties
    var foodComponent: FoodComponent? = null; private set
    var toolComponent: ToolComponent? = null; private set
    var enchantmentGlindOverride: Boolean? = null; private set
    var maxStackSize: Int? = null; private set
    var itemName: Component? = null; private set
    var fireResistant: Boolean? = null; private set
    var hideToolTip: Boolean? = null; private set
    var rarity: ItemRarity? = null; private set
    var durability: Int? = null; private set
    var isDamagedOnBlockBreak = false
    var isDamagedOnEntityHit = false

    // 1.21+ properties
    var jukeboxPlayable: JukeboxPlayableComponent? = null; private set

    // 1.21.2+ properties
    var equippable: EquippableComponent? = null; private set
    var isGlider: Boolean? = null; private set
    var useCooldown: UseCooldownComponent? = null; private set
    var useRemainder: ItemStack? = null; private set
    var damageResistant: Tag<DamageType>? = null; private set
    var tooltipStyle: NamespacedKey? = null; private set
    var itemModel: NamespacedKey? = null; private set
    var blockStates: Map<String, String>? = null; private set
    var enchantable: Int? = null; private set
    var consumableComponent: Any? = null; private set
    var repairableComponent: Any? = null; private set
    var cache: Boolean = true

    // 1.21.4+ properties
    var customModelDataComponent: CustomModelDataComponent? = null; private set

    // 1.21.5+ properties
    var tooltipDisplay: TooltipDisplay? = null; private set


    constructor(material: Material) : this(ItemStack(material))

    constructor(wrapped: WrappedMMOItem) : this(wrapped.build()!!) {
        this.cache = wrapped.cache
    }

    constructor(wrapped: WrappedCrucibleItem) : this(wrapped.build()!!) {
        this.cache = wrapped.cache
    }

    constructor(itemBuilder: ItemBuilder) : this(itemBuilder.build())

    init {
        type = itemStack.type

        amount = itemStack.amount

        val itemMeta = checkNotNull(itemStack.itemMeta)
        if (itemMeta is LeatherArmorMeta) color = itemMeta.color

        if (itemMeta is PotionMeta) {
            color = itemMeta.color
            basePotionType = itemMeta.basePotionType
            customPotionEffects = itemMeta.customEffects.toMutableList()
        }

        if (itemMeta is MapMeta) color = itemMeta.color

        if (itemMeta is FireworkEffectMeta) color = itemMeta.effect?.colors?.firstOrNull() ?: Color.WHITE

        if ((itemMeta as? ArmorMeta)?.hasTrim() == true) trimPattern = itemMeta.trim?.material?.key()

        displayName = itemMeta.displayName()
        lore = itemMeta.lore()

        unbreakable = itemMeta.isUnbreakable

        if (itemMeta.itemFlags.isNotEmpty()) itemFlags = itemMeta.itemFlags

        attributeModifiers = itemMeta.attributeModifiers


        customModelData = if (itemMeta.hasCustomModelData()) itemMeta.customModelData else null

        persistentDataContainer = itemMeta.persistentDataContainer

        enchantments = HashMap()

        if (VersionUtil.atleast("1.20.5")) {
            itemName = if (itemMeta.hasItemName()) itemMeta.itemName() else null

            durability = if ((itemMeta is Damageable) && itemMeta.hasMaxDamage()) itemMeta.maxDamage else null
            fireResistant = if (itemMeta.isFireResistant) true else null
            hideToolTip = if (itemMeta.isHideTooltip) true else null
            foodComponent = if (itemMeta.hasFood()) itemMeta.food else null
            toolComponent = if (itemMeta.hasTool()) itemMeta.tool else null
            enchantmentGlindOverride = if (itemMeta.hasEnchantmentGlintOverride()) itemMeta.enchantmentGlintOverride else null
            rarity = if (itemMeta.hasRarity()) itemMeta.rarity else null
            maxStackSize = if (itemMeta.hasMaxStackSize()) itemMeta.maxStackSize else null
        }

        if (VersionUtil.atleast("1.21")) {
            jukeboxPlayable = if (itemMeta.hasJukeboxPlayable()) itemMeta.jukeboxPlayable else null
        }

        if (VersionUtil.atleast("1.21.2")) {
            equippable = if (itemMeta.hasEquippable()) itemMeta.equippable else null
            useCooldown = if (itemMeta.hasUseCooldown()) itemMeta.useCooldown else null
            useRemainder = if (itemMeta.hasUseRemainder()) itemMeta.useRemainder else null
            damageResistant = if (itemMeta.hasDamageResistant()) itemMeta.damageResistant else null
            itemModel = if (itemMeta.hasItemModel()) itemMeta.itemModel else null
            enchantable = if (itemMeta.hasEnchantable()) itemMeta.enchantable else null
            isGlider = if (itemMeta.isGlider) true else null

            if (color == null) color = itemStack.getData(DataComponentTypes.DYED_COLOR)?.color()
                ?: itemStack.getData(DataComponentTypes.MAP_COLOR)?.color()
                ?: itemStack.getData(DataComponentTypes.BASE_COLOR)?.color
        }

        if (VersionUtil.atleast("1.21.4")) {
            customModelDataComponent = if (itemMeta.hasCustomModelData()) itemMeta.customModelDataComponent else null
        }

        if (VersionUtil.atleast("1.21.5")) {
            tooltipDisplay = itemStack.getData(DataComponentTypes.TOOLTIP_DISPLAY)
        }
    }

    fun setType(type: Material): ItemBuilder {
        this.type = type
        return this
    }

    fun setAmount(amount: Int): ItemBuilder {
        this.amount = amount.coerceIn(0, type.maxStackSize)
        return this
    }

    fun displayName(displayName: Component?): ItemBuilder {
        this.displayName = displayName
        return this
    }

    fun hasItemName(): Boolean {
        return itemName != null
    }

    fun itemName(itemName: Component?): ItemBuilder {
        this.itemName = itemName
        return this
    }

    fun hasLores(): Boolean {
        return !lore.isNullOrEmpty()
    }

    fun lore() = lore ?: listOf()

    fun lore(lore: List<Component>?): ItemBuilder {
        this.lore = lore
        return this
    }

    fun setUnbreakable(unbreakable: Boolean): ItemBuilder {
        this.unbreakable = unbreakable
        return this
    }

    fun setDurability(durability: Int?): ItemBuilder {
        this.durability = durability
        return this
    }

    /**
     * Check if the ItemBuilder has color.
     *
     * @return true if the ItemBuilder has color that is not default LeatherMetaColor
     */
    fun hasColor(): Boolean {
        return color != null && color != Bukkit.getItemFactory().defaultLeatherColor
    }

    fun setColor(color: Color?): ItemBuilder {
        this.color = color
        return this
    }

    fun hasTrimPattern(): Boolean {
        return trimPattern != null && getTrimPattern() != null
    }

    val trimPatternKey: Key?
        get() {
            if (!Tag.ITEMS_TRIMMABLE_ARMOR.isTagged(type)) return null
            return trimPattern
        }

    fun getTrimPattern(): TrimPattern? {
        if (!Tag.ITEMS_TRIMMABLE_ARMOR.isTagged(type) || trimPattern == null) return null
        val key = NamespacedKey.fromString(trimPattern!!.asString()) ?: return null
        return Registry.TRIM_PATTERN.get(key)
    }

    fun setTrimPattern(trimKey: Key?): ItemBuilder {
        if (!Tag.ITEMS_TRIMMABLE_ARMOR.isTagged(type)) return this
        this.trimPattern = trimKey
        return this
    }

    fun hasItemModel(): Boolean {
        return VersionUtil.atleast("1.21.2") && itemModel != null
    }

    fun setItemModel(itemModel: NamespacedKey?): ItemBuilder {
        this.itemModel = itemModel
        return this
    }

    fun setItemModel(itemModel: Key?): ItemBuilder {
        this.itemModel = itemModel?.let { NamespacedKey.fromString(it.asString()) }
        return this
    }

    fun hasBlockStates(): Boolean {
        return VersionUtil.atleast("1.21.2") && !blockStates.isNullOrEmpty()
    }

    fun setBlockStates(blockStates: Map<String, String>): ItemBuilder {
        this.blockStates = blockStates
        return this
    }

    fun hasTooltipStyle(): Boolean {
        return VersionUtil.atleast("1.21.2") && tooltipStyle != null
    }

    fun setTooltipStyle(tooltipStyle: NamespacedKey?): ItemBuilder {
        this.tooltipStyle = tooltipStyle
        return this
    }

    fun hasTooltipDisplay(): Boolean {
        return VersionUtil.atleast("1.21.5") && tooltipDisplay != null
    }

    fun setTooltipDisplay(tooltipDisplay: TooltipDisplay?): ItemBuilder {
        this.tooltipDisplay = tooltipDisplay
        return this
    }

    fun hasEnchantable(): Boolean {
        return VersionUtil.atleast("1.21.2") && enchantable != null
    }

    fun setEnchantable(enchantable: Int?): ItemBuilder {
        this.enchantable = enchantable?.coerceAtLeast(1)
        return this
    }

    fun hasDamageResistant(): Boolean {
        return VersionUtil.atleast("1.21.2") && damageResistant != null
    }

    fun setDamageResistant(damageResistant: Tag<DamageType>?): ItemBuilder {
        this.damageResistant = damageResistant
        return this
    }

    fun setGlider(glider: Boolean): ItemBuilder {
        this.isGlider = glider
        return this
    }

    fun hasUseRemainder(): Boolean {
        return VersionUtil.atleast("1.21.2") && useRemainder != null
    }

    fun setUseRemainder(itemStack: ItemStack?): ItemBuilder {
        this.useRemainder = itemStack
        return this
    }

    fun hasUseCooldownComponent(): Boolean {
        return VersionUtil.atleast("1.21.2") && useCooldown != null
    }

    fun setUseCooldownComponent(useCooldownComponent: UseCooldownComponent?): ItemBuilder {
        this.useCooldown = useCooldownComponent
        return this
    }

    fun hasEquippableComponent(): Boolean {
        return VersionUtil.atleast("1.21.2") && equippable != null
    }

    fun setEquippableComponent(equippableComponent: EquippableComponent?): ItemBuilder {
        this.equippable = equippableComponent
        return this
    }

    fun hasFoodComponent(): Boolean {
        return VersionUtil.atleast("1.20.5") && foodComponent != null
    }

    fun setFoodComponent(foodComponent: FoodComponent?): ItemBuilder {
        this.foodComponent = foodComponent
        return this
    }

    fun hasConsumableComponent(): Boolean {
        return VersionUtil.atleast("1.21.2") && consumableComponent != null
    }

    fun setConsumableComponent(consumableComponent: Any?): ItemBuilder {
        this.consumableComponent = consumableComponent
        return this
    }

    fun hasRepairableComponent(): Boolean {
        return VersionUtil.atleast("1.21.2") && repairableComponent != null
    }

    fun setRepairableComponent(repairableComponent: Any?): ItemBuilder {
        this.repairableComponent = repairableComponent
        return this
    }

    fun hasToolComponent(): Boolean {
        return VersionUtil.atleast("1.20.5") && toolComponent != null
    }

    fun setToolComponent(toolComponent: ToolComponent?): ItemBuilder {
        this.toolComponent = toolComponent
        return this
    }

    fun hasJukeboxPlayable(): Boolean {
        return VersionUtil.atleast("1.21") && jukeboxPlayable != null
    }

    fun setJukeboxPlayable(jukeboxPlayable: JukeboxPlayableComponent?): ItemBuilder {
        this.jukeboxPlayable = jukeboxPlayable
        return this
    }

    fun hasCustomModelDataComponent(): Boolean {
        return VersionUtil.atleast("1.21.4") && customModelDataComponent != null
    }

    fun setCustomModelDataComponent(customModelData: CustomModelDataComponent?): ItemBuilder {
        this.customModelDataComponent = customModelData
        return this
    }

    fun hasEnchantmentGlindOverride(): Boolean {
        return VersionUtil.atleast("1.20.5") && enchantmentGlindOverride != null
    }

    fun setEnchantmentGlindOverride(enchantmentGlintOverride: Boolean?): ItemBuilder {
        this.enchantmentGlindOverride = enchantmentGlintOverride
        return this
    }

    fun hasRarity(): Boolean {
        return VersionUtil.atleast("1.20.5") && rarity != null
    }

    fun setRarity(rarity: ItemRarity?): ItemBuilder {
        this.rarity = rarity
        return this
    }

    fun setFireResistant(fireResistant: Boolean): ItemBuilder {
        this.fireResistant = fireResistant
        return this
    }

    fun setHideToolTip(hideToolTip: Boolean): ItemBuilder {
        this.hideToolTip = hideToolTip
        return this
    }

    fun hasMaxStackSize(): Boolean {
        return VersionUtil.atleast("1.20.5") && maxStackSize != null
    }

    fun maxStackSize(maxStackSize: Int?): ItemBuilder {
        this.maxStackSize = Math.clamp(maxStackSize!!.toLong(), 0, 99)
        return this
    }

    fun basePotionType(potionType: PotionType?): ItemBuilder {
        this.basePotionType = potionType
        return this
    }

    fun addPotionEffect(potionEffect: PotionEffect): ItemBuilder {
        if (customPotionEffects == null) customPotionEffects = mutableListOf()
        customPotionEffects!!.add(potionEffect)
        return this
    }

    fun hasCustomTag(): Boolean = !persistentDataContainer.isEmpty

    fun <T, Z> customTag(namespacedKey: NamespacedKey, dataType: PersistentDataType<T, Z>, data: Z): ItemBuilder {
        persistentDataMap[PersistentDataSpace(namespacedKey, dataType)] = data as Any
        return this
    }

    @Suppress("UNCHECKED_CAST")
    fun <T, Z> customTag(namespacedKey: NamespacedKey, dataType: PersistentDataType<T, Z>): Z? {
        for ((key, value) in persistentDataMap) {
            if (key.key == namespacedKey && key.dataType == dataType)
                return value as Z
        }
        return null
    }

    fun removeCustomTag(key: NamespacedKey): ItemBuilder {
        persistentDataContainer.remove(key)
        return this
    }

    fun customModelData(customModelData: Int): ItemBuilder {
        this.customModelData = customModelData
        return this
    }

    fun addItemFlags(vararg itemFlags: ItemFlag): ItemBuilder {
        this.itemFlags.addAll(itemFlags)
        return this
    }

    fun itemFlags() = itemFlags

    fun addAttributeModifiers(attribute: Attribute, attributeModifier: AttributeModifier): ItemBuilder {
        if (attributeModifiers == null) attributeModifiers = HashMultimap.create()
        attributeModifiers!!.put(attribute, attributeModifier)
        return this
    }

    fun addAttributeModifiers(attributeModifiers: Multimap<Attribute, AttributeModifier>): ItemBuilder {
        if (this.attributeModifiers == null) this.attributeModifiers = HashMultimap.create()
        this.attributeModifiers!!.putAll(attributeModifiers)
        return this
    }

    fun addEnchant(enchant: Enchantment, level: Int): ItemBuilder {
        enchantments[enchant] = level
        return this
    }

    fun addEnchants(enchants: Map<Enchantment, Int>): ItemBuilder {
        enchants.forEach(::addEnchant)
        return this
    }

    fun hasNexoMeta(): Boolean {
        return nexoMeta != null
    }

    fun nexoMeta(itemResources: NexoMeta?): ItemBuilder {
        nexoMeta = itemResources
        return this
    }

    fun referenceCopy() = itemStack.clone()

    fun clone() = ItemBuilder(build())

    fun regenerateItem(): ItemBuilder {
        itemStack.type = type
        if (amount != itemStack.amount) itemStack.amount = amount

        val itemMeta = itemStack.itemMeta

        // 1.20.5+ properties
        if (VersionUtil.atleast("1.20.5")) {
            if (itemMeta is Damageable) itemMeta.setMaxDamage(durability)
            if (itemName != null) itemMeta.itemName(itemName)
            if (hasMaxStackSize()) itemMeta.setMaxStackSize(maxStackSize)
            if (hasEnchantmentGlindOverride()) itemMeta.setEnchantmentGlintOverride(enchantmentGlindOverride)
            if (hasRarity()) itemMeta.setRarity(rarity)
            if (hasFoodComponent()) itemMeta.setFood(foodComponent)
            if (hasToolComponent()) itemMeta.setTool(toolComponent)
            if (fireResistant != null) itemMeta.isFireResistant = fireResistant!!
            if (hideToolTip != null) itemMeta.isHideTooltip = hideToolTip!!
        }

        if (VersionUtil.atleast("1.21")) {
            if (hasJukeboxPlayable()) itemMeta.setJukeboxPlayable(jukeboxPlayable)
        }

        if (VersionUtil.atleast("1.21.2")) {
            if (hasEquippableComponent()) itemMeta.setEquippable(equippable)
            if (hasUseCooldownComponent()) itemMeta.setUseCooldown(useCooldown)
            if (hasDamageResistant()) itemMeta.damageResistant = damageResistant
            if (hasTooltipStyle()) itemMeta.tooltipStyle = tooltipStyle
            if (hasUseRemainder()) itemMeta.useRemainder = useRemainder
            if (hasEnchantable()) itemMeta.setEnchantable(enchantable)
            if (itemModel != null) itemMeta.itemModel = itemModel
            if (isGlider != null) itemMeta.isGlider = isGlider!!
        }

        handleVariousMeta(itemMeta)
        itemMeta.isUnbreakable = unbreakable

        val pdc = itemMeta.persistentDataContainer
        if (displayName != null) {
            if (VersionUtil.below("1.20.5"))
                pdc.set(ORIGINAL_NAME_KEY, DataType.STRING, AdventureUtils.MINI_MESSAGE.serialize(displayName!!))
            itemMeta.displayName(displayName?.setDefaultStyle())
        }

        enchantments.entries.forEach { enchant: Map.Entry<Enchantment?, Int?> ->
            if (enchant.key == null) return@forEach
            val lvl = enchant.value ?: 1
            itemMeta.addEnchant(enchant.key!!, lvl, true)
        }

        itemMeta.addItemFlags(*itemFlags.toTypedArray())
        attributeModifiers?.also(itemMeta::setAttributeModifiers)
        itemMeta.setCustomModelData(customModelData)

        if (VersionUtil.atleast("1.21.4")) {
            val cmdComponent = (customModelDataComponent ?: itemMeta.customModelDataComponent.takeIf { customModelData != null })?.apply {
                if (customModelData != null) floats = floats.plus(customModelData!!.toFloat()).distinct()
            }
            itemMeta.setCustomModelDataComponent(cmdComponent)
        }

        if (VersionUtil.atleast("1.21.5")) {

        }

        for ((key, value) in persistentDataMap) {
            val dataSpaceKey = key.safeCast<PersistentDataSpace<Any, Any>>() ?: continue
            pdc.set(dataSpaceKey.key, dataSpaceKey.dataType, value)
        }

        itemMeta.lore(lore)

        if (VersionUtil.atleast("1.21.2") && !itemMeta.hasItemModel() && nexoMeta?.dyeableModel != null)
            itemMeta.itemModel = NamespacedKey.fromString("nexo:${NexoItems.idFromItem(this)}")

        itemStack.itemMeta = itemMeta

        NMSHandlers.handler().itemUtils().asColorable(itemStack)?.color = color
        NMSHandlers.handler().itemUtils().consumableComponent(itemStack, consumableComponent)
        NMSHandlers.handler().itemUtils().repairableComponent(itemStack, repairableComponent)
        NMSHandlers.handler().itemUtils().blockstateComponent(itemStack, blockStates)
        NMSHandlers.handler().itemUtils().handleItemFlagToolTips(itemStack, itemFlags)

        if (VersionUtil.atleast("1.21.5") && tooltipDisplay != null) {
            itemStack.setData(DataComponentTypes.TOOLTIP_DISPLAY, tooltipDisplay!!)
        }

        if (VersionUtil.atleast("1.20.5") && NexoFurniture.isFurniture(itemStack)) itemStack.editMeta { meta ->
            when {
                meta is PotionMeta -> {
                    NMSHandlers.handler().itemUtils().consumableComponent(itemStack, null)
                    meta.setFood(null)
                }
                meta is LeatherArmorMeta && VersionUtil.atleast("1.21.2") ->
                    meta.setEquippable(meta.equippable.apply { slot = EquipmentSlot.HAND })
            }
        }

        finalItemStack = itemStack
        return this
    }

    fun save() {
        regenerateItem()
        NexoItems.itemMap().entries.firstOrNull { it.value.containsValue(this) }?.let { (key, _) ->
            val yamlConfig = loadConfiguration(key)
            val itemId = NexoItems.idFromItem(this)
            if (this.hasColor()) {
                val color = color!!.red.toString() + "," + color!!.green + "," + color!!.blue
                yamlConfig.set("$itemId.color", color)
            }
            if (this.hasTrimPattern()) yamlConfig.set("$itemId.trim_pattern", trimPatternKey!!.asString())

            if (!itemFlags.isNullOrEmpty()) yamlConfig.set("$itemId.ItemFlags", itemFlags!!.map(ItemFlag::name))

            if (hasEquippableComponent()) {
                yamlConfig.set("$itemId.Components.equippable.slot", equippable!!.slot.name)
                yamlConfig.set("$itemId.Components.equippable.model", equippable!!.model?.toString())
            }

            runCatching {
                yamlConfig.save(key)
            }.printOnFailure(true)
        }
    }

    private fun handleVariousMeta(itemMeta: ItemMeta) {
        when {
            itemMeta is LeatherArmorMeta && color != null && (color != itemMeta.color) -> itemMeta.setColor(color)
            itemMeta is PotionMeta -> handlePotionMeta(itemMeta)
            itemMeta is MapMeta && color != null && (color != itemMeta.color) -> itemMeta.color = color
            itemMeta is FireworkEffectMeta -> {
                val fireWorkBuilder = FireworkEffect.builder()
                if (color != null) fireWorkBuilder.withColor(color!!)

                runCatching { itemMeta.effect = fireWorkBuilder.build() }
            }
            itemMeta is ArmorMeta && hasTrimPattern() -> {
                itemMeta.trim = ArmorTrim(TrimMaterial.REDSTONE, getTrimPattern()!!)
            }
        }
    }

    private fun handlePotionMeta(potionMeta: PotionMeta): ItemMeta {
        if (color != null && color != potionMeta.color) potionMeta.color = color

        if (basePotionType != null && basePotionType != potionMeta.basePotionType) potionMeta.basePotionType = basePotionType

        customPotionEffects.takeUnless(potionMeta.customEffects::equals)?.forEach { potionEffect: PotionEffect ->
            potionMeta.addCustomEffect(potionEffect, true)
        }

        return potionMeta
    }

    fun buildArray(amount: Int): Array<ItemStack?> {
        val built = build()
        val max = if (hasMaxStackSize()) maxStackSize!! else type.maxStackSize
        val rest = if (max == amount) amount else amount % max
        val iterations = if (amount > max) (amount - rest) / max else 0
        val output = arrayOfNulls<ItemStack>(iterations + (if (rest > 0) 1 else 0))

        for (index in 0 until iterations)
            output[index] = ItemUpdater.updateItem(built.clone().also { it.amount = max })

        if (rest != 0)
            output[iterations] = ItemUpdater.updateItem(built.clone().also { it.amount = rest })

        return output

    }

    fun build(): ItemStack {
        if (!cache) NexoItems.reloadItem(NexoItems.idFromItem(this)!!)
        if (finalItemStack == null) regenerateItem()
        return finalItemStack!!.clone()
    }

    companion object {
        val ORIGINAL_NAME_KEY = NamespacedKey(NexoPlugin.instance(), "original_name")
    }
}
