package com.nexomc.nexo.pack.creative;

import com.google.gson.stream.JsonWriter;
import com.nexomc.nexo.NexoPlugin;
import com.nexomc.nexo.configs.Settings;
import com.nexomc.nexo.nms.NMSHandlers;
import com.nexomc.nexo.utils.logs.Logs;
import net.kyori.adventure.key.Keyed;
import org.jetbrains.annotations.NotNull;
import team.unnamed.creative.ResourcePack;
import team.unnamed.creative.base.Writable;
import team.unnamed.creative.metadata.Metadata;
import team.unnamed.creative.metadata.overlays.OverlayEntry;
import team.unnamed.creative.metadata.overlays.OverlaysMeta;
import team.unnamed.creative.metadata.pack.PackFormat;
import team.unnamed.creative.metadata.pack.PackMeta;
import team.unnamed.creative.overlay.Overlay;
import team.unnamed.creative.overlay.ResourceContainer;
import team.unnamed.creative.part.ResourcePackPart;
import team.unnamed.creative.serialize.minecraft.MinecraftResourcePackWriter;
import team.unnamed.creative.serialize.minecraft.ResourceCategories;
import team.unnamed.creative.serialize.minecraft.ResourceCategory;
import team.unnamed.creative.serialize.minecraft.equipment.EquipmentSerializer;
import team.unnamed.creative.serialize.minecraft.fs.FileTreeWriter;
import team.unnamed.creative.serialize.minecraft.io.JsonResourceSerializer;
import team.unnamed.creative.serialize.minecraft.io.ResourceSerializer;
import team.unnamed.creative.serialize.minecraft.metadata.MetadataSerializer;
import team.unnamed.creative.serialize.minecraft.sound.SoundRegistrySerializer;
import team.unnamed.creative.sound.SoundRegistry;
import team.unnamed.creative.texture.Texture;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.Map;

import static team.unnamed.creative.serialize.minecraft.MinecraftResourcePackStructure.PACK_ICON_FILE;
import static team.unnamed.creative.serialize.minecraft.MinecraftResourcePackStructure.PACK_METADATA_FILE;


public class NexoPackWriter implements MinecraftResourcePackWriter {

    public static NexoPackWriter INSTANCE = new NexoPackWriter();
    public static void resetWriter() {
        INSTANCE = new NexoPackWriter();
    }

    private static final int targetPackFormat = NMSHandlers.handler().resourcepackFormat();
    private static final boolean prettyPrinting = !Settings.PACK_MINIMIZE_JSON.toBool();

    private NexoPackWriter() {
    }

    public <T extends Keyed & ResourcePackPart> void writeFullCategory(
            final @NotNull String basePath,
            final @NotNull ResourceContainer resourceContainer,
            final @NotNull FileTreeWriter target,
            final @NotNull ResourceCategory<T> category,
            final int localTargetPackFormat
    ) {
        for (T resource : category.lister().apply(resourceContainer)) {
            String path = basePath + category.pathOf(resource, localTargetPackFormat);
            final ResourceSerializer<T> serializer = category.serializer();

            if (serializer instanceof JsonResourceSerializer) {
                // if it's a JSON serializer, we can use our own method, that will
                // do some extra configuration
                writeToJson(target, (JsonResourceSerializer<T>) serializer, resource, path, localTargetPackFormat);
                if (serializer instanceof EquipmentSerializer) {
                    // We want to add both paths, so run writeToJson for old/new packformat aswell
                    try {
                        int altPackFormat = localTargetPackFormat > 42 ? 42 : 46;
                        path = basePath + category.pathOf(resource, altPackFormat);
                        writeToJson(target, (JsonResourceSerializer<T>) serializer, resource, path, altPackFormat);
                    } catch (Exception ignored) {}
                }
            } else {
                try (OutputStream output = target.openStream(path)) {
                    category.serializer().serialize(resource, output, localTargetPackFormat);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
        }
    }

    private void writeWithBasePathAndTargetPackFormat(FileTreeWriter target, ResourceContainer container, String basePath, final int localTargetPackFormat) {
        // write resources from most categories
        for (ResourceCategory<?> category : ResourceCategories.categories()) {
            writeFullCategory(basePath, container, target, category, localTargetPackFormat);
        }

        // write sound registries
        for (SoundRegistry soundRegistry : container.soundRegistries()) {
            writeToJson(target, SoundRegistrySerializer.INSTANCE, soundRegistry, basePath + team.unnamed.creative.serialize.minecraft.MinecraftResourcePackStructure.pathOf(soundRegistry), localTargetPackFormat);
        }

        // write textures
        for (Texture texture : container.textures()) {
            target.write(
                    basePath + team.unnamed.creative.serialize.minecraft.MinecraftResourcePackStructure.pathOf(texture),
                    texture.data()
            );

            Metadata metadata = texture.meta();
            if (!metadata.parts().isEmpty()) {
                writeToJson(target, MetadataSerializer.INSTANCE, metadata, basePath + team.unnamed.creative.serialize.minecraft.MinecraftResourcePackStructure.pathOfMeta(texture), localTargetPackFormat);
            }
        }

        // write unknown files
        for (Map.Entry<String, Writable> entry : container.unknownFiles().entrySet()) {
            try {
                target.write(basePath + entry.getKey(), entry.getValue());
            } catch (Exception e) {
                if (Settings.DEBUG.toBool()) e.printStackTrace();
            }
        }
    }

    @Override
    public void write(final @NotNull FileTreeWriter target, final @NotNull ResourcePack resourcePack) {
        // write icon
        {
            Writable icon = resourcePack.icon();
            if (icon != null) {
                target.write(PACK_ICON_FILE, icon);
            }
        }

        // write metadata
        {
            Metadata metadata = resourcePack.metadata();
            PackMeta packMeta = metadata.meta(PackMeta.class);
            // todo: find a better way to log warnings
            if (packMeta == null) {
                System.err.println("Resource pack does not contain PackMeta, won't be recognized by Minecraft");
            } /*else if (targetPackFormat != -1 && !packMeta.formats().isInRange(targetPackFormat)) {
                System.err.println("Resource pack format mismatch, the resource pack specifies formats "
                        + packMeta.formats() + " but the target format specified to the writer is " + targetPackFormat);
            }*/
            writeToJson(target, MetadataSerializer.INSTANCE, metadata, PACK_METADATA_FILE, targetPackFormat);
        }

        writeWithBasePathAndTargetPackFormat(target, resourcePack, "", targetPackFormat);

        // write from overlays
        Map<String, PackFormat> overlayFormats = new HashMap<>();
        {
            OverlaysMeta overlaysMeta = resourcePack.metadata().meta(OverlaysMeta.class);
            if (overlaysMeta != null) {
                for (OverlayEntry entry : overlaysMeta.entries()) {
                    overlayFormats.put(entry.directory(), entry.formats());
                }
            }
        }

        for (Overlay overlay : resourcePack.overlays()) {
            String dir = overlay.directory();
            PackFormat packFormat = overlayFormats.get(dir);
            int overlayTargetPackFormat = packFormat == null ? -1 : packFormat.min(); // todo: consider max pack format
            writeWithBasePathAndTargetPackFormat(target, overlay, dir + '/', overlayTargetPackFormat);
        }
    }

    private <T> void writeToJson(FileTreeWriter writer, JsonResourceSerializer<T> serializer, T object, String path, final int localTargetPackFormat) {
        try (JsonWriter jsonWriter = new JsonWriter(writer.openWriter(path))) {
            if (prettyPrinting) {
                jsonWriter.setIndent("  ");
            }
            serializer.serializeToJson(object, jsonWriter, localTargetPackFormat);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write to " + path, e);
        }
    }
}
