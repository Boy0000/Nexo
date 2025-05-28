package com.nexomc.nexo;

import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEvent;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.TypedKey;
import io.papermc.paper.registry.event.RegistryEvents;
import io.papermc.paper.registry.tag.TagKey;
import io.papermc.paper.tag.PostFlattenTagRegistrar;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Collection;
import java.util.stream.Collectors;

public class NexoBootstrapper implements PluginBootstrap {
    private static final TagKey MINEABLE_AXE = TagKey.create(RegistryKey.BLOCK, Key.key("mineable/axe"));

    @Override
    public void bootstrap(io.papermc.paper.plugin.bootstrap.BootstrapContext context) {
        // Remove note blocks from the mineable axe tag, as they are not actually axe blocks.
        context.getLifecycleManager().registerEventHandler(LifecycleEvents.TAGS.postFlatten(RegistryKey.BLOCK), (event) -> {
            PostFlattenTagRegistrar registrar = event.registrar();
            @Unmodifiable Collection<TypedKey> mineableAxeTag = registrar.getTag(MINEABLE_AXE);
            registrar.setTag(MINEABLE_AXE, mineableAxeTag.stream().filter(t -> !t.key().asString().contains("note_block")).collect(Collectors.toSet()));
        });
    }
}
