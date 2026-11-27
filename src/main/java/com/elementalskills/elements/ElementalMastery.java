package com.elementalskills.elements;

import com.elementalskills.managers.SkillExecutionManager;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.List;

public interface ElementalMastery {

    String getId();

    String getDisplayName();

    NamedTextColor getThemeColor();

    Material getIcon();

    List<String> getOverviewLore();

    String getSkillName(int slot); // slot: 1, 2, 3

    int getSkillCooldown(int slot); // giây

    List<String> getSkillLore(int slot);

    void castSkill1(Player player, SkillExecutionManager ctx);

    void castSkill2(Player player, SkillExecutionManager ctx);

    void castSkill3(Player player, SkillExecutionManager ctx);

    default void tickEnvironment(Player player, SkillExecutionManager ctx) {
    }

    default double modifyOutgoingSkillDamage(Player player, LivingEntity target, double baseDamage, SkillExecutionManager ctx) {
        return baseDamage;
    }

    default void modifyIncomingDamage(Player player, EntityDamageEvent event, SkillExecutionManager ctx) {
    }
}
