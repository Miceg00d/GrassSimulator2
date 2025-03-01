package com.example.grasssimulator;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class LegendaryChestManager implements Listener {

    private Main plugin;
    private Random random;
    private Map<Location, Boolean> chestLocations;

    public LegendaryChestManager(Main plugin) {
        this.plugin = plugin;
        this.random = new Random();
        this.chestLocations = new HashMap<>();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void createLegendaryChest(Location location) {
        location.getBlock().setType(Material.CHEST);
        chestLocations.put(location, true);
    }

    private boolean isLegendaryChest(Location location) {
        return chestLocations.containsKey(location);
    }

    private void openChestInfoGUI(Player player) {
        Inventory gui = Bukkit.createInventory(player, 27, "§6Легендарный сундук");

        ItemStack infoItem = new ItemStack(Material.PAPER);
        ItemMeta meta = infoItem.getItemMeta();
        meta.setDisplayName("§aВозможные награды:");
        meta.setLore(Arrays.asList(
                "§7+5% к балансу",
                "§7+10% к балансу"
        ));
        infoItem.setItemMeta(meta);
        gui.setItem(13, infoItem);

        player.openInventory(gui);
    }

    private void giveRandomReward(Player player) {
        BigDecimal balance = plugin.getCustomEconomy().getBalance(player.getUniqueId());
        BigDecimal rewardMultiplier = random.nextDouble() < 0.05 ? new BigDecimal("0.10") : new BigDecimal("0.05");
        BigDecimal reward = balance.multiply(rewardMultiplier);

        BigDecimal newBalance = balance.add(reward);

        if (newBalance.compareTo(CustomEconomy.getMaxBalance()) > 0) {
            player.sendMessage("§cВы достигли максимального баланса (999.9az)!");
            return;
        }

        plugin.getCustomEconomy().deposit(player.getUniqueId(), reward);
        player.sendMessage("§aВы получили +" + rewardMultiplier.multiply(new BigDecimal("100")) + "% к балансу: " + Main.formatNumber(reward) + " монет!");

        plugin.getScoreboardManager().updateScoreboard(player);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock() != null && event.getClickedBlock().getType() == Material.CHEST) {
            Location location = event.getClickedBlock().getLocation();

            if (isLegendaryChest(location)) {
                event.setCancelled(true);
                Player player = event.getPlayer();
                openChestInfoGUI(player);
            }
        } else if (event.getAction() == Action.LEFT_CLICK_BLOCK && event.getClickedBlock() != null && event.getClickedBlock().getType() == Material.CHEST) {
            Location location = event.getClickedBlock().getLocation();

            if (isLegendaryChest(location)) {
                event.setCancelled(true);
                Player player = event.getPlayer();
                if (hasChestKey(player)) {
                    removeChestKey(player);
                    giveRandomReward(player);
                } else {
                    player.sendMessage("§cУ вас нет ключа для открытия этого сундука!");
                }
            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTitle().equals("§6Легендарный сундук")) {
            event.setCancelled(true);
        }
    }

    private boolean hasChestKey(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == Material.TRIPWIRE_HOOK && item.hasItemMeta() && item.getItemMeta().getDisplayName().equals("§aКлюч от сундука")) {
                return true;
            }
        }
        return false;
    }

    private void removeChestKey(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == Material.TRIPWIRE_HOOK && item.hasItemMeta() && item.getItemMeta().getDisplayName().equals("§aКлюч от сундука")) {
                item.setAmount(item.getAmount() - 1);
                break;
            }
        }
    }
}

