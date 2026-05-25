package org.kkaemok.dongwon.text;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class YamlDefaultsUpdater {
    private static final DateTimeFormatter BACKUP_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private YamlDefaultsUpdater() {
    }

    public static int ensureDefaults(JavaPlugin plugin, String fileName) {
        if (!plugin.getDataFolder().exists()) {
            //noinspection ResultOfMethodCallIgnored
            plugin.getDataFolder().mkdirs();
        }

        File file = new File(plugin.getDataFolder(), fileName);
        if (!file.exists()) {
            return createFromResourceOrEmpty(plugin, fileName, file);
        }

        InputStream defaultsStream = plugin.getResource(fileName);
        if (defaultsStream == null) {
            return 0;
        }

        try (InputStreamReader reader = new InputStreamReader(defaultsStream, StandardCharsets.UTF_8)) {
            YamlConfiguration current = YamlConfiguration.loadConfiguration(file);
            YamlConfiguration defaults = YamlConfiguration.loadConfiguration(reader);
            int added = addMissingDefaults(current, defaults);
            if (added <= 0) {
                return 0;
            }

            backup(file);
            current.save(file);
            plugin.getLogger().info(fileName + "에 누락된 설정 " + added + "개를 추가했습니다.");
            return added;
        } catch (IOException e) {
            plugin.getLogger().warning(fileName + " 기본 설정 병합 실패: " + e.getMessage());
            return 0;
        }
    }

    private static int createFromResourceOrEmpty(JavaPlugin plugin, String fileName, File file) {
        try {
            plugin.saveResource(fileName, false);
            return 0;
        } catch (IllegalArgumentException ignored) {
            try {
                //noinspection ResultOfMethodCallIgnored
                file.createNewFile();
            } catch (IOException e) {
                throw new IllegalStateException("Cannot create config file: " + file.getAbsolutePath(), e);
            }
            return 0;
        }
    }

    private static int addMissingDefaults(YamlConfiguration current, YamlConfiguration defaults) {
        int added = 0;
        for (String path : defaults.getKeys(true)) {
            if (defaults.isConfigurationSection(path) || current.contains(path, true)) {
                continue;
            }
            current.set(path, defaults.get(path));
            added++;
        }
        return added;
    }

    private static void backup(File file) throws IOException {
        String timestamp = LocalDateTime.now().format(BACKUP_TIME_FORMAT);
        File backupFile = new File(file.getParentFile(), file.getName() + ".bak-" + timestamp);
        int duplicate = 1;
        while (backupFile.exists()) {
            backupFile = new File(file.getParentFile(), file.getName() + ".bak-" + timestamp + "-" + duplicate);
            duplicate++;
        }
        Files.copy(file.toPath(), backupFile.toPath(), StandardCopyOption.COPY_ATTRIBUTES);
    }
}
