package org.kkaemok.dongwon.guild;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.kkaemok.dongwon.text.ConfigText;
import org.kkaemok.dongwon.text.MessageConfig;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public final class GuildConfig {
    private static final Pattern DEFAULT_NAME_PATTERN = Pattern.compile("^[A-Za-z0-9가-힣_]{2,12}$");
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    private final MessageConfig config;
    private final ConfigText text;

    public GuildConfig(JavaPlugin plugin) {
        this.config = new MessageConfig(plugin, "guildconfig.yml");
        this.text = new ConfigText(config);
    }

    public void reload() {
        config.reload();
    }

    public Pattern namePattern() {
        String raw = config.getString("settings.name-pattern", DEFAULT_NAME_PATTERN.pattern());
        try {
            return Pattern.compile(raw);
        } catch (PatternSyntaxException ex) {
            return DEFAULT_NAME_PATTERN;
        }
    }

    public String luckPermsGroupPrefix() {
        return config.getString("settings.luckperms-group-prefix", "dw_guild_");
    }

    public int suffixPriority() {
        return config.getInt("settings.suffix-priority", 50);
    }

    public String defaultGuildName() {
        return config.getString("settings.default-guild-name", "미가입");
    }

    public String message(String key, String fallback) {
        return config.getString("messages." + key, fallback);
    }

    public String string(String key, String fallback, ConfigText.Placeholder... placeholders) {
        return text.string("messages." + key, fallback, placeholders);
    }

    public String legacyString(String key, String fallback, ConfigText.Placeholder... placeholders) {
        return LEGACY.serialize(component(key, fallback, placeholders));
    }

    public Component component(String key, String fallback, ConfigText.Placeholder... placeholders) {
        return text.component("messages." + key, fallback, placeholders);
    }

    public Component format(String raw, ConfigText.Placeholder... placeholders) {
        return text.format(raw, placeholders);
    }

    public void send(CommandSender sender, String key, String fallback, ConfigText.Placeholder... placeholders) {
        text.send(sender, "messages." + key, fallback, placeholders);
    }
}
