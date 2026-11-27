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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class LightningElement implements ElementMastery {
    @Override public String getId() { return "set"; }
    @Override public String getDisplayName() { return "Hệ Sét"; }
    @Override public NamedTextColor getThemeColor() { return NamedTextColor.YELLOW; }
    @Override public Material getGuiIcon() { return Material.LIGHTNING_ROD; }
    @Override public int[] getCooldownSeconds() { return new int[]{8, 12, 15, 24, 36}; }

    @Override
    public List<String> getSkillNames() {
        return List.of("Cửu Thiên Lôi Ngục", "Lôi Cầu Liên Hoàn", "Lôi Thần Tức Khí", "Ngạo Khung Thiên", "Vạn Lôi Quy Tông");
    }

    @Override
    public List<String> getSkillDescriptions() {
        return List.of(
                "Giáng 5 tia sét xuyên giáp xuống vùng ngắm bán kính 5 block.",
                "Phóng cầu điện giật lan liên hoàn tối đa 4 kẻ địch.",
                "Hóa tia chớp lao vút 15 block & giáng sét làm choáng.",
                "Bay lên gọi 3 đợt sét rồi đáp xuống quét luồng 8 tia sét.",
                "Phủ mây giông 14 block giáng 25 tia sét + cột sét khổng lồ."
        );
    }

    @Override
    public void castSkill(Player player, int skillIndex, CombatModeManager ctx) {
        switch (skillIndex) {
            case 0 -> { // Cửu Thiên Lôi Ngục
                Location targetLoc = SkillVisualHelper.raycastLocation(player, 22.0);
                for (int i = 0; i < 5; i++) {
                    Bukkit.getScheduler().runTaskLater(ctx.getPlugin(), () -> {
                        Location strike = targetLoc.clone().add(
                                ThreadLocalRandom.current().nextDouble(-3.5, 3.5), 0,
                                ThreadLocalRandom.current().nextDouble(-3.5, 3.5));
                        strike.getWorld().strikeLightningEffect(strike);
                        for (LivingEntity t : SkillVisualHelper.getNearbyTargets(player, strike, 4.5, ctx)) {
                            SkillVisualHelper.dealSkillDamage(player, t, 8.5, true);
                            t.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 2));
                        }
                    }, i * 3L);
                }
            }
            case 1 -> { // Lôi Cầu Liên Hoàn
                LivingEntity first = SkillVisualHelper.raycastTarget(player, 20.0, 1.8, ctx);
                if (first == null) {
                    List<LivingEntity> near = SkillVisualHelper.getNearbyTargets(player, player.getLocation(), 12.0, ctx);
                    if (!near.isEmpty()) first = near.get(0);
                }
                if (first == null) return;

                Set<UUID> hit = new HashSet<>();
                LivingEntity cur = first;
                Location prevLoc = player.getEyeLocation();
                for (int bounce = 0; bounce < 4 && cur != null; bounce++) {
                    hit.add(cur.getUniqueId());
                    Location targetEye = cur.getEyeLocation();
                    SkillVisualHelper.drawBeam(prevLoc, targetEye, Particle.ELECTRIC_SPARK, 0.3, 4);
                    cur.getWorld().strikeLightningEffect(cur.getLocation());
                    SkillVisualHelper.dealSkillDamage(player, cur, 12.0 - bounce * 1.5, true);

                    prevLoc = targetEye;
                    LivingEntity next = null;
                    for (LivingEntity cand : SkillVisualHelper.getNearbyTargets(player, cur.getLocation(), 8.0, ctx)) {
                        if (!hit.contains(cand.getUniqueId())) {
                            next = cand;
                            break;
                        }
                    }
                    cur = next;
                }
            }
            case 2 -> { // Lôi Thần Tức Khí
                ctx.grantFallImmunity(player, 4);
                Location start = player.getLocation();
                Location dest = SkillVisualHelper.raycastLocation(player, 15.0);
                SkillVisualHelper.drawBeam(start.clone().add(0, 1, 0), dest.clone().add(0, 1, 0), Particle.ELECTRIC_SPARK, 0.25, 5);
                dest.setYaw(start.getYaw());
                dest.setPitch(start.getPitch());
                player.teleport(dest);
                dest.getWorld().strikeLightningEffect(dest);
                for (LivingEntity t : SkillVisualHelper.getNearbyTargets(player, dest, 5.0, ctx)) {
                    SkillVisualHelper.dealSkillDamage(player, t, 14.0, true);
                    t.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 50, 3));
                    t.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 30, 0));
                }
            }
            case 3 -> { // Ngạo Khung Thiên (bay lên gọi 3 đợt sét -> đáp xuống quét 8 tia sét đường rộng 3 block)
                ctx.grantFallImmunity(player, 8);
                player.setVelocity(new Vector(0, 0.85, 0));
                SkillVisualHelper.playSound(player.getLocation(), "entity.lightning_bolt.thunder", 1.5f, 1.2f);

                for (int wave = 1; wave <= 3; wave++) {
                    Bukkit.getScheduler().runTaskLater(ctx.getPlugin(), () -> {
                        if (!player.isOnline()) return;
                        player.setVelocity(new Vector(0, 0.25, 0));
                        Location pLoc = player.getLocation();
                        pLoc.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, pLoc, 50, 2.5, 1.0, 2.5, 0.1);
                        for (LivingEntity t : SkillVisualHelper.getNearbyTargets(player, pLoc, 6.0, ctx)) {
                            t.getWorld().strikeLightningEffect(t.getLocation());
                            SkillVisualHelper.dealSkillDamage(player, t, 7.5, true);
                        }
                    }, wave * 10L);
                }

                // Sau 38 ticks đáp mạnh xuống đất và càn quét đường rộng 3 block với 8 tia sét
                Bukkit.getScheduler().runTaskLater(ctx.getPlugin(), () -> {
                    if (!player.isOnline()) return;
                    player.setVelocity(new Vector(0, -1.6, 0));
                    Bukkit.getScheduler().runTaskLater(ctx.getPlugin(), () -> {
                        Location ground = player.getLocation();
                        Vector dir = ground.getDirection().setY(0).normalize();
                        Vector right = new Vector(-dir.getZ(), 0, dir.getX()).normalize();
                        ground.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, ground, 2, 0.5, 0.2, 0.5, 0);

                        for (int s = 1; s <= 8; s++) {
                            final int step = s;
                            Bukkit.getScheduler().runTaskLater(ctx.getPlugin(), () -> {
                                double lat = (step % 3) - 1.0; // -1, 0, +1 -> tạo bề rộng 3 block
                                Location strike = ground.clone().add(dir.clone().multiply(step * 2.2)).add(right.clone().multiply(lat));
                                strike.getWorld().strikeLightningEffect(strike);
                                strike.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, strike, 30, 1.2, 0.5, 1.2, 0.1);
                                for (LivingEntity t : SkillVisualHelper.getNearbyTargets(player, strike, 2.6, ctx)) {
                                    SkillVisualHelper.dealSkillDamage(player, t, 12.0, true);
                                }
                            }, s * 2L);
                        }
                    }, 8L);
                }, 36L);
            }
            case 4 -> { // Vạn Lôi Quy Tông (Tuyệt Kỹ)
                Location targetLoc = SkillVisualHelper.raycastLocation(player, 24.0);
                SkillVisualHelper.playSound(targetLoc, "entity.lightning_bolt.thunder", 2.0f, 0.7f);
                new BukkitRunnable() {
                    int count = 0;
                    @Override
                    public void run() {
                        count++;
                        SkillVisualHelper.spawnRing(targetLoc, Particle.ELECTRIC_SPARK, 14.0, 50);
                        for (int b = 0; b < 2; b++) {
                            double ox = ThreadLocalRandom.current().nextDouble(-12.0, 12.0);
                            double oz = ThreadLocalRandom.current().nextDouble(-12.0, 12.0);
                            Location strike = targetLoc.clone().add(ox, 0, oz);
                            strike.getWorld().strikeLightningEffect(strike);
                            for (LivingEntity t : SkillVisualHelper.getNearbyTargets(player, strike, 3.5, ctx)) {
                                SkillVisualHelper.dealSkillDamage(player, t, 9.0, true);
                            }
                        }
                        if (count >= 12) {
                            cancel();
                            for (int c = 0; c < 4; c++) targetLoc.getWorld().strikeLightningEffect(targetLoc);
                            targetLoc.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, targetLoc, 3, 1.5, 0.5, 1.5, 0);
                            for (LivingEntity t : SkillVisualHelper.getNearbyTargets(player, targetLoc, 8.0, ctx)) {
                                SkillVisualHelper.dealSkillDamage(player, t, 20.0, true);
                            }
                        }
                    }
                }.runTaskTimer(ctx.getPlugin(), 0L, 4L);
            }
        }
    }
}
