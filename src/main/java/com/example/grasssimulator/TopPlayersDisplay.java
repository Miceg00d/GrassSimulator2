package com.example.grasssimulator;

import org.bukkit.Location;
import org.bukkit.entity.TextDisplay;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.joml.Vector3f;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class TopPlayersDisplay {

    private DatabaseManager databaseManager;
    private Location displayLocation;
    private TextDisplay textDisplay;

    public TopPlayersDisplay(DatabaseManager databaseManager, Location displayLocation, JavaPlugin plugin) {
        this.databaseManager = databaseManager;
        this.displayLocation = displayLocation;
        createDisplay();
        startUpdateTask(plugin);
    }

    // Создаем TextDisplay
    private void createDisplay() {
        if (displayLocation.getWorld() == null) return;

        // Удаляем старый TextDisplay, если он есть
        if (textDisplay != null) {
            textDisplay.remove();
        }

        // Создаем новый TextDisplay
        textDisplay = displayLocation.getWorld().spawn(displayLocation, TextDisplay.class);
        textDisplay.setText("§6Загрузка топа...");
        textDisplay.setAlignment(TextDisplay.TextAlignment.CENTER);
        textDisplay.setBackgroundColor(org.bukkit.Color.fromARGB(100, 0, 0, 0)); // Полупрозрачный фон
        textDisplay.setSeeThrough(true); // Видно через блоки
        textDisplay.setShadowed(true); // Тень текста
        textDisplay.setLineWidth(200); // Ширина текста

        // Настройка размера и ориентации текста
        Transformation transformation = textDisplay.getTransformation();
        transformation.getScale().set(new Vector3f(2.0f, 2.0f, 2.0f)); // Увеличиваем размер текста
        textDisplay.setTransformation(transformation);
    }

    // Обновляем текст топа
    private void updateDisplay() {
        if (textDisplay == null || textDisplay.isDead()) {
            createDisplay();
        }

        List<Map.Entry<String, PlayerStats>> topPlayers = databaseManager.getTopPlayers();

        StringBuilder topText = new StringBuilder("§6Топ игроков:\n");
        int position = 1;
        for (Map.Entry<String, PlayerStats> entry : topPlayers) {
            String username = entry.getKey();
            int rebirths = entry.getValue().getRebirths();
            BigDecimal balance = entry.getValue().getBalance();

            topText.append("§e")
                    .append(position)
                    .append(". §a")
                    .append(username)
                    .append(" §7- §c")
                    .append(rebirths)
                    .append(" рб §7- §b")
                    .append(Main.formatNumber(balance))
                    .append("\n");
            position++;
        }

        textDisplay.setText(topText.toString());
    }

    // Запускаем задачу для обновления текста каждые 5 минут
    private void startUpdateTask(JavaPlugin plugin) {
        new BukkitRunnable() {
            @Override
            public void run() {
                updateDisplay();
            }
        }.runTaskTimer(plugin, 0, 20 * 60 * 2); // Обновление каждые 5 минут
    }
}
