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

public class SwordImmortalElement implements ElementMastery {
    @Override public String getId() { return "kiemtien"; }
    @Override public String getDisplayName() { return "Hệ Kiếm Tiên"; }
    @Override public NamedTextColor getThemeColor() { return NamedTextColor.GOLD; }
    @Override public Material getGuiIcon() { return Material.NETHERITE_SWORD; }
    @Override public int[] getCooldownSeconds() { return new int[]{8, 11, 16, 22, 36}; }

    @Override
    public List<String> getSkillNames() {
        return List.of("Ngự Kiếm Phi Hành", "Kiếm Khí Tung Hoành", "Tru Tiên Kiếm Trận", "Nhân Kiếm Hợp Nhất", "Vạn Kiếm Quy Tông");
    }

    @Override
    public List<String> getSkillDescriptions() {
        return List.of(
                "Đạp phi kiếm lướt bay 16 block & chém xoáy kẻ địch lướt qua.",
                "Phóng liên tiếp 5 luồng kiếm khí bán nguyệt tầm xa 20 block.",
                "Dựng 8 linh kiếm giam cầm mục tiêu 3s rồi đâm xuyên tâm.",
                "Hóa kiếm quang lướt chém liên hoàn tối đa 6 kẻ địch.",
                "Triệu hồi 30 tiên kiếm trút mưa xuống bán kính 12 block."
        );
    }

    @Override
    public void castSkill(Player player, int skillIndex, CombatModeManager ctx) {
        switch (skillIndex) {
            case 0 -> { // Ngự Kiếm Phi Hành
                ctx.grantFallImmunity(player, 6);
                Vector dir = player.getLocation().getDirection().normalize();
                player.setVelocity(dir.clone().multiply(1.8).setY(0.45));
                SkillVisualHelper.playSound(player.getLocation(), "entity.player.attack.sweep", 1.4f, 1.3f);
                new BukkitRunnable() {
                    int ticks = 0;
                    @Override
                    public void run() {
                        if (++ticks > 12) { cancel(); return; }
                        Location loc = player.getLocation();
                        loc.getWorld().spawnParticle(Particle.END_ROD, loc, 15, 0.6, 0.2, 0.6, 0.02);
                        loc.getWorld().spawnParticle(Particle.SWEEP_ATTACK, loc.clone().add(0, 1, 0), 2, 0.8, 0.4, 0.8, 0);
                        for (LivingEntity t : SkillVisualHelper.getNearbyTargets(player, loc, 3.0, ctx)) {
                            SkillVisualHelper.dealSkillDamage(player, t, 11.0, true);
                        }
                    }
                }.runTaskTimer(ctx.getPlugin(), 0L, 1L);
            }
            case 1 -> { // Kiếm Khí Tung Hoành
                for (int wave = 0; wave < 5; wave++) {
                    Bukkit.getScheduler().runTaskLater(ctx.getPlugin(), () -> {
                        Location start = player.getEyeLocation();
                        Vector dir = start.getDirection().normalize();
                        SkillVisualHelper.playSound(start, "entity.player.attack.sweep", 1.3f, 1.5f);
                        new BukkitRunnable() {
                            int dist = 0;
                            @Override
                            public void run() {
                                if (++dist > 20) { cancel(); return; }
                                Location cur = start.clone().add(dir.clone().multiply(dist));
                                cur.getWorld().spawnParticle(Particle.SWEEP_ATTACK, cur, 3, 0.7, 0.2, 0.7, 0);
                                cur.getWorld().spawnParticle(Particle.END_ROD, cur, 4, 0.5, 0.1, 0.5, 0.01);
                                for (LivingEntity t : SkillVisualHelper.getNearbyTargets(player, cur, 2.4, ctx)) {
                                    SkillVisualHelper.dealSkillDamage(player, t, 7.0, true);
                                }
                            }
                        }.runTaskTimer(ctx.getPlugin(), 0L, 1L);
                    }, wave * 4L);
                }
            }
            case 2 -> { // Tru Tiên Kiếm Trận
                LivingEntity target = SkillVisualHelper.raycastTarget(player, 18.0, 2.0, ctx);
                if (target == null) return;
                Location center = target.getLocation();
                ctx.applyHardFreeze(target, 60);
                SkillVisualHelper.playSound(center, "block.anvil.place", 1.4f, 1.6f);
                new BukkitRunnable() {
                    int ticks = 0;
                    @Override
                    public void run() {
                        ticks += 5;
                        SkillVisualHelper.spawnRing(center.clone().add(0, 1.5, 0), Particle.END_ROD, 3.0, 16);
                        if (ticks >= 60) {
                            cancel();
                            for (int i = 0; i < 8; i++) {
                                double a = (2 * Math.PI * i) / 8.0;
                                SkillVisualHelper.drawBeam(center.clone().add(Math.cos(a) * 3.5, 2.0, Math.sin(a) * 3.5),
                                        center.clone().add(0, 1.0, 0), Particle.CRIT, 0.25, 3);
                            }
                            SkillVisualHelper.dealSkillDamage(player, target, 18.0, true);
                        }
                    }
                }.runTaskTimer(ctx.getPlugin(), 0L, 5L);
            }
            case 3 -> { // Nhân Kiếm Hợp Nhất
                List<LivingEntity> targets = SkillVisualHelper.getNearbyTargets(player, player.getLocation(), 15.0, ctx);
                if (targets.isEmpty()) return;
                int maxHits = Math.min(6, targets.size());
                for (int i = 0; i < maxHits; i++) {
                    final LivingEntity victim = targets.get(i);
                    Bukkit.getScheduler().runTaskLater(ctx.getPlugin(), () -> {
                        if (!victim.isValid() || victim.isDead()) return;
                        SkillVisualHelper.drawBeam(player.getEyeLocation(), victim.getEyeLocation(), Particle.END_ROD, 0.3, 4);
                        player.teleport(victim.getLocation());
                        victim.getWorld().spawnParticle(Particle.SWEEP_ATTACK, victim.getLocation().add(0, 1, 0), 3, 0.5, 0.5, 0.5, 0);
                        SkillVisualHelper.dealSkillDamage(player, victim, 12.0, true);
                    }, i * 4L);
                }
            }
            case 4 -> { // Vạn Kiếm Quy Tông (Tuyệt Kỹ)
                Location targetLoc = SkillVisualHelper.raycastLocation(player, 24.0);
                SkillVisualHelper.playSound(targetLoc, "entity.ender_dragon.growl", 1.8f, 1.4f);
                for (int s = 0; s < 30; s++) {
                    Bukkit.getScheduler().runTaskLater(ctx.getPlugin(), () -> {
                        double ox = ThreadLocalRandom.current().nextDouble(-10.0, 10.0);
                        double oz = ThreadLocalRandom.current().nextDouble(-10.0, 10.0);
                        Location hit = targetLoc.clone().add(ox, 0, oz);
                        SkillVisualHelper.drawBeam(hit.clone().add(0, 14, 0), hit, Particle.END_ROD, 0.5, 3);
                        hit.getWorld().spawnParticle(Particle.SWEEP_ATTACK, hit.clone().add(0, 1, 0), 2, 0.4, 0.4, 0.4, 0);
                        for (LivingEntity t : SkillVisualHelper.getNearbyTargets(player, hit, 3.2, ctx)) {
                            SkillVisualHelper.dealSkillDamage(player, t, 8.0, true);
                        }
                    }, s * 2L);
                }
            }
        }
    }
}
