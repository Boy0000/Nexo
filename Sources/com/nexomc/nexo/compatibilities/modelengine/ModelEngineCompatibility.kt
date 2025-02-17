package com.nexomc.nexo.compatibilities.modelengine

import com.nexomc.nexo.compatibilities.CompatibilityProvider
import com.nexomc.nexo.configs.Settings
import com.nexomc.nexo.utils.PluginUtils
import com.nexomc.nexo.utils.PluginUtils.isEnabled
import com.nexomc.nexo.utils.logs.Logs
import com.ticxo.modelengine.api.ModelEngineAPI
import com.ticxo.modelengine.api.events.ModelRegistrationEvent
import com.ticxo.modelengine.api.generator.ModelGenerator
import org.bukkit.event.EventHandler
import java.util.concurrent.CompletableFuture

class ModelEngineCompatibility : CompatibilityProvider<ModelEngineAPI>() {
    @EventHandler
    fun ModelRegistrationEvent.onMegReload() {
        when (phase) {
            ModelGenerator.Phase.PRE_IMPORT -> {
                Logs.logInfo("Awaiting ModelEngine ResourcePack...")
                modelEngineFuture = CompletableFuture()
            }
            ModelGenerator.Phase.FINISHED -> {
                modelEngineFuture!!.complete(null)
                Logs.logInfo("ModelEngine ResourcePack is ready.")
            }

            else -> {}
        }
    }

    companion object {
        private var modelEngineFuture: CompletableFuture<Void?>?

        init {
            modelEngineFuture = CompletableFuture()
            if (!PluginUtils.isModelEngineEnabled || !Settings.PACK_IMPORT_MODEL_ENGINE.toBool())
                modelEngineFuture!!.complete(null)
        }

        fun modelEngineFuture(): CompletableFuture<Void?> {
            if (modelEngineFuture == null) modelEngineFuture = CompletableFuture()
            if (!PluginUtils.isModelEngineEnabled || !Settings.PACK_IMPORT_MODEL_ENGINE.toBool())
                modelEngineFuture!!.complete(null)

            return modelEngineFuture!!
        }
    }
}
