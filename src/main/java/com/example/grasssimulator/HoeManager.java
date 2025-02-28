package com.example.grasssimulator;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class HoeManager {

    private HashMap<UUID, List<String>> purchasedHoes; // Хранит купленные мотыги для каждого игрока
    private HashMap<UUID, String> activeHoes; // Хранит активную мотыгу для каждого игрока
    private HashMap<UUID, BigDecimal> playerTokens; // Используем BigDecimal для токенов
    private HashMap<UUID, Integer> hoeLevels; // Уровни мотыг
    private Main plugin; // Добавляем ссылку на Main

    public HoeManager(Main plugin, HashMap<UUID, Integer> hoeLevels) {
        this.plugin = plugin;
        this.hoeLevels = hoeLevels; // Инициализируем hoeLevels
        this.purchasedHoes = new HashMap<>();
        this.activeHoes = new HashMap<>();
        this.playerTokens = new HashMap<>();
    }

    // Проверка, куплена ли мотыга
    public boolean hasHoe(Player player, String hoeType) {
        UUID playerId = player.getUniqueId();
        return purchasedHoes.containsKey(playerId) && purchasedHoes.get(playerId).contains(hoeType);
    }

    // Добавление мотыги в список купленных
    public void addPurchasedHoe(Player player, String hoeType) {
        UUID playerId = player.getUniqueId();
        purchasedHoes.computeIfAbsent(playerId, k -> new ArrayList<>()).add(hoeType);
    }

    // Получение списка купленных мотыг
    public List<String> getPurchasedHoes(Player player) {
        UUID playerId = player.getUniqueId();
        return purchasedHoes.getOrDefault(playerId, new ArrayList<>());
    }

    // Получение активной мотыги игрока
    public String getActiveHoe(UUID playerId) {
        return activeHoes.getOrDefault(playerId, "Обычная"); // Возвращаем активную мотыгу или "Обычная" по умолчанию
    }

    public int getHoeLevel(UUID playerId) {
        return hoeLevels.getOrDefault(playerId, 1); // Возвращаем уровень мотыги или 1 по умолчанию
    }

    public void setHoeLevel(UUID playerId, int level) {
        hoeLevels.put(playerId, level); // Устанавливаем уровень мотыги
    }

    // Выдача мотыги игроку
    public void giveHoe(Player player, String hoeType, int hoeLevel) {
        ItemStack hoe = createHoe(hoeType, hoeLevel); // Создаем мотыгу с указанным уровнем
        player.getInventory().setItem(0, hoe); // Выдаем в первый слот
        activeHoes.put(player.getUniqueId(), hoeType); // Устанавливаем активную мотыгу
        player.sendMessage("§aТеперь вы используете мотыгу: " + hoeType);
    }

    private ItemStack createHoe(String hoeType, int hoeLevel) {
        ItemStack hoe = new ItemStack(Material.DIAMOND_HOE);
        ItemMeta meta = hoe.getItemMeta();

        switch (hoeType) {
            case "Бустер":
                meta.setDisplayName("§6Мотыга-Бустер");
                break;
            case "Легенда":
                meta.setDisplayName("§cМотыга-Легенда");
                break;
            default:
                meta.setDisplayName("§fОбычная мотыга");
                break;
        }

        // Добавляем уровень мотыги в Lore
        List<String> lore = new ArrayList<>();
        lore.add("§§c§lУровень: " + hoeLevel); // Уровень мотыги
        meta.setLore(lore);

        hoe.setItemMeta(meta);
        return hoe;
    }

    // Применение эффектов мотыги
    public void applyHoeEffects(Player player) {
        String hoeType = activeHoes.get(player.getUniqueId());

        if (hoeType != null) {
            switch (hoeType) {
                case "Бустер":
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 200, 5)); // 10 секунд скорости
                    break;
                case "Легенда":
                    // Эффект уже учитывается в основном коде (2x множитель)
                    break;
            }
        }
    }

    // Покупка мотыги
    public boolean buyHoe(Player player, String hoeType, BigDecimal cost) {
        UUID playerId = player.getUniqueId();
        BigDecimal tokens = playerTokens.getOrDefault(playerId, BigDecimal.ZERO);

        if (tokens.compareTo(cost) >= 0) {
            playerTokens.put(playerId, tokens.subtract(cost));
            plugin.setTokens(playerId, tokens.subtract(cost));

            int hoeLevel = getHoeLevel(playerId); // Получаем текущий уровень мотыги
            giveHoe(player, hoeType, hoeLevel); // Передаем уровень мотыги
            return true;
        } else {
            player.sendMessage("§cУ вас недостаточно токенов для покупки этой мотыги!");
            return false;
        }
    }

    // Установка токенов (для админов)
    public void setTokens(Player player, BigDecimal amount) {
        UUID playerId = player.getUniqueId();
        playerTokens.put(playerId, amount);
        plugin.setTokens(playerId, amount);
    }

    // Получение токенов
    public BigDecimal getTokens(Player player) {
        return playerTokens.getOrDefault(player.getUniqueId(), BigDecimal.ZERO);
    }

    // Метод для проверки, является ли мотыга защищённой
    public boolean isProtectedHoe(ItemStack item) {
        if (item != null && item.getType().toString().endsWith("_HOE")) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null && meta.hasDisplayName()) {
                String displayName = meta.getDisplayName();
                return displayName.equals("§6Мотыга-Бустер") ||
                        displayName.equals("§cМотыга-Легенда") ||
                        displayName.equals("§fОбычная мотыга"); // Добавляем обычную мотыгу
            }
        }
        return false;
    }
}