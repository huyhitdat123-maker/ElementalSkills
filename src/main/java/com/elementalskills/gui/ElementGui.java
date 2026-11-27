package com.elementalskills.gui;

import com.elementalskills.ElementalSkillsPlugin;
import com.elementalskills.elements.ElementRegistry;
import com.elementalskills.elements.ElementalMastery;
import com.elementalskills.managers.CombatInventoryManager;
import com.elementalskills.managers.SkillExecutionManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class ElementGui implements Listener {

    private static class ElementMenuHolder implements InventoryHolder {
        @Override
        public @NotNull Inventory getInventory() {
            return Bukkit.createInventory(null, 27);
        }
    }

    private static final int[] DISPLAY_SLOTS = {10, 11, 12, 13, 14, 15, 16, 22};

    private final ElementRegistry registry;
    private final CombatInventoryManager inventoryManager;
    private final NamespacedKey elementIdKey;

    public ElementGui(ElementalSkillsPlugin plugin, ElementRegistry registry, CombatInventoryManager inventoryManager) {
        this.registry = registry;
        this.inventoryManager = inventoryManager;
        this.elementIdKey = new NamespacedKey(plugin, "gui_element_id");
    }

    public void openMenu(Player player) {
        Inventory inv = Bukkit.createInventory(
                new ElementMenuHolder(),
                27,
                Component.text("Bảng Chọn Hệ Nguyên Tố (Admin)", NamedTextColor.DARK_PURPLE, TextDecoration.BOLD)
        );

        ItemStack glass = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta gMeta = glass.getItemMeta();
        if (gMeta != null) {
            gMeta.displayName(Component.empty());
            glass.setItemMeta(gMeta);
        }
        for (int i = 0; i < 27; i++) inv.setItem(i, glass);

        ElementalMastery current = inventoryManager.getChosenElement(player);
        int idx = 0;

        for (ElementalMastery el : registry.getAll()) {
            if (idx >= DISPLAY_SLOTS.length) break;
            boolean selected = current != null && current.getId().equals(el.getId());

            ItemStack item = new ItemStack(el.getIcon());
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.displayName(Component.text((selected ? "✔ [ĐANG CHỌN] " : "") + "Hệ " + el.getDisplayName(),
                        el.getThemeColor(), TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false));

                List<Component> lore = new ArrayList<>();
                for (String line : el.getOverviewLore()) {
                    lore.add(Component.text(line, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
                }
                lore.add(Component.empty());
                for (int s = 1; s <= 3; s++) {
                    lore.add(Component.text("• Chiêu " + s + " (" + el.getSkillName(s) + " - " + el.getSkillCooldown(s) + "s):",
                            NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
                    for (String desc : el.getSkillLore(s)) {
                        lore.add(Component.text("   " + desc, NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false));
                    }
                }
                lore.add(Component.empty());
                lore.add(Component.text(selected ? "✔ Bạn đang sở hữu hệ này!" : "▶ Bấm để chọn Hệ " + el.getDisplayName(),
                        selected ? NamedTextColor.GREEN : NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));

                meta.lore(lore);
                meta.setEnchantmentGlintOverride(selected);
                meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
                meta.getPersistentDataContainer().set(elementIdKey, PersistentDataType.STRING, el.getId());
                item.setItemMeta(meta);
            }
            inv.setItem(DISPLAY_SLOTS[idx++], item);
        }

        player.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof ElementMenuHolder)) return;
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) return;
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;

        String id = clicked.getItemMeta().getPersistentDataContainer().get(elementIdKey, PersistentDataType.STRING);
        if (id == null) return;

        ElementalMastery chosen = registry.get(id);
        if (chosen == null) return;

        inventoryManager.setChosenElement(player, id);
        SkillExecutionManager.playSoundSafe(player, "entity.player.levelup", 1.2f);
        player.sendMessage(Component.text("✦ Đã gán Hệ ", NamedTextColor.GREEN)
                .append(Component.text(chosen.getDisplayName(), chosen.getThemeColor(), TextDecoration.BOLD))
                .append(Component.text("! Gõ /batkinang để rút vũ khí kỹ năng.", NamedTextColor.GREEN)));
        player.closeInventory();
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof ElementMenuHolder) {
            event.setCancelled(true);
        }
    }
}
