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
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class HoaElement implements ElementalMastery {

    @Override
    public String getId() {
        return "hoa";
    }

    @Override
    public String getDisplayName() {
        return "Hỏa";
    }

    @Override
    public NamedTextColor getThemeColor() {
        return NamedTextColor.RED;
    }

    @Override
    public Material getIcon() {
        return Material.FIRE_CHARGE;
    }

    @Override
    public List<String> getOverviewLore() {
        return List.of(
                "Ưu điểm: Đứng trong Lava miễn nhiễm nhiệt & +35% sát thương chiêu.",
                "Nhược điểm: Rơi xuống nước sẽ tắt kỹ năng ngay lập tức!"
        );
    }

    @Override
    public String getSkillName(int slot) {
        return switch (slot) {
            case 1 -> "Hỏa Diệm Giáng Thế";
            case 2 -> "Liên Hoàn Hỏa Cầu";
            default -> "Viêm Hoàng Lĩnh Vực";
        };
    }

    @Override
    public int getSkillCooldown(int slot) {
        return switch (slot) {
            case 1 -> 8;
            case 2 -> 5;
            default -> 14;
        };
    }

    @Override
    public List<String> getSkillLore(int slot) {
        return switch (slot) {
            case 1 -> List.of("Nhảy lên không trung rồi đạp mạnh xuống đất,", "nổ tâm 4 block và lan lửa cháy 8 block.");
            case 2 -> List.of("Bắn liên tiếp 3 quả cầu lửa gây nổ & hất văng.");
            default -> List.of("Tạo vòng xoáy cột lửa hút địch 5s rồi bùng nổ.");
        };
    }

    private boolean isInFireOrLava(Player player) {
        Material m = player.getLocation().getBlock().getType();
        return m == Material.LAVA || m == Material.FIRE || m == Material.SOUL_FIRE || player.getFireTicks() > 0;
    }

    @Override
    public void tickEnvironment(Player player, SkillExecutionManager ctx) {
        if (player.isInWater()) {
            ctx.getInventoryManager().disableCombatMode(player, true);
            player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 60, 1, true, false));
            return;
        }
        if (isInFireOrLava(player)) {
            player.getWorld().spawnParticle(Particle.FLAME, player.getLocation().add(0, 1, 0), 8, 0.4, 0.5, 0.4, 0.02);
        }
    }

    @Override
    public double modifyOutgoingSkillDamage(Player player, LivingEntity target, double baseDamage, SkillExecutionManager ctx) {
        return isInFireOrLava(player) ? baseDamage * 1.35 : baseDamage;
    }

    @Override
    public void modifyIncomingDamage(Player player, EntityDamageEvent event, SkillExecutionManager ctx) {
        EntityDamageEvent.DamageCause cause = event.getCause();
        if (cause == EntityDamageEvent.DamageCause.LAVA
                || cause == EntityDamageEvent.DamageCause.FIRE
                || cause == EntityDamageEvent.DamageCause.FIRE_TICK) {
            event.setCancelled(true);
            player.setFireTicks(0);
        }
    }

    @Override
    public void castSkill1(Player player, SkillExecutionManager ctx) {
        ctx.grantFallImmunity(player, 6);
        Vector launch = player.getLocation().getDirection().setY(0);
        if (launch.lengthSquared() > 0.01) launch.normalize().multiply(0.75);
        launch.setY(1.15);
        player.setVelocity(launch);

        SkillExecutionManager.playSoundSafe(player, "entity.blaze.shoot", 1.1f);
        player.sendActionBar(Component.text("🔥 Hỏa Diệm Giáng Thế!", NamedTextColor.RED, TextDecoration.BOLD));

        new BukkitRunnable() {
            int ticks = 0;
            boolean slammed = false;

            @Override
            public void run() {
                if (!player.isOnline() || ticks > 50) {
                    cancel();
                    return;
                }
                player.getWorld().spawnParticle(Particle.FLAME, player.getLocation(), 8, 0.3, 0.2, 0.3, 0.03);

                if (ticks == 8 && !slammed) {
                    slammed = true;
                    player.setVelocity(new Vector(0, -1.8, 0));
                }

                if (ticks > 8 && player.isOnGround()) {
                    Location center = player.getLocation();
                    World w = center.getWorld();

                    w.spawnParticle(Particle.EXPLOSION_EMITTER, center, 2, 0.5, 0.2, 0.5, 0);
                    SkillExecutionManager.playSoundSafe(player, "entity.generic.explode", 0.9f);

                    for (Entity e : w.getNearbyEntities(center, 4.0, 3.0, 4.0)) {
                        if (e instanceof LivingEntity le && le != player) {
                            double dmg = ctx.calculateFinalSkillDamage(player, le, 10.0);
                            le.damage(dmg, player);
                            le.setVelocity(new Vector(0, 0.7, 0));
                        }
                    }

                    for (Entity e : w.getNearbyEntities(center, 8.0, 3.5, 8.0)) {
                        if (e instanceof LivingEntity le && le != player) {
                            le.setFireTicks(140);
                            double burnDmg = ctx.calculateFinalSkillDamage(player, le, 4.0);
                            le.damage(burnDmg, player);
                        }
                    }

                    for (int x = -8; x <= 8; x++) {
                        for (int z = -8; z <= 8; z++) {
                            if (x * x + z * z > 64) continue;
                            int topY = w.getHighestBlockYAt(center.getBlockX() + x, center.getBlockZ() + z);
                            Block fireBlock = w.getBlockAt(center.getBlockX() + x, topY + 1, center.getBlockZ() + z);
                            if (fireBlock.getType() == Material.AIR) {
                                ctx.placeTemporaryBlock(fireBlock, Material.FIRE, 100);
                            }
                        }
                    }
                    cancel();
                    return;
                }
                ticks++;
            }
        }.runTaskTimer(ctx.getPlugin(), 1L, 1L);
    }

    @Override
    public void castSkill2(Player player, SkillExecutionManager ctx) {
        player.sendActionBar(Component.text("🔥 Liên Hoàn Hỏa Cầu!", NamedTextColor.GOLD, TextDecoration.BOLD));

        for (int shot = 0; shot < 3; shot++) {
            Bukkit.getScheduler().runTaskLater(ctx.getPlugin(), () -> {
                if (!player.isOnline()) return;
                SkillExecutionManager.playSoundSafe(player, "entity.ghast.shoot", 1.2f);
                Location start = player.getEyeLocation();
                Vector dir = start.getDirection().normalize();

                new BukkitRunnable() {
                    int step = 0;
                    final Location cur = start.clone();
                    final Set<UUID> hit = new HashSet<>();

                    @Override
                    public void run() {
                        if (step > 18) {
                            cancel();
                            return;
                        }
                        cur.add(dir.clone().multiply(1.1));
                        World w = cur.getWorld();
                        w.spawnParticle(Particle.FLAME, cur, 12, 0.25, 0.25, 0.25, 0.03);
                        w.spawnParticle(Particle.LAVA, cur, 2, 0.15, 0.15, 0.15, 0);

                        boolean collided = !cur.getBlock().isPassable();
                        for (Entity e : w.getNearbyEntities(cur, 1.5, 1.5, 1.5)) {
                            if (e instanceof LivingEntity le && le != player && hit.add(le.getUniqueId())) {
                                collided = true;
                                le.setFireTicks(100);
                                le.damage(ctx.calculateFinalSkillDamage(player, le, 6.0), player);
                                le.setVelocity(dir.clone().multiply(0.9).setY(0.35));
                            }
                        }
                        if (collided) {
                            w.spawnParticle(Particle.EXPLOSION, cur, 1, 0, 0, 0, 0);
                            cancel();
                        }
                        step++;
                    }
                }.runTaskTimer(ctx.getPlugin(), 0L, 1L);
            }, shot * 5L);
        }
    }

    @Override
    public void castSkill3(Player player, SkillExecutionManager ctx) {
        SkillExecutionManager.playSoundSafe(player, "entity.ender_dragon.growl", 1.1f);
        player.sendActionBar(Component.text("🔥 Viêm Hoàng Lĩnh Vực!", NamedTextColor.DARK_RED, TextDecoration.BOLD));

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (!player.isOnline() || ticks > 100) {
                    if (player.isOnline()) {
                        Location endCenter = player.getLocation();
                        World w = endCenter.getWorld();
                        w.spawnParticle(Particle.EXPLOSION_EMITTER, endCenter, 2, 1.0, 0.5, 1.0, 0);
                        SkillExecutionManager.playSoundSafe(player, "entity.generic.explode", 0.8f);
                        for (Entity e : w.getNearbyEntities(endCenter, 6.5, 4.0, 6.5)) {
                            if (e instanceof LivingEntity le && le != player) {
                                Vector push = le.getLocation().toVector().subtract(endCenter.toVector());
                                if (push.lengthSquared() < 0.01) push = new Vector(1, 0, 0);
                                le.setVelocity(push.setY(0).normalize().multiply(1.5).setY(0.65));
                                le.damage(ctx.calculateFinalSkillDamage(player, le, 8.0), player);
                            }
                        }
                    }
                    cancel();
                    return;
                }

                Location c = player.getLocation();
                World w = c.getWorld();
                for (int angle = 0; angle < 360; angle += 18) {
                    double rad = Math.toRadians(angle + ticks * 8);
                    double x = Math.cos(rad) * 6.0;
                    double z = Math.sin(rad) * 6.0;
                    w.spawnParticle(Particle.FLAME, c.clone().add(x, 0.5, z), 2, 0.1, 0.8, 0.1, 0.02);
                    w.spawnParticle(Particle.SOUL_FIRE_FLAME, c.clone().add(x, 1.8, z), 1, 0, 0.3, 0, 0.01);
                }

                for (Entity e : w.getNearbyEntities(c, 6.0, 4.0, 6.0)) {
                    if (e instanceof LivingEntity le && le != player) {
                        Vector pull = c.toVector().subtract(le.getLocation().toVector());
                        if (pull.lengthSquared() > 1.5) {
                            le.setVelocity(pull.normalize().multiply(0.25));
                        }
                        if (ticks % 20 == 0) {
                            le.setFireTicks(60);
                            le.damage(ctx.calculateFinalSkillDamage(player, le, 3.0), player);
                        }
                    }
                }
                ticks += 5;
            }
        }.runTaskTimer(ctx.getPlugin(), 0L, 5L);
    }
}
