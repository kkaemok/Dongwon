package org.kkaemok.dongwon.land;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.kkaemok.dongwon.progression.ProfileManager;
import org.kkaemok.dongwon.text.ConfigText;
import org.kkaemok.dongwon.text.MessageConfig;

import java.io.File;
import java.lang.reflect.Field;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import static org.kkaemok.dongwon.text.ConfigText.placeholder;

public final class LandManager {
    private static final Pattern DEFAULT_NAME_PATTERN = Pattern.compile("^[A-Za-z0-9가-힣_-]{1,24}$");

    private final JavaPlugin plugin;
    private final ConfigText text;
    private final ProfileManager profileManager;
    private final MessageConfig landConfig;
    private final Map<String, LandClaim> landsByKey = new LinkedHashMap<>();
    private final Map<UUID, String> currentLandByPlayer = new HashMap<>();
    private final Map<UUID, String> pendingRequestByPlayer = new HashMap<>();
    private final Map<UUID, BukkitTask> boundaryTasks = new HashMap<>();

    public LandManager(JavaPlugin plugin, ConfigText text, ProfileManager profileManager, MessageConfig landConfig) {
        this.plugin = plugin;
        this.text = text;
        this.profileManager = profileManager;
        this.landConfig = landConfig;
        load();
    }

    public void create(Player player, String rawName) {
        String name = rawName == null ? "" : rawName.trim();
        if (!isValidName(name)) {
            send(player, "invalid-name", "<red>땅 이름은 한글, 영문, 숫자, _, - 조합으로 1~24자만 가능합니다.");
            return;
        }

        String key = normalizeKey(name);
        if (landsByKey.containsKey(key)) {
            send(player, "name-exists", "<red>이미 존재하는 땅 이름입니다. 다른 이름을 사용해 주세요.");
            return;
        }

        int maxLands = maxLands();
        int owned = ownedLands(player.getUniqueId()).size();
        if (owned >= maxLands) {
            send(player, "max-reached", "<red>더 이상 땅을 설정할 수 없습니다! (최대 %max%개)",
                    placeholder("max", maxLands));
            return;
        }

        int radius = claimRadius();
        Location center = player.getLocation();
        int minX = center.getBlockX() - radius;
        int maxX = center.getBlockX() + radius;
        int minZ = center.getBlockZ() - radius;
        int maxZ = center.getBlockZ() + radius;
        LandClaim claim = new LandClaim(
                key,
                name,
                player.getUniqueId(),
                player.getName(),
                player.getWorld().getName(),
                minX,
                maxX,
                minZ,
                maxZ,
                System.currentTimeMillis(),
                Map.of()
        );

        Optional<LandClaim> overlap = landsByKey.values().stream()
                .filter(claim::overlaps)
                .findFirst();
        if (overlap.isPresent()) {
            send(player, "overlap", "<red>설정하려는 구역이 '%land%' 땅과 겹칩니다! 다른 곳에 설정해 주세요.",
                    placeholders(overlap.get()));
            playSound(player, "deny");
            return;
        }

        landsByKey.put(key, claim);
        save();
        send(player, "created", "<green>성공적으로 '%land%' 땅을 설정했습니다! 중심 반경: %radius%블럭",
                placeholders(claim, placeholder("radius", radius)));
        send(player, "created-boundary-hint", "<yellow>/땅경계 %land% <white>명령어로 테두리를 확인할 수 있습니다.",
                placeholders(claim));
        playSound(player, "success");
    }

    public void delete(Player player, String name) {
        LandClaim claim = landByName(name).orElse(null);
        if (claim == null) {
            send(player, "not-found", "<red>존재하지 않는 땅입니다.");
            return;
        }
        if (!canManage(player, claim)) {
            send(player, "not-owner", "<red>해당 땅의 소유자만 사용할 수 있습니다.");
            return;
        }

        landsByKey.remove(claim.getKey());
        pendingRequestByPlayer.entrySet().removeIf(entry -> entry.getValue().equals(claim.getKey()));
        currentLandByPlayer.entrySet().removeIf(entry -> entry.getValue().equals(claim.getKey()));
        save();
        send(player, "deleted", "<green>'%land%' 땅을 삭제했습니다.", placeholders(claim));
        playSound(player, "success");
    }

    public void kick(Player player, String landName, String targetName) {
        LandClaim claim = ownedLand(player, landName);
        if (claim == null) {
            return;
        }

        UUID targetId = memberIdByName(claim, targetName).orElse(null);
        if (targetId == null) {
            send(player, "not-member", "<red>해당 플레이어는 이 땅의 멤버가 아닙니다.");
            return;
        }

        String removedName = claim.getMembers().getOrDefault(targetId, targetName);
        claim.removeMember(targetId);
        save();
        send(player, "member-kicked", "<yellow>%player% 님을 '%land%' 땅에서 추방했습니다.",
                placeholders(claim, placeholder("player", removedName)));
        Player target = Bukkit.getPlayer(targetId);
        if (target != null) {
            send(target, "member-kicked-notify", "<red>'%land%' 땅에서 추방되었습니다.", placeholders(claim));
        }
        playSound(player, "success");
    }

    public void requestAccess(Player player) {
        LandClaim claim = landAt(player.getLocation()).orElse(null);
        if (claim == null) {
            send(player, "request-no-land", "<red>현재 서 있는 곳은 다른 사람의 땅이 아닙니다.");
            return;
        }
        if (canUse(player, claim)) {
            send(player, "already-allowed", "<yellow>이미 이 땅에서 상호작용할 수 있습니다.");
            return;
        }

        pendingRequestByPlayer.put(player.getUniqueId(), claim.getKey());
        send(player, "request-sent", "<green>%owner% 님에게 '%land%' 땅 허락 요청을 보냈습니다.", placeholders(claim));
        Player owner = Bukkit.getPlayer(claim.getOwnerId());
        if (owner != null && owner.isOnline()) {
            send(owner, "request-received", "<yellow>%player% 님이 '%land%' 땅 허락을 요청했습니다. /허락수락 %land% %player%",
                    placeholders(claim, placeholder("player", player.getName())));
            playSound(owner, "request");
        }
    }

    public void acceptRequest(Player owner, String landName, Player target) {
        LandClaim claim = ownedLand(owner, landName);
        if (claim == null) {
            return;
        }

        String requestedLand = pendingRequestByPlayer.get(target.getUniqueId());
        if (!claim.getKey().equals(requestedLand)) {
            send(owner, "no-request", "<red>해당 플레이어의 허락 요청이 없습니다.");
            return;
        }

        pendingRequestByPlayer.remove(target.getUniqueId());
        claim.addMember(target.getUniqueId(), target.getName());
        save();
        send(owner, "request-accepted", "<green>%player% 님에게 '%land%' 땅 권한을 부여했습니다.",
                placeholders(claim, placeholder("player", target.getName())));
        send(target, "request-accepted-notify", "<green>'%land%' 땅 권한을 받았습니다.", placeholders(claim));
        playSound(owner, "success");
        playSound(target, "success");
    }

    public void transfer(Player owner, String landName, Player target) {
        LandClaim claim = ownedLand(owner, landName);
        if (claim == null) {
            return;
        }
        if (owner.getUniqueId().equals(target.getUniqueId())) {
            send(owner, "transfer-self", "<red>자기 자신에게는 땅을 양도할 수 없습니다.");
            return;
        }

        claim.setOwner(target.getUniqueId(), target.getName());
        claim.addMember(owner.getUniqueId(), owner.getName());
        save();
        send(owner, "transferred", "<green>'%land%' 땅을 %player% 님에게 양도했습니다.",
                placeholders(claim, placeholder("player", target.getName())));
        send(target, "transferred-notify", "<green>%player% 님이 '%land%' 땅을 양도했습니다.",
                placeholders(claim, placeholder("player", owner.getName())));
        playSound(owner, "success");
        playSound(target, "success");
    }

    public void info(Player player, String landName) {
        LandClaim claim = landName == null
                ? landAt(player.getLocation()).orElse(null)
                : landByName(landName).orElse(null);
        if (claim == null) {
            send(player, "not-found", "<red>존재하지 않는 땅입니다.");
            return;
        }

        String members = claim.getMembers().values().stream()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .reduce((left, right) -> left + ", " + right)
                .orElse("-");
        send(player, "info", "<yellow>%land% <gray>| <white>소유자: %owner% <gray>| <white>월드: %world% <gray>| <white>X %min_x%~%max_x%, Z %min_z%~%max_z% <gray>| <white>멤버: %members%",
                placeholders(claim, placeholder("members", members)));
    }

    public void list(Player player) {
        List<LandClaim> lands = ownedLands(player.getUniqueId());
        if (lands.isEmpty()) {
            send(player, "list-empty", "<yellow>보유한 땅이 없습니다.");
            return;
        }

        String names = lands.stream()
                .map(LandClaim::getName)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .reduce((left, right) -> left + ", " + right)
                .orElse("-");
        send(player, "list", "<yellow>내 땅: <white>%lands%", placeholder("lands", names));
    }

    public void showBoundary(Player player, String landName) {
        LandClaim claim = landByName(landName).orElse(null);
        if (claim == null) {
            send(player, "not-found", "<red>존재하지 않는 땅입니다.");
            return;
        }
        if (!player.getWorld().getName().equals(claim.getWorldName())) {
            send(player, "boundary-wrong-world", "<red>해당 땅이 있는 월드에서만 경계를 볼 수 있습니다. 월드: %world%",
                    placeholders(claim));
            return;
        }

        BukkitTask previous = boundaryTasks.remove(player.getUniqueId());
        if (previous != null) {
            previous.cancel();
        }

        send(player, "boundary-start", "<green>'%land%' 땅의 경계선을 %seconds%초 동안 표시합니다.",
                placeholders(claim, placeholder("seconds", boundaryDurationSeconds())));
        playSound(player, "boundary");

        long interval = boundaryIntervalTicks();
        int durationTicks = boundaryDurationSeconds() * 20;
        AtomicInteger elapsed = new AtomicInteger();
        BukkitTask[] taskRef = new BukkitTask[1];
        taskRef[0] = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            drawBoundary(player, claim);
            if (elapsed.addAndGet((int) interval) >= durationTicks) {
                BukkitTask task = taskRef[0];
                if (task != null) {
                    task.cancel();
                    boundaryTasks.remove(player.getUniqueId());
                }
            }
        }, 0L, interval);
        boundaryTasks.put(player.getUniqueId(), taskRef[0]);
    }

    public void handleMove(Player player, Location to) {
        LandClaim claim = landAt(to).orElse(null);
        String nextKey = claim == null ? null : claim.getKey();
        String previousKey = currentLandByPlayer.get(player.getUniqueId());
        if ((previousKey == null && nextKey == null) || (previousKey != null && previousKey.equals(nextKey))) {
            return;
        }

        if (nextKey == null) {
            currentLandByPlayer.remove(player.getUniqueId());
            showTitle(player, "leave-title", "leave-subtitle",
                    "<gray>야생", "<white>주인 없는 구역으로 나왔습니다.");
            return;
        }

        currentLandByPlayer.put(player.getUniqueId(), nextKey);
        if (claim.isOwner(player.getUniqueId())) {
            showTitle(player, "enter-owned-title", "enter-owned-subtitle",
                    "<green>나의 땅", "<white>%land% 구역에 진입했습니다.", placeholders(claim));
            return;
        }
        if (claim.isMember(player.getUniqueId())) {
            showTitle(player, "enter-member-title", "enter-member-subtitle",
                    "<green>%owner%님의 땅", "<white>%land% 구역에 진입했습니다.", placeholders(claim));
            return;
        }

        pendingRequestByPlayer.put(player.getUniqueId(), claim.getKey());
        showTitle(player, "enter-other-title", "enter-other-subtitle",
                "<red>타인의 땅", "<white>상호작용이 불가능한 구역입니다.", placeholders(claim));
        send(player, "enter-other-chat", "<red>[%owner%]님의 땅입니다. 상자를 열거나 건축하려면 <yellow>/허락요청 <red>명령어를 치세요.",
                placeholders(claim));
    }

    public Optional<LandClaim> landAt(Location location) {
        return landsByKey.values().stream()
                .filter(land -> land.contains(location))
                .findFirst();
    }

    public Optional<LandClaim> landByName(String name) {
        return Optional.ofNullable(landsByKey.get(normalizeKey(name)));
    }

    public boolean canUse(Player player, LandClaim claim) {
        UUID playerId = player.getUniqueId();
        return claim.isOwner(playerId)
                || claim.isMember(playerId)
                || player.hasPermission("dongwon.land.bypass");
    }

    public void deny(Player player, LandClaim claim, String messageKey, String fallback) {
        send(player, messageKey, fallback, placeholders(claim));
        playSound(player, "deny");
    }

    public List<String> allLandNames() {
        return landsByKey.values().stream()
                .map(LandClaim::getName)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    public List<String> ownedLandNames(Player player) {
        return ownedLands(player.getUniqueId()).stream()
                .map(LandClaim::getName)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    public List<String> memberNames(Player player, String landName) {
        LandClaim claim = landByName(landName).orElse(null);
        if (claim == null || !canManage(player, claim)) {
            return List.of();
        }
        return claim.getMembers().values().stream()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    public void save() {
        profileManager.saveLandClaims(landsByKey.values());
    }

    public void shutdown() {
        for (BukkitTask task : boundaryTasks.values()) {
            task.cancel();
        }
        boundaryTasks.clear();
        save();
    }

    private void load() {
        landsByKey.clear();
        for (LandClaim claim : profileManager.loadLandClaims()) {
            landsByKey.put(claim.getKey(), claim);
        }
        int beforeLegacy = landsByKey.size();
        loadLegacyLands();
        if (landsByKey.size() > beforeLegacy) {
            save();
        }
    }

    private void loadLegacyLands() {
        File file = new File(plugin.getDataFolder(), "lands.yml");
        if (!file.exists()) {
            return;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection lands = config.getConfigurationSection("lands");
        if (lands == null) {
            return;
        }

        for (String key : lands.getKeys(false)) {
            String base = "lands." + key;
            try {
                String name = config.getString(base + ".name", key);
                String world = config.getString(base + ".world", "");
                UUID owner = UUID.fromString(config.getString(base + ".owner", ""));
                String ownerName = config.getString(base + ".owner-name", owner.toString());
                int minX = config.getInt(base + ".min-x");
                int maxX = config.getInt(base + ".max-x");
                int minZ = config.getInt(base + ".min-z");
                int maxZ = config.getInt(base + ".max-z");
                long createdAt = parseCreatedAt(config.getString(base + ".created-at", ""));
                Map<UUID, String> members = new LinkedHashMap<>();
                ConfigurationSection memberSection = config.getConfigurationSection(base + ".members");
                if (memberSection != null) {
                    for (String memberKey : memberSection.getKeys(false)) {
                        UUID memberId = UUID.fromString(memberKey);
                        members.put(memberId, config.getString(base + ".members." + memberKey + ".name", memberId.toString()));
                    }
                }

                LandClaim claim = new LandClaim(key, name, owner, ownerName, world, minX, maxX, minZ, maxZ, createdAt, members);
                landsByKey.putIfAbsent(key, claim);
            } catch (IllegalArgumentException ex) {
                plugin.getLogger().warning("lands.yml의 땅 데이터를 불러오지 못했습니다: " + key);
            }
        }
    }

    private LandClaim ownedLand(Player player, String landName) {
        LandClaim claim = landByName(landName).orElse(null);
        if (claim == null) {
            send(player, "not-found", "<red>존재하지 않는 땅입니다.");
            return null;
        }
        if (!canManage(player, claim)) {
            send(player, "not-owner", "<red>해당 땅의 소유자만 사용할 수 있습니다.");
            return null;
        }
        return claim;
    }

    private boolean canManage(Player player, LandClaim claim) {
        return claim.isOwner(player.getUniqueId()) || player.hasPermission("dongwon.land.bypass");
    }

    private Optional<UUID> memberIdByName(LandClaim claim, String name) {
        String normalized = name.toLowerCase(Locale.ROOT);
        return claim.getMembers().entrySet().stream()
                .filter(entry -> entry.getValue().toLowerCase(Locale.ROOT).equals(normalized))
                .map(Map.Entry::getKey)
                .findFirst();
    }

    private List<LandClaim> ownedLands(UUID ownerId) {
        return landsByKey.values().stream()
                .filter(land -> land.isOwner(ownerId))
                .sorted(Comparator.comparing(LandClaim::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private boolean isValidName(String name) {
        if (name.isBlank() || name.contains(".")) {
            return false;
        }
        Pattern pattern = namePattern();
        return pattern.matcher(name).matches();
    }

    private Pattern namePattern() {
        String raw = landConfig.getString("land.settings.name-pattern", "");
        if (raw == null || raw.isBlank()) {
            return DEFAULT_NAME_PATTERN;
        }
        try {
            return Pattern.compile(raw);
        } catch (PatternSyntaxException ex) {
            return DEFAULT_NAME_PATTERN;
        }
    }

    private String normalizeKey(String name) {
        return name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
    }

    private int maxLands() {
        return Math.max(1, landConfig.getInt("land.settings.max-lands", 3));
    }

    private int claimRadius() {
        return Math.max(1, landConfig.getInt("land.settings.claim-radius", 50));
    }

    private int boundaryDurationSeconds() {
        return Math.max(1, landConfig.getInt("land.particles.duration-seconds", 10));
    }

    private long boundaryIntervalTicks() {
        return Math.max(1L, landConfig.getLong("land.particles.interval-ticks", 10L));
    }

    private int boundaryStep() {
        return Math.max(1, landConfig.getInt("land.particles.step", 2));
    }

    private int cornerHeight() {
        return Math.max(1, landConfig.getInt("land.particles.corner.height", 5));
    }

    private void drawBoundary(Player player, LandClaim claim) {
        World world = Bukkit.getWorld(claim.getWorldName());
        if (world == null || !player.getWorld().equals(world)) {
            return;
        }

        int y = player.getLocation().getBlockY() + 1;
        int step = boundaryStep();
        for (int x = claim.getMinX(); x <= claim.getMaxX(); x += step) {
            spawnLineParticle(player, new Location(world, x + 0.5D, y, claim.getMinZ() + 0.5D));
            spawnLineParticle(player, new Location(world, x + 0.5D, y, claim.getMaxZ() + 0.5D));
        }
        for (int z = claim.getMinZ(); z <= claim.getMaxZ(); z += step) {
            spawnLineParticle(player, new Location(world, claim.getMinX() + 0.5D, y, z + 0.5D));
            spawnLineParticle(player, new Location(world, claim.getMaxX() + 0.5D, y, z + 0.5D));
        }

        drawCorner(player, world, claim.getMinX(), claim.getMinZ(), y);
        drawCorner(player, world, claim.getMinX(), claim.getMaxZ(), y);
        drawCorner(player, world, claim.getMaxX(), claim.getMinZ(), y);
        drawCorner(player, world, claim.getMaxX(), claim.getMaxZ(), y);
    }

    private void drawCorner(Player player, World world, int x, int z, int baseY) {
        for (int y = baseY; y <= baseY + cornerHeight(); y++) {
            spawnCornerParticle(player, new Location(world, x + 0.5D, y, z + 0.5D));
        }
    }

    private void spawnLineParticle(Player player, Location location) {
        Particle particle = particle("land.particles.line.type", Particle.DUST);
        int count = Math.max(1, landConfig.getInt("land.particles.line.count", 2));
        double offset = Math.max(0.0D, landConfig.getDouble("land.particles.line.offset", 0.08D));
        if (particle == Particle.DUST) {
            Color color = color("land.particles.line.color", Color.LIME);
            float size = (float) Math.max(0.1D, landConfig.getDouble("land.particles.line.size", 1.4D));
            player.spawnParticle(particle, location, count, offset, offset, offset, 0.0D, new Particle.DustOptions(color, size));
            return;
        }
        player.spawnParticle(particle, location, count, offset, offset, offset, 0.0D);
    }

    private void spawnCornerParticle(Player player, Location location) {
        Particle particle = particle("land.particles.corner.type", Particle.END_ROD);
        int count = Math.max(1, landConfig.getInt("land.particles.corner.count", 2));
        double offset = Math.max(0.0D, landConfig.getDouble("land.particles.corner.offset", 0.05D));
        player.spawnParticle(particle, location, count, offset, offset, offset, 0.0D);
    }

    private Particle particle(String path, Particle fallback) {
        String raw = landConfig.getString(path, "");
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return Particle.valueOf(normalizeEnum(raw));
        } catch (IllegalArgumentException ex) {
            return fallback;
        }
    }

    private Color color(String path, Color fallback) {
        String raw = landConfig.getString(path, "");
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        String hex = raw.trim();
        if (hex.startsWith("#")) {
            hex = hex.substring(1);
        }
        if (hex.length() != 6) {
            return fallback;
        }
        try {
            int rgb = Integer.parseInt(hex, 16);
            return Color.fromRGB((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF);
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private void playSound(Player player, String key) {
        String base = "land.sounds." + key;
        if (!landConfig.getBoolean(base + ".enabled", true)) {
            return;
        }
        String raw = landConfig.getString(base + ".name", "");
        if (raw == null || raw.isBlank()) {
            return;
        }
        try {
            Sound sound = sound(raw);
            if (sound == null) {
                return;
            }
            float volume = (float) landConfig.getDouble(base + ".volume", 0.8D);
            float pitch = (float) landConfig.getDouble(base + ".pitch", 1.0D);
            player.playSound(player.getLocation(), sound, volume, pitch);
        } catch (IllegalArgumentException ignored) {
        }
    }

    private Sound sound(String raw) {
        NamespacedKey key = NamespacedKey.fromString(raw.toLowerCase(Locale.ROOT));
        if (key != null) {
            Sound sound = Registry.SOUNDS.get(key);
            if (sound != null) {
                return sound;
            }
        }

        try {
            Field field = Sound.class.getField(normalizeEnum(raw));
            Object value = field.get(null);
            return value instanceof Sound sound ? sound : null;
        } catch (IllegalAccessException | NoSuchFieldException ex) {
            return null;
        }
    }

    private String normalizeEnum(String raw) {
        return raw.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
    }

    private void showTitle(
            Player player,
            String titlePath,
            String subtitlePath,
            String titleFallback,
            String subtitleFallback,
            ConfigText.Placeholder... placeholders
    ) {
        Component title = text.component("land.titles." + titlePath, titleFallback, placeholders);
        Component subtitle = text.component("land.titles." + subtitlePath, subtitleFallback, placeholders);
        int fadeIn = Math.max(0, landConfig.getInt("land.titles.fade-in-ticks", 5));
        int stay = Math.max(1, landConfig.getInt("land.titles.stay-ticks", 40));
        int fadeOut = Math.max(0, landConfig.getInt("land.titles.fade-out-ticks", 10));
        player.showTitle(Title.title(
                title,
                subtitle,
                Title.Times.times(Duration.ofMillis(fadeIn * 50L), Duration.ofMillis(stay * 50L), Duration.ofMillis(fadeOut * 50L))
        ));
    }

    private void send(Player player, String key, String fallback, ConfigText.Placeholder... placeholders) {
        text.send(player, "land.messages." + key, fallback, placeholders);
    }

    private ConfigText.Placeholder[] placeholders(LandClaim claim, ConfigText.Placeholder... extra) {
        List<ConfigText.Placeholder> placeholders = new ArrayList<>();
        placeholders.add(placeholder("land", claim.getName()));
        placeholders.add(placeholder("owner", claim.getOwnerName()));
        placeholders.add(placeholder("world", claim.getWorldName()));
        placeholders.add(placeholder("min_x", claim.getMinX()));
        placeholders.add(placeholder("max_x", claim.getMaxX()));
        placeholders.add(placeholder("min_z", claim.getMinZ()));
        placeholders.add(placeholder("max_z", claim.getMaxZ()));
        placeholders.add(placeholder("size_x", claim.getMaxX() - claim.getMinX() + 1));
        placeholders.add(placeholder("size_z", claim.getMaxZ() - claim.getMinZ() + 1));
        placeholders.addAll(List.of(extra));
        return placeholders.toArray(ConfigText.Placeholder[]::new);
    }

    private long parseCreatedAt(String raw) {
        if (raw == null || raw.isBlank()) {
            return System.currentTimeMillis();
        }
        try {
            return Instant.parse(raw).toEpochMilli();
        } catch (RuntimeException ignored) {
            return System.currentTimeMillis();
        }
    }
}
