package com.example.grasssimulator;

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
            legendaryChestManager.createLegendaryChest(player.getLocation()); // Создаем сундук на месте игрока
            player.sendMessage("§aЛегендарный сундук создан!");
            return true;
        }
        return false;
    }
}

