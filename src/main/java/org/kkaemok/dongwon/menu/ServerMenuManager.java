package org.kkaemok.dongwon.menu;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
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
import org.jetbrains.annotations.NotNull;
import org.kkaemok.dongwon.home.HomeManager;
import org.kkaemok.dongwon.job.JobManager;
import org.kkaemok.dongwon.job.JobService;
import org.kkaemok.dongwon.job.JobType;
import org.kkaemok.dongwon.progression.PlayerProfile;
import org.kkaemok.dongwon.progression.ProfileManager;
import org.kkaemok.dongwon.storage.StorageManager;
import org.kkaemok.dongwon.text.ConfigText;

import java.text.DecimalFormat;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static org.kkaemok.dongwon.text.ConfigText.placeholder;

public final class ServerMenuManager {
    private static final DecimalFormat NUMBER_FORMAT = new DecimalFormat("#,###");

    private final JobManager jobManager;
    private final JobService jobService;
    private final ProfileManager profileManager;
    private final HomeManager homeManager;
    private final StorageManager storageManager;
    private final ConfigText text;
    private final NamespacedKey compassMarkerKey;
    private final NamespacedKey actionKey;
    private final NamespacedKey tpaTargetKey;

    public ServerMenuManager(
            Plugin plugin,
            JobManager jobManager,
            JobService jobService,
            ProfileManager profileManager,
            HomeManager homeManager,
            StorageManager storageManager,
            ConfigText text
    ) {
        this.jobManager = jobManager;
        this.jobService = jobService;
        this.profileManager = profileManager;
        this.homeManager = homeManager;
        this.storageManager = storageManager;
        this.text = text;
        this.compassMarkerKey = new NamespacedKey(plugin, "server_menu_compass");
        this.actionKey = new NamespacedKey(plugin, "server_menu_action");
        this.tpaTargetKey = new NamespacedKey(plugin, "server_menu_tpa_target");
    }

    public void giveCompassIfMissing(Player player) {
        giveCompassIfMissing(player, false);
    }

    public void giveCompassIfMissing(Player player, boolean notify) {
        if (hasMenuCompass(player.getInventory())) {
            if (notify) {
                text.send(player, "messages.menu.compass.already-has", "<yellow>이미 서버 메뉴 버튼을 가지고 있습니다.");
            }
            return;
        }

        int slot = player.getInventory().firstEmpty();
        if (slot < 0) {
            if (notify) {
                text.send(player, "messages.menu.compass.inventory-full", "<red>인벤토리에 빈 칸이 없어 서버 메뉴 버튼을 지급할 수 없습니다.");
            }
            return;
        }

        ItemStack compass = createMenuCompass();
        player.getInventory().setItem(slot, compass);
        if (notify) {
            text.send(player, "messages.menu.compass.given", "<green>서버 메뉴 버튼을 지급했습니다.");
        }
    }

    public boolean isMenuCompass(ItemStack itemStack) {
        if (itemStack == null || itemStack.getType().isAir() || !itemStack.hasItemMeta()) {
            return false;
        }
        ItemMeta itemMeta = itemStack.getItemMeta();
        return itemMeta.getPersistentDataContainer().has(compassMarkerKey, PersistentDataType.BYTE);
    }

    public void sendCompassDropBlocked(Player player) {
        text.send(player, "messages.menu.compass.drop-blocked", "<red>서버 메뉴 버튼은 버릴 수 없습니다.");
    }

    public void openMainMenu(Player player) {
        Inventory inventory = createInventory(MenuType.MAIN, 36,
                text.component("menus.main.title", "<dark_gray>서버 메뉴"));
        fillBorder(inventory);

        inventory.setItem(10, createActionItem("menus.main.buttons.teleport", Material.ENDER_PEARL, "<aqua>-텔레포트-", "main.teleport"));
        inventory.setItem(11, createActionItem("menus.main.buttons.guild", Material.WHITE_BANNER, "<green>-길드-", "main.guild"));
        inventory.setItem(12, createActionItem("menus.main.buttons.party", Material.AMETHYST_SHARD, "<light_purple>-파티-", "main.party"));
        inventory.setItem(13, createActionItem("menus.main.buttons.remit", Material.GOLD_INGOT, "<yellow>송금", "main.remit"));
        inventory.setItem(14, createActionItem("menus.main.buttons.basic-job", Material.IRON_SWORD, "<white>기본 직업 획득", "main.basic_job"));
        inventory.setItem(15, createActionItem("menus.main.buttons.storage", Material.CHEST, "<gold>창고", "main.storage"));
        inventory.setItem(16, createActionItem("menus.main.buttons.land", Material.GRASS_BLOCK, "<green>땅설정", "main.land"));
        inventory.setItem(22, createActionItem("menus.main.buttons.info", Material.BOOK, "<light_purple>-내정보-", "main.info"));

        player.openInventory(inventory);
    }

    public void openTeleportMenu(Player player) {
        Inventory inventory = createInventory(MenuType.TELEPORT, 27,
                text.component("menus.teleport.title", "<dark_gray>텔레포트 메뉴"));
        fillBorder(inventory);

        inventory.setItem(10, createActionItem("menus.teleport.buttons.spawn", Material.LODESTONE, "<white>스폰", "teleport.spawn"));
        inventory.setItem(11, createActionItem("menus.teleport.buttons.shop", Material.EMERALD, "<green>상점", "teleport.shop"));
        inventory.setItem(12, createActionItem("menus.teleport.buttons.home", Material.OAK_DOOR, "<yellow>홈", "teleport.home"));
        inventory.setItem(13, createActionItem("menus.teleport.buttons.set-home", Material.COMPASS, "<gold>홈 설정", "teleport.home_set"));
        inventory.setItem(14, createActionItem("menus.teleport.buttons.tpa", Material.PLAYER_HEAD, "<aqua>TPA", "teleport.tpa"));
        inventory.setItem(15, createActionItem("menus.teleport.buttons.rtp", Material.ENDER_EYE, "<light_purple>RTP", "teleport.rtp"));
        inventory.setItem(22, createActionItem("menus.shared.back", Material.BARRIER, "<red>뒤로가기", "teleport.back"));

        player.openInventory(inventory);
    }

    public void openTpaMenu(Player player) {
        Inventory inventory = createInventory(MenuType.TPA, 54,
                text.component("menus.tpa.title", "<dark_gray>TPA 대상 선택"));
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
            inventory.setItem(22, createActionItem("menus.tpa.buttons.empty", Material.BARRIER, "<red>보낼 수 있는 대상이 없습니다.", "noop"));
        }
        inventory.setItem(49, createActionItem("menus.shared.back", Material.BARRIER, "<red>뒤로가기", "tpa.back"));

        player.openInventory(inventory);
    }

    public void openBasicJobMenu(Player player) {
        Inventory inventory = createInventory(MenuType.BASIC_JOB, 27,
                text.component("menus.basic-job.title", "<dark_gray>기본 직업 선택"));
        fillBorder(inventory);

        inventory.setItem(10, createActionItem("menus.basic-job.buttons.miner", Material.IRON_PICKAXE, "<white>광부", "basic.miner"));
        inventory.setItem(11, createActionItem("menus.basic-job.buttons.farmer", Material.WHEAT, "<green>농부", "basic.farmer"));
        inventory.setItem(12, createActionItem("menus.basic-job.buttons.warrior", Material.IRON_SWORD, "<white>전사", "basic.warrior"));
        inventory.setItem(13, createActionItem("menus.basic-job.buttons.buffer", Material.NETHER_STAR, "<yellow>버퍼", "basic.buffer"));
        inventory.setItem(14, createActionItem("menus.basic-job.buttons.swordsman", Material.DIAMOND_SWORD, "<white>검사", "basic.swordsman"));
        inventory.setItem(15, createActionItem("menus.basic-job.buttons.healer", Material.GLISTERING_MELON_SLICE, "<green>힐러", "basic.healer"));
        inventory.setItem(16, createActionItem("menus.basic-job.buttons.fisherman", Material.FISHING_ROD, "<white>낚시꾼", "basic.fisherman"));
        inventory.setItem(22, createActionItem("menus.shared.back", Material.BARRIER, "<red>뒤로가기", "basic.back"));

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
                text.send(player, "messages.menu.guild.prompt", "<yellow>[길드] /길드 요청 명령어를 채팅창에 입력하세요.");
                player.sendMessage(text.component("messages.menu.guild.click", "<aqua>[클릭해서 입력]")
                        .clickEvent(ClickEvent.suggestCommand("/길드 요청 ")));
            }
            case "main.party" -> {
                player.closeInventory();
                text.send(player, "messages.menu.party.prompt", "<yellow>[파티] /파티 생성 또는 /파티 초대 <플레이어>를 입력하세요.");
                player.sendMessage(text.component("messages.menu.party.click-create", "<aqua>[파티 생성]")
                        .clickEvent(ClickEvent.suggestCommand("/파티 생성")));
                player.sendMessage(text.component("messages.menu.party.click-invite", "<light_purple>[파티 초대 입력]")
                        .clickEvent(ClickEvent.suggestCommand("/파티 초대 ")));
            }
            case "main.remit" -> {
                player.closeInventory();
                text.send(player, "messages.menu.remit.prompt", "<yellow>[송금] /송금 \\<플레이어> \\<수량>");
                player.sendMessage(text.component("messages.menu.remit.click", "<gold>[클릭해서 입력]")
                        .clickEvent(ClickEvent.suggestCommand("/송금 ")));
            }
            case "main.basic_job" -> openBasicJobMenu(player);
            case "main.storage" -> storageManager.open(player);
            case "main.land" -> {
                player.closeInventory();
                text.send(player, "messages.menu.land.prompt", "<yellow>[땅설정] 현재 위치를 중심으로 땅을 만들려면 /땅설정 <땅이름>");
                player.sendMessage(text.component("messages.menu.land.click-create", "<green>[땅 생성 입력]")
                        .clickEvent(ClickEvent.suggestCommand("/땅설정 ")));
                player.sendMessage(text.component("messages.menu.land.click-list", "<aqua>[내 땅 목록]")
                        .clickEvent(ClickEvent.runCommand("/땅목록")));
            }
            case "main.info" -> {
                player.closeInventory();
                sendMyInfo(player);
            }
            case "teleport.spawn" -> {
                player.closeInventory();
                player.performCommand(text.string("settings.commands.spawn", "spawn"));
            }
            case "teleport.shop" -> {
                player.closeInventory();
                player.performCommand(text.string("settings.commands.shop", "상점"));
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
            case "basic.farmer" -> {
                player.closeInventory();
                grantBasicJob(player, JobType.FARMER);
            }
            case "basic.warrior" -> {
                player.closeInventory();
                grantBasicJob(player, JobType.WARRIOR);
            }
            case "basic.buffer" -> {
                player.closeInventory();
                grantBasicJob(player, JobType.BUFFER);
            }
            case "basic.swordsman" -> {
                player.closeInventory();
                grantBasicJob(player, JobType.SWORDSMAN);
            }
            case "basic.healer" -> {
                player.closeInventory();
                grantBasicJob(player, JobType.HEALER);
            }
            case "basic.fisherman" -> {
                player.closeInventory();
                grantBasicJob(player, JobType.FISHERMAN);
            }
            case "teleport.back", "tpa.back", "basic.back" -> openMainMenu(player);
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
            text.send(player, "messages.menu.tpa-target-offline", "<red>대상 플레이어가 접속 중이 아닙니다.");
            return;
        }
        player.closeInventory();
        player.performCommand("tpa " + target.getName());
    }

    private void grantBasicJob(Player player, JobType jobType) {
        jobManager.setJob(player.getUniqueId(), jobType);
        jobService.applyJobPassivesImmediately(player);
        text.send(player, "messages.menu.basic-job-selected", "<green>기본 직업을 선택했습니다: %job%",
                placeholder("job", jobType.getDisplayName()));
    }

    private void sendMyInfo(Player player) {
        PlayerProfile profile = profileManager.get(player.getUniqueId());
        JobType jobType = jobManager.getJob(player.getUniqueId());

        text.send(player, "messages.menu.info.header", "<yellow>내정보");
        text.send(player, "messages.menu.info.name", "<white>이름 | %player%",
                placeholder("player", player.getName()));
        text.send(player, "messages.menu.info.job", "<white>직업 | %job%",
                placeholder("job", jobType.getDisplayName()));
        text.send(player, "messages.menu.info.currency",
                "<white>캔 | %can% | 실버캔 | %silver_can% | 황금캔 | %golden_can%",
                placeholder("can", format(profile.getCan())),
                placeholder("silver_can", format(profile.getSilverCan())),
                placeholder("golden_can", format(profile.getGoldenCan())));
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
        ItemStack item = new ItemStack(getMenuCompassMaterial());
        ItemMeta itemMeta = Objects.requireNonNull(item.getItemMeta());
        itemMeta.displayName(text.component("menus.compass.name", "<yellow>서버 메뉴"));
        itemMeta.lore(text.componentList("menus.compass.lore", List.of("<gray>우클릭으로 서버 메뉴 열기")));
        itemMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        itemMeta.getPersistentDataContainer().set(compassMarkerKey, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(itemMeta);
        return item;
    }

    private Material getMenuCompassMaterial() {
        return text.material("menus.compass.material", Material.COMPASS);
    }

    private Inventory createInventory(MenuType type, int size, Component title) {
        MenuHolder holder = new MenuHolder(type);
        Inventory inventory = Bukkit.createInventory(holder, size, title);
        holder.inventory = inventory;
        return inventory;
    }

    private ItemStack createActionItem(String configPath, Material fallbackMaterial, String fallbackName, String action) {
        Material material = text.material(configPath + ".material", fallbackMaterial);
        ItemStack itemStack = new ItemStack(material);
        ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta == null) {
            return itemStack;
        }
        itemMeta.displayName(text.component(configPath + ".name", fallbackName));
        List<Component> lore = text.componentList(configPath + ".lore", List.of());
        if (!lore.isEmpty()) {
            itemMeta.lore(lore);
        }
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
        skullMeta.displayName(text.component("menus.tpa.target.name", "<yellow>%player%",
                placeholder("player", target.getName())));
        skullMeta.lore(text.componentList("menus.tpa.target.lore",
                List.of("<gray>클릭 시 TPA 요청 전송"),
                placeholder("player", target.getName())));
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
        ItemStack itemStack = new ItemStack(text.material("menus.filler.material", Material.GRAY_STAINED_GLASS_PANE));
        ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta != null) {
            itemMeta.displayName(text.component("menus.filler.name", " "));
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
        public @NotNull Inventory getInventory() {
            return inventory;
        }
    }
}
