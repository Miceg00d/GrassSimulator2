package com.example.grasssimulator;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.UUID;

public class HoeShopGUI implements CommandExecutor, Listener {

    private HoeManager hoeManager;
    private JavaPlugin plugin;
    private PlayerScoreboardManager scoreboardManager;

    public HoeShopGUI(JavaPlugin plugin, HoeManager hoeManager, PlayerScoreboardManager scoreboardManager) {
        this.plugin = plugin;
        this.hoeManager = hoeManager;
        this.scoreboardManager = scoreboardManager;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            openShop(player);
            return true;
        }
        return false;
    }

    public void openShop(Player player) {
        Inventory gui = Bukkit.createInventory(player, 9, "§6Магазин мотыг");

        // Мотыга-Бустер
        ItemStack boosterHoe = new ItemStack(Material.GOLDEN_HOE);
        ItemMeta boosterMeta = boosterHoe.getItemMeta();
        boosterMeta.setDisplayName("§6Мотыга-Бустер");
        if (hoeManager.hasHoe(player, "Бустер")) {
            boosterMeta.setLore(Arrays.asList(
                    "§aЭффект: Скорость I на 10 секунд",
                    "§aУже куплена!",
                    "§aНажмите, чтобы выбрать."
            ));
        } else {
            boosterMeta.setLore(Arrays.asList(
                    "§aЭффект: Скорость I на 10 секунд",
                    "§aЦена: 14999 токенов",
                    "§aНажмите, чтобы купить."
            ));
        }
        boosterHoe.setItemMeta(boosterMeta);
        gui.setItem(2, boosterHoe);

        // Мотыга-Легенда
        ItemStack legendHoe = new ItemStack(Material.DIAMOND_HOE);
        ItemMeta legendMeta = legendHoe.getItemMeta();
        legendMeta.setDisplayName("§cМотыга-Легенда");
        if (hoeManager.hasHoe(player, "Легенда")) {
            legendMeta.setLore(Arrays.asList(
                    "§aЭффект: 2x множитель дохода",
                    "§eУже куплена!",
                    "§cНажмите, чтобы выбрать."
            ));
        } else {
            legendMeta.setLore(Arrays.asList(
                    "§aЭффект: 2x множитель дохода",
                    "§eЦена: 150K токенов",
                    "§cНажмите, чтобы купить."
            ));
        }
        legendHoe.setItemMeta(legendMeta);
        gui.setItem(6, legendHoe);

        player.openInventory(gui);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTitle().equals("§6Магазин мотыг")) {
            event.setCancelled(true);

            if (event.getWhoClicked() instanceof Player) {
                Player player = (Player) event.getWhoClicked();
                ItemStack clickedItem = event.getCurrentItem();

                if (clickedItem != null && clickedItem.getType().toString().endsWith("_HOE")) {
                    UUID playerId = player.getUniqueId();
                    int hoeLevel = hoeManager.getHoeLevel(playerId);

                    switch (event.getSlot()) {
                        case 2: // Мотыга-Бустер
                            if (hoeManager.hasHoe(player, "Бустер")) {
                                hoeManager.giveHoe(player, "Бустер", hoeLevel);
                                player.sendMessage("§aВы выбрали мотыгу-бустер!");
                            } else {
                                if (hoeManager.buyHoe(player, "Бустер", new BigDecimal("14999"))) {
                                    player.sendMessage("§aВы успешно купили мотыгу-бустер!");
                                    hoeManager.addPurchasedHoe(player, "Бустер");
                                    scoreboardManager.updateScoreboard(player);
                                    openShop(player);
                                } else {
                                    player.sendMessage("§cУ вас недостаточно токенов для покупки мотыги-бустер!");
                                }
                            }
                            break;
                        case 6: // Мотыга-Легенда
                            if (hoeManager.hasHoe(player, "Легенда")) {
                                hoeManager.giveHoe(player, "Легенда", hoeLevel);
                                player.sendMessage("§aВы выбрали мотыгу-легенду!");
                            } else {
                                if (hoeManager.buyHoe(player, "Легенда", new BigDecimal("150000"))) {
                                    player.sendMessage("§aВы успешно купили мотыгу-легенду!");
                                    hoeManager.addPurchasedHoe(player, "Легенда");
                                    scoreboardManager.updateScoreboard(player);
                                    openShop(player);
                                } else {
                                    player.sendMessage("§cУ вас недостаточно токенов для покупки мотыги-легенды!");
                                }
                            }
                            break;
                    }
                }
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getView().getTitle().equals("§6Магазин мотыг")) {
            event.setCancelled(true);
        }
    }
}

