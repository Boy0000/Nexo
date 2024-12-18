package com.nexomc.nexo.compatibilities

import com.nexomc.nexo.NexoPlugin
import com.nexomc.nexo.compatibilities.blocklocker.BlockLockerCompatibility
import com.nexomc.nexo.compatibilities.modelengine.ModelEngineCompatibility
import com.nexomc.nexo.compatibilities.mythicmobs.MythicMobsCompatibility
import com.nexomc.nexo.compatibilities.placeholderapi.PlaceholderAPICompatibility
import com.nexomc.nexo.compatibilities.worldedit.WrappedWorldEdit
import com.nexomc.nexo.configs.Message
import com.nexomc.nexo.utils.AdventureUtils.tagResolver
import com.nexomc.nexo.utils.PluginUtils
import com.nexomc.nexo.utils.printOnFailure
import org.bukkit.Bukkit
import java.util.concurrent.ConcurrentHashMap

object CompatibilitiesManager {
    private val COMPAT_PROVIDERS: ConcurrentHashMap<String, Class<out CompatibilityProvider<*>>> = ConcurrentHashMap()
    private val ACTIVE_COMPAT_PROVIDERS: ConcurrentHashMap<String, CompatibilityProvider<*>> = ConcurrentHashMap()

    fun enableCompatibilies() {
        WrappedWorldEdit.init()
        WrappedWorldEdit.registerParser()
        Bukkit.getPluginManager().registerEvents(CompatibilityListener(), NexoPlugin.instance())
        addCompatibility("PlaceholderAPI", PlaceholderAPICompatibility::class.java, true)
        addCompatibility("MythicMobs", MythicMobsCompatibility::class.java, true)
        addCompatibility("BlockLocker", BlockLockerCompatibility::class.java, true)
        addCompatibility("ModelEngine", ModelEngineCompatibility::class.java, true)
    }

    fun disableCompatibilities() {
        WrappedWorldEdit.unregister()

        ACTIVE_COMPAT_PROVIDERS.forEach { (pluginName, _) ->
            disableCompatibility(pluginName)
        }
    }

    fun enableCompatibility(pluginName: String) = runCatching {
        if (!ACTIVE_COMPAT_PROVIDERS.containsKey(pluginName) && COMPAT_PROVIDERS.containsKey(pluginName) && PluginUtils.isEnabled(pluginName)) {
            ACTIVE_COMPAT_PROVIDERS[pluginName] = COMPAT_PROVIDERS[pluginName]!!.getConstructor().newInstance().apply {
                enable(pluginName)
            }
            Message.PLUGIN_HOOKS.log(tagResolver("plugin", pluginName))
        }
    }.printOnFailure().getOrNull() != null

    fun disableCompatibility(pluginName: String) {
        runCatching {
            if (!ACTIVE_COMPAT_PROVIDERS.containsKey(pluginName)) return
            ACTIVE_COMPAT_PROVIDERS.remove(pluginName)?.takeIf { it.isEnabled }?.disable()
            Message.PLUGIN_UNHOOKS.log(tagResolver("plugin", pluginName))
        }.printOnFailure().getOrNull() != null
    }

    fun addCompatibility(compatPlugin: String, clazz: Class<out CompatibilityProvider<*>>, tryEnable: Boolean): Boolean {
        return runCatching {
            COMPAT_PROVIDERS[compatPlugin] = clazz
            !tryEnable || enableCompatibility(compatPlugin)
        }.printOnFailure().getOrNull() != null
    }

    fun addCompatibility(compatibilityPluginName: String, clazz: Class<out CompatibilityProvider<*>>) =
        addCompatibility(compatibilityPluginName, clazz, false)
    fun activeCompatibility(pluginName: String) = ACTIVE_COMPAT_PROVIDERS[pluginName]!!
    fun compatibility(pluginName: String) = COMPAT_PROVIDERS[pluginName]!!
    fun isCompatibilityEnabled(pluginName: String) = ACTIVE_COMPAT_PROVIDERS[pluginName]?.isEnabled == true
    fun compatProviders() = COMPAT_PROVIDERS
    fun activeCompatProviders() = ACTIVE_COMPAT_PROVIDERS
}
