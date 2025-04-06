package com.nexomc.nexo.pack.creative;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;
import com.nexomc.nexo.configs.Settings;
import com.nexomc.nexo.utils.KeyUtils;
import com.nexomc.nexo.utils.logs.Logs;
import net.kyori.adventure.key.Key;
import org.intellij.lang.annotations.Subst;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import team.unnamed.creative.ResourcePack;
import team.unnamed.creative.base.Writable;
import team.unnamed.creative.metadata.Metadata;
import team.unnamed.creative.metadata.overlays.OverlayEntry;
import team.unnamed.creative.metadata.overlays.OverlaysMeta;
import team.unnamed.creative.metadata.pack.PackMeta;
import team.unnamed.creative.overlay.Overlay;
import team.unnamed.creative.overlay.ResourceContainer;
import team.unnamed.creative.part.ResourcePackPart;
import team.unnamed.creative.serialize.minecraft.GsonUtil;
import team.unnamed.creative.serialize.minecraft.MinecraftResourcePackReader;
import team.unnamed.creative.serialize.minecraft.ResourceCategories;
import team.unnamed.creative.serialize.minecraft.ResourceCategory;
import team.unnamed.creative.serialize.minecraft.fs.FileTreeReader;
import team.unnamed.creative.serialize.minecraft.io.BinaryResourceDeserializer;
import team.unnamed.creative.serialize.minecraft.io.JsonResourceDeserializer;
import team.unnamed.creative.serialize.minecraft.io.ResourceDeserializer;
import team.unnamed.creative.serialize.minecraft.metadata.MetadataSerializer;
import team.unnamed.creative.serialize.minecraft.sound.SoundRegistrySerializer;
import team.unnamed.creative.texture.Texture;
import team.unnamed.creative.util.Keys;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Queue;

import static com.nexomc.nexo.pack.creative.MinecraftResourcePackStructure.*;
import static java.util.Objects.requireNonNull;
import static team.unnamed.creative.serialize.minecraft.MinecraftResourcePackStructure.ASSETS_FOLDER;
import static team.unnamed.creative.serialize.minecraft.MinecraftResourcePackStructure.METADATA_EXTENSION;
import static team.unnamed.creative.serialize.minecraft.MinecraftResourcePackStructure.OVERLAYS_FOLDER;
import static team.unnamed.creative.serialize.minecraft.MinecraftResourcePackStructure.PACK_ICON_FILE;
import static team.unnamed.creative.serialize.minecraft.MinecraftResourcePackStructure.PACK_METADATA_FILE;
import static team.unnamed.creative.serialize.minecraft.MinecraftResourcePackStructure.SOUNDS_FILE;
import static team.unnamed.creative.serialize.minecraft.MinecraftResourcePackStructure.TEXTURES_FOLDER;
import static team.unnamed.creative.serialize.minecraft.MinecraftResourcePackStructure.path;
import static team.unnamed.creative.serialize.minecraft.MinecraftResourcePackStructure.tokenize;

public class NexoPackReader implements MinecraftResourcePackReader {

    public static NexoPackReader INSTANCE = new NexoPackReader();

    public static void resetReader() {
        INSTANCE = new NexoPackReader();
    }

    private static final boolean lenient = Settings.PACK_READER_LENIENT.toBool();
    private static final boolean debug = Settings.DEBUG.toBool();

    private NexoPackReader() {
    }

    public @NotNull ResourcePack readFile(@NotNull final File resourcePackFile) {
        ResourcePack resourcePack = ResourcePack.resourcePack();
        try {
            if (resourcePackFile.isDirectory()) resourcePack = readFromDirectory(resourcePackFile);
            else if (resourcePackFile.getName().endsWith(".zip")) resourcePack = readFromZipFile(resourcePackFile);
        } catch (Exception e) {
            Logs.logError(String.format("Failed to read %s resourcePack...", resourcePackFile.getName()));
            e.printStackTrace();
        }

        return resourcePack;
    }

    @Override
    @SuppressWarnings("PatternValidation")
    public @NotNull ResourcePack read(final @NotNull FileTreeReader reader) {
        ResourcePack resourcePack = ResourcePack.resourcePack();

        // textures that are waiting for metadata, or metadata
        // waiting for textures (because we can't know the order
        // they come in)
        // (null key means it is root resource pack)
        Map<@Nullable String, Map<Key, Texture>> incompleteTextures = new LinkedHashMap<>();

        // fill in with the default ones first (pack format is unknown at the start)
        Map<String, ResourceCategory<?>> categoriesByFolderThisPackFormat = ResourceCategories.buildCategoryMapByFolder(-1);
        Map<String, Integer> packFormatsByOverlayDir = new HashMap<>();
        int packFormat = -1;

        while (reader.hasNext()) {
            String path = reader.next();
            try {

                // tokenize path in sections, e.g.: [ assets, minecraft, textures, ... ]
                Queue<String> tokens = tokenize(path);

                if (tokens.isEmpty()) {
                    // this should never happen
                    throw new IllegalStateException("Token collection is empty!");
                }

                // single token means the file is on the
                // root level (top level files) so it may be:
                // - pack.mcmeta
                // - pack.png
                if (tokens.size() == 1) {
                    switch (tokens.poll()) {
                        case PACK_ZIP: continue;
                        case PACK_METADATA_FILE: {
                            try {
                                // found pack.mcmeta file, deserialize and add
                                Metadata metadata = MetadataSerializer.INSTANCE.readFromTree(parseJson(reader.stream()));
                                resourcePack.metadata(metadata);

                                // get the pack format from the metadata
                                PackMeta packMeta = metadata.meta(PackMeta.class);
                                if (packMeta == null) {
                                    // TODO: better warning system
                                    System.err.println("Reading a resource-pack with no pack meta in its pack.mcmeta file! Unknown pack format version :(");
                                } else {
                                    // update the pack format and categories
                                    packFormat = packMeta.formats().min();
                                    categoriesByFolderThisPackFormat = ResourceCategories.buildCategoryMapByFolder(packFormat);
                                }

                                // overlays info
                                OverlaysMeta overlaysMeta = metadata.meta(OverlaysMeta.class);
                                if (overlaysMeta != null) for (OverlayEntry entry : overlaysMeta.entries()) {
                                    packFormatsByOverlayDir.put(entry.directory(), entry.formats().min());
                                }
                            } catch (Exception e) {
                                if (lenient) {
                                    Logs.logWarn(String.format("Failed to parse %s resourcePack...", path));
                                    if (debug) e.printStackTrace();
                                }
                                else throw e;
                            }
                            continue;
                        }
                        case PACK_ICON_FILE: {
                            // found pack.png file, add
                            resourcePack.icon(reader.content().asWritable());
                            continue;
                        }
                        default: {
                            // unknown top level file
                            resourcePack.unknownFile(path, reader.content().asWritable());
                            continue;
                        }
                    }
                }

                // the container to use, it is initially the default resource-pack,
                // but it may change if the file is inside an overlay folder
                @Subst("dir")
                @Nullable String overlayDir = null;
                int localPackFormat = packFormat;

                // the file path, relative to the container
                String containerPath = path;
                ResourceContainer container = resourcePack;

                // if there are two or more tokens, it means the
                // file is inside a folder, in a Minecraft resource
                // pack, the first folder is always "assets"
                String folder = tokens.poll();

                if (folder.equals(OVERLAYS_FOLDER)) {
                    // gets the overlay name, set after the
                    // "overlays" folder, e.g. "overlays/foo",
                    // or "overlays/bar"
                    overlayDir = tokens.poll();
                    if (tokens.isEmpty()) {
                        // this means that there is a file directly
                        // inside the "overlays" folder, this is illegal
                        resourcePack.unknownFile(containerPath, reader.content().asWritable());
                        continue;
                    }

                    Overlay overlay = resourcePack.overlay(overlayDir);
                    if (overlay == null) {
                        // first occurrence, register overlay
                        overlay = Overlay.overlay(overlayDir);
                        resourcePack.overlay(overlay);
                    }

                    container = overlay;
                    folder = tokens.poll();
                    containerPath = path.substring((OVERLAYS_FOLDER + '/' + overlayDir + '/').length());
                    localPackFormat = packFormatsByOverlayDir.getOrDefault(overlayDir, -1);
                }

                // Skip nexo-specific folders
                if (folder != null && IGNORED_NEXO_FOLDERS.contains(folder)) continue;

                // null check to make ide happy
                if (folder == null || !folder.equals(ASSETS_FOLDER) || tokens.isEmpty()) {
                    // not assets! this is an unknown file
                    container.unknownFile(containerPath, reader.content().asWritable());
                    continue;
                }

                // inside "assets", we should always have a folder
                // with any name, which is a namespace, e.g. "minecraft"
                String namespace = tokens.poll();

                if (!Keys.isValidNamespace(namespace)) {
                    // invalid namespace found
                    container.unknownFile(containerPath, reader.content().asWritable());
                    continue;
                }

                if (tokens.isEmpty()) {
                    // found a file directly inside "assets", like
                    // assets/<file>, it is not allowed
                    container.unknownFile(containerPath, reader.content().asWritable());
                    continue;
                }

                // so we already have "assets/<namespace>/", most files inside
                // the namespace folder always have a "category", e.g. textures,
                // lang, font, etc. But not always! There is sounds.json file and
                // gpu_warnlist.json file
                String categoryName = tokens.poll();

                if (tokens.isEmpty()) {
                    // this means "category" is a file
                    // (remember: last tokens are always files)
                    if (categoryName.equals(SOUNDS_FILE)) {
                        // found a sound registry!
                        container.soundRegistry(SoundRegistrySerializer.INSTANCE.readFromTree(
                                parseJson(reader.stream()),
                                namespace
                        ));
                    } else {
                        // TODO: gpu_warnlist.json?
                        container.unknownFile(containerPath, reader.content().asWritable());
                    }
                    continue;
                }

                // so "category" is actually a category like "textures",
                // "lang", "font", etc. next we can compute the relative
                // path inside the category
                String categoryPath = path(tokens);

                // Filter out unwanted & common extensions
                if (categoryPath.endsWith(DS_STORE_EXTENSION) || categoryPath.endsWith(DB_EXTENSION) || categoryPath.endsWith(TXT_EXTENSION)) {
                    container.unknownFile(containerPath, reader.content().asWritable());
                    continue;
                }

                if (categoryName.equals(TEXTURES_FOLDER)) {
                    try {
                        String keyOfMetadata = withoutExtension(categoryPath, METADATA_EXTENSION);
                        if (keyOfMetadata != null) {
                            // found metadata for texture
                            Key key = Key.key(namespace, keyOfMetadata);
                            Metadata metadata = MetadataSerializer.INSTANCE.readFromTree(parseJson(reader.stream()));

                            Map<Key, Texture> incompleteTexturesThisContainer = incompleteTextures.computeIfAbsent(overlayDir, k -> new LinkedHashMap<>());
                            Texture texture = incompleteTexturesThisContainer.remove(key);
                            if (texture == null) {
                                // metadata was found first, put
                                incompleteTexturesThisContainer.put(key, Texture.texture(key, Writable.EMPTY, metadata));
                            } else {
                                // texture was found before the metadata, nice!
                                container.texture(texture.meta(metadata));
                            }
                        } else {
                            Key key = Key.key(namespace, categoryPath);
                            Writable data = reader.content().asWritable();
                            Map<Key, Texture> incompleteTexturesThisContainer = incompleteTextures.computeIfAbsent(overlayDir, k -> new LinkedHashMap<>());
                            Texture waiting = incompleteTexturesThisContainer.remove(key);

                            if (waiting == null) {
                                // found texture before metadata
                                incompleteTexturesThisContainer.put(key, Texture.texture(key, data));
                            } else {
                                // metadata was found first
                                container.texture(Texture.texture(
                                        key,
                                        data,
                                        waiting.meta()
                                ));
                            }
                        }
                    } catch (Exception e) {
                        if (lenient) {
                            Logs.logWarn(String.format("Failed to parse %s resourcePack...", path));
                            if (debug) e.printStackTrace();
                        }
                        else throw e;
                    }
                } else {
                    try {
                        // get the resource category, if the local pack format (overlay or root) is the same as the
                        // root pack format, we can use the previously computed map, otherwise we need to compute it
                        // (we could save some time by caching the computed map, but, is it worth it?)
                        ResourceCategory<?> category = (localPackFormat == packFormat
                                ? categoriesByFolderThisPackFormat
                                : ResourceCategories.buildCategoryMapByFolder(localPackFormat)).get(categoryName);
                        if (category == null) {
                            // unknown category
                            container.unknownFile(containerPath, reader.content().asWritable());
                            continue;
                        }
                        String keyValue = withoutExtension(categoryPath, category.extension(-1));
                        if (keyValue == null) {
                            // wrong extension
                            container.unknownFile(containerPath, reader.content().asWritable());
                            continue;
                        }

                        Key key = Key.key(namespace, keyValue);

                        ResourceDeserializer<? extends ResourcePackPart> deserializer = category.deserializer();
                        ResourcePackPart resource;
                        if (deserializer instanceof BinaryResourceDeserializer) {
                            resource = ((BinaryResourceDeserializer<? extends ResourcePackPart>) deserializer)
                                    .deserializeBinary(reader.content().asWritable(), key);
                        } else if (deserializer instanceof JsonResourceDeserializer) {
                            resource = ((JsonResourceDeserializer<? extends ResourcePackPart>) deserializer)
                                    .deserializeFromJson(parseJson(reader.stream()), key);
                        } else {
                            resource = deserializer.deserialize(reader.stream(), key);
                        }
                        resource.addTo(container);
                    } catch (IOException e) {
                        if (lenient) {
                            Logs.logWarn("Failed to deserialize resource at: '" + path + "'");
                            if (debug) e.printStackTrace();
                        }
                        else throw new UncheckedIOException("Failed to deserialize resource at: '" + path + "'", e);
                    } catch (Exception e) {
                        if (lenient) {
                            Logs.logWarn("Failed to deserialize resource at: '" + path + "'");
                            if (debug) e.printStackTrace();
                        }
                        else throw e;
                    }
                }
            } catch (Exception e) {
                if (lenient) {
                    Logs.logWarn("Failed to deserialize resource at: '" + path + "'");
                    if (debug) e.printStackTrace();
                } else throw e;
            }
        }

        for (Map.Entry<String, Map<Key, Texture>> entry : incompleteTextures.entrySet()) {
            @Subst("dir")
            @Nullable String overlayDir = entry.getKey();
            Map<Key, Texture> incompleteTexturesThisContainer = entry.getValue();
            ResourceContainer container;

            if (overlayDir == null) {
                // root
                container = resourcePack;
            } else {
                // from an overlay
                container = resourcePack.overlay(overlayDir);
                requireNonNull(container, "container"); // should never happen, but make ide happy
            }

            for (Texture texture : incompleteTexturesThisContainer.values()) {
                if (texture.data() != Writable.EMPTY) {
                    container.texture(texture);
                }
            }
        }
        return resourcePack;
    }

    private static @Nullable String withoutExtension(String string, String extension) {
        if (string.endsWith(extension)) {
            return string.substring(0, string.length() - extension.length());
        } else {
            // string doesn't end with extension
            return null;
        }
    }

    private @NotNull JsonElement parseJson(final @NotNull InputStream input) {
        try (final JsonReader jsonReader = new JsonReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            jsonReader.setLenient(true);
            return GsonUtil.parseReader(jsonReader);
        } catch (final IOException e) {
            if (lenient) {
                Logs.logWarn("Failed to parse json: " + e.getMessage());
                if (debug) e.printStackTrace();
                return new JsonObject();
            }
            else throw new UncheckedIOException("Failed to close JSON reader", e);
        }
    }
}
