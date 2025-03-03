package com.example.grasssimulator;

import org.bukkit.plugin.java.JavaPlugin;

import java.math.BigDecimal;
import java.sql.*;
import java.util.*;

public class DatabaseManager {

    private Connection connection;

    public DatabaseManager(JavaPlugin plugin) {
        try {
            // Загружаем драйвер SQLite
            Class.forName("org.sqlite.JDBC");

            // Убедимся, что папка плагина существует
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs(); // Создаем папку, если её нет
            }

            // Подключаемся к базе данных (файл player_stats.db в папке плагина)
            String dbPath = plugin.getDataFolder() + "/player_stats.db";
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);

            // Создаем таблицу, если её нет
            createTable();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Создаем таблицу, если её нет
    private void createTable() {
        try (Statement stmt = connection.createStatement()) {
            // Создаем таблицу, если её нет
            stmt.execute("CREATE TABLE IF NOT EXISTS player_stats (" +
                    "uuid TEXT PRIMARY KEY, " +
                    "username TEXT NOT NULL, " +
                    "rebirths INTEGER DEFAULT 0, " +
                    "balance DECIMAL(20, 2) DEFAULT 0, " +
                    "tokens DECIMAL(20, 2) DEFAULT 0, " +
                    "hoe_level INTEGER DEFAULT 1, " +
                    "active_hoe TEXT DEFAULT 'Обычная')");

            // Проверяем наличие столбцов и добавляем их, если они отсутствуют
            addColumnIfNotExists("tokens", "DECIMAL(20, 2) DEFAULT 0");
            addColumnIfNotExists("hoe_level", "INTEGER DEFAULT 1");
            addColumnIfNotExists("active_hoe", "TEXT DEFAULT 'Обычная'");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Метод для добавления столбца, если он отсутствует
    private void addColumnIfNotExists(String columnName, String columnDefinition) {
        try (Statement stmt = connection.createStatement()) {
            // Проверяем, существует ли столбец
            ResultSet rs = stmt.executeQuery("PRAGMA table_info(player_stats)");
            boolean columnExists = false;
            while (rs.next()) {
                if (rs.getString("name").equalsIgnoreCase(columnName)) {
                    columnExists = true;
                    break;
                }
            }

            // Если столбец отсутствует, добавляем его
            if (!columnExists) {
                stmt.execute("ALTER TABLE player_stats ADD COLUMN " + columnName + " " + columnDefinition);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Обновляем данные игрока
    public void updatePlayerStats(UUID uuid, String username, int rebirths, BigDecimal balance, BigDecimal tokens, int hoeLevel, String activeHoe) {
        String query = "INSERT OR REPLACE INTO player_stats (uuid, username, rebirths, balance, tokens, hoe_level, active_hoe) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, uuid.toString());
            stmt.setString(2, username);
            stmt.setInt(3, rebirths);
            stmt.setBigDecimal(4, balance);
            stmt.setBigDecimal(5, tokens);
            stmt.setInt(6, hoeLevel);
            stmt.setString(7, activeHoe);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Получаем данные игрока
    public PlayerStats getPlayerStats(UUID uuid) {
        String query = "SELECT * FROM player_stats WHERE uuid = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, uuid.toString());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String username = rs.getString("username");
                int rebirths = rs.getInt("rebirths");
                BigDecimal balance = rs.getBigDecimal("balance");
                BigDecimal tokens = rs.getBigDecimal("tokens");
                int hoeLevel = rs.getInt("hoe_level");
                String activeHoe = rs.getString("active_hoe");
                return new PlayerStats(username, rebirths, balance, tokens, hoeLevel, activeHoe);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // Получаем топ игроков по ребитхам и балансу
    public List<Map.Entry<String, PlayerStats>> getTopPlayers() {
        List<Map.Entry<String, PlayerStats>> topPlayers = new ArrayList<>();
        String query = "SELECT username, rebirths, balance FROM player_stats ORDER BY rebirths DESC, balance DESC LIMIT 10";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                String username = rs.getString("username");
                int rebirths = rs.getInt("rebirths");
                BigDecimal balance = rs.getBigDecimal("balance");
                topPlayers.add(new AbstractMap.SimpleEntry<>(username, new PlayerStats(username, rebirths, balance, BigDecimal.ZERO, 1, "Обычная")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return topPlayers;
    }

    // Закрываем соединение с базой данных
    public void close() {
        try {
            if (connection != null) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
