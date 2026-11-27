package com.elementalskills.managers;

import com.elementalskills.ElementalSkillsPlugin;
import com.elementalskills.elements.ElementRegistry;
import com.elementalskills.elements.ElementalMastery;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.ShulkerBullet;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

public class SkillExecutionManager implements Listener {

    private final ElementalSkillsPlugin plugin;
    private final ElementRegistry registry;
    private final CombatInventoryManager inventoryManager;
    private final NamespacedKey windBulletKey;

    private final Map<UUID, Map<String, Long>> cooldowns = new HashMap<>();
    private final Map<UUID, Long> fallImmuneUntil = new HashMap<>();
    private final Map<UUID, Double> cooldownMultiplier = new HashMap<>();
    private final Set<UUID> shadowEmpowered = new HashSet<>();
    private final Map<Location, Material> temporaryBlocks = new HashMap<>();

    public SkillExecutionManager(ElementalSkillsPlugin plugin, ElementRegistry registry, CombatInventoryManager inventoryManager) {
        this.plugin = plugin;
        this.registry = registry;
        this.inventoryManager = inventoryManager;
        this.windBulletKey = new NamespacedKey(plugin, "es_wind_bullet");
    }

    public ElementalSkillsPlugin getPlugin() {
        return plugin;
    }

    public CombatInventoryManager getInventoryManager() {
        return inventoryManager;
    }

    public NamespacedKey getWindBulletKey() {
        return windBulletKey;
    }

    public void setCooldownMultiplier(Player player, double multiplier) {
        cooldownMultiplier.put(player.getUniqueId(), multiplier);
    }

    public void grantFallImmunity(Player player, int seconds) {
        fallImmuneUntil.put(player.getUniqueId(), System.currentTimeMillis() + (seconds * 1000L));
    }

    public boolean hasFallImmunity(Player player) {
        return System.currentTimeMillis() < fallImmuneUntil.getOrDefault(player.getUniqueId(), 0L);
    }

    public void setShadowEmpowered(Player player, boolean active) {
        if (active) shadowEmpowered.add(player.getUniqueId());
        else shadowEmpowered.remove(player.getUniqueId());
    }

    public boolean consumeShadowEmpowerment(Player player) {
        return shadowEmpowered.remove(player.getUniqueId());
    }

    public void placeTemporaryBlock(Block block, Material newMat, int durationTicks) {
        if (block.getType() != Material.AIR) return;
        Location loc = block.getLocation();
        temporaryBlocks.putIfAbsent(loc, Material.AIR);
        block.setType(newMat, false);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Material original = temporaryBlocks.remove(loc);
            if (original != null && loc.getBlock().getType() == newMat) {
                loc.getBlock().setType(original, false);
            }
        }, durationTicks);
    }

    public void cleanupAllTemporaryBlocks() {
        for (Map.Entry<Location, Material> entry : temporaryBlocks.entrySet()) {
            entry.getKey().getBlock().setType(entry.getValue(), false);
        }
        temporaryBlocks.clear();
    }

    public void tickEnvironments() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            cooldownMultiplier.put(player.getUniqueId(), 1.0);
            if (!inventoryManager.isCombatActive(player)) continue;

            ElementalMastery element = inventoryManager.getChosenElement(player);
            if (element != null) {
                element.tickEnvironment(player, this);
            }
        }
    }

    public double calculateFinalSkillDamage(Player caster, LivingEntity target, double baseDamage) {
        double dmg = baseDamage;
        ElementalMastery casterEl = inventoryManager.getChosenElement(caster);
        if (casterEl != null) {
            dmg = casterEl.modifyOutgoingSkillDamage(caster, target, dmg, this);
        }

        // Khắc chế Quang <-> Ám
        if (target instanceof Player targetPlayer && inventoryManager.isCombatActive(targetPlayer)) {
            ElementalMastery targetEl = inventoryManager.getChosenElement(targetPlayer);
            if (casterEl != null && targetEl != null) {
                if (casterEl.getId().equals("quang") && targetEl.getId().equals("am")) {
                    dmg *= 1.35;
                }
            }
        }
        return dmg;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (!event.getAction().isRightClick()) return;

        Player player = event.getPlayer();
        ItemStack hand = player.getInventory().getItemInMainHand();
        int skillSlot = inventoryManager.getSkillSlotFromItem(hand);
        if (skillSlot < 1 || skillSlot > 3) return;

        // Chặn hành vi vanilla của item (ném Fire Charge, Wind Charge, đặt Blue Ice/End Rod...)
        event.setCancelled(true);

        if (!inventoryManager.isCombatActive(player)) {
            player.sendMessage(Component.text("Hãy gõ /batkinang để kích hoạt trạng thái chiến đấu!", NamedTextColor.RED));
            return;
        }

        if (skillSlot > inventoryManager.getUnlockedSkillTier(player)) {
            player.sendMessage(Component.text("Chiêu này chưa được mở khóa!", NamedTextColor.RED));
            return;
        }

        ElementalMastery element = inventoryManager.getChosenElement(player);
        if (element == null) return;

        String cdKey = element.getId() + "_" + skillSlot;
        long now = System.currentTimeMillis();
        Map<String, Long> playerCd = cooldowns.computeIfAbsent(player.getUniqueId(), k -> new HashMap<>());
        long readyAt = playerCd.getOrDefault(cdKey, 0L);

        if (now < readyAt) {
            double remain = (readyAt - now) / 1000.0;
            playSoundSafe(player, "entity.villager.no", 1.2f);
            player.sendActionBar(Component.text("⏳ " + element.getSkillName(skillSlot) + " đang hồi: " + String.format("%.1fs", remain), NamedTextColor.RED));
            return;
        }

        double mult = cooldownMultiplier.getOrDefault(player.getUniqueId(), 1.0);
        long cdMillis = Math.round(element.getSkillCooldown(skillSlot) * 1000.0 * mult);
        playerCd.put(cdKey, now + cdMillis);

        switch (skillSlot) {
            case 1 -> element.castSkill1(player, this);
            case 2 -> element.castSkill2(player, this);
            case 3 -> element.castSkill3(player, this);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;

        if (event.getCause() == EntityDamageEvent.DamageCause.FALL && hasFallImmunity(victim)) {
            event.setCancelled(true);
            return;
        }

        if (!inventoryManager.isCombatActive(victim)) return;
        ElementalMastery element = inventoryManager.getChosenElement(victim);
        if (element != null) {
            element.modifyIncomingDamage(victim, event, this);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        // Xử lý sát thương đạn Shulker của Hệ Phong Chiêu 1
        if (event.getDamager() instanceof ShulkerBullet bullet
                && bullet.getPersistentDataContainer().has(windBulletKey, PersistentDataType.BYTE)
                && bullet.getShooter() instanceof Player caster
                && event.getEntity() instanceof LivingEntity target) {
            double dmg = calculateFinalSkillDamage(caster, target, 3.0);
            event.setDamage(dmg);
            target.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 25, 0));
            return;
        }

        // Bạo kích đòn đánh thường từ Ẩn Thân Bóng Đêm (Hệ Ám)
        if (event.getDamager() instanceof Player attacker && consumeShadowEmpowerment(attacker)) {
            event.setDamage(event.getDamage() * 1.5);
            attacker.getWorld().spawnParticle(Particle.SMOKE, event.getEntity().getLocation().add(0, 1, 0), 20, 0.4, 0.5, 0.4, 0.05);
            attacker.sendActionBar(Component.text("🌑 Ám Kích Bạo Phát (+50% Sát Thương)!", NamedTextColor.DARK_PURPLE));
        }
    }

    public static void playSoundSafe(Player player, String key, float pitch) {
        Sound sound = Registry.SOUNDS.get(NamespacedKey.minecraft(key));
        if (sound != null) {
            player.playSound(player.getLocation(), sound, 1.0f, pitch);
        }
    }
}
