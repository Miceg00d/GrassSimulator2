package com.example.grasssimulator;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.UUID;

public class PlayerScoreboardManager {

    private Main plugin;
    private Economy economy;

    // Добавляем конструктор с двумя аргументами
    public PlayerScoreboardManager(Main plugin, Economy economy) {
        this.plugin = plugin;
        this.economy = economy;
    }

    public void updateScoreboard(Player player) {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager == null) {
            player.sendMessage(ChatColor.RED + "Ошибка: ScoreboardManager не найден!");
            return;
        }

        Scoreboard board = manager.getNewScoreboard();
        Objective objective = board.registerNewObjective("balance", "dummy", ChatColor.GOLD + "Монеты");
        objective.setDisplaySlot(org.bukkit.scoreboard.DisplaySlot.SIDEBAR);

        UUID playerId = player.getUniqueId();
        BigDecimal balance = new BigDecimal(economy.getBalance(player));
        int hoeLevel = plugin.getHoeLevel(playerId);
        int rebirthLevel = plugin.getRebirthLevel(playerId);
        BigDecimal tokens = plugin.getTokens(playerId);

        BigDecimal hoeMultiplier = new BigDecimal("2").pow(hoeLevel);
        BigDecimal rebirthMultiplier = new BigDecimal(rebirthLevel + 1);
        BigDecimal totalMultiplier = hoeMultiplier.multiply(rebirthMultiplier);

        String formattedBalance = Main.formatNumber(balance);
        String formattedTokens = Main.formatNumber(tokens);
        String formattedMultiplier = Main.formatNumber(totalMultiplier);

        Score score1 = objective.getScore(ChatColor.GREEN + "Монеты: " + formattedBalance);
        score1.setScore(4);

        Score score2 = objective.getScore(ChatColor.AQUA + "Ребитхов: " + rebirthLevel);
        score2.setScore(3);

        Score score3 = objective.getScore(ChatColor.YELLOW + "Токены: " + formattedTokens);
        score3.setScore(2);

        Score score4 = objective.getScore(ChatColor.RED + "Множитель: x" + formattedMultiplier); // Используем отформатированный множитель
        score4.setScore(1);

        player.setScoreboard(board);
    }
}