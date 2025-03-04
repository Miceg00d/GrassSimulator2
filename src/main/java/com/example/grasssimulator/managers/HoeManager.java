package com.example.grasssimulator.managers;

import com.example.grasssimulator.Main;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.math.BigDecimal;
import java.util.*;

public class HoeManager {

    private final Map<UUID, Set<String>> purchasedHoes = new HashMap<>();
    private HashMap<UUID, BigDecimal> playerTokens;
    private HashMap<UUID, Integer> hoeLevels;
    private Main plugin;
    private final Map<UUID, String> activeHoes = new HashMap<>();

    public HoeManager(Main plugin, HashMap<UUID, Integer> hoeLevels) {
        this.plugin = plugin;
        this.hoeLevels = hoeLevels;
        this.playerTokens = new HashMap<>();
    }

    public boolean hasHoe(Player player, String hoeType) {
        UUID playerId = player.getUniqueId();
        return purchasedHoes.containsKey(playerId) && purchasedHoes.get(playerId).contains(hoeType);
    }

    public void addPurchasedHoe(Player player, String hoeType) {
        UUID playerId = player.getUniqueId();
        purchasedHoes.computeIfAbsent(playerId, k -> new HashSet<>()).add(hoeType); // Теперь Set<String>
    }

    public Set<String> getPurchasedHoes(UUID playerId) {
        return purchasedHoes.getOrDefault(playerId, new HashSet<>());
    }


    public int getHoeLevel(UUID playerId) {
        return hoeLevels.getOrDefault(playerId, 1);
    }

    public void setHoeLevel(UUID playerId, int level) {
        hoeLevels.put(playerId, level);
        plugin.getDatabaseManager().updateHoeLevel(playerId, level); // ✅ Сохраняем правильный уровень
    }



    public void giveHoe(Player player, String hoeType, int hoeLevel) {
        UUID playerId = player.getUniqueId();

        // Загружаем правильный уровень перед выдачей
        int correctHoeLevel = hoeLevels.getOrDefault(playerId, 1); // Используем актуальный уровень из памяти

        ItemStack hoe = new ItemStack(Material.DIAMOND_HOE);
        ItemMeta meta = hoe.getItemMeta();

        meta.setDisplayName("§a" + hoeType + " мотыга");

        List<String> lore = new ArrayList<>();
        lore.add("§7Уровень: §e" + hoeLevel); // ✅ Показываем правильный уровень
        meta.setLore(lore);

        hoe.setItemMeta(meta);
        player.getInventory().setItem(0, hoe); // Кладём мотыгу в 1-й слот

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

        List<String> lore = new ArrayList<>();
        lore.add("§c§lУровень: " + hoeLevel); // Загружаем уровень из базы!
        meta.setLore(lore);

        hoe.setItemMeta(meta);
        return hoe;
    }

    public void applyHoeEffects(Player player) {
        String hoeType = activeHoes.get(player.getUniqueId());

        if (hoeType != null) {
            switch (hoeType) {
                case "Бустер":
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 200, 5));
                    break;
                case "Легенда":
                    break;
            }
        }
    }

    public boolean buyHoe(Player player, String hoeType, BigDecimal cost) {
        UUID playerId = player.getUniqueId();
        BigDecimal tokens = plugin.getTokens(playerId); // Используем актуальные данные из плагина

        if (tokens.compareTo(cost) >= 0) {
            BigDecimal newTokens = tokens.subtract(cost);
            plugin.setTokens(playerId, newTokens); // Сразу сохраняем новые токены
            setActiveHoe(playerId, hoeType); // Сохраняем новую мотыгу

            int hoeLevel = plugin.getHoeLevel(playerId); // Загружаем правильный уровень мотыги!
            giveHoe(player, hoeType, hoeLevel); // Теперь выдаётся мотыга с сохранённым уровнем

            plugin.savePlayerData(player); // Сохраняем покупку

            player.sendMessage("§aВы успешно купили мотыгу " + hoeType + "!");
            return true;
        } else {
            player.sendMessage("§cУ вас недостаточно токенов для покупки!");
            return false;
        }
    }

    public void setTokens(Player player, BigDecimal amount) {
        UUID playerId = player.getUniqueId();
        playerTokens.put(playerId, amount);
        plugin.setTokens(playerId, amount);
    }

    public BigDecimal getTokens(Player player) {
        return playerTokens.getOrDefault(player.getUniqueId(), BigDecimal.ZERO);
    }

    public boolean isProtectedHoe(ItemStack item) {
        if (item != null && item.getType().toString().endsWith("_HOE")) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null && meta.hasDisplayName()) {
                String displayName = meta.getDisplayName();
                return displayName.equals("§6Мотыга-Бустер") ||
                        displayName.equals("§cМотыга-Легенда") ||
                        displayName.equals("§fОбычная мотыга");
            }
        }
        return false;
    }
    public void setActiveHoe(UUID playerId, String hoeType) {
        activeHoes.put(playerId, hoeType);
    }

    public String getActiveHoe(UUID playerId) {
        return activeHoes.getOrDefault(playerId, "Обычная"); // Если нет данных, по умолчанию "Обычная"
    }
    public void loadPurchasedHoes(UUID playerId) {
        purchasedHoes.putIfAbsent(playerId, new HashSet<>()); // Теперь создаётся HashSet<String>
    }
    public void setPurchasedHoes(UUID playerId, Set<String> hoes) {
        purchasedHoes.put(playerId, hoes);
    }
}