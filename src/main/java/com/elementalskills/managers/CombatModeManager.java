package com.elementalskills.managers;

import com.elementalskills.ElementalSkillsPlugin;
import com.elementalskills.elements.ElementMastery;
import com.elementalskills.elements.ElementRegistry;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class CombatModeManager implements Listener {

    private final ElementalSkillsPlugin plugin;
    private final ElementRegistry registry;
    private final NamespacedKey skillItemKey;
    private final NamespacedKey skillSlotKey;
    private final NamespacedKey awakeningScrollKey;
    private final NamespacedKey minionOwnerKey;
    private final NamespacedKey customBulletKey;

    private final File stashFile;
    private final YamlConfiguration stashConfig;

    private final Set<UUID> activeCombatPlayers = new HashSet<>();
    private final Map<UUID, ItemStack[]> savedHotbars = new HashMap<>();
    private final Map<UUID, String> playerElements = new HashMap<>();
    private final Map<UUID, String> pendingElements = new HashMap<>();
    private final Map<UUID, Set<String>> rejectedElements = new HashMap<>();
    private final Map<UUID, Integer> unlockedStages = new HashMap<>();
    private final Map<UUID, long[]> cooldowns = new HashMap<>();

    // Dữ liệu trạng thái chiến đấu tạm thời
    private final Map<UUID, Long> fallImmunityUntil = new HashMap<>();
    private final Map<UUID, Long> freezeUntil = new HashMap<>();
    private final Map<UUID, Long> infinityShieldUntil = new HashMap<>();
    private final Map<UUID, Double> diamondReflectPercent = new HashMap<>();
    private final Map<UUID, Long> diamondReflectUntil = new HashMap<>();
    private final Map<UUID, Double> resonanceBonus = new HashMap<>();
    private final Map<UUID, Long> resonanceUntil = new HashMap<>();

    public CombatModeManager(ElementalSkillsPlugin plugin, ElementRegistry registry) {
        this.plugin = plugin;
        this.registry = registry;
        this.skillItemKey = new NamespacedKey(plugin, "es_skill_item");
        this.skillSlotKey = new NamespacedKey(plugin, "es_skill_slot");
        this.awakeningScrollKey = new NamespacedKey(plugin, "es_awakening_scroll");
        this.minionOwnerKey = new NamespacedKey(plugin, "es_minion_owner");
        this.customBulletKey = new NamespacedKey(plugin, "es_custom_bullet");

        this.stashFile = new File(plugin.getDataFolder(), "combat_stash.yml");
        this.stashConfig = YamlConfiguration.loadConfiguration(stashFile);

        for (Player online : Bukkit.getOnlinePlayers()) {
            loadPlayerData(online);
        }
    }

    public ElementalSkillsPlugin getPlugin() {
        return plugin;
    }

    public void saveData() {
        try {
            stashConfig.save(stashFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Loi luu combat_stash.yml: " + e.getMessage());
        }
    }

    // =========================================================================
    // TRẠNG THÁI HỖ TRỢ CHO 20 HỆ
    // =========================================================================

    public void grantFallImmunity(Player player, int seconds) {
        fallImmunityUntil.put(player.getUniqueId(), System.currentTimeMillis() + seconds * 1000L);
    }

    public void applyHardFreeze(LivingEntity target, int ticks) {
        freezeUntil.put(target.getUniqueId(), System.currentTimeMillis() + (ticks * 50L));
        SkillVisualHelper.freezeEntity(target, ticks);
    }

    public boolean isHardFrozen(Entity entity) {
        return System.currentTimeMillis() < freezeUntil.getOrDefault(entity.getUniqueId(), 0L);
    }

    public void grantInfinityShield(Player player, int seconds) {
        infinityShieldUntil.put(player.getUniqueId(), System.currentTimeMillis() + seconds * 1000L);
    }

    public boolean hasInfinityShield(Player player) {
        return System.currentTimeMillis() < infinityShieldUntil.getOrDefault(player.getUniqueId(), 0L);
    }

    public void grantDiamondReflect(Player player, double percent, int seconds) {
        diamondReflectPercent.put(player.getUniqueId(), percent);
        diamondReflectUntil.put(player.getUniqueId(), System.currentTimeMillis() + seconds * 1000L);
    }

    public void markSoundResonance(LivingEntity target, double bonusDamage, int seconds) {
        resonanceBonus.put(target.getUniqueId(), bonusDamage);
        resonanceUntil.put(target.getUniqueId(), System.currentTimeMillis() + seconds * 1000L);
    }

    public void registerMinion(Player owner, LivingEntity minion, int lifetimeSeconds) {
        minion.getPersistentDataContainer().set(minionOwnerKey, PersistentDataType.STRING, owner.getUniqueId().toString());
        minion.setPersistent(false);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (minion.isValid() && !minion.isDead()) {
                minion.getWorld().spawnParticle(Particle.SMOKE, minion.getLocation().add(0, 0.8, 0), 15, 0.3, 0.4, 0.3, 0.02);
                minion.remove();
            }
        }, lifetimeSeconds * 20L);
    }

    public boolean isOwnedMinion(Player owner, Entity entity) {
        if (entity == null || !entity.getPersistentDataContainer().has(minionOwnerKey, PersistentDataType.STRING)) {
            return false;
        }
        String stored = entity.getPersistentDataContainer().get(minionOwnerKey, PersistentDataType.STRING);
        return owner.getUniqueId().toString().equals(stored);
    }

    public UUID getMinionOwner(Entity entity) {
        if (entity == null || !entity.getPersistentDataContainer().has(minionOwnerKey, PersistentDataType.STRING)) {
            return null;
        }
        String stored = entity.getPersistentDataContainer().get(minionOwnerKey, PersistentDataType.STRING);
        try {
            return UUID.fromString(stored);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public void tagCustomShulkerBullet(ShulkerBullet bullet, Player shooter, double damage) {
        bullet.setShooter(shooter);
        bullet.getPersistentDataContainer().set(customBulletKey, PersistentDataType.DOUBLE, damage);
    }

    // =========================================================================
    // DỮ LIỆU NGƯỜI CHƠI & GIẤY THỨC TỈNH
    // =========================================================================

    private void loadPlayerData(Player player) {
        UUID uuid = player.getUniqueId();
        String base = "players." + uuid;

        String savedEl = stashConfig.getString(base + ".element");
        if (savedEl != null && registry.get(savedEl) != null) {
            playerElements.put(uuid, savedEl.toLowerCase());
        }

        String savedPending = stashConfig.getString(base + ".pending");
        if (savedPending != null && registry.get(savedPending) != null) {
            pendingElements.put(uuid, savedPending.toLowerCase());
        }

        rejectedElements.put(uuid, new HashSet<>(stashConfig.getStringList(base + ".rejected")));
        unlockedStages.put(uuid, Math.max(1, stashConfig.getInt(base + ".unlocked_stage", 1)));

        // Khôi phục 5 ô hotbar nếu thoát game lúc đang bật /batkinang
        if (stashConfig.contains(base + ".hotbar")) {
            ItemStack[] restored = new ItemStack[5];
            for (int i = 0; i < 5; i++) {
                restored[i] = stashConfig.getItemStack(base + ".hotbar." + i);
            }
            savedHotbars.put(uuid, restored);
            restorePlayerHotbar(player);
        }

        // Cấp 2 Giấy Thức Tỉnh duy nhất 1 lần khi vào server lần đầu
        if (!stashConfig.getBoolean(base + ".starter_given", false)) {
            stashConfig.set(base + ".starter_given", true);
            saveData();

            int count = Math.max(1, plugin.getConfig().getInt("starter-scrolls", 2));
            player.getInventory().addItem(createAwakeningScroll(count))
                    .values().forEach(rem -> player.getWorld().dropItemNaturally(player.getLocation(), rem));

            player.sendMessage(Component.text("✦ Chào mừng! Bạn nhận được ", NamedTextColor.GOLD)
                    .append(Component.text(count + " Giấy Thức Tỉnh", NamedTextColor.YELLOW, TextDecoration.BOLD))
                    .append(Component.text(". Chuột phải để thức tỉnh 1 trong 20 Hệ Sức Mạnh!", NamedTextColor.GOLD)));
        } else if (pendingElements.containsKey(uuid)) {
            ElementMastery pending = registry.get(pendingElements.get(uuid));
            if (pending != null) {
                sendAwakeningPrompt(player, pending);
            }
        }
    }

    public ItemStack createAwakeningScroll(int amount) {
        ItemStack scroll = new ItemStack(Material.PAPER, Math.max(1, amount));
        ItemMeta meta = scroll.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("✦ Giấy Thức Tỉnh Nguyên Tố ✦", NamedTextColor.GOLD, TextDecoration.BOLD)
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(
                    Component.text("Chuột Phải để quay ngẫu nhiên 1 trong 20 Hệ (100 Chiêu).", NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false),
                    Component.text("Dùng /dongy để nhận hoặc /tuchoi để bỏ qua (tối đa 2 lần).", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                    Component.text("Vật phẩm duy nhất — không thể chế tạo!", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false)
            ));
            meta.setEnchantmentGlintOverride(true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            meta.getPersistentDataContainer().set(awakeningScrollKey, PersistentDataType.BYTE, (byte) 1);
            scroll.setItemMeta(meta);
        }
        return scroll;
    }

    public boolean isAwakeningScroll(ItemStack item) {
        if (item == null || item.getType() != Material.PAPER || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(awakeningScrollKey, PersistentDataType.BYTE);
    }

    public ElementMastery getPlayerElement(Player player) {
        String id = playerElements.get(player.getUniqueId());
        return id != null ? registry.get(id) : null;
    }

    public int getUnlockedStage(Player player) {
        UUID uuid = player.getUniqueId();
        int current = unlockedStages.getOrDefault(uuid, 1);
        int lv = player.getLevel();

        int lv2 = plugin.getConfig().getInt("unlock-levels.skill-2", 30);
        int lv3 = plugin.getConfig().getInt("unlock-levels.skill-3", 60);
        int lv4 = plugin.getConfig().getInt("unlock-levels.skill-4", 100);
        int lv5 = plugin.getConfig().getInt("unlock-levels.skill-5", 150);

        int newStage = current;
        if (lv >= lv5 && newStage < 5) newStage = 5;
        else if (lv >= lv4 && newStage < 4) newStage = 4;
        else if (lv >= lv3 && newStage < 3) newStage = 3;
        else if (lv >= lv2 && newStage < 2) newStage = 2;

        if (newStage > current) {
            unlockedStages.put(uuid, newStage);
            stashConfig.set("players." + uuid + ".unlocked_stage", newStage);
            saveData();

            ElementMastery el = getPlayerElement(player);
            if (el != null) {
                String skillName = el.getSkillNames().get(newStage - 1);
                SkillVisualHelper.playSound(player.getLocation(), "entity.player.levelup", 1.0f, 1.25f);
                player.sendMessage(Component.text("★ Đột phá cảnh giới! Bạn đã mở khóa vĩnh viễn Chiêu " + newStage + ": ", NamedTextColor.GREEN)
                        .append(Component.text(skillName, el.getThemeColor(), TextDecoration.BOLD)));
                if (activeCombatPlayers.contains(uuid)) {
                    equipSkillItems(player, el);
                }
            }
        }
        return newStage;
    }

    // =========================================================================
    // QUAY GIẤY THỨC TỈNH & LỆNH /dongy - /tuchoi - /chonhe
    // =========================================================================

    @EventHandler
    public void onUseScroll(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (!event.getAction().isRightClick()) return;

        Player player = event.getPlayer();
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (!isAwakeningScroll(hand)) return;

        event.setCancelled(true);
        UUID uuid = player.getUniqueId();

        if (playerElements.containsKey(uuid)) {
            ElementMastery cur = getPlayerElement(player);
            player.sendMessage(Component.text("Bạn đã thức tỉnh " + (cur != null ? cur.getDisplayName() : "") + " rồi!", NamedTextColor.RED));
            return;
        }

        if (pendingElements.containsKey(uuid)) {
            ElementMastery pending = registry.get(pendingElements.get(uuid));
            player.sendMessage(Component.text("Bạn đang có " + (pending != null ? pending.getDisplayName() : "")
                    + " chờ xác nhận! Dùng /dongy hoặc /tuchoi trước.", NamedTextColor.YELLOW));
            return;
        }

        Set<String> rejected = rejectedElements.computeIfAbsent(uuid, k -> new HashSet<>());
        List<ElementMastery> pool = new ArrayList<>();
        for (ElementMastery el : registry.getAll()) {
            if (!rejected.contains(el.getId())) {
                pool.add(el);
            }
        }

        if (pool.isEmpty()) {
            player.sendMessage(Component.text("Không còn hệ nào khả dụng để thức tỉnh!", NamedTextColor.RED));
            return;
        }

        if (hand.getAmount() > 1) {
            hand.setAmount(hand.getAmount() - 1);
        } else {
            player.getInventory().setItemInMainHand(null);
        }

        ElementMastery rolled = pool.get(ThreadLocalRandom.current().nextInt(pool.size()));
        pendingElements.put(uuid, rolled.getId());
        stashConfig.set("players." + uuid + ".pending", rolled.getId());
        saveData();

        SkillVisualHelper.playSound(player.getLocation(), "entity.experience_orb.pickup", 1.0f, 1.2f);
        sendAwakeningPrompt(player, rolled);
    }

    private void sendAwakeningPrompt(Player player, ElementMastery el) {
        player.sendMessage(Component.empty());
        player.sendMessage(Component.text("✦ Linh hồn cộng hưởng! Bạn đã cảm ứng được: ", NamedTextColor.GRAY)
                .append(Component.text(el.getDisplayName(), el.getThemeColor(), TextDecoration.BOLD)));
        for (int i = 0; i < 5; i++) {
            player.sendMessage(Component.text("  • Chiêu " + (i + 1) + " (" + el.getSkillNames().get(i) + "): ", NamedTextColor.GOLD)
                    .append(Component.text(el.getSkillDescriptions().get(i), NamedTextColor.AQUA)));
        }

        Component acceptBtn = Component.text("[✔ ĐỒNG Ý (/dongy)]", NamedTextColor.GREEN, TextDecoration.BOLD)
                .clickEvent(ClickEvent.runCommand("/dongy"))
                .hoverEvent(HoverEvent.showText(Component.text("Bấm để nhận vĩnh viễn " + el.getDisplayName())));

        Component declineBtn = Component.text("[✖ TỪ CHỐI (/tuchoi)]", NamedTextColor.RED, TextDecoration.BOLD)
                .clickEvent(ClickEvent.runCommand("/tuchoi"))
                .hoverEvent(HoverEvent.showText(Component.text("Từ chối hệ này (sẽ không quay trúng lại)")));

        player.sendMessage(acceptBtn.append(Component.text("   ")).append(declineBtn));
        player.sendMessage(Component.empty());
    }

    public void acceptPendingElement(Player player) {
        UUID uuid = player.getUniqueId();
        String pendingId = pendingElements.remove(uuid);
        if (pendingId == null) {
            player.sendMessage(Component.text("Bạn không có hệ nào đang chờ xác nhận!", NamedTextColor.RED));
            return;
        }

        ElementMastery el = registry.get(pendingId);
        if (el == null) return;

        playerElements.put(uuid, el.getId());
        stashConfig.set("players." + uuid + ".pending", null);
        stashConfig.set("players." + uuid + ".element", el.getId());
        saveData();

        getUnlockedStage(player);
        SkillVisualHelper.playSound(player.getLocation(), "ui.toast.challenge_complete", 1.0f, 1.1f);
        player.sendMessage(Component.text("★ Chúc mừng! Bạn đã chính thức thức tỉnh ", NamedTextColor.GREEN)
                .append(Component.text(el.getDisplayName(), el.getThemeColor(), TextDecoration.BOLD))
                .append(Component.text(". Gõ /batkinang để chiến đấu!", NamedTextColor.GREEN)));
    }

    public void declinePendingElement(Player player) {
        UUID uuid = player.getUniqueId();
        String pendingId = pendingElements.remove(uuid);
        if (pendingId == null) {
            player.sendMessage(Component.text("Bạn không có hệ nào đang chờ quyết định!", NamedTextColor.RED));
            return;
        }

        ElementMastery declined = registry.get(pendingId);
        Set<String> rejected = rejectedElements.computeIfAbsent(uuid, k -> new HashSet<>());
        rejected.add(pendingId);

        stashConfig.set("players." + uuid + ".pending", null);
        stashConfig.set("players." + uuid + ".rejected", new ArrayList<>(rejected));
        saveData();

        SkillVisualHelper.playSound(player.getLocation(), "entity.item.break", 1.0f, 0.9f);
        player.sendMessage(Component.text("✖ Bạn đã từ chối ", NamedTextColor.RED)
                .append(Component.text(declined != null ? declined.getDisplayName() : pendingId, NamedTextColor.DARK_RED, TextDecoration.BOLD))
                .append(Component.text(". Dùng Giấy Thức Tỉnh còn lại nếu muốn quay tiếp!", NamedTextColor.RED)));
    }

    public void selectElementAdmin(Player player, String elementId) {
        ElementMastery el = registry.get(elementId);
        if (el == null) return;

        UUID uuid = player.getUniqueId();
        pendingElements.remove(uuid);
        playerElements.put(uuid, el.getId());
        stashConfig.set("players." + uuid + ".pending", null);
        stashConfig.set("players." + uuid + ".element", el.getId());
        saveData();

        getUnlockedStage(player);
        if (activeCombatPlayers.contains(uuid)) {
            equipSkillItems(player, el);
        }
    }

    // =========================================================================
    // CHẾ ĐỘ CHIẾN ĐẤU /batkinang (5 Ô KỸ NĂNG HOTBAR SLOT 0..4)
    // =========================================================================

    public void toggleCombatMode(Player player) {
        UUID uuid = player.getUniqueId();
        if (activeCombatPlayers.contains(uuid)) {
            restorePlayerHotbar(player);
            player.sendMessage(Component.text("✔ Đã tắt chế độ kỹ năng và hoàn trả 5 ô vật phẩm!", NamedTextColor.YELLOW));
            return;
        }

        ElementMastery el = getPlayerElement(player);
        if (el == null) {
            player.sendMessage(Component.text("Bạn chưa thức tỉnh hệ nào! Hãy dùng Giấy Thức Tỉnh hoặc /chonhe.", NamedTextColor.RED));
            return;
        }

        PlayerInventory inv = player.getInventory();
        ItemStack[] snapshot = new ItemStack[5];
        for (int i = 0; i < 5; i++) {
            ItemStack item = inv.getItem(i);
            if (item != null && !isSkillItem(item)) {
                snapshot[i] = item.clone();
                stashConfig.set("players." + uuid + ".hotbar." + i, item);
            } else {
                stashConfig.set("players." + uuid + ".hotbar." + i, null);
            }
        }
        saveData();

        savedHotbars.put(uuid, snapshot);
        activeCombatPlayers.add(uuid);
        equipSkillItems(player, el);

        SkillVisualHelper.playSound(player.getLocation(), "item.armor.equip_netherite", 1.0f, 1.1f);
        player.sendMessage(Component.text("⚔ Đã kích hoạt ", NamedTextColor.GREEN)
                .append(Component.text(el.getDisplayName(), el.getThemeColor(), TextDecoration.BOLD))
                .append(Component.text(" — Chuột Phải vào ô 1-5 để tung chiêu!", NamedTextColor.GREEN)));
    }

    private void equipSkillItems(Player player, ElementMastery el) {
        int stage = getUnlockedStage(player);
        int[] unlockReq = {
                1,
                plugin.getConfig().getInt("unlock-levels.skill-2", 30),
                plugin.getConfig().getInt("unlock-levels.skill-3", 60),
                plugin.getConfig().getInt("unlock-levels.skill-4", 100),
                plugin.getConfig().getInt("unlock-levels.skill-5", 150)
        };

        PlayerInventory inv = player.getInventory();
        for (int i = 0; i < 5; i++) {
            boolean unlocked = (i + 1) <= stage;
            ItemStack item = new ItemStack(unlocked ? el.getGuiIcon() : Material.BARRIER);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                String title = el.getSkillNames().get(i);
                if (unlocked) {
                    meta.displayName(Component.text("[" + (i + 1) + "] " + title, el.getThemeColor(), TextDecoration.BOLD)
                            .decoration(TextDecoration.ITALIC, false));
                    meta.lore(List.of(
                            Component.text(el.getSkillDescriptions().get(i), NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false),
                            Component.text("Hồi chiêu: " + el.getCooldownSeconds()[i] + " giây", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                            Component.text("Chuột Phải để thi triển!", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false)
                    ));
                    meta.setEnchantmentGlintOverride(true);
                } else {
                    meta.displayName(Component.text("🔒 Chiêu " + (i + 1) + ": " + title, NamedTextColor.RED, TextDecoration.BOLD)
                            .decoration(TextDecoration.ITALIC, false));
                    meta.lore(List.of(
                            Component.text("Đạt mốc " + unlockReq[i] + " Cấp XP để mở khóa vĩnh viễn!", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
                    ));
                }
                meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
                meta.getPersistentDataContainer().set(skillItemKey, PersistentDataType.BYTE, (byte) 1);
                meta.getPersistentDataContainer().set(skillSlotKey, PersistentDataType.INTEGER, i);
                item.setItemMeta(meta);
            }
            inv.setItem(i, item);
        }
    }

    public void restorePlayerHotbar(Player player) {
        UUID uuid = player.getUniqueId();
        activeCombatPlayers.remove(uuid);

        PlayerInventory inv = player.getInventory();
        for (int i = 0; i < inv.getSize(); i++) {
            if (isSkillItem(inv.getItem(i))) {
                inv.setItem(i, null);
            }
        }

        ItemStack[] snapshot = savedHotbars.remove(uuid);
        if (snapshot != null) {
            for (int i = 0; i < 5; i++) {
                if (snapshot[i] != null) {
                    if (inv.getItem(i) == null || inv.getItem(i).getType().isAir()) {
                        inv.setItem(i, snapshot[i]);
                    } else {
                        inv.addItem(snapshot[i]).values().forEach(rem -> player.getWorld().dropItemNaturally(player.getLocation(), rem));
                    }
                }
            }
        }

        stashConfig.set("players." + uuid + ".hotbar", null);
        saveData();
    }

    public void restoreAllOnlinePlayers() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (activeCombatPlayers.contains(p.getUniqueId())) {
                restorePlayerHotbar(p);
            }
        }
    }

    public boolean isSkillItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(skillItemKey, PersistentDataType.BYTE);
    }

    // =========================================================================
    // KÍCH HOẠT CHIÊU THỨC & BẢO VỆ VẬT PHẨM KỸ NĂNG
    // =========================================================================

    @EventHandler
    public void onCastSkill(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (!event.getAction().isRightClick()) return;

        Player player = event.getPlayer();
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (!isSkillItem(hand)) return;

        event.setCancelled(true);
        Integer slot = hand.getItemMeta().getPersistentDataContainer().get(skillSlotKey, PersistentDataType.INTEGER);
        if (slot == null || slot < 0 || slot > 4) return;

        ElementMastery el = getPlayerElement(player);
        if (el == null) return;

        int stage = getUnlockedStage(player);
        if ((slot + 1) > stage) {
            player.sendMessage(Component.text("Bạn chưa đạt đủ cấp độ XP để mở khóa chiêu này!", NamedTextColor.RED));
            return;
        }

        UUID uuid = player.getUniqueId();
        long[] cd = cooldowns.computeIfAbsent(uuid, k -> new long[5]);
        long now = System.currentTimeMillis();
        if (now < cd[slot]) {
            double rem = (cd[slot] - now) / 1000.0;
            player.sendActionBar(Component.text(String.format("⏳ %s đang hồi: %.1fs", el.getSkillNames().get(slot), rem), NamedTextColor.RED));
            return;
        }

        cd[slot] = now + (el.getCooldownSeconds()[slot] * 1000L);
        el.castSkill(player, slot, this);
    }

    public void tickActionBarAndUnlocks() {
        long now = System.currentTimeMillis();
        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID uuid = player.getUniqueId();
            if (playerElements.containsKey(uuid)) {
                getUnlockedStage(player);
            }
            if (!activeCombatPlayers.contains(uuid)) continue;

            ElementMastery el = getPlayerElement(player);
            if (el == null) continue;

            long[] cd = cooldowns.computeIfAbsent(uuid, k -> new long[5]);
            Component bar = Component.empty();
            for (int i = 0; i < 5; i++) {
                if (i > 0) bar = bar.append(Component.text("  |  ", NamedTextColor.DARK_GRAY));
                long rem = cd[i] - now;
                if (rem <= 0) {
                    bar = bar.append(Component.text("[" + (i + 1) + "] Sẵn sàng", NamedTextColor.GREEN));
                } else {
                    bar = bar.append(Component.text(String.format("[%d] %.1fs", i + 1, rem / 1000.0), NamedTextColor.YELLOW));
                }
            }
            player.sendActionBar(bar);
        }
    }

    // =========================================================================
    // CÁC EVENT BẢO VỆ & CƠ CHẾ HỆ (VÔ HẠN, KIM CƯƠNG, TRIỆU HỒI, NGƯNG ĐỌNG...)
    // =========================================================================

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        loadPlayerData(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        if (activeCombatPlayers.contains(event.getPlayer().getUniqueId())) {
            restorePlayerHotbar(event.getPlayer());
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        if (isSkillItem(event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (isSkillItem(event.getCurrentItem()) || isSkillItem(event.getCursor())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onSwapHand(PlayerSwapHandItemsEvent event) {
        if (isSkillItem(event.getMainHandItem()) || isSkillItem(event.getOffHandItem())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        if (isSkillItem(event.getItemInHand())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        event.getDrops().removeIf(this::isSkillItem);
        if (activeCombatPlayers.contains(event.getEntity().getUniqueId())) {
            restorePlayerHotbar(event.getEntity());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (isHardFrozen(event.getPlayer())) {
            if (event.getFrom().getX() != event.getTo().getX()
                    || event.getFrom().getY() != event.getTo().getY()
                    || event.getFrom().getZ() != event.getTo().getZ()) {
                event.setTo(event.getFrom());
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onMinionTarget(EntityTargetLivingEntityEvent event) {
        UUID ownerId = getMinionOwner(event.getEntity());
        if (ownerId == null || event.getTarget() == null) return;
        if (event.getTarget().getUniqueId().equals(ownerId)) {
            event.setCancelled(true);
            return;
        }
        UUID targetOwner = getMinionOwner(event.getTarget());
        if (ownerId.equals(targetOwner)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        Entity victim = event.getEntity();

        // Miễn nhiễm sát thương rơi sau khi dùng chiêu bay/nhảy
        if (victim instanceof Player p && event.getCause() == EntityDamageEvent.DamageCause.FALL) {
            if (System.currentTimeMillis() < fallImmunityUntil.getOrDefault(p.getUniqueId(), 0L)) {
                event.setCancelled(true);
                return;
            }
        }

        // Chặn đòn đánh từ linh thú triệu hồi lên chủ nhân
        if (event instanceof EntityDamageByEntityEvent byEntity) {
            UUID minionOwner = getMinionOwner(byEntity.getDamager());
            if (minionOwner != null && minionOwner.equals(victim.getUniqueId())) {
                event.setCancelled(true);
                return;
            }

            // Sát thương tùy chỉnh từ đạn Shulker của Hệ Phong
            if (byEntity.getDamager() instanceof ShulkerBullet bullet
                    && bullet.getPersistentDataContainer().has(customBulletKey, PersistentDataType.DOUBLE)) {
                Double dmg = bullet.getPersistentDataContainer().get(customBulletKey, PersistentDataType.DOUBLE);
                if (dmg != null) {
                    event.setDamage(dmg);
                }
            }

            // Vô Hạ Hạn (Gojo) chặn 100% đạn bắn
            if (victim instanceof Player p && hasInfinityShield(p) && byEntity.getDamager() instanceof Projectile) {
                event.setCancelled(true);
                p.getWorld().spawnParticle(Particle.END_ROD, p.getLocation().add(0, 1, 0), 8, 0.4, 0.4, 0.4, 0.02);
                return;
            }

            // Phản sát thương của Hệ Kim Cương
            if (victim instanceof Player p
                    && System.currentTimeMillis() < diamondReflectUntil.getOrDefault(p.getUniqueId(), 0L)
                    && byEntity.getDamager() instanceof LivingEntity attacker) {
                double pct = diamondReflectPercent.getOrDefault(p.getUniqueId(), 0.30);
                attacker.damage(event.getDamage() * pct, p);
            }
        }

        // Cộng dồn sát thương Âm Vang (Hệ Âm Thanh)
        if (victim instanceof LivingEntity living
                && System.currentTimeMillis() < resonanceUntil.getOrDefault(living.getUniqueId(), 0L)) {
            double extra = resonanceBonus.getOrDefault(living.getUniqueId(), 0.0);
            event.setDamage(event.getDamage() + extra);
        }
    }
}
