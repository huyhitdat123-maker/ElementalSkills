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

public class IceElement implements ElementMastery {
    @Override public String getId() { return "bang"; }
    @Override public String getDisplayName() { return "Hệ Băng"; }
    @Override public NamedTextColor getThemeColor() { return NamedTextColor.AQUA; }
    @Override public Material getGuiIcon() { return Material.BLUE_ICE; }
    @Override public int[] getCooldownSeconds() { return new int[]{8, 12, 16, 24, 35}; }

    @Override
    public List<String> getSkillNames() {
        return List.of("Mưa Gai Băng", "Băng Thương Xuyên Phá", "Băng Tường Pháo Đài", "Tuyệt Đối Lĩnh Vực", "Kỷ Băng Hà Tận Thế");
    }

    @Override
    public List<String> getSkillDescriptions() {
        return List.of(
                "Phóng chùm 7 mũi băng nhọn hình nón đóng băng kẻ địch.",
                "Triệu hồi 3 giáo băng khổng lồ phóng dồn dập vào mục tiêu.",
                "Dựng kết giới băng quanh người rồi kích nổ văng mảnh băng.",
                "Đóng băng cứng toàn bộ địch trong 7 block suốt 3.5s rồi vỡ vụn.",
                "Gọi mưa thiên thạch băng trút xuống bán kính 12 block trong 5s."
        );
    }

    @Override
    public void castSkill(Player player, int skillIndex, CombatModeManager ctx) {
        switch (skillIndex) {
            case 0 -> { // Mưa Gai Băng
                Location eye = player.getEyeLocation();
                Vector forward = eye.getDirection().normalize();
                Vector right = new Vector(-forward.getZ(), 0, forward.getX()).normalize();
                SkillVisualHelper.playSound(eye, "block.glass.break", 1.3f, 1.4f);
                for (int i = -3; i <= 3; i++) {
                    Vector dir = forward.clone().add(right.clone().multiply(i * 0.14)).normalize();
                    Location end = eye.clone().add(dir.clone().multiply(16));
                    SkillVisualHelper.drawBeam(eye, end, Particle.SNOWFLAKE, 0.5, 2);
                    for (double d = 1; d <= 16; d += 1.5) {
                        for (LivingEntity t : SkillVisualHelper.getNearbyTargets(player, eye.clone().add(dir.clone().multiply(d)), 1.6, ctx)) {
                            SkillVisualHelper.dealSkillDamage(player, t, 9.5, true);
                            t.setFreezeTicks(140);
                            t.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 80, 2));
                        }
                    }
                }
            }
            case 1 -> { // Băng Thương Xuyên Phá
                for (int lance = 0; lance < 3; lance++) {
                    final int idx = lance;
                    Bukkit.getScheduler().runTaskLater(ctx.getPlugin(), () -> {
                        Location start = player.getEyeLocation().add(0, 0.5, 0);
                        Vector dir = start.getDirection().normalize();
                        Location end = start.clone().add(dir.clone().multiply(22));
                        SkillVisualHelper.drawBeam(start, end, Particle.ITEM_SNOWBALL, 0.3, 4);
                        SkillVisualHelper.playSound(start, "entity.arrow.shoot", 1.4f, 0.7f + idx * 0.2f);
                        for (double d = 1; d <= 22; d += 1.5) {
                            for (LivingEntity t : SkillVisualHelper.getNearbyTargets(player, start.clone().add(dir.clone().multiply(d)), 1.8, ctx)) {
                                SkillVisualHelper.dealSkillDamage(player, t, 8.0 + idx * 3.0, true);
                                t.setFreezeTicks(160);
                            }
                        }
                    }, idx * 6L);
                }
            }
            case 2 -> { // Băng Tường Pháo Đài
                Location center = player.getLocation();
                SkillVisualHelper.playSound(center, "block.glass.place", 1.5f, 0.8f);
                player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 60, 2));
                new BukkitRunnable() {
                    int ticks = 0;
                    @Override
                    public void run() {
                        ticks += 5;
                        for (double y = 0; y <= 2.5; y += 0.8) {
                            SkillVisualHelper.spawnRing(center.clone().add(0, y, 0), Particle.SNOWFLAKE, 3.0, 28);
                        }
                        if (ticks >= 60) {
                            cancel();
                            center.getWorld().spawnParticle(Particle.EXPLOSION, center, 3, 1.5, 0.5, 1.5, 0);
                            SkillVisualHelper.playSound(center, "block.glass.break", 1.8f, 0.6f);
                            for (LivingEntity t : SkillVisualHelper.getNearbyTargets(player, center, 6.5, ctx)) {
                                SkillVisualHelper.dealSkillDamage(player, t, 15.0, true);
                                t.setVelocity(t.getLocation().toVector().subtract(center.toVector()).normalize().multiply(1.5).setY(0.6));
                            }
                        }
                    }
                }.runTaskTimer(ctx.getPlugin(), 0L, 5L);
            }
            case 3 -> { // Tuyệt Đối Lĩnh Vực
                Location center = player.getLocation();
                SkillVisualHelper.playSound(center, "entity.player.hurt_freeze", 1.6f, 0.6f);
                List<LivingEntity> frozen = SkillVisualHelper.getNearbyTargets(player, center, 7.0, ctx);
                for (LivingEntity t : frozen) {
                    ctx.applyHardFreeze(t, 70);
                    t.setFreezeTicks(200);
                }
                new BukkitRunnable() {
                    int ticks = 0;
                    @Override
                    public void run() {
                        ticks += 10;
                        SkillVisualHelper.spawnRing(center, Particle.SNOWFLAKE, 7.0, 45);
                        if (ticks >= 70) {
                            cancel();
                            SkillVisualHelper.playSound(center, "block.glass.break", 2.0f, 0.5f);
                            for (LivingEntity t : frozen) {
                                if (t.isValid() && !t.isDead()) {
                                    t.getWorld().spawnParticle(Particle.ITEM_SNOWBALL, t.getLocation().add(0, 1, 0), 35, 0.6, 0.8, 0.6, 0.2);
                                    SkillVisualHelper.dealSkillDamage(player, t, 18.0, true);
                                }
                            }
                        }
                    }
                }.runTaskTimer(ctx.getPlugin(), 0L, 10L);
            }
            case 4 -> { // Kỷ Băng Hà Tận Thế (Tuyệt Kỹ)
                Location targetLoc = SkillVisualHelper.raycastLocation(player, 24.0);
                SkillVisualHelper.playSound(targetLoc, "entity.wither.ambient", 1.5f, 1.4f);
                new BukkitRunnable() {
                    int ticks = 0;
                    @Override
                    public void run() {
                        ticks += 5;
                        SkillVisualHelper.spawnRing(targetLoc, Particle.SNOWFLAKE, 12.0, 60);
                        for (int k = 0; k < 4; k++) {
                            double ox = ThreadLocalRandom.current().nextDouble(-10.0, 10.0);
                            double oz = ThreadLocalRandom.current().nextDouble(-10.0, 10.0);
                            Location strike = targetLoc.clone().add(ox, 0, oz);
                            SkillVisualHelper.drawBeam(strike.clone().add(0, 14, 0), strike, Particle.SNOWFLAKE, 0.6, 4);
                            strike.getWorld().spawnParticle(Particle.ITEM_SNOWBALL, strike, 25, 1.5, 0.4, 1.5, 0.15);
                            for (LivingEntity t : SkillVisualHelper.getNearbyTargets(player, strike, 3.8, ctx)) {
                                SkillVisualHelper.dealSkillDamage(player, t, 10.0, true);
                                t.setFreezeTicks(180);
                                t.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 3));
                            }
                        }
                        if (ticks >= 100) cancel();
                    }
                }.runTaskTimer(ctx.getPlugin(), 0L, 5L);
            }
        }
    }
}
