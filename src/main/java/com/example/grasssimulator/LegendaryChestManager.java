package com.example.grasssimulator;

import net.milkbowl.vault.economy.Economy;
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
    private Economy economy; // Добавляем поле economy
    private Random random;
    private Map<Location, Boolean> chestLocations; // Хранит координаты сундуков

    public LegendaryChestManager(Main plugin, Economy economy) {
        this.plugin = plugin; // Сохраняем ссылку на Main
        this.economy = economy;
        this.random = new Random();
        this.chestLocations = new HashMap<>();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    // Метод для создания легендарного сундука на определённых координатах
    public void createLegendaryChest(Location location) {
        location.getBlock().setType(Material.CHEST); // Устанавливаем сундук
        chestLocations.put(location, true); // Добавляем сундук в список
    }

    // Метод для проверки, является ли блок легендарным сундуком
    private boolean isLegendaryChest(Location location) {
        return chestLocations.containsKey(location);
    }

    // Метод для открытия GUI с информацией о наградах
    private void openChestInfoGUI(Player player) {
        Inventory gui = Bukkit.createInventory(player, 27, "§6Легендарный сундук");

        // Добавляем информацию о наградах
        ItemStack infoItem = new ItemStack(Material.PAPER);
        ItemMeta meta = infoItem.getItemMeta();
        meta.setDisplayName("§aВозможные награды:");
        meta.setLore(Arrays.asList(
                "§7+5% к балансу",
                "§7+10% к балансу"
        ));
        infoItem.setItemMeta(meta);
        gui.setItem(13, infoItem); // Размещаем информацию в центре GUI

        player.openInventory(gui);
    }

    // Метод для выдачи случайной награды
    private void giveRandomReward(Player player) {
        if (economy == null) {
            plugin.getLogger().severe("Economy не инициализирован!"); // Используем plugin.getLogger()
            return;
        }

        // Получаем текущий баланс игрока как BigDecimal
        BigDecimal balance = new BigDecimal(economy.getBalance(player));

        // Логируем баланс до выдачи награды
        plugin.getLogger().info("Баланс игрока " + player.getName() + " до открытия сундука: " + balance);

        // Рассчитываем награду (5% или 10%)
        BigDecimal rewardMultiplier = random.nextDouble() < 0.05 ? new BigDecimal("0.10") : new BigDecimal("0.05");
        BigDecimal reward = balance.multiply(rewardMultiplier);

        // Выдаем награду (добавляем к текущему балансу)
        economy.depositPlayer(player, reward.doubleValue());

        // Сообщаем игроку о награде
        player.sendMessage("§aВы получили +" + rewardMultiplier.multiply(new BigDecimal("100")) + "% к балансу: " + Main.formatNumber(reward) + " монет!");

        // Обновляем скорборд
        plugin.getScoreboardManager().updateScoreboard(player);

        // Логируем баланс после выдачи награды
        plugin.getLogger().info("Баланс игрока " + player.getName() + " после открытия сундука: " + economy.getBalance(player));
    }

    // Обработка кликов по сундуку
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock() != null && event.getClickedBlock().getType() == Material.CHEST) {
            Location location = event.getClickedBlock().getLocation();

            if (isLegendaryChest(location)) {
                event.setCancelled(true); // Отменяем открытие сундука

                Player player = event.getPlayer();
                openChestInfoGUI(player); // Открываем GUI с информацией о наградах
            }
        } else if (event.getAction() == Action.LEFT_CLICK_BLOCK && event.getClickedBlock() != null && event.getClickedBlock().getType() == Material.CHEST) {
            Location location = event.getClickedBlock().getLocation();

            if (isLegendaryChest(location)) {
                event.setCancelled(true); // Отменяем разрушение сундука

                Player player = event.getPlayer();
                if (hasChestKey(player)) {
                    removeChestKey(player); // Убираем 1 ключ
                    giveRandomReward(player); // Выдаем случайную награду
                } else {
                    player.sendMessage("§cУ вас нет ключа для открытия этого сундука!");
                }
            }
        }
    }

    // Обработка кликов в GUI сундука
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTitle().equals("§6Легендарный сундук")) { // Проверяем, что это GUI сундука
            event.setCancelled(true); // Отменяем возможность забирать или перемещать предметы
        }
    }

    // Метод для проверки наличия ключа у игрока
    private boolean hasChestKey(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == Material.TRIPWIRE_HOOK && item.hasItemMeta() && item.getItemMeta().getDisplayName().equals("§aКлюч от сундука")) {
                return true;
            }
        }
        return false;
    }

    // Метод для удаления ключа у игрока
    private void removeChestKey(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == Material.TRIPWIRE_HOOK && item.hasItemMeta() && item.getItemMeta().getDisplayName().equals("§aКлюч от сундука")) {
                item.setAmount(item.getAmount() - 1); // Убираем 1 ключ
                break;
            }
        }
    }
}
