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

public class WaterElement implements ElementMastery {
    @Override public String getId() { return "thuy"; }
    @Override public String getDisplayName() { return "Hệ Thủy"; }
    @Override public NamedTextColor getThemeColor() { return NamedTextColor.AQUA; }
    @Override public Material getGuiIcon() { return Material.HEART_OF_THE_SEA; }
    @Override public int[] getCooldownSeconds() { return new int[]{7, 11, 16, 22, 34}; }

    @Override
    public List<String> getSkillNames() {
        return List.of("Thủy Triều Xung Kích", "Thủy Long Pháo", "Thủy Ngục Giam Cầm", "Đại Hồng Thủy", "Hải Thần Nộ Triều");
    }

    @Override
    public List<String> getSkillDescriptions() {
        return List.of(
                "Lướt sóng tốc độ cao cuốn phăng & đẩy lùi kẻ địch.",
                "Bắn 3 luồng thủy áp liên hoàn xuyên thấu mục tiêu.",
                "Nhốt kẻ địch vào cầu nước trên không 3s rồi kích nổ.",
                "Tạo xoáy nước bán kính 7 block làm chậm địch & hồi máu.",
                "Dựng sóng thần rộng 12 block càn quét 22 block phía trước."
        );
    }

    @Override
    public void castSkill(Player player, int skillIndex, CombatModeManager ctx) {
        switch (skillIndex) {
            case 0 -> { // Thủy Triều Xung Kích
                Vector dir = player.getLocation().getDirection().setY(0).normalize();
                player.setVelocity(dir.clone().multiply(1.8).setY(0.35));
                SkillVisualHelper.playSound(player.getLocation(), "entity.dolphin.splash", 1.4f, 1.0f);
                new BukkitRunnable() {
                    int ticks = 0;
                    @Override
                    public void run() {
                        if (++ticks > 12) { cancel(); return; }
                        Location loc = player.getLocation();
                        loc.getWorld().spawnParticle(Particle.SPLASH, loc, 40, 1.5, 0.8, 1.5, 0.2);
                        for (LivingEntity t : SkillVisualHelper.getNearbyTargets(player, loc, 3.2, ctx)) {
                            SkillVisualHelper.dealSkillDamage(player, t, 10.0, false);
                            t.setVelocity(dir.clone().multiply(1.4).setY(0.55));
                        }
                    }
                }.runTaskTimer(ctx.getPlugin(), 0L, 1L);
            }
            case 1 -> { // Thủy Long Pháo
                for (int shot = 0; shot < 3; shot++) {
                    Bukkit.getScheduler().runTaskLater(ctx.getPlugin(), () -> {
                        Location start = player.getEyeLocation();
                        Vector dir = start.getDirection().normalize();
                        Location end = start.clone().add(dir.clone().multiply(20));
                        SkillVisualHelper.drawBeam(start, end, Particle.BUBBLE_POP, 0.4, 5);
                        SkillVisualHelper.drawBeam(start, end, Particle.SPLASH, 0.5, 4);
                        SkillVisualHelper.playSound(start, "entity.player.splash.high_speed", 1.3f, 1.2f);
                        for (double d = 1; d <= 20; d += 1.5) {
                            Location pt = start.clone().add(dir.clone().multiply(d));
                            for (LivingEntity t : SkillVisualHelper.getNearbyTargets(player, pt, 2.0, ctx)) {
                                SkillVisualHelper.dealSkillDamage(player, t, 7.5, true);
                                t.setVelocity(dir.clone().multiply(0.9));
                                t.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 50, 2));
                            }
                        }
                    }, shot * 5L);
                }
            }
            case 2 -> { // Thủy Ngục Giam Cầm
                LivingEntity target = SkillVisualHelper.raycastTarget(player, 18.0, 1.5, ctx);
                if (target == null) {
                    List<LivingEntity> near = SkillVisualHelper.getNearbyTargets(player, player.getLocation(), 10.0, ctx);
                    if (!near.isEmpty()) target = near.get(0);
                }
                if (target == null) return;
                final LivingEntity prisoned = target;
                Location cagePos = prisoned.getLocation().add(0, 3.0, 0);
                SkillVisualHelper.playSound(cagePos, "block.bubble_column.whirlpool_inside", 1.5f, 0.8f);
                new BukkitRunnable() {
                    int ticks = 0;
                    @Override
                    public void run() {
                        ticks += 2;
                        prisoned.teleport(cagePos);
                        prisoned.setVelocity(new Vector(0, 0, 0));
                        cagePos.getWorld().spawnParticle(Particle.FALLING_WATER, cagePos.clone().add(0, 1, 0), 35, 1.3, 1.3, 1.3, 0);
                        cagePos.getWorld().spawnParticle(Particle.BUBBLE, cagePos.clone().add(0, 1, 0), 20, 1.2, 1.2, 1.2, 0.05);
                        if (ticks >= 60) {
                            cancel();
                            cagePos.getWorld().spawnParticle(Particle.EXPLOSION, cagePos, 3, 0.5, 0.5, 0.5, 0);
                            SkillVisualHelper.playSound(cagePos, "entity.generic.splash", 1.6f, 0.7f);
                            SkillVisualHelper.dealSkillDamage(player, prisoned, 16.0, true);
                            prisoned.setVelocity(new Vector(0, -1.5, 0));
                        }
                    }
                }.runTaskTimer(ctx.getPlugin(), 0L, 2L);
            }
            case 3 -> { // Đại Hồng Thủy
                Location center = player.getLocation();
                SkillVisualHelper.playSound(center, "ambient.underwater.enter", 1.5f, 0.8f);
                new BukkitRunnable() {
                    int ticks = 0;
                    @Override
                    public void run() {
                        ticks += 10;
                        SkillVisualHelper.spawnRing(center, Particle.SPLASH, 7.0, 45);
                        center.getWorld().spawnParticle(Particle.DRIPPING_WATER, center.clone().add(0, 2, 0), 40, 3.5, 1.0, 3.5, 0);
                        if (player.getLocation().distanceSquared(center) <= 49) {
                            player.setFireTicks(0);
                            SkillVisualHelper.healPlayer(player, 2.0);
                        }
                        for (LivingEntity t : SkillVisualHelper.getNearbyTargets(player, center, 7.0, ctx)) {
                            SkillVisualHelper.dealSkillDamage(player, t, 4.0, false);
                            t.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 30, 2));
                            t.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 30, 2));
                        }
                        if (ticks >= 80) cancel();
                    }
                }.runTaskTimer(ctx.getPlugin(), 0L, 10L);
            }
            case 4 -> { // Hải Thần Nộ Triều (Tuyệt Kỹ)
                Location origin = player.getLocation();
                Vector dir = origin.getDirection().setY(0).normalize();
                Vector right = new Vector(-dir.getZ(), 0, dir.getX()).normalize();
                SkillVisualHelper.playSound(origin, "entity.elder_guardian.curse", 1.5f, 0.8f);
                new BukkitRunnable() {
                    int dist = 0;
                    @Override
                    public void run() {
                        if (++dist > 22) { cancel(); return; }
                        Location waveCenter = origin.clone().add(dir.clone().multiply(dist));
                        for (double w = -6.0; w <= 6.0; w += 0.8) {
                            for (double h = 0; h <= 5.0; h += 1.0) {
                                Location p = waveCenter.clone().add(right.clone().multiply(w)).add(0, h, 0);
                                p.getWorld().spawnParticle(Particle.SPLASH, p, 3, 0.2, 0.2, 0.2, 0.1);
                            }
                        }
                        for (LivingEntity t : SkillVisualHelper.getNearbyTargets(player, waveCenter, 6.2, ctx)) {
                            SkillVisualHelper.dealSkillDamage(player, t, 14.0, true);
                            t.setVelocity(dir.clone().multiply(1.6).setY(0.75));
                            t.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 80, 3));
                        }
                    }
                }.runTaskTimer(ctx.getPlugin(), 0L, 2L);
            }
        }
    }
}
