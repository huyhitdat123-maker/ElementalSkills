package com.elementalskills.gui;

import com.elementalskills.elements.ElementMastery;
import com.elementalskills.elements.ElementRegistry;
import com.elementalskills.managers.CombatModeManager;
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

public class ElementSelectGui implements Listener {

    private static class ElementMenuHolder implements InventoryHolder {
        @Override
        public @NotNull Inventory getInventory() {
            return Bukkit.createInventory(null, 54);
        }
    }

    private final ElementRegistry registry;
    private final CombatModeManager combatManager;
    private final NamespacedKey guiElementKey;

    public ElementSelectGui(ElementRegistry registry, CombatModeManager combatManager) {
        this.registry = registry;
        this.combatManager = combatManager;
        this.guiElementKey = new NamespacedKey(combatManager.getPlugin(), "es_gui_element_id");
    }

    public void openMenu(Player player) {
        Inventory inv = Bukkit.createInventory(
                new ElementMenuHolder(),
                54,
                Component.text("Chọn Hệ Nguyên Tố (20 Hệ)", NamedTextColor.GOLD, TextDecoration.BOLD)
        );

        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fMeta = filler.getItemMeta();
        if (fMeta != null) {
            fMeta.displayName(Component.empty());
            filler.setItemMeta(fMeta);
        }
        for (int i = 0; i < 54; i++) inv.setItem(i, filler);

        int[] slots = {
                10, 11, 12, 13, 14, 15, 16,
                19, 20, 21, 22, 23, 24, 25,
                28, 29, 30, 31, 32, 33
        };

        ElementMastery current = combatManager.getPlayerElement(player);
        int idx = 0;
        for (ElementMastery el : registry.getAll()) {
            if (idx >= slots.length) break;
            boolean selected = current != null && current.getId().equals(el.getId());

            ItemStack icon = new ItemStack(el.getGuiIcon());
            ItemMeta meta = icon.getItemMeta();
            if (meta != null) {
                meta.displayName(Component.text((selected ? "✔ " : "✦ ") + el.getDisplayName(), el.getThemeColor(), TextDecoration.BOLD)
                        .decoration(TextDecoration.ITALIC, false));

                List<Component> lore = new ArrayList<>();
                for (int i = 0; i < 5; i++) {
                    lore.add(Component.text("Chiêu " + (i + 1) + ": " + el.getSkillNames().get(i), NamedTextColor.GOLD)
                            .decoration(TextDecoration.ITALIC, false));
                }
                lore.add(Component.empty());
                lore.add(Component.text(selected ? "✔ Hệ đang sử dụng" : "Bấm để chuyển sang hệ này!",
                        selected ? NamedTextColor.GREEN : NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));

                meta.lore(lore);
                meta.setEnchantmentGlintOverride(selected);
                meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
                meta.getPersistentDataContainer().set(guiElementKey, PersistentDataType.STRING, el.getId());
                icon.setItemMeta(meta);
            }
            inv.setItem(slots[idx++], icon);
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

        String id = clicked.getItemMeta().getPersistentDataContainer().get(guiElementKey, PersistentDataType.STRING);
        if (id == null) return;

        combatManager.selectElementAdmin(player, id);
        ElementMastery chosen = combatManager.getPlayerElement(player);
        player.closeInventory();
        if (chosen != null) {
            player.sendMessage(Component.text("✔ Đã chọn ", NamedTextColor.GREEN)
                    .append(Component.text(chosen.getDisplayName(), chosen.getThemeColor(), TextDecoration.BOLD))
                    .append(Component.text(" (5 chiêu thức)!", NamedTextColor.GREEN)));
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof ElementMenuHolder) {
            event.setCancelled(true);
        }
    }
}
