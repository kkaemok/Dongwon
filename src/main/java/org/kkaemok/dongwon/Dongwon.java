package org.kkaemok.dongwon;

import org.bukkit.plugin.java.JavaPlugin;
import org.kkaemok.dongwon.board.FastBoardConfig;
import org.kkaemok.dongwon.board.BoardService;
import org.kkaemok.dongwon.bootstrap.CommandRegistry;
import org.kkaemok.dongwon.bootstrap.ListenerRegistry;
import org.kkaemok.dongwon.guild.GuildManager;
import org.kkaemok.dongwon.home.HomeManager;
import org.kkaemok.dongwon.job.JobManager;
import org.kkaemok.dongwon.job.JobService;
import org.kkaemok.dongwon.land.LandManager;
import org.kkaemok.dongwon.level.BossBarConfig;
import org.kkaemok.dongwon.level.LevelBossBarManager;
import org.kkaemok.dongwon.level.LevelService;
import org.kkaemok.dongwon.menu.ServerMenuManager;
import org.kkaemok.dongwon.party.PartyManager;
import org.kkaemok.dongwon.progression.JinSwordsmanService;
import org.kkaemok.dongwon.progression.MasteryConfig;
import org.kkaemok.dongwon.progression.MasteryService;
import org.kkaemok.dongwon.progression.ProfileManager;
import org.kkaemok.dongwon.progression.ShihyeonryuService;
import org.kkaemok.dongwon.rtp.RtpConfig;
import org.kkaemok.dongwon.rtp.RtpManager;
import org.kkaemok.dongwon.settings.PlayerSettingsManager;
import org.kkaemok.dongwon.settings.SettingsMenu;
import org.kkaemok.dongwon.storage.StorageConfig;
import org.kkaemok.dongwon.storage.StorageManager;
import org.kkaemok.dongwon.text.ConfigText;
import org.kkaemok.dongwon.text.MessageConfig;
import org.kkaemok.dongwon.text.YamlDefaultsUpdater;
import org.kkaemok.dongwon.tpa.TpaConfig;
import org.kkaemok.dongwon.tpa.TpaMenu;
import org.kkaemok.dongwon.tpa.TpaManager;

public final class Dongwon extends JavaPlugin {
    private MessageConfig menuConfig;
    private MessageConfig partyConfig;
    private MessageConfig landConfig;
    private MasteryConfig masteryConfig;
    private BossBarConfig bossBarConfig;
    private FastBoardConfig fastBoardConfig;
    private RtpConfig rtpConfig;
    private TpaConfig tpaConfig;
    private StorageConfig storageConfig;
    private JobManager jobManager;
    private JobService jobService;
    private MasteryService masteryService;
    private BoardService boardService;
    private GuildManager guildManager;
    private HomeManager homeManager;
    private LandManager landManager;
    private LevelService levelService;
    private PartyManager partyManager;
    private PlayerSettingsManager playerSettingsManager;
    private RtpManager rtpManager;
    private TpaMenu tpaMenu;
    private TpaManager tpaManager;
    private StorageManager storageManager;

    @Override
    public void onEnable() {
        YamlDefaultsUpdater.ensureDefaults(this, "config.yml");
        reloadConfig();

        ConfigText text = new ConfigText(this);
        ProfileManager profileManager = new ProfileManager(this);
        this.jobManager = new JobManager(this, profileManager);
        this.menuConfig = new MessageConfig(this, "menu.yml");
        ConfigText menuText = new ConfigText(menuConfig);
        this.partyConfig = new MessageConfig(this, "party.yml");
        ConfigText partyText = new ConfigText(partyConfig);
        this.landConfig = new MessageConfig(this, "land.yml");
        ConfigText landText = new ConfigText(landConfig);
        profileManager.migrateTpaGuiSettings(new java.io.File(getDataFolder(), "player-settings.yml"));
        this.guildManager = new GuildManager(this, profileManager);
        this.masteryConfig = new MasteryConfig(this, text);
        this.masteryService = new MasteryService(profileManager, jobManager, masteryConfig);
        this.bossBarConfig = new BossBarConfig(this, text);
        LevelBossBarManager levelBossBarManager = new LevelBossBarManager(this, bossBarConfig);
        this.levelService = new LevelService(profileManager, bossBarConfig, levelBossBarManager);
        this.partyManager = new PartyManager(partyText);
        this.jobService = new JobService(this, jobManager, partyManager);
        this.landManager = new LandManager(this, landText, profileManager, landConfig);
        this.playerSettingsManager = new PlayerSettingsManager(
                profileManager,
                menuConfig.getBoolean("settings.defaults.tpa-gui", true)
        );

        JinSwordsmanService jinSwordsmanService = new JinSwordsmanService(this, masteryService, masteryConfig);
        ShihyeonryuService shihyeonryuService = new ShihyeonryuService(this, masteryService, masteryConfig);
        this.fastBoardConfig = new FastBoardConfig(this);
        this.boardService = new BoardService(this, fastBoardConfig, jobManager, profileManager, partyManager);
        this.homeManager = new HomeManager(this, menuText);
        this.storageConfig = new StorageConfig(this);
        this.storageManager = new StorageManager(this, storageConfig);
        ServerMenuManager serverMenuManager = new ServerMenuManager(
                this,
                jobManager,
                jobService,
                profileManager,
                homeManager,
                storageManager,
                menuText
        );

        this.rtpConfig = new RtpConfig(this, text);
        this.rtpManager = new RtpManager(this, rtpConfig);
        this.tpaConfig = new TpaConfig(this, text);
        this.tpaMenu = new TpaMenu(this, tpaConfig);
        this.tpaManager = new TpaManager(tpaConfig, tpaMenu, playerSettingsManager);
        SettingsMenu settingsMenu = new SettingsMenu(this, playerSettingsManager, menuText);

        new ListenerRegistry(
                this,
                jobManager,
                jobService,
                profileManager,
                masteryService,
                jinSwordsmanService,
                shihyeonryuService,
                boardService,
                guildManager,
                serverMenuManager,
                landManager,
                levelService,
                partyManager,
                settingsMenu,
                tpaManager,
                tpaConfig,
                tpaMenu,
                rtpManager,
                storageManager
        ).registerAll();

        new CommandRegistry(
                this,
                jobManager,
                jobService,
                profileManager,
                guildManager,
                masteryService,
                serverMenuManager,
                landManager,
                partyManager,
                landText,
                partyText,
                settingsMenu,
                playerSettingsManager,
                tpaConfig,
                tpaMenu,
                tpaManager,
                rtpManager,
                storageManager,
                this::reloadDongwonConfigs,
                menuText
        ).registerAll();

        jobService.start();
        boardService.start();
    }

    public void reloadDongwonConfigs() {
        YamlDefaultsUpdater.ensureDefaults(this, "config.yml");
        reloadConfig();

        if (menuConfig != null) {
            menuConfig.reload();
        }
        if (partyConfig != null) {
            partyConfig.reload();
        }
        if (landConfig != null) {
            landConfig.reload();
        }
        if (masteryConfig != null) {
            masteryConfig.reload();
        }
        if (bossBarConfig != null) {
            bossBarConfig.reload();
        }
        if (fastBoardConfig != null) {
            fastBoardConfig.reload();
        }
        if (rtpConfig != null) {
            rtpConfig.reload();
        }
        if (tpaConfig != null) {
            tpaConfig.reload();
        }
        if (storageConfig != null) {
            storageConfig.reload();
        }
        if (guildManager != null) {
            guildManager.reloadConfig();
        }
        if (playerSettingsManager != null && menuConfig != null) {
            playerSettingsManager.setDefaultTpaGuiEnabled(
                    menuConfig.getBoolean("settings.defaults.tpa-gui", true)
            );
        }
        if (boardService != null) {
            boardService.start();
        }
        if (tpaMenu != null) {
            tpaMenu.reload();
        }
    }

    @Override
    public void onDisable() {
        if (jobService != null) {
            jobService.stop();
        }
        if (jobManager != null) {
            jobManager.save();
        }
        if (masteryService != null) {
            masteryService.save();
        }
        if (guildManager != null) {
            guildManager.save();
        }
        if (homeManager != null) {
            homeManager.save();
        }
        if (landManager != null) {
            landManager.shutdown();
        }
        if (boardService != null) {
            boardService.stop();
        }
        if (levelService != null) {
            levelService.shutdown();
        }
        if (playerSettingsManager != null) {
            playerSettingsManager.save();
        }
        if (tpaMenu != null) {
            tpaMenu.shutdown();
        }
        if (rtpManager != null) {
            rtpManager.shutdown();
        }
        if (tpaManager != null) {
            tpaManager.shutdown();
        }
        if (storageManager != null) {
            storageManager.shutdown();
        }
    }
}
