package com.elementalskills.commands;

import com.elementalskills.gui.ElementSelectGui;
import com.elementalskills.managers.CombatModeManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.stream.Collectors;

public class ElementalCommand implements CommandExecutor, TabCompleter {

    private final CombatModeManager combatManager;
    private final ElementSelectGui selectGui;

    public ElementalCommand(CombatModeManager combatManager, ElementSelectGui selectGui) {
        this.combatManager = combatManager;
        this.selectGui = selectGui;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        String cmd = command.getName().toLowerCase();

        if (cmd.equals("givegiay")) {
            if (!sender.hasPermission("elementalskills.admin")) {
                sender.sendMessage(Component.text("Bạn không có quyền dùng lệnh này!", NamedTextColor.RED));
                return true;
            }
            if (args.length < 1) {
                sender.sendMessage(Component.text("Dùng: /givegiay <tên_người_chơi> [số_lượng]", NamedTextColor.RED));
                return true;
            }
            Player target = Bukkit.getPlayerExact(args[0]);
            if (target == null) {
                sender.sendMessage(Component.text("Không tìm thấy người chơi này!", NamedTextColor.RED));
                return true;
            }
            int amount = 1;
            if (args.length >= 2) {
                try {
                    amount = Math.max(1, Integer.parseInt(args[1]));
                } catch (NumberFormatException ignored) {}
            }
            target.getInventory().addItem(combatManager.createAwakeningScroll(amount))
                    .values().forEach(rem -> target.getWorld().dropItemNaturally(target.getLocation(), rem));
            sender.sendMessage(Component.text("Đã cấp " + amount + " Giấy Thức Tỉnh cho " + target.getName() + ".", NamedTextColor.GREEN));
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage("Lenh nay chi danh cho nguoi choi!");
            return true;
        }

        switch (cmd) {
            case "batkinang" -> combatManager.toggleCombatMode(player);
            case "chonhe" -> {
                if (!player.hasPermission("elementalskills.admin")) {
                    player.sendMessage(Component.text("Bạn không có quyền mở bảng chọn hệ!", NamedTextColor.RED));
                    return true;
                }
                selectGui.openMenu(player);
            }
            case "dongy" -> combatManager.acceptPendingElement(player);
            case "tuchoi" -> combatManager.declinePendingElement(player);
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (command.getName().equalsIgnoreCase("givegiay")) {
            if (args.length == 1) {
                return Bukkit.getOnlinePlayers().stream().map(Player::getName)
                        .filter(n -> n.toLowerCase().startsWith(args[0].toLowerCase())).collect(Collectors.toList());
            }
            if (args.length == 2) return List.of("1", "2");
        }
        return List.of();
    }
}
