package com.nexomc.nexo;

import net.byteflux.libby.BukkitLibraryManager;
import net.byteflux.libby.classloader.URLClassLoaderHelper;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;

public class NexoLibsLoader {
    public static boolean loadNexoLibs(JavaPlugin plugin) {
        BukkitLibraryManager manager = new BukkitLibraryManager(plugin, "libs");
        File[] pluginFiles = Optional.ofNullable(Bukkit.getPluginsFolder().listFiles()).orElse(new File[]{});
        File nexoLibs = Arrays.stream(pluginFiles).filter(f -> f.getName().matches("NexoLibs-.*.lib")).findFirst().orElse(null);
        if (nexoLibs == null) return false;

        URLClassLoaderHelper classLoader;
        try {
            Field classLoaderField = BukkitLibraryManager.class.getDeclaredField("classLoader");
            classLoaderField.setAccessible(true);
            classLoader = (URLClassLoaderHelper) classLoaderField.get(manager);
            classLoader.addToClasspath(nexoLibs.toPath());

            Bukkit.getConsoleSender().sendMessage(ChatColor.GREEN + "[Nexo] Loaded NexoLibs, skipping downloading libraries!");
            return true;
        } catch (Exception e) {
            Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "[Nexo] Failed to load NexoLibs...");
            return false;
        }
    }
}
