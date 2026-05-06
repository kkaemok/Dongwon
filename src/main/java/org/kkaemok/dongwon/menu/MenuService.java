package org.kkaemok.dongwon.menu;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.kkaemok.dongwon.job.JobManager;
import org.kkaemok.dongwon.job.JobService;
import org.kkaemok.dongwon.job.JobType;
import org.kkaemok.dongwon.progression.PlayerProfile;
import org.kkaemok.dongwon.progression.ProfileManager;

import java.text.DecimalFormat;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class MenuService {
    private static final DecimalFormat NUMBER_FORMAT = new DecimalFormat("#,###");

    private final Plugin plugin;
    private final JobManager jobManager;
    private final JobService jobService;
    private final ProfileManager profileManager;
    private final HomeManager homeManager;
    private final NamespacedKey compassMarkerKey;
    private final NamespacedKey actionKey;
    private final NamespacedKey tpaTargetKey;

    public MenuService(
            Plugin plugin,
            JobManager jobManager,
            JobService jobService,
            ProfileManager profileManager,
            HomeManager homeManager
    ) {
        this.plugin = plugin;
        this.jobManager = jobManager;
        this.jobService = jobService;
        this.profileManager = profileManager;
        this.homeManager = homeManager;
        this.compassMarkerKey = new NamespacedKey(plugin, "server_menu_compass");
        this.actionKey = new NamespacedKey(plugin, "server_menu_action");
        this.tpaTargetKey = new NamespacedKey(plugin, "server_menu_tpa_target");
    }

    public void giveCompassIfMissing(Player player) {
        if (hasMenuCompass(player.getInventory())) {
            return;
        }
        ItemStack compass = createMenuCompass();
        player.getInventory().addItem(compass).values()
                .forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
    }

    public boolean isMenuCompass(ItemStack itemStack) {
        if (itemStack == null || itemStack.getType() != Material.COMPASS || !itemStack.hasItemMeta()) {
            return false;
        }
        ItemMeta itemMeta = itemStack.getItemMeta();
        return itemMeta.getPersistentDataContainer().has(compassMarkerKey, PersistentDataType.BYTE);
    }

    public void openMainMenu(Player player) {
        Inventory inventory = createInventory(MenuType.MAIN, 27, "§8서버 메뉴");
        fillBorder(inventory);

        inventory.setItem(11, createActionItem(Material.ENDER_PEARL, "§b-텔레포트-", "main.teleport"));
        inventory.setItem(12, createActionItem(Material.WHITE_BANNER, "§a-길드-", "main.guild"));
        inventory.setItem(13, createActionItem(Material.GOLD_INGOT, "§e송금", "main.remit"));
        inventory.setItem(14, createActionItem(Material.IRON_SWORD, "§f기본 직업 획득", "main.basic_job"));
        inventory.setItem(15, createActionItem(Material.BOOK, "§d-내정보-", "main.info"));

        player.openInventory(inventory);
    }

    public void openTeleportMenu(Player player) {
        Inventory inventory = createInventory(MenuType.TELEPORT, 27, "§8텔레포트 메뉴");
        fillBorder(inventory);

        inventory.setItem(10, createActionItem(Material.LODESTONE, "§f스폰", "teleport.spawn"));
        inventory.setItem(11, createActionItem(Material.EMERALD, "§a상점", "teleport.shop"));
        inventory.setItem(12, createActionItem(Material.OAK_DOOR, "§e홈", "teleport.home"));
        inventory.setItem(13, createActionItem(Material.COMPASS, "§6홈 설정", "teleport.home_set"));
        inventory.setItem(14, createActionItem(Material.PLAYER_HEAD, "§bTPA", "teleport.tpa"));
        inventory.setItem(15, createActionItem(Material.ENDER_EYE, "§dRTP", "teleport.rtp"));
        inventory.setItem(22, createActionItem(Material.BARRIER, "§c뒤로가기", "teleport.back"));

        player.openInventory(inventory);
    }

    public void openTpaMenu(Player player) {
        Inventory inventory = createInventory(MenuType.TPA, 54, "§8TPA 대상 선택");
        fillInventory(inventory, createFiller());

        List<? extends Player> targets = Bukkit.getOnlinePlayers().stream()
                .filter(target -> !target.getUniqueId().equals(player.getUniqueId()))
                .sorted((a, b) -> a.getName().compareToIgnoreCase(b.getName()))
                .toList();

        int slot = 0;
        for (Player target : targets) {
            if (slot >= 45) {
                break;
            }
            inventory.setItem(slot, createTpaTargetHead(target));
            slot++;
        }
        if (targets.isEmpty()) {
            inventory.setItem(22, createActionItem(Material.BARRIER, "§c보낼 수 있는 대상이 없습니다.", "noop"));
        }
        inventory.setItem(49, createActionItem(Material.BARRIER, "§c뒤로가기", "tpa.back"));

        player.openInventory(inventory);
    }

    public void openBasicJobMenu(Player player) {
        Inventory inventory = createInventory(MenuType.BASIC_JOB, 27, "§8기본 직업 선택");
        fillBorder(inventory);

        inventory.setItem(10, createActionItem(Material.IRON_PICKAXE, "§f광부", "basic.miner"));
        inventory.setItem(12, createActionItem(Material.IRON_SWORD, "§f전사", "basic.warrior"));
        inventory.setItem(14, createActionItem(Material.DIAMOND_SWORD, "§f검사", "basic.swordsman"));
        inventory.setItem(16, createActionItem(Material.FISHING_ROD, "§f낚시꾼", "basic.fisherman"));
        inventory.setItem(22, createActionItem(Material.BARRIER, "§c뒤로가기", "basic.back"));

        player.openInventory(inventory);
    }

    public void handleInventoryClick(InventoryClickEvent event) {
        Inventory topInventory = event.getView().getTopInventory();
        if (!(topInventory.getHolder() instanceof MenuHolder menuHolder)) {
            return;
        }
        event.setCancelled(true);

        if (event.getClickedInventory() == null || !event.getClickedInventory().equals(topInventory)) {
            return;
        }

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType().isAir() || !clicked.hasItemMeta()) {
            return;
        }

        if (menuHolder.type == MenuType.TPA) {
            handleTpaMenuClick(player, clicked);
            return;
        }

        String action = getAction(clicked);
        if (action == null || action.equals("noop")) {
            return;
        }

        switch (action) {
            case "main.teleport" -> openTeleportMenu(player);
            case "main.guild" -> {
                player.closeInventory();
                player.sendMessage("§e[길드] /길드 요청 명령어를 채팅창에 입력하세요.");
                player.sendMessage(Component.text("[클릭해서 입력]", NamedTextColor.AQUA)
                        .clickEvent(ClickEvent.suggestCommand("/길드 요청 ")));
            }
            case "main.remit" -> {
                player.closeInventory();
                player.sendMessage("§e[송금] /송금 <플레이어> <수량>");
                player.sendMessage(Component.text("[클릭해서 입력]", NamedTextColor.GOLD)
                        .clickEvent(ClickEvent.suggestCommand("/송금 ")));
            }
            case "main.basic_job" -> openBasicJobMenu(player);
            case "main.info" -> {
                player.closeInventory();
                sendMyInfo(player);
            }
            case "teleport.spawn" -> {
                player.closeInventory();
                player.performCommand("spawn");
            }
            case "teleport.shop" -> {
                player.closeInventory();
                player.performCommand("상점");
            }
            case "teleport.home" -> {
                player.closeInventory();
                homeManager.teleportHome(player);
            }
            case "teleport.home_set" -> {
                player.closeInventory();
                homeManager.setHome(player);
            }
            case "teleport.tpa" -> openTpaMenu(player);
            case "teleport.rtp" -> {
                player.closeInventory();
                player.performCommand("rtp");
            }
            case "basic.miner" -> {
                player.closeInventory();
                grantBasicJob(player, JobType.MINER);
            }
            case "basic.warrior" -> {
                player.closeInventory();
                grantBasicJob(player, JobType.WARRIOR);
            }
            case "basic.swordsman" -> {
                player.closeInventory();
                grantBasicJob(player, JobType.SWORDSMAN);
            }
            case "basic.fisherman" -> {
                player.closeInventory();
                grantBasicJob(player, JobType.FISHERMAN);
            }
            case "teleport.back", "tpa.back", "basic.back" -> openMainMenu(player);
            default -> {
                // no-op
            }
        }
    }

    public void handleInventoryDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof MenuHolder) {
            event.setCancelled(true);
        }
    }

    private void handleTpaMenuClick(Player player, ItemStack clicked) {
        String action = getAction(clicked);
        if ("tpa.back".equals(action)) {
            openTeleportMenu(player);
            return;
        }
        ItemMeta itemMeta = clicked.getItemMeta();
        String rawTargetId = itemMeta.getPersistentDataContainer().get(tpaTargetKey, PersistentDataType.STRING);
        if (rawTargetId == null) {
            return;
        }
        UUID targetId;
        try {
            targetId = UUID.fromString(rawTargetId);
        } catch (IllegalArgumentException ex) {
            return;
        }
        Player target = Bukkit.getPlayer(targetId);
        if (target == null || !target.isOnline()) {
            player.sendMessage("§c대상 플레이어가 접속 중이 아닙니다.");
            return;
        }
        player.closeInventory();
        player.performCommand("tpa " + target.getName());
    }

    private void grantBasicJob(Player player, JobType jobType) {
        jobManager.setJob(player.getUniqueId(), jobType);
        jobService.applyJobPassivesImmediately(player);
        player.sendMessage("§a기본 직업을 선택했습니다: " + jobType.getDisplayName());
    }

    private void sendMyInfo(Player player) {
        PlayerProfile profile = profileManager.get(player.getUniqueId());
        JobType jobType = jobManager.getJob(player.getUniqueId());

        player.sendMessage("§e내정보");
        player.sendMessage("§f이름 | " + player.getName());
        player.sendMessage("§f직업 | " + jobType.getDisplayName());
        player.sendMessage(
                "§f캔 | " + format(profile.getCan())
                        + " | 실버캔 | " + format(profile.getSilverCan())
                        + " | 황금캔 | " + format(profile.getGoldenCan())
        );
    }

    private String format(long value) {
        return NUMBER_FORMAT.format(Math.max(0L, value));
    }

    private String getAction(ItemStack itemStack) {
        ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta == null) {
            return null;
        }
        return itemMeta.getPersistentDataContainer().get(actionKey, PersistentDataType.STRING);
    }

    private boolean hasMenuCompass(PlayerInventory inventory) {
        for (ItemStack itemStack : inventory.getContents()) {
            if (isMenuCompass(itemStack)) {
                return true;
            }
        }
        return isMenuCompass(inventory.getItemInOffHand());
    }

    private ItemStack createMenuCompass() {
        ItemStack item = new ItemStack(Material.COMPASS);
        ItemMeta itemMeta = Objects.requireNonNull(item.getItemMeta());
        itemMeta.setDisplayName("§e서버 메뉴");
        itemMeta.setLore(List.of("§7우클릭으로 서버 메뉴 열기"));
        itemMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        itemMeta.getPersistentDataContainer().set(compassMarkerKey, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(itemMeta);
        return item;
    }

    private Inventory createInventory(MenuType type, int size, String title) {
        MenuHolder holder = new MenuHolder(type);
        Inventory inventory = Bukkit.createInventory(holder, size, title);
        holder.inventory = inventory;
        return inventory;
    }

    private ItemStack createActionItem(Material material, String name, String action) {
        ItemStack itemStack = new ItemStack(material);
        ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta == null) {
            return itemStack;
        }
        itemMeta.setDisplayName(name);
        itemMeta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, action);
        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }

    private ItemStack createTpaTargetHead(Player target) {
        ItemStack itemStack = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta skullMeta = (SkullMeta) itemStack.getItemMeta();
        if (skullMeta == null) {
            return itemStack;
        }
        skullMeta.setOwningPlayer(target);
        skullMeta.setDisplayName("§e" + target.getName());
        skullMeta.setLore(List.of("§7클릭 시 TPA 요청 전송"));
        PersistentDataContainer dataContainer = skullMeta.getPersistentDataContainer();
        dataContainer.set(tpaTargetKey, PersistentDataType.STRING, target.getUniqueId().toString());
        skullMeta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, "tpa.target");
        itemStack.setItemMeta(skullMeta);
        return itemStack;
    }

    private void fillBorder(Inventory inventory) {
        ItemStack filler = createFiller();
        int size = inventory.getSize();
        int rows = size / 9;
        for (int col = 0; col < 9; col++) {
            inventory.setItem(col, filler);
            inventory.setItem((rows - 1) * 9 + col, filler);
        }
        for (int row = 0; row < rows; row++) {
            inventory.setItem(row * 9, filler);
            inventory.setItem(row * 9 + 8, filler);
        }
    }

    private void fillInventory(Inventory inventory, ItemStack itemStack) {
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, itemStack);
        }
    }

    private ItemStack createFiller() {
        ItemStack itemStack = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta != null) {
            itemMeta.setDisplayName(" ");
            itemMeta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, "noop");
            itemStack.setItemMeta(itemMeta);
        }
        return itemStack;
    }

    private enum MenuType {
        MAIN,
        TELEPORT,
        TPA,
        BASIC_JOB
    }

    private static final class MenuHolder implements InventoryHolder {
        private final MenuType type;
        private Inventory inventory;

        private MenuHolder(MenuType type) {
            this.type = type;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }
}
