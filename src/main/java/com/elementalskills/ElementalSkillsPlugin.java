package com.elementalskills;

import com.elementalskills.commands.ElementalCommand;
import com.elementalskills.elements.ElementRegistry;
import com.elementalskills.gui.ElementSelectGui;
import com.elementalskills.managers.CombatModeManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class ElementalSkillsPlugin extends JavaPlugin {

    private ElementRegistry elementRegistry;
    private CombatModeManager combatModeManager;
    private ElementSelectGui selectGui;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.elementRegistry = new ElementRegistry();
        this.elementRegistry.registerAllDefaults();

        this.combatModeManager = new CombatModeManager(this, elementRegistry);
        this.selectGui = new ElementSelectGui(elementRegistry, combatModeManager);

        Bukkit.getPluginManager().registerEvents(combatModeManager, this);
        Bukkit.getPluginManager().registerEvents(selectGui, this);

        ElementalCommand cmd = new ElementalCommand(combatModeManager, selectGui);
        for (String name : new String[]{"batkinang", "chonhe", "dongy", "tuchoi", "givegiay"}) {
            if (getCommand(name) != null) {
                getCommand(name).setExecutor(cmd);
                getCommand(name).setTabCompleter(cmd);
            }
        }

        // Vòng lặp mỗi 1 giây cập nhật cooldown ActionBar và kiểm tra mốc XP mở khóa chiêu
        Bukkit.getScheduler().runTaskTimer(this, () -> combatModeManager.tickActionBarAndUnlocks(), 20L, 20L);

        getLogger().info("ElementalSkills v2.0 da khoi dong! Da nap " + elementRegistry.getAll().size() + " he (100 chieu).");
    }

    @Override
    public void onDisable() {
        if (combatModeManager != null) {
            combatModeManager.restoreAllOnlinePlayers();
            combatModeManager.saveData();
        }
    }
}
