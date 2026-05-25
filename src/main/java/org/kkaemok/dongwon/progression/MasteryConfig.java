package org.kkaemok.dongwon.progression;

import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.kkaemok.dongwon.text.ConfigText;
import org.kkaemok.dongwon.text.MessageConfig;

import java.util.List;

public final class MasteryConfig {
    private final MessageConfig config;
    private final ConfigText text;

    public MasteryConfig(JavaPlugin plugin, ConfigText text) {
        this.config = new MessageConfig(plugin, "mastery.yml");
        this.text = text;
    }

    public void reload() {
        config.reload();
    }

    public void send(CommandSender sender, String path, String fallback, ConfigText.Placeholder... placeholders) {
        String raw = config.getString("messages." + path, fallback);
        if (raw.isBlank()) {
            return;
        }
        sender.sendMessage(text.format(raw, placeholders));
    }

    public void sendLines(CommandSender sender, String path, List<String> fallback, ConfigText.Placeholder... placeholders) {
        List<String> rawLines = config.getStringList("messages." + path);
        if (rawLines.isEmpty()) {
            rawLines = fallback;
        }
        for (String rawLine : rawLines) {
            if (!rawLine.isBlank()) {
                sender.sendMessage(text.format(rawLine, placeholders));
            }
        }
    }
}
