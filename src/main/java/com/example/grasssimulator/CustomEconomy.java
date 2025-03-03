package com.example.grasssimulator;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.UUID;

public class CustomEconomy {
    private Main plugin;
    private HashMap<UUID, BigDecimal> balances = new HashMap<>();
    private static final BigDecimal MAX_BALANCE = new BigDecimal("999.9e96"); // 999.9az

    public CustomEconomy(Main plugin) {
        this.plugin = plugin;
    }

    public static BigDecimal getMaxBalance() {
        return MAX_BALANCE;
    }

    public BigDecimal getBalance(UUID playerId) {
        return balances.getOrDefault(playerId, BigDecimal.ZERO);
    }

    public void setBalance(UUID playerId, BigDecimal balance) {
        if (balance.compareTo(MAX_BALANCE) > 0) {
            balance = MAX_BALANCE; // Ограничиваем баланс
        }
        balances.put(playerId, balance);
    }

    public void deposit(UUID playerId, BigDecimal amount) {
        BigDecimal currentBalance = getBalance(playerId);
        BigDecimal newBalance = currentBalance.add(amount);

        if (newBalance.compareTo(MAX_BALANCE) > 0) {
            newBalance = MAX_BALANCE; // Ограничиваем баланс
        }

        setBalance(playerId, newBalance);
        // Сохраняем данные в базе данных
        Player player = Bukkit.getPlayer(playerId);
        if (player != null) {
            plugin.savePlayerData(player);
        }
    }

    public void withdraw(UUID playerId, BigDecimal amount) {
        BigDecimal currentBalance = getBalance(playerId);
        BigDecimal newBalance = currentBalance.subtract(amount);

        if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
            newBalance = BigDecimal.ZERO; // Баланс не может быть отрицательным
        }

        setBalance(playerId, newBalance);
    }

    public boolean has(UUID playerId, BigDecimal amount) {
        return getBalance(playerId).compareTo(amount) >= 0;
    }

    public boolean transfer(UUID fromPlayerId, UUID toPlayerId, BigDecimal amount) {
        if (has(fromPlayerId, amount)) {
            withdraw(fromPlayerId, amount);
            deposit(toPlayerId, amount);
            return true;
        }
        return false;
    }
}
