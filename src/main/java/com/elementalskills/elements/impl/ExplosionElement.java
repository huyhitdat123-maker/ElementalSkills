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

public class ExplosionElement implements ElementMastery {
    @Override public String getId() { return "no"; }
    @Override public String getDisplayName() { return "Hệ Nổ"; }
    @Override public NamedTextColor getThemeColor() { return NamedTextColor.RED; }
    @Override public Material getGuiIcon() { return Material.TNT; }
    @Override public int[] getCooldownSeconds() { return new int[]{8, 13, 15, 22, 36}; }

    @Override
    public List<String> getSkillNames() {
        return List.of("Lựu Đạn Phản Lực", "Liên Hoàn Địa Lôi", "Hỏa Tiễn Dẫn Đường", "Xung Kích Cận Chiến", "Cơn Mưa Pháo Kích");
    }

    @Override
    public List<String> getSkillDescriptions() {
        return List.of(
                "Kích nổ dưới chân bật nhảy vọt & hất tung kẻ địch tại chỗ.",
                "Rải 5 quả mìn năng lượng phát nổ khi kẻ địch giẫm phải.",
                "Phóng 3 tên lửa tầm nhiệt tự khóa mục tiêu & phát nổ.",
                "Tụ năng lượng 1s rồi giải phóng vụ nổ bán kính 8 block.",
                "Gọi 15 đợt pháo kích + Siêu Vụ Nổ trút xuống vùng ngắm."
        );
    }

    @Override
    public void castSkill(Player player, int skillIndex, CombatModeManager ctx) {
        switch (skillIndex) {
            case 0 -> { // Lựu Đạn Phản Lực
                ctx.grantFallImmunity(player, 6);
                Location c = player.getLocation();
                c.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, c, 1, 0, 0, 0, 0);
                SkillVisualHelper.playSound(c, "entity.generic.explode", 1.5f, 1.2f);
                player.setVelocity(c.getDirection().multiply(1.5).setY(1.1));
                for (LivingEntity t : SkillVisualHelper.getNearbyTargets(player, c, 5.0, ctx)) {
                    SkillVisualHelper.dealSkillDamage(player, t, 11.0, true);
                    t.setVelocity(new Vector(0, 0.9, 0));
                }
            }
            case 1 -> { // Liên Hoàn Địa Lôi
                Location base = player.getLocation();
                Vector dir = base.getDirection().setY(0).normalize();
                SkillVisualHelper.playSound(base, "block.tripwire.click_on", 1.4f, 1.0f);
                for (int m = 1; m <= 5; m++) {
                    Location mine = base.clone().add(dir.clone().multiply(m * 2.2));
                    new BukkitRunnable() {
                        int ticks = 0;
                        @Override
                        public void run() {
                            ticks += 5;
                            mine.getWorld().spawnParticle(Particle.CRIT, mine.clone().add(0, 0.2, 0), 3, 0.2, 0.05, 0.2, 0);
                            List<LivingEntity> near = SkillVisualHelper.getNearbyTargets(player, mine, 2.2, ctx);
                            if (!near.isEmpty() || ticks >= 180) {
                                cancel();
                                if (!near.isEmpty()) {
                                    mine.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, mine, 1, 0, 0, 0, 0);
                                    SkillVisualHelper.playSound(mine, "entity.generic.explode", 1.4f, 1.1f);
                                    for (LivingEntity t : SkillVisualHelper.getNearbyTargets(player, mine, 3.8, ctx)) {
                                        SkillVisualHelper.dealSkillDamage(player, t, 12.0, true);
                                        t.setVelocity(new Vector(0, 0.85, 0));
                                    }
                                }
                            }
                        }
                    }.runTaskTimer(ctx.getPlugin(), 0L, 5L);
                }
            }
            case 2 -> { // Hỏa Tiễn Dẫn Đường
                List<LivingEntity> targets = SkillVisualHelper.getNearbyTargets(player, player.getLocation(), 24.0, ctx);
                for (int r = 0; r < 3; r++) {
                    final LivingEntity lock = targets.isEmpty() ? null : targets.get(r % targets.size());
                    Bukkit.getScheduler().runTaskLater(ctx.getPlugin(), () -> {
                        Location pos = player.getEyeLocation();
                        Vector fallbackDir = pos.getDirection().normalize();
                        SkillVisualHelper.playSound(pos, "entity.firework_rocket.launch", 1.4f, 1.0f);
                        new BukkitRunnable() {
                            int step = 0;
                            @Override
                            public void run() {
                                step++;
                                Vector dir = (lock != null && lock.isValid() && !lock.isDead())
                                        ? lock.getEyeLocation().toVector().subtract(pos.toVector()).normalize()
                                        : fallbackDir;
                                pos.add(dir.multiply(1.35));
                                pos.getWorld().spawnParticle(Particle.FLAME, pos, 10, 0.15, 0.15, 0.15, 0.02);
                                pos.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, pos, 4, 0.1, 0.1, 0.1, 0.01);

                                List<LivingEntity> hit = SkillVisualHelper.getNearbyTargets(player, pos, 2.0, ctx);
                                if (!hit.isEmpty() || step >= 22) {
                                    cancel();
                                    pos.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, pos, 1, 0, 0, 0, 0);
                                    SkillVisualHelper.playSound(pos, "entity.generic.explode", 1.5f, 1.0f);
                                    for (LivingEntity t : SkillVisualHelper.getNearbyTargets(player, pos, 4.2, ctx)) {
                                        SkillVisualHelper.dealSkillDamage(player, t, 11.0, true);
                                        t.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 2));
                                    }
                                }
                            }
                        }.runTaskTimer(ctx.getPlugin(), 0L, 1L);
                    }, r * 5L);
                }
            }
            case 3 -> { // Xung Kích Cận Chiến
                SkillVisualHelper.playSound(player.getLocation(), "entity.tnt.primed", 1.6f, 1.0f);
                player.getWorld().spawnParticle(Particle.FLAME, player.getLocation().add(0, 1, 0), 60, 1.5, 1.0, 1.5, 0.1);
                Bukkit.getScheduler().runTaskLater(ctx.getPlugin(), () -> {
                    Location c = player.getLocation();
                    c.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, c, 4, 2.5, 1.0, 2.5, 0);
                    SkillVisualHelper.playSound(c, "entity.generic.explode", 2.0f, 0.7f);
                    for (LivingEntity t : SkillVisualHelper.getNearbyTargets(player, c, 8.0, ctx)) {
                        SkillVisualHelper.dealSkillDamage(player, t, 20.0, true);
                        t.setVelocity(t.getLocation().toVector().subtract(c.toVector()).normalize().multiply(2.0).setY(0.8));
                    }
                }, 20L);
            }
            case 4 -> { // Cơn Mưa Pháo Kích Hủy Diệt (Tuyệt Kỹ)
                Location targetLoc = SkillVisualHelper.raycastLocation(player, 24.0);
                for (int b = 0; b < 15; b++) {
                    final boolean finalNuke = (b == 14);
                    Bukkit.getScheduler().runTaskLater(ctx.getPlugin(), () -> {
                        double ox = finalNuke ? 0 : ThreadLocalRandom.current().nextDouble(-10.0, 10.0);
                        double oz = finalNuke ? 0 : ThreadLocalRandom.current().nextDouble(-10.0, 10.0);
                        Location hit = targetLoc.clone().add(ox, 0, oz);
                        SkillVisualHelper.drawBeam(hit.clone().add(0, 15, 0), hit, Particle.CAMPFIRE_COSY_SMOKE, 0.6, 2);
                        hit.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, hit, finalNuke ? 5 : 1, 1.5, 0.5, 1.5, 0);
                        SkillVisualHelper.playSound(hit, "entity.generic.explode", 2.0f, finalNuke ? 0.5f : 1.0f);
                        for (LivingEntity t : SkillVisualHelper.getNearbyTargets(player, hit, finalNuke ? 9.0 : 4.5, ctx)) {
                            SkillVisualHelper.dealSkillDamage(player, t, finalNuke ? 22.0 : 9.0, true);
                        }
                    }, b * 5L);
                }
            }
        }
    }
}
