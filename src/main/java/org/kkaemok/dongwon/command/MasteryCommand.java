package org.kkaemok.dongwon.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.kkaemok.dongwon.progression.MasterySpecialization;
import org.kkaemok.dongwon.progression.MasteryService;
import org.kkaemok.dongwon.progression.PlayerProfile;

public final class MasteryCommand implements CommandExecutor {
    private final MasteryService masteryService;

    public MasteryCommand(MasteryService masteryService) {
        this.masteryService = masteryService;
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args
    ) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("플레이어만 사용 가능합니다.");
            return true;
        }

        if (!masteryService.isMasteryEnabled(player)) {
            sender.sendMessage("숙련도 시스템은 검사 직업에서만 사용할 수 있습니다.");
            return true;
        }

        if (args.length >= 1 && args[0].equalsIgnoreCase("선택")) {
            if (args.length < 2) {
                sender.sendMessage("사용법: /숙련도 선택 <시현류|진-검사>");
                return true;
            }
            MasterySpecialization specialization = MasterySpecialization.fromInput(args[1]);
            masteryService.chooseSpecialization(player, specialization);
            return true;
        }

        PlayerProfile profile = masteryService.getProfile(player);
        sender.sendMessage("숙련도 정보");
        sender.sendMessage("- 레벨: " + profile.getMasteryLevel());
        sender.sendMessage("- 경험치: " + profile.getMasteryExp());
        sender.sendMessage("- 특화: " + profile.getSpecialization().getDisplayName());
        sender.sendMessage("- 캔: " + profile.getCan());
        sender.sendMessage("- 실버캔: " + profile.getSilverCan());
        sender.sendMessage("- 황금캔: " + profile.getGoldenCan());
        if (profile.getMasteryLevel() >= 3 && profile.getSpecialization() == MasterySpecialization.NONE) {
            sender.sendMessage("- 특화 선택: /숙련도 선택 <시현류|진-검사>");
        }
        return true;
    }
}
