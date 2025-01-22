package com.nexomc.nexo.utils

import com.nexomc.nexo.NexoPlugin
import org.bstats.bukkit.Metrics

object NexoMetrics {
    fun initializeMetrics() {
        val metrics = Metrics(NexoPlugin.instance(), 23930)
    }
}