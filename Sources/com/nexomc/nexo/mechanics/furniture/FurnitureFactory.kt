package com.nexomc.nexo.mechanics.furniture

import com.nexomc.nexo.NexoPlugin
import com.nexomc.nexo.converter.ConverterListener
import com.nexomc.nexo.mechanics.MechanicFactory
import com.nexomc.nexo.mechanics.MechanicsManager
import com.nexomc.nexo.mechanics.furniture.compatibility.SpartanCompatibility
import com.nexomc.nexo.mechanics.furniture.compatibility.VulcanCompatibility
import com.nexomc.nexo.mechanics.furniture.evolution.EvolutionListener
import com.nexomc.nexo.mechanics.furniture.evolution.EvolutionTask
import com.nexomc.nexo.mechanics.furniture.jukebox.JukeboxListener
import com.nexomc.nexo.mechanics.furniture.listeners.FurnitureBarrierHitboxListener
import com.nexomc.nexo.mechanics.furniture.listeners.FurnitureListener
import com.nexomc.nexo.mechanics.furniture.listeners.FurniturePacketListener
import com.nexomc.nexo.mechanics.furniture.listeners.FurnitureSoundListener
import com.nexomc.nexo.nms.EmptyFurniturePacketManager
import com.nexomc.nexo.nms.NMSHandlers
import com.nexomc.nexo.utils.PluginUtils
import com.nexomc.nexo.utils.VersionUtil
import com.nexomc.nexo.utils.logs.Logs
import org.bukkit.Bukkit
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.inventory.ItemStack
import kotlin.math.pow

class FurnitureFactory(section: ConfigurationSection) : MechanicFactory(section) {
    val toolTypes: List<String> = section.getStringList("tool_types")
    val evolutionCheckDelay: Int = section.getInt("evolution_check_delay")
    private val customSounds: Boolean = section.getBoolean("custom_block_sounds", true)
    private var evolvingFurnitures: Boolean
    var simulationRadius = Bukkit.getServer().simulationDistance.times(16.0).pow(2.0)

    init {
        instance = this
        registerListeners(FurnitureListener(), EvolutionListener(), JukeboxListener())

        if (VersionUtil.isPaperServer) registerListeners(FurniturePacketListener(), FurnitureBarrierHitboxListener())
        else {
            Logs.logWarn("Seems that your server is a Spigot-server")
            Logs.logWarn("FurnitureHitboxes will not work properly due to it relying on Paper-only events")
            Logs.logWarn("It is heavily recommended to make the upgrade to Paper", true)
        }

        evolvingFurnitures = false

        if (PluginUtils.isEnabled("Spartan"))
            registerListeners(SpartanCompatibility())
        if (PluginUtils.isEnabled("Vulcan"))
            registerListeners(VulcanCompatibility())

        if (NexoPlugin.instance().converter().oraxenConverter.convertFurnitureOnLoad)
            registerListeners(ConverterListener())

        if (customSounds) registerListeners(FurnitureSoundListener())
    }

    fun packetManager(): IFurniturePacketManager = if (VersionUtil.isPaperServer) NMSHandlers.handler().furniturePacketManager() else EmptyFurniturePacketManager()

    override fun parse(section: ConfigurationSection) = FurnitureMechanic(this, section).apply(::addToImplemented)

    fun registerEvolution() {
        if (evolvingFurnitures) return
        evolutionTask?.cancel()
        evolutionTask = EvolutionTask(this, evolutionCheckDelay)
        val task = evolutionTask!!.runTaskTimer(NexoPlugin.instance(), 0, evolutionCheckDelay.toLong())
        MechanicsManager.registerTask(mechanicID, task)
        evolvingFurnitures = true
    }

    override fun getMechanic(itemID: String?) = super.getMechanic(itemID) as? FurnitureMechanic?

    override fun getMechanic(itemStack: ItemStack?) = super.getMechanic(itemStack) as? FurnitureMechanic?

    companion object {
        private var instance: FurnitureFactory? = null
        private var evolutionTask: EvolutionTask? = null
        val isEnabled: Boolean
            get() = instance != null

        fun instance(): FurnitureFactory? {
            return instance
        }

        fun unregisterEvolution() {
            if (evolutionTask != null) evolutionTask!!.cancel()
        }

        fun removeAllFurniturePackets() {
            if (instance == null) return
            instance!!.packetManager().removeAllFurniturePackets()
        }
    }
}
