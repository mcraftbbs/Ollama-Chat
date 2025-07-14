package com.ollamachat;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class DependencyLoader {
    private final JavaPlugin plugin;
    private final Logger logger;
    private final File libDir;
    private URLClassLoader dependencyClassLoader;

    public DependencyLoader(JavaPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.libDir = new File(plugin.getDataFolder(), "libs");
    }

    public boolean loadDependencies() {
        try {
            // Create libs directory if it doesn't exist
            if (!libDir.exists()) {
                libDir.mkdirs();
            }

            // Define dependencies to download
            List<Dependency> dependencies = new ArrayList<>();
            dependencies.add(new Dependency("com.mysql", "mysql-connector-j", "8.0.33"));
            dependencies.add(new Dependency("org.xerial", "sqlite-jdbc", "3.46.0.0"));

            // Download each dependency
            List<URL> jarUrls = new ArrayList<>();
            for (Dependency dep : dependencies) {
                File jarFile = downloadDependency(dep);
                if (jarFile != null) {
                    jarUrls.add(jarFile.toURI().toURL());
                    logger.info("Found dependency: " + jarFile.getName());
                } else {
                    logger.warning("Failed to download dependency: " + dep.artifactId + "-" + dep.version);
                }
            }

            if (jarUrls.isEmpty()) {
                logger.warning("No dependencies were successfully downloaded.");
                return false;
            }

            // Create a new classloader with the dependencies
            dependencyClassLoader = new URLClassLoader(
                    jarUrls.toArray(new URL[0]),
                    plugin.getClass().getClassLoader()
            );

            // Set this classloader as the thread context classloader
            Thread.currentThread().setContextClassLoader(dependencyClassLoader);
            logger.info("Successfully loaded " + jarUrls.size() + " dependencies");

            return true;
        } catch (Exception e) {
            logger.severe("Failed to load dependencies: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public ClassLoader getDependencyClassLoader() {
        return dependencyClassLoader;
    }

    private File downloadDependency(Dependency dep) {
        String fileName = dep.artifactId + "-" + dep.version + ".jar";
        File file = new File(libDir, fileName);

        if (file.exists()) {
            return file;
        }

        String mavenPath = String.format("%s/%s/%s/%s-%s.jar",
                dep.groupId.replace(".", "/"), dep.artifactId, dep.version, dep.artifactId, dep.version);
        String url = "https://repo1.maven.org/maven2/" + mavenPath;

        try (InputStream in = new URL(url).openStream()) {
            Files.copy(in, file.toPath());
            logger.info("Downloaded dependency: " + fileName);
            return file;
        } catch (IOException e) {
            logger.severe("Failed to download dependency " + fileName + ": " + e.getMessage());
            return null;
        }
    }

    private static class Dependency {
        String groupId;
        String artifactId;
        String version;

        Dependency(String groupId, String artifactId, String version) {
            this.groupId = groupId;
            this.artifactId = artifactId;
            this.version = version;
        }
    }
}




