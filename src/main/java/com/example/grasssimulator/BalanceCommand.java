package com.example.grasssimulator;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.math.BigDecimal;
import java.util.UUID;

public class BalanceCommand implements CommandExecutor {

    private Main plugin;

    public BalanceCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            UUID playerId = player.getUniqueId();

            BigDecimal balance = plugin.getCustomEconomy().getBalance(playerId);
            player.sendMessage("§aВаш баланс: " + Main.formatNumber(balance) + " монет.");
            return true;
        } else {
            sender.sendMessage("§cЭта команда доступна только игрокам.");
            return false;
        }
    }
}