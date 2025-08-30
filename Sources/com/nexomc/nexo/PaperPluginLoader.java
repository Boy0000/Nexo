package com.nexomc.nexo;

import io.papermc.paper.plugin.loader.PluginClasspathBuilder;
import io.papermc.paper.plugin.loader.PluginLoader;
import io.papermc.paper.plugin.loader.library.impl.JarLibrary;
import io.papermc.paper.plugin.loader.library.impl.MavenLibraryResolver;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.RepositoryPolicy;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.eclipse.aether.repository.RepositoryPolicy.CHECKSUM_POLICY_IGNORE;
import static org.eclipse.aether.repository.RepositoryPolicy.UPDATE_POLICY_ALWAYS;

public class PaperPluginLoader implements PluginLoader {

    public static boolean usedPaperPluginLoader = false;
    private static final List<String> libraries = new ArrayList<>();

    private static final String COMMAND_API_VERSION = "10.1.2";

    private static final List<Repo> repositories = List.of(
        new Repo("central", getDefaultMavenCentralMirror()),
        new Repo("nexomc-release", "https://repo.nexomc.com/releases"),
        new Repo("nexomc-snapshot", "https://repo.nexomc.com/snapshots"),
        new Repo("paper", "https://repo.papermc.io/repository/maven-public/")
    );

    private static String getDefaultMavenCentralMirror() {
        String central = System.getenv("PAPER_DEFAULT_CENTRAL_REPOSITORY");
        if (central == null) {
            central = System.getProperty("org.bukkit.plugin.java.LibraryLoader.centralURL");
        }

        if (central == null) {
            central = "https://maven-central.storage-download.googleapis.com/maven2";
        }

        return central;
    }

    static {
        libraries.add("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2");
        libraries.add("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.10.2");
        libraries.add("com.github.shynixn.mccoroutine:mccoroutine-folia-api:2.22.0");
        libraries.add("com.github.shynixn.mccoroutine:mccoroutine-folia-core:2.22.0");
        libraries.add("org.jetbrains.kotlin:kotlin-stdlib:2.2.0");

        try {
            Class.forName("org.bukkit.craftbukkit.v1_20_R3.entity.CraftPlayer");
            libraries.add("dev.jorel:commandapi-bukkit-shade:" + COMMAND_API_VERSION);
        } catch (Exception e) {
            libraries.add("dev.jorel:commandapi-bukkit-shade-mojang-mapped:" + COMMAND_API_VERSION);
        }

        libraries.add("dev.jorel:commandapi-bukkit-kotlin:" + COMMAND_API_VERSION);
        libraries.add("org.springframework:spring-expression:6.0.8");
        libraries.add("org.spongepowered:configurate-yaml:4.2.0");
        libraries.add("org.spongepowered:configurate-extra-kotlin:4.2.0");
        libraries.add("gs.mclo:java:2.2.1");
        libraries.add("commons-io:commons-io:2.18.0");
        libraries.add("com.google.code.gson:gson:2.11.0");
        libraries.add("org.apache.commons:commons-lang3:3.17.0");
        libraries.add("org.apache.httpcomponents.client5:httpclient5:5.4.3");
        libraries.add("org.glassfish:javax.json:1.1.4");
        libraries.add("software.amazon.awssdk:s3:2.32.4");
    }


    @Override
    public void classloader(PluginClasspathBuilder classpathBuilder) {
        File nexoLibs = Arrays.stream(classpathBuilder.getContext().getPluginSource().getParent().toFile().listFiles())
                .filter(f -> f.getName().matches("NexoLibs-.*.lib")).findFirst().orElse(null);

        if (nexoLibs != null) try {
            classpathBuilder.addLibrary(new JarLibrary(nexoLibs.toPath()));
            usedPaperPluginLoader = true;
            return;
        } catch (Exception e) {
            e.printStackTrace();
        }

        MavenLibraryResolver resolver = new MavenLibraryResolver();
        RepositoryPolicy snapshot = new RepositoryPolicy(true, UPDATE_POLICY_ALWAYS, CHECKSUM_POLICY_IGNORE);
        for (Repo repo : repositories) resolver.addRepository(
                new RemoteRepository.Builder(repo.name(), "default", repo.link())
                        .setSnapshotPolicy(snapshot).build()
        );

        for (String lib : libraries) resolver.addDependency(new Dependency(new DefaultArtifact(lib), null));

        classpathBuilder.addLibrary(resolver);

        usedPaperPluginLoader = true;
    }

    private record Repo(String name, String link) {
    }
}
