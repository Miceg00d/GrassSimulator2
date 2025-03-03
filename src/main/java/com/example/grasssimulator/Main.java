package com.example.grasssimulator;

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
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Random;
import java.util.UUID;

public class Main extends JavaPlugin implements Listener {

    private DatabaseManager databaseManager;
    private TopPlayersDisplay topPlayersDisplay;

    private HashMap<UUID, Integer> hoeLevels = new HashMap<>();
    private HashMap<UUID, Integer> rebirthLevels = new HashMap<>();
    private HashMap<UUID, BigDecimal> tokens = new HashMap<>();
    private HashMap<Block, UUID> brokenBlocks = new HashMap<>();
    private CustomEconomy customEconomy;
    private PlayerScoreboardManager scoreboardManager;
    private WeatherManager weatherManager;
    private MobSpawnManager mobSpawnManager;
    private CustomHoeManager customHoeManager;
    private RebirthManager rebirthManager;
    private HungerManager hungerManager;
    private HoeManager hoeManager;
    private HoeShopGUI hoeShopGUI;
    private LegendaryChestManager legendaryChestManager;
    private Random random = new Random();

    @Override
    public void onEnable() {

        databaseManager = new DatabaseManager(this);
        customEconomy = new CustomEconomy(this);

        // Загрузка данных игроков при входе
        for (Player player : Bukkit.getOnlinePlayers()) {
            loadPlayerData(player);
        }
        // Инициализация менеджеров
        scoreboardManager = new PlayerScoreboardManager(this);
        weatherManager = new WeatherManager(this);
        mobSpawnManager = new MobSpawnManager(this);
        customHoeManager = new CustomHoeManager(this);
        hoeManager = new HoeManager(this, new HashMap<>());
        rebirthManager = new RebirthManager(this, rebirthLevels, tokens, hoeLevels, scoreboardManager);
        hungerManager = new HungerManager(this);
        hoeShopGUI = new HoeShopGUI(this, hoeManager, scoreboardManager);
        legendaryChestManager = new LegendaryChestManager(this);

        // Удаляем все старые TextDisplay в мире при запуске плагина
        legendaryChestManager.removeAllTextDisplaysInWorld();

        // Создание сундука при запуске плагина
        Location chestLocation = new Location(Bukkit.getWorld("world"), 112, 103, -117); // Координаты сундука
        legendaryChestManager.createLegendaryChest(chestLocation, null); // Передаем null, так как игрок не требуется

        Location displayLocation = new Location(Bukkit.getWorld("world"), 123, 108, -128);
        topPlayersDisplay = new TopPlayersDisplay(databaseManager, displayLocation, this);

        // Регистрация событий
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(new HoeListener(hoeManager), this);

        // Регистрация команд
        getCommand("upgradehoe").setExecutor(new HoeUpgradeGUI(this, hoeLevels, scoreboardManager, hoeManager));
        getCommand("cogdafeodal").setExecutor(rebirthManager);
        getCommand("hoeshop").setExecutor(hoeShopGUI);
        getCommand("mo").setExecutor(new AdminCommands(this, hoeManager, scoreboardManager));
        getCommand("to").setExecutor(new AdminCommands(this, hoeManager, scoreboardManager));
        getCommand("createlegendarychest").setExecutor(new CreateLegendaryChestCommand(legendaryChestManager));
        getCommand("balance").setExecutor(new BalanceCommand(this));
        getCommand("setbalance").setExecutor(new AdminCommands(this, hoeManager, scoreboardManager));
        getCommand("settokens").setExecutor(new AdminCommands(this, hoeManager, scoreboardManager));


        getLogger().info("GrassSimulator enabled!");
    }

    @Override
    public void onDisable() {
        // Сохранение данных игроков при выключении
        for (Player player : Bukkit.getOnlinePlayers()) {
            savePlayerData(player);
        }
        databaseManager.close();

        getLogger().info("GrassSimulator disabled!");
    }
    // Загрузка данных игрока из базы данных
    public void loadPlayerData(Player player) {
        UUID playerId = player.getUniqueId();
        PlayerStats stats = databaseManager.getPlayerStats(playerId);
        if (stats != null) {
            rebirthLevels.put(playerId, stats.getRebirths());
            tokens.put(playerId, stats.getTokens()); // Загружаем токены
            hoeLevels.put(playerId, stats.getHoeLevel());
            customEconomy.setBalance(playerId, stats.getBalance());
        } else {
            // Если данные игрока не найдены, создаем новые
            rebirthLevels.put(playerId, 0);
            tokens.put(playerId, BigDecimal.ZERO);
            hoeLevels.put(playerId, 1);
            customEconomy.setBalance(playerId, BigDecimal.ZERO);
            databaseManager.updatePlayerStats(playerId, player.getName(), 0, BigDecimal.ZERO, BigDecimal.ZERO, 1, "Обычная");
        }
    }

    // Сохранение данных игрока в базу данных
    public void savePlayerData(Player player) {
        UUID playerId = player.getUniqueId();
        String username = player.getName();
        int rebirths = getRebirthLevel(playerId);
        BigDecimal balance = getCustomEconomy().getBalance(playerId);
        BigDecimal tokens = getTokens(playerId);
        int hoeLevel = getHoeLevel(playerId); // Получаем уровень мотыги
        String activeHoe = "Обычная"; // Замените на реальное значение, если у вас есть система активной мотыги

        // Обновляем данные игрока в базе данных
        databaseManager.updatePlayerStats(playerId, username, rebirths, balance, tokens, hoeLevel, activeHoe);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();

        // Если блок — трава
        if (block.getType() == Material.GRASS) {
            ItemStack item = player.getInventory().getItemInMainHand();

            // Если игрок не оператор и не использует мотыгу, отменяем событие
            if (!player.isOp() && !item.getType().toString().endsWith("_HOE")) {
                event.setCancelled(true);
                player.sendMessage("§cВы можете ломать траву только мотыгой!");
                return;
            }

            // Если игрок использует мотыгу или это оператор, обрабатываем как обычно
            if (player.isOp() || item.getType().toString().endsWith("_HOE")) {
                event.setDropItems(false);

                UUID playerId = player.getUniqueId();
                int hoeLevel = hoeLevels.getOrDefault(playerId, 1);
                int rebirthLevel = rebirthLevels.getOrDefault(playerId, 0);

                BigDecimal hoeMultiplier = new BigDecimal("2").pow(hoeLevel);
                BigDecimal rebirthMultiplier = new BigDecimal(rebirthLevel + 1);
                BigDecimal totalMultiplier = hoeMultiplier.multiply(rebirthMultiplier);

                if (hoeManager.getActiveHoe(playerId).equals("Легенда")) {
                    totalMultiplier = totalMultiplier.multiply(new BigDecimal("2"));
                }

                BigDecimal baseIncome = new BigDecimal("10");
                BigDecimal money = totalMultiplier.multiply(baseIncome);

                // Проверяем, не превысит ли новый баланс максимальное значение
                BigDecimal currentBalance = customEconomy.getBalance(playerId);
                BigDecimal newBalance = currentBalance.add(money);

                if (newBalance.compareTo(CustomEconomy.getMaxBalance()) > 0) {
                    player.sendMessage("§cВы достигли максимального баланса (999.9az)!");
                    return;
                }

                customEconomy.deposit(playerId, money);
                player.sendMessage("§aВы получили " + Main.formatNumber(money) + " монет!");

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

                scoreboardManager.updateScoreboard(player);

                // Шанс 0.01% на выпадение ключа
                if (random.nextDouble() < 0.1) {
                    ItemStack key = new ItemStack(Material.TRIPWIRE_HOOK);
                    ItemMeta meta = key.getItemMeta();
                    meta.setDisplayName("§aКлюч от сундука");
                    key.setItemMeta(meta);

                    player.getInventory().addItem(key);
                    player.sendMessage("§aВам выпал ключ от легендарного сундука!");
                }
            }
        } else {
            // Если блок не трава, и игрок не оператор, отменяем событие
            if (!player.isOp()) {
                event.setCancelled(true);
                player.sendMessage("§cВы можете ломать только траву!");
            }
        }
    }

    @EventHandler
    public void onHungerDeplete(PlayerItemDamageEvent event) {
        if (event.getItem().getType().toString().endsWith("_HOE")) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        customHoeManager.giveCustomHoe(player);
        hoeManager.giveHoe(player, "Обычная", 1);
        loadPlayerData(player);
    }

    public static String formatNumber(BigDecimal number) {
        String[] suffixes = {"", "K", "M", "B", "T", "Qa", "Qi", "aa", "ab", "ac", "ad", "ae", "af", "ag", "ah", "ai", "aj", "ak", "al", "am", "an", "ao", "ap", "aq", "ar", "as", "at", "au", "av", "aw", "ax", "ay", "az"};
        int suffixIndex = 0;

        while (number.compareTo(new BigDecimal("999.9")) >= 0 && suffixIndex < suffixes.length - 1) {
            number = number.divide(new BigDecimal("1000"), 2, BigDecimal.ROUND_HALF_UP);
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
    public void setTokens(UUID playerId, BigDecimal amount) {
        tokens.put(playerId, amount);
        // Сохраняем данные в базе данных
        Player player = Bukkit.getPlayer(playerId);
        if (player != null) {
            savePlayerData(player); // Используем this.savePlayerData(player)
        }
    }
    public void setHoeLevel(UUID playerId, int level) {
        hoeLevels.put(playerId, level);

        // Сохраняем данные в базе данных
        Player player = Bukkit.getPlayer(playerId);
        if (player != null) {
            savePlayerData(player);
        }
    }

    public int getHoeLevel(UUID playerId) {
        return hoeLevels.getOrDefault(playerId, 1);
    }

    public CustomEconomy getCustomEconomy() {
        return customEconomy;
    }

    public PlayerScoreboardManager getScoreboardManager() {
        return scoreboardManager;
    }
    public HoeManager getHoeManager() {
        return hoeManager;
    }
}