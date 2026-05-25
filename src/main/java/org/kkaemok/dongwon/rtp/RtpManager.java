package org.kkaemok.dongwon.rtp;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.HeightMap;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.kkaemok.dongwon.text.ConfigText.placeholder;

public final class RtpManager {
    private static final long TICKS_PER_SECOND = 20L;

    private final JavaPlugin plugin;
    private final RtpConfig config;
    private final Random random = new Random();
    private final Map<UUID, PendingRtp> pendingByPlayer = new HashMap<>();

    public RtpManager(JavaPlugin plugin, RtpConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    public Result start(Player player) {
        UUID playerId = player.getUniqueId();
        if (pendingByPlayer.containsKey(playerId)) {
            return Result.fail(config.message("already-running", "<red>이미 RTP가 진행 중입니다."));
        }

        PendingRtp pending = new PendingRtp(config.countdownSeconds());
        pendingByPlayer.put(playerId, pending);

        CompletableFuture<Location> prepareFuture = new CompletableFuture<>();
        pending.setPrepareFuture(prepareFuture);
        Location start = player.getLocation().clone();
        searchSafeLocationAsync(
                player.getWorld(),
                0,
                0,
                start.getYaw(),
                start.getPitch(),
                0,
                prepareFuture,
                pending
        );

        pending.countdownTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> tick(playerId), 0L, TICKS_PER_SECOND);
        return Result.ok(config.message("started", "<green>RTP를 시작했습니다. 움직이면 취소됩니다."));
    }

    public void onPlayerMove(PlayerMoveEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        if (!pendingByPlayer.containsKey(playerId)) {
            return;
        }

        Location from = event.getFrom();
        Location to = event.getTo();
        if (from.getX() != to.getX() || from.getZ() != to.getZ()) {
            cancel(playerId);
        }
    }

    public void onQuit(Player player) {
        cancelSilently(player.getUniqueId());
    }

    public void shutdown() {
        for (UUID playerId : pendingByPlayer.keySet().toArray(UUID[]::new)) {
            cancelSilently(playerId);
        }
    }

    private void tick(UUID playerId) {
        PendingRtp pending = pendingByPlayer.get(playerId);
        if (pending == null || pending.isCancelled()) {
            return;
        }

        Player player = plugin.getServer().getPlayer(playerId);
        if (player == null || !player.isOnline()) {
            cancelSilently(playerId);
            return;
        }

        if (pending.remainingSeconds > 0) {
            player.sendActionBar(config.message("countdown-actionbar",
                    "<gold>RTP %seconds%초... <gray>(이동 시 취소)",
                    placeholder("seconds", pending.remainingSeconds)));
            pending.remainingSeconds--;
            return;
        }

        pending.cancelCountdownTask();
        onCountdownCompleted(playerId);
    }

    private void onCountdownCompleted(UUID playerId) {
        PendingRtp pending = pendingByPlayer.get(playerId);
        if (pending == null || pending.isCancelled()) {
            return;
        }

        Player player = Bukkit.getPlayer(playerId);
        if (player == null || !player.isOnline()) {
            cancelSilently(playerId);
            return;
        }

        CompletableFuture<Location> prepareFuture = pending.prepareFuture();
        if (prepareFuture == null) {
            cancelSession(playerId, true, config.message("failed", "<red>RTP에 실패했습니다. 잠시 후 다시 시도해 주세요."));
            return;
        }

        if (!prepareFuture.isDone()) {
            startWaitingActionbar(player, pending);
        }

        prepareFuture.whenComplete((safeLocation, throwable) ->
                Bukkit.getScheduler().runTask(plugin, () -> handlePreparedResult(playerId, pending, safeLocation, throwable))
        );
    }

    private void startWaitingActionbar(Player player, PendingRtp pending) {
        if (pending.waitingTask() != null) {
            return;
        }

        BukkitTask waitingTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancelSilently(player.getUniqueId());
                    cancel();
                    return;
                }

                if (pending.isCancelled()) {
                    cancel();
                    return;
                }

                CompletableFuture<Location> future = pending.prepareFuture();
                if (future != null && future.isDone()) {
                    cancel();
                    return;
                }

                if (pending.waitingActionbarTick() % TICKS_PER_SECOND == 0L) {
                    player.sendActionBar(config.message("waiting-actionbar", "<gray>목적지 청크를 준비하는 중입니다..."));
                }
                pending.setWaitingActionbarTick(pending.waitingActionbarTick() + 1L);
            }
        }.runTaskTimer(plugin, 0L, 1L);

        pending.setWaitingTask(waitingTask);
    }

    private void handlePreparedResult(UUID playerId, PendingRtp pending, Location safeLocation, Throwable throwable) {
        if (pendingByPlayer.get(playerId) != pending) {
            return;
        }

        pending.cancelWaitingTask();

        Player player = Bukkit.getPlayer(playerId);
        if (player == null || !player.isOnline()) {
            cancelSilently(playerId);
            return;
        }
        if (pending.isCancelled()) {
            cancelSilently(playerId);
            return;
        }

        if (throwable != null || safeLocation == null) {
            cancelSession(playerId, true, config.message("failed", "<red>RTP에 실패했습니다. 잠시 후 다시 시도해 주세요."));
            return;
        }

        player.teleportAsync(safeLocation).whenComplete((success, teleportError) ->
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (!player.isOnline()) {
                        cancelSilently(playerId);
                        return;
                    }
                    if (pending.isCancelled()) {
                        cancelSilently(playerId);
                        return;
                    }

                    if (teleportError != null || !Boolean.TRUE.equals(success)) {
                        cancelSession(playerId, true, config.message("teleport-failed", "<red>텔레포트에 실패했습니다."));
                        return;
                    }

                    player.sendMessage(config.message("success",
                            "<green>RTP 완료: %x%, %y%, %z%",
                            placeholder("x", safeLocation.getBlockX()),
                            placeholder("y", safeLocation.getBlockY()),
                            placeholder("z", safeLocation.getBlockZ())));
                    cancelSession(playerId, false, null);
                })
        );
    }

    private void searchSafeLocationAsync(
            World world,
            int centerX,
            int centerZ,
            float yaw,
            float pitch,
            int attempt,
            CompletableFuture<Location> result,
            PendingRtp pending
    ) {
        if (pending.isCancelled() || result.isDone()) {
            return;
        }

        if (attempt >= config.maxAttempts()) {
            result.complete(null);
            return;
        }

        Candidate candidate = randomCandidate(centerX, centerZ, config.minDistance(), config.maxDistance(world));
        Location sampleLocation = new Location(world, candidate.x + 0.5D, world.getMinHeight() + 1.0D, candidate.z + 0.5D);
        if (!world.getWorldBorder().isInside(sampleLocation)) {
            searchSafeLocationAsync(world, centerX, centerZ, yaw, pitch, attempt + 1, result, pending);
            return;
        }

        int chunkX = Math.floorDiv(candidate.x, 16);
        int chunkZ = Math.floorDiv(candidate.z, 16);
        world.getChunkAtAsync(chunkX, chunkZ, true).thenRun(() -> {
            if (pending.isCancelled() || result.isDone()) {
                return;
            }

            Location safeLocation = findSafeLocation(world, candidate.x, candidate.z, yaw, pitch);
            if (safeLocation == null) {
                searchSafeLocationAsync(world, centerX, centerZ, yaw, pitch, attempt + 1, result, pending);
                return;
            }
            result.complete(safeLocation);
        }).exceptionally(throwable -> {
            if (!pending.isCancelled() && !result.isDone()) {
                searchSafeLocationAsync(world, centerX, centerZ, yaw, pitch, attempt + 1, result, pending);
            }
            return null;
        });
    }

    private Location findSafeLocation(World world, int x, int z, float yaw, float pitch) {
        if (world.getEnvironment() == World.Environment.NETHER) {
            return findSafeLocationInNether(world, x, z, yaw, pitch);
        }
        return findSafeLocationByHeightMap(world, x, z, yaw, pitch);
    }

    private Location findSafeLocationByHeightMap(World world, int x, int z, float yaw, float pitch) {
        Block ground = world.getHighestBlockAt(x, z, HeightMap.MOTION_BLOCKING_NO_LEAVES);
        if (ground.getY() <= world.getMinHeight() || ground.getY() >= world.getMaxHeight() - 2) {
            return null;
        }
        if (isInvalidGround(world, ground)) {
            return null;
        }

        Block feet = ground.getRelative(BlockFace.UP);
        Block head = feet.getRelative(BlockFace.UP);
        if (isUnsafeBodySpace(feet, head)) {
            return null;
        }

        Location location = feet.getLocation().add(0.5D, 0.0D, 0.5D);
        location.setYaw(yaw);
        location.setPitch(pitch);
        return location;
    }

    private Location findSafeLocationInNether(World world, int x, int z, float yaw, float pitch) {
        int minY = world.getMinHeight() + 1;
        int maxFeetY = Math.min(world.getMaxHeight() - 2, 120);

        for (int feetY = maxFeetY; feetY >= minY; feetY--) {
            Block feet = world.getBlockAt(x, feetY, z);
            Block head = feet.getRelative(BlockFace.UP);
            Block ground = feet.getRelative(BlockFace.DOWN);

            if (isInvalidGround(world, ground)) {
                continue;
            }
            if (isUnsafeBodySpace(feet, head)) {
                continue;
            }

            Location location = feet.getLocation().add(0.5D, 0.0D, 0.5D);
            location.setYaw(yaw);
            location.setPitch(pitch);
            return location;
        }

        return null;
    }

    private boolean isInvalidGround(World world, Block ground) {
        Material material = ground.getType();
        return !material.isSolid()
                || ground.isLiquid()
                || config.isBannedGround(material)
                || isDangerousGround(material)
                || isNetherRoofBedrock(world, ground);
    }

    private boolean isUnsafeBodySpace(Block feet, Block head) {
        return !feet.isPassable()
                || !head.isPassable()
                || feet.isLiquid()
                || head.isLiquid()
                || isDangerousBodyBlock(feet.getType())
                || isDangerousBodyBlock(head.getType());
    }

    private boolean isDangerousGround(Material material) {
        return material == Material.MAGMA_BLOCK
                || material == Material.CACTUS
                || material == Material.CAMPFIRE
                || material == Material.SOUL_CAMPFIRE;
    }

    private boolean isDangerousBodyBlock(Material material) {
        return material == Material.FIRE
                || material == Material.SOUL_FIRE
                || material == Material.CACTUS
                || material == Material.SWEET_BERRY_BUSH
                || material == Material.POWDER_SNOW;
    }

    private boolean isNetherRoofBedrock(World world, Block ground) {
        return world.getEnvironment() == World.Environment.NETHER
                && ground.getType() == Material.BEDROCK
                && ground.getY() >= world.getMaxHeight() - 8;
    }

    private Candidate randomCandidate(int centerX, int centerZ, int minDistance, int maxDistance) {
        double max = Math.max(minDistance, maxDistance);
        double minSquared = (double) minDistance * minDistance;
        double maxSquared = max * max;
        double radius = Math.sqrt(random.nextDouble() * (maxSquared - minSquared) + minSquared);
        double angle = random.nextDouble() * Math.PI * 2.0D;
        int x = centerX + (int) Math.round(Math.cos(angle) * radius);
        int z = centerZ + (int) Math.round(Math.sin(angle) * radius);
        return new Candidate(x, z);
    }

    private void cancel(UUID playerId) {
        cancelSession(playerId, true, config.message("cancelled-moved", "<red>RTP가 취소되었습니다. 이동이 감지되었습니다."));
    }

    private void cancelSilently(UUID playerId) {
        cancelSession(playerId, false, null);
    }

    private void cancelSession(UUID playerId, boolean sendMessage, Component message) {
        PendingRtp pending = pendingByPlayer.remove(playerId);
        if (pending == null) {
            return;
        }
        pending.markCancelled();
        pending.cancelCountdownTask();
        pending.cancelWaitingTask();

        Player player = plugin.getServer().getPlayer(playerId);
        if (player != null && player.isOnline()) {
            if (sendMessage && message != null) {
                player.sendMessage(message);
            }
            player.sendActionBar(Component.empty());
        }
    }

    private record Candidate(int x, int z) {
    }

    public record Result(boolean success, Component message) {
        public static Result ok(Component message) {
            return new Result(true, message);
        }

        public static Result fail(Component message) {
            return new Result(false, message);
        }

        public void send(Player player) {
            player.sendMessage(message);
        }
    }

    private static final class PendingRtp {
        private int remainingSeconds;
        private BukkitTask countdownTask;
        private CompletableFuture<Location> prepareFuture;
        private BukkitTask waitingTask;
        private long waitingActionbarTick;
        private boolean cancelled;

        private PendingRtp(int remainingSeconds) {
            this.remainingSeconds = remainingSeconds;
        }

        private CompletableFuture<Location> prepareFuture() {
            return prepareFuture;
        }

        private void setPrepareFuture(CompletableFuture<Location> prepareFuture) {
            this.prepareFuture = prepareFuture;
        }

        private BukkitTask waitingTask() {
            return waitingTask;
        }

        private void setWaitingTask(BukkitTask waitingTask) {
            this.waitingTask = waitingTask;
        }

        private void cancelCountdownTask() {
            if (countdownTask != null) {
                countdownTask.cancel();
                countdownTask = null;
            }
        }

        private void cancelWaitingTask() {
            if (waitingTask != null) {
                waitingTask.cancel();
                waitingTask = null;
            }
        }

        private long waitingActionbarTick() {
            return waitingActionbarTick;
        }

        private void setWaitingActionbarTick(long waitingActionbarTick) {
            this.waitingActionbarTick = waitingActionbarTick;
        }

        private boolean isCancelled() {
            return cancelled;
        }

        private void markCancelled() {
            this.cancelled = true;
        }
    }
}
