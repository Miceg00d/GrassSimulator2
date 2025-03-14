package com.example.grasssimulator.managers;

import com.example.grasssimulator.Main;
import com.example.grasssimulator.gui.HoeUpgradeGUI;
import com.example.grasssimulator.gui.HoeShopGUI;
import com.example.grasssimulator.gui.RebirthGUI;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.Set;
import java.util.UUID;

public class MenuStarManager implements Listener {

    private final Main plugin;
    private final HoeShopGUI hoeShopGUI;
    private final HoeUpgradeGUI hoeUpgradeGUI;
    private final RebirthGUI rebirthGUI; // Используем RebirthGUI вместо RebirthManager

    public MenuStarManager(Main plugin, HoeShopGUI hoeShopGUI, HoeUpgradeGUI hoeUpgradeGUI, RebirthGUI rebirthGUI) {
        this.plugin = plugin;
        this.hoeShopGUI = hoeShopGUI;
        this.hoeUpgradeGUI = hoeUpgradeGUI;
        this.rebirthGUI = rebirthGUI; // Сохраняем RebirthGUI
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    // Создаем звезду Незера
    public ItemStack createMenuStar() {
        ItemStack star = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = star.getItemMeta();
        meta.setDisplayName("§6Меню");
        meta.setLore(Arrays.asList("§aНажмите, чтобы открыть меню"));
        meta.setUnbreakable(true);
        meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
        star.setItemMeta(meta);
        return star;
    }

    // Выдаем звезду Незера при входе игрока
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        player.getInventory().setItem(8, createMenuStar()); // Помещаем звезду в 9 слот
    }

    // Обработка клика правой кнопкой мыши на звезду
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        ItemStack item = event.getItem();
        if (item != null && item.getType() == Material.NETHER_STAR && item.getItemMeta().getDisplayName().equals("§6Меню")) {
            event.setCancelled(true); // Отменяем событие, чтобы звезда не использовалась как обычный предмет
            openMainMenu(event.getPlayer()); // Открываем главное меню
        }
    }

    // Открываем главное меню
    public void openMainMenu(Player player) {
        Inventory gui = Bukkit.createInventory(player, 9, "§6Главное меню");

        ItemStack petMenu = new ItemStack(Material.BONE);
        ItemMeta petMenuMeta = petMenu.getItemMeta();
        petMenuMeta.setDisplayName("§aУправление питомцами");
        petMenu.setItemMeta(petMenuMeta);

        // Создаем предметы для меню
        ItemStack hoeShop = new ItemStack(Material.GOLDEN_HOE);
        ItemMeta hoeShopMeta = hoeShop.getItemMeta();
        hoeShopMeta.setDisplayName("§aМагазин мотыг");
        hoeShop.setItemMeta(hoeShopMeta);

        ItemStack hoeUpgrade = new ItemStack(Material.DIAMOND_HOE);
        ItemMeta hoeUpgradeMeta = hoeUpgrade.getItemMeta();
        hoeUpgradeMeta.setDisplayName("§aПрокачка мотыги");
        hoeUpgrade.setItemMeta(hoeUpgradeMeta);

        ItemStack rebirth = new ItemStack(Material.GOLDEN_HELMET);
        ItemMeta rebirthMeta = rebirth.getItemMeta();
        rebirthMeta.setDisplayName("§aРебитх");
        rebirth.setItemMeta(rebirthMeta);

        // Размещаем предметы в инвентаре
        gui.setItem(2, hoeShop);
        gui.setItem(4, hoeUpgrade);
        gui.setItem(6, rebirth);
        gui.setItem(8, petMenu); // Добавляем кнопку для меню питомцев

        player.openInventory(gui);
    }

    // Обработка кликов в главном меню
    @EventHandler
    public void onMainMenuClick(InventoryClickEvent event) {
        if (event.getView().getTitle().equals("§6Главное меню")) {
            event.setCancelled(true); // Отменяем событие, чтобы предметы нельзя было взять

            Player player = (Player) event.getWhoClicked();
            ItemStack clickedItem = event.getCurrentItem();

            if (clickedItem != null) {
                switch (clickedItem.getType()) {
                    case GOLDEN_HOE:
                        hoeShopGUI.openShop(player); // Открываем магазин мотыг
                        break;
                    case DIAMOND_HOE:
                        hoeUpgradeGUI.openUpgradeMenu(player); // Открываем прокачку мотыги
                        break;
                    case GOLDEN_HELMET:
                        rebirthGUI.openRebirthMenu(player); // Используем RebirthGUI для открытия меню ребитха
                        break;
                    case BONE:
                        openPetMenu(player); // Открываем меню питомцев
                        break;
                }
            }
        }
    }
    public void openPetMenu(Player player) {
        Inventory gui = Bukkit.createInventory(player, 27, "§6Управление питомцами");

        UUID playerId = player.getUniqueId();
        PetManager.PetData petData = plugin.getPetManager().getPetData(playerId);

        // Получаем список всех выбитых питомцев
        Set<PetManager.PetType> ownedPets = plugin.getPetManager().getOwnedPets(playerId);

        // Если у игрока нет питомцев
        if (ownedPets.isEmpty()) {
            ItemStack noPetsItem = new ItemStack(Material.BARRIER);
            ItemMeta noPetsMeta = noPetsItem.getItemMeta();
            noPetsMeta.setDisplayName("§cНет доступных питомцев");
            noPetsItem.setItemMeta(noPetsMeta);
            gui.setItem(13, noPetsItem); // Размещаем барьер в центре GUI
        } else {
            // Если у игрока есть питомцы
            int slot = 10; // Начинаем с 10 слота (вторая строка)
            for (PetManager.PetType petType : ownedPets) {
                ItemStack petItem = new ItemStack(petType.getHeadMaterial());
                ItemMeta petMeta = petItem.getItemMeta();
                petMeta.setDisplayName("§a" + petType.name()); // Название питомца

                // Проверяем, одет ли питомец
                boolean isPetActive = plugin.getPetManager().isPetActive(playerId) &&
                        plugin.getPetManager().getActivePetType(playerId) == petType;
                petMeta.setLore(Arrays.asList(
                        isPetActive ? "§cПитомец одет. Хотите снять?" : "§aПитомец доступен. Хотите надеть?"
                ));
                petItem.setItemMeta(petMeta);
                gui.setItem(slot, petItem); // Размещаем питомца в GUI
                slot++;
            }
        }

        player.openInventory(gui);
    }

    @EventHandler
    public void onPetMenuClick(InventoryClickEvent event) {
        if (event.getView().getTitle().equals("§6Управление питомцев")) {
            event.setCancelled(true); // Отменяем событие, чтобы игрок не мог забрать предметы

            Player player = (Player) event.getWhoClicked();
            ItemStack clickedItem = event.getCurrentItem();

            if (clickedItem != null && clickedItem.getType() != Material.AIR) {
                UUID playerId = player.getUniqueId();
                PetManager.PetType clickedPetType = null;

                // Определяем, какой питомец был выбран
                for (PetManager.PetType petType : PetManager.PetType.values()) {
                    if (clickedItem.getType() == petType.getHeadMaterial()) {
                        clickedPetType = petType;
                        break;
                    }
                }

                if (clickedPetType != null) {
                    boolean isPetActive = plugin.getPetManager().isPetActive(playerId) &&
                            plugin.getPetManager().getActivePetType(playerId) == clickedPetType;

                    if (isPetActive) {
                        // Если питомец одет, снимаем его
                        plugin.getPetManager().removePet(player);
                        player.sendMessage("§cПитомец " + clickedPetType.name() + " снят.");
                    } else {
                        // Если питомец не одет, надеваем его
                        plugin.getPetManager().spawnPet(player, clickedPetType);
                        player.sendMessage("§aПитомец " + clickedPetType.name() + " надет.");
                    }

                    player.closeInventory(); // Закрываем GUI после действия
                }
            }
        }
    }

    // Запрет на выброс звезды
    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        ItemStack item = event.getItemDrop().getItemStack();
        if (item.getType() == Material.NETHER_STAR && item.getItemMeta().getDisplayName().equals("§6Меню")) {
            event.setCancelled(true); // Отменяем событие, чтобы звезду нельзя было выбросить
            event.getPlayer().sendMessage("§cВы не можете выбросить эту звезду!");
        }
    }
    // Запрет на перемещение звезды в инвентаре
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();
        ItemStack cursorItem = event.getCursor();

        // Проверяем, если игрок пытается переместить звезду
        if ((clickedItem != null && isMenuStar(clickedItem)) || (cursorItem != null && isMenuStar(cursorItem))) {
            event.setCancelled(true); // Отменяем событие, чтобы звезду нельзя было переместить
            player.updateInventory(); // Обновляем инвентарь, чтобы вернуть звезду на место
        }
    }

    // Метод для проверки, является ли предмет звездой меню
    private boolean isMenuStar(ItemStack item) {
        if (item != null && item.getType() == Material.NETHER_STAR && item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();
            return meta.getDisplayName().equals("§6Меню");
        }
        return false;
    }
}

