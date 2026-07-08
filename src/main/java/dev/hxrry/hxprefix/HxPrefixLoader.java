package dev.hxrry.hxprefix;

import io.papermc.paper.plugin.loader.PluginClasspathBuilder;
import io.papermc.paper.plugin.loader.PluginLoader;
import io.papermc.paper.plugin.loader.library.impl.MavenLibraryResolver;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.repository.RemoteRepository;
import org.jetbrains.annotations.NotNull;

public class HxPrefixLoader implements PluginLoader {

    @Override
    public void classloader(@NotNull PluginClasspathBuilder builder) {
        MavenLibraryResolver resolver = new MavenLibraryResolver();

        // papers req. mirror for maven central
        resolver.addRepository(new RemoteRepository.Builder(
            "central", "default", MavenLibraryResolver.MAVEN_CENTRAL_DEFAULT_MIRROR).build());

        for (String lib : new String[] {
            "org.xerial:sqlite-jdbc:3.46.0.0",
            "org.postgresql:postgresql:42.7.3",
            "com.zaxxer:HikariCP:5.1.0",
            "com.github.ben-manes.caffeine:caffeine:3.1.8",
            "com.mysql:mysql-connector-j:8.3.0"
        }) {
            resolver.addDependency(new Dependency(new DefaultArtifact(lib), null));
        }

        builder.addLibrary(resolver);
    }
}