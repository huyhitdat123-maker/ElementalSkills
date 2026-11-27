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

import java.util.List;

public class PoisonElement implements ElementMastery {
    @Override public String getId() { return "docduoc"; }
    @Override public String getDisplayName() { return "Hệ Độc Dược"; }
    @Override public NamedTextColor getThemeColor() { return NamedTextColor.DARK_GREEN; }
    @Override public Material getGuiIcon() { return Material.FERMENTED_SPIDER_EYE; }
    @Override public int[] getCooldownSeconds() { return new int[]{8, 11, 15, 20, 36}; }

    @Override
    public List<String> getSkillNames() {
        return List.of("Phun Sương Độc", "Phi Tiêu Kịch Độc", "Bẫy Bào Tử Nổ", "Chuyển Hóa Độc Tố", "Đại Dịch Bùng Phát");
    }

    @Override
    public List<String> getSkillDescriptions() {
        return List.of(
                "Thổi làn khói độc lục bảo 10 block gây Độc II & mù tầm nhìn.",
                "Phóng liên tiếp 5 phi tiêu tẩm độc tốc độ cao.",
                "Đặt cụm bào tử phát nổ tạo vùng khói độc 6 block trong 5s.",
                "Hấp thụ độc tố hồi máu lớn & miễn nhiễm mọi hiệu ứng xấu 6s.",
                "Giải phóng bão độc 13 block rút máu không phanh toàn bộ kẻ địch."
        );
    }

    @Override
    public void castSkill(Player player, int skillIndex, CombatModeManager ctx) {
        Particle.DustOptions poisonDust = new Particle.DustOptions(Color.fromRGB(30, 180, 40), 2.0f);
        switch (skillIndex) {
            case 0 -> { // Phun Sương Độc
                Location start = player.getEyeLocation();
                Vector dir = start.getDirection().normalize();
                SkillVisualHelper.playSound(start, "entity.witch.throw", 1.4f, 0.8f);
                for (double d = 1; d <= 10; d += 1.0) {
                    Location pt = start.clone().add(dir.clone().multiply(d));
                    pt.getWorld().spawnParticle(Particle.DUST, pt, 20, 1.2, 1.0, 1.2, poisonDust);
                    for (LivingEntity t : SkillVisualHelper.getNearbyTargets(player, pt, 2.5, ctx)) {
                        SkillVisualHelper.dealSkillDamage(player, t, 8.0, true);
                        t.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 120, 2));
                        t.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 60, 0));
                    }
                }
            }
            case 1 -> { // Phi Tiêu Kịch Độc
                for (int dart = 0; dart < 5; dart++) {
                    Bukkit.getScheduler().runTaskLater(ctx.getPlugin(), () -> {
                        Location start = player.getEyeLocation();
                        Vector dir = start.getDirection().normalize();
                        Location end = start.clone().add(dir.clone().multiply(18));
                        SkillVisualHelper.drawBeam(start, end, Particle.ITEM_SLIME, 0.4, 2);
                        SkillVisualHelper.playSound(start, "entity.arrow.shoot", 1.3f, 1.5f);
                        for (double d = 1; d <= 18; d += 1.5) {
                            for (LivingEntity t : SkillVisualHelper.getNearbyTargets(player, start.clone().add(dir.clone().multiply(d)), 1.6, ctx)) {
                                SkillVisualHelper.dealSkillDamage(player, t, 6.5, true);
                                t.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 140, 2));
                            }
                        }
                    }, dart * 4L);
                }
            }
            case 2 -> { // Bẫy Bào Tử Nổ
                Location spore = SkillVisualHelper.raycastLocation(player, 16.0);
                Bukkit.getScheduler().runTaskLater(ctx.getPlugin(), () -> {
                    SkillVisualHelper.playSound(spore, "entity.dragon_fireball.explode", 1.6f, 0.9f);
                    new BukkitRunnable() {
                        int ticks = 0;
                        @Override
                        public void run() {
                            ticks += 10;
                            SkillVisualHelper.spawnRing(spore, Particle.ITEM_SLIME, 6.0, 40);
                            spore.getWorld().spawnParticle(Particle.DUST, spore.clone().add(0, 1, 0), 40, 3.0, 1.0, 3.0, poisonDust);
                            for (LivingEntity t : SkillVisualHelper.getNearbyTargets(player, spore, 6.0, ctx)) {
                                SkillVisualHelper.dealSkillDamage(player, t, 4.5, true);
                                t.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 80, 2));
                            }
                            if (ticks >= 100) cancel();
                        }
                    }.runTaskTimer(ctx.getPlugin(), 0L, 10L);
                }, 25L);
            }
            case 3 -> { // Chuyển Hóa Độc Tố
                for (PotionEffect eff : player.getActivePotionEffects()) {
                    player.removePotionEffect(eff.getType());
                }
                SkillVisualHelper.healPlayer(player, 9.0);
                player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 120, 2));
                player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 120, 1));
                player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, player.getLocation().add(0, 1, 0), 40, 1.0, 1.0, 1.0, 0.1);
                SkillVisualHelper.playSound(player.getLocation(), "entity.witch.drink", 1.4f, 1.2f);
            }
            case 4 -> { // Đại Dịch Bùng Phát (Tuyệt Kỹ)
                Location c = player.getLocation();
                SkillVisualHelper.playSound(c, "entity.ender_dragon.growl", 1.8f, 0.8f);
                new BukkitRunnable() {
                    int ticks = 0;
                    @Override
                    public void run() {
                        ticks += 10;
                        SkillVisualHelper.spawnRing(c, Particle.ITEM_SLIME, 13.0, 60);
                        c.getWorld().spawnParticle(Particle.DUST, c.clone().add(0, 1.5, 0), 60, 6.0, 1.5, 6.0, poisonDust);
                        for (LivingEntity t : SkillVisualHelper.getNearbyTargets(player, c, 13.0, ctx)) {
                            SkillVisualHelper.dealSkillDamage(player, t, 6.5, true);
                            t.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 100, 3));
                            t.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 100, 1));
                            t.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 100, 2));
                        }
                        if (ticks >= 100) cancel();
                    }
                }.runTaskTimer(ctx.getPlugin(), 0L, 10L);
            }
        }
    }
}
