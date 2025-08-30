package com.nexomc.nexo.compatibilities.modelengine

import com.github.shynixn.mccoroutine.folia.launch
import com.nexomc.nexo.NexoPlugin
import com.nexomc.nexo.compatibilities.CompatibilityProvider
import com.nexomc.nexo.configs.Settings
import com.nexomc.nexo.utils.PluginUtils
import com.nexomc.nexo.utils.logs.Logs
import com.ticxo.modelengine.api.ModelEngineAPI
import com.ticxo.modelengine.api.events.ModelRegistrationEvent
import com.ticxo.modelengine.api.generator.ModelGenerator
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import org.bukkit.event.EventHandler

class ModelEngineCompatibility : CompatibilityProvider<ModelEngineAPI>() {

    @EventHandler
    fun ModelRegistrationEvent.onMegReload() {
        when (phase) {
            ModelGenerator.Phase.PRE_IMPORT -> {
                Logs.logInfo("Awaiting ModelEngine ResourcePack...")
                megJob = NexoPlugin.instance().launch(start = CoroutineStart.LAZY) {}
            }
            ModelGenerator.Phase.FINISHED -> {
                megJob?.start()
                Logs.logInfo("ModelEngine ResourcePack is ready.")
            }
            else -> {}
        }
    }

    companion object {
        private var megJob: Job? = null

        init {
            megJob = NexoPlugin.instance().launch(start = CoroutineStart.LAZY) {}
            if (!PluginUtils.isModelEngineEnabled || !Settings.PACK_IMPORT_MODEL_ENGINE.toBool()) {
                megJob?.start()
            }
        }

        fun megJob(): Job {
            if (megJob == null) megJob = NexoPlugin.instance().launch(start = CoroutineStart.LAZY) {}
            if (!PluginUtils.isModelEngineEnabled || !Settings.PACK_IMPORT_MODEL_ENGINE.toBool()) {
                megJob?.start()
            }
            return megJob!!
        }
    }
}
