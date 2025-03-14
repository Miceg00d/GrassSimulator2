package com.example.grasssimulator.managers;

import com.example.grasssimulator.CustomEconomy;
import com.example.grasssimulator.Main;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.math.BigDecimal;
import java.util.*;

public class LegendaryChestManager implements Listener {

    private Main plugin;
    private Random random;
    private Map<Location, TextDisplay> chestTextDisplays; // Храним TextDisplay для каждого сундука
    private Map<TextDisplay, BukkitRunnable> rotationTasks; // Храним задачи для вращения текста
    private Set<UUID> playersWithActiveInteraction = new HashSet<>(); // Храним UUID игроков, которые уже взаимодействуют с сундуком

    public LegendaryChestManager(Main plugin) {
        this.plugin = plugin;
        this.random = new Random();
        this.chestTextDisplays = new HashMap<>();
        this.rotationTasks = new HashMap<>();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void createLegendaryChest(Location location, Player player) {
        // Устанавливаем блок сундука
        location.getBlock().setType(Material.CHEST);

        // Создаем первый текстовый дисплей над сундуком (лицевая сторона)
        Location textLocation1 = location.clone().add(0.5, 1.3, 0.5); // Позиция текста над сундуком
        TextDisplay textDisplay1 = (TextDisplay) location.getWorld().spawnEntity(textLocation1, EntityType.TEXT_DISPLAY);
        textDisplay1.setText("§6Легендарный сундук");
        textDisplay1.setAlignment(TextDisplay.TextAlignment.CENTER);

        // Устанавливаем черный фон (ARGB: 255, 0, 0, 0)
        textDisplay1.setBackgroundColor(org.bukkit.Color.fromARGB(255, 25, 25, 25));

        textDisplay1.setSeeThrough(true); // Текст виден через блоки
        textDisplay1.setShadowed(false); // Тень текста
        textDisplay1.setLineWidth(200); // Ширина текста

        // Настраиваем начальную ориентацию текста (лицевая сторона)
        Transformation transformation1 = textDisplay1.getTransformation();
        transformation1.getLeftRotation().set(new AxisAngle4f(0, 0, 1, 0)); // Без поворота
        transformation1.getScale().set(new Vector3f(0.8f, 0.8f, 0.8f)); // Масштаб текста
        textDisplay1.setTransformation(transformation1);

        // Создаем второй текстовый дисплей над сундуком (обратная сторона)
        Location textLocation2 = location.clone().add(0.5, 1.3, 0.5); // Та же позиция, но повернута на 180 градусов
        TextDisplay textDisplay2 = (TextDisplay) location.getWorld().spawnEntity(textLocation2, EntityType.TEXT_DISPLAY);
        textDisplay2.setText("§6Легендарный сундук");
        textDisplay2.setAlignment(TextDisplay.TextAlignment.CENTER);
        textDisplay2.setBackgroundColor(org.bukkit.Color.fromARGB(255, 25, 25, 25)); // Прозрачный фон
        textDisplay2.setSeeThrough(true); // Текст виден через блоки
        textDisplay2.setShadowed(false); // Тень текста
        textDisplay2.setLineWidth(200); // Ширина текста

        // Настраиваем начальную ориентацию текста (обратная сторона)
        Transformation transformation2 = textDisplay2.getTransformation();
        transformation2.getLeftRotation().set(new AxisAngle4f((float) Math.PI, 0, 1, 0)); // Поворот на 180 градусов
        transformation2.getScale().set(new Vector3f(0.8f, 0.8f, 0.8f)); // Масштаб текста
        textDisplay2.setTransformation(transformation2);

        // Сохраняем TextDisplay для сундука
        chestTextDisplays.put(location, textDisplay1);
        chestTextDisplays.put(location.clone().add(0, 0, 0.001), textDisplay2); // Небольшое смещение, чтобы тексты не конфликтовали


    }

    public void removeAllTextDisplaysInWorld() {
        // Проходим по всем сущностям в мире
        for (World world : plugin.getServer().getWorlds()) {
            for (org.bukkit.entity.Entity entity : world.getEntities()) {
                if (entity instanceof TextDisplay) {
                    entity.remove(); // Удаляем TextDisplay
                }
            }
        }
        Bukkit.getLogger().info("Все TextDisplay в мире удалены.");
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
        // Проверяем, что событие вызвано для основной руки (не для дополнительной)
        if (event.getHand() != EquipmentSlot.HAND) {
            return; // Игнорируем событие для дополнительной руки
        }

        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        // Проверяем, что игрок уже не взаимодействует с сундуком
        if (playersWithActiveInteraction.contains(playerId)) {
            return; // Игнорируем событие, если игрок уже взаимодействует
        }

        playersWithActiveInteraction.add(playerId); // Добавляем игрока в список активных взаимодействий
        if (event.getClickedBlock() != null && event.getClickedBlock().getType() == Material.CHEST) {
            Location location = event.getClickedBlock().getLocation();
            String chestType = plugin.getChestType(location);

            // Проверяем, что это легендарный сундук
            if (chestType.equals("LEGENDARY")) {
                event.setCancelled(true); // Отменяем стандартное действие

                if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                    openChestInfoGUI(event.getPlayer()); // Открываем GUI с информацией о наградах
                } else if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
                    // Проверяем, что ключ находится в активной руке игрока
                    ItemStack itemInHand = event.getPlayer().getInventory().getItemInMainHand();
                    if (itemInHand != null && itemInHand.getType() == Material.TRIPWIRE_HOOK && itemInHand.hasItemMeta() && itemInHand.getItemMeta().getDisplayName().equals("§aКлюч от сундука")) {
                        if (itemInHand.getAmount() > 0) {
                            itemInHand.setAmount(itemInHand.getAmount() - 1); // Убираем один ключ из руки
                            giveRandomReward(event.getPlayer()); // Выдаем случайную награду
                        } else {
                            event.getPlayer().sendMessage("§cУ вас нет ключа для открытия этого сундука!");
                        }
                    } else {
                        event.getPlayer().sendMessage("§cУ вас нет ключа для открытия этого сундука!");
                    }
                }
            }
        }
        // Убираем игрока из списка активных взаимодействий после завершения обработки
        new BukkitRunnable() {
            @Override
            public void run() {
                playersWithActiveInteraction.remove(playerId);
            }
        }.runTaskLater(plugin, 1); // Задержка в 1 тик
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

