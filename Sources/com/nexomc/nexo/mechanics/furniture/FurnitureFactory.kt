package com.nexomc.nexo.mechanics.furniture

import com.nexomc.nexo.NexoPlugin
import com.nexomc.nexo.converter.ItemsAdderConverterListener
import com.nexomc.nexo.converter.OraxenConverterListener
import com.nexomc.nexo.mechanics.MechanicFactory
import com.nexomc.nexo.mechanics.MechanicsManager
import com.nexomc.nexo.mechanics.furniture.compatibility.ModelEngineCompatibility
import com.nexomc.nexo.mechanics.furniture.compatibility.SpartanCompatibility
import com.nexomc.nexo.mechanics.furniture.compatibility.VacanCompatibility
import com.nexomc.nexo.mechanics.furniture.compatibility.VulcanCompatibility
import com.nexomc.nexo.mechanics.furniture.evolution.EvolutionListener
import com.nexomc.nexo.mechanics.furniture.evolution.EvolutionTask
import com.nexomc.nexo.mechanics.furniture.jukebox.JukeboxListener
import com.nexomc.nexo.mechanics.furniture.listeners.FurnitureBarrierHitboxListener
import com.nexomc.nexo.mechanics.furniture.listeners.FurnitureListener
import com.nexomc.nexo.mechanics.furniture.listeners.FurniturePacketListener
import com.nexomc.nexo.mechanics.furniture.listeners.FurnitureSoundListener
import com.nexomc.nexo.nms.NMSHandlers
import com.nexomc.nexo.utils.PluginUtils
import com.nexomc.nexo.utils.blocksounds.BlockSounds
import com.tcoded.folialib.wrapper.task.WrappedBukkitTask
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.inventory.ItemStack
import team.unnamed.creative.ResourcePack
import team.unnamed.creative.sound.SoundRegistry

class FurnitureFactory(section: ConfigurationSection) : MechanicFactory(section) {
    val toolTypes: List<String> = section.getStringList("tool_types")
    private val evolutionCheckDelay: Int = section.getInt("evolution_check_delay")
    val customSounds: Boolean = section.getBoolean("custom_block_sounds", true)
    private var evolvingFurnitures: Boolean
    val defaultRotatableOnSneak = section.getBoolean("default_rotatable_on_sneak", false)
    val handleNonPlayerBarrierCollision = section.getBoolean("handle_non_player_barrier_collision", false)
    val tryPreventingBarrierKick = section.getBoolean("try_preventing_barrier_kick", false)

    init {
        instance = this
        registerListeners(
            FurnitureListener(),
            FurniturePacketListener(),
            FurnitureBarrierHitboxListener(),
            EvolutionListener(),
            JukeboxListener(),
        )

        evolvingFurnitures = false

        if (PluginUtils.isVacanEnabled) registerListeners(VacanCompatibility())
        if (PluginUtils.isSpartanEnabled) registerListeners(SpartanCompatibility())
        if (PluginUtils.isVulcanEnabled) registerListeners(VulcanCompatibility())
        if (PluginUtils.isModelEngineEnabled) registerListeners(ModelEngineCompatibility())

        if (NexoPlugin.instance().converter().oraxenConverter.convertFurnitureOnLoad)
            registerListeners(OraxenConverterListener())
        if (NexoPlugin.instance().converter().itemsadderConverter.convertFurnitureOnLoad)
            registerListeners(ItemsAdderConverterListener())

        if (customSounds) registerListeners(FurnitureSoundListener())
    }

    fun soundRegistries(resourcePack: ResourcePack) {
        if (customSounds) arrayOf(BlockSounds.NEXO_STONE_SOUND_REGISTRY, BlockSounds.VANILLA_STONE_SOUND_REGISTRY).forEach { soundRegistry: SoundRegistry ->
            (resourcePack.soundRegistry(soundRegistry.namespace())?.let {
                SoundRegistry.soundRegistry().sounds(soundRegistry.sounds().plus(it.sounds())).namespace(soundRegistry.namespace()).build()
            } ?: soundRegistry).addTo(resourcePack)
        }
    }

    fun packetManager(): IFurniturePacketManager = NMSHandlers.handler().furniturePacketManager()

    override fun parse(section: ConfigurationSection) = FurnitureMechanic(this, section).apply(::addToImplemented)

    fun registerEvolution() {
        if (evolvingFurnitures) return
        evolutionTask?.cancel()
        evolutionTask = EvolutionTask(this, evolutionCheckDelay)
        val task = evolutionTask!!.runTaskTimer(NexoPlugin.instance(), 0, evolutionCheckDelay.toLong())
        MechanicsManager.registerTask(mechanicID, WrappedBukkitTask(task))
        evolvingFurnitures = true
    }

    override fun getMechanic(itemID: String?) = super.getMechanic(itemID) as? FurnitureMechanic

    override fun getMechanic(itemStack: ItemStack?) = super.getMechanic(itemStack) as? FurnitureMechanic

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
