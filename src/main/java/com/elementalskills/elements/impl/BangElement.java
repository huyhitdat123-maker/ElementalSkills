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
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class BangElement implements ElementalMastery {

    @Override
    public String getId() {
        return "bang";
    }

    @Override
    public String getDisplayName() {
        return "Băng";
    }

    @Override
    public NamedTextColor getThemeColor() {
        return NamedTextColor.BLUE;
    }

    @Override
    public Material getIcon() {
        return Material.BLUE_ICE;
    }

    @Override
    public List<String> getOverviewLore() {
        return List.of(
                "Ưu điểm: Đi trên Băng/Tuyết được tăng tốc độ & hồi máu nhẹ.",
                "Nhược điểm: Ở Nether giảm một nửa hiệu lực làm chậm."
        );
    }

    @Override
    public String getSkillName(int slot) {
        return switch (slot) {
            case 1 -> "Mưa Gai Băng";
            case 2 -> "Băng Tường Hộ Mệnh";
            default -> "Tuyệt Đối Lĩnh Vực";
        };
    }

    @Override
    public int getSkillCooldown(int slot) {
        return switch (slot) {
            case 1 -> 5;
            case 2 -> 9;
            default -> 15;
        };
    }

    @Override
    public List<String> getSkillLore(int slot) {
        return switch (slot) {
            case 1 -> List.of("Phóng loạt mảnh băng hình nón gây sát thương & đóng băng.");
            case 2 -> List.of("Dựng tường băng trước mặt trong 4 giây để chặn đòn.");
            default -> List.of("Đóng băng cứng toàn bộ địch trong 6 block rồi nổ vỡ băng.");
        };
    }

    @Override
    public void tickEnvironment(Player player, SkillExecutionManager ctx) {
        Material under = player.getLocation().subtract(0, 1, 0).getBlock().getType();
        String name = under.name();
        if (name.contains("ICE") || name.contains("SNOW")) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 40, 1, true, false));
            player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 40, 0, true, false));
        }
    }

    @Override
    public void castSkill1(Player player, SkillExecutionManager ctx) {
        Location eye = player.getEyeLocation();
        Vector forward = eye.getDirection().normalize();
        Vector right = new Vector(-forward.getZ(), 0, forward.getX()).normalize();
        double slowMult = player.getWorld().getEnvironment() == World.Environment.NETHER ? 0.5 : 1.0;

        SkillExecutionManager.playSoundSafe(player, "block.glass.break", 1.4f);
        player.sendActionBar(Component.text("❄ Mưa Gai Băng!", NamedTextColor.AQUA, TextDecoration.BOLD));

        Set<UUID> hit = new HashSet<>();
        for (int i = -2; i <= 2; i++) {
            Vector coneDir = forward.clone().add(right.clone().multiply(i * 0.16)).normalize();
            for (int d = 1; d <= 10; d++) {
                Location p = eye.clone().add(coneDir.clone().multiply(d));
                p.getWorld().spawnParticle(Particle.SNOWFLAKE, p, 3, 0.1, 0.1, 0.1, 0.02);
                for (Entity e : p.getWorld().getNearbyEntities(p, 1.1, 1.1, 1.1)) {
                    if (e instanceof LivingEntity le && le != player && hit.add(le.getUniqueId())) {
                        le.damage(ctx.calculateFinalSkillDamage(player, le, 6.5), player);
                        le.setFreezeTicks((int) (160 * slowMult));
                        le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, (int) (70 * slowMult), 3));
                    }
                }
            }
        }
    }

    @Override
    public void castSkill2(Player player, SkillExecutionManager ctx) {
        Location base = player.getLocation();
        Vector dir = base.getDirection().setY(0).normalize();
        Vector right = new Vector(-dir.getZ(), 0, dir.getX()).normalize();
        Location center = base.clone().add(dir.multiply(3.0));

        SkillExecutionManager.playSoundSafe(player, "block.glass.place", 0.8f);
        player.sendActionBar(Component.text("🧊 Băng Tường Hộ Mệnh!", NamedTextColor.BLUE, TextDecoration.BOLD));

        for (int w = -2; w <= 2; w++) {
            for (int h = 0; h <= 2; h++) {
                Block b = center.clone().add(right.clone().multiply(w)).add(0, h, 0).getBlock();
                ctx.placeTemporaryBlock(b, Material.PACKED_ICE, 80);
            }
        }
    }

    @Override
    public void castSkill3(Player player, SkillExecutionManager ctx) {
        Location c = player.getLocation();
        World w = c.getWorld();
        double slowMult = w.getEnvironment() == World.Environment.NETHER ? 0.5 : 1.0;
        int rootTicks = (int) (60 * slowMult);

        SkillExecutionManager.playSoundSafe(player, "entity.player.hurt_freeze", 0.9f);
        player.sendActionBar(Component.text("❄ Tuyệt Đối Lĩnh Vực!", NamedTextColor.AQUA, TextDecoration.BOLD));

        for (int angle = 0; angle < 360; angle += 10) {
            double rad = Math.toRadians(angle);
            w.spawnParticle(Particle.SNOWFLAKE, c.clone().add(Math.cos(rad) * 6.0, 0.5, Math.sin(rad) * 6.0), 4, 0.2, 0.4, 0.2, 0.02);
        }

        Set<LivingEntity> frozen = new HashSet<>();
        for (Entity e : w.getNearbyEntities(c, 6.0, 3.5, 6.0)) {
            if (e instanceof LivingEntity le && le != player) {
                frozen.add(le);
                le.setFreezeTicks(220);
                le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, rootTicks, 10));
                le.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, rootTicks, 250));
            }
        }

        Bukkit.getScheduler().runTaskLater(ctx.getPlugin(), () -> {
            for (LivingEntity le : frozen) {
                if (le.isValid() && !le.isDead()) {
                    le.getWorld().spawnParticle(Particle.BLOCK, le.getLocation().add(0, 1, 0), 25, 0.4, 0.5, 0.4, Material.BLUE_ICE.createBlockData());
                    double dmg = ctx.calculateFinalSkillDamage(player, le, 9.0);
                    le.setHealth(Math.max(0.0, le.getHealth() - dmg));
                    le.damage(0.01, player);
                }
            }
            if (player.isOnline()) {
                SkillExecutionManager.playSoundSafe(player, "block.glass.break", 0.8f);
            }
        }, rootTicks);
    }
}
