package com.example.grasssimulator;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.UUID;

public class RebirthManager implements CommandExecutor {

    private Main plugin;
    private HashMap<UUID, Integer> rebirthLevels;
    private HashMap<UUID, BigDecimal> tokens;
    private HashMap<UUID, Integer> hoeLevels;
    private PlayerScoreboardManager scoreboardManager;
    private RebirthGUI rebirthGUI;

    public RebirthManager(Main plugin, HashMap<UUID, Integer> rebirthLevels, HashMap<UUID, BigDecimal> tokens, HashMap<UUID, Integer> hoeLevels, PlayerScoreboardManager scoreboardManager) {
        this.plugin = plugin;
        this.rebirthLevels = rebirthLevels;
        this.tokens = tokens;
        this.hoeLevels = hoeLevels;
        this.scoreboardManager = scoreboardManager;
        this.rebirthGUI = new RebirthGUI(plugin, rebirthLevels, tokens, hoeLevels, scoreboardManager);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            rebirthGUI.openRebirthMenu(player);
            return true;
        }
        return false;
    }
    // Метод для получения уровня ребитха игрока
    public int getRebirthLevel(UUID playerId) {
        return rebirthLevels.getOrDefault(playerId, 0);
    }
    public void setRebirthLevel(UUID playerId, int rebirths) {
        rebirthLevels.put(playerId, rebirths);
// Сохраняем данные в базе данных
        Player player = Bukkit.getPlayer(playerId);
        if (player != null) {
            plugin.savePlayerData(player);
        }
    }
}

