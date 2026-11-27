package com.elementalskills.elements.impl;

import com.elementalskills.elements.ElementalMastery;
import com.elementalskills.managers.SkillExecutionManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class SetElement implements ElementalMastery {

    @Override
    public String getId() {
        return "set";
    }

    @Override
    public String getDisplayName() {
        return "Sét";
    }

    @Override
    public NamedTextColor getThemeColor() {
        return NamedTextColor.YELLOW;
    }

    @Override
    public Material getIcon() {
        return Material.LIGHTNING_ROD;
    }

    @Override
    public List<String> getOverviewLore() {
        return List.of(
                "Ưu điểm: Trời mưa/bão tăng 25% sát thương & giảm 25% hồi chiêu.",
                "Nhược điểm: Tung chiêu khi ngâm dưới nước tự bị điện giật!"
        );
    }

    @Override
    public String getSkillName(int slot) {
        return switch (slot) {
            case 1 -> "Cửu Thiên Lôi Ngục";
            case 2 -> "Lôi Cầu Liên Hoàn";
            default -> "Ngạo Khung Thiên";
        };
    }

    @Override
    public int getSkillCooldown(int slot) {
        return switch (slot) {
            case 1 -> 9;
            case 2 -> 6;
            default -> 16;
        };
    }

    @Override
    public List<String> getSkillLore(int slot) {
        return switch (slot) {
            case 1 -> List.of("Giáng liên tiếp 5 tia sét xuống vùng 5 block tại điểm ngắm.");
            case 2 -> List.of("Phóng cầu sét giật lan sang tối đa 3 mục tiêu lân cận.");
            default -> List.of("Bay lên trời gọi 3 đợt sét bán kính 6 block, sau đó", "đáp đất phóng đường sét 3 block gồm chùm 8 tia sét!");
        };
    }

    private void checkWaterBacklash(Player player) {
        if (player.isInWater()) {
            player.damage(3.0);
            player.sendActionBar(Component.text("⚡ Điện giật ngược dưới nước!", NamedTextColor.RED));
        }
    }

    @Override
    public void tickEnvironment(Player player, SkillExecutionManager ctx) {
        if (player.getWorld().hasStorm()) {
            ctx.setCooldownMultiplier(player, 0.75);
        }
    }

    @Override
    public double modifyOutgoingSkillDamage(Player player, LivingEntity target, double baseDamage, SkillExecutionManager ctx) {
        return player.getWorld().hasStorm() ? baseDamage * 1.25 : baseDamage;
    }

    // Chiêu 1: Cửu Thiên Lôi Ngục
    @Override
    public void castSkill1(Player player, SkillExecutionManager ctx) {
        checkWaterBacklash(player);
        Block targetBlock = player.getTargetBlockExact(20);
        Location center = targetBlock != null
                ? targetBlock.getLocation().add(0.5, 1.0, 0.5)
                : player.getLocation().add(player.getLocation().getDirection().multiply(8));

        player.sendActionBar(Component.text("⚡ Cửu Thiên Lôi Ngục!", NamedTextColor.YELLOW, TextDecoration.BOLD));

        for (int i = 0; i < 5; i++) {
            Bukkit.getScheduler().runTaskLater(ctx.getPlugin(), () -> {
                World w = center.getWorld();
                double ox = ThreadLocalRandom.current().nextDouble(-4.0, 4.0);
                double oz = ThreadLocalRandom.current().nextDouble(-4.0, 4.0);
                Location strike = center.clone().add(ox, 0, oz);
                strike.setY(w.getHighestBlockYAt(strike) + 1);
                w.strikeLightningEffect(strike);

                for (Entity e : w.getNearbyEntities(strike, 3.0, 3.5, 3.0)) {
                    if (e instanceof LivingEntity le && le != player) {
                        double dmg = ctx.calculateFinalSkillDamage(player, le, 4.5);
                        le.setHealth(Math.max(0.0, le.getHealth() - dmg));
                        le.damage(0.01, player);
                        le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 30, 4));
                    }
                }
            }, i * 8L);
        }
    }

    // Chiêu 2: Lôi Cầu Liên Hoàn
    @Override
    public void castSkill2(Player player, SkillExecutionManager ctx) {
        checkWaterBacklash(player);
        Location start = player.getEyeLocation();
        Vector dir = start.getDirection().normalize();
        SkillExecutionManager.playSoundSafe(player, "entity.illusioner.cast_spell", 1.3f);
        player.sendActionBar(Component.text("⚡ Lôi Cầu Liên Hoàn!", NamedTextColor.AQUA, TextDecoration.BOLD));

        new BukkitRunnable() {
            int step = 0;
            final Location cur = start.clone();

            @Override
            public void run() {
                if (step > 18) {
                    cancel();
                    return;
                }
                cur.add(dir.clone().multiply(1.15));
                World w = cur.getWorld();
                w.spawnParticle(Particle.ELECTRIC_SPARK, cur, 15, 0.25, 0.25, 0.25, 0.05);

                LivingEntity primary = null;
                for (Entity e : w.getNearbyEntities(cur, 1.5, 1.5, 1.5)) {
                    if (e instanceof LivingEntity le && le != player) {
                        primary = le;
                        break;
                    }
                }

                if (primary != null) {
                    List<LivingEntity> chain = new ArrayList<>();
                    chain.add(primary);
                    for (Entity e : w.getNearbyEntities(primary.getLocation(), 5.5, 4.0, 5.5)) {
                        if (e instanceof LivingEntity le && le != player && le != primary && chain.size() < 4) {
                            chain.add(le);
                        }
                    }

                    for (LivingEntity target : chain) {
                        w.strikeLightningEffect(target.getLocation());
                        double dmg = ctx.calculateFinalSkillDamage(player, target, 6.5);
                        target.setHealth(Math.max(0.0, target.getHealth() - dmg));
                        target.damage(0.01, player);
                    }
                    cancel();
                    return;
                }

                if (!cur.getBlock().isPassable()) {
                    cancel();
                }
                step++;
            }
        }.runTaskTimer(ctx.getPlugin(), 0L, 1L);
    }

    // Chiêu 3: Ngạo Khung Thiên
    @Override
    public void castSkill3(Player player, SkillExecutionManager ctx) {
        checkWaterBacklash(player);
        ctx.grantFallImmunity(player, 10);
        SkillExecutionManager.playSoundSafe(player, "entity.ender_dragon.growl", 1.2f);
        player.sendActionBar(Component.text("⚡ NGẠO KHUNG THIÊN!", NamedTextColor.AQUA, TextDecoration.BOLD));

        new BukkitRunnable() {
            int tick = 0;
            boolean slamming = false;
            int slamTicks = 0;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancel();
                    return;
                }

                // Giai đoạn 1: Bay từ từ lên trời (30 ticks = 1.5s) và giáng 3 đợt sét bán kính 6 block
                if (!slamming) {
                    player.setVelocity(new Vector(0, 0.32, 0));
                    Location pLoc = player.getLocation();
                    World w = pLoc.getWorld();

                    int groundY = w.getHighestBlockYAt(pLoc);
                    Location emblemCenter = new Location(w, pLoc.getX(), groundY + 0.25, pLoc.getZ());
                    for (int angle = 0; angle < 360; angle += 15) {
                        double rad = Math.toRadians(angle + tick * 6);
                        w.spawnParticle(Particle.SOUL_FIRE_FLAME, emblemCenter.clone().add(Math.cos(rad) * 6.0, 0, Math.sin(rad) * 6.0), 1, 0, 0, 0, 0);
                        w.spawnParticle(Particle.ELECTRIC_SPARK, emblemCenter.clone().add(Math.cos(rad) * 3.0, 0.2, Math.sin(rad) * 3.0), 1, 0, 0, 0, 0);
                    }

                    if (tick == 10 || tick == 20 || tick == 30) {
                        SkillExecutionManager.playSoundSafe(player, "entity.lightning_bolt.thunder", 1.2f);
                        boolean hitAny = false;
                        for (Entity e : w.getNearbyEntities(emblemCenter, 6.0, 6.0, 6.0)) {
                            if (e instanceof LivingEntity le && le != player) {
                                hitAny = true;
                                w.strikeLightningEffect(le.getLocation());
                                double dmg = ctx.calculateFinalSkillDamage(player, le, 5.0);
                                le.setHealth(Math.max(0.0, le.getHealth() - dmg));
                                le.damage(0.01, player);
                            }
                        }
                        if (!hitAny) {
                            w.strikeLightningEffect(emblemCenter);
                        }
                    }

                    if (tick >= 30) {
                        slamming = true;
                        player.setVelocity(new Vector(0, -2.2, 0));
                    }
                    tick++;
                    return;
                }

                // Giai đoạn 2: Đáp mạnh xuống đất và tạo đường rộng 3 block gồm chùm 8 tia sét tới kẻ địch
                slamTicks++;
                if (player.isOnGround() || slamTicks > 25) {
                    unleashEightLightningBeams(player, ctx);
                    cancel();
                }
            }
        }.runTaskTimer(ctx.getPlugin(), 0L, 1L);
    }

    private void unleashEightLightningBeams(Player player, SkillExecutionManager ctx) {
        Location landLoc = player.getLocation();
        World w = landLoc.getWorld();

        SkillExecutionManager.playSoundSafe(player, "entity.generic.explode", 0.9f);
        SkillExecutionManager.playSoundSafe(player, "entity.lightning_bolt.thunder", 1.0f);
        w.spawnParticle(Particle.EXPLOSION_EMITTER, landLoc, 1, 0, 0, 0, 0);

        Vector forward = landLoc.getDirection().setY(0);
        if (forward.lengthSquared() < 0.01) forward = new Vector(0, 0, 1);
        forward.normalize();

        LivingEntity nearest = null;
        double bestDistSq = 18.0 * 18.0;
        for (Entity e : w.getNearbyEntities(landLoc, 18.0, 6.0, 18.0)) {
            if (e instanceof LivingEntity le && le != player) {
                double dSq = le.getLocation().distanceSquared(landLoc);
                if (dSq < bestDistSq) {
                    bestDistSq = dSq;
                    nearest = le;
                }
            }
        }
        if (nearest != null) {
            Vector toEnemy = nearest.getLocation().toVector().subtract(landLoc.toVector()).setY(0);
            if (toEnemy.lengthSquared() > 0.01) {
                forward = toEnemy.normalize();
            }
        }

        Vector right = new Vector(-forward.getZ(), 0, forward.getX()).normalize();
        // Chùm 8 tia sét trải đều trên đường rộng 3 block (-1.4 đến +1.4)
        double[] beamOffsets = {-1.4, -1.0, -0.6, -0.2, 0.2, 0.6, 1.0, 1.4};
        Set<UUID> hitTargets = new HashSet<>();

        for (int step = 1; step <= 16; step++) {
            Location centerStep = landLoc.clone().add(forward.clone().multiply(step));

            for (double offset : beamOffsets) {
                Location beamLoc = centerStep.clone().add(right.clone().multiply(offset)).add(0, 0.6, 0);
                w.spawnParticle(Particle.ELECTRIC_SPARK, beamLoc, 4, 0.05, 0.25, 0.05, 0.03);
                w.spawnParticle(Particle.SOUL_FIRE_FLAME, beamLoc, 1, 0, 0.1, 0, 0.01);
            }

            if (step % 4 == 0) {
                w.strikeLightningEffect(centerStep);
            }

            for (Entity e : w.getNearbyEntities(centerStep, 1.6, 2.5, 1.6)) {
                if (e instanceof LivingEntity le && le != player && hitTargets.add(le.getUniqueId())) {
                    double dmg = ctx.calculateFinalSkillDamage(player, le, 14.0);
                    le.setHealth(Math.max(0.0, le.getHealth() - dmg));
                    le.damage(0.01, player);
                    le.setVelocity(forward.clone().multiply(1.3).setY(0.5));
                }
            }
        }
    }
}
