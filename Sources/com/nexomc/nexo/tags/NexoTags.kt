package com.nexomc.nexo.tags

import io.papermc.paper.plugin.bootstrap.BootstrapContext
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents
import io.papermc.paper.registry.RegistryKey
import io.papermc.paper.registry.TypedKey
import io.papermc.paper.registry.tag.TagKey
import net.kyori.adventure.key.Key

object NexoTags {

    private val MINEABLE_AXE = TagKey.create(RegistryKey.BLOCK, (Key.key("mineable/axe")))
    private val NOTEBLOCK = TypedKey.create(RegistryKey.BLOCK, Key.key("note_block"))

    @JvmStatic
    fun registerTags(context: BootstrapContext) {
        context.lifecycleManager.registerEventHandler(LifecycleEvents.TAGS.postFlatten(RegistryKey.BLOCK)) { event ->
            event.registrar().setTag(MINEABLE_AXE, event.registrar().getTag(MINEABLE_AXE).minus(NOTEBLOCK))
        }
    }
}