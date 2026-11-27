package com.elementalskills.elements.impl;

import com.elementalskills.elements.ElementMastery;
import com.elementalskills.managers.CombatModeManager;
import com.elementalskills.managers.SkillVisualHelper;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;

public class SummonElement implements ElementMastery {
    @Override public String getId() { return "trieuhoi"; }
    @Override public String getDisplayName() { return "Hệ Triệu Hồi"; }
    @Override public NamedTextColor getThemeColor() { return NamedTextColor.LIGHT_PURPLE; }
    @Override public Material getGuiIcon() { return Material.TOTEM_OF_UNDYING; }
    @Override public int[] getCooldownSeconds() { return new int[]{14, 16, 20, 24, 40}; }

    @Override
    public List<String> getSkillNames() {
        return List.of("Triệu Hồi Bầy Sói Linh Hồn", "Đại Quân Xương Xạ Thủ", "Vệ Thần Thiết Giáp", "Bầy Dơi Hút Huyết (Vex)", "Cổng Triệu Hồi Ma Vương");
    }

    @Override
    public List<String> getSkillDescriptions() {
        return List.of(
                "Triệu hồi 4 Sói Tinh Linh tốc độ cao cắn xé kẻ địch trong 15s.",
                "Triệu hồi 3 Cung Thủ Hắc Ám bắn tên xuyên giáp trong 15s.",
                "Triệu hồi Cự Nhân Thép Hộ Vệ (Iron Golem) bảo vệ 18s.",
                "Giải phóng 6 Tinh Linh Kiếm Bay (Vex) chém xuyên địch trong 15s.",
                "Mở Cổng Hắc Ám triệu hồi Hộ Vệ Bóng Tối càn quét 15s."
        );
    }

    @Override
    public void castSkill(Player player, int skillIndex, CombatModeManager ctx) {
        Location base = player.getLocation();
        List<LivingEntity> enemies = SkillVisualHelper.getNearbyTargets(player, base, 16.0, ctx);
        LivingEntity primaryTarget = enemies.isEmpty() ? null : enemies.get(0);

        switch (skillIndex) {
            case 0 -> { // 4 Sói Linh Hồn
                SkillVisualHelper.playSound(base, "entity.wolf.howl", 1.4f, 1.1f);
                for (int i = 0; i < 4; i++) {
                    Wolf wolf = (Wolf) player.getWorld().spawnEntity(base.clone().add((i - 1.5), 0, 1), EntityType.WOLF);
                    wolf.customName(Component.text("Sói Linh Hồn (" + player.getName() + ")", NamedTextColor.LIGHT_PURPLE));
                    wolf.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 320, 2));
                    wolf.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 320, 1));
                    if (primaryTarget != null) wolf.setTarget(primaryTarget);
                    ctx.registerMinion(player, wolf, 15);
                }
            }
            case 1 -> { // 3 Cung Thủ Hắc Ám
                SkillVisualHelper.playSound(base, "entity.skeleton.ambient", 1.4f, 0.9f);
                for (int i = 0; i < 3; i++) {
                    Skeleton sk = (Skeleton) player.getWorld().spawnEntity(base.clone().add((i - 1) * 1.5, 0, 1), EntityType.SKELETON);
                    sk.customName(Component.text("Xạ Thủ Hắc Ám (" + player.getName() + ")", NamedTextColor.DARK_PURPLE));
                    sk.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 320, 0));
                    sk.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 320, 1));
                    if (primaryTarget != null) sk.setTarget(primaryTarget);
                    ctx.registerMinion(player, sk, 15);
                }
            }
            case 2 -> { // Vệ Thần Thiết Giáp
                SkillVisualHelper.playSound(base, "entity.iron_golem.repair", 1.5f, 0.8f);
                IronGolem golem = (IronGolem) player.getWorld().spawnEntity(base.clone().add(0, 0, 1.5), EntityType.IRON_GOLEM);
                golem.customName(Component.text("Vệ Thần (" + player.getName() + ")", NamedTextColor.AQUA));
                golem.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 380, 1));
                golem.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 380, 1));
                if (primaryTarget != null) golem.setTarget(primaryTarget);
                ctx.registerMinion(player, golem, 18);
            }
            case 3 -> { // 6 Tinh Linh Vex
                SkillVisualHelper.playSound(base, "entity.vex.charge", 1.5f, 1.1f);
                for (int i = 0; i < 6; i++) {
                    Vex vex = (Vex) player.getWorld().spawnEntity(base.clone().add(0, 1.5, 0), EntityType.VEX);
                    vex.customName(Component.text("Tinh Linh Kiếm (" + player.getName() + ")", NamedTextColor.RED));
                    vex.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 320, 1));
                    if (primaryTarget != null) vex.setTarget(primaryTarget);
                    ctx.registerMinion(player, vex, 15);
                }
            }
            case 4 -> { // Cổng Triệu Hồi Ma Vương (Tuyệt Kỹ)
                Location spawnLoc = SkillVisualHelper.raycastLocation(player, 10.0);
                spawnLoc.getWorld().spawnParticle(Particle.SCULK_SOUL, spawnLoc.clone().add(0, 1, 0), 60, 1.5, 1.5, 1.5, 0.08);
                SkillVisualHelper.playSound(spawnLoc, "entity.warden.emerge", 1.8f, 0.9f);
                Warden warden = (Warden) player.getWorld().spawnEntity(spawnLoc, EntityType.WARDEN);
                warden.customName(Component.text("Hộ Vệ Bóng Tối (" + player.getName() + ")", NamedTextColor.DARK_AQUA));
                if (primaryTarget != null) {
                    warden.setAnger(primaryTarget, 150);
                    warden.setTarget(primaryTarget);
                }
                ctx.registerMinion(player, warden, 15);
            }
        }
    }
}
