package com.example.grasssimulator;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.math.BigDecimal;
import java.util.UUID;

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
        if (sender instanceof Player) {
            Player player = (Player) sender;

            if (command.getName().equalsIgnoreCase("mo")) {
                if (!player.isOp()) {
                    player.sendMessage("§cУ вас нет прав на использование этой команды!");
                    return true;
                }
                return handleMoneyCommand(player, args);
            } else if (command.getName().equalsIgnoreCase("to")) {
                if (!player.isOp()) {
                    player.sendMessage("§cУ вас нет прав на использование этой команды!");
                    return true;
                }
                return handleTokensCommand(player, args);
            } else if (command.getName().equalsIgnoreCase("setbalance")) {
                if (!player.isOp()) {
                    player.sendMessage("§cУ вас нет прав на использование этой команды!");
                    return true;
                }
                return handleSetBalanceCommand(player, args);
            } else if (command.getName().equalsIgnoreCase("settokens")) {
                if (!player.isOp()) {
                    player.sendMessage("§cУ вас нет прав на использование этой команды!");
                    return true;
                }
                return handleSetTokensCommand(player, args);
            }
        }
        return false;
    }

    private boolean handleMoneyCommand(Player sender, String[] args) {
        if (args.length < 1 || args.length > 2) {
            sender.sendMessage("§cИспользование: /mo <количество> [игрок]");
            return true;
        }

        try {
            BigDecimal amount = new BigDecimal(args[0]);
            Player targetPlayer = args.length == 2 ? plugin.getServer().getPlayer(args[1]) : sender;

            if (targetPlayer == null) {
                sender.sendMessage("§cИгрок не найден!");
                return true;
            }

            if (amount.compareTo(BigDecimal.ZERO) < 0) {
                sender.sendMessage("§cНекорректная сумма!");
                return true;
            }

            BigDecimal currentBalance = plugin.getCustomEconomy().getBalance(targetPlayer.getUniqueId());
            BigDecimal newBalance = currentBalance.add(amount);

            if (newBalance.compareTo(CustomEconomy.getMaxBalance()) > 0) {
                sender.sendMessage("§cМаксимальный баланс достигнут (999.9az)!");
                return true;
            }

            plugin.getCustomEconomy().deposit(targetPlayer.getUniqueId(), amount);
            sender.sendMessage("§aВы выдали " + Main.formatNumber(amount) + " монет игроку " + targetPlayer.getName() + "!");
            targetPlayer.sendMessage("§aВы получили " + Main.formatNumber(amount) + " монет от " + sender.getName() + "!");

            scoreboardManager.updateScoreboard(targetPlayer);
            return true;
        } catch (NumberFormatException e) {
            sender.sendMessage("§cНекорректная сумма!");
            return true;
        }
        
    }

    private boolean handleTokensCommand(Player sender, String[] args) {
        if (args.length < 1 || args.length > 2) {
            sender.sendMessage("§cИспользование: /to <количество> [игрок]");
            return true;
        }

        try {
            BigDecimal amount = new BigDecimal(args[0]);
            Player targetPlayer = args.length == 2 ? plugin.getServer().getPlayer(args[1]) : sender;

            if (targetPlayer == null) {
                sender.sendMessage("§cИгрок не найден!");
                return true;
            }

            if (amount.compareTo(BigDecimal.ZERO) < 0) {
                sender.sendMessage("§cНекорректная сумма!");
                return true;
            }

            UUID targetPlayerId = targetPlayer.getUniqueId();
            BigDecimal currentTokens = plugin.getTokens(targetPlayerId);
            BigDecimal newTokens = currentTokens.add(amount);

            // Устанавливаем новые токены
            plugin.setTokens(targetPlayerId, newTokens);

            // Сохраняем данные игрока в базе данных
            plugin.savePlayerData(targetPlayer);
            sender.sendMessage("§aВы выдали " + Main.formatNumber(amount) + " токенов игроку " + targetPlayer.getName() + "!");
            targetPlayer.sendMessage("§aВы получили " + Main.formatNumber(amount) + " токенов от " + sender.getName() + "!");

            scoreboardManager.updateScoreboard(targetPlayer);
            return true;
        } catch (NumberFormatException e) {
            sender.sendMessage("§cНекорректная сумма!");
            return true;
        }

    }

    private boolean handleSetBalanceCommand(Player sender, String[] args) {
        if (args.length != 2) {
            sender.sendMessage("§cИспользование: /setbalance <игрок> <количество>");
            return true;
        }

        try {
            Player targetPlayer = plugin.getServer().getPlayer(args[0]);
            BigDecimal amount = new BigDecimal(args[1]);

            if (targetPlayer == null) {
                sender.sendMessage("§cИгрок не найден!");
                return true;
            }

            if (amount.compareTo(BigDecimal.ZERO) < 0) {
                sender.sendMessage("§cНекорректная сумма!");
                return true;
            }

            if (amount.compareTo(CustomEconomy.getMaxBalance()) > 0) {
                sender.sendMessage("§cМаксимальный баланс (999.9az)!");
                return true;
            }

            plugin.getCustomEconomy().setBalance(targetPlayer.getUniqueId(), amount);
            sender.sendMessage("§aВы установили баланс игрока " + targetPlayer.getName() + " на " + Main.formatNumber(amount) + " монет!");
            targetPlayer.sendMessage("§aВаш баланс был установлен на " + Main.formatNumber(amount) + " монет!");

            scoreboardManager.updateScoreboard(targetPlayer);
            return true;
        } catch (NumberFormatException e) {
            sender.sendMessage("§cНекорректная сумма!");
            return true;
        }
    }

    private boolean handleSetTokensCommand(Player sender, String[] args) {
        if (args.length != 2) {
            sender.sendMessage("§cИспользование: /settokens <игрок> <количество>");
            return true;
        }

        try {
            Player targetPlayer = plugin.getServer().getPlayer(args[0]);
            BigDecimal amount = new BigDecimal(args[1]);

            if (targetPlayer == null) {
                sender.sendMessage("§cИгрок не найден!");
                return true;
            }

            if (amount.compareTo(BigDecimal.ZERO) < 0) {
                sender.sendMessage("§cНекорректная сумма!");
                return true;
            }

            plugin.setTokens(targetPlayer.getUniqueId(), amount);

            plugin.savePlayerData(targetPlayer);

            sender.sendMessage("§aВы установили токены игрока " + targetPlayer.getName() + " на " + Main.formatNumber(amount) + "!");
            targetPlayer.sendMessage("§aВаши токены были установлены на " + Main.formatNumber(amount) + "!");

            scoreboardManager.updateScoreboard(targetPlayer);
            return true;
        } catch (NumberFormatException e) {
            sender.sendMessage("§cНекорректная сумма!");
            return true;
        }
    }
}
