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

public class LightElement implements ElementMastery {
    @Override public String getId() { return "quang"; }
    @Override public String getDisplayName() { return "Hệ Quang"; }
    @Override public NamedTextColor getThemeColor() { return NamedTextColor.YELLOW; }
    @Override public Material getGuiIcon() { return Material.END_ROD; }
    @Override public int[] getCooldownSeconds() { return new int[]{9, 12, 16, 22, 36}; }

    @Override
    public List<String> getSkillNames() {
        return List.of("Quang Minh Hộ Thuẫn", "Thánh Quang Phán Quyết", "Thiên Sứ Giáng Lâm", "Thánh Quang Hủy Diệt", "Thẩm Phán Thiên Đàng");
    }

    @Override
    public List<String> getSkillDescriptions() {
        return List.of(
                "Xóa mọi hiệu ứng xấu, hồi máu lớn & tạo khiên ánh sáng.",
                "Bắn tia laser thánh quang xuyên giáp tầm xa 22 block.",
                "Lướt bay ánh sáng làm mù mắt kẻ địch & hồi máu đồng minh.",
                "Phóng 3 luồng ánh sáng vàng cam song song càn quét phía trước.",
                "Gọi cột ánh sáng từ trời thiêu rụi bán kính 9 block trong 4s."
        );
    }

    @Override
    public void castSkill(Player player, int skillIndex, CombatModeManager ctx) {
        switch (skillIndex) {
            case 0 -> { // Quang Minh Hộ Thuẫn
                for (PotionEffect eff : player.getActivePotionEffects()) {
                    player.removePotionEffect(eff.getType());
                }
                SkillVisualHelper.healPlayer(player, 8.0);
                player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 160, 1));
                player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 100, 1));
                player.getWorld().spawnParticle(Particle.END_ROD, player.getLocation().add(0, 1, 0), 50, 1.0, 1.0, 1.0, 0.08);
                SkillVisualHelper.playSound(player.getLocation(), "block.beacon.activate", 1.3f, 1.3f);
            }
            case 1 -> { // Thánh Quang Phán Quyết
                Location start = player.getEyeLocation();
                Vector dir = start.getDirection().normalize();
                Location end = start.clone().add(dir.clone().multiply(22));
                SkillVisualHelper.drawBeam(start, end, Particle.END_ROD, 0.3, 3);
                SkillVisualHelper.drawBeam(start, end, Particle.FLAME, 0.5, 2);
                SkillVisualHelper.playSound(start, "entity.guardian.attack", 1.5f, 1.4f);
                for (double d = 1; d <= 22; d += 1.5) {
                    for (LivingEntity t : SkillVisualHelper.getNearbyTargets(player, start.clone().add(dir.clone().multiply(d)), 2.0, ctx)) {
                        SkillVisualHelper.dealSkillDamage(player, t, 15.0, true);
                        t.setFireTicks(80);
                    }
                }
            }
            case 2 -> { // Thiên Sứ Giáng Lâm
                ctx.grantFallImmunity(player, 6);
                player.setVelocity(player.getLocation().getDirection().multiply(1.4).setY(0.8));
                Location c = player.getLocation();
                c.getWorld().spawnParticle(Particle.END_ROD, c.clone().add(0, 1, 0), 70, 2.5, 1.5, 2.5, 0.1);
                SkillVisualHelper.playSound(c, "entity.allay.ambient_without_item", 1.5f, 1.2f);
                for (LivingEntity t : SkillVisualHelper.getNearbyTargets(player, c, 7.0, ctx)) {
                    SkillVisualHelper.dealSkillDamage(player, t, 10.0, true);
                    t.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 70, 0));
                    t.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 120, 0));
                }
            }
            case 3 -> { // Thánh Quang Hủy Diệt (3 luồng tia vàng cam song song cao 4 block, rộng 2 block)
                Location origin = player.getLocation();
                Vector dir = origin.getDirection().setY(0).normalize();
                Vector right = new Vector(-dir.getZ(), 0, dir.getX()).normalize();
                SkillVisualHelper.playSound(origin, "entity.wither.shoot", 1.6f, 1.3f);

                double[] offsets = {-3.5, 0.0, 3.5};
                new BukkitRunnable() {
                    int dist = 0;
                    @Override
                    public void run() {
                        if (++dist > 20) { cancel(); return; }
                        for (double off : offsets) {
                            Location beamCenter = origin.clone().add(dir.clone().multiply(dist)).add(right.clone().multiply(off));
                            for (double w = -1.0; w <= 1.0; w += 0.5) {
                                for (double h = 0.2; h <= 4.0; h += 0.8) {
                                    Location pt = beamCenter.clone().add(right.clone().multiply(w)).add(0, h, 0);
                                    pt.getWorld().spawnParticle(Particle.FLAME, pt, 1, 0.05, 0.05, 0.05, 0);
                                    pt.getWorld().spawnParticle(Particle.END_ROD, pt, 1, 0.05, 0.05, 0.05, 0);
                                }
                            }
                            for (LivingEntity t : SkillVisualHelper.getNearbyTargets(player, beamCenter.clone().add(0, 1.5, 0), 2.2, ctx)) {
                                SkillVisualHelper.dealSkillDamage(player, t, 16.0, true);
                                t.setFireTicks(100);
                            }
                        }
                    }
                }.runTaskTimer(ctx.getPlugin(), 0L, 1L);
            }
            case 4 -> { // Thẩm Phán Thiên Đàng (Tuyệt Kỹ)
                Location targetLoc = SkillVisualHelper.raycastLocation(player, 24.0);
                SkillVisualHelper.playSound(targetLoc, "block.beacon.power_select", 2.0f, 0.7f);
                new BukkitRunnable() {
                    int ticks = 0;
                    @Override
                    public void run() {
                        ticks += 5;
                        SkillVisualHelper.spawnRing(targetLoc, Particle.END_ROD, 9.0, 50);
                        for (double y = 0; y <= 18; y += 1.5) {
                            targetLoc.getWorld().spawnParticle(Particle.END_ROD, targetLoc.clone().add(0, y, 0), 20, 3.5, 0.5, 3.5, 0.02);
                            targetLoc.getWorld().spawnParticle(Particle.FLAME, targetLoc.clone().add(0, y, 0), 15, 3.0, 0.5, 3.0, 0.02);
                        }
                        for (LivingEntity t : SkillVisualHelper.getNearbyTargets(player, targetLoc, 9.0, ctx)) {
                            SkillVisualHelper.dealSkillDamage(player, t, 7.5, true);
                            t.setFireTicks(100);
                        }
                        if (ticks >= 80) cancel();
                    }
                }.runTaskTimer(ctx.getPlugin(), 0L, 5L);
            }
        }
    }
}
