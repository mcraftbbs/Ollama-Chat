package com.ollamachat;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.*;
import java.util.*;
import java.util.logging.Logger;

public class DependencyLoader {
    private final JavaPlugin plugin;
    private final Logger logger;
    private final File libDir;

    public DependencyLoader(JavaPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.libDir = new File(plugin.getDataFolder(), "libs");
    }

    public boolean loadDependencies() {
        try {
            if (!libDir.exists() && !libDir.mkdirs()) {
                logger.severe("Failed to create libs directory: " + libDir.getAbsolutePath());
                return false;
            }

            // 定义依赖
            List<Dependency> dependencies = List.of(
                    new Dependency("com.mysql", "mysql-connector-j", "8.0.33"),
                    new Dependency("org.xerial", "sqlite-jdbc", "3.46.0.0"),
                    new Dependency("com.zaxxer", "HikariCP", "5.1.0")
            );

            int loadedCount = 0;

            for (Dependency dep : dependencies) {
                File jarFile = downloadDependency(dep);
                if (jarFile != null && injectToPluginClassLoader(jarFile)) {
                    loadedCount++;
                    logger.info("Loaded dependency: " + jarFile.getName());
                } else {
                    logger.warning("Failed to load dependency: " + dep.artifactId);
                }
            }

            logger.info("Successfully loaded " + loadedCount + " dependencies.");
            return true;
        } catch (Exception e) {
            logger.severe("Failed to load dependencies: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private File downloadDependency(Dependency dep) {
        String fileName = dep.artifactId + "-" + dep.version + ".jar";
        File file = new File(libDir, fileName);

        // 已存在则直接返回
        if (file.exists()) {
            return file;
        }

        String mavenPath = String.format("%s/%s/%s/%s-%s.jar",
                dep.groupId.replace(".", "/"), dep.artifactId, dep.version, dep.artifactId, dep.version);
        String url = "https://repo1.maven.org/maven2/" + mavenPath;

        logger.info("Downloading dependency: " + fileName + " from " + url);

        try (InputStream in = new URL(url).openStream()) {
            Path temp = Files.createTempFile(libDir.toPath(), dep.artifactId, ".tmp");
            Files.copy(in, temp, StandardCopyOption.REPLACE_EXISTING);
            Files.move(temp, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            logger.info("Downloaded dependency: " + fileName);
            return file;
        } catch (IOException e) {
            logger.severe("Failed to download " + fileName + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * 将 JAR 动态注入到 Bukkit 的 PluginClassLoader。
     */
    private boolean injectToPluginClassLoader(File jarFile) {
        try {
            ClassLoader pluginClassLoader = plugin.getClass().getClassLoader();

            // 仅在类加载器是 URLClassLoader 或包含 addURL 方法时注入
            Method addURLMethod = null;
            Class<?> current = pluginClassLoader.getClass();
            while (current != null && addURLMethod == null) {
                try {
                    addURLMethod = current.getDeclaredMethod("addURL", URL.class);
                } catch (NoSuchMethodException ignored) {
                }
                current = current.getSuperclass();
            }

            if (addURLMethod == null) {
                logger.severe("Unable to find addURL method in plugin classloader. Injection failed.");
                return false;
            }

            addURLMethod.setAccessible(true);
            addURLMethod.invoke(pluginClassLoader, jarFile.toURI().toURL());
            logger.info("Injected dependency into classloader: " + jarFile.getName());
            return true;
        } catch (Exception e) {
            logger.severe("Failed to inject dependency " + jarFile.getName() + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * 内部依赖描述类
     */
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
