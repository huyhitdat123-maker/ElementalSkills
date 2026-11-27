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
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class DatElement implements ElementalMastery {

    @Override
    public String getId() {
        return "dat";
    }

    @Override
    public String getDisplayName() {
        return "Đất";
    }

    @Override
    public NamedTextColor getThemeColor() {
        return NamedTextColor.GOLD;
    }

    @Override
    public Material getIcon() {
        return Material.POINTED_DRIPSTONE;
    }

    @Override
    public List<String> getOverviewLore() {
        return List.of(
                "Ưu điểm: Chạm đất hoặc dưới lòng đất giảm 20% sát thương nhận vào.",
                "Nhược điểm: Lơ lửng trên không hoặc dưới nước sâu giảm 35% sát thương."
        );
    }

    @Override
    public String getSkillName(int slot) {
        return switch (slot) {
            case 1 -> "Địa Chấn Nứt Đất";
            case 2 -> "Thạch Thứ Xuyên Tâm";
            default -> "Bất Động Như Sơn";
        };
    }

    @Override
    public int getSkillCooldown(int slot) {
        return switch (slot) {
            case 1 -> 7;
            case 2 -> 6;
            default -> 15;
        };
    }

    @Override
    public List<String> getSkillLore(int slot) {
        return switch (slot) {
            case 1 -> List.of("Dậm đất tạo chấn động 5 block hất tung toàn bộ địch.");
            case 2 -> List.of("Triệu hồi hàng gai đá nhọn trồi lên theo đường thẳng 12 block.");
            default -> List.of("Hóa cứng nhận giáp ảo & kháng đẩy lùi tuyệt đối 5 giây.");
        };
    }

    @Override
    public void modifyIncomingDamage(Player player, EntityDamageEvent event, SkillExecutionManager ctx) {
        if (player.isOnGround() || player.getLocation().getBlockY() < 55) {
            event.setDamage(event.getDamage() * 0.80);
        }
    }

    @Override
    public double modifyOutgoingSkillDamage(Player player, LivingEntity target, double baseDamage, SkillExecutionManager ctx) {
        if (!player.isOnGround() || player.isInWater()) {
            return baseDamage * 0.65;
        }
        return baseDamage;
    }

    @Override
    public void castSkill1(Player player, SkillExecutionManager ctx) {
        Location c = player.getLocation();
        World w = c.getWorld();

        SkillExecutionManager.playSoundSafe(player, "entity.iron_golem.attack", 0.7f);
        player.sendActionBar(Component.text("⛰ Địa Chấn Nứt Đất!", NamedTextColor.GOLD, TextDecoration.BOLD));
        w.spawnParticle(Particle.BLOCK, c.clone().add(0, 0.3, 0), 60, 3.0, 0.3, 3.0, Material.DIRT.createBlockData());

        for (Entity e : w.getNearbyEntities(c, 5.0, 3.0, 5.0)) {
            if (e instanceof LivingEntity le && le != player) {
                le.damage(ctx.calculateFinalSkillDamage(player, le, 7.5), player);
                le.setVelocity(new Vector(0, 0.85, 0));
            }
        }
    }

    @Override
    public void castSkill2(Player player, SkillExecutionManager ctx) {
        Location start = player.getLocation();
        Vector dir = start.getDirection().setY(0).normalize();
        World w = start.getWorld();

        SkillExecutionManager.playSoundSafe(player, "block.pointed_dripstone.land", 0.8f);
        player.sendActionBar(Component.text("🪨 Thạch Thứ Xuyên Tâm!", NamedTextColor.GOLD, TextDecoration.BOLD));

        Set<UUID> hit = new HashSet<>();
        for (int i = 1; i <= 12; i++) {
            int step = i;
            Bukkit.getScheduler().runTaskLater(ctx.getPlugin(), () -> {
                Location spikeLoc = start.clone().add(dir.clone().multiply(step));
                w.spawnParticle(Particle.BLOCK, spikeLoc.clone().add(0, 0.8, 0), 20, 0.3, 0.7, 0.3, Material.POINTED_DRIPSTONE.createBlockData());
                for (Entity e : w.getNearbyEntities(spikeLoc, 1.4, 2.2, 1.4)) {
                    if (e instanceof LivingEntity le && le != player && hit.add(le.getUniqueId())) {
                        le.damage(ctx.calculateFinalSkillDamage(player, le, 8.0), player);
                        le.setVelocity(dir.clone().multiply(0.9).setY(0.5));
                    }
                }
            }, i * 1L);
        }
    }

    @Override
    public void castSkill3(Player player, SkillExecutionManager ctx) {
        SkillExecutionManager.playSoundSafe(player, "block.anvil.land", 0.8f);
        player.sendActionBar(Component.text("🛡 Bất Động Như Sơn (5s)!", NamedTextColor.GOLD, TextDecoration.BOLD));

        player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 100, 3));
        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 100, 2));
        player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 100, 250));

        AttributeInstance kb = player.getAttribute(Attribute.KNOCKBACK_RESISTANCE);
        double oldVal = kb != null ? kb.getBaseValue() : 0.0;
        if (kb != null) kb.setBaseValue(1.0);

        Bukkit.getScheduler().runTaskLater(ctx.getPlugin(), () -> {
            if (player.isOnline()) {
                AttributeInstance cur = player.getAttribute(Attribute.KNOCKBACK_RESISTANCE);
                if (cur != null) cur.setBaseValue(oldVal);
            }
        }, 100L);
    }
}
