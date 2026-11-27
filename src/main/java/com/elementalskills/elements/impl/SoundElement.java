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
import org.bukkit.util.Vector;

import java.util.List;

public class SoundElement implements ElementMastery {
    @Override public String getId() { return "amthanh"; }
    @Override public String getDisplayName() { return "Hệ Âm Thanh"; }
    @Override public NamedTextColor getThemeColor() { return NamedTextColor.DARK_AQUA; }
    @Override public Material getGuiIcon() { return Material.SCULK_SHRIEKER; }
    @Override public int[] getCooldownSeconds() { return new int[]{8, 12, 15, 20, 36}; }

    @Override
    public List<String> getSkillNames() {
        return List.of("Sóng Âm Xung Kích", "Tiếng Thét Chói Tai", "Âm Vang Dội Lại", "Bức Tường Thanh Âm", "Khúc Nhạc Hủy Diệt");
    }

    @Override
    public List<String> getSkillDescriptions() {
        return List.of(
                "Bắn sóng âm xuyên khiên đẩy văng kẻ địch tầm xa 15 block.",
                "Phát tần số cực đại bán kính 7 block gây sát thương xuyên giáp & mù.",
                "Đánh dấu cộng hưởng 4s khiến mọi đòn đánh gây thêm sát thương chuẩn.",
                "Dựng kết giới thanh âm quanh người hất văng kẻ địch áp sát.",
                "Phóng 3 luồng Sonic Boom của Warden liên tiếp tầm xa 24 block."
        );
    }

    @Override
    public void castSkill(Player player, int skillIndex, CombatModeManager ctx) {
        switch (skillIndex) {
            case 0 -> { // Sóng Âm Xung Kích
                Location start = player.getEyeLocation();
                Vector dir = start.getDirection().normalize();
                SkillVisualHelper.drawBeam(start, start.clone().add(dir.clone().multiply(15)), Particle.SONIC_BOOM, 1.5, 1);
                SkillVisualHelper.playSound(start, "entity.warden.sonic_boom", 1.2f, 1.4f);
                for (double d = 1; d <= 15; d += 2.0) {
                    for (LivingEntity t : SkillVisualHelper.getNearbyTargets(player, start.clone().add(dir.clone().multiply(d)), 2.5, ctx)) {
                        SkillVisualHelper.dealSkillDamage(player, t, 11.0, true);
                        t.setVelocity(dir.clone().multiply(1.8).setY(0.5));
                    }
                }
            }
            case 1 -> { // Tiếng Thét Chói Tai
                Location c = player.getLocation();
                c.getWorld().spawnParticle(Particle.NOTE, c.clone().add(0, 1.5, 0), 50, 3.5, 1.0, 3.5, 1.0);
                SkillVisualHelper.playSound(c, "block.sculk_shrieker.shriek", 1.8f, 1.3f);
                for (LivingEntity t : SkillVisualHelper.getNearbyTargets(player, c, 7.0, ctx)) {
                    SkillVisualHelper.dealSkillDamage(player, t, 12.0, true);
                    t.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 60, 0));
                    t.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 80, 1));
                }
            }
            case 2 -> { // Âm Vang Dội Lại
                LivingEntity target = SkillVisualHelper.raycastTarget(player, 18.0, 2.0, ctx);
                if (target == null) return;
                ctx.markSoundResonance(target, 5.0, 4);
                target.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 80, 0));
                target.getWorld().spawnParticle(Particle.NOTE, target.getLocation().add(0, 1.5, 0), 25, 0.6, 0.6, 0.6, 1.0);
                SkillVisualHelper.playSound(target.getLocation(), "block.bell.resonate", 1.5f, 1.2f);
            }
            case 3 -> { // Bức Tường Thanh Âm
                Location c = player.getLocation();
                SkillVisualHelper.spawnRing(c.clone().add(0, 1, 0), Particle.NOTE, 5.5, 36);
                SkillVisualHelper.playSound(c, "entity.warden.roar", 1.5f, 1.2f);
                for (LivingEntity t : SkillVisualHelper.getNearbyTargets(player, c, 6.0, ctx)) {
                    SkillVisualHelper.dealSkillDamage(player, t, 10.0, true);
                    t.setVelocity(t.getLocation().toVector().subtract(c.toVector()).normalize().multiply(2.0).setY(0.6));
                }
            }
            case 4 -> { // Khúc Nhạc Hủy Diệt (3 luồng Sonic Boom — Tuyệt Kỹ)
                for (int boom = 0; boom < 3; boom++) {
                    Bukkit.getScheduler().runTaskLater(ctx.getPlugin(), () -> {
                        Location start = player.getEyeLocation();
                        Vector dir = start.getDirection().normalize();
                        SkillVisualHelper.drawBeam(start, start.clone().add(dir.clone().multiply(24)), Particle.SONIC_BOOM, 1.2, 1);
                        SkillVisualHelper.playSound(start, "entity.warden.sonic_boom", 2.0f, 0.9f);
                        for (double d = 1; d <= 24; d += 1.5) {
                            for (LivingEntity t : SkillVisualHelper.getNearbyTargets(player, start.clone().add(dir.clone().multiply(d)), 2.8, ctx)) {
                                SkillVisualHelper.dealSkillDamage(player, t, 13.0, true);
                                t.setVelocity(dir.clone().multiply(1.5).setY(0.4));
                            }
                        }
                    }, boom * 8L);
                }
            }
        }
    }
}
