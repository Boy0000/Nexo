package com.nexomc.nexo.mechanics

import com.nexomc.nexo.NexoPlugin
import com.nexomc.nexo.api.events.NexoMechanicsRegisteredEvent
import com.nexomc.nexo.mechanics.combat.lifesteal.LifeStealMechanicFactory
import com.nexomc.nexo.mechanics.combat.spell.energyblast.EnergyBlastMechanicFactory
import com.nexomc.nexo.mechanics.combat.spell.fireball.FireballMechanicFactory
import com.nexomc.nexo.mechanics.combat.spell.thor.ThorMechanicFactory
import com.nexomc.nexo.mechanics.combat.spell.witherskull.WitherSkullMechanicFactory
import com.nexomc.nexo.mechanics.custom_block.CustomBlockFactory
import com.nexomc.nexo.mechanics.custom_block.chorusblock.ChorusBlockFactory
import com.nexomc.nexo.mechanics.custom_block.noteblock.NoteBlockMechanicFactory
import com.nexomc.nexo.mechanics.custom_block.stringblock.StringBlockMechanicFactory
import com.nexomc.nexo.mechanics.farming.harvesting.HarvestingMechanicFactory
import com.nexomc.nexo.mechanics.farming.smelting.SmeltingMechanicFactory
import com.nexomc.nexo.mechanics.furniture.FurnitureFactory
import com.nexomc.nexo.mechanics.misc.armor_effects.ArmorEffectsFactory
import com.nexomc.nexo.mechanics.misc.backpack.BackpackMechanicFactory
import com.nexomc.nexo.mechanics.misc.commands.CommandsMechanicFactory
import com.nexomc.nexo.mechanics.misc.custom.CustomMechanicFactory
import com.nexomc.nexo.mechanics.misc.itemtype.ItemTypeMechanicFactory
import com.nexomc.nexo.mechanics.misc.misc.MiscMechanicFactory
import com.nexomc.nexo.mechanics.misc.soulbound.SoulBoundMechanicFactory
import com.nexomc.nexo.mechanics.repair.RepairMechanicFactory
import com.nexomc.nexo.mechanics.trident.TridentFactory
import com.nexomc.nexo.utils.EventUtils.call
import com.nexomc.nexo.utils.SchedulerUtils
import com.tcoded.folialib.wrapper.task.WrappedTask
import java.util.Collections
import org.bukkit.Bukkit
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.plugin.java.JavaPlugin

object MechanicsManager {
    private val FACTORIES_BY_MECHANIC_ID = mutableMapOf<String, MechanicFactory>()
    private val MECHANIC_TASKS = mutableMapOf<String, MutableList<WrappedTask>>()
    private val MECHANICS_LISTENERS = mutableMapOf<String, MutableList<Listener>>()

    fun registerNativeMechanics(reload: Boolean) {
        // misc
        registerMechanicFactory("armor_effects", ::ArmorEffectsFactory)
        registerMechanicFactory("soulbound", ::SoulBoundMechanicFactory)
        registerMechanicFactory("itemtype", ::ItemTypeMechanicFactory)
        registerMechanicFactory("custom", ::CustomMechanicFactory)
        registerMechanicFactory("commands", ::CommandsMechanicFactory)
        registerMechanicFactory("backpack", ::BackpackMechanicFactory)
        registerMechanicFactory("misc", ::MiscMechanicFactory)
        registerMechanicFactory("trident", ::TridentFactory)

        // gameplay
        registerMechanicFactory("furniture", ::FurnitureFactory)
        registerMechanicFactory("noteblock", ::NoteBlockMechanicFactory, "custom_blocks.noteblock")
        registerMechanicFactory("stringblock", ::StringBlockMechanicFactory, "custom_blocks.stringblock")
        registerMechanicFactory("chorusblock", ::ChorusBlockFactory, "custom_blocks.chorusblock")
        registerMechanicFactory("custom_block", ::CustomBlockFactory, "custom_blocks")

        // combat
        registerMechanicFactory("thor", ::ThorMechanicFactory)
        registerMechanicFactory("lifesteal", ::LifeStealMechanicFactory)
        registerMechanicFactory("energyblast", ::EnergyBlastMechanicFactory)
        registerMechanicFactory("witherskull", ::WitherSkullMechanicFactory)
        registerMechanicFactory("fireball", ::FireballMechanicFactory)

        // farming
        registerMechanicFactory("smelting", ::SmeltingMechanicFactory)
        registerMechanicFactory("harvesting", ::HarvestingMechanicFactory)
        registerMechanicFactory("repair", ::RepairMechanicFactory)

        if (reload) NexoMechanicsRegisteredEvent().call()
        // Schedule sync as this is called during onEnable
        // Need to register mechanics & listeners first but call the event before items are parsed post-onEnable
        else SchedulerUtils.syncDelayedTask { NexoMechanicsRegisteredEvent().call() }
    }

    /**
     * Register a new MechanicFactory
     *
     * @param factory    the MechanicFactory of the mechanic
     * @param enabled    if the mechanic should be enabled by default or not
     */
    fun registerMechanicFactory(factory: MechanicFactory, enabled: Boolean) {
        if (enabled) FACTORIES_BY_MECHANIC_ID[factory.mechanicID] = factory
    }

    fun unregisterMechanicFactory(mechanicId: String) {
        FACTORIES_BY_MECHANIC_ID.remove(mechanicId)
        unregisterListeners(mechanicId)
        unregisterTasks(mechanicId)
    }

    private fun registerMechanicFactory(mechanicId: String, constructor: FactoryConstructor, configPath: String = mechanicId) {
        val factorySection = NexoPlugin.instance().resourceManager().mechanics.config.getConfigurationSection(configPath) ?: return
        if (factorySection.getBoolean("enabled")) FACTORIES_BY_MECHANIC_ID[mechanicId] = constructor.create(factorySection)
    }

    fun registerTask(mechanicId: String, task: WrappedTask) {
        MECHANIC_TASKS.compute(mechanicId) { _, value ->
            (value ?: mutableListOf()).also { it += task }
        }
    }

    fun unregisterTasks() {
        MECHANIC_TASKS.values.forEach { tasks ->
            tasks.forEach { taskId -> SchedulerUtils.foliaScheduler.cancelTask(taskId) }
        }
        MECHANIC_TASKS.clear()
    }

    fun unregisterTasks(mechanicId: String) {
        MECHANIC_TASKS.computeIfPresent(mechanicId) { _, value ->
            value.forEach { taskId ->
                SchedulerUtils.foliaScheduler.cancelTask(taskId)
            }
            Collections.emptyList()
        }
    }

    fun registerListeners(plugin: JavaPlugin, mechanicId: String, vararg listeners: Listener) {
        for (listener: Listener in listeners) Bukkit.getPluginManager().registerEvents(listener, plugin)
        MECHANICS_LISTENERS.compute(mechanicId) { _, value ->
            (value ?: mutableListOf()).also { it += listeners }
        }
    }

    fun unregisterListeners() {
        for (listener in MECHANICS_LISTENERS.values.flatten()) HandlerList.unregisterAll(listener)
    }

    fun unregisterListeners(mechanicId: String?) {
        MECHANICS_LISTENERS.remove(mechanicId)?.forEach(HandlerList::unregisterAll)
    }

    fun getMechanicFactory(mechanicID: String?) = FACTORIES_BY_MECHANIC_ID[mechanicID]

    fun interface FactoryConstructor {
        fun create(section: ConfigurationSection): MechanicFactory
    }
}
