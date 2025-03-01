package com.example.grasssimulator;

import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public class WeatherManager {

    private JavaPlugin plugin;

    public WeatherManager(JavaPlugin plugin) {
        this.plugin = plugin;
        startWeatherTask();
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
