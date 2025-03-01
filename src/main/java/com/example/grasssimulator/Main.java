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
        customEconomy = new CustomEconomy();

        // Инициализация менеджеров
        scoreboardManager = new PlayerScoreboardManager(this);
        weatherManager = new WeatherManager(this);
        mobSpawnManager = new MobSpawnManager(this);
        customHoeManager = new CustomHoeManager(this);
        hoeManager = new HoeManager(this, hoeLevels);
        rebirthManager = new RebirthManager(this, rebirthLevels, tokens, hoeLevels, scoreboardManager);
        hungerManager = new HungerManager(this);
        hoeShopGUI = new HoeShopGUI(this, hoeManager, scoreboardManager);
        legendaryChestManager = new LegendaryChestManager(this);

        // Создание легендарного сундука на координатах
        Location chestLocation = new Location(Bukkit.getWorld("world"), 112, 103, -117); // Пример координат
        legendaryChestManager.createLegendaryChest(chestLocation);

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

        getLogger().info("GrassSimulator enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("GrassSimulator disabled!");
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
        } else {
            event.setCancelled(true);
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
}