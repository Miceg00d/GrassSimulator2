package com.example.grasssimulator;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.UUID;

public class RebirthManager implements CommandExecutor {

    private Main plugin; // Используем Main вместо JavaPlugin
    private Economy economy;
    private HashMap<UUID, Integer> rebirthLevels;
    private HashMap<UUID, BigDecimal> tokens;
    private HashMap<UUID, Integer> hoeLevels;
    private PlayerScoreboardManager scoreboardManager;
    private RebirthGUI rebirthGUI;

    public RebirthManager(Main plugin, Economy economy, HashMap<UUID, Integer> rebirthLevels, HashMap<UUID, BigDecimal> tokens, HashMap<UUID, Integer> hoeLevels, PlayerScoreboardManager scoreboardManager) {
        this.plugin = plugin;
        this.economy = economy;
        this.rebirthLevels = rebirthLevels;
        this.tokens = tokens;
        this.hoeLevels = hoeLevels;
        this.scoreboardManager = scoreboardManager;
        this.rebirthGUI = new RebirthGUI(plugin, economy, rebirthLevels, tokens, hoeLevels, scoreboardManager);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            rebirthGUI.openRebirthMenu(player); // Открываем GUI ребитха
            return true;
        }
        return false;
    }
}
