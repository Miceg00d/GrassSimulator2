package com.example.grasssimulator;

import net.milkbowl.vault.economy.Economy;
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
import java.math.BigInteger;
import java.util.HashMap;
import java.util.UUID;

public class RebirthGUI implements Listener {

    private Main plugin;
    private Economy economy;
    private HashMap<UUID, Integer> rebirthLevels;
    private HashMap<UUID, BigDecimal> tokens;
    private HashMap<UUID, Integer> hoeLevels;
    private PlayerScoreboardManager scoreboardManager;

    public RebirthGUI(Main plugin, Economy economy, HashMap<UUID, Integer> rebirthLevels, HashMap<UUID, BigDecimal> tokens, HashMap<UUID, Integer> hoeLevels, PlayerScoreboardManager scoreboardManager) {
        this.plugin = plugin;
        this.economy = economy;
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
        // Используем BigInteger для расчетов
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
                BigDecimal playerBalance = plugin.getBalance(playerId);

                // Логируем баланс до ребитха
                plugin.getLogger().info("Баланс игрока " + player.getName() + " до ребитха: " + playerBalance);

                if (playerBalance.compareTo(cost) >= 0) {
                    // Обнуляем баланс игрока
                    plugin.setBalance(playerId, BigDecimal.ZERO);

                    // Увеличиваем уровень ребитха
                    rebirthLevels.put(playerId, rebirthLevel + 1);

                    // Выдаем токены за ребитх
                    BigDecimal tokensEarned = new BigDecimal("100").add(new BigDecimal(rebirthLevel * 75));
                    tokens.put(playerId, tokens.getOrDefault(playerId, BigDecimal.ZERO).add(tokensEarned));

                    // Сбрасываем уровень мотыги до 1
                    hoeLevels.put(playerId, 1);

                    // Сообщаем игроку
                    player.sendMessage("§aВы совершили ребитх! Теперь у вас " + (rebirthLevel + 1) + " ребитхов.");
                    player.closeInventory();

                    // Обновляем скорборд
                    scoreboardManager.updateScoreboard(player);

                    // Логируем баланс после ребитха
                    plugin.getLogger().info("Баланс игрока " + player.getName() + " после ребитха: " + plugin.getBalance(playerId));
                } else {
                    player.sendMessage("§cУ вас недостаточно монет для ребитха!");
                }
            }
        }
    }
}

