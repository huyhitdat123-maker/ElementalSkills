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

public class WoodElement implements ElementMastery {
    @Override public String getId() { return "moc"; }
    @Override public String getDisplayName() { return "Hệ Mộc"; }
    @Override public NamedTextColor getThemeColor() { return NamedTextColor.GREEN; }
    @Override public Material getGuiIcon() { return Material.SPORE_BLOSSOM; }
    @Override public int[] getCooldownSeconds() { return new int[]{9, 12, 15, 22, 36}; }

    @Override
    public List<String> getSkillNames() {
        return List.of("Dây Leo Trói Chân", "Hạt Giống Ký Sinh", "Mưa Gai Độc Rừng Sâu", "Cổ Thụ Hộ Mệnh", "Rừng Rậm Nuốt Chửng");
    }

    @Override
    public List<String> getSkillDescriptions() {
        return List.of(
                "Trói cứng kẻ địch bán kính 6 block suốt 3 giây & gây độc.",
                "Cấy hạt giống hút sinh lực mục tiêu hồi máu cho bản thân 5s.",
                "Bắn tỏa chùm 9 gai độc xuyên giáp & làm chậm nặng.",
                "Triệu hồi Cổ Thụ hồi máu liên tục & tăng Kháng Cự trong 6s.",
                "Trồi hàng chục rễ cây khổng lồ trói & nghiền nát bán kính 12 block."
        );
    }

    @Override
    public void castSkill(Player player, int skillIndex, CombatModeManager ctx) {
        switch (skillIndex) {
            case 0 -> { // Dây Leo Trói Chân
                Location c = player.getLocation();
                SkillVisualHelper.spawnRing(c, Particle.HAPPY_VILLAGER, 6.0, 40);
                SkillVisualHelper.playSound(c, "block.vine.place", 1.5f, 0.8f);
                for (LivingEntity t : SkillVisualHelper.getNearbyTargets(player, c, 6.0, ctx)) {
                    ctx.applyHardFreeze(t, 60);
                    t.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 80, 1));
                    SkillVisualHelper.dealSkillDamage(player, t, 8.0, false);
                }
            }
            case 1 -> { // Hạt Giống Ký Sinh
                LivingEntity target = SkillVisualHelper.raycastTarget(player, 18.0, 1.8, ctx);
                if (target == null) return;
                SkillVisualHelper.drawBeam(player.getEyeLocation(), target.getEyeLocation(), Particle.HAPPY_VILLAGER, 0.3, 3);
                new BukkitRunnable() {
                    int ticks = 0;
                    @Override
                    public void run() {
                        ticks += 20;
                        if (!target.isValid() || target.isDead()) { cancel(); return; }
                        target.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, target.getLocation().add(0, 1, 0), 15, 0.5, 0.6, 0.5, 0.05);
                        SkillVisualHelper.dealSkillDamage(player, target, 4.5, true);
                        SkillVisualHelper.healPlayer(player, 3.0);
                        if (ticks >= 100) cancel();
                    }
                }.runTaskTimer(ctx.getPlugin(), 0L, 20L);
            }
            case 2 -> { // Mưa Gai Độc Rừng Sâu
                Location eye = player.getEyeLocation();
                Vector forward = eye.getDirection().normalize();
                Vector right = new Vector(-forward.getZ(), 0, forward.getX()).normalize();
                SkillVisualHelper.playSound(eye, "entity.arrow.shoot", 1.4f, 1.3f);
                for (int i = -4; i <= 4; i++) {
                    Vector dir = forward.clone().add(right.clone().multiply(i * 0.11)).normalize();
                    Location end = eye.clone().add(dir.clone().multiply(16));
                    SkillVisualHelper.drawBeam(eye, end, Particle.SPORE_BLOSSOM_AIR, 0.4, 3);
                    for (double d = 1; d <= 16; d += 1.5) {
                        for (LivingEntity t : SkillVisualHelper.getNearbyTargets(player, eye.clone().add(dir.clone().multiply(d)), 1.6, ctx)) {
                            SkillVisualHelper.dealSkillDamage(player, t, 8.5, true);
                            t.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 100, 1));
                            t.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 2));
                        }
                    }
                }
            }
            case 3 -> { // Cổ Thụ Hộ Mệnh
                Location tree = player.getLocation();
                SkillVisualHelper.playSound(tree, "block.azalea_leaves.place", 1.6f, 0.8f);
                new BukkitRunnable() {
                    int ticks = 0;
                    @Override
                    public void run() {
                        ticks += 10;
                        SkillVisualHelper.spawnRing(tree, Particle.HAPPY_VILLAGER, 6.0, 35);
                        tree.getWorld().spawnParticle(Particle.CHERRY_LEAVES, tree.clone().add(0, 3, 0), 30, 2.5, 1.5, 2.5, 0.05);
                        if (player.getLocation().distanceSquared(tree) <= 36) {
                            SkillVisualHelper.healPlayer(player, 2.5);
                            player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 30, 1));
                        }
                        if (ticks >= 120) cancel();
                    }
                }.runTaskTimer(ctx.getPlugin(), 0L, 10L);
            }
            case 4 -> { // Rừng Rậm Nuốt Chửng (Tuyệt Kỹ)
                Location targetLoc = SkillVisualHelper.raycastLocation(player, 22.0);
                SkillVisualHelper.playSound(targetLoc, "entity.warden.tendril_clicks", 1.8f, 0.7f);
                new BukkitRunnable() {
                    int wave = 0;
                    @Override
                    public void run() {
                        wave++;
                        SkillVisualHelper.spawnRing(targetLoc, Particle.HAPPY_VILLAGER, 12.0, 55);
                        for (int r = 0; r < 5; r++) {
                            double ox = ThreadLocalRandom.current().nextDouble(-10.0, 10.0);
                            double oz = ThreadLocalRandom.current().nextDouble(-10.0, 10.0);
                            Location root = targetLoc.clone().add(ox, 0, oz);
                            root.getWorld().spawnParticle(Particle.BLOCK, root.clone().add(0, 1.5, 0), 25, 0.4, 1.5, 0.4, Material.OAK_WOOD.createBlockData());
                            for (LivingEntity t : SkillVisualHelper.getNearbyTargets(player, root, 3.2, ctx)) {
                                ctx.applyHardFreeze(t, 40);
                                SkillVisualHelper.dealSkillDamage(player, t, 10.5, true);
                                t.setVelocity(new Vector(0, 0.9, 0));
                            }
                        }
                        if (wave >= 8) cancel();
                    }
                }.runTaskTimer(ctx.getPlugin(), 0L, 10L);
            }
        }
    }
}
