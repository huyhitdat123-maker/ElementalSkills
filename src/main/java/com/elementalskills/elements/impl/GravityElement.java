package com.elementalskills.elements.impl;

import com.elementalskills.elements.ElementMastery;
import com.elementalskills.managers.CombatModeManager;
import com.elementalskills.managers.SkillVisualHelper;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.List;

public class GravityElement implements ElementMastery {
    @Override public String getId() { return "trongluc"; }
    @Override public String getDisplayName() { return "Hệ Trọng Lực"; }
    @Override public NamedTextColor getThemeColor() { return NamedTextColor.DARK_PURPLE; }
    @Override public Material getGuiIcon() { return Material.HEAVY_CORE; }
    @Override public int[] getCooldownSeconds() { return new int[]{8, 12, 16, 22, 38}; }

    @Override
    public List<String> getSkillNames() {
        return List.of("Đè Nén Trọng Trường", "Phản Trọng Lực", "Cầu Hấp Dẫn", "Vùng Cấm Địa Trọng Lực", "Thiên Tinh Bạo Toái");
    }

    @Override
    public List<String> getSkillDescriptions() {
        return List.of(
                "Ép chặt kẻ địch bán kính 6 block xuống đất & gây chấn động.",
                "Nhấc bổng kẻ địch lên cao 10 block rồi dập mạnh xuống đất.",
                "Ném cầu trọng lực hút mọi kẻ địch trên đường bay rồi phát nổ.",
                "Trường lực 8 block kéo rơi rụng mọi mũi tên/đạn bay tới trong 5s.",
                "Hút toàn bộ kẻ địch bán kính 14 block tụ lên trời rồi kích nổ."
        );
    }

    @Override
    public void castSkill(Player player, int skillIndex, CombatModeManager ctx) {
        switch (skillIndex) {
            case 0 -> { // Đè Nén Trọng Trường
                Location c = player.getLocation();
                SkillVisualHelper.spawnRing(c, Particle.PORTAL, 6.0, 45);
                SkillVisualHelper.playSound(c, "block.anvil.land", 1.5f, 0.5f);
                for (LivingEntity t : SkillVisualHelper.getNearbyTargets(player, c, 6.0, ctx)) {
                    SkillVisualHelper.dealSkillDamage(player, t, 11.0, true);
                    t.setVelocity(new Vector(0, -1.8, 0));
                    t.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 80, 4));
                    t.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 80, 200));
                }
            }
            case 1 -> { // Phản Trọng Lực
                Location targetLoc = SkillVisualHelper.raycastLocation(player, 18.0);
                List<LivingEntity> victims = SkillVisualHelper.getNearbyTargets(player, targetLoc, 6.5, ctx);
                SkillVisualHelper.playSound(targetLoc, "entity.shulker.shoot", 1.5f, 0.6f);
                for (LivingEntity t : victims) {
                    t.setVelocity(new Vector(0, 1.55, 0));
                    t.getWorld().spawnParticle(Particle.PORTAL, t.getLocation(), 30, 0.5, 0.8, 0.5, 0.2);
                }
                Bukkit.getScheduler().runTaskLater(ctx.getPlugin(), () -> {
                    for (LivingEntity t : victims) {
                        if (t.isValid() && !t.isDead()) {
                            t.setVelocity(new Vector(0, -2.5, 0));
                            SkillVisualHelper.dealSkillDamage(player, t, 14.0, true);
                        }
                    }
                }, 28L);
            }
            case 2 -> { // Cầu Hấp Dẫn
                Location pos = player.getEyeLocation();
                Vector dir = pos.getDirection().normalize();
                new BukkitRunnable() {
                    int step = 0;
                    @Override
                    public void run() {
                        step++;
                        pos.add(dir.clone().multiply(0.9));
                        pos.getWorld().spawnParticle(Particle.PORTAL, pos, 30, 1.2, 1.2, 1.2, 0.1);
                        for (LivingEntity t : SkillVisualHelper.getNearbyTargets(player, pos, 5.5, ctx)) {
                            t.setVelocity(pos.toVector().subtract(t.getLocation().toVector()).normalize().multiply(0.75));
                        }
                        if (step >= 18) {
                            cancel();
                            pos.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, pos, 2, 0.5, 0.5, 0.5, 0);
                            SkillVisualHelper.playSound(pos, "entity.generic.explode", 1.6f, 0.7f);
                            for (LivingEntity t : SkillVisualHelper.getNearbyTargets(player, pos, 5.5, ctx)) {
                                SkillVisualHelper.dealSkillDamage(player, t, 15.0, true);
                            }
                        }
                    }
                }.runTaskTimer(ctx.getPlugin(), 0L, 1L);
            }
            case 3 -> { // Vùng Cấm Địa Trọng Lực
                new BukkitRunnable() {
                    int ticks = 0;
                    @Override
                    public void run() {
                        ticks += 4;
                        Location c = player.getLocation();
                        SkillVisualHelper.spawnRing(c, Particle.PORTAL, 8.0, 40);
                        for (Entity e : c.getWorld().getNearbyEntities(c, 8.0, 8.0, 8.0)) {
                            if (e instanceof Projectile proj && !(proj.getShooter() instanceof Player p && p.equals(player))) {
                                proj.setVelocity(new Vector(0, -1.5, 0));
                            }
                        }
                        if (ticks >= 100) cancel();
                    }
                }.runTaskTimer(ctx.getPlugin(), 0L, 4L);
            }
            case 4 -> { // Thiên Tinh Bạo Toái (Chibaku Tensei — Tuyệt Kỹ)
                Location targetLoc = SkillVisualHelper.raycastLocation(player, 22.0);
                Location core = targetLoc.clone().add(0, 10, 0);
                SkillVisualHelper.playSound(core, "entity.wither.spawn", 2.0f, 0.5f);
                List<LivingEntity> victims = SkillVisualHelper.getNearbyTargets(player, targetLoc, 14.0, ctx);
                new BukkitRunnable() {
                    int ticks = 0;
                    @Override
                    public void run() {
                        ticks += 4;
                        core.getWorld().spawnParticle(Particle.PORTAL, core, 60, 2.5, 2.5, 2.5, 0.1);
                        core.getWorld().spawnParticle(Particle.BLOCK, core, 25, 2.0, 2.0, 2.0, Material.OBSIDIAN.createBlockData());
                        for (LivingEntity t : victims) {
                            if (t.isValid() && !t.isDead()) {
                                t.setVelocity(core.toVector().subtract(t.getLocation().toVector()).normalize().multiply(0.95));
                            }
                        }
                        if (ticks >= 60) {
                            cancel();
                            core.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, core, 5, 2, 2, 2, 0);
                            SkillVisualHelper.playSound(core, "entity.generic.explode", 2.0f, 0.5f);
                            for (LivingEntity t : victims) {
                                if (t.isValid() && !t.isDead()) {
                                    t.setVelocity(new Vector(0, -2.5, 0));
                                    SkillVisualHelper.dealSkillDamage(player, t, 24.0, true);
                                }
                            }
                        }
                    }
                }.runTaskTimer(ctx.getPlugin(), 0L, 4L);
            }
        }
    }
}
