package org.kkaemok.dongwon.progression;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

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
            String @NotNull [] args
    ) {
        if (!(sender instanceof Player player)) {
            masteryService.sendPlayerOnly(sender);
            return true;
        }

        if (args.length >= 1 && args[0].equalsIgnoreCase("선택")) {
            if (args.length < 2) {
                masteryService.sendSwordsmanSelectUsage(sender);
                return true;
            }
            MasterySpecialization specialization = MasterySpecialization.fromInput(args[1]);
            masteryService.chooseSpecialization(player, specialization);
            return true;
        }

        masteryService.sendMasteryInfo(player);
        return true;
    }
}
