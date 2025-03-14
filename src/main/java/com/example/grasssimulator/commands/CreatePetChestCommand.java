package com.example.grasssimulator.commands;

import com.example.grasssimulator.managers.PetChestManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.Location;

public class CreatePetChestCommand implements CommandExecutor {

    private PetChestManager petChestManager;

    public CreatePetChestCommand(PetChestManager petChestManager) {
        this.petChestManager = petChestManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            Location chestLocation = player.getLocation(); // Создаем сундук на месте игрока
            petChestManager.createPetChest(chestLocation); // Передаем координаты
            player.sendMessage("§aСундук с питомцами создан!");
            return true;
        } else {
            sender.sendMessage("§cЭта команда доступна только игрокам.");
            return false;
        }
    }
}
