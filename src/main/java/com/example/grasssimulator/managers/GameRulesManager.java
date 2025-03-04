package com.example.grasssimulator.managers;

import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public class GameRulesManager implements Listener {

    private JavaPlugin plugin;

    public GameRulesManager(JavaPlugin plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        startWeatherTask();
    }

    @EventHandler
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (event.getEntity() instanceof Player) {
            event.setCancelled(true);
            ((Player) event.getEntity()).setFoodLevel(20);
        }
    }

    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        event.setCancelled(true);
    }

    private void startWeatherTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (World world : plugin.getServer().getWorlds()) {
                    world.setStorm(false);
                    world.setThundering(false);
                    world.setClearWeatherDuration(Integer.MAX_VALUE);
                    world.setTime(6000);
                }
            }
        }.runTaskTimer(plugin, 0, 20 * 60 * 5);
    }
}
