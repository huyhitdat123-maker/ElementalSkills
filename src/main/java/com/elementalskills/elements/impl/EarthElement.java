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

public class EarthElement implements ElementMastery {
    @Override public String getId() { return "dat"; }
    @Override public String getDisplayName() { return "Hệ Đất"; }
    @Override public NamedTextColor getThemeColor() { return NamedTextColor.GOLD; }
    @Override public Material getGuiIcon() { return Material.POINTED_DRIPSTONE; }
    @Override public int[] getCooldownSeconds() { return new int[]{8, 12, 16, 22, 35}; }

    @Override
    public List<String> getSkillNames() {
        return List.of("Địa Chấn Nứt Đất", "Thạch Thứ Xuyên Tâm", "Nham Thạch Hộ Giáp", "Thiên Thạch Nghiền Ép", "Đại Địa Sụp Đổ");
    }

    @Override
    public List<String> getSkillDescriptions() {
        return List.of(
                "Dậm chấn động bán kính 6 block hất tung toàn bộ kẻ địch.",
                "Trồi 3 hàng gai đá nhọn lao thẳng 15 block đâm xuyên địch.",
                "Bọc giáp đá nhận Kháng Cự, Sức Mạnh & kháng đẩy lùi 6s.",
                "Ném khối thiên thạch khổng lồ nổ tung bán kính 7 block.",
                "Phun trào hàng chục cột đá nhọn khắp bán kính 12 block."
        );
    }

    @Override
    public void castSkill(Player player, int skillIndex, CombatModeManager ctx) {
        switch (skillIndex) {
            case 0 -> { // Địa Chấn Nứt Đất
                Location c = player.getLocation();
                SkillVisualHelper.playSound(c, "entity.iron_golem.attack", 1.5f, 0.6f);
                c.getWorld().spawnParticle(Particle.BLOCK, c, 90, 3.5, 0.3, 3.5, Material.DIRT.createBlockData());
                for (LivingEntity t : SkillVisualHelper.getNearbyTargets(player, c, 6.0, ctx)) {
                    SkillVisualHelper.dealSkillDamage(player, t, 11.0, false);
                    t.setVelocity(new Vector(0, 1.05, 0));
                }
            }
            case 1 -> { // Thạch Thứ Xuyên Tâm
                Location origin = player.getLocation();
                Vector forward = origin.getDirection().setY(0).normalize();
                Vector right = new Vector(-forward.getZ(), 0, forward.getX()).normalize();
                SkillVisualHelper.playSound(origin, "block.pointed_dripstone.break", 1.5f, 0.7f);
                for (int lane = -1; lane <= 1; lane++) {
                    Vector dir = forward.clone().add(right.clone().multiply(lane * 0.25)).normalize();
                    new BukkitRunnable() {
                        int step = 0;
                        @Override
                        public void run() {
                            if (++step > 15) { cancel(); return; }
                            Location pt = origin.clone().add(dir.clone().multiply(step));
                            pt.getWorld().spawnParticle(Particle.BLOCK, pt.clone().add(0, 1, 0), 20, 0.3, 0.8, 0.3, Material.STONE.createBlockData());
                            for (LivingEntity t : SkillVisualHelper.getNearbyTargets(player, pt, 2.0, ctx)) {
                                SkillVisualHelper.dealSkillDamage(player, t, 10.5, true);
                                t.setVelocity(new Vector(0, 0.75, 0));
                            }
                        }
                    }.runTaskTimer(ctx.getPlugin(), 0L, 1L);
                }
            }
            case 2 -> { // Nham Thạch Hộ Giáp
                SkillVisualHelper.playSound(player.getLocation(), "block.anvil.use", 1.2f, 0.7f);
                player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 120, 2));
                player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 120, 1));
                player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 120, 2));
                player.getWorld().spawnParticle(Particle.BLOCK, player.getLocation().add(0, 1, 0), 50, 0.8, 0.9, 0.8, Material.OBSIDIAN.createBlockData());
            }
            case 3 -> { // Thiên Thạch Nghiền Ép
                Location targetLoc = SkillVisualHelper.raycastLocation(player, 22.0);
                Location start = player.getEyeLocation().add(0, 3, 0);
                new BukkitRunnable() {
                    int step = 0;
                    @Override
                    public void run() {
                        step++;
                        Location cur = start.clone().add(targetLoc.toVector().subtract(start.toVector()).multiply(step / 10.0));
                        cur.getWorld().spawnParticle(Particle.BLOCK, cur, 35, 1.0, 1.0, 1.0, Material.COBBLESTONE.createBlockData());
                        if (step >= 10) {
                            cancel();
                            targetLoc.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, targetLoc, 2, 1, 0.5, 1, 0);
                            SkillVisualHelper.playSound(targetLoc, "entity.generic.explode", 1.8f, 0.6f);
                            for (LivingEntity t : SkillVisualHelper.getNearbyTargets(player, targetLoc, 7.0, ctx)) {
                                SkillVisualHelper.dealSkillDamage(player, t, 18.0, true);
                                t.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 3));
                            }
                        }
                    }
                }.runTaskTimer(ctx.getPlugin(), 0L, 1L);
            }
            case 4 -> { // Đại Địa Sụp Đổ (Tuyệt Kỹ)
                Location c = player.getLocation();
                SkillVisualHelper.playSound(c, "entity.warden.sonic_boom", 1.8f, 0.5f);
                new BukkitRunnable() {
                    int wave = 0;
                    @Override
                    public void run() {
                        wave++;
                        SkillVisualHelper.spawnRing(c, Particle.BLOCK, 12.0, 55);
                        for (int k = 0; k < 6; k++) {
                            double ox = ThreadLocalRandom.current().nextDouble(-11.0, 11.0);
                            double oz = ThreadLocalRandom.current().nextDouble(-11.0, 11.0);
                            Location spike = c.clone().add(ox, 0, oz);
                            spike.getWorld().spawnParticle(Particle.BLOCK, spike.clone().add(0, 1.5, 0), 30, 0.5, 1.5, 0.5, Material.POINTED_DRIPSTONE.createBlockData());
                            for (LivingEntity t : SkillVisualHelper.getNearbyTargets(player, spike, 3.0, ctx)) {
                                SkillVisualHelper.dealSkillDamage(player, t, 12.0, true);
                                t.setVelocity(new Vector(0, 1.15, 0));
                            }
                        }
                        if (wave >= 8) cancel();
                    }
                }.runTaskTimer(ctx.getPlugin(), 0L, 8L);
            }
        }
    }
}
