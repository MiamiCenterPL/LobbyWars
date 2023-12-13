package net.miamicenter.lobbywars;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class CommandManager implements CommandExecutor {
    LobbyWars plugin = LobbyWars.getPlugin(LobbyWars.class);
    @Override
    public boolean onCommand(@NotNull final CommandSender cs, @NotNull Command cmd, @NotNull String label, String[] args) {
        if (cs instanceof Player player) {
            if (args.length != 1) {
                cs.sendMessage("Usage:");
                cs.sendMessage("  /lobbywars stats");
                return true;
            }
            plugin.getDBManager().getPlayerDeaths(player.getUniqueId());
            plugin.getDBManager().getPlayerKills(player.getUniqueId());
            return true;
        }
        cs.sendMessage("This command is usable only by players.");
        return true;
    }
}
