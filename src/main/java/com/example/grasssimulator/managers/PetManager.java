package com.example.grasssimulator.managers;

import com.example.grasssimulator.Main;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.EulerAngle;

import java.util.*;

public class PetManager {

    private Main plugin;
    private Map<UUID, ArmorStand> playerPets = new HashMap<>();
    private Map<UUID, PetData> petData = new HashMap<>(); // Храним данные о питомцах игроков
    private Map<UUID, Set<PetType>> ownedPets = new HashMap<>(); // Храним выбитых питомцев для каждого игрока

    public PetManager(Main plugin) {
        this.plugin = plugin;
    }
    // Внутренний класс PetData
    public static class PetData {
        private PetType petType;
        private int level;

        public PetData(PetType petType) {
            this.petType = petType;
            this.level = 1; // Начальный уровень питомца
        }

        // Геттер для типа питомца
        public PetType getPetType() {
            return petType;
        }

        // Геттер для уровня питомца
        public int getLevel() {
            return level;
        }

        // Сеттер для уровня питомца
        public void setLevel(int level) {
            this.level = level;
        }
    }


    // Создание питомца
    public void spawnPet(Player player, PetType petType) {
        UUID playerId = player.getUniqueId();
        if (playerPets.containsKey(playerId)) {
            playerPets.get(playerId).remove(); // Удаляем старого питомца, если он есть
        }

        ArmorStand pet = (ArmorStand) player.getWorld().spawnEntity(player.getLocation(), EntityType.ARMOR_STAND);
        pet.setVisible(false);
        pet.setSmall(true);
        pet.setGravity(false);
        pet.setHelmet(new ItemStack(petType.getHeadMaterial()));
        pet.setHeadPose(new EulerAngle(0, 0, 0));

        playerPets.put(playerId, pet);

        // Запускаем задачу для перемещения питомца за игроком
        new BukkitRunnable() {
            @Override
            public void run() {
                if (pet.isDead() || !player.isOnline()) {
                    this.cancel();
                    return;
                }
                Location loc = player.getLocation().add(0, 2, 0);
                pet.teleport(loc);
            }
        }.runTaskTimer(plugin, 0, 1);
    }
    public PetData getPetData(UUID playerId) {
        return petData.getOrDefault(playerId, new PetData(PetType.COMMON)); // Возвращаем данные о питомце или создаем нового по умолчанию
    }


    // Удаление питомца
    public void removePet(Player player) {
        UUID playerId = player.getUniqueId();
        if (playerPets.containsKey(playerId)) {
            playerPets.get(playerId).remove();
            playerPets.remove(playerId);
        }
    }

    // Проверка, активен ли питомец
    public boolean isPetActive(UUID playerId) {
        return playerPets.containsKey(playerId);
    }

    // Типы питомцев
    public enum PetType {
        COMMON(Material.SKELETON_SKULL),
        EPIC(Material.ZOMBIE_HEAD),
        MYTHIC(Material.CREEPER_HEAD);

        private final Material headMaterial;

        PetType(Material headMaterial) {
            this.headMaterial = headMaterial;
        }

        public Material getHeadMaterial() {
            return headMaterial;
        }
    }
    // Добавляем питомца в список выбитых
    public void addOwnedPet(UUID playerId, PetType petType) {
        Set<PetType> pets = ownedPets.getOrDefault(playerId, new HashSet<>());
        pets.add(petType);
        ownedPets.put(playerId, pets);
    }

    // Получаем список выбитых питомцев
    public Set<PetType> getOwnedPets(UUID playerId) {
        return ownedPets.getOrDefault(playerId, new HashSet<>());
    }

    // Получаем тип активного питомца
    public PetType getActivePetType(UUID playerId) {
        if (playerPets.containsKey(playerId)) {
            return petData.get(playerId).getPetType();
        }
        return null;
    }
}

