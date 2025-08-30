package com.nexomc.nexo.mechanics

import com.github.shynixn.mccoroutine.folia.registerSuspendingEvents
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
import com.nexomc.nexo.utils.coroutines.SuspendingListener
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import kotlinx.coroutines.Job
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.plugin.java.JavaPlugin
import javax.annotation.Nullable

object MechanicsManager {
    private val FACTORIES_BY_MECHANIC_ID = Object2ObjectOpenHashMap<String, MechanicFactory>()
    private val MECHANIC_TASKS = Object2ObjectOpenHashMap<String, ObjectArrayList<Job>>()
    private val MECHANICS_LISTENERS = Object2ObjectOpenHashMap<String, ObjectArrayList<Listener>>()

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
        else SchedulerUtils.launch { NexoMechanicsRegisteredEvent().call() }
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

    fun registerTask(mechanicId: String, task: Job) {
        MECHANIC_TASKS.compute(mechanicId) { _, value ->
            (value ?: ObjectArrayList()).also { it += task }
        }
    }

    fun unregisterTasks() {
        MECHANIC_TASKS.onEach {
            it.value.forEach(Job::cancel)
        }.clear()
    }

    fun unregisterTasks(mechanicId: String) {
        MECHANIC_TASKS.computeIfPresent(mechanicId) { _, value ->
            value.forEach(Job::cancel)
            ObjectArrayList()
        }
    }

    fun registerListeners(plugin: JavaPlugin, mechanicId: String, vararg listeners: Listener) {
        for (listener in listeners) plugin.server.pluginManager.registerEvents(listener, plugin)
        MECHANICS_LISTENERS.compute(mechanicId) { _, value ->
            (value ?: ObjectArrayList()).also { it += listeners }
        }
    }

    fun registerSuspendingListeners(plugin: JavaPlugin, mechanicId: String, vararg listeners: SuspendingListener) {
        for (listener in listeners) plugin.server.pluginManager.registerSuspendingEvents(listener, plugin, listener.dispatchers)
        MECHANICS_LISTENERS.compute(mechanicId) { _, value ->
            (value ?: ObjectArrayList()).also { it += listeners }
        }
    }

    fun unregisterListeners() {
        MECHANICS_LISTENERS.values.forEach { it.forEach(HandlerList::unregisterAll) }
    }

    fun unregisterListeners(mechanicId: String?) {
        MECHANICS_LISTENERS.remove(mechanicId)?.forEach(HandlerList::unregisterAll)
    }

    @Deprecated(message = "", replaceWith = ReplaceWith(expression = "mechanicFactory"))
    fun getMechanicFactory(mechanicID: String?) = FACTORIES_BY_MECHANIC_ID[mechanicID]

    @Nullable
    fun mechanicFactory(factoryId: String?) = FACTORIES_BY_MECHANIC_ID[factoryId]

    fun interface FactoryConstructor {
        fun create(section: ConfigurationSection): MechanicFactory
    }
}
