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
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

public class QuangElement implements ElementalMastery {

    private static final Set<EntityType> UNDEAD_TYPES = Set.of(
            EntityType.ZOMBIE, EntityType.ZOMBIE_VILLAGER, EntityType.HUSK, EntityType.DROWNED,
            EntityType.SKELETON, EntityType.STRAY, EntityType.WITHER_SKELETON, EntityType.BOGGED,
            EntityType.PHANTOM, EntityType.WITHER, EntityType.ZOMBIFIED_PIGLIN
    );

    @Override
    public String getId() {
        return "quang";
    }

    @Override
    public String getDisplayName() {
        return "Quang";
    }

    @Override
    public NamedTextColor getThemeColor() {
        return NamedTextColor.YELLOW;
    }

    @Override
    public Material getIcon() {
        return Material.END_ROD;
    }

    @Override
    public List<String> getOverviewLore() {
        return List.of(
                "Ưu điểm: Ban ngày ngoài trời sáng +25% sát thương & hồi máu.",
                "Nhược điểm: Trong bóng tối hoàn toàn giảm 25% sát thương."
        );
    }

    @Override
    public String getSkillName(int slot) {
        return switch (slot) {
            case 1 -> "Quang Minh Hộ Thuẫn";
            case 2 -> "Thánh Quang Phán Quyết";
            default -> "Thánh Quang Hủy Diệt";
        };
    }

    @Override
    public int getSkillCooldown(int slot) {
        return switch (slot) {
            case 1 -> 9;
            case 2 -> 6;
            default -> 14;
        };
    }

    @Override
    public List<String> getSkillLore(int slot) {
        return switch (slot) {
            case 1 -> List.of("Thanh tẩy toàn bộ bất lợi và hồi phục lượng máu lớn.");
            case 2 -> List.of("Bắn tia sáng tức thời xuyên giáp, khắc chế mạnh Hệ Ám & Bất Tử.");
            default -> List.of("Phóng 3 luồng thánh quang song song (cao 4 block, rộng 2 block)", "càn quét toàn bộ kẻ địch phía trước!");
        };
    }

    private boolean isBrightDaylight(Player player) {
        long time = player.getWorld().getTime();
        return (time < 12300 || time > 23850) && player.getLocation().getBlock().getLightFromSky() >= 12;
    }

    @Override
    public void tickEnvironment(Player player, SkillExecutionManager ctx) {
        if (isBrightDaylight(player)) {
            AttributeInstance attr = player.getAttribute(Attribute.MAX_HEALTH);
            double max = attr != null ? attr.getValue() : 20.0;
            player.setHealth(Math.min(max, player.getHealth() + 0.5));
        }
    }

    @Override
    public double modifyOutgoingSkillDamage(Player player, LivingEntity target, double baseDamage, SkillExecutionManager ctx) {
        double dmg = baseDamage;
        if (isBrightDaylight(player)) {
            dmg *= 1.25;
        } else if (player.getLocation().getBlock().getLightLevel() <= 4) {
            dmg *= 0.75;
        }
        if (target != null && UNDEAD_TYPES.contains(target.getType())) {
            dmg *= 1.40;
        }
        return dmg;
    }

    @Override
    public void castSkill1(Player player, SkillExecutionManager ctx) {
        for (PotionEffect ef : new ArrayList<>(player.getActivePotionEffects())) {
            player.removePotionEffect(ef.getType());
        }
        AttributeInstance attr = player.getAttribute(Attribute.MAX_HEALTH);
        double max = attr != null ? attr.getValue() : 20.0;
        player.setHealth(Math.min(max, player.getHealth() + 8.0));
        player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 120, 0));

        player.getWorld().spawnParticle(Particle.END_ROD, player.getLocation().add(0, 1, 0), 35, 0.7, 0.8, 0.7, 0.06);
        SkillExecutionManager.playSoundSafe(player, "item.totem.use", 1.3f);
        player.sendActionBar(Component.text("✨ Quang Minh Hộ Thuẫn!", NamedTextColor.YELLOW, TextDecoration.BOLD));
    }

    @Override
    public void castSkill2(Player player, SkillExecutionManager ctx) {
        Location start = player.getEyeLocation();
        Vector dir = start.getDirection().normalize();
        World w = start.getWorld();

        SkillExecutionManager.playSoundSafe(player, "entity.guardian.attack", 1.5f);
        player.sendActionBar(Component.text("☀ Thánh Quang Phán Quyết!", NamedTextColor.YELLOW, TextDecoration.BOLD));

        Set<UUID> hit = new HashSet<>();
        for (double d = 1.0; d <= 18.0; d += 0.5) {
            Location p = start.clone().add(dir.clone().multiply(d));
            w.spawnParticle(Particle.END_ROD, p, 2, 0.05, 0.05, 0.05, 0);
            if (!p.getBlock().isPassable()) break;

            for (Entity e : w.getNearbyEntities(p, 1.0, 1.0, 1.0)) {
                if (e instanceof LivingEntity le && le != player && hit.add(le.getUniqueId())) {
                    double dmg = ctx.calculateFinalSkillDamage(player, le, 8.5);
                    le.setHealth(Math.max(0.0, le.getHealth() - dmg));
                    le.damage(0.01, player);
                }
            }
        }
    }

    // Chiêu 3: Thánh Quang Hủy Diệt (3 tia ánh sáng song song, mỗi tia cao 4 block, rộng 2 block)
    @Override
    public void castSkill3(Player player, SkillExecutionManager ctx) {
        Location origin = player.getLocation();
        Vector forward = origin.getDirection().setY(0);
        if (forward.lengthSquared() < 0.01) forward = new Vector(0, 0, 1);
        forward.normalize();
        Vector right = new Vector(-forward.getZ(), 0, forward.getX()).normalize();

        SkillExecutionManager.playSoundSafe(player, "entity.warden.sonic_boom", 1.3f);
        SkillExecutionManager.playSoundSafe(player, "block.beacon.power_select", 1.4f);
        player.sendActionBar(Component.text("☀ THÁNH QUANG HỦY DIỆT!", NamedTextColor.GOLD, TextDecoration.BOLD));

        double[] laneOffsets = {-2.4, 0.0, 2.4};
        Particle.DustOptions goldenBeamDust = new Particle.DustOptions(Color.fromRGB(255, 190, 45), 1.8f);

        Vector finalForward = forward;
        new BukkitRunnable() {
            int step = 1;
            final Set<UUID> hitTargets = new HashSet<>();

            @Override
            public void run() {
                if (!player.isOnline() || step > 15) {
                    cancel();
                    return;
                }

                Location stepCenter = origin.clone().add(finalForward.clone().multiply(step));
                World w = stepCenter.getWorld();

                for (double lane : laneOffsets) {
                    Location beamBase = stepCenter.clone().add(right.clone().multiply(lane));

                    // Dựng luồng thánh quang cao 4 block, rộng 2 block
                    for (double y = 0.2; y <= 4.0; y += 0.6) {
                        for (double width = -0.8; width <= 0.8; width += 0.8) {
                            Location pLoc = beamBase.clone().add(right.clone().multiply(width)).add(0, y, 0);
                            w.spawnParticle(Particle.DUST, pLoc, 1, 0.08, 0.08, 0.08, 0, goldenBeamDust);
                        }
                        if (y == 2.0) {
                            w.spawnParticle(Particle.END_ROD, beamBase.clone().add(0, y, 0), 2, 0.4, 0.6, 0.4, 0.02);
                        }
                    }

                    // Vùng va chạm: rộng 2 block (half-extent = 1.0) và cao 4 block
                    Location hitboxCenter = beamBase.clone().add(0, 2.0, 0);
                    for (Entity e : w.getNearbyEntities(hitboxCenter, 1.05, 2.0, 1.05)) {
                        if (e instanceof LivingEntity le && le != player && hitTargets.add(le.getUniqueId())) {
                            double dmg = ctx.calculateFinalSkillDamage(player, le, 13.0);
                            le.setHealth(Math.max(0.0, le.getHealth() - dmg));
                            le.damage(0.01, player);
                            le.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40, 0));
                            le.setVelocity(finalForward.clone().multiply(1.35).setY(0.45));
                        }
                    }
                }
                step++;
            }
        }.runTaskTimer(ctx.getPlugin(), 0L, 1L);
    }
}
