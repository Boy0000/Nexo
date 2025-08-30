package com.nexomc.nexo.utils

import com.github.shynixn.mccoroutine.folia.asyncDispatcher
import com.github.shynixn.mccoroutine.folia.entityDispatcher
import com.github.shynixn.mccoroutine.folia.launch
import com.github.shynixn.mccoroutine.folia.regionDispatcher
import com.github.shynixn.mccoroutine.folia.scope
import com.nexomc.nexo.NexoPlugin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import org.bukkit.Bukkit
import org.bukkit.Chunk
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.block.BlockState
import org.bukkit.entity.Entity
import java.util.function.Predicate
import kotlin.coroutines.CoroutineContext


suspend fun withContext(entity: Entity, delay: Long? = null, block: suspend CoroutineScope.() -> Unit) {
    kotlinx.coroutines.withContext(SchedulerUtils.entityDispatcher(entity)) {
        if (delay != null) delay(delay)
        block(this)
    }
}

suspend fun withContext(location: Location, delay: Long? = null, block: suspend CoroutineScope.() -> Unit) {
    kotlinx.coroutines.withContext(SchedulerUtils.regionDispatcher(location)) {
        if (delay != null) delay(delay)
        block(this)
    }
}

suspend fun withContext(delay: Long? = null, block: suspend CoroutineScope.() -> Unit) {
    kotlinx.coroutines.withContext(SchedulerUtils.minecraftDispatcher) {
        if (delay != null) delay(delay)
        block(this)
    }
}

object SchedulerUtils {

    val minecraftDispatcher get() = NexoPlugin.instance().scope.coroutineContext
    val asyncDispatcher get() = NexoPlugin.instance().asyncDispatcher
    fun entityDispatcher(entity: Entity) = NexoPlugin.instance().entityDispatcher(entity)
    fun regionDispatcher(location: Location) = NexoPlugin.instance().regionDispatcher(location)
    fun regionDispatcher(world: World, chunk: Chunk) = NexoPlugin.instance().regionDispatcher(world, chunk.x, chunk.z)

    fun launch(entity: Entity, async: Boolean = false, block: suspend CoroutineScope.() -> Unit): Job {
        return when {
            async && !VersionUtil.isFoliaServer -> NexoPlugin.instance().launch(asyncDispatcher, block = block)
            else -> NexoPlugin.instance().launch(entityDispatcher(entity), block = block)
        }
    }

    fun launch(location: Location, block: suspend CoroutineScope.() -> Unit): Job {
        return NexoPlugin.instance().launch(regionDispatcher(location), block = block)
    }

    fun launch(world: World, chunk: Chunk, block: suspend CoroutineScope.() -> Unit): Job {
        return NexoPlugin.instance().launch(regionDispatcher(world, chunk), block = block)
    }

    fun launch(context: CoroutineContext, block: suspend CoroutineScope.() -> Unit): Job {
        return NexoPlugin.instance().launch(context, block = block)
    }

    fun launch(block: suspend CoroutineScope.() -> Unit): Job {
        return NexoPlugin.instance().launch(minecraftDispatcher, block = block)
    }

    fun launchAsync(block: suspend CoroutineScope.() -> Unit): Job {
        return when {
            VersionUtil.isFoliaServer -> NexoPlugin.instance().launch(minecraftDispatcher, block = block)
            else -> NexoPlugin.instance().launch(asyncDispatcher, block = block)
        }
    }

    fun launchDelayed(delay: Long = 1, block: suspend CoroutineScope.() -> Unit): Job {
        return launch {
            delay(delay)
            block.invoke(this)
        }
    }

    fun launchDelayed(entity: Entity, delay: Long = 1, async: Boolean = false, block: suspend CoroutineScope.() -> Unit): Job {
        return when {
            async && !VersionUtil.isFoliaServer -> NexoPlugin.instance().launch(asyncDispatcher) {
                delay(delay)
                block.invoke(this)
            }
            else -> NexoPlugin.instance().launch(entityDispatcher(entity)) {
                delay(delay)
                block.invoke(this)
            }
        }
    }

    fun launchDelayed(location: Location, delay: Long = 1, block: suspend CoroutineScope.() -> Unit): Job {
        return NexoPlugin.instance().launch(regionDispatcher(location)) {
            delay(delay)
            block.invoke(this)
        }
    }

    fun launchDelayed(world: World, chunk: Chunk, delay: Long = 1, block: suspend CoroutineScope.() -> Unit): Job {
        return NexoPlugin.instance().launch(regionDispatcher(world, chunk)) {
            delay(delay)
            block.invoke(this)
        }
    }

    fun launchRepeating(initialDelay: Long, delay: Long, block: suspend CoroutineScope.() -> Unit): Job {
        return launch {
            delay(initialDelay)
            while (true) {
                block.invoke(this)
                delay(delay)
            }
        }
    }

    fun launchRepeating(location: Location, initialDelay: Long, delay: Long, block: suspend CoroutineScope.() -> Unit): Job {
        return launch(location) {
            delay(initialDelay)
            while (true) {
                block.invoke(this)
                delay(delay)
            }
        }
    }

    @JvmName("_runAtWorldEntities")
    fun runAtWorldEntities(task: (Entity) -> Unit) {
        runAtWorldEntities<Entity>(task)
    }
    inline fun <reified T : Entity> runAtWorldEntities(noinline task: (T) -> Unit) {
        Bukkit.getWorlds().forEach { world ->
            if (VersionUtil.isFoliaServer) world.loadedChunks.forEach { chunk ->
                launch(world, chunk) {
                    chunk.entities.forEach { entity ->
                        if (entity is T) launch(entity) { task.invoke(entity) }
                    }
                }
            } else world.getEntitiesByClass(T::class.java).forEach(task::invoke)
        }
    }

    fun runAtWorldTileStates(predicate: Predicate<Block>, task: (BlockState) -> Unit) {
        Bukkit.getWorlds().forEach { world ->
            world.loadedChunks.forEach { chunk ->
                if (VersionUtil.isFoliaServer) launch(world, chunk) {
                    chunk.getTileEntities(predicate, false).forEach(task::invoke)
                } else chunk.getTileEntities(false).forEach(task::invoke)
            }
        }
    }
}