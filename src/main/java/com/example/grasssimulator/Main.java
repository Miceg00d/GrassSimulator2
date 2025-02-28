package com.example.grasssimulator;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.math.BigDecimal;;
import java.util.HashMap;
import java.util.Random;
import java.util.UUID;

public class Main extends JavaPlugin implements Listener {

    private HashMap<UUID, BigDecimal> playerBalances = new HashMap<>();
    private HashMap<UUID, Integer> hoeLevels = new HashMap<>();
    private HashMap<UUID, Integer> rebirthLevels = new HashMap<>();
    private HashMap<UUID, BigDecimal> tokens = new HashMap<>(); // Используем BigDecimal для токенов
    private HashMap<Block, UUID> brokenBlocks = new HashMap<>();
    private Economy economy;
    private PlayerScoreboardManager scoreboardManager;
    private WeatherManager weatherManager;
    private MobSpawnManager mobSpawnManager;
    private CustomHoeManager customHoeManager;
    private RebirthManager rebirthManager;
    private HungerManager hungerManager;
    private HoeManager hoeManager;
    private HoeShopGUI hoeShopGUI;
    private LegendaryChestManager legendaryChestManager; // Добавляем менеджер легендарных сундуков
    private Random random = new Random(); // Для генерации случайных чисел

    @Override
    public void onEnable() {
        if (!setupEconomy()) {
            getLogger().severe("Vault не найден! Плагин будет отключен.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        Bukkit.getPluginManager().registerEvents(this, this);

        // Инициализация менеджеров
        scoreboardManager = new PlayerScoreboardManager(this, economy);
        weatherManager = new WeatherManager(this);
        mobSpawnManager = new MobSpawnManager(this);
        customHoeManager = new CustomHoeManager(this);
        hoeManager = new HoeManager(this, hoeLevels); // Передаем this и hoeLevels
        rebirthManager = new RebirthManager(this, economy, rebirthLevels, tokens, hoeLevels, scoreboardManager);
        hungerManager = new HungerManager(this);
        hoeShopGUI = new HoeShopGUI(this, hoeManager, scoreboardManager);
        // Передаем this (Main) и economy в LegendaryChestManager
        legendaryChestManager = new LegendaryChestManager(this, economy);

        // Создание легендарного сундука на определённых координатах
        Location chestLocation = new Location(Bukkit.getWorld("world"), 112, 103, -117); // Пример координат
        legendaryChestManager.createLegendaryChest(chestLocation);

        // Регистрация HoeListener
        getServer().getPluginManager().registerEvents(new HoeListener(hoeManager), this);

        // Регистрация команд
        getCommand("upgradehoe").setExecutor(new HoeUpgradeGUI(this, economy, hoeLevels, scoreboardManager, hoeManager)); // Передаем hoeManager
        getCommand("cogdafeodal").setExecutor(rebirthManager);
        getCommand("hoeshop").setExecutor(hoeShopGUI);
        getCommand("mo").setExecutor(new AdminCommands(this, economy, hoeManager, scoreboardManager)); // Передаем this (Main)
        getCommand("to").setExecutor(new AdminCommands(this, economy, hoeManager, scoreboardManager)); // Передаем this (Main)
        getCommand("createlegendarychest").setExecutor(new CreateLegendaryChestCommand(legendaryChestManager)); // Регистрируем команду для создания сундуков

        getLogger().info("GrassSimulator enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("GrassSimulator disabled!");
    }

    private boolean setupEconomy() {
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            getLogger().severe("Vault не найден! Плагин будет отключен.");
            return false;
        }
        economy = rsp.getProvider();
        return economy != null;
    }

    // Метод для обновления токенов
    public void setTokens(UUID playerId, BigDecimal amount) {
        tokens.put(playerId, amount);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        ItemStack item = player.getInventory().getItemInMainHand();


        if (block.getType() == Material.GRASS && item.getType().toString().endsWith("_HOE")) {
            event.setDropItems(false);

            UUID playerId = player.getUniqueId();
            int hoeLevel = hoeLevels.getOrDefault(playerId, 1);
            int rebirthLevel = rebirthLevels.getOrDefault(playerId, 0);

            // Учитываем множитель мотыги-легенды
            String activeHoe = hoeManager.getActiveHoe(playerId);
            BigDecimal hoeMultiplier = new BigDecimal("2").pow(hoeLevel);
            BigDecimal rebirthMultiplier = new BigDecimal(rebirthLevel + 1);
            BigDecimal totalMultiplier = hoeMultiplier.multiply(rebirthMultiplier);

            if (activeHoe != null && activeHoe.equals("Легенда")) {
                totalMultiplier = totalMultiplier.multiply(new BigDecimal("2"));
            }

            BigDecimal baseIncome = new BigDecimal("10");
            BigDecimal money = totalMultiplier.multiply(baseIncome);

            economy.depositPlayer(player, money.doubleValue());
            player.sendMessage("§aВы получили " + Main.formatNumber(money) + " монет!");

            // Применение эффектов мотыги
            hoeManager.applyHoeEffects(player);

            player.spawnParticle(Particle.VILLAGER_HAPPY, block.getLocation().add(0.5, 0.5, 0.5), 10, 0.3, 0.3, 0.3, 0.1);
            player.playSound(block.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);

            brokenBlocks.put(block, playerId);
            new BukkitRunnable() {
                @Override
                public void run() {
                    block.setType(Material.GRASS);
                    brokenBlocks.remove(block);
                }
            }.runTaskLater(this, 40);

            scoreboardManager.updateScoreboard(player); // Обновляем скорборд

            // Шанс 0.01% на выпадение ключа
            if (random.nextDouble() < 1) {
                ItemStack key = new ItemStack(Material.TRIPWIRE_HOOK);
                ItemMeta meta = key.getItemMeta();
                meta.setDisplayName("§aКлюч от сундука");
                key.setItemMeta(meta);

                player.getInventory().addItem(key);
                player.sendMessage("§aВам выпал ключ от легендарного сундука!");
            }

        } else {
            event.setCancelled(true); // Запрещаем ломать что-либо кроме травы
        }
    }

    @EventHandler
    public void onHungerDeplete(PlayerItemDamageEvent event) {
        if (event.getItem().getType().toString().endsWith("_HOE")) {
            event.setCancelled(true); // Отключаем трату голода
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        customHoeManager.giveCustomHoe(player); // Выдаем кастомную мотыгу
        hoeManager.giveHoe(player, "Обычная", 1); // Выдаем обычную мотыгу с уровнем 1
    }

    public static String formatNumber(BigDecimal number) {
        String[] suffixes = {"", "K", "M", "B", "T", "Qa", "Qi", "aa", "ab", "ac", "ad", "ae", "af", "ag", "ah", "ai", "aj", "ak", "al", "am", "an", "ao", "ap", "aq", "ar", "as", "at", "au", "av", "aw", "ax", "ay", "az"};
        int suffixIndex = 0;

        while (number.compareTo(new BigDecimal("999.9")) >= 0 && suffixIndex < suffixes.length - 1) {
            number = number.divide(new BigDecimal("999.9"), 2, BigDecimal.ROUND_HALF_UP);
            suffixIndex++;
        }

        return String.format("%.1f%s", number, suffixes[suffixIndex]);
    }

    public int getRebirthLevel(UUID playerId) {
        return rebirthLevels.getOrDefault(playerId, 0);
    }

    public BigDecimal getTokens(UUID playerId) {
        return tokens.getOrDefault(playerId, BigDecimal.ZERO);
    }

    // Метод для получения уровня мотыги
    public int getHoeLevel(UUID playerId) {
        return hoeLevels.getOrDefault(playerId, 1); // Возвращаем уровень мотыги или 1 по умолчанию
    }

    // Метод для получения экономики (для использования в LegendaryChestManager)
    public Economy getEconomy() {
        return economy;
    }

    public PlayerScoreboardManager getScoreboardManager() {
        return scoreboardManager;
    }
    public BigDecimal getBalance(UUID playerId) {
        return playerBalances.getOrDefault(playerId, BigDecimal.ZERO);
    }

    public void setBalance(UUID playerId, BigDecimal balance) {
        playerBalances.put(playerId, balance);
        // Синхронизируем с Vault
        economy.withdrawPlayer(Bukkit.getOfflinePlayer(playerId), economy.getBalance(Bukkit.getOfflinePlayer(playerId)));
        economy.depositPlayer(Bukkit.getOfflinePlayer(playerId), balance.doubleValue());
    }

}