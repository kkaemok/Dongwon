package org.kkaemok.dongwon.bootstrap;

import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.java.JavaPlugin;
import org.kkaemok.dongwon.admin.DongwonCommand;
import org.kkaemok.dongwon.economy.RemitCommand;
import org.kkaemok.dongwon.guild.GuildCommand;
import org.kkaemok.dongwon.guild.GuildManager;
import org.kkaemok.dongwon.job.JobCommand;
import org.kkaemok.dongwon.job.JobManager;
import org.kkaemok.dongwon.job.JobService;
import org.kkaemok.dongwon.land.LandCommand;
import org.kkaemok.dongwon.land.LandManager;
import org.kkaemok.dongwon.menu.MenuGiveCommand;
import org.kkaemok.dongwon.menu.ServerMenuCommand;
import org.kkaemok.dongwon.menu.ServerMenuManager;
import org.kkaemok.dongwon.party.PartyCommand;
import org.kkaemok.dongwon.party.PartyManager;
import org.kkaemok.dongwon.progression.MasteryCommand;
import org.kkaemok.dongwon.progression.MasteryService;
import org.kkaemok.dongwon.progression.ProfileManager;
import org.kkaemok.dongwon.rtp.RtpCommand;
import org.kkaemok.dongwon.rtp.RtpManager;
import org.kkaemok.dongwon.settings.PlayerSettingsManager;
import org.kkaemok.dongwon.settings.SettingsCommand;
import org.kkaemok.dongwon.settings.SettingsMenu;
import org.kkaemok.dongwon.storage.StorageCommand;
import org.kkaemok.dongwon.storage.StorageManager;
import org.kkaemok.dongwon.text.ConfigText;
import org.kkaemok.dongwon.tpa.TpaConfig;
import org.kkaemok.dongwon.tpa.TpaCommand;
import org.kkaemok.dongwon.tpa.TpaMenu;
import org.kkaemok.dongwon.tpa.TpaManager;

public final class CommandRegistry {
    private final JavaPlugin plugin;
    private final JobManager jobManager;
    private final JobService jobService;
    private final ProfileManager profileManager;
    private final GuildManager guildManager;
    private final MasteryService masteryService;
    private final ServerMenuManager serverMenuManager;
    private final LandManager landManager;
    private final PartyManager partyManager;
    private final ConfigText landText;
    private final ConfigText partyText;
    private final SettingsMenu settingsMenu;
    private final PlayerSettingsManager playerSettingsManager;
    private final TpaConfig tpaConfig;
    private final TpaMenu tpaMenu;
    private final TpaManager tpaManager;
    private final RtpManager rtpManager;
    private final StorageManager storageManager;
    private final Runnable reloadAction;
    private final ConfigText text;

    public CommandRegistry(
            JavaPlugin plugin,
            JobManager jobManager,
            JobService jobService,
            ProfileManager profileManager,
            GuildManager guildManager,
            MasteryService masteryService,
            ServerMenuManager serverMenuManager,
            LandManager landManager,
            PartyManager partyManager,
            ConfigText landText,
            ConfigText partyText,
            SettingsMenu settingsMenu,
            PlayerSettingsManager playerSettingsManager,
            TpaConfig tpaConfig,
            TpaMenu tpaMenu,
            TpaManager tpaManager,
            RtpManager rtpManager,
            StorageManager storageManager,
            Runnable reloadAction,
            ConfigText text
    ) {
        this.plugin = plugin;
        this.jobManager = jobManager;
        this.jobService = jobService;
        this.profileManager = profileManager;
        this.guildManager = guildManager;
        this.masteryService = masteryService;
        this.serverMenuManager = serverMenuManager;
        this.landManager = landManager;
        this.partyManager = partyManager;
        this.landText = landText;
        this.partyText = partyText;
        this.settingsMenu = settingsMenu;
        this.playerSettingsManager = playerSettingsManager;
        this.tpaConfig = tpaConfig;
        this.tpaMenu = tpaMenu;
        this.tpaManager = tpaManager;
        this.rtpManager = rtpManager;
        this.storageManager = storageManager;
        this.reloadAction = reloadAction;
        this.text = text;
    }

    public void registerAll() {
        JobCommand jobCommand = new JobCommand(jobManager, jobService);
        GuildCommand guildCommand = new GuildCommand(guildManager);
        DongwonCommand dongwonCommand = new DongwonCommand(jobManager, jobService, profileManager, guildManager, reloadAction);
        RemitCommand remitCommand = new RemitCommand(profileManager);
        TpaCommand tpaCommand = new TpaCommand(tpaManager, tpaConfig, tpaMenu, playerSettingsManager);
        PartyCommand partyCommand = new PartyCommand(partyManager, partyText);
        LandCommand landCommand = new LandCommand(landManager, landText);

        register("직업", jobCommand, jobCommand);
        register("직업설정", jobCommand, jobCommand);
        register("숙련도", new MasteryCommand(masteryService), null);
        register("길드", guildCommand, guildCommand);
        register("동원", dongwonCommand, dongwonCommand);
        register("메뉴", new ServerMenuCommand(serverMenuManager, text), null);
        register("메뉴지급", new MenuGiveCommand(serverMenuManager, text), null);
        register("설정", new SettingsCommand(settingsMenu, text), null);
        register("송금", remitCommand, remitCommand);
        register("파티", partyCommand, partyCommand);
        register("땅설정", landCommand, landCommand);
        register("땅경계", landCommand, landCommand);
        register("땅삭제", landCommand, landCommand);
        register("땅추방", landCommand, landCommand);
        register("허락요청", landCommand, landCommand);
        register("허락수락", landCommand, landCommand);
        register("땅양도", landCommand, landCommand);
        register("땅목록", landCommand, landCommand);
        register("땅정보", landCommand, landCommand);
        register("tpa", tpaCommand, tpaCommand);
        register("rtp", new RtpCommand(rtpManager, text), null);
        register("창고", new StorageCommand(storageManager, text), null);
    }

    private void register(String name, CommandExecutor executor, TabCompleter completer) {
        PluginCommand command = plugin.getCommand(name);
        if (command == null) {
            throw new IllegalStateException("Missing command in plugin.yml: " + name);
        }
        command.setExecutor(executor);
        if (completer != null) {
            command.setTabCompleter(completer);
        }
    }
}
