package com.example.grasssimulator.quests;

import com.mojang.authlib.GameProfile;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.bukkit.*;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.craftbukkit.v1_20_R1.CraftServer;
import org.bukkit.craftbukkit.v1_20_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_20_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class QuestNPC implements Listener {
    private final JavaPlugin plugin;
    private final Map<UUID, Integer> playerQuestStage = new HashMap<>();
    private final Map<UUID, Integer> playerProgress = new HashMap<>();
    private final Map<UUID, BossBar> playerBossBars = new HashMap<>();
    private final int[] questStages = {100, 500, 1000, 5000, 10000};
    private ServerPlayer npc;

    public QuestNPC(JavaPlugin plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void spawnNPC(Location location) {
        if (npc != null) {
            Bukkit.getLogger().info("[DEBUG] NPC уже существует! Пропускаем спавн.");
            return;
        }

        if (Bukkit.getOnlinePlayers().isEmpty()) {
            Bukkit.getLogger().warning("[DEBUG] Нет игроков на сервере! NPC не может быть создан.");
            return;
        }

        GameProfile profile = new GameProfile(UUID.randomUUID(), "§c§lМайнер");
        npc = new ServerPlayer(
                ((CraftServer) Bukkit.getServer()).getServer(),
                ((CraftWorld) location.getWorld()).getHandle(),
                profile
        );

        npc.setPos(location.getX(), location.getY(), location.getZ()); // Устанавливаем позицию

        // Проверка, что npc не null перед отправкой данных
        if (npc.getBukkitEntity() == null) {
            Bukkit.getLogger().warning("[DEBUG] Ошибка создания NPC! BukkitEntity = null");
            npc = null; // Обнуляем, чтобы не пытаться работать с ним дальше
            return;
        }

        npc.getBukkitEntity().setCustomNameVisible(true);

        for (Player p : Bukkit.getOnlinePlayers()) {
            sendNPCToPlayer(p);
        }

        Bukkit.getLogger().info("[DEBUG] Спавним NPC на координатах: " + location);
    }


    private void sendNPCToPlayer(Player player) {
        if (npc == null) {
            Bukkit.getLogger().warning("[DEBUG] NPC не существует! Отмена отправки.");
            return;
        }

        ServerGamePacketListenerImpl connection = ((CraftPlayer) player).getHandle().connection;

        Bukkit.getLogger().info("[DEBUG] Отправка NPC игроку: " + player.getName());

        // Удаляем NPC, если он был добавлен ранее
        connection.send(new ClientboundPlayerInfoRemovePacket(List.of(npc.getGameProfile().getId())));
        Bukkit.getLogger().info("[DEBUG] REMOVE_PLAYER отправлен");

        // Даем задержку перед добавлением
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            connection.send(new ClientboundPlayerInfoUpdatePacket(EnumSet.of(ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER), List.of(npc)));
            Bukkit.getLogger().info("[DEBUG] ADD_PLAYER отправлен");

            connection.send(new ClientboundAddPlayerPacket(npc));
            Bukkit.getLogger().info("[DEBUG] AddPlayerPacket отправлен");

            connection.send(new ClientboundTeleportEntityPacket(npc));
            Bukkit.getLogger().info("[DEBUG] TeleportEntityPacket отправлен");

            connection.send(new ClientboundSetEntityDataPacket(npc.getId(), npc.getEntityData().getNonDefaultValues()));
            Bukkit.getLogger().info("[DEBUG] SetEntityDataPacket отправлен");
        }, 10L); // ⏳ Даем 0.5 секунды задержки перед отправкой пакетов
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> sendNPCToPlayer(event.getPlayer()), 20L);
    }

    @EventHandler
    public void onNPCClick(PlayerInteractAtEntityEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        // Проверяем, что игрок кликнул по NPC (он выглядит как игрок)
        if (!(event.getRightClicked() instanceof CraftPlayer)) return;

        // Проверяем, что кликнули именно по нашему NPC
        CraftPlayer clickedPlayer = (CraftPlayer) event.getRightClicked();
        if (!clickedPlayer.getProfile().getName().equals("QuestNPC")) return;

        event.setCancelled(true); // Отключаем стандартное взаимодействие с "игроком"

        player.sendMessage("§eВы поговорили с §cКвестовым NPC§e!");

        int stage = playerQuestStage.getOrDefault(playerId, 0);
        if (stage >= questStages.length) {
            player.sendMessage("§aВы выполнили все квесты!");
            return;
        }

        player.sendMessage("§eВы взяли квест: сломать " + questStages[stage] + " травы!");
        playerProgress.put(playerId, 0);

        BossBar bossBar = Bukkit.createBossBar("Прогресс: 0/" + questStages[stage], BarColor.GREEN, BarStyle.SOLID);
        bossBar.addPlayer(player);
        playerBossBars.put(playerId, bossBar);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        if (event.getBlock().getType() != Material.GRASS) return;

        int stage = playerQuestStage.getOrDefault(playerId, 0);
        if (stage >= questStages.length) return;

        int progress = playerProgress.getOrDefault(playerId, 0) + 1;
        playerProgress.put(playerId, progress);

        BossBar bossBar = playerBossBars.get(playerId);
        if (bossBar != null) {
            bossBar.setTitle("Прогресс: " + progress + "/" + questStages[stage]);
            bossBar.setProgress((double) progress / questStages[stage]);
        }

        if (progress >= questStages[stage]) {
            player.sendMessage(ChatColor.GREEN + "Квест завершён! Следующий этап доступен.");
            playerQuestStage.put(playerId, stage + 1);
            bossBar.removeAll();
            playerBossBars.remove(playerId);
        }
    }
}
