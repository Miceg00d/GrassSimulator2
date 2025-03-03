package com.example.grasssimulator;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.UUID;

public class RebirthGUI implements Listener {

    private Main plugin;
    private HashMap<UUID, Integer> rebirthLevels;
    private HashMap<UUID, BigDecimal> tokens;
    private HashMap<UUID, Integer> hoeLevels;
    private PlayerScoreboardManager scoreboardManager;

    public RebirthGUI(Main plugin, HashMap<UUID, Integer> rebirthLevels, HashMap<UUID, BigDecimal> tokens, HashMap<UUID, Integer> hoeLevels, PlayerScoreboardManager scoreboardManager) {
        this.plugin = plugin;
        this.rebirthLevels = rebirthLevels;
        this.tokens = tokens;
        this.hoeLevels = hoeLevels;
        this.scoreboardManager = scoreboardManager;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void openRebirthMenu(Player player) {
        Inventory gui = Bukkit.createInventory(player, 27, "§6Ребитх");

        UUID playerId = player.getUniqueId();
        int rebirthLevel = rebirthLevels.getOrDefault(playerId, 0);
        BigDecimal cost = new BigDecimal("5000000").multiply(new BigDecimal("2").pow(rebirthLevel));

        ItemStack infoItem = new ItemStack(Material.PAPER);
        ItemMeta meta = infoItem.getItemMeta();
        meta.setDisplayName("§eРебитхов: " + rebirthLevel + " (x" + (rebirthLevel + 1) + ")");
        infoItem.setItemMeta(meta);
        gui.setItem(11, infoItem);

        ItemStack rebirthItem = new ItemStack(Material.NETHER_STAR);
        ItemMeta rebirthMeta = rebirthItem.getItemMeta();
        rebirthMeta.setDisplayName("§aСовершить ребитх (§6Стоимость: " + Main.formatNumber(cost) + "§a)");
        rebirthItem.setItemMeta(rebirthMeta);
        gui.setItem(13, rebirthItem);

        player.openInventory(gui);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTitle().equals("§6Ребитх")) {
            event.setCancelled(true);

            Player player = (Player) event.getWhoClicked();
            UUID playerId = player.getUniqueId();
            int rebirthLevel = rebirthLevels.getOrDefault(playerId, 0);
            BigDecimal cost = new BigDecimal("5000000").multiply(new BigDecimal("2").pow(rebirthLevel));

            if (event.getSlot() == 13) {
                BigDecimal playerBalance = plugin.getCustomEconomy().getBalance(playerId);

                if (playerBalance.compareTo(cost) >= 0) {

                    plugin.getCustomEconomy().withdraw(playerId, cost);

                    plugin.getCustomEconomy().setBalance(playerId, BigDecimal.ZERO);

                    rebirthLevels.put(playerId, rebirthLevel + 1);

                    BigDecimal tokensEarned = new BigDecimal("100").add(new BigDecimal(rebirthLevel * 75));
                    tokens.put(playerId, tokens.getOrDefault(playerId, BigDecimal.ZERO).add(tokensEarned));
                    plugin.setTokens(playerId, tokens.get(playerId)); // Используем новый метод

                    hoeLevels.put(playerId, 1);

                    player.sendMessage("§aВы совершили ребитх! Теперь у вас " + (rebirthLevel + 1) + " ребитхов.");
                    player.closeInventory();
                    scoreboardManager.updateScoreboard(player);
                } else {
                    player.sendMessage("§cУ вас недостаточно монет для ребитха!");
                }
            }
        }
    }
}

