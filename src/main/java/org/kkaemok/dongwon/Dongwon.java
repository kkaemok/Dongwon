package org.kkaemok.dongwon;

import org.kkaemok.dongwon.board.BoardService;
import org.kkaemok.dongwon.command.DongwonCommand;
import org.kkaemok.dongwon.command.GuildCommand;
import org.kkaemok.dongwon.command.JobCommand;
import org.kkaemok.dongwon.command.MasteryCommand;
import org.kkaemok.dongwon.command.MenuCommand;
import org.kkaemok.dongwon.command.RemitCommand;
import org.kkaemok.dongwon.command.TpaCommand;
import org.kkaemok.dongwon.guild.GuildManager;
import org.kkaemok.dongwon.job.JobManager;
import org.kkaemok.dongwon.job.JobService;
import org.kkaemok.dongwon.listener.BoardListener;
import org.kkaemok.dongwon.listener.GuildJoinNotifyListener;
import org.kkaemok.dongwon.listener.JinSwordsmanListener;
import org.kkaemok.dongwon.listener.JobListener;
import org.kkaemok.dongwon.listener.MasteryListener;
import org.kkaemok.dongwon.listener.MenuListener;
import org.kkaemok.dongwon.listener.PlayerJoinListener;
import org.kkaemok.dongwon.listener.ShihyeonryuListener;
import org.kkaemok.dongwon.listener.TpaListener;
import org.kkaemok.dongwon.menu.HomeManager;
import org.kkaemok.dongwon.menu.MenuService;
import org.kkaemok.dongwon.progression.JinSwordsmanService;
import org.kkaemok.dongwon.progression.MasteryService;
import org.kkaemok.dongwon.progression.ProfileManager;
import org.kkaemok.dongwon.progression.ShihyeonryuService;
import org.kkaemok.dongwon.teleport.TpaService;
import org.bukkit.plugin.java.JavaPlugin;

public final class Dongwon extends JavaPlugin {
    private JobManager jobManager;
    private JobService jobService;
    private ProfileManager profileManager;
    private MasteryService masteryService;
    private JinSwordsmanService jinSwordsmanService;
    private ShihyeonryuService shihyeonryuService;
    private BoardService boardService;
    private GuildManager guildManager;
    private HomeManager homeManager;
    private MenuService menuService;
    private TpaService tpaService;

    @Override
    public void onEnable() {
        this.jobManager = new JobManager(this);
        this.jobService = new JobService(this, jobManager);
        this.profileManager = new ProfileManager(this);
        this.guildManager = new GuildManager(this, profileManager);
        this.masteryService = new MasteryService(profileManager, jobManager);
        this.jinSwordsmanService = new JinSwordsmanService(this, masteryService);
        this.shihyeonryuService = new ShihyeonryuService(this, masteryService);
        this.boardService = new BoardService(this, jobManager, profileManager);
        this.homeManager = new HomeManager(this);
        this.menuService = new MenuService(this, jobManager, jobService, profileManager, homeManager);
        this.tpaService = new TpaService();

        getServer().getPluginManager().registerEvents(new JobListener(jobService), this);
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this, jobManager, jobService), this);
        getServer().getPluginManager().registerEvents(new MasteryListener(masteryService), this);
        getServer().getPluginManager().registerEvents(new JinSwordsmanListener(jinSwordsmanService), this);
        getServer().getPluginManager().registerEvents(new ShihyeonryuListener(shihyeonryuService), this);
        getServer().getPluginManager().registerEvents(new BoardListener(boardService), this);
        getServer().getPluginManager().registerEvents(new GuildJoinNotifyListener(guildManager), this);
        getServer().getPluginManager().registerEvents(new MenuListener(menuService), this);
        getServer().getPluginManager().registerEvents(new TpaListener(tpaService), this);

        JobCommand jobCommand = new JobCommand(jobManager, jobService);
        if (getCommand("직업") != null) {
            getCommand("직업").setExecutor(jobCommand);
            getCommand("직업").setTabCompleter(jobCommand);
        }
        if (getCommand("직업설정") != null) {
            getCommand("직업설정").setExecutor(jobCommand);
            getCommand("직업설정").setTabCompleter(jobCommand);
        }
        if (getCommand("숙련도") != null) {
            getCommand("숙련도").setExecutor(new MasteryCommand(masteryService));
        }
        GuildCommand guildCommand = new GuildCommand(guildManager);
        if (getCommand("길드") != null) {
            getCommand("길드").setExecutor(guildCommand);
            getCommand("길드").setTabCompleter(guildCommand);
        }
        DongwonCommand dongwonCommand = new DongwonCommand(jobManager, jobService, profileManager, guildManager);
        if (getCommand("동원") != null) {
            getCommand("동원").setExecutor(dongwonCommand);
            getCommand("동원").setTabCompleter(dongwonCommand);
        }
        if (getCommand("메뉴") != null) {
            getCommand("메뉴").setExecutor(new MenuCommand(menuService));
        }
        RemitCommand remitCommand = new RemitCommand(profileManager);
        if (getCommand("송금") != null) {
            getCommand("송금").setExecutor(remitCommand);
            getCommand("송금").setTabCompleter(remitCommand);
        }
        TpaCommand tpaCommand = new TpaCommand(tpaService);
        if (getCommand("tpa") != null) {
            getCommand("tpa").setExecutor(tpaCommand);
            getCommand("tpa").setTabCompleter(tpaCommand);
        }

        jobService.start();
        boardService.start();
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
        if (boardService != null) {
            boardService.stop();
        }
    }
}
