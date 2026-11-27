package com.elementalskills.elements.impl;

import com.elementalskills.elements.ElementMastery;
import com.elementalskills.managers.CombatModeManager;
import com.elementalskills.managers.SkillVisualHelper;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.ShulkerBullet;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class WindElement implements ElementMastery {
    @Override public String getId() { return "phong"; }
    @Override public String getDisplayName() { return "Hệ Phong"; }
    @Override public NamedTextColor getThemeColor() { return NamedTextColor.WHITE; }
    @Override public Material getGuiIcon() { return Material.WIND_CHARGE; }
    @Override public int[] getCooldownSeconds() { return new int[]{8, 11, 14, 20, 35}; }

    @Override
    public List<String> getSkillNames() {
        return List.of("Đạp Gió Lên Mây", "Liệt Phong Liên Trảm", "Tật Phong Tàn Ảnh", "Bão Tố Cuồng Nộ", "Thiên Phong Đại Long Quyển");
    }

    @Override
    public List<String> getSkillDescriptions() {
        return List.of(
                "Bay vọt lên không & phóng 5 đạn Shulker gia tốc tự khóa mục tiêu.",
                "Phóng 3 lưỡi dao gió khổng lồ chém xuyên & hất tung kẻ địch.",
                "Lướt xuyên qua 12 block phía trước chém toàn bộ mục tiêu trên đường.",
                "Tạo lốc xoáy quanh người 4s hất tung kẻ địch áp sát.",
                "Gọi 3 cơn lốc xoáy khổng lồ cuốn địch lên trời rồi quật xuống đất."
        );
    }

    @Override
    public void castSkill(Player player, int skillIndex, CombatModeManager ctx) {
        switch (skillIndex) {
            case 0 -> { // Đạp Gió Lên Mây + 5 đạn Shulker gia tốc cao
                ctx.grantFallImmunity(player, 8);
                player.setVelocity(player.getLocation().getDirection().multiply(0.6).setY(1.25));
                Location loc = player.getLocation();
                loc.getWorld().spawnParticle(Particle.CLOUD, loc, 45, 1.0, 0.3, 1.0, 0.15);
                SkillVisualHelper.playSound(loc, "entity.breeze.jump", 1.5f, 1.2f);

                List<LivingEntity> targets = SkillVisualHelper.getNearbyTargets(player, loc, 24.0, ctx);
                for (int i = 0; i < 5; i++) {
                    double angle = (2 * Math.PI * i) / 5.0;
                    Location spawn = player.getEyeLocation().add(Math.cos(angle) * 1.4, 0.6, Math.sin(angle) * 1.4);
                    ShulkerBullet bullet = (ShulkerBullet) player.getWorld().spawnEntity(spawn, EntityType.SHULKER_BULLET);
                    ctx.tagCustomShulkerBullet(bullet, player, 7.5);
                    if (!targets.isEmpty()) {
                        bullet.setTarget(targets.get(i % targets.size()));
                    }
                    // Gia tốc liên tục cho đạn Shulker bay nhanh hơn mặc định
                    new BukkitRunnable() {
                        int life = 0;
                        @Override
                        public void run() {
                            if (!bullet.isValid() || bullet.isDead() || ++life > 80) {
                                cancel();
                                return;
                            }
                            if (bullet.getTarget() != null && bullet.getTarget().isValid()) {
                                Vector toTarget = bullet.getTarget().getLocation().add(0, 1, 0).toVector()
                                        .subtract(bullet.getLocation().toVector()).normalize();
                                bullet.setVelocity(toTarget.multiply(1.35));
                            }
                            bullet.getWorld().spawnParticle(Particle.CLOUD, bullet.getLocation(), 2, 0.05, 0.05, 0.05, 0.01);
                        }
                    }.runTaskTimer(ctx.getPlugin(), 1L, 1L);
                }
            }
            case 1 -> { // Liệt Phong Liên Trảm
                for (int blade = 0; blade < 3; blade++) {
                    Bukkit.getScheduler().runTaskLater(ctx.getPlugin(), () -> {
                        Location start = player.getEyeLocation();
                        Vector dir = start.getDirection().normalize();
                        SkillVisualHelper.playSound(start, "entity.player.attack.sweep", 1.4f, 1.4f);
                        new BukkitRunnable() {
                            int step = 0;
                            @Override
                            public void run() {
                                if (++step > 18) { cancel(); return; }
                                Location cur = start.clone().add(dir.clone().multiply(step));
                                cur.getWorld().spawnParticle(Particle.SWEEP_ATTACK, cur, 3, 0.6, 0.3, 0.6, 0);
                                cur.getWorld().spawnParticle(Particle.CLOUD, cur, 8, 0.5, 0.2, 0.5, 0.05);
                                for (LivingEntity t : SkillVisualHelper.getNearbyTargets(player, cur, 2.5, ctx)) {
                                    SkillVisualHelper.dealSkillDamage(player, t, 8.5, true);
                                    t.setVelocity(new Vector(0, 0.65, 0));
                                }
                            }
                        }.runTaskTimer(ctx.getPlugin(), 0L, 1L);
                    }, blade * 5L);
                }
            }
            case 2 -> { // Tật Phong Tàn Ảnh
                ctx.grantFallImmunity(player, 4);
                Location start = player.getLocation();
                Vector dir = start.getDirection().setY(0).normalize();
                Location dest = SkillVisualHelper.raycastLocation(player, 12.0);
                SkillVisualHelper.drawBeam(start.clone().add(0, 1, 0), dest.clone().add(0, 1, 0), Particle.CLOUD, 0.3, 4);
                SkillVisualHelper.playSound(start, "entity.enderman.teleport", 1.3f, 1.5f);
                for (double d = 0; d <= 12; d += 1.5) {
                    for (LivingEntity t : SkillVisualHelper.getNearbyTargets(player, start.clone().add(dir.clone().multiply(d)), 2.5, ctx)) {
                        SkillVisualHelper.dealSkillDamage(player, t, 14.0, true);
                    }
                }
                dest.setYaw(start.getYaw());
                dest.setPitch(start.getPitch());
                player.teleport(dest);
            }
            case 3 -> { // Bão Tố Cuồng Nộ
                SkillVisualHelper.playSound(player.getLocation(), "entity.breeze.inhale", 1.5f, 1.0f);
                new BukkitRunnable() {
                    int ticks = 0;
                    @Override
                    public void run() {
                        ticks += 4;
                        Location c = player.getLocation();
                        SkillVisualHelper.spawnRing(c.clone().add(0, 0.5, 0), Particle.CLOUD, 4.5, 30);
                        SkillVisualHelper.spawnRing(c.clone().add(0, 1.8, 0), Particle.CLOUD, 5.5, 30);
                        for (LivingEntity t : SkillVisualHelper.getNearbyTargets(player, c, 5.5, ctx)) {
                            SkillVisualHelper.dealSkillDamage(player, t, 3.5, false);
                            Vector push = t.getLocation().toVector().subtract(c.toVector()).normalize().multiply(1.3).setY(0.8);
                            t.setVelocity(push);
                        }
                        if (ticks >= 80) cancel();
                    }
                }.runTaskTimer(ctx.getPlugin(), 0L, 4L);
            }
            case 4 -> { // Thiên Phong Đại Long Quyển (Tuyệt Kỹ)
                Location origin = player.getLocation();
                Vector baseDir = origin.getDirection().setY(0).normalize();
                Vector right = new Vector(-baseDir.getZ(), 0, baseDir.getX()).normalize();
                SkillVisualHelper.playSound(origin, "entity.ender_dragon.flap", 1.8f, 0.7f);
                for (int nado = -1; nado <= 1; nado++) {
                    Vector dir = baseDir.clone().add(right.clone().multiply(nado * 0.35)).normalize();
                    new BukkitRunnable() {
                        int ticks = 0;
                        Location pos = origin.clone().add(dir.clone().multiply(2));
                        @Override
                        public void run() {
                            ticks += 2;
                            pos.add(dir.clone().multiply(0.9));
                            for (double y = 0; y <= 8.0; y += 1.0) {
                                double r = 1.0 + (y * 0.35);
                                SkillVisualHelper.spawnRing(pos.clone().add(0, y, 0), Particle.CLOUD, r, 14);
                            }
                            for (LivingEntity t : SkillVisualHelper.getNearbyTargets(player, pos, 4.2, ctx)) {
                                SkillVisualHelper.dealSkillDamage(player, t, 5.0, true);
                                double spin = ThreadLocalRandom.current().nextDouble(-0.5, 0.5);
                                t.setVelocity(new Vector(spin, 1.05, -spin));
                            }
                            if (ticks >= 100) cancel();
                        }
                    }.runTaskTimer(ctx.getPlugin(), 0L, 2L);
                }
            }
        }
    }
}
