package com.nexomc.nexo.utils

import com.nexomc.nexo.NexoPlugin
import org.bstats.bukkit.Metrics
import org.bstats.charts.SimplePie

object NexoMetrics {
    fun initializeMetrics() {
        val metrics = Metrics(NexoPlugin.instance(), 23930)
        metrics.addCustomChart(SimplePie("marketplace") {
            if (VersionUtil.isCompiled) return@SimplePie "Compiled"
            return@SimplePie "Unknown"
        })
    }
}