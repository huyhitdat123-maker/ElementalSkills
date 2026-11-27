package com.elementalskills.elements.impl;

import com.elementalskills.elements.ElementalMastery;
import com.elementalskills.managers.SkillExecutionManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class ThuyElement implements ElementalMastery {

    @Override
    public String getId() {
        return "thuy";
    }

    @Override
    public String getDisplayName() {
        return "Thủy";
    }

    @Override
    public NamedTextColor getThemeColor() {
        return NamedTextColor.AQUA;
    }

    @Override
    public Material getIcon() {
        return Material.HEART_OF_THE_SEA;
    }

    @Override
    public List<String> getOverviewLore() {
        return List.of(
                "Ưu điểm: Dưới nước hoặc trời mưa giảm 25% hồi chiêu & tăng tốc.",
                "Nhược điểm: Ở Nether bị giảm 25% sát thương và tăng 25% hồi chiêu."
        );
    }

    @Override
    public String getSkillName(int slot) {
        return switch (slot) {
            case 1 -> "Thủy Triều Xung Kích";
            case 2 -> "Thủy Long Đạn";
            default -> "Đại Hồng Thủy";
        };
    }

    @Override
    public int getSkillCooldown(int slot) {
        return switch (slot) {
            case 1 -> 6;
            case 2 -> 5;
            default -> 14;
        };
    }

    @Override
    public List<String> getSkillLore(int slot) {
        return switch (slot) {
            case 1 -> List.of("Lướt sóng về phía trước, cuốn trôi & đẩy lùi địch.");
            case 2 -> List.of("Bắn tia nước áp lực làm chậm địch hoặc dập lửa & hồi máu đồng minh.");
            default -> List.of("Triệu hồi xoáy nước 6 block làm chậm địch và hồi máu bản thân.");
        };
    }

    private boolean isNetherOrHot(Player player) {
        return player.getWorld().getEnvironment() == World.Environment.NETHER;
    }

    @Override
    public void tickEnvironment(Player player, SkillExecutionManager ctx) {
        if (isNetherOrHot(player)) {
            ctx.setCooldownMultiplier(player, 1.25);
            return;
        }
        if (player.isInWater() || player.getWorld().hasStorm()) {
            ctx.setCooldownMultiplier(player, 0.75);
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 40, 1, true, false));
            player.addPotionEffect(new PotionEffect(PotionEffectType.DOLPHINS_GRACE, 40, 0, true, false));
        }
    }

    @Override
    public double modifyOutgoingSkillDamage(Player player, LivingEntity target, double baseDamage, SkillExecutionManager ctx) {
        return isNetherOrHot(player) ? baseDamage * 0.75 : baseDamage;
    }

    @Override
    public void castSkill1(Player player, SkillExecutionManager ctx) {
        Vector dir = player.getLocation().getDirection().setY(0);
        if (dir.lengthSquared() < 0.01) dir = new Vector(0, 0, 1);
        dir.normalize();
        player.setVelocity(dir.clone().multiply(1.75).setY(0.25));

        SkillExecutionManager.playSoundSafe(player, "item.trident.riptide_2", 1.1f);
        player.sendActionBar(Component.text("🌊 Thủy Triều Xung Kích!", NamedTextColor.AQUA, TextDecoration.BOLD));

        Vector finalDir = dir;
        new BukkitRunnable() {
            int ticks = 0;
            final Set<UUID> hit = new HashSet<>();

            @Override
            public void run() {
                if (!player.isOnline() || ticks > 12) {
                    cancel();
                    return;
                }
                Location c = player.getLocation().add(0, 0.8, 0);
                c.getWorld().spawnParticle(Particle.SPLASH, c, 20, 0.8, 0.4, 0.8, 0.1);
                c.getWorld().spawnParticle(Particle.BUBBLE, c, 10, 0.6, 0.4, 0.6, 0.05);

                for (Entity e : c.getWorld().getNearbyEntities(c, 2.5, 2.0, 2.5)) {
                    if (e instanceof LivingEntity le && le != player && hit.add(le.getUniqueId())) {
                        le.damage(ctx.calculateFinalSkillDamage(player, le, 6.5), player);
                        le.setVelocity(finalDir.clone().multiply(1.4).setY(0.4));
                        le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 50, 2));
                    }
                }
                ticks++;
            }
        }.runTaskTimer(ctx.getPlugin(), 0L, 1L);
    }

    @Override
    public void castSkill2(Player player, SkillExecutionManager ctx) {
        Location start = player.getEyeLocation();
        Vector dir = start.getDirection().normalize();
        SkillExecutionManager.playSoundSafe(player, "entity.dolphin.splash", 1.2f);
        player.sendActionBar(Component.text("💧 Thủy Long Đạn!", NamedTextColor.BLUE, TextDecoration.BOLD));

        new BukkitRunnable() {
            int step = 0;
            final Location cur = start.clone();
            final Set<UUID> hit = new HashSet<>();

            @Override
            public void run() {
                if (step > 18) {
                    cancel();
                    return;
                }
                cur.add(dir.clone().multiply(1.1));
                World w = cur.getWorld();
                w.spawnParticle(Particle.SPLASH, cur, 16, 0.25, 0.25, 0.25, 0.05);
                w.spawnParticle(Particle.FALLING_WATER, cur, 8, 0.2, 0.2, 0.2, 0);

                for (Entity e : w.getNearbyEntities(cur, 1.5, 1.5, 1.5)) {
                    if (e instanceof Player ally && ally != player && hit.add(ally.getUniqueId())) {
                        ally.setFireTicks(0);
                        AttributeInstance attr = ally.getAttribute(Attribute.MAX_HEALTH);
                        double max = attr != null ? attr.getValue() : 20.0;
                        ally.setHealth(Math.min(max, ally.getHealth() + 4.0));
                    } else if (e instanceof LivingEntity le && le != player && hit.add(le.getUniqueId())) {
                        le.setFireTicks(0);
                        le.damage(ctx.calculateFinalSkillDamage(player, le, 7.5), player);
                        le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 70, 3));
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
        SkillExecutionManager.playSoundSafe(player, "entity.elder_guardian.curse", 1.1f);
        player.sendActionBar(Component.text("🌊 Đại Hồng Thủy!", NamedTextColor.DARK_AQUA, TextDecoration.BOLD));

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (!player.isOnline() || ticks > 100) {
                    cancel();
                    return;
                }
                Location c = player.getLocation();
                World w = c.getWorld();

                for (int angle = 0; angle < 360; angle += 15) {
                    double rad = Math.toRadians(angle + ticks * 9);
                    w.spawnParticle(Particle.SPLASH, c.clone().add(Math.cos(rad) * 6.0, 0.5, Math.sin(rad) * 6.0), 3, 0.2, 0.4, 0.2, 0.05);
                }

                if (ticks % 20 == 0) {
                    AttributeInstance attr = player.getAttribute(Attribute.MAX_HEALTH);
                    double max = attr != null ? attr.getValue() : 20.0;
                    player.setHealth(Math.min(max, player.getHealth() + 1.5));

                    for (Entity e : w.getNearbyEntities(c, 6.0, 3.5, 6.0)) {
                        if (e instanceof LivingEntity le && le != player) {
                            le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 35, 3));
                            le.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 35, 2));
                            le.damage(ctx.calculateFinalSkillDamage(player, le, 3.0), player);
                        }
                    }
                }
                ticks += 5;
            }
        }.runTaskTimer(ctx.getPlugin(), 0L, 5L);
    }
}
