package org.kkaemok.dongwon.menu;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class HomeManager {
    private final Plugin plugin;
    private final File file;
    private final Map<UUID, Location> homes = new HashMap<>();
    private YamlConfiguration config;

    public HomeManager(Plugin plugin) {
        this.plugin = plugin;
        if (!plugin.getDataFolder().exists()) {
            //noinspection ResultOfMethodCallIgnored
            plugin.getDataFolder().mkdirs();
        }
        this.file = new File(plugin.getDataFolder(), "homes.yml");
        this.config = YamlConfiguration.loadConfiguration(file);
        load();
    }

    public boolean setHome(Player player) {
        if (player.getWorld().getEnvironment() != World.Environment.NORMAL) {
            player.sendMessage("§c홈 설정은 오버월드에서만 가능합니다.");
            return false;
        }
        homes.put(player.getUniqueId(), player.getLocation().clone());
        save();
        player.sendMessage("§a홈이 설정되었습니다.");
        return true;
    }

    public boolean teleportHome(Player player) {
        Location home = homes.get(player.getUniqueId());
        if (home == null) {
            player.sendMessage("§c설정된 홈이 없습니다. 오버월드에서 홈 설정을 먼저 해주세요.");
            return false;
        }

        World world = home.getWorld();
        if (world == null || world.getEnvironment() != World.Environment.NORMAL) {
            world = findOverworld().orElse(null);
        }
        if (world == null) {
            player.sendMessage("§c오버월드를 찾을 수 없어 홈으로 이동할 수 없습니다.");
            return false;
        }

        Location target = new Location(
                world,
                home.getX(),
                home.getY(),
                home.getZ(),
                home.getYaw(),
                home.getPitch()
        );
        player.teleport(target);
        player.sendMessage("§a홈으로 이동했습니다.");
        return true;
    }

    public void save() {
        config.set("players", null);
        for (Map.Entry<UUID, Location> entry : homes.entrySet()) {
            String base = "players." + entry.getKey();
            Location location = entry.getValue();
            World world = location.getWorld();
            if (world == null) {
                continue;
            }
            config.set(base + ".world", world.getName());
            config.set(base + ".x", location.getX());
            config.set(base + ".y", location.getY());
            config.set(base + ".z", location.getZ());
            config.set(base + ".yaw", location.getYaw());
            config.set(base + ".pitch", location.getPitch());
        }
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("homes.yml 저장 실패: " + e.getMessage());
        }
    }

    private void load() {
        homes.clear();
        this.config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = config.getConfigurationSection("players");
        if (section == null) {
            return;
        }
        for (String key : section.getKeys(false)) {
            try {
                UUID playerId = UUID.fromString(key);
                String base = "players." + key;
                String worldName = config.getString(base + ".world", "");
                World world = Bukkit.getWorld(worldName);
                if (world == null) {
                    world = findOverworld().orElse(null);
                }
                if (world == null) {
                    continue;
                }
                double x = config.getDouble(base + ".x");
                double y = config.getDouble(base + ".y");
                double z = config.getDouble(base + ".z");
                float yaw = (float) config.getDouble(base + ".yaw");
                float pitch = (float) config.getDouble(base + ".pitch");
                homes.put(playerId, new Location(world, x, y, z, yaw, pitch));
            } catch (IllegalArgumentException ignored) {
                // malformed uuid; ignore
            }
        }
    }

    private Optional<World> findOverworld() {
        return Bukkit.getWorlds().stream()
                .filter(world -> world.getEnvironment() == World.Environment.NORMAL)
                .findFirst();
    }
}
