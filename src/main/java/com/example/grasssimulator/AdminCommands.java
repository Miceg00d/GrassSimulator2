package com.example.grasssimulator;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.math.BigDecimal;

public class AdminCommands implements CommandExecutor {

    private Main plugin;
    private HoeManager hoeManager;
    private PlayerScoreboardManager scoreboardManager;

    public AdminCommands(Main plugin, HoeManager hoeManager, PlayerScoreboardManager scoreboardManager) {
        this.plugin = plugin;
        this.hoeManager = hoeManager;
        this.scoreboardManager = scoreboardManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player && sender.isOp()) {
            Player player = (Player) sender;

            if (command.getName().equalsIgnoreCase("mo")) {
                if (args.length == 1) {
                    try {
                        BigDecimal amount = new BigDecimal(args[0]);

                        if (amount.compareTo(BigDecimal.ZERO) < 0) {
                            player.sendMessage("§cНекорректная сумма!");
                            return true;
                        }

                        BigDecimal currentBalance = plugin.getCustomEconomy().getBalance(player.getUniqueId());
                        BigDecimal newBalance = currentBalance.add(amount);

                        if (newBalance.compareTo(CustomEconomy.getMaxBalance()) > 0) {
                            player.sendMessage("§cВы достигли максимального баланса (999.9az)!");
                            return true;
                        }

                        plugin.getCustomEconomy().deposit(player.getUniqueId(), amount);
                        player.sendMessage("§aВы получили " + Main.formatNumber(amount) + " монет!");
                        plugin.getScoreboardManager().updateScoreboard(player);
                        return true;
                    } catch (NumberFormatException e) {
                        player.sendMessage("§cНекорректная сумма!");
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
