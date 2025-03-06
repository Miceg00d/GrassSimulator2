package com.example.grasssimulator.quests;

import com.mojang.authlib.GameProfile;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.game.ClientboundAddPlayerPacket;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;

import java.lang.reflect.Field;

import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.bukkit.*;
import org.bukkit.boss.BossBar;
import org.bukkit.craftbukkit.v1_20_R1.CraftServer;
import org.bukkit.craftbukkit.v1_20_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_20_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
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

        GameProfile profile = new GameProfile(UUID.randomUUID(), "Майнер");
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
        npc.getBukkitEntity().setCustomName("§aКвестовый NPC"); // Важно для проверки в onNPCClick
        npc.getBukkitEntity().setCustomNameVisible(true);

        for (Player p : Bukkit.getOnlinePlayers()) {
            sendNPCToPlayer(p);
        }

        Bukkit.getLogger().info("[DEBUG] Спавним NPC на координатах: " + location);
        Bukkit.getLogger().info("[DEBUG] NPC UUID: " + npc.getUUID());
        Bukkit.getLogger().info("[DEBUG] NPC EntityID: " + npc.getId());
        if (npc.getBukkitEntity() == null) {
            Bukkit.getLogger().severe("Не удалось создать NPC!");
            return;
        }
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
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTaskLater(plugin, () -> sendNPCToPlayer(event.getPlayer()), 20L);
        injectPacketListener(player); // Перехватываем клик
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
    @EventHandler
    public void onNPCClick(EntityDamageByEntityEvent event) {
        Bukkit.getLogger().info("NPC Name: " + event.getEntity().getCustomName()); // Убедитесь, что имя установлено
        if (!(event.getDamager() instanceof Player player)) return;

        if (event.getEntity() instanceof org.bukkit.entity.Player bukkitNPC
                && bukkitNPC.getUniqueId().equals(npc.getUUID())) {

            event.setCancelled(true);
            player.sendMessage("§eВы кликнули по квестовому NPC!");
            handleNPCClick(player); // Вызовите метод выдачи квеста

        }
    }
    private void injectPacketListener(Player player) {
        ServerGamePacketListenerImpl connection = ((CraftPlayer) player).getHandle().connection;

        try {
            Field channelField = connection.getClass().getDeclaredField("channel");
            channelField.setAccessible(true);
            Object channel = channelField.get(connection);

            if (channel instanceof io.netty.channel.Channel) {
                ((io.netty.channel.Channel) channel).pipeline().addBefore("packet_handler", "npc_interact", new ChannelDuplexHandler() {
                    @Override
                    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                        if (msg instanceof ServerboundInteractPacket packet) {
                            Bukkit.getLogger().info("[DEBUG] Пакет взаимодействия получен!");

                            // Получаем entityId через рефлексию
                            int entityId = 0;
                            try {
                                Field entityIdField = packet.getClass().getDeclaredField("entityId");
                                entityIdField.setAccessible(true);
                                entityId = entityIdField.getInt(packet);
                            } catch (Exception e) {
                                e.printStackTrace();
                                return;
                            }

                            // Получаем цель через мир игрока
                            ServerPlayer target = ((CraftPlayer) player).getHandle().getLevel().getEntity(entityId);

                            if (target != null && target.getUUID().equals(npc.getUUID())) {
                                Bukkit.getLogger().info("[DEBUG] Игрок кликнул по NPC!");
                                handleNPCClick(player);
                                return;
                            } else {
                                Bukkit.getLogger().warning("[DEBUG] Цель не совпадает с NPC (Target ID: " + (target != null ? target.getId() : "null") + ")");
                            }
                        }
                        super.channelRead(ctx, msg);
                    }
                });
            }
        } catch (Exception e) {
            Bukkit.getLogger().severe("[ERROR] Ошибка при обработке пакета:");
            e.printStackTrace();
        }
    }

    private void handleNPCClick(Player player) {
        player.sendMessage(ChatColor.YELLOW + "Вы взяли квест у NPC!");
        Bukkit.getLogger().info("[DEBUG] Квест выдан игроку " + player.getName());
    }
}

