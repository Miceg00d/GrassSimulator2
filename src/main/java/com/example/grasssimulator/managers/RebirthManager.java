package com.example.grasssimulator.managers;

import com.example.grasssimulator.Main;
import com.example.grasssimulator.gui.RebirthGUI;
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

        Player player = Bukkit.getPlayer(playerId);
        if (player != null) {
            String lastActiveHoe = plugin.getHoeManager().getActiveHoe(playerId);
            int currentHoeLevel = plugin.getHoeManager().getHoeLevel(playerId); // Загружаем уровень мотыги

            plugin.getHoeManager().giveHoe(player, lastActiveHoe, currentHoeLevel);
            plugin.getHoeManager().setActiveHoe(playerId, lastActiveHoe);

            // ✅ Гарантированно сохраняем уровень мотыги в базе (чтобы не обнулялся!)
            plugin.getHoeManager().setHoeLevel(playerId, currentHoeLevel);
            plugin.savePlayerData(player);

            player.sendMessage("§aРебитх выполнен! Ваш уровень мотыги сохранён: " + currentHoeLevel);
        }
    }


}

