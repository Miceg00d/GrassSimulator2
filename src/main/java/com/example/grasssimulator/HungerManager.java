package com.example.grasssimulator;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class HungerManager implements Listener {

    public HungerManager(JavaPlugin plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (event.getEntity() instanceof Player) {
            event.setCancelled(true); // Отключаем изменение уровня голода
            Player player = (Player) event.getEntity();
            player.setFoodLevel(20); // Устанавливаем уровень голода на максимум
        }
    }
}
