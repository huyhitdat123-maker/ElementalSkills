package com.elementalskills.elements.impl;

import com.elementalskills.elements.ElementalMastery;
import com.elementalskills.managers.SkillExecutionManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.List;

public class AmElement implements ElementalMastery {

    @Override
    public String getId() {
        return "am";
    }

    @Override
    public String getDisplayName() {
        return "Ám";
    }

    @Override
    public NamedTextColor getThemeColor() {
        return NamedTextColor.DARK_PURPLE;
    }

    @Override
    public Material getIcon() {
        return Material.ECHO_SHARD;
    }

    @Override
    public List<String> getOverviewLore() {
        return List.of(
                "Ưu điểm: Ban đêm hoặc nơi tối +25% sát thương & giảm 20% hồi chiêu.",
                "Nhược điểm: Dưới ánh nắng mặt trời bị nhận thêm 20% sát thương."
        );
    }

    @Override
    public String getSkillName(int slot) {
        return switch (slot) {
            case 1 -> "Ẩn Thân Bóng Đêm";
            case 2 -> "Trảo Hắc Ám";
            default -> "Hố Đen Thôn Phệ";
        };
    }

    @Override
    public int getSkillCooldown(int slot) {
        return switch (slot) {
            case 1 -> 8;
            case 2 -> 7;
            default -> 15;
        };
    }

    @Override
    public List<String> getSkillLore(int slot) {
        return switch (slot) {
            case 1 -> List.of("Tàng hình & tăng tốc 4s; đòn đánh tiếp theo bạo kích +50%.");
            case 2 -> List.of("Kéo giật mục tiêu tầm xa về trước mặt và gây Héo Úa.");
            default -> List.of("Tạo Hố Đen 6 block hút địch, rút máu để hồi phục cho bản thân.");
        };
    }

    private boolean isDarkness(Player player) {
        long time = player.getWorld().getTime();
        return (time >= 13000 && time <= 23000) || player.getLocation().getBlock().getLightLevel() <= 6;
    }

    private boolean isDirectSunlight(Player player) {
        long time = player.getWorld().getTime();
        return (time < 12300 || time > 23850) && player.getLocation().getBlock().getLightFromSky() >= 13;
    }

    @Override
    public void tickEnvironment(Player player, SkillExecutionManager ctx) {
        if (isDarkness(player)) {
            ctx.setCooldownMultiplier(player, 0.80);
        }
    }

    @Override
    public double modifyOutgoingSkillDamage(Player player, LivingEntity target, double baseDamage, SkillExecutionManager ctx) {
        double dmg = isDarkness(player) ? baseDamage * 1.25 : baseDamage;
        if (ctx.consumeShadowEmpowerment(player)) {
            dmg *= 1.50;
        }
        return dmg;
    }

    @Override
    public void modifyIncomingDamage(Player player, EntityDamageEvent event, SkillExecutionManager ctx) {
        if (isDirectSunlight(player)) {
            event.setDamage(event.getDamage() * 1.20);
        }
    }

    @Override
    public void castSkill1(Player player, SkillExecutionManager ctx) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 80, 0, true, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 80, 2, true, false));
        ctx.setShadowEmpowered(player, true);

        player.getWorld().spawnParticle(Particle.LARGE_SMOKE, player.getLocation().add(0, 1, 0), 25, 0.5, 0.6, 0.5, 0.04);
        SkillExecutionManager.playSoundSafe(player, "entity.enderman.teleport", 0.9f);
        player.sendActionBar(Component.text("🌑 Ẩn Thân Bóng Đêm!", NamedTextColor.DARK_PURPLE, TextDecoration.BOLD));
    }

    @Override
    public void castSkill2(Player player, SkillExecutionManager ctx) {
        Location start = player.getEyeLocation();
        Vector dir = start.getDirection().normalize();
        World w = start.getWorld();

        SkillExecutionManager.playSoundSafe(player, "entity.warden.tendril_clicks", 1.2f);
        player.sendActionBar(Component.text("🕷 Trảo Hắc Ám!", NamedTextColor.DARK_PURPLE, TextDecoration.BOLD));

        for (double d = 1.5; d <= 16.0; d += 0.75) {
            Location p = start.clone().add(dir.clone().multiply(d));
            w.spawnParticle(Particle.PORTAL, p, 6, 0.15, 0.15, 0.15, 0.05);
            if (!p.getBlock().isPassable()) break;

            for (Entity e : w.getNearbyEntities(p, 1.3, 1.3, 1.3)) {
                if (e instanceof LivingEntity le && le != player) {
                    Location pullDest = player.getLocation().add(player.getLocation().getDirection().setY(0).normalize().multiply(1.8));
                    le.teleport(pullDest);
                    le.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 80, 1));
                    le.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40, 0));
                    le.damage(ctx.calculateFinalSkillDamage(player, le, 6.5), player);
                    return;
                }
            }
        }
    }

    @Override
    public void castSkill3(Player player, SkillExecutionManager ctx) {
        Location center = player.getLocation().add(player.getLocation().getDirection().setY(0).normalize().multiply(5.0));
        World w = center.getWorld();

        SkillExecutionManager.playSoundSafe(player, "entity.warden.roar", 1.1f);
        player.sendActionBar(Component.text("🕳 Hố Đen Thôn Phệ!", NamedTextColor.DARK_PURPLE, TextDecoration.BOLD));

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (!player.isOnline() || ticks > 80) {
                    cancel();
                    return;
                }
                w.spawnParticle(Particle.PORTAL, center.clone().add(0, 1, 0), 30, 2.5, 0.8, 2.5, 0.15);
                w.spawnParticle(Particle.LARGE_SMOKE, center.clone().add(0, 0.8, 0), 10, 1.5, 0.5, 1.5, 0.03);

                for (Entity e : w.getNearbyEntities(center, 6.0, 4.0, 6.0)) {
                    if (e instanceof LivingEntity le && le != player) {
                        Vector pull = center.toVector().subtract(le.getLocation().toVector());
                        if (pull.lengthSquared() > 0.3) {
                            le.setVelocity(pull.normalize().multiply(0.45));
                        }
                        if (ticks % 20 == 0) {
                            double dmg = ctx.calculateFinalSkillDamage(player, le, 4.0);
                            le.damage(dmg, player);
                            AttributeInstance attr = player.getAttribute(Attribute.MAX_HEALTH);
                            double max = attr != null ? attr.getValue() : 20.0;
                            player.setHealth(Math.min(max, player.getHealth() + dmg * 0.5));
                        }
                    }
                }
                ticks += 5;
            }
        }.runTaskTimer(ctx.getPlugin(), 0L, 5L);
    }
}
