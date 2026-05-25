package org.kkaemok.dongwon.board;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.plugin.java.JavaPlugin;
import org.kkaemok.dongwon.text.ConfigText;
import org.kkaemok.dongwon.text.MessageConfig;

import java.util.ArrayList;
import java.util.List;

public final class FastBoardConfig {
    private final MessageConfig config;
    private final ConfigText text;

    public FastBoardConfig(JavaPlugin plugin) {
        this.config = new MessageConfig(plugin, "fastboard.yml");
        this.text = new ConfigText(config);
    }

    public void reload() {
        config.reload();
    }

    public long updateIntervalTicks() {
        return Math.max(1L, config.getLong("settings.update-interval-ticks", 20L));
    }

    public String title(ConfigText.Placeholder... placeholders) {
        return legacy(text.component("title", "&6&lDongwon", placeholders));
    }

    public List<String> lines(ConfigText.Placeholder... placeholders) {
        return lines(List.of(), placeholders);
    }

    public List<String> lines(List<String> partyMemberLines, ConfigText.Placeholder... placeholders) {
        List<String> rawLines = config.getStringList("lines");
        if (rawLines.isEmpty()) {
            rawLines = List.of(
                    "&7이름 &f%player%",
                    "&7직업 &f%job%",
                    "&7레벨 &aLv.%level% &7(%level_exp%)",
                    "&7길드 &f%guild%",
                    "%party_members%",
                    "",
                    "&6황금캔 &f%golden_can%",
                    "&7실버캔 &f%silver_can%",
                    "&f캔 &f%can%",
                    "",
                    "&a플레이타임",
                    "&f%playtime%"
            );
        }
        List<String> lines = new ArrayList<>();
        for (String rawLine : rawLines) {
            if (rawLine.contains("%party_members%")) {
                if (partyMemberLines.isEmpty()) {
                    continue;
                }
                for (String partyLine : partyMemberLines) {
                    lines.add(legacy(text.format(rawLine.replace("%party_members%", partyLine), placeholders)));
                }
                continue;
            }
            lines.add(legacy(text.format(rawLine, placeholders)));
        }
        return lines;
    }

    private String legacy(net.kyori.adventure.text.Component component) {
        return LegacyComponentSerializer.legacySection().serialize(component);
    }
}
