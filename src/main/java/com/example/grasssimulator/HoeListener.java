package com.example.grasssimulator;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;

public class HoeListener implements Listener {

    private HoeManager hoeManager;

    public HoeListener(HoeManager hoeManager) {
        this.hoeManager = hoeManager;
    }

    // Запрет выкидывания мотыг
    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        ItemStack item = event.getItemDrop().getItemStack();
        if (hoeManager.isProtectedHoe(item)) { // Проверяем, является ли предмет защищённой мотыгой
            event.setCancelled(true);
            event.getPlayer().sendMessage("§cВы не можете выкинуть эту мотыгу!");
        }
    }

    // Запрет перемещения мотыг в инвентаре
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem != null && hoeManager.isProtectedHoe(clickedItem)) {
            event.setCancelled(true); // Запрещаем перемещение защищённых мотыг
            event.getWhoClicked().sendMessage("§cВы не можете перемещать эту мотыгу!");
        }
    }
}
