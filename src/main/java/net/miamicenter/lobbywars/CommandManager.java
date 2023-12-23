package net.miamicenter.lobbywars;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class CommandManager implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull final CommandSender cs, @NotNull Command cmd, @NotNull String label, String[] args) {
        if (cs instanceof Player player) {
            if (args.length != 1) {
                cs.sendMessage("Usage:");
                cs.sendMessage("  /lobbywars stats");
                return true;
            }
            PlayerStats stats = PlayerStatsCache.getStats(player.getUniqueId());
            player.sendMessage("Kills : %kills%\nDeaths : %deaths%"
                    .replace("%deaths%", String.valueOf(stats.getDeaths()))
                    .replace("%kills%", String.valueOf(stats.getKills()))
            );
            return true;
        }
        cs.sendMessage("This command is usable only by players.");
        return true;
    }
}
