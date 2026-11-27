package com.elementalskills.elements.impl;

import com.elementalskills.elements.ElementMastery;
import com.elementalskills.managers.CombatModeManager;
import com.elementalskills.managers.SkillVisualHelper;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TimeElement implements ElementMastery {
    @Override public String getId() { return "thoigian"; }
    @Override public String getDisplayName() { return "Hệ Thời Gian"; }
    @Override public NamedTextColor getThemeColor() { return NamedTextColor.GOLD; }
    @Override public Material getGuiIcon() { return Material.CLOCK; }
    @Override public int[] getCooldownSeconds() { return new int[]{10, 16, 15, 22, 42}; }

    @Override
    public List<String> getSkillNames() {
        return List.of("Gia Tốc Thời Gian", "Nghịch Đảo Thời Gian", "Lão Hóa Mục Tiêu", "Vòng Lặp Ngưng Trệ", "Thế Giới Ngưng Đọng");
    }

    @Override
    public List<String> getSkillDescriptions() {
        return List.of(
                "Tăng tốc độ chạy & tốc độ đánh lên mức siêu tốc trong 5 giây.",
                "Ghi dấu vị trí & máu hiện tại; sau 4 giây tự tua ngược lại.",
                "Làm chậm thê thảm tốc độ đánh & chạy của kẻ địch trong 4s.",
                "Giam cầm địch trong vùng 7 block 3s rồi dồn nổ sát thương.",
                "Ngưng đọng toàn bộ kẻ địch trong 13 block suốt 4 giây."
        );
    }

    @Override
    public void castSkill(Player player, int skillIndex, CombatModeManager ctx) {
        switch (skillIndex) {
            case 0 -> { // Gia Tốc Thời Gian
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 100, 3));
                player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, 100, 3));
                player.getWorld().spawnParticle(Particle.END_ROD, player.getLocation().add(0, 1, 0), 35, 0.8, 0.8, 0.8, 0.08);
                SkillVisualHelper.playSound(player.getLocation(), "block.note_block.chime", 1.5f, 1.8f);
            }
            case 1 -> { // Nghịch Đảo Thời Gian
                Location savedLoc = player.getLocation().clone();
                double savedHp = player.getHealth();
                SkillVisualHelper.playSound(savedLoc, "block.beacon.power_select", 1.4f, 1.5f);
                player.sendMessage(Component.text("⏳ Đã lưu mốc thời gian! Sẽ tua ngược sau 4 giây.", NamedTextColor.GOLD));
                Bukkit.getScheduler().runTaskLater(ctx.getPlugin(), () -> {
                    if (!player.isOnline() || player.isDead()) return;
                    player.teleport(savedLoc);
                    player.setHealth(Math.max(player.getHealth(), savedHp));
                    savedLoc.getWorld().spawnParticle(Particle.PORTAL, savedLoc.add(0, 1, 0), 50, 0.8, 1.0, 0.8, 0.1);
                    SkillVisualHelper.playSound(savedLoc, "entity.enderman.teleport", 1.5f, 1.6f);
                }, 80L);
            }
            case 2 -> { // Lão Hóa Mục Tiêu
                LivingEntity target = SkillVisualHelper.raycastTarget(player, 18.0, 1.8, ctx);
                if (target == null) return;
                SkillVisualHelper.drawBeam(player.getEyeLocation(), target.getEyeLocation(), Particle.END_ROD, 0.3, 3);
                target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 80, 3));
                target.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 80, 3));
                target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 80, 2));
                SkillVisualHelper.dealSkillDamage(player, target, 10.0, true);
            }
            case 3 -> { // Vòng Lặp Ngưng Trệ
                Location c = player.getLocation();
                List<LivingEntity> victims = SkillVisualHelper.getNearbyTargets(player, c, 7.0, ctx);
                Map<LivingEntity, Double> initialHp = new HashMap<>();
                for (LivingEntity t : victims) {
                    initialHp.put(t, t.getHealth());
                    t.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 4));
                }
                SkillVisualHelper.spawnRing(c, Particle.END_ROD, 7.0, 45);
                Bukkit.getScheduler().runTaskLater(ctx.getPlugin(), () -> {
                    SkillVisualHelper.playSound(c, "entity.generic.explode", 1.6f, 1.3f);
                    for (LivingEntity t : victims) {
                        if (t.isValid() && !t.isDead()) {
                            double lost = Math.max(0, initialHp.getOrDefault(t, t.getHealth()) - t.getHealth());
                            SkillVisualHelper.dealSkillDamage(player, t, 10.0 + lost * 0.75, true);
                        }
                    }
                }, 60L);
            }
            case 4 -> { // Thế Giới Ngưng Đọng (Time Stop — Tuyệt Kỹ)
                Location c = player.getLocation();
                SkillVisualHelper.playSound(c, "entity.warden.sonic_boom", 1.8f, 1.6f);
                List<LivingEntity> frozen = SkillVisualHelper.getNearbyTargets(player, c, 13.0, ctx);
                for (LivingEntity t : frozen) {
                    ctx.applyHardFreeze(t, 80);
                }
                new BukkitRunnable() {
                    int ticks = 0;
                    @Override
                    public void run() {
                        ticks += 10;
                        SkillVisualHelper.spawnRing(c, Particle.END_ROD, 13.0, 64);
                        if (ticks >= 80) {
                            cancel();
                            for (LivingEntity t : frozen) {
                                if (t.isValid() && !t.isDead()) {
                                    SkillVisualHelper.dealSkillDamage(player, t, 16.0, true);
                                }
                            }
                        }
                    }
                }.runTaskTimer(ctx.getPlugin(), 0L, 10L);
            }
        }
    }
}
