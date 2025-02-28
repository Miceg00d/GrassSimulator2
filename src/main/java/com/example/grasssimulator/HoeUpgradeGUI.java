package com.example.grasssimulator;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
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

public class HoeUpgradeGUI implements CommandExecutor, Listener {

    private Main plugin;
    private Economy economy;
    private HashMap<UUID, Integer> hoeLevels;
    private PlayerScoreboardManager scoreboardManager;
    private HoeManager hoeManager; // Добавляем hoeManager

    public HoeUpgradeGUI(Main plugin, Economy economy, HashMap<UUID, Integer> hoeLevels, PlayerScoreboardManager scoreboardManager, HoeManager hoeManager) {
        this.plugin = plugin;
        this.economy = economy;
        this.hoeLevels = hoeLevels;
        this.scoreboardManager = scoreboardManager;
        this.hoeManager = hoeManager; // Инициализируем hoeManager
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            openUpgradeMenu(player);
            return true;
        }
        return false;
    }

    public void openUpgradeMenu(Player player) {
        Inventory gui = Bukkit.createInventory(player, 27, "§6Улучшение мотыги");

        UUID playerId = player.getUniqueId();
        int hoeLevel = hoeLevels.getOrDefault(playerId, 1);
        int rebirthLevel = plugin.getRebirthLevel(playerId);

        BigDecimal cost = new BigDecimal("100").multiply(new BigDecimal("3").pow(hoeLevel - 1));
        BigDecimal hoeMultiplier = new BigDecimal("2").pow(hoeLevel);
        BigDecimal totalMultiplier = hoeMultiplier.multiply(new BigDecimal(rebirthLevel + 1));

        ItemStack infoItem = new ItemStack(Material.PAPER);
        ItemMeta meta = infoItem.getItemMeta();
        meta.setDisplayName("§eУровень мотыги: " + hoeLevel + " (x" + Main.formatNumber(hoeMultiplier) + ")");
        infoItem.setItemMeta(meta);
        gui.setItem(11, infoItem);

        ItemStack upgradeItem = new ItemStack(Material.GOLDEN_HOE);
        ItemMeta upgradeMeta = upgradeItem.getItemMeta();

        if (hoeLevel >= 200) {
            // Если уровень мотыги максимальный, показываем сообщение вместо цены
            upgradeMeta.setDisplayName("§aМаксимальный уровень");
            upgradeMeta.setLore(Arrays.asList(
                    "§cВы достигли максимального уровня мотыги!"
            ));
        } else {

            upgradeMeta.setDisplayName("§aУлучшить мотыгу (§6Стоимость: " + Main.formatNumber(cost) + "§a)");
        }
        upgradeItem.setItemMeta(upgradeMeta);
        gui.setItem(13, upgradeItem);

        player.openInventory(gui);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTitle().equals("§6Улучшение мотыги")) {
            event.setCancelled(true);

            Player player = (Player) event.getWhoClicked();
            UUID playerId = player.getUniqueId();
            int hoeLevel = hoeLevels.getOrDefault(playerId, 1);

            // Проверяем, не превышен ли максимальный уровень (200)
            if (hoeLevel >= 200) {
                player.sendMessage("§cВы достигли максимального уровня мотыги!");
                return;
            }

            // Рассчитываем стоимость улучшения
            BigDecimal cost = new BigDecimal("100").multiply(new BigDecimal("3").pow(hoeLevel - 1));


            if (event.getSlot() == 13) { // Проверяем, что клик был по кнопке улучшения
                if (economy.has(player, cost.doubleValue())) { // Проверяем, хватает ли денег
                    economy.withdrawPlayer(player, cost.doubleValue()); // Списываем стоимость улучшения
                    hoeLevels.put(playerId, hoeLevel + 1); // Увеличиваем уровень мотыги
                    hoeManager.giveHoe(player, hoeManager.getActiveHoe(playerId), hoeLevel + 1); // Обновляем мотыгу в инвентаре
                    player.sendMessage("§aВаша мотыга улучшена до уровня " + (hoeLevel + 1) + "!");
                    openUpgradeMenu(player); // Обновляем GUI
                    scoreboardManager.updateScoreboard(player); // Обновляем скорборд
                } else {
                    player.sendMessage("§cУ вас недостаточно монет для улучшения!");
                }
            }
        }
    }
}
