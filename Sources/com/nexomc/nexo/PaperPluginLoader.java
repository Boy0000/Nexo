package com.nexomc.nexo;

import io.netty.handler.codec.compression.Snappy;
import io.papermc.paper.plugin.loader.PluginClasspathBuilder;
import io.papermc.paper.plugin.loader.PluginLoader;
import io.papermc.paper.plugin.loader.library.impl.JarLibrary;
import io.papermc.paper.plugin.loader.library.impl.MavenLibraryResolver;
import net.byteflux.libby.Library;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.RepositoryPolicy;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.eclipse.aether.repository.RepositoryPolicy.*;

public class PaperPluginLoader implements PluginLoader {

    public static boolean usedPaperPluginLoader = false;

    private static final String COMMAND_API_VERSION = "10.0.1";

    private static final List<Repo> repositories = List.of(
        new Repo("central", "https://repo1.maven.org/maven2/"),
        new Repo("nexomc-release", "https://repo.nexomc.com/releases"),
        new Repo("nexomc-snapshot", "https://repo.nexomc.com/snapshots"),
        new Repo("paper", "https://repo.papermc.io/repository/maven-public/")
    );


    private static final List<String> libraries = List.of(
            "org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1",
            "org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.10.1",
            "org.jetbrains.kotlin:kotlin-stdlib:2.0.21",
            "dev.jorel:commandapi-bukkit-shade-mojang-mapped:" + COMMAND_API_VERSION,
            "dev.jorel:commandapi-bukkit-kotlin:" + COMMAND_API_VERSION,

            "org.springframework:spring-expression:6.0.8",
            "org.spongepowered:configurate-yaml:4.2.0",
            "org.spongepowered:configurate-extra-kotlin:4.2.0",
            "gs.mclo:java:2.2.1",
            "commons-io:commons-io:2.18.0",
            "com.google.code.gson:gson:2.11.0",
            "org.apache.commons:commons-lang3:3.17.0",
            "org.apache.httpcomponents.client5:httpclient5:5.4.3",
            "org.glassfish:javax.json:1.1.4"
    );

    @Override
    public void classloader(PluginClasspathBuilder classpathBuilder) {
        System.out.println("<red>PaperPluginLoader.classloader");
        File nexoLibs = Arrays.stream(classpathBuilder.getContext().getPluginSource().getParent().toFile().listFiles())
                .filter(f -> f.getName().matches("NexoLibs-.*.lib")).findFirst().orElse(null);

        if (nexoLibs != null) try {
            classpathBuilder.addLibrary(new JarLibrary(nexoLibs.toPath()));
            usedPaperPluginLoader = true;
            System.out.println("<red>PaperPluginLoader.classloader");
            return;
        } catch (Exception e) {
            e.printStackTrace();
        }

        MavenLibraryResolver resolver = new MavenLibraryResolver();
        RepositoryPolicy snapshot = new RepositoryPolicy(true, UPDATE_POLICY_ALWAYS, CHECKSUM_POLICY_IGNORE);
        for (Repo repo : repositories) resolver.addRepository(
                new RemoteRepository.Builder(repo.getName(), "default", repo.getLink())
                        .setSnapshotPolicy(snapshot).build()
        );

        for (String lib : libraries) resolver.addDependency(new Dependency(new DefaultArtifact(lib), null));

        classpathBuilder.addLibrary(resolver);

        usedPaperPluginLoader = true;
        System.out.printf("PaperPluginLoader DONE!, %s%n", usedPaperPluginLoader);
    }

    private static class Repo {
        private final String name;
        private final String link;

        private Repo(String name, String link) {
            this.name = name;
            this.link = link;
        }

        public String getName() {
            return name;
        }

        public String getLink() {
            return link;
        }
    }
}
