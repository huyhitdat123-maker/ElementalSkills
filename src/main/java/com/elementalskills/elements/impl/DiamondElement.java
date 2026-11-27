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

public class DiamondElement implements ElementMastery {
    @Override public String getId() { return "kimcuong"; }
    @Override public String getDisplayName() { return "Hệ Kim Cương"; }
    @Override public NamedTextColor getThemeColor() { return NamedTextColor.AQUA; }
    @Override public Material getGuiIcon() { return Material.DIAMOND; }
    @Override public int[] getCooldownSeconds() { return new int[]{9, 12, 16, 22, 36}; }

    @Override
    public List<String> getSkillNames() {
        return List.of("Kim Cương Hộ Thể", "Toản Thạch Liên Xạ", "Tinh Thể Giáng Lâm", "Vạn Kiếm Tinh Thể", "Bất Hoại Kim Cương Giới");
    }

    @Override
    public List<String> getSkillDescriptions() {
        return List.of(
                "Bọc giáp tinh thể tăng phòng thủ & phản 30% sát thương cận chiến trong 6s.",
                "Bắn chùm 7 mảnh tinh thể kim cương xuyên giáp tốc độ cao.",
                "Rơi khối tinh thể khổng lồ xuống đầu mục tiêu nổ mảnh vỡ 5 block.",
                "Dựng 12 mũi tinh thể xoay quanh người 4s rồi phóng đồng loạt.",
                "Mở pháo đài tinh thể 11 block đâm xuyên mọi kẻ địch trong 5s."
        );
    }

    @Override
    public void castSkill(Player player, int skillIndex, CombatModeManager ctx) {
        switch (skillIndex) {
            case 0 -> { // Kim Cương Hộ Thể
                ctx.grantDiamondReflect(player, 0.30, 6);
                player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 120, 2));
                player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 120, 2));
                player.getWorld().spawnParticle(Particle.BLOCK, player.getLocation().add(0, 1, 0), 50, 0.8, 0.9, 0.8, Material.DIAMOND_BLOCK.createBlockData());
                SkillVisualHelper.playSound(player.getLocation(), "block.amethyst_block.chime", 1.5f, 1.2f);
            }
            case 1 -> { // Toản Thạch Liên Xạ
                Location eye = player.getEyeLocation();
                Vector forward = eye.getDirection().normalize();
                Vector right = new Vector(-forward.getZ(), 0, forward.getX()).normalize();
                SkillVisualHelper.playSound(eye, "block.glass.break", 1.4f, 1.6f);
                for (int i = -3; i <= 3; i++) {
                    Vector dir = forward.clone().add(right.clone().multiply(i * 0.12)).normalize();
                    Location end = eye.clone().add(dir.clone().multiply(18));
                    SkillVisualHelper.drawBeam(eye, end, Particle.BLOCK, 0.4, 2);
                    for (double d = 1; d <= 18; d += 1.5) {
                        for (LivingEntity t : SkillVisualHelper.getNearbyTargets(player, eye.clone().add(dir.clone().multiply(d)), 1.6, ctx)) {
                            SkillVisualHelper.dealSkillDamage(player, t, 10.5, true);
                        }
                    }
                }
            }
            case 2 -> { // Tinh Thể Giáng Lâm
                Location targetLoc = SkillVisualHelper.raycastLocation(player, 20.0);
                Location sky = targetLoc.clone().add(0, 12, 0);
                new BukkitRunnable() {
                    int step = 0;
                    @Override
                    public void run() {
                        step++;
                        Location cur = sky.clone().subtract(0, step * 1.2, 0);
                        cur.getWorld().spawnParticle(Particle.BLOCK, cur, 30, 0.9, 0.9, 0.9, Material.DIAMOND_BLOCK.createBlockData());
                        if (step >= 10) {
                            cancel();
                            targetLoc.getWorld().spawnParticle(Particle.EXPLOSION, targetLoc, 3, 1, 0.5, 1, 0);
                            SkillVisualHelper.playSound(targetLoc, "block.amethyst_cluster.break", 1.8f, 0.7f);
                            for (LivingEntity t : SkillVisualHelper.getNearbyTargets(player, targetLoc, 5.5, ctx)) {
                                SkillVisualHelper.dealSkillDamage(player, t, 16.0, true);
                                t.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 50, 3));
                            }
                        }
                    }
                }.runTaskTimer(ctx.getPlugin(), 0L, 1L);
            }
            case 3 -> { // Vạn Kiếm Tinh Thể
                SkillVisualHelper.playSound(player.getLocation(), "block.amethyst_block.resonate", 1.5f, 1.3f);
                new BukkitRunnable() {
                    int ticks = 0;
                    @Override
                    public void run() {
                        ticks += 4;
                        Location c = player.getLocation().add(0, 1.2, 0);
                        SkillVisualHelper.spawnRing(c, Particle.END_ROD, 2.8, 12);
                        for (LivingEntity t : SkillVisualHelper.getNearbyTargets(player, c, 4.0, ctx)) {
                            SkillVisualHelper.dealSkillDamage(player, t, 3.5, true);
                        }
                        if (ticks >= 80) {
                            cancel();
                            Location eye = player.getEyeLocation();
                            Vector dir = eye.getDirection().normalize();
                            SkillVisualHelper.drawBeam(eye, eye.clone().add(dir.clone().multiply(20)), Particle.END_ROD, 0.3, 5);
                            for (double d = 1; d <= 20; d += 1.5) {
                                for (LivingEntity t : SkillVisualHelper.getNearbyTargets(player, eye.clone().add(dir.clone().multiply(d)), 2.5, ctx)) {
                                    SkillVisualHelper.dealSkillDamage(player, t, 14.0, true);
                                }
                            }
                        }
                    }
                }.runTaskTimer(ctx.getPlugin(), 0L, 4L);
            }
            case 4 -> { // Bất Hoại Kim Cương Giới (Tuyệt Kỹ)
                Location c = player.getLocation();
                player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 100, 2));
                player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 100, 2));
                SkillVisualHelper.playSound(c, "block.beacon.activate", 2.0f, 1.1f);
                new BukkitRunnable() {
                    int ticks = 0;
                    @Override
                    public void run() {
                        ticks += 10;
                        SkillVisualHelper.spawnRing(c, Particle.END_ROD, 11.0, 60);
                        for (int k = 0; k < 5; k++) {
                            double ox = ThreadLocalRandom.current().nextDouble(-10.0, 10.0);
                            double oz = ThreadLocalRandom.current().nextDouble(-10.0, 10.0);
                            Location pillar = c.clone().add(ox, 0, oz);
                            pillar.getWorld().spawnParticle(Particle.BLOCK, pillar.clone().add(0, 1.5, 0), 25, 0.4, 1.5, 0.4, Material.DIAMOND_BLOCK.createBlockData());
                            for (LivingEntity t : SkillVisualHelper.getNearbyTargets(player, pillar, 3.0, ctx)) {
                                SkillVisualHelper.dealSkillDamage(player, t, 11.0, true);
                            }
                        }
                        if (ticks >= 100) cancel();
                    }
                }.runTaskTimer(ctx.getPlugin(), 0L, 10L);
            }
        }
    }
}
