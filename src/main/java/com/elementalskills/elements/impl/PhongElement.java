package com.elementalskills.elements.impl;

import com.elementalskills.elements.ElementalMastery;
import com.elementalskills.managers.SkillExecutionManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

public class PhongElement implements ElementalMastery {

    @Override
    public String getId() {
        return "phong";
    }

    @Override
    public String getDisplayName() {
        return "Phong";
    }

    @Override
    public NamedTextColor getThemeColor() {
        return NamedTextColor.GREEN;
    }

    @Override
    public Material getIcon() {
        return Material.WIND_CHARGE;
    }

    @Override
    public List<String> getOverviewLore() {
        return List.of(
                "Ưu điểm: Miễn nhiễm sát thương rơi & chạy nhanh liên tục.",
                "Nhược điểm: Giáp mỏng, nhận thêm 20% sát thương trực tiếp."
        );
    }

    @Override
    public String getSkillName(int slot) {
        return switch (slot) {
            case 1 -> "Đạp Gió Lên Mây";
            case 2 -> "Liệt Phong Trảm";
            default -> "Bão Tố Cuồng Nộ";
        };
    }

    @Override
    public int getSkillCooldown(int slot) {
        return switch (slot) {
            case 1 -> 6;
            case 2 -> 6;
            default -> 14;
        };
    }

    @Override
    public List<String> getSkillLore(int slot) {
        return switch (slot) {
            case 1 -> List.of("Lướt gió bay vọt lên không, sau đó phóng ra", "5 đạn Shulker tốc độ cao tự đuổi người chơi!");
            case 2 -> List.of("Phóng lưỡi dao gió bay xa 15 block hất tung địch.");
            default -> List.of("Tạo cơn lốc quanh bản thân hất văng địch & bật ngược đạn.");
        };
    }

    @Override
    public void tickEnvironment(Player player, SkillExecutionManager ctx) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 40, 1, true, false));
    }

    @Override
    public void modifyIncomingDamage(Player player, EntityDamageEvent event, SkillExecutionManager ctx) {
        if (event.getCause() == EntityDamageEvent.DamageCause.FALL) {
            event.setCancelled(true);
            return;
        }
        if (event instanceof EntityDamageByEntityEvent) {
            event.setDamage(event.getDamage() * 1.20);
        }
    }

    // Chiêu 1: Đạp Gió Lên Mây -> Sau khi lướt tạo ra 5 đạn Shulker đi nhanh tự động tấn công người chơi
    @Override
    public void castSkill1(Player player, SkillExecutionManager ctx) {
        Vector boost = player.getLocation().getDirection().multiply(1.35).setY(1.15);
        player.setVelocity(boost);
        player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation(), 25, 0.5, 0.2, 0.5, 0.1);
        SkillExecutionManager.playSoundSafe(player, "entity.breeze.jump", 1.1f);
        player.sendActionBar(Component.text("🍃 Đạp Gió Lên Mây!", NamedTextColor.GREEN, TextDecoration.BOLD));

        // Ngay sau nhịp lướt (6 ticks = 0.3s), triệu hồi 5 đạn Shulker tốc độ cao khóa người chơi
        Bukkit.getScheduler().runTaskLater(ctx.getPlugin(), () -> {
            if (!player.isOnline()) return;

            Location center = player.getEyeLocation();
            World w = center.getWorld();

            List<Player> enemyPlayers = new ArrayList<>();
            for (Entity e : w.getNearbyEntities(center, 26.0, 18.0, 26.0)) {
                if (e instanceof Player target
                        && target != player
                        && target.isValid()
                        && !target.isDead()
                        && target.getGameMode() != GameMode.SPECTATOR
                        && target.getGameMode() != GameMode.CREATIVE) {
                    enemyPlayers.add(target);
                }
            }

            enemyPlayers.sort(Comparator.comparingDouble(p -> p.getLocation().distanceSquared(center)));
            SkillExecutionManager.playSoundSafe(player, "entity.shulker.shoot", 1.3f);

            List<ShulkerBullet> bullets = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                double angle = Math.toRadians((360.0 / 5.0) * i);
                Location spawnLoc = center.clone().add(Math.cos(angle) * 1.4, 0.3, Math.sin(angle) * 1.4);
                Player assignedTarget = enemyPlayers.isEmpty() ? null : enemyPlayers.get(i % enemyPlayers.size());

                ShulkerBullet bullet = w.spawn(spawnLoc, ShulkerBullet.class, b -> {
                    b.setShooter(player);
                    if (assignedTarget != null) {
                        b.setTarget(assignedTarget);
                    }
                    b.getPersistentDataContainer().set(ctx.getWindBulletKey(), PersistentDataType.BYTE, (byte) 1);
                });
                bullets.add(bullet);
            }

            // Gia tốc liên tục mỗi tick để 5 đạn Shulker lao đi cực nhanh
            new BukkitRunnable() {
                int ticks = 0;

                @Override
                public void run() {
                    if (ticks > 70) {
                        for (ShulkerBullet b : bullets) {
                            if (b.isValid()) b.remove();
                        }
                        cancel();
                        return;
                    }

                    boolean anyAlive = false;
                    for (ShulkerBullet b : bullets) {
                        if (!b.isValid() || b.isDead()) continue;
                        anyAlive = true;

                        b.getWorld().spawnParticle(Particle.CLOUD, b.getLocation(), 2, 0.05, 0.05, 0.05, 0.01);

                        if (b.getTarget() instanceof Player target && target.isOnline() && !target.isDead()
                                && target.getWorld().equals(b.getWorld())) {
                            Vector toTarget = target.getEyeLocation().toVector().subtract(b.getLocation().toVector());
                            if (toTarget.lengthSquared() > 0.01) {
                                b.setVelocity(toTarget.normalize().multiply(1.35));
                            }
                        } else if (b.getVelocity().lengthSquared() > 0.01) {
                            b.setVelocity(b.getVelocity().normalize().multiply(1.25));
                        }
                    }

                    if (!anyAlive) {
                        cancel();
                        return;
                    }
                    ticks++;
                }
            }.runTaskTimer(ctx.getPlugin(), 1L, 1L);
        }, 6L);
    }

    @Override
    public void castSkill2(Player player, SkillExecutionManager ctx) {
        Location start = player.getEyeLocation();
        Vector dir = start.getDirection().normalize();
        SkillExecutionManager.playSoundSafe(player, "entity.player.attack.sweep", 1.3f);
        player.sendActionBar(Component.text("🌪 Liệt Phong Trảm!", NamedTextColor.GREEN, TextDecoration.BOLD));

        new BukkitRunnable() {
            int step = 0;
            final Location cur = start.clone();
            final Set<UUID> hit = new HashSet<>();

            @Override
            public void run() {
                if (step > 15) {
                    cancel();
                    return;
                }
                cur.add(dir.clone().multiply(1.1));
                World w = cur.getWorld();
                w.spawnParticle(Particle.SWEEP_ATTACK, cur, 2, 0.3, 0.2, 0.3, 0);
                w.spawnParticle(Particle.CLOUD, cur, 5, 0.25, 0.25, 0.25, 0.04);

                for (Entity e : w.getNearbyEntities(cur, 2.0, 2.0, 2.0)) {
                    if (e instanceof LivingEntity le && le != player && hit.add(le.getUniqueId())) {
                        le.damage(ctx.calculateFinalSkillDamage(player, le, 7.5), player);
                        le.setVelocity(dir.clone().multiply(0.8).setY(0.65));
                    }
                }
                if (!cur.getBlock().isPassable()) {
                    cancel();
                }
                step++;
            }
        }.runTaskTimer(ctx.getPlugin(), 0L, 1L);
    }

    @Override
    public void castSkill3(Player player, SkillExecutionManager ctx) {
        SkillExecutionManager.playSoundSafe(player, "entity.breeze.whirl", 1.0f);
        player.sendActionBar(Component.text("🌪 Bão Tố Cuồng Nộ!", NamedTextColor.GREEN, TextDecoration.BOLD));

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (!player.isOnline() || ticks > 80) {
                    cancel();
                    return;
                }
                Location c = player.getLocation();
                World w = c.getWorld();

                for (int angle = 0; angle < 360; angle += 20) {
                    double rad = Math.toRadians(angle + ticks * 12);
                    w.spawnParticle(Particle.CLOUD, c.clone().add(Math.cos(rad) * 5.0, (ticks % 20) * 0.15, Math.sin(rad) * 5.0), 1, 0, 0, 0, 0.02);
                }

                for (Entity e : w.getNearbyEntities(c, 5.5, 4.0, 5.5)) {
                    if (e instanceof Projectile proj && proj.getShooter() != player) {
                        proj.setVelocity(proj.getVelocity().multiply(-1.3));
                        proj.setShooter(player);
                    } else if (e instanceof LivingEntity le && le != player && ticks % 15 == 0) {
                        le.damage(ctx.calculateFinalSkillDamage(player, le, 3.5), player);
                        le.setVelocity(new Vector(0, 0.75, 0));
                    }
                }
                ticks += 5;
            }
        }.runTaskTimer(ctx.getPlugin(), 0L, 5L);
    }
}
