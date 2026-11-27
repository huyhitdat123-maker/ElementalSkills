package com.elementalskills.managers;

import com.elementalskills.ElementalSkillsPlugin;
import com.elementalskills.elements.ElementRegistry;
import com.elementalskills.elements.ElementalMastery;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class CombatInventoryManager implements Listener {

    private final ElementalSkillsPlugin plugin;
    private final ElementRegistry registry;
    private final NamespacedKey skillSlotKey;
    private final NamespacedKey skillElementKey;
    private final NamespacedKey awakeningPaperKey;

    private final File storageFile;
    private final YamlConfiguration storageConfig;

    private final Map<UUID, String> chosenElements = new HashMap<>();
    private final Map<UUID, String> pendingElements = new HashMap<>();
    private final Map<UUID, Set<String>> rejectedElements = new HashMap<>();
    private final Map<UUID, Set<Material>> rejectedRepresentativeMaterials = new HashMap<>();
    private final Map<UUID, Integer> unlockedSkillTiers = new HashMap<>();
    private final Set<UUID> activeCombatPlayers = new HashSet<>();

    public CombatInventoryManager(ElementalSkillsPlugin plugin, ElementRegistry registry) {
        this.plugin = plugin;
        this.registry = registry;
        this.skillSlotKey = new NamespacedKey(plugin, "es_skill_slot");
        this.skillElementKey = new NamespacedKey(plugin, "es_skill_element");
        this.awakeningPaperKey = new NamespacedKey(plugin, "es_awakening_paper");
        this.storageFile = new File(plugin.getDataFolder(), "combat_stash.yml");
        this.storageConfig = YamlConfiguration.loadConfiguration(storageFile);

        for (Player online : Bukkit.getOnlinePlayers()) {
            loadPlayerRecord(online);
        }
    }

    private int getSkill2RequiredLevel() {
        return plugin.getConfig().getInt("unlock-levels.skill-2", 50);
    }

    private int getSkill3RequiredLevel() {
        return plugin.getConfig().getInt("unlock-levels.skill-3", 100);
    }

    private void loadPlayerRecord(Player player) {
        UUID uuid = player.getUniqueId();
        String basePath = "players." + uuid;

        String savedElement = storageConfig.getString(basePath + ".element");
        if (savedElement != null && registry.get(savedElement) != null) {
            chosenElements.put(uuid, savedElement.toLowerCase());
        }

        String savedPending = storageConfig.getString(basePath + ".pending");
        if (savedPending != null && registry.get(savedPending) != null) {
            pendingElements.put(uuid, savedPending.toLowerCase());
        }

        List<String> rejectedList = storageConfig.getStringList(basePath + ".rejected");
        Set<String> rejSet = new HashSet<>(rejectedList);
        rejectedElements.put(uuid, rejSet);

        Set<Material> rejMats = EnumSet.noneOf(Material.class);
        for (String rejId : rejSet) {
            ElementalMastery el = registry.get(rejId);
            if (el != null) {
                rejMats.add(el.getIcon());
            }
        }
        rejectedRepresentativeMaterials.put(uuid, rejMats);

        int savedTier = storageConfig.getInt(basePath + ".unlocked_tier", 1);
        unlockedSkillTiers.put(uuid, Math.max(1, Math.min(3, savedTier)));

        // Cấp Giấy Thức Tỉnh duy nhất 1 lần khi người chơi vào server lần đầu
        if (!storageConfig.getBoolean(basePath + ".starter_given", false)) {
            storageConfig.set(basePath + ".starter_given", true);
            saveStorage();

            int starterCount = Math.max(1, plugin.getConfig().getInt("starter-papers", 2));
            ItemStack starterPapers = createAwakeningPaper(starterCount);
            Map<Integer, ItemStack> rem = player.getInventory().addItem(starterPapers);
            rem.values().forEach(r -> player.getWorld().dropItemNaturally(player.getLocation(), r));

            player.sendMessage(Component.text("✦ Chào mừng! Bạn nhận được ", NamedTextColor.GOLD)
                    .append(Component.text(starterCount + " Giấy Thức Tỉnh Nguyên Tố", NamedTextColor.YELLOW, TextDecoration.BOLD))
                    .append(Component.text(". Chuột phải vào giấy để quay hệ (từ chối sẽ mất vĩnh viễn hệ & vật phẩm đại diện đó)!", NamedTextColor.GOLD)));
        } else if (pendingElements.containsKey(uuid)) {
            ElementalMastery pendingEl = registry.get(pendingElements.get(uuid));
            if (pendingEl != null) {
                sendAcceptancePrompt(player, pendingEl);
            }
        }

        // Khôi phục túi đồ nếu server tắt đột ngột lúc đang bật kỹ năng
        if (storageConfig.getBoolean(basePath + ".active", false)) {
            restoreStashToPlayer(player, false);
        }
    }

    private void saveStorage() {
        try {
            storageConfig.save(storageFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Loi luu combat_stash.yml: " + e.getMessage());
        }
    }

    public ItemStack createAwakeningPaper(int amount) {
        ItemStack paper = new ItemStack(Material.PAPER, Math.max(1, amount));
        ItemMeta meta = paper.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("✦ Giấy Thức Tỉnh Nguyên Tố ✦", NamedTextColor.GOLD, TextDecoration.BOLD)
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(
                    Component.text("Chuột Phải để quay ngẫu nhiên 1 trong 8 Hệ Nguyên Tố.", NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false),
                    Component.text("Sau khi quay, dùng /dongy để nhận hoặc /tuchoi để bỏ.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                    Component.empty(),
                    Component.text("⚠ Cảnh báo: Nếu /tuchoi, nguyên tố và vật phẩm", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false),
                    Component.text("đại diện của hệ đó sẽ bị hủy bỏ vĩnh viễn!", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false)
            ));
            meta.setEnchantmentGlintOverride(true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            meta.getPersistentDataContainer().set(awakeningPaperKey, PersistentDataType.BYTE, (byte) 1);
            paper.setItemMeta(meta);
        }
        return paper;
    }

    public boolean isAwakeningPaper(ItemStack item) {
        if (item == null || item.getType() != Material.PAPER || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(awakeningPaperKey, PersistentDataType.BYTE);
    }

    // Dùng cho lệnh Admin /chonhe
    public void setChosenElement(Player player, String elementId) {
        ElementalMastery el = registry.get(elementId);
        if (el == null) return;

        if (isCombatActive(player)) {
            disableCombatMode(player, false);
        }

        UUID uuid = player.getUniqueId();
        pendingElements.remove(uuid);
        chosenElements.put(uuid, el.getId());

        int tier = computeTierFromLevel(player.getLevel(), 1);
        unlockedSkillTiers.put(uuid, tier);

        String basePath = "players." + uuid;
        storageConfig.set(basePath + ".pending", null);
        storageConfig.set(basePath + ".element", el.getId());
        storageConfig.set(basePath + ".unlocked_tier", tier);
        saveStorage();
    }

    public ElementalMastery getChosenElement(Player player) {
        String id = chosenElements.get(player.getUniqueId());
        return id != null ? registry.get(id) : null;
    }

    public int getUnlockedSkillTier(Player player) {
        return unlockedSkillTiers.getOrDefault(player.getUniqueId(), 1);
    }

    private int computeTierFromLevel(int level, int currentTier) {
        int tier = Math.max(1, currentTier);
        if (level >= getSkill2RequiredLevel()) tier = Math.max(tier, 2);
        if (level >= getSkill3RequiredLevel()) tier = Math.max(tier, 3);
        return tier;
    }

    // =========================================================================
    // QUAY GIẤY THỨC TỈNH & XỬ LÝ /dongy - /tuchoi
    // =========================================================================

    @EventHandler
    public void onUseAwakeningPaper(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (!event.getAction().isRightClick()) return;

        Player player = event.getPlayer();
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (!isAwakeningPaper(hand)) return;

        event.setCancelled(true);
        UUID uuid = player.getUniqueId();

        if (chosenElements.containsKey(uuid)) {
            ElementalMastery cur = getChosenElement(player);
            player.sendMessage(Component.text("Bạn đã sở hữu ân huệ của Hệ " + (cur != null ? cur.getDisplayName() : "") + " rồi!", NamedTextColor.RED));
            return;
        }

        String existingPendingId = pendingElements.get(uuid);
        if (existingPendingId != null) {
            ElementalMastery pendingEl = registry.get(existingPendingId);
            player.sendMessage(Component.text("Bạn đang có Hệ " + (pendingEl != null ? pendingEl.getDisplayName() : "") + " chờ xác nhận! Hãy dùng /dongy hoặc /tuchoi trước.", NamedTextColor.YELLOW));
            return;
        }

        Set<String> rejected = rejectedElements.computeIfAbsent(uuid, k -> new HashSet<>());
        List<ElementalMastery> pool = new ArrayList<>();
        for (ElementalMastery el : registry.getAll()) {
            if (!rejected.contains(el.getId())) {
                pool.add(el);
            }
        }

        if (pool.isEmpty()) {
            player.sendMessage(Component.text("Bạn đã từ chối toàn bộ nguyên tố rồi, không còn hệ nào để thức tỉnh!", NamedTextColor.RED));
            return;
        }

        // Tiêu hao ngay 1 tờ giấy
        if (hand.getAmount() > 1) {
            hand.setAmount(hand.getAmount() - 1);
        } else {
            player.getInventory().setItemInMainHand(null);
        }

        ElementalMastery rolled = pool.get(ThreadLocalRandom.current().nextInt(pool.size()));
        pendingElements.put(uuid, rolled.getId());
        storageConfig.set("players." + uuid + ".pending", rolled.getId());
        saveStorage();

        SkillExecutionManager.playSoundSafe(player, "entity.player.levelup", 1.2f);
        sendAcceptancePrompt(player, rolled);
    }

    private void sendAcceptancePrompt(Player player, ElementalMastery element) {
        player.sendMessage(Component.empty());
        player.sendMessage(Component.text("✦ Vật phẩm đại diện xuất hiện: ", NamedTextColor.GRAY)
                .append(Component.text(element.getIcon().name(), NamedTextColor.AQUA)));
        player.sendMessage(Component.text("Bạn có muốn nhận ân huệ của ", NamedTextColor.GOLD)
                .append(Component.text("Hệ " + element.getDisplayName(), element.getThemeColor(), TextDecoration.BOLD))
                .append(Component.text(" không?", NamedTextColor.GOLD)));

        Component acceptBtn = Component.text("[✔ ĐỒNG Ý (/dongy)]", NamedTextColor.GREEN, TextDecoration.BOLD)
                .clickEvent(ClickEvent.runCommand("/dongy"))
                .hoverEvent(HoverEvent.showText(Component.text("Bấm để nhận Hệ " + element.getDisplayName() + " và mở khóa Chiêu 1")));

        Component declineBtn = Component.text("[✖ TỪ CHỐI (/tuchoi)]", NamedTextColor.RED, TextDecoration.BOLD)
                .clickEvent(ClickEvent.runCommand("/tuchoi"))
                .hoverEvent(HoverEvent.showText(Component.text("Cảnh báo: Từ chối sẽ làm mất vĩnh viễn Hệ " + element.getDisplayName() + " & vật phẩm đại diện!")));

        player.sendMessage(acceptBtn.append(Component.text("    ")).append(declineBtn));
        player.sendMessage(Component.empty());
    }

    public void acceptPendingElement(Player player) {
        UUID uuid = player.getUniqueId();
        String pendingId = pendingElements.remove(uuid);
        if (pendingId == null) {
            player.sendMessage(Component.text("Bạn không có nguyên tố nào đang chờ xác nhận!", NamedTextColor.RED));
            return;
        }

        ElementalMastery element = registry.get(pendingId);
        if (element == null) return;

        chosenElements.put(uuid, element.getId());
        int unlockedTier = computeTierFromLevel(player.getLevel(), 1);
        unlockedSkillTiers.put(uuid, unlockedTier);

        String basePath = "players." + uuid;
        storageConfig.set(basePath + ".pending", null);
        storageConfig.set(basePath + ".element", element.getId());
        storageConfig.set(basePath + ".unlocked_tier", unlockedTier);
        saveStorage();

        SkillExecutionManager.playSoundSafe(player, "ui.toast.challenge_complete", 1.1f);
        player.sendMessage(Component.text("★ Chúc mừng! Bạn đã tiếp nhận ân huệ của ", NamedTextColor.GREEN)
                .append(Component.text("Hệ " + element.getDisplayName(), element.getThemeColor(), TextDecoration.BOLD))
                .append(Component.text(" và mở khóa ", NamedTextColor.GREEN))
                .append(Component.text(element.getSkillName(1), NamedTextColor.YELLOW, TextDecoration.BOLD))
                .append(Component.text("!", NamedTextColor.GREEN)));

        if (unlockedTier >= 2) {
            player.sendMessage(Component.text("✦ Nhờ đạt mốc Cấp XP, bạn đã mở khóa sẵn tới Chiêu " + unlockedTier + "!", NamedTextColor.AQUA));
        } else {
            player.sendMessage(Component.text("▶ Đạt mốc " + getSkill2RequiredLevel() + " Cấp XP để mở vĩnh viễn Chiêu 2, "
                    + getSkill3RequiredLevel() + " Cấp XP để mở vĩnh viễn Chiêu 3.", NamedTextColor.GRAY));
        }
    }

    public void declinePendingElement(Player player) {
        UUID uuid = player.getUniqueId();
        String pendingId = pendingElements.remove(uuid);
        if (pendingId == null) {
            player.sendMessage(Component.text("Bạn không có nguyên tố nào đang chờ xác nhận!", NamedTextColor.RED));
            return;
        }

        ElementalMastery element = registry.get(pendingId);
        Set<String> rejected = rejectedElements.computeIfAbsent(uuid, k -> new HashSet<>());
        rejected.add(pendingId);

        if (element != null) {
            rejectedRepresentativeMaterials.computeIfAbsent(uuid, k -> EnumSet.noneOf(Material.class))
                    .add(element.getIcon());

            // Hủy bỏ vật phẩm đại diện của nguyên tố đó nếu người chơi đang giữ trong túi
            if (plugin.getConfig().getBoolean("destroy-rejected-representative-item", true)) {
                player.getInventory().remove(element.getIcon());
                if (player.getInventory().getItemInOffHand().getType() == element.getIcon()) {
                    player.getInventory().setItemInOffHand(null);
                }
            }
        }

        String basePath = "players." + uuid;
        storageConfig.set(basePath + ".pending", null);
        storageConfig.set(basePath + ".rejected", new ArrayList<>(rejected));
        saveStorage();

        SkillExecutionManager.playSoundSafe(player, "entity.item.break", 0.9f);
        String elName = element != null ? element.getDisplayName() : pendingId;
        String iconName = element != null ? element.getIcon().name() : "ITEM";

        player.sendMessage(Component.text("✖ Bạn đã từ chối ", NamedTextColor.RED)
                .append(Component.text("Hệ " + elName, NamedTextColor.DARK_RED, TextDecoration.BOLD))
                .append(Component.text("! Nguyên tố này cùng vật phẩm đại diện (", NamedTextColor.RED))
                .append(Component.text(iconName, NamedTextColor.GRAY))
                .append(Component.text(") đã bị mất vĩnh viễn.", NamedTextColor.RED)));
    }

    @EventHandler(ignoreCancelled = true)
    public void onPickupRejectedItem(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!plugin.getConfig().getBoolean("destroy-rejected-representative-item", true)) return;

        Material mat = event.getItem().getItemStack().getType();
        Set<Material> bannedMats = rejectedRepresentativeMaterials.get(player.getUniqueId());
        if (bannedMats != null && bannedMats.contains(mat)) {
            event.setCancelled(true);
            event.getItem().remove();
            player.getWorld().spawnParticle(Particle.SMOKE, event.getItem().getLocation(), 15, 0.2, 0.2, 0.2, 0.03);
            SkillExecutionManager.playSoundSafe(player, "entity.generic.extinguish_fire", 1.2f);
            player.sendActionBar(Component.text("✖ Vật phẩm đại diện của nguyên tố bạn đã từ chối nên đã tan biến!", NamedTextColor.RED));
        }
    }

    // =========================================================================
    // MỞ KHÓA VĨNH VIỄN CHIÊU 2 (CẤP 50) & CHIÊU 3 (CẤP 100)
    // =========================================================================

    @EventHandler
    public void onPlayerLevelChange(PlayerLevelChangeEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        ElementalMastery element = getChosenElement(player);
        if (element == null) return;

        int oldTier = getUnlockedSkillTier(player);
        int newTier = computeTierFromLevel(event.getNewLevel(), oldTier);
        if (newTier <= oldTier) return;

        unlockedSkillTiers.put(uuid, newTier);
        storageConfig.set("players." + uuid + ".unlocked_tier", newTier);
        saveStorage();

        SkillExecutionManager.playSoundSafe(player, "ui.toast.challenge_complete", 1.2f);
        for (int t = oldTier + 1; t <= newTier; t++) {
            player.sendMessage(Component.text("★ Đột phá cảnh giới! Bạn đã mở khóa VĨNH VIỄN ", NamedTextColor.GOLD)
                    .append(Component.text("Chiêu " + t + ": " + element.getSkillName(t), element.getThemeColor(), TextDecoration.BOLD))
                    .append(Component.text(" (không mất khi tiêu XP)!", NamedTextColor.GOLD)));

            if (isCombatActive(player)) {
                int emptySlot = findFirstEmptyMainSlot(player.getInventory());
                if (emptySlot != -1) {
                    player.getInventory().setItem(emptySlot, createSkillItem(element, t));
                }
            }
        }
    }

    private int findFirstEmptyMainSlot(PlayerInventory inv) {
        for (int i = 0; i <= 35; i++) {
            ItemStack cur = inv.getItem(i);
            if (cur == null || cur.getType().isAir()) {
                return i;
            }
        }
        return -1;
    }

    // =========================================================================
    // BẬT / TẮT KHO CHIẾN ĐẤU (/batkinang & /tatkinang)
    // =========================================================================

    public boolean isCombatActive(Player player) {
        return activeCombatPlayers.contains(player.getUniqueId());
    }

    public int getSkillSlotFromItem(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) return -1;
        Integer slot = item.getItemMeta().getPersistentDataContainer().get(skillSlotKey, PersistentDataType.INTEGER);
        return slot != null ? slot : -1;
    }

    public boolean isSkillItem(ItemStack item) {
        return getSkillSlotFromItem(item) > 0;
    }

    // Chỉ giữ lại Vũ khí, Công cụ và Đồ ăn
    public boolean isCombatAllowedItem(ItemStack item) {
        if (item == null || item.getType().isAir()) return false;
        Material mat = item.getType();
        if (mat.isEdible()) return true;

        String name = mat.name();
        return name.endsWith("_SWORD")
                || name.endsWith("_AXE")
                || name.endsWith("_PICKAXE")
                || name.endsWith("_SHOVEL")
                || name.endsWith("_HOE")
                || mat == Material.BOW
                || mat == Material.CROSSBOW
                || mat == Material.TRIDENT
                || mat == Material.MACE
                || mat == Material.SHIELD
                || mat == Material.FLINT_AND_STEEL
                || mat == Material.SHEARS
                || mat == Material.FISHING_ROD
                || mat == Material.BUCKET
                || mat == Material.WATER_BUCKET
                || mat == Material.LAVA_BUCKET;
    }

    public void enableCombatMode(Player player) {
        if (isCombatActive(player)) {
            player.sendMessage(Component.text("Bạn đang ở trong trạng thái kích hoạt kỹ năng rồi!", NamedTextColor.YELLOW));
            return;
        }

        ElementalMastery element = getChosenElement(player);
        if (element == null) {
            player.sendMessage(Component.text("Bạn chưa thức tỉnh nguyên tố nào! Hãy dùng Giấy Thức Tỉnh trước.", NamedTextColor.RED));
            return;
        }

        int unlockedTier = getUnlockedSkillTier(player);
        UUID uuid = player.getUniqueId();
        PlayerInventory inv = player.getInventory();
        String basePath = "players." + uuid;

        storageConfig.set(basePath + ".stash", null);
        for (int i = 0; i <= 35; i++) {
            ItemStack item = inv.getItem(i);
            if (item != null && !item.getType().isAir() && !isCombatAllowedItem(item)) {
                storageConfig.set(basePath + ".stash." + i, item.clone());
                inv.setItem(i, null);
            }
        }
        ItemStack offhand = inv.getItemInOffHand();
        if (!offhand.getType().isAir() && !isCombatAllowedItem(offhand)) {
            storageConfig.set(basePath + ".stash.40", offhand.clone());
            inv.setItemInOffHand(null);
        }

        List<Integer> emptySlots = new ArrayList<>();
        for (int i = 0; i <= 35; i++) {
            ItemStack cur = inv.getItem(i);
            if (cur == null || cur.getType().isAir()) {
                emptySlots.add(i);
            }
        }

        if (emptySlots.size() < unlockedTier) {
            restoreStashFromConfigOnly(player);
            player.sendMessage(Component.text("Túi đồ đang chứa quá nhiều vũ khí/đồ ăn, cần trống ít nhất " + unlockedTier + " ô để chứa vật phẩm kỹ năng!", NamedTextColor.RED));
            return;
        }

        for (int skillNum = 1; skillNum <= unlockedTier; skillNum++) {
            int targetSlot = emptySlots.get(skillNum - 1);
            inv.setItem(targetSlot, createSkillItem(element, skillNum));
        }

        activeCombatPlayers.add(uuid);
        storageConfig.set(basePath + ".active", true);
        saveStorage();

        SkillExecutionManager.playSoundSafe(player, "block.beacon.activate", 1.3f);
        player.sendMessage(Component.text("⚔ Đã BẬT kỹ năng Hệ ", NamedTextColor.GREEN)
                .append(Component.text(element.getDisplayName(), element.getThemeColor(), TextDecoration.BOLD))
                .append(Component.text(" (" + unlockedTier + "/3 chiêu đã mở)! Chuột phải vật phẩm kỹ năng để thi triển.", NamedTextColor.GREEN)));
    }

    public void disableCombatMode(Player player, boolean waterExtinguished) {
        if (!isCombatActive(player)) {
            if (!waterExtinguished) {
                player.sendMessage(Component.text("Bạn chưa bật trạng thái kỹ năng!", NamedTextColor.YELLOW));
            }
            return;
        }

        restoreStashToPlayer(player, true);

        if (waterExtinguished) {
            SkillExecutionManager.playSoundSafe(player, "entity.generic.extinguish_fire", 1.0f);
            player.sendMessage(Component.text("💧 Rơi xuống nước khiến Hỏa Lực tắt ngấm! Túi đồ gốc đã được trả lại.", NamedTextColor.RED));
        } else {
            SkillExecutionManager.playSoundSafe(player, "block.beacon.deactivate", 1.1f);
            player.sendMessage(Component.text("✔ Đã TẮT kỹ năng và hoàn trả toàn bộ vật phẩm từ kho tạm!", NamedTextColor.AQUA));
        }
    }

    private void restoreStashToPlayer(Player player, boolean saveToDisk) {
        UUID uuid = player.getUniqueId();
        PlayerInventory inv = player.getInventory();

        for (int i = 0; i <= 35; i++) {
            if (isSkillItem(inv.getItem(i))) {
                inv.setItem(i, null);
            }
        }
        if (isSkillItem(inv.getItemInOffHand())) {
            inv.setItemInOffHand(null);
        }

        restoreStashFromConfigOnly(player);

        activeCombatPlayers.remove(uuid);
        storageConfig.set("players." + uuid + ".active", false);
        storageConfig.set("players." + uuid + ".stash", null);
        if (saveToDisk) {
            saveStorage();
        }
    }

    private void restoreStashFromConfigOnly(Player player) {
        UUID uuid = player.getUniqueId();
        PlayerInventory inv = player.getInventory();
        ConfigurationSection sec = storageConfig.getConfigurationSection("players." + uuid + ".stash");
        if (sec == null) return;

        for (String key : sec.getKeys(false)) {
            ItemStack item = sec.getItemStack(key);
            if (item == null || item.getType().isAir()) continue;

            int slot = Integer.parseInt(key);
            if (slot == 40) {
                if (inv.getItemInOffHand().getType().isAir()) {
                    inv.setItemInOffHand(item);
                } else {
                    inv.addItem(item).values().forEach(rem -> player.getWorld().dropItemNaturally(player.getLocation(), rem));
                }
            } else if (slot >= 0 && slot <= 35) {
                ItemStack existing = inv.getItem(slot);
                if (existing == null || existing.getType().isAir()) {
                    inv.setItem(slot, item);
                } else {
                    inv.addItem(item).values().forEach(rem -> player.getWorld().dropItemNaturally(player.getLocation(), rem));
                }
            }
        }
    }

    public void restoreAllOnlinePlayers() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (isCombatActive(p)) {
                restoreStashToPlayer(p, false);
            }
        }
        saveStorage();
    }

    private ItemStack createSkillItem(ElementalMastery element, int skillNum) {
        ItemStack item = new ItemStack(element.getIcon());
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("[Chiêu " + skillNum + "] " + element.getSkillName(skillNum),
                    element.getThemeColor(), TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false));

            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("Hệ: " + element.getDisplayName() + " | Hồi chiêu: " + element.getSkillCooldown(skillNum) + "s", NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false));
            for (String line : element.getSkillLore(skillNum)) {
                lore.add(Component.text(line, NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false));
            }
            lore.add(Component.empty());
            lore.add(Component.text("▶ Bấm Chuột Phải để kích hoạt!", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("▶ Gõ /tatkinang để thu hồi & trả lại túi đồ", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));

            meta.lore(lore);
            meta.setEnchantmentGlintOverride(true);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);

            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            pdc.set(skillSlotKey, PersistentDataType.INTEGER, skillNum);
            pdc.set(skillElementKey, PersistentDataType.STRING, element.getId());
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        loadPlayerRecord(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (isCombatActive(player)) {
            restoreStashToPlayer(player, true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (!isCombatActive(player)) return;

        event.getDrops().removeIf(this::isSkillItem);

        UUID uuid = player.getUniqueId();
        ConfigurationSection sec = storageConfig.getConfigurationSection("players." + uuid + ".stash");
        if (sec != null) {
            for (String key : sec.getKeys(false)) {
                ItemStack stashed = sec.getItemStack(key);
                if (stashed != null && !stashed.getType().isAir()) {
                    if (event.getKeepInventory()) {
                        player.getInventory().addItem(stashed);
                    } else {
                        event.getDrops().add(stashed);
                    }
                }
            }
        }

        activeCombatPlayers.remove(uuid);
        storageConfig.set("players." + uuid + ".active", false);
        storageConfig.set("players." + uuid + ".stash", null);
        saveStorage();
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        if (isSkillItem(event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
            event.getPlayer().sendActionBar(Component.text("✖ Không thể vứt vật phẩm kỹ năng! Dùng /tatkinang để tắt.", NamedTextColor.RED));
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (isSkillItem(event.getItemInHand())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!isCombatActive(player)) return;

        ItemStack current = event.getCurrentItem();
        ItemStack cursor = event.getCursor();

        if (event.getView().getTopInventory().getType() != InventoryType.CRAFTING) {
            if (event.getClickedInventory() == player.getInventory() && event.isShiftClick() && isSkillItem(current)) {
                event.setCancelled(true);
                return;
            }
            if (event.getClickedInventory() != player.getInventory() && isSkillItem(cursor)) {
                event.setCancelled(true);
                return;
            }
            if (event.getClick() == ClickType.NUMBER_KEY && event.getClickedInventory() != player.getInventory()) {
                ItemStack hotbar = player.getInventory().getItem(event.getHotbarButton());
                if (isSkillItem(hotbar)) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!isCombatActive(player)) return;
        if (isSkillItem(event.getOldCursor()) && event.getView().getTopInventory().getType() != InventoryType.CRAFTING) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onSwapHand(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        if (!player.isSneaking()) return;
        event.setCancelled(true);
        if (isCombatActive(player)) {
            disableCombatMode(player, false);
        } else {
            enableCombatMode(player);
        }
    }
}
