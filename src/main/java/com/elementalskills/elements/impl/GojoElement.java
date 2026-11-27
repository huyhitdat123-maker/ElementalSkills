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

public class GojoElement implements ElementMastery {
    @Override public String getId() { return "gojo"; }
    @Override public String getDisplayName() { return "Hệ Vô Hạn (Gojo)"; }
    @Override public NamedTextColor getThemeColor() { return NamedTextColor.LIGHT_PURPLE; }
    @Override public Material getGuiIcon() { return Material.NETHER_STAR; }
    @Override public int[] getCooldownSeconds() { return new int[]{10, 13, 15, 24, 40}; }

    @Override
    public List<String> getSkillNames() {
        return List.of("Vô Hạ Hạn (Infinity)", "Thuật Thức: Thương (Blue)", "Thuật Thức: Hách (Red)", "Hư Thức: Tử (Purple)", "Lãnh Địa: Vô Lượng Không Xứ");
    }

    @Override
    public List<String> getSkillDescriptions() {
        return List.of(
                "Chặn 100% đạn bắn & đẩy văng kẻ địch áp sát trong 4 giây.",
                "Tạo điểm hút trọng lực xanh dương hút chặt & nghiền nát địch.",
                "Kích nổ sóng xung kích đỏ rực thổi bay kẻ địch xa 18 block.",
                "Hợp nhất Thương & Hách bắn quả cầu tím xé toạc mọi thứ.",
                "Triển khai Lãnh Địa 12 block đóng băng cử động kẻ địch 5s."
        );
    }

    @Override
    public void castSkill(Player player, int skillIndex, CombatModeManager ctx) {
        switch (skillIndex) {
            case 0 -> { // Vô Hạ Hạn (Infinity)
                ctx.grantInfinityShield(player, 4);
                SkillVisualHelper.playSound(player.getLocation(), "block.beacon.activate", 1.4f, 1.6f);
                new BukkitRunnable() {
                    int ticks = 0;
                    @Override
                    public void run() {
                        ticks += 4;
                        Location c = player.getLocation();
                        SkillVisualHelper.spawnRing(c.clone().add(0, 1, 0), Particle.END_ROD, 2.8, 24);
                        for (LivingEntity t : SkillVisualHelper.getNearbyTargets(player, c, 3.2, ctx)) {
                            Vector repel = t.getLocation().toVector().subtract(c.toVector()).normalize().multiply(1.4).setY(0.3);
                            t.setVelocity(repel);
                        }
                        if (ticks >= 80) cancel();
                    }
                }.runTaskTimer(ctx.getPlugin(), 0L, 4L);
            }
            case 1 -> { // Thuật Thức Thuận Chuyển: Thương (Blue)
                Location center = SkillVisualHelper.raycastLocation(player, 16.0);
                Particle.DustOptions blueDust = new Particle.DustOptions(Color.fromRGB(0, 140, 255), 2.0f);
                SkillVisualHelper.playSound(center, "block.portal.trigger", 1.4f, 1.5f);
                new BukkitRunnable() {
                    int ticks = 0;
                    @Override
                    public void run() {
                        ticks += 4;
                        center.getWorld().spawnParticle(Particle.DUST, center.clone().add(0, 1, 0), 40, 2.0, 2.0, 2.0, blueDust);
                        for (LivingEntity t : SkillVisualHelper.getNearbyTargets(player, center, 8.0, ctx)) {
                            Vector pull = center.toVector().subtract(t.getLocation().toVector()).normalize().multiply(0.75);
                            t.setVelocity(pull);
                            SkillVisualHelper.dealSkillDamage(player, t, 4.0, true);
                        }
                        if (ticks >= 60) cancel();
                    }
                }.runTaskTimer(ctx.getPlugin(), 0L, 4L);
            }
            case 2 -> { // Thuật Thức Nghịch Chuyển: Hách (Red)
                Location eye = player.getEyeLocation();
                Vector dir = eye.getDirection().normalize();
                Particle.DustOptions redDust = new Particle.DustOptions(Color.fromRGB(255, 30, 60), 2.2f);
                SkillVisualHelper.playSound(eye, "entity.generic.explode", 1.8f, 1.3f);
                Location blast = eye.clone().add(dir.clone().multiply(3.0));
                blast.getWorld().spawnParticle(Particle.DUST, blast, 80, 2.5, 2.5, 2.5, redDust);
                blast.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, blast, 1, 0, 0, 0, 0);
                for (LivingEntity t : SkillVisualHelper.getNearbyTargets(player, blast, 7.5, ctx)) {
                    SkillVisualHelper.dealSkillDamage(player, t, 16.0, true);
                    t.setVelocity(dir.clone().multiply(2.4).setY(0.8));
                }
            }
            case 3 -> { // Hư Thức: Tử (Hollow Purple)
                Location start = player.getEyeLocation();
                Vector dir = start.getDirection().normalize();
                Particle.DustOptions purpleDust = new Particle.DustOptions(Color.fromRGB(170, 0, 255), 2.8f);
                SkillVisualHelper.playSound(start, "entity.warden.sonic_boom", 1.8f, 1.1f);
                new BukkitRunnable() {
                    int step = 0;
                    @Override
                    public void run() {
                        if (++step > 24) { cancel(); return; }
                        Location cur = start.clone().add(dir.clone().multiply(step));
                        cur.getWorld().spawnParticle(Particle.DUST, cur, 55, 1.6, 1.6, 1.6, purpleDust);
                        cur.getWorld().spawnParticle(Particle.WITCH, cur, 25, 1.2, 1.2, 1.2, 0.05);
                        for (LivingEntity t : SkillVisualHelper.getNearbyTargets(player, cur, 3.5, ctx)) {
                            SkillVisualHelper.dealSkillDamage(player, t, 22.0, true);
                            t.setVelocity(dir.clone().multiply(1.5));
                        }
                    }
                }.runTaskTimer(ctx.getPlugin(), 0L, 1L);
            }
            case 4 -> { // Lãnh Địa Triển Khai: Vô Lượng Không Xứ (Tuyệt Kỹ)
                Location center = player.getLocation();
                SkillVisualHelper.playSound(center, "entity.ender_dragon.death", 1.5f, 1.4f);
                player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 120, 2));
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 120, 2));

                List<LivingEntity> trapped = SkillVisualHelper.getNearbyTargets(player, center, 12.0, ctx);
                for (LivingEntity t : trapped) {
                    ctx.applyHardFreeze(t, 100);
                    t.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 100, 0));
                }

                new BukkitRunnable() {
                    int ticks = 0;
                    @Override
                    public void run() {
                        ticks += 10;
                        SkillVisualHelper.spawnRing(center, Particle.END_ROD, 12.0, 64);
                        SkillVisualHelper.spawnRing(center.clone().add(0, 3, 0), Particle.WITCH, 12.0, 64);
                        if (ticks >= 100) cancel();
                    }
                }.runTaskTimer(ctx.getPlugin(), 0L, 10L);
            }
        }
    }
}
