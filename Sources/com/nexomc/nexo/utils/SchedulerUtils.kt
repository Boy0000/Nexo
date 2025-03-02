package com.nexomc.nexo.utils

import com.nexomc.nexo.NexoPlugin
import com.tcoded.folialib.impl.PlatformScheduler
import com.tcoded.folialib.wrapper.task.WrappedTask
import org.bukkit.Bukkit
import org.bukkit.Chunk
import org.bukkit.Location
import org.bukkit.entity.Entity
import org.bukkit.entity.ItemDisplay
import org.bukkit.scheduler.BukkitTask
import java.util.concurrent.Future

object SchedulerUtils {

    val foliaScheduler: PlatformScheduler get() = NexoPlugin.instance().foliaLib.scheduler

    fun runAtWorldEntities(task: (Entity) -> Unit) {
        if (VersionUtil.isFoliaServer) Bukkit.getWorlds().forEach { world ->
            world.loadedChunks.forEach { chunk ->
                foliaScheduler.runAtLocation(Location(world, chunk.x * 16.0, 100.0, chunk.z * 16.0)) {
                    chunk.entities.forEach { entity ->
                        foliaScheduler.runAtEntity(entity) {
                            task.invoke(entity)
                        }
                    }
                }
            }
        } else Bukkit.getWorlds().flatMapFast { it.entities }.filterFastIsInstance<ItemDisplay>().forEach(task::invoke)
    }

    fun runTaskLater(delay: Long, task: () -> Unit) {
        foliaScheduler.runLater(task, delay)
    }

    fun runTaskTimer(delay: Long, period: Long, task: () -> Unit): BukkitTask {
        return Bukkit.getScheduler().runTaskTimer(NexoPlugin.instance(), task, delay, period)
    }

    fun syncDelayedTask(delay: Long = 0L, task: () -> Unit) {
        foliaScheduler.runLater(task, delay)
    }

    fun callSyncMethod(task: () -> Unit): Future<*> {
        return foliaScheduler.runNextTick { task() }
    }

    fun runTaskAsync(task: () -> Unit) {
        Bukkit.getScheduler().runTaskAsynchronously(NexoPlugin.instance(), task)
    }

    fun runTaskAsyncLater(delay: Long, task: () -> Unit) {
        Bukkit.getScheduler().runTaskLaterAsynchronously(NexoPlugin.instance(), task, delay)
    }

    fun runTask(task: () -> Unit) {
        Bukkit.getScheduler().runTask(NexoPlugin.instance(), task)
    }
}