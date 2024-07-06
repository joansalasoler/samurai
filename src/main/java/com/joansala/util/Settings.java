package com.joansala.util;

/*
 * Copyright (C) 2024 Joan Sala Soler <contact@joansala.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.ProtectionDomain;
import java.util.Properties;
import java.util.logging.Logger;
import picocli.CommandLine.IDefaultValueProvider;
import picocli.CommandLine.PropertiesDefaultProvider;


/**
 * Utility class for loading and accessing settings.
 */
public class Settings {

    /** Module logger instance */
    protected static Logger logger = Logger.getLogger("com.joansala");

    /** Properties instance to hold loaded configuration properties */
    protected static Properties properties = new Properties();


    /**
     * Private constructor to prevent instantiation.
     */
    private Settings() {}


    /**
     * Retrieves the value of the specified property.
     *
     * @param key the property key to retrieve
     * @return the value of the property, or {@code null}
     */
    public static String get(String key) {
        return properties.getProperty(key);
    }


    /**
     * Retrieves the name of the engine from the configuration.
     *
     * @return name of the engine or {@code null}
     */
    public static String getEngineName() {
        return get("engine.name");
    }


    /**
     * Retrieves the version of the engine from the configuration.
     *
     * @return version of the engine or {@code null}
     */
    public static String getEngineVersion() {
        return get("engine.version");
    }


    /**
     * Retrieves the author of the engine from the configuration.
     *
     * @return author of the engine or {@code null}
     */
    public static String getEngineAuthor() {
        return get("engine.author");
    }


    /**
     * Obtain the default value provider for the command line.
     */
    public static IDefaultValueProvider getDefaultsProvider() {
        return new PropertiesDefaultProvider(properties);
    }


    /**
     * Obtain a file for the given resource path.
     */
    public static File getFile(String path) throws IOException {
        try {
            Path base = Paths.get(getEnginePath().toUri());
            return base.resolve(path).toRealPath().toFile();
        } catch (Exception e) {
            // Ignore
        }

        return Paths.get(path).toRealPath().toFile();
    }


    /**
     * Check if the application is running under native-image mode.
     *
     * @return          True if running under native-image mode
     */
    public static boolean isNativeImage() {
        try {
            String className = "org.graalvm.nativeimage.ImageInfo";
            Class<?> c = Class.forName(className);
            return (boolean) c.getMethod("isExecutable").invoke(null);
        } catch (Exception e) {
            return false;
        }
    }


    /**
     * Load settings from the given properties file.
     *
     * @param name Properties file path
     */
    public static void load(String path) {
        try (InputStream input = new FileInputStream(getFile(path))) {
            properties.load(input);
        } catch (Exception e) {
            logger.warning("Cannot load settings: " + path);
        }
    }


    /**
     * Path where the engine is located.
     *
     * @return          Path of the engine executable or jar file
     */
    public static Path getEnginePath() throws Exception {
        return isNativeImage() ? getExeFolder() : getJarFolder();
    }


    /**
     * Path where the engine jar file is located.
     *
     * @return          Path of the engine jar file
     */
    private static Path getJarFolder() throws Exception {
        ProtectionDomain domain = Settings.class.getProtectionDomain();
        URI uri = domain.getCodeSource().getLocation().toURI();
        return Paths.get(uri).toRealPath().getParent();
    }


    /**
     * Path where the engine executable is located.
     *
     * @return          Path of the engine executable
     */
    private static Path getExeFolder() throws Exception {
        String command = System.getenv("_");
        return Paths.get(command).toRealPath().getParent();
    }
}