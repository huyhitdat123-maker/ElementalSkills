package com.elementalskills.managers;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

public final class SkillVisualHelper {

    private SkillVisualHelper() {}

    public static void playSound(Location loc, String key, float volume, float pitch) {
        if (loc == null || loc.getWorld() == null) return;
        Sound sound = Registry.SOUNDS.get(NamespacedKey.minecraft(key));
        if (sound != null) {
            loc.getWorld().playSound(loc, sound, volume, pitch);
        }
    }

    public static boolean isValidTarget(Player caster, Entity entity, CombatModeManager ctx) {
        if (!(entity instanceof LivingEntity living)) return false;
        if (living.getUniqueId().equals(caster.getUniqueId())) return false;
        if (living.isDead() || !living.isValid()) return false;
        return !ctx.isOwnedMinion(caster, living);
    }

    public static List<LivingEntity> getNearbyTargets(Player caster, Location center, double radius, CombatModeManager ctx) {
        List<LivingEntity> list = new ArrayList<>();
        if (center == null || center.getWorld() == null) return list;
        for (Entity e : center.getWorld().getNearbyEntities(center, radius, radius, radius)) {
            if (isValidTarget(caster, e, ctx)) {
                list.add((LivingEntity) e);
            }
        }
        return list;
    }

    public static LivingEntity raycastTarget(Player caster, double maxRange, double raySize, CombatModeManager ctx) {
        Location eye = caster.getEyeLocation();
        Vector dir = eye.getDirection();
        RayTraceResult res = caster.getWorld().rayTraceEntities(
                eye.clone().add(dir.clone().multiply(1.2)),
                dir,
                maxRange,
                raySize,
                e -> isValidTarget(caster, e, ctx)
        );
        if (res != null && res.getHitEntity() instanceof LivingEntity living) {
            return living;
        }
        return null;
    }

    public static Location raycastLocation(Player caster, double maxRange) {
        Location eye = caster.getEyeLocation();
        Vector dir = eye.getDirection();
        RayTraceResult res = caster.getWorld().rayTraceBlocks(eye, dir, maxRange, FluidCollisionMode.NEVER, true);
        if (res != null) {
            return res.getHitPosition().toLocation(caster.getWorld());
        }
        return eye.add(dir.multiply(maxRange));
    }

    public static void dealSkillDamage(Player caster, LivingEntity target, double amount, boolean trueDamage) {
        if (target == null || target.isDead()) return;
        if (trueDamage) {
            double armor = 0.0;
            AttributeInstance armorAttr = target.getAttribute(Attribute.ARMOR);
            if (armorAttr != null) armor = armorAttr.getValue();
            // Bù sát thương xuyên qua lớp giáp thông thường
            double bonus = amount * Math.min(0.65, armor * 0.025);
            target.damage(amount + bonus, caster);
        } else {
            target.damage(amount, caster);
        }
    }

    public static void healPlayer(Player player, double amount) {
        AttributeInstance maxAttr = player.getAttribute(Attribute.MAX_HEALTH);
        double max = maxAttr != null ? maxAttr.getValue() : 20.0;
        player.setHealth(Math.min(max, player.getHealth() + amount));
    }

    public static void freezeEntity(LivingEntity target, int durationTicks) {
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, durationTicks, 10, false, false));
        target.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, durationTicks, 200, false, false));
        target.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, durationTicks, 5, false, false));
    }

    public static void drawBeam(Location start, Location end, Particle particle, double step, int count) {
        if (start.getWorld() == null || !start.getWorld().equals(end.getWorld())) return;
        Vector diff = end.toVector().subtract(start.toVector());
        double length = diff.length();
        if (length < 0.1) return;
        Vector dir = diff.normalize();
        for (double d = 0; d <= length; d += step) {
            Location point = start.clone().add(dir.clone().multiply(d));
            start.getWorld().spawnParticle(particle, point, count, 0.05, 0.05, 0.05, 0.01);
        }
    }

    public static void spawnRing(Location center, Particle particle, double radius, int points) {
        if (center.getWorld() == null) return;
        for (int i = 0; i < points; i++) {
            double angle = (2 * Math.PI * i) / points;
            double x = Math.cos(angle) * radius;
            double z = Math.sin(angle) * radius;
            center.getWorld().spawnParticle(particle, center.clone().add(x, 0.2, z), 1, 0, 0, 0, 0);
        }
    }
}
