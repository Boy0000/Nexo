package com.nexomc.nexo;

import net.byteflux.libby.BukkitLibraryManager;
import net.byteflux.libby.classloader.URLClassLoaderHelper;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Optional;

public class NexoLibsLoader {
    public static boolean usedNexoLoader = false;
    public static void loadNexoLibs(JavaPlugin plugin) {
        BukkitLibraryManager manager = new BukkitLibraryManager(plugin, "libs");
        File[] pluginFiles = Optional.ofNullable(plugin.getDataFolder().getParentFile().listFiles()).orElse(new File[]{});
        File nexoLibs = Arrays.stream(pluginFiles).filter(f -> f.getName().matches("NexoLibs-.*.lib")).findFirst().orElse(null);
        if (nexoLibs == null) return;

        URLClassLoaderHelper classLoader;
        try {
            Field classLoaderField = BukkitLibraryManager.class.getDeclaredField("classLoader");
            classLoaderField.setAccessible(true);
            classLoader = (URLClassLoaderHelper) classLoaderField.get(manager);
            classLoader.addToClasspath(nexoLibs.toPath());

            plugin.getComponentLogger().info(Component.text("[Nexo] Loaded NexoLibs, skipping downloading libraries!", NamedTextColor.GREEN));
            usedNexoLoader = true;
        } catch (Exception e) {
            plugin.getComponentLogger().error("[Nexo] Failed to load NexoLibs...");
        }
    }
}
