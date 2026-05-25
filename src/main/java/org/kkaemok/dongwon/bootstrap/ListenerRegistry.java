package org.kkaemok.dongwon.bootstrap;

import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.kkaemok.dongwon.board.BoardListener;
import org.kkaemok.dongwon.board.BoardService;
import org.kkaemok.dongwon.guild.GuildJoinNotifyListener;
import org.kkaemok.dongwon.guild.GuildManager;
import org.kkaemok.dongwon.job.JobListener;
import org.kkaemok.dongwon.job.JobManager;
import org.kkaemok.dongwon.job.JobService;
import org.kkaemok.dongwon.land.LandListener;
import org.kkaemok.dongwon.land.LandManager;
import org.kkaemok.dongwon.level.LevelListener;
import org.kkaemok.dongwon.level.LevelService;
import org.kkaemok.dongwon.menu.ServerMenuListener;
import org.kkaemok.dongwon.menu.ServerMenuManager;
import org.kkaemok.dongwon.party.PartyListener;
import org.kkaemok.dongwon.party.PartyManager;
import org.kkaemok.dongwon.progression.JinSwordsmanService;
import org.kkaemok.dongwon.progression.JinSwordsmanListener;
import org.kkaemok.dongwon.progression.MasteryListener;
import org.kkaemok.dongwon.progression.MasteryService;
import org.kkaemok.dongwon.progression.ProfileManager;
import org.kkaemok.dongwon.player.PlayerJoinListener;
import org.kkaemok.dongwon.progression.ShihyeonryuListener;
import org.kkaemok.dongwon.progression.ShihyeonryuService;
import org.kkaemok.dongwon.rtp.RtpListener;
import org.kkaemok.dongwon.rtp.RtpManager;
import org.kkaemok.dongwon.settings.SettingsListener;
import org.kkaemok.dongwon.settings.SettingsMenu;
import org.kkaemok.dongwon.storage.StorageListener;
import org.kkaemok.dongwon.storage.StorageManager;
import org.kkaemok.dongwon.tpa.TpaConfig;
import org.kkaemok.dongwon.tpa.TpaListener;
import org.kkaemok.dongwon.tpa.TpaMenu;
import org.kkaemok.dongwon.tpa.TpaManager;

public final class ListenerRegistry {
    private final JavaPlugin plugin;
    private final JobManager jobManager;
    private final JobService jobService;
    private final ProfileManager profileManager;
    private final MasteryService masteryService;
    private final JinSwordsmanService jinSwordsmanService;
    private final ShihyeonryuService shihyeonryuService;
    private final BoardService boardService;
    private final GuildManager guildManager;
    private final ServerMenuManager serverMenuManager;
    private final LandManager landManager;
    private final LevelService levelService;
    private final PartyManager partyManager;
    private final SettingsMenu settingsMenu;
    private final TpaManager tpaManager;
    private final TpaConfig tpaConfig;
    private final TpaMenu tpaMenu;
    private final RtpManager rtpManager;
    private final StorageManager storageManager;

    public ListenerRegistry(
            JavaPlugin plugin,
            JobManager jobManager,
            JobService jobService,
            ProfileManager profileManager,
            MasteryService masteryService,
            JinSwordsmanService jinSwordsmanService,
            ShihyeonryuService shihyeonryuService,
            BoardService boardService,
            GuildManager guildManager,
            ServerMenuManager serverMenuManager,
            LandManager landManager,
            LevelService levelService,
            PartyManager partyManager,
            SettingsMenu settingsMenu,
            TpaManager tpaManager,
            TpaConfig tpaConfig,
            TpaMenu tpaMenu,
            RtpManager rtpManager,
            StorageManager storageManager
    ) {
        this.plugin = plugin;
        this.jobManager = jobManager;
        this.jobService = jobService;
        this.profileManager = profileManager;
        this.masteryService = masteryService;
        this.jinSwordsmanService = jinSwordsmanService;
        this.shihyeonryuService = shihyeonryuService;
        this.boardService = boardService;
        this.guildManager = guildManager;
        this.serverMenuManager = serverMenuManager;
        this.landManager = landManager;
        this.levelService = levelService;
        this.partyManager = partyManager;
        this.settingsMenu = settingsMenu;
        this.tpaManager = tpaManager;
        this.tpaConfig = tpaConfig;
        this.tpaMenu = tpaMenu;
        this.rtpManager = rtpManager;
        this.storageManager = storageManager;
    }

    public void registerAll() {
        PluginManager pluginManager = plugin.getServer().getPluginManager();
        pluginManager.registerEvents(new JobListener(jobService, masteryService), plugin);
        pluginManager.registerEvents(new PlayerJoinListener(plugin, jobManager, jobService, profileManager), plugin);
        pluginManager.registerEvents(new MasteryListener(masteryService), plugin);
        pluginManager.registerEvents(new LevelListener(levelService, partyManager), plugin);
        pluginManager.registerEvents(new JinSwordsmanListener(jinSwordsmanService), plugin);
        pluginManager.registerEvents(new ShihyeonryuListener(shihyeonryuService), plugin);
        pluginManager.registerEvents(new BoardListener(boardService), plugin);
        pluginManager.registerEvents(new GuildJoinNotifyListener(guildManager), plugin);
        pluginManager.registerEvents(new ServerMenuListener(plugin, serverMenuManager), plugin);
        pluginManager.registerEvents(new LandListener(landManager), plugin);
        pluginManager.registerEvents(new PartyListener(partyManager), plugin);
        pluginManager.registerEvents(new SettingsListener(settingsMenu), plugin);
        pluginManager.registerEvents(new TpaListener(tpaManager, tpaConfig, tpaMenu), plugin);
        pluginManager.registerEvents(new RtpListener(rtpManager), plugin);
        pluginManager.registerEvents(new StorageListener(storageManager), plugin);
    }
}
