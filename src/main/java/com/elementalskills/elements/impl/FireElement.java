package com.elementalskills.elements.impl;

import com.elementalskills.elements.ElementMastery;
import com.elementalskills.managers.CombatModeManager;
import com.elementalskills.managers.SkillVisualHelper;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class FireElement implements ElementMastery {
    @Override public String getId() { return "hoa"; }
    @Override public String getDisplayName() { return "Hệ Hỏa"; }
    @Override public NamedTextColor getThemeColor() { return NamedTextColor.RED; }
    @Override public Material getGuiIcon() { return Material.FIRE_CHARGE; }
    @Override public int[] getCooldownSeconds() { return new int[]{8, 12, 16, 22, 35}; }

    @Override
    public List<String> getSkillNames() {
        return List.of("Hỏa Diệm Giáng Thế", "Hỏa Long Xung Thiên", "Bạo Viêm Trảm Kích", "Viêm Hoàng Lĩnh Vực", "Thiên Hỏa Vẫn Thạch");
    }

    @Override
    public List<String> getSkillDescriptions() {
        return List.of(
                "Nhảy vọt lên rồi dậm nổ biển lửa bán kính 8 block.",
                "Triệu hồi rồng lửa càn quét thẳng 20 block phía trước.",
                "Chém 3 luồng sóng hỏa diệm bán nguyệt tầm xa.",
                "Mở xoáy lửa bán kính 7 block hút chặt & thiêu đốt 4s.",
                "Gọi 5 thiên thạch khổng lồ giáng xuống vùng ngắm."
        );
    }

    @Override
    public void castSkill(Player player, int skillIndex, CombatModeManager ctx) {
        switch (skillIndex) {
            case 0 -> { // Hỏa Diệm Giáng Thế
                ctx.grantFallImmunity(player, 6);
                player.setVelocity(player.getLocation().getDirection().multiply(0.9).setY(1.05));
                SkillVisualHelper.playSound(player.getLocation(), "entity.blaze.shoot", 1.2f, 0.9f);
                Bukkit.getScheduler().runTaskLater(ctx.getPlugin(), () -> {
                    Location loc = player.getLocation();
                    loc.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, loc, 2, 0.5, 0.2, 0.5, 0);
                    loc.getWorld().spawnParticle(Particle.FLAME, loc, 120, 4.0, 0.6, 4.0, 0.12);
                    SkillVisualHelper.playSound(loc, "entity.generic.explode", 1.5f, 0.85f);
                    for (LivingEntity target : SkillVisualHelper.getNearbyTargets(player, loc, 8.0, ctx)) {
                        SkillVisualHelper.dealSkillDamage(player, target, 12.0, false);
                        target.setFireTicks(120);
                        target.setVelocity(target.getLocation().toVector().subtract(loc.toVector()).normalize().multiply(1.2).setY(0.65));
                    }
                }, 14L);
            }
            case 1 -> { // Hỏa Long Xung Thiên
                Location start = player.getEyeLocation();
                Vector dir = start.getDirection().normalize();
                SkillVisualHelper.playSound(start, "entity.ender_dragon.growl", 1.2f, 1.3f);
                new BukkitRunnable() {
                    int step = 0;
                    @Override
                    public void run() {
                        if (++step > 20) { cancel(); return; }
                        Location cur = start.clone().add(dir.clone().multiply(step));
                        double wave = Math.sin(step * 0.7) * 0.8;
                        Vector side = new Vector(-dir.getZ(), 0, dir.getX()).normalize().multiply(wave);
                        Location head = cur.clone().add(side);
                        head.getWorld().spawnParticle(Particle.FLAME, head, 25, 0.7, 0.7, 0.7, 0.05);
                        head.getWorld().spawnParticle(Particle.LAVA, head, 6, 0.5, 0.5, 0.5, 0);
                        for (LivingEntity t : SkillVisualHelper.getNearbyTargets(player, head, 2.8, ctx)) {
                            SkillVisualHelper.dealSkillDamage(player, t, 14.0, true);
                            t.setFireTicks(140);
                            t.setVelocity(dir.clone().multiply(1.1).setY(0.6));
                        }
                    }
                }.runTaskTimer(ctx.getPlugin(), 0L, 1L);
            }
            case 2 -> { // Bạo Viêm Trảm Kích
                Location eye = player.getEyeLocation();
                Vector forward = eye.getDirection().setY(0).normalize();
                Vector right = new Vector(-forward.getZ(), 0, forward.getX()).normalize();
                SkillVisualHelper.playSound(eye, "entity.player.attack.sweep", 1.4f, 0.7f);
                for (int waveIdx = 0; waveIdx < 3; waveIdx++) {
                    final int idx = waveIdx;
                    Bukkit.getScheduler().runTaskLater(ctx.getPlugin(), () -> {
                        double yawOffset = (idx - 1) * 0.25;
                        Vector dir = forward.clone().add(right.clone().multiply(yawOffset)).normalize();
                        new BukkitRunnable() {
                            int dist = 0;
                            @Override
                            public void run() {
                                if (++dist > 18) { cancel(); return; }
                                Location center = eye.clone().add(dir.clone().multiply(dist));
                                for (double a = -2.2; a <= 2.2; a += 0.4) {
                                    Location p = center.clone().add(right.clone().multiply(a));
                                    p.getWorld().spawnParticle(Particle.FLAME, p, 4, 0.1, 0.2, 0.1, 0.02);
                                }
                                for (LivingEntity t : SkillVisualHelper.getNearbyTargets(player, center, 2.6, ctx)) {
                                    SkillVisualHelper.dealSkillDamage(player, t, 10.0, false);
                                    t.setFireTicks(100);
                                }
                            }
                        }.runTaskTimer(ctx.getPlugin(), 0L, 1L);
                    }, idx * 4L);
                }
            }
            case 3 -> { // Viêm Hoàng Lĩnh Vực
                Location center = player.getLocation();
                SkillVisualHelper.playSound(center, "block.fire.ambient", 1.6f, 0.8f);
                new BukkitRunnable() {
                    int ticks = 0;
                    @Override
                    public void run() {
                        ticks += 5;
                        SkillVisualHelper.spawnRing(center, Particle.FLAME, 7.0, 40);
                        center.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, center.clone().add(0, 1, 0), 25, 3.0, 1.5, 3.0, 0.05);
                        for (LivingEntity t : SkillVisualHelper.getNearbyTargets(player, center, 7.0, ctx)) {
                            Vector pull = center.toVector().subtract(t.getLocation().toVector()).normalize().multiply(0.45);
                            t.setVelocity(pull);
                            SkillVisualHelper.dealSkillDamage(player, t, 3.5, true);
                            t.setFireTicks(80);
                        }
                        if (ticks >= 80) {
                            cancel();
                            center.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, center, 3, 2, 1, 2, 0);
                            SkillVisualHelper.playSound(center, "entity.generic.explode", 1.8f, 0.7f);
                            for (LivingEntity t : SkillVisualHelper.getNearbyTargets(player, center, 7.5, ctx)) {
                                SkillVisualHelper.dealSkillDamage(player, t, 14.0, true);
                                t.setVelocity(t.getLocation().toVector().subtract(center.toVector()).normalize().multiply(1.6).setY(0.8));
                            }
                        }
                    }
                }.runTaskTimer(ctx.getPlugin(), 0L, 5L);
            }
            case 4 -> { // Thiên Hỏa Vẫn Thạch (Tuyệt Kỹ)
                Location targetLoc = SkillVisualHelper.raycastLocation(player, 26.0);
                SkillVisualHelper.playSound(targetLoc, "entity.wither.spawn", 1.3f, 0.8f);
                for (int m = 0; m < 5; m++) {
                    Bukkit.getScheduler().runTaskLater(ctx.getPlugin(), () -> {
                        double ox = ThreadLocalRandom.current().nextDouble(-6.0, 6.0);
                        double oz = ThreadLocalRandom.current().nextDouble(-6.0, 6.0);
                        Location impact = targetLoc.clone().add(ox, 0, oz);
                        Location sky = impact.clone().add(4, 16, 4);
                        new BukkitRunnable() {
                            int step = 0;
                            @Override
                            public void run() {
                                step++;
                                double ratio = step / 10.0;
                                Location cur = sky.clone().add(impact.toVector().subtract(sky.toVector()).multiply(ratio));
                                cur.getWorld().spawnParticle(Particle.FLAME, cur, 35, 0.8, 0.8, 0.8, 0.05);
                                cur.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, cur, 12, 0.6, 0.6, 0.6, 0.02);
                                if (step >= 10) {
                                    cancel();
                                    impact.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, impact, 3, 1.5, 0.5, 1.5, 0);
                                    impact.getWorld().spawnParticle(Particle.LAVA, impact, 40, 3.0, 1.0, 3.0, 0);
                                    SkillVisualHelper.playSound(impact, "entity.generic.explode", 2.0f, 0.65f);
                                    for (LivingEntity t : SkillVisualHelper.getNearbyTargets(player, impact, 6.5, ctx)) {
                                        SkillVisualHelper.dealSkillDamage(player, t, 18.0, true);
                                        t.setFireTicks(160);
                                        t.setVelocity(new Vector(0, 0.9, 0));
                                    }
                                }
                            }
                        }.runTaskTimer(ctx.getPlugin(), 0L, 1L);
                    }, m * 8L);
                }
            }
        }
    }
}
