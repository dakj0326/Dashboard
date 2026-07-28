package org.net.settings;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Properties;
import java.util.Map;

public final class AppSettings {
    private static final Path SETTINGS_DIRECTORY = Path.of(
            System.getProperty("user.home"),
            ".kjellberius-dashboard"
    );
    private static final Path SETTINGS_FILE = SETTINGS_DIRECTORY.resolve("settings.properties");
    private static final AppSettings INSTANCE = new AppSettings();

    private final Properties values = new Properties();

    private AppSettings() {
        load();
        save();
    }

    public static AppSettings getInstance() {
        return INSTANCE;
    }

    public synchronized String get(String key, String defaultValue) {
        String value = values.getProperty(key);
        if (value == null) {
            values.setProperty(key, defaultValue);
            save();
            return defaultValue;
        }
        return value;
    }

    public synchronized boolean getBoolean(String key, boolean defaultValue) {
        String value = values.getProperty(key);
        if (!"true".equalsIgnoreCase(value) && !"false".equalsIgnoreCase(value)) {
            setBoolean(key, defaultValue);
            return defaultValue;
        }
        return Boolean.parseBoolean(value);
    }

    public synchronized void set(String key, String value) {
        values.setProperty(key, value);
        save();
    }

    public synchronized void setBoolean(String key, boolean value) {
        set(key, Boolean.toString(value));
    }

    public synchronized void setAll(Map<String, String> entries) {
        values.putAll(entries);
        save();
    }

    public synchronized void remove(String key) {
        values.remove(key);
        save();
    }

    public Path getSettingsFile() {
        return SETTINGS_FILE;
    }

    private void load() {
        if (!Files.exists(SETTINGS_FILE)) {
            return;
        }

        try (InputStream input = Files.newInputStream(SETTINGS_FILE)) {
            values.load(input);
        } catch (IOException | IllegalArgumentException ignored) {
            // Keep defaults if an existing settings file cannot be read.
        }
    }

    private synchronized void save() {
        Path temporaryFile = SETTINGS_DIRECTORY.resolve("settings.properties.tmp");
        try {
            Files.createDirectories(SETTINGS_DIRECTORY);
            try (OutputStream output = Files.newOutputStream(
                    temporaryFile,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE
            )) {
                values.store(output, "Kjellberius Dashboard settings");
            }
            try {
                Files.move(
                        temporaryFile,
                        SETTINGS_FILE,
                        StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING
                );
            } catch (java.nio.file.AtomicMoveNotSupportedException exception) {
                Files.move(temporaryFile, SETTINGS_FILE, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            System.err.println("Could not save settings to " + SETTINGS_FILE + ": "
                    + exception.getMessage());
        }
    }
}
