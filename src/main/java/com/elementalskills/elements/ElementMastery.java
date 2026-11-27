package com.elementalskills.elements;

import com.elementalskills.managers.CombatModeManager;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.List;

public interface ElementMastery {

    String getId();

    String getDisplayName();

    NamedTextColor getThemeColor();

    Material getGuiIcon();

    List<String> getSkillNames();

    List<String> getSkillDescriptions();

    int[] getCooldownSeconds();

    void castSkill(Player player, int skillIndex, CombatModeManager ctx);
}
