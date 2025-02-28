package com.example.grasssimulator;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.math.BigDecimal;

public class AdminCommands implements CommandExecutor {

    private Economy economy;
    private HoeManager hoeManager;
    private PlayerScoreboardManager scoreboardManager;
    private Main plugin; // Добавляем ссылку на Main

    public AdminCommands(Main plugin, Economy economy, HoeManager hoeManager, PlayerScoreboardManager scoreboardManager) {
        this.plugin = plugin; // Инициализируем plugin
        this.economy = economy;
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

                        // Получаем текущий баланс
                        BigDecimal currentBalance = plugin.getBalance(player.getUniqueId());

                        // Устанавливаем новый баланс
                        plugin.setBalance(player.getUniqueId(), currentBalance.add(amount));

                        // Сообщаем игроку
                        player.sendMessage("§aВы получили " + Main.formatNumber(amount) + " монет!");

                        // Обновляем скорборд
                        scoreboardManager.updateScoreboard(player);

                        return true;
                    } catch (NumberFormatException e) {
                        player.sendMessage("§cНекорректная сумма!");
                        return true;
                    }
                }
            }

            if (command.getName().equalsIgnoreCase("to")) {
                if (args.length == 1) {
                    try {
                        BigDecimal amount = new BigDecimal(args[0]);

                        if (amount.compareTo(BigDecimal.ZERO) < 0) {
                            player.sendMessage("§cНекорректное количество токенов!");
                            return true;
                        }

                        hoeManager.setTokens(player, amount); // Передаем BigDecimal напрямую
                        player.sendMessage("§aВы получили " + amount + " токенов!");
                        scoreboardManager.updateScoreboard(player); // Обновляем скорборд
                        return true;
                    } catch (NumberFormatException e) {
                        player.sendMessage("§cНекорректное количество токенов!");
                        return true;
                    }
                }
            }
        }
        return false;
    }
}

