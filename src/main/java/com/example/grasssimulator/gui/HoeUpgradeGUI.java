package com.example.grasssimulator.gui;

import com.example.grasssimulator.managers.HoeManager;
import com.example.grasssimulator.Main;
import com.example.grasssimulator.managers.PlayerScoreboardManager;
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
    private HashMap<UUID, Integer> hoeLevels;
    private PlayerScoreboardManager scoreboardManager;
    private HoeManager hoeManager;

    public HoeUpgradeGUI(Main plugin, HashMap<UUID, Integer> hoeLevels, PlayerScoreboardManager scoreboardManager, HoeManager hoeManager) {
        this.plugin = plugin;
        this.hoeLevels = hoeLevels;
        this.scoreboardManager = scoreboardManager;
        this.hoeManager = hoeManager;
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
        int hoeLevel = plugin.getHoeManager().getHoeLevel(playerId); // Берём актуальный уровень
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
            int hoeLevel = plugin.getHoeManager().getHoeLevel(playerId);

            if (hoeLevel >= 200) {
                player.sendMessage("§cВы достигли максимального уровня мотыги!");
                return;
            }

            BigDecimal cost = new BigDecimal("100").multiply(new BigDecimal("3").pow(hoeLevel - 1));

            if (event.getSlot() == 13) {
                BigDecimal playerBalance = plugin.getCustomEconomy().getBalance(playerId);

                if (playerBalance.compareTo(cost) >= 0) {
                    plugin.getCustomEconomy().withdraw(playerId, cost);
                    hoeLevels.put(playerId, hoeLevel + 1);
                    plugin.getHoeManager().setHoeLevel(playerId, hoeLevel + 1); // Сохраняем новый уровень
                    plugin.savePlayerData(player); // ✅ Гарантированно сохраняем уровень

                    // Выдаём новую мотыгу с обновлённым уровнем!
                    String activeHoe = plugin.getHoeManager().getActiveHoe(playerId);
                    plugin.getHoeManager().giveHoe(player, activeHoe, hoeLevel + 1);
                    plugin.savePlayerData(player); // ✅ Гарантированно сохраняем уровень


                    player.sendMessage("§aВаша мотыга улучшена до уровня " + (hoeLevel + 1) + "!");
                    openUpgradeMenu(player);
                    scoreboardManager.updateScoreboard(player);
                } else {
                    player.sendMessage("§cУ вас недостаточно монет для улучшения!");
                }
            }
        }
    }
}
