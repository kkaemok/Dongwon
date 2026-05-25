package org.kkaemok.dongwon.text;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.List;

public final class MessageConfig {
    private final JavaPlugin plugin;
    private final String fileName;
    private final File file;
    private FileConfiguration configuration;

    public MessageConfig(JavaPlugin plugin, String fileName) {
        this.plugin = plugin;
        this.fileName = fileName;
        if (!plugin.getDataFolder().exists()) {
            //noinspection ResultOfMethodCallIgnored
            plugin.getDataFolder().mkdirs();
        }
        this.file = new File(plugin.getDataFolder(), fileName);
        YamlDefaultsUpdater.ensureDefaults(plugin, fileName);
        reload();
    }

    public void reload() {
        YamlDefaultsUpdater.ensureDefaults(plugin, fileName);
        configuration = YamlConfiguration.loadConfiguration(file);
    }

    public FileConfiguration raw() {
        return configuration;
    }

    public int getInt(String path, int fallback) {
        return configuration.getInt(path, fallback);
    }

    public long getLong(String path, long fallback) {
        return configuration.getLong(path, fallback);
    }

    public double getDouble(String path, double fallback) {
        return configuration.getDouble(path, fallback);
    }

    public boolean getBoolean(String path, boolean fallback) {
        return configuration.getBoolean(path, fallback);
    }

    public String getString(String path, String fallback) {
        String value = configuration.getString(path);
        return value == null ? fallback : value;
    }

    public List<String> getStringList(String path) {
        return List.copyOf(configuration.getStringList(path));
    }

}
