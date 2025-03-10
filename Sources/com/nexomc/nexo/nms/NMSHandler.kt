package com.nexomc.nexo.nms

import com.nexomc.nexo.items.ItemBuilder
import com.nexomc.nexo.mechanics.furniture.IFurniturePacketManager
import com.nexomc.nexo.utils.InteractionResult
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.Player
import org.bukkit.event.Listener
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack

interface NMSHandler {

    val resourcePackListener: Listener?
    val pluginConverter: IPluginConverter

    fun furniturePacketManager(): IFurniturePacketManager = EmptyFurniturePacketManager()

    fun packetHandler(): IPacketHandler

    fun noteblockUpdatesDisabled(): Boolean

    fun tripwireUpdatesDisabled(): Boolean

    fun chorusplantUpdateDisabled(): Boolean

    fun resourcepackFormat(): Int
    fun datapackFormat(): Int

    /**
     * Copies over all NBT-Tags from oldItem to newItem
     * Useful for plugins that might register their own NBT-Tags outside
     * the ItemStacks PersistentDataContainer
     *
     * @param oldItem The old ItemStack to copy the NBT-Tags from
     * @param newItem The new ItemStack to copy the NBT-Tags to
     * @return The new ItemStack with the copied NBT-Tags
     */
    fun copyItemNBTTags(oldItem: ItemStack, newItem: ItemStack): ItemStack

    /**
     * Corrects the BlockData of a placed block.
     * Mainly fired when placing a block against an NexoNoteBlock due to vanilla behaviour requiring Sneaking
     *
     * @param player          The player that placed the block
     * @param slot            The hand the player placed the block with
     * @param itemStack       The ItemStack the player placed the block with
     * @param target          The block where the new block will be placed (usually air or another replaceable block)
     * @param blockFace       The face of the block that the player clicked
     * @return The enum interaction result
     */
    fun correctBlockStates(player: Player, slot: EquipmentSlot, itemStack: ItemStack, target: Block, blockFace: BlockFace): InteractionResult?

    fun foodComponent(itemBuilder: ItemBuilder, foodSection: ConfigurationSection) {}

    fun consumableComponent(itemStack: ItemStack?): Any? {
        return null
    }

    fun consumableComponent(itemBuilder: ItemBuilder, consumableSection: ConfigurationSection) {}

    fun consumableComponent(itemStack: ItemStack, consumable: Any?) {}

    fun repairableComponent(itemStack: ItemStack?): Any? {
        return null
    }

    fun repairableComponent(itemBuilder: ItemBuilder, repairableWith: List<String>) {}

    fun repairableComponent(itemStack: ItemStack, repairable: Any?) {}

    fun handleItemFlagToolTips(itemStack: ItemStack) {}

    fun applyMiningEffect(player: Player) {}

    fun removeMiningEffect(player: Player) {}

    fun noteBlockInstrument(block: Block): String

    companion object {
        /**
         * Keys that are used by vanilla Minecraft and should therefore be skipped
         * Some are accessed through API methods, others are just used internally
         */
        @JvmField
        val vanillaKeys = setOf(
            "PublicBukkitValues",
            "display",
            "CustomModelData",
            "Damage",
            "AttributeModifiers",
            "Unbreakable",
            "CanDestroy",
            "slot",
            "count",
            "HideFlags",
            "CanPlaceOn",
            "Enchantments",
            "StoredEnchantments",
            "RepairCost",
            "CustomPotionEffects",
            "Potion",
            "CustomPotionColor",
            "Trim",
            "EntityTag",
            "pages",
            "filtered_pages",
            "filtered_title",
            "resolved",
            "generation",
            "author",
            "title",
            "BucketVariantTag",
            "Items",
            "LodestoneTracked",
            "LodestoneDimension",
            "LodestonePos",
            "ChargedProjectiles",
            "Charged",
            "DebugProperty",
            "Fireworks",
            "Explosion",
            "Flight",
            "map",
            "map_scale_direction",
            "map_to_lock",
            "Decorations",
            "SkullOwner",
            "Effects",
            "BlockEntityTag",
            "BlockStateTag"
        )
    }
}
