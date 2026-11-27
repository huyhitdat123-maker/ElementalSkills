package com.elementalskills.elements.impl;

import com.elementalskills.elements.ElementMastery;
import com.elementalskills.managers.CombatModeManager;
import com.elementalskills.managers.SkillVisualHelper;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class LavaElement implements ElementMastery {
    @Override public String getId() { return "lava"; }
    @Override public String getDisplayName() { return "Hệ Nham Thạch Lava"; }
    @Override public NamedTextColor getThemeColor() { return NamedTextColor.GOLD; }
    @Override public Material getGuiIcon() { return Material.MAGMA_CREAM; }
    @Override public int[] getCooldownSeconds() { return new int[]{8, 12, 15, 22, 36}; }

    @Override
    public List<String> getSkillNames() {
        return List.of("Dung Nham Phun Trào", "Cầu Nham Thạch Khổng Lồ", "Hắc Diện Thạch Xung Kích", "Dung Nham Địa Ngục", "Đại Núi Lửa Tận Thế");
    }

    @Override
    public List<String> getSkillDescriptions() {
        return List.of(
                "Phun trào cột dung nham dưới chân địch hất tung & thiêu đốt.",
                "Ném cầu magma tạo vũng dung nham sôi sục bán kính 5 block.",
                "Hóa nham thạch húc thẳng 14 block hất văng mọi mục tiêu.",
                "Triệu hồi 4 cột magma xoay quanh người thiêu rụi địch trong 5s.",
                "Kích hoạt núi lửa bán kính 12 block bắn mưa cầu magma 5s."
        );
    }

    @Override
    public void castSkill(Player player, int skillIndex, CombatModeManager ctx) {
        switch (skillIndex) {
            case 0 -> { // Dung Nham Phun Trào
                Location targetLoc = SkillVisualHelper.raycastLocation(player, 16.0);
                targetLoc.getWorld().spawnParticle(Particle.LAVA, targetLoc, 50, 1.5, 2.0, 1.5, 0);
                targetLoc.getWorld().spawnParticle(Particle.FLAME, targetLoc, 60, 1.5, 2.0, 1.5, 0.1);
                SkillVisualHelper.playSound(targetLoc, "block.lava.pop", 1.6f, 0.7f);
                for (LivingEntity t : SkillVisualHelper.getNearbyTargets(player, targetLoc, 4.5, ctx)) {
                    SkillVisualHelper.dealSkillDamage(player, t, 12.0, true);
                    t.setFireTicks(140);
                    t.setVelocity(new Vector(0, 1.1, 0));
                }
            }
            case 1 -> { // Cầu Nham Thạch Khổng Lồ
                Location impact = SkillVisualHelper.raycastLocation(player, 20.0);
                SkillVisualHelper.drawBeam(player.getEyeLocation(), impact, Particle.LAVA, 0.5, 3);
                SkillVisualHelper.playSound(impact, "entity.generic.explode", 1.5f, 0.7f);
                new BukkitRunnable() {
                    int ticks = 0;
                    @Override
                    public void run() {
                        ticks += 10;
                        SkillVisualHelper.spawnRing(impact, Particle.LAVA, 5.0, 35);
                        impact.getWorld().spawnParticle(Particle.FLAME, impact, 30, 2.5, 0.3, 2.5, 0.03);
                        for (LivingEntity t : SkillVisualHelper.getNearbyTargets(player, impact, 5.0, ctx)) {
                            SkillVisualHelper.dealSkillDamage(player, t, 5.0, true);
                            t.setFireTicks(100);
                            t.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 30, 2));
                        }
                        if (ticks >= 80) cancel();
                    }
                }.runTaskTimer(ctx.getPlugin(), 0L, 10L);
            }
            case 2 -> { // Hắc Diện Thạch Xung Kích
                Vector dir = player.getLocation().getDirection().setY(0).normalize();
                player.setVelocity(dir.clone().multiply(1.9).setY(0.3));
                player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 120, 0));
                SkillVisualHelper.playSound(player.getLocation(), "entity.ravager.roar", 1.3f, 0.8f);
                new BukkitRunnable() {
                    int ticks = 0;
                    @Override
                    public void run() {
                        if (++ticks > 12) { cancel(); return; }
                        Location loc = player.getLocation();
                        loc.getWorld().spawnParticle(Particle.LAVA, loc, 20, 1.0, 0.5, 1.0, 0);
                        for (LivingEntity t : SkillVisualHelper.getNearbyTargets(player, loc, 3.0, ctx)) {
                            SkillVisualHelper.dealSkillDamage(player, t, 13.0, true);
                            t.setFireTicks(120);
                            t.setVelocity(dir.clone().multiply(1.5).setY(0.6));
                        }
                    }
                }.runTaskTimer(ctx.getPlugin(), 0L, 1L);
            }
            case 3 -> { // Dung Nham Địa Ngục
                player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 140, 0));
                new BukkitRunnable() {
                    int ticks = 0;
                    @Override
                    public void run() {
                        ticks += 4;
                        Location c = player.getLocation();
                        for (int p = 0; p < 4; p++) {
                            double angle = (ticks * 0.15) + (Math.PI / 2 * p);
                            Location pillar = c.clone().add(Math.cos(angle) * 3.5, 0, Math.sin(angle) * 3.5);
                            for (double y = 0; y <= 3.0; y += 0.6) {
                                pillar.getWorld().spawnParticle(Particle.LAVA, pillar.clone().add(0, y, 0), 2, 0.1, 0.1, 0.1, 0);
                            }
                        }
                        for (LivingEntity t : SkillVisualHelper.getNearbyTargets(player, c, 4.5, ctx)) {
                            SkillVisualHelper.dealSkillDamage(player, t, 4.5, true);
                            t.setFireTicks(100);
                        }
                        if (ticks >= 100) cancel();
                    }
                }.runTaskTimer(ctx.getPlugin(), 0L, 4L);
            }
            case 4 -> { // Đại Núi Lửa Tận Thế (Tuyệt Kỹ)
                Location targetLoc = SkillVisualHelper.raycastLocation(player, 24.0);
                SkillVisualHelper.playSound(targetLoc, "entity.ender_dragon.growl", 2.0f, 0.6f);
                new BukkitRunnable() {
                    int ticks = 0;
                    @Override
                    public void run() {
                        ticks += 5;
                        SkillVisualHelper.spawnRing(targetLoc, Particle.LAVA, 12.0, 55);
                        targetLoc.getWorld().spawnParticle(Particle.LAVA, targetLoc.clone().add(0, 4, 0), 45, 5.0, 3.0, 5.0, 0);
                        for (int k = 0; k < 3; k++) {
                            double ox = ThreadLocalRandom.current().nextDouble(-10.0, 10.0);
                            double oz = ThreadLocalRandom.current().nextDouble(-10.0, 10.0);
                            Location drop = targetLoc.clone().add(ox, 0, oz);
                            drop.getWorld().spawnParticle(Particle.EXPLOSION, drop, 2, 0.5, 0.3, 0.5, 0);
                            for (LivingEntity t : SkillVisualHelper.getNearbyTargets(player, drop, 4.0, ctx)) {
                                SkillVisualHelper.dealSkillDamage(player, t, 11.0, true);
                                t.setFireTicks(160);
                            }
                        }
                        if (ticks >= 100) cancel();
                    }
                }.runTaskTimer(ctx.getPlugin(), 0L, 5L);
            }
        }
    }
}
