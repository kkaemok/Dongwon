package org.kkaemok.dongwon.settings;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.kkaemok.dongwon.text.ConfigText;

import java.util.List;

import static org.kkaemok.dongwon.text.ConfigText.placeholder;

public final class SettingsMenu {
    private static final int SIZE = 45;
    private static final int TPA_GUI_SLOT = 0;
    private static final String ACTION_TPA_GUI = "tpa_gui";

    private final PlayerSettingsManager settingsManager;
    private final ConfigText text;
    private final NamespacedKey actionKey;

    public SettingsMenu(Plugin plugin, PlayerSettingsManager settingsManager, ConfigText text) {
        this.settingsManager = settingsManager;
        this.text = text;
        this.actionKey = new NamespacedKey(plugin, "settings_action");
    }

    public void open(Player player) {
        Holder holder = new Holder();
        Inventory inventory = Bukkit.createInventory(holder, SIZE, text.component("settings-menu.title", "<dark_gray>설정"));
        holder.bind(inventory);
        render(player, inventory);
        player.openInventory(inventory);
    }

    public boolean isMenu(Inventory inventory) {
        return inventory != null && inventory.getHolder() instanceof Holder;
    }

    public void handleClick(InventoryClickEvent event) {
        Inventory topInventory = event.getView().getTopInventory();
        if (!isMenu(topInventory)) {
            return;
        }
        event.setCancelled(true);

        if (event.getClickedInventory() == null || event.getClickedInventory() != topInventory) {
            return;
        }
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) {
            return;
        }
        String action = clicked.getItemMeta().getPersistentDataContainer().get(actionKey, PersistentDataType.STRING);
        if (!ACTION_TPA_GUI.equals(action)) {
            return;
        }

        boolean enabled = settingsManager.toggleTpaGui(player.getUniqueId());
        text.send(player,
                enabled ? "messages.settings.tpa-gui-enabled" : "messages.settings.tpa-gui-disabled",
                enabled ? "<green>TPA 확인 GUI를 켰습니다." : "<red>TPA 확인 GUI를 껐습니다.");
        render(player, topInventory);
    }

    public void handleDrag(InventoryDragEvent event) {
        if (!isMenu(event.getView().getTopInventory())) {
            return;
        }
        int topSize = event.getView().getTopInventory().getSize();
        for (int rawSlot : event.getRawSlots()) {
            if (rawSlot < topSize) {
                event.setCancelled(true);
                return;
            }
        }
    }

    private void render(Player player, Inventory inventory) {
        inventory.setItem(TPA_GUI_SLOT, createTpaGuiItem(player));
    }

    private ItemStack createTpaGuiItem(Player player) {
        boolean enabled = settingsManager.isTpaGuiEnabled(player.getUniqueId());
        String state = enabled
                ? text.string("settings-menu.tpa-gui.state-enabled", "<green>켜짐")
                : text.string("settings-menu.tpa-gui.state-disabled", "<red>꺼짐");

        ItemStack item = new ItemStack(text.material("settings-menu.tpa-gui.material", Material.ENDER_PEARL));
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        meta.displayName(text.component("settings-menu.tpa-gui.name", "<aqua>TPA 확인 GUI: %state%",
                placeholder("state", state)));
        List<Component> lore = text.componentList("settings-menu.tpa-gui.lore",
                List.of("<gray>클릭하면 TPA 확인 GUI를 켜거나 끕니다.", "<gray>현재: %state%"),
                placeholder("state", state));
        if (!lore.isEmpty()) {
            meta.lore(lore);
        }
        meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, ACTION_TPA_GUI);
        item.setItemMeta(meta);
        return item;
    }

    private static final class Holder implements InventoryHolder {
        private Inventory inventory;

        private void bind(Inventory inventory) {
            this.inventory = inventory;
        }

        @Override
        public @NotNull Inventory getInventory() {
            return inventory;
        }
    }
}
