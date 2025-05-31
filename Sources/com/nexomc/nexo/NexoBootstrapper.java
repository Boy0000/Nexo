package com.nexomc.nexo;

import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.TypedKey;
import io.papermc.paper.registry.tag.TagKey;
import io.papermc.paper.tag.PostFlattenTagRegistrar;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Collection;
import java.util.stream.Collectors;

public class NexoBootstrapper implements PluginBootstrap {

    public static Boolean bootsStrung = false;

    @Override
    public void bootstrap(BootstrapContext context) {
        try {
            registerTags(context); // Nothing gets resolved yet
            bootsStrung = true;
        } catch (Throwable t) {
            context.getLogger().error("Failed to bootstrap Nexo: %s".formatted(t.getMessage()));
        }
    }

    private void registerTags(BootstrapContext context) {
        final TagKey MINEABLE_AXE = TagKey.create(RegistryKey.BLOCK, Key.key("mineable/axe"));

        context.getLifecycleManager().registerEventHandler(LifecycleEvents.TAGS.postFlatten(RegistryKey.BLOCK), (event) -> {
            PostFlattenTagRegistrar registrar = event.registrar();
            @Unmodifiable Collection<TypedKey> mineableAxeTag = registrar.getTag(MINEABLE_AXE);
            registrar.setTag(MINEABLE_AXE, mineableAxeTag.stream()
                    .filter(t -> !t.key().asString().contains("note_block"))
                    .collect(Collectors.toSet()));
        });
    }
}
