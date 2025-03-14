package com.example.grasssimulator.managers;

import com.example.grasssimulator.Main;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
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

import java.util.*;

public class PetChestManager implements Listener {

    private Main plugin;
    private Random random;
    private Location chestLocation; // Координаты сундука с питомцами
    private Map<Location, TextDisplay> chestTextDisplays; // Храним TextDisplay для каждого сундука
    private Set<UUID> playersWithActiveInteraction = new HashSet<>(); // Храним UUID игроков, которые уже взаимодействуют с сундуком

    public PetChestManager(Main plugin, Location chestLocation) {
        this.plugin = plugin;
        this.random = new Random();
        this.chestLocation = chestLocation;
        this.chestTextDisplays = new HashMap<>(); // Инициализируем карту
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTitle().equals("§6Сундук с питомцами")) {
            event.setCancelled(true); // Отменяем событие, чтобы игрок не мог забрать предметы
        }
    }

    public void createPetChest(Location location) {
        location.getBlock().setType(Material.CHEST); // Устанавливаем блок сундука

        // Создаем TextDisplay для отображения информации о сундуке
        Location textLocation = location.clone().add(0.5, 1.3, 0.5); // Позиция текста над сундуком
        TextDisplay textDisplay = (TextDisplay) location.getWorld().spawnEntity(textLocation, EntityType.TEXT_DISPLAY);
        textDisplay.setText("§6Сундук с питомцами");
        textDisplay.setAlignment(TextDisplay.TextAlignment.CENTER);
        textDisplay.setBackgroundColor(org.bukkit.Color.fromARGB(255, 25, 25, 25)); // Черный фон
        textDisplay.setSeeThrough(true); // Текст виден через блоки
        textDisplay.setShadowed(false); // Тень текста
        textDisplay.setLineWidth(200); // Ширина текста

        // Настраиваем начальную ориентацию текста (поворот на 180 градусов)
        Transformation transformation = textDisplay.getTransformation();
        transformation.getLeftRotation().set(new AxisAngle4f((float) Math.PI, 0, 1, 0)); // Поворот на 180 градусов
        transformation.getScale().set(new Vector3f(0.8f, 0.8f, 0.8f)); // Масштаб текста
        textDisplay.setTransformation(transformation);

        // Сохраняем TextDisplay для сундука
        chestTextDisplays.put(location, textDisplay);
    }

    // Удаляем все TextDisplay в мире
    public void removeAllTextDisplaysInWorld() {
        for (TextDisplay textDisplay : chestTextDisplays.values()) {
            if (textDisplay != null && !textDisplay.isDead()) {
                textDisplay.remove(); // Удаляем TextDisplay
            }
        }
        chestTextDisplays.clear(); // Очищаем карту
        Bukkit.getLogger().info("Все TextDisplay в мире удалены.");
    }

    // Открываем GUI с информацией о шансах
    private void openPetChestGUI(Player player) {
        Inventory gui = Bukkit.createInventory(player, 27, "§6Сундук с питомцами");

        ItemStack infoItem = new ItemStack(Material.PAPER);
        ItemMeta meta = infoItem.getItemMeta();
        meta.setDisplayName("§aШансы выпадения:");
        meta.setLore(Arrays.asList(
                "§7Обычный питомец: 70%",
                "§7Эпический питомец: 25%",
                "§7Мифический питомец: 5%"
        ));
        infoItem.setItemMeta(meta);
        gui.setItem(13, infoItem);

        player.openInventory(gui);
    }

    // Выдача случайного питомца
    private void giveRandomPet(Player player) {
        double chance = random.nextDouble();
        PetManager.PetType petType;

        if (chance < 0.7) {
            petType = PetManager.PetType.COMMON;
        } else if (chance < 0.95) {
            petType = PetManager.PetType.EPIC;
        } else {
            petType = PetManager.PetType.MYTHIC;
        }

        player.sendTitle("§aВы получили питомца!", "§e" + petType.name(), 10, 70, 20);

        PetManager.PetData petData = new PetManager.PetData(petType);
        plugin.getDatabaseManager().updatePlayerStats(
                player.getUniqueId(),
                player.getName(),
                plugin.getRebirthLevel(player.getUniqueId()),
                plugin.getCustomEconomy().getBalance(player.getUniqueId()),
                plugin.getTokens(player.getUniqueId()),
                plugin.getHoeLevel(player.getUniqueId()),
                plugin.getHoeManager().getActiveHoe(player.getUniqueId()),
                plugin.getHoeManager().getPurchasedHoes(player.getUniqueId()),
                petData
        );
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
        if (event.getClickedBlock() != null && event.getClickedBlock().getType() == Material.CHEST) {
            Location location = event.getClickedBlock().getLocation();
            String chestType = plugin.getChestType(location);

            // Проверяем, что это сундук с питомцами
            if (chestType.equals("PET")) {
                event.setCancelled(true); // Отменяем стандартное действие

                if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                    openPetChestGUI(event.getPlayer()); // Открываем GUI с информацией о питомцах
                } else if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
                    // Проверяем, что ключ находится в активной руке игрока
                    ItemStack itemInHand = event.getPlayer().getInventory().getItemInMainHand();
                    if (itemInHand != null && itemInHand.getType() == Material.ARROW) {
                        if (itemInHand.getAmount() > 0) {
                            itemInHand.setAmount(itemInHand.getAmount() - 1); // Убираем один ключ из руки
                            giveRandomPet(event.getPlayer()); // Выдаем случайного питомца
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

    // Проверка наличия ключа (стрелы)
    private boolean hasPetKey(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == Material.ARROW) {
                return true;
            }
        }
        return false;
    }

    // Удаление ключа (стрелы)
    private void removePetKey(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == Material.ARROW) {
                item.setAmount(item.getAmount() - 1);
                break;
            }
        }
    }
}
