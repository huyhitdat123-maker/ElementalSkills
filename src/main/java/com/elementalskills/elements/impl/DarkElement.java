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

public class DarkElement implements ElementMastery {
    @Override public String getId() { return "am"; }
    @Override public String getDisplayName() { return "Hệ Ám"; }
    @Override public NamedTextColor getThemeColor() { return NamedTextColor.DARK_PURPLE; }
    @Override public Material getGuiIcon() { return Material.ECHO_SHARD; }
    @Override public int[] getCooldownSeconds() { return new int[]{9, 12, 16, 22, 36}; }

    @Override
    public List<String> getSkillNames() {
        return List.of("Ẩn Thân Bóng Đêm", "Trảo Hắc Ám", "Vạn Ảnh Phân Thân Trảm", "Hố Đen Thôn Phệ", "Vĩnh Hằng Hắc Ám");
    }

    @Override
    public List<String> getSkillDescriptions() {
        return List.of(
                "Tàng hình hoàn toàn, tăng tốc chạy & sức mạnh trong 4 giây.",
                "Phóng bàn tay hắc ám kéo giật kẻ địch về trước mặt & gây Héo Úa.",
                "Lao vào mục tiêu chém 5 nhát bóng tối chớp nhoáng từ mọi hướng.",
                "Mở hố đen bán kính 7 block hút địch & hút máu hồi cho bản thân.",
                "Phủ bóng tối 14 block gây mù lòa & phóng 10 kiếm hắc ám xuyên phá."
        );
    }

    @Override
    public void castSkill(Player player, int skillIndex, CombatModeManager ctx) {
        switch (skillIndex) {
            case 0 -> { // Ẩn Thân Bóng Đêm
                player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 80, 0, false, false));
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 80, 2, false, false));
                player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 80, 1, false, false));
                player.getWorld().spawnParticle(Particle.LARGE_SMOKE, player.getLocation().add(0, 1, 0), 40, 0.7, 0.8, 0.7, 0.05);
                SkillVisualHelper.playSound(player.getLocation(), "entity.enderman.teleport", 1.2f, 0.7f);
            }
            case 1 -> { // Trảo Hắc Ám
                LivingEntity target = SkillVisualHelper.raycastTarget(player, 20.0, 1.8, ctx);
                if (target == null) return;
                SkillVisualHelper.drawBeam(player.getEyeLocation(), target.getEyeLocation(), Particle.WITCH, 0.3, 4);
                Vector pull = player.getLocation().toVector().subtract(target.getLocation().toVector()).normalize().multiply(1.8).setY(0.4);
                target.setVelocity(pull);
                target.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 80, 1));
                target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 60, 0));
                SkillVisualHelper.dealSkillDamage(player, target, 10.0, true);
                SkillVisualHelper.playSound(player.getLocation(), "entity.phantom.bite", 1.4f, 0.6f);
            }
            case 2 -> { // Vạn Ảnh Phân Thân Trảm
                LivingEntity target = SkillVisualHelper.raycastTarget(player, 16.0, 2.0, ctx);
                if (target == null) {
                    List<LivingEntity> near = SkillVisualHelper.getNearbyTargets(player, player.getLocation(), 12.0, ctx);
                    if (!near.isEmpty()) target = near.get(0);
                }
                if (target == null) return;
                final LivingEntity victim = target;
                for (int slash = 0; slash < 5; slash++) {
                    Bukkit.getScheduler().runTaskLater(ctx.getPlugin(), () -> {
                        if (!victim.isValid() || victim.isDead()) return;
                        double angle = ThreadLocalRandom.current().nextDouble(0, Math.PI * 2);
                        Location dashLoc = victim.getLocation().add(Math.cos(angle) * 2.0, 0.2, Math.sin(angle) * 2.0);
                        dashLoc.setDirection(victim.getLocation().toVector().subtract(dashLoc.toVector()));
                        player.teleport(dashLoc);
                        victim.getWorld().spawnParticle(Particle.SWEEP_ATTACK, victim.getLocation().add(0, 1, 0), 2, 0.4, 0.4, 0.4, 0);
                        victim.getWorld().spawnParticle(Particle.SQUID_INK, victim.getLocation().add(0, 1, 0), 15, 0.5, 0.5, 0.5, 0.1);
                        SkillVisualHelper.playSound(victim.getLocation(), "entity.player.attack.crit", 1.2f, 1.3f);
                        SkillVisualHelper.dealSkillDamage(player, victim, 5.5, true);
                    }, slash * 4L);
                }
            }
            case 3 -> { // Hố Đen Thôn Phệ
                Location center = SkillVisualHelper.raycastLocation(player, 16.0);
                SkillVisualHelper.playSound(center, "entity.warden.heartbeat", 1.8f, 0.5f);
                new BukkitRunnable() {
                    int ticks = 0;
                    @Override
                    public void run() {
                        ticks += 5;
                        SkillVisualHelper.spawnRing(center, Particle.WITCH, 7.0, 45);
                        center.getWorld().spawnParticle(Particle.SQUID_INK, center.clone().add(0, 1, 0), 30, 2.5, 1.0, 2.5, 0.05);
                        for (LivingEntity t : SkillVisualHelper.getNearbyTargets(player, center, 7.0, ctx)) {
                            Vector pull = center.toVector().subtract(t.getLocation().toVector()).normalize().multiply(0.5);
                            t.setVelocity(pull);
                            SkillVisualHelper.dealSkillDamage(player, t, 4.0, true);
                            SkillVisualHelper.healPlayer(player, 1.5);
                        }
                        if (ticks >= 80) cancel();
                    }
                }.runTaskTimer(ctx.getPlugin(), 0L, 5L);
            }
            case 4 -> { // Vĩnh Hằng Hắc Ám (Tuyệt Kỹ)
                Location center = player.getLocation();
                SkillVisualHelper.playSound(center, "entity.wither.death", 1.5f, 0.6f);
                List<LivingEntity> victims = SkillVisualHelper.getNearbyTargets(player, center, 14.0, ctx);
                for (LivingEntity t : victims) {
                    t.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 100, 0));
                    t.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 100, 0));
                    t.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 100, 2));
                }
                for (int blade = 0; blade < 10; blade++) {
                    Bukkit.getScheduler().runTaskLater(ctx.getPlugin(), () -> {
                        center.getWorld().spawnParticle(Particle.SQUID_INK, center.clone().add(0, 2, 0), 50, 6.0, 2.0, 6.0, 0.05);
                        if (!victims.isEmpty()) {
                            LivingEntity pick = victims.get(ThreadLocalRandom.current().nextInt(victims.size()));
                            if (pick.isValid() && !pick.isDead()) {
                                SkillVisualHelper.drawBeam(pick.getLocation().add(0, 5, 0), pick.getLocation(), Particle.WITCH, 0.3, 4);
                                SkillVisualHelper.dealSkillDamage(player, pick, 9.0, true);
                            }
                        }
                    }, blade * 7L);
                }
            }
        }
    }
}
