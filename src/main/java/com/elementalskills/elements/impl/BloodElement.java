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

public class BloodElement implements ElementMastery {
    @Override public String getId() { return "huyet"; }
    @Override public String getDisplayName() { return "Hệ Huyết"; }
    @Override public NamedTextColor getThemeColor() { return NamedTextColor.DARK_RED; }
    @Override public Material getGuiIcon() { return Material.CRIMSON_FUNGUS; }
    @Override public int[] getCooldownSeconds() { return new int[]{7, 11, 15, 22, 36}; }

    @Override
    public List<String> getSkillNames() {
        return List.of("Huyết Trảm Cuồng Bạo", "Huyết Thương Xuyên Tim", "Huyết Vụ Hộ Thân", "Huyết Trì Lĩnh Vực", "Huyết Thần Giáng Thế");
    }

    @Override
    public List<String> getSkillDescriptions() {
        return List.of(
                "Chém kiếm khí đỏ thẫm & hút lượng máu lớn khi trúng đích.",
                "Phóng 3 ngọn giáo huyết sắc bạo kích kẻ địch dưới 50% máu.",
                "Hóa sương mù máu 3s lướt xuyên địch gây chảy máu & miễn sát thương.",
                "Mở hồ máu bán kính 7 block hút sinh lực địch trong 4.5s.",
                "Hóa Huyết Thần 6s giải phóng 3 đợt sóng huyết sắc bán kính 12 block."
        );
    }

    @Override
    public void castSkill(Player player, int skillIndex, CombatModeManager ctx) {
        Particle.DustOptions bloodDust = new Particle.DustOptions(Color.fromRGB(190, 0, 20), 2.0f);
        switch (skillIndex) {
            case 0 -> { // Huyết Trảm Cuồng Bạo
                Location start = player.getEyeLocation();
                Vector dir = start.getDirection().normalize();
                Location end = start.clone().add(dir.clone().multiply(14));
                SkillVisualHelper.drawBeam(start, end, Particle.DUST, 0.35, 3);
                SkillVisualHelper.playSound(start, "entity.player.attack.sweep", 1.4f, 0.7f);
                for (double d = 1; d <= 14; d += 1.5) {
                    for (LivingEntity t : SkillVisualHelper.getNearbyTargets(player, start.clone().add(dir.clone().multiply(d)), 2.2, ctx)) {
                        SkillVisualHelper.dealSkillDamage(player, t, 12.0, true);
                        SkillVisualHelper.healPlayer(player, 4.0);
                    }
                }
            }
            case 1 -> { // Huyết Thương Xuyên Tim
                for (int s = 0; s < 3; s++) {
                    Bukkit.getScheduler().runTaskLater(ctx.getPlugin(), () -> {
                        Location start = player.getEyeLocation();
                        Vector dir = start.getDirection().normalize();
                        Location end = start.clone().add(dir.clone().multiply(20));
                        for (double d = 1; d <= 20; d += 0.5) {
                            start.getWorld().spawnParticle(Particle.DUST, start.clone().add(dir.clone().multiply(d)), 2, 0.1, 0.1, 0.1, bloodDust);
                        }
                        SkillVisualHelper.playSound(start, "item.trident.throw", 1.3f, 0.8f);
                        for (double d = 1; d <= 20; d += 1.5) {
                            for (LivingEntity t : SkillVisualHelper.getNearbyTargets(player, start.clone().add(dir.clone().multiply(d)), 1.8, ctx)) {
                                double dmg = (t.getHealth() < 10.0) ? 12.0 : 8.0;
                                SkillVisualHelper.dealSkillDamage(player, t, dmg, true);
                            }
                        }
                    }, s * 5L);
                }
            }
            case 2 -> { // Huyết Vụ Hộ Thân
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 60, 3));
                player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 60, 4));
                SkillVisualHelper.playSound(player.getLocation(), "entity.phantom.flap", 1.5f, 0.7f);
                new BukkitRunnable() {
                    int ticks = 0;
                    @Override
                    public void run() {
                        ticks += 4;
                        Location c = player.getLocation().add(0, 1, 0);
                        c.getWorld().spawnParticle(Particle.DUST, c, 35, 1.0, 0.8, 1.0, bloodDust);
                        for (LivingEntity t : SkillVisualHelper.getNearbyTargets(player, c, 2.8, ctx)) {
                            SkillVisualHelper.dealSkillDamage(player, t, 4.5, true);
                            SkillVisualHelper.healPlayer(player, 1.5);
                        }
                        if (ticks >= 60) cancel();
                    }
                }.runTaskTimer(ctx.getPlugin(), 0L, 4L);
            }
            case 3 -> { // Huyết Trì Lĩnh Vực
                Location c = player.getLocation();
                SkillVisualHelper.playSound(c, "block.lava.ambient", 1.5f, 0.6f);
                new BukkitRunnable() {
                    int ticks = 0;
                    @Override
                    public void run() {
                        ticks += 10;
                        SkillVisualHelper.spawnRing(c, Particle.CRIMSON_SPORE, 7.0, 45);
                        c.getWorld().spawnParticle(Particle.DUST, c.clone().add(0, 0.4, 0), 40, 3.5, 0.2, 3.5, bloodDust);
                        for (LivingEntity t : SkillVisualHelper.getNearbyTargets(player, c, 7.0, ctx)) {
                            SkillVisualHelper.dealSkillDamage(player, t, 4.5, true);
                            SkillVisualHelper.healPlayer(player, 2.0);
                        }
                        if (ticks >= 90) cancel();
                    }
                }.runTaskTimer(ctx.getPlugin(), 0L, 10L);
            }
            case 4 -> { // Huyết Thần Giáng Thế (Tuyệt Kỹ)
                player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 120, 2));
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 120, 2));
                SkillVisualHelper.playSound(player.getLocation(), "entity.wither.spawn", 1.8f, 0.6f);
                for (int wave = 0; wave < 3; wave++) {
                    Bukkit.getScheduler().runTaskLater(ctx.getPlugin(), () -> {
                        Location c = player.getLocation();
                        for (double r = 2.0; r <= 12.0; r += 2.0) {
                            SkillVisualHelper.spawnRing(c.clone().add(0, 1, 0), Particle.CRIMSON_SPORE, r, 36);
                        }
                        for (LivingEntity t : SkillVisualHelper.getNearbyTargets(player, c, 12.0, ctx)) {
                            SkillVisualHelper.dealSkillDamage(player, t, 12.0, true);
                            SkillVisualHelper.healPlayer(player, 2.5);
                        }
                    }, wave * 25L);
                }
            }
        }
    }
}
