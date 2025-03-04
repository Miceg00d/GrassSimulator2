package com.example.grasssimulator.managers;

import com.example.grasssimulator.Main;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;

import java.math.BigDecimal;
import java.util.UUID;

public class PlayerScoreboardManager {

    private Main plugin;

    public PlayerScoreboardManager(Main plugin) {
        this.plugin = plugin;
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
        BigDecimal balance = plugin.getCustomEconomy().getBalance(playerId);
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

        Score score4 = objective.getScore(ChatColor.RED + "Множитель: x" + formattedMultiplier);
        score4.setScore(1);

        player.setScoreboard(board);
    }
}