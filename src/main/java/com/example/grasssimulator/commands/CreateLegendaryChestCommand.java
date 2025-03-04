package com.example.grasssimulator.commands;

import com.example.grasssimulator.managers.LegendaryChestManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CreateLegendaryChestCommand implements CommandExecutor {

    private LegendaryChestManager legendaryChestManager;

    public CreateLegendaryChestCommand(LegendaryChestManager legendaryChestManager) {
        this.legendaryChestManager = legendaryChestManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            Location chestLocation = new Location(Bukkit.getWorld("world"), 112, 103, -117); // Пример координат
            legendaryChestManager.createLegendaryChest(chestLocation, player); // Передаем оба аргумента
            player.sendMessage("§aЛегендарный сундук создан!");
            return true;
        } else {
            sender.sendMessage("§cЭта команда доступна только игрокам.");
            return false;
        }
    }
}

