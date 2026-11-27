package com.elementalskills;

import com.elementalskills.commands.ElementCommand;
import com.elementalskills.elements.ElementRegistry;
import com.elementalskills.gui.ElementGui;
import com.elementalskills.managers.CombatInventoryManager;
import com.elementalskills.managers.SkillExecutionManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class ElementalSkillsPlugin extends JavaPlugin {

    private ElementRegistry elementRegistry;
    private CombatInventoryManager inventoryManager;
    private SkillExecutionManager executionManager;
    private ElementGui elementGui;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.elementRegistry = new ElementRegistry();
        this.elementRegistry.registerAllDefaults();

        this.inventoryManager = new CombatInventoryManager(this, elementRegistry);
        this.executionManager = new SkillExecutionManager(this, elementRegistry, inventoryManager);
        this.elementGui = new ElementGui(this, elementRegistry, inventoryManager);

        Bukkit.getPluginManager().registerEvents(inventoryManager, this);
        Bukkit.getPluginManager().registerEvents(executionManager, this);
        Bukkit.getPluginManager().registerEvents(elementGui, this);

        ElementCommand cmd = new ElementCommand(inventoryManager, elementGui);
        for (String name : new String[]{"dongy", "tuchoi", "batkinang", "tatkinang", "givegiay", "chonhe"}) {
            if (getCommand(name) != null) {
                getCommand(name).setExecutor(cmd);
                getCommand(name).setTabCompleter(cmd);
            }
        }

        // Vòng lặp kiểm tra ưu/nhược điểm môi trường mỗi 1 giây (20 ticks)
        Bukkit.getScheduler().runTaskTimer(this, () -> executionManager.tickEnvironments(), 20L, 20L);

        getLogger().info("ElementalSkills da khoi dong! Da nap " + elementRegistry.getAll().size() + " he nguyen to.");
    }

    @Override
    public void onDisable() {
        if (executionManager != null) {
            executionManager.cleanupAllTemporaryBlocks();
        }
        if (inventoryManager != null) {
            inventoryManager.restoreAllOnlinePlayers();
        }
    }
}
