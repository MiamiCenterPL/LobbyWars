package net.miamicenter.lobbywars;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

public class ItemManager {
    private final LobbyWars plugin = LobbyWars.getPlugin(LobbyWars.class);
    private final NamespacedKey nonDroppableKey = CustomAttributes.NON_DROPPABLE.getNamespacedKey(plugin);
    private final NamespacedKey nonMovableKey = CustomAttributes.NON_MOVABLE.getNamespacedKey(plugin);
    private final NamespacedKey triggerPvp = CustomAttributes.TRIGGER_PVP.getNamespacedKey(plugin);

    public boolean blockItemDrop(ItemMeta meta){
        if (meta != null) {
            PersistentDataContainer container = meta.getPersistentDataContainer();
            if (container.has(nonDroppableKey, PersistentDataType.BOOLEAN)) return container.get(nonDroppableKey, PersistentDataType.BOOLEAN);
        }
        return false;
    }
    public boolean blockItemMove(ItemStack item){
        if (item == null) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            PersistentDataContainer container = meta.getPersistentDataContainer();
            return container.has(nonMovableKey, PersistentDataType.BOOLEAN) && container.get(nonMovableKey, PersistentDataType.BOOLEAN);
        }
        return false;
    }
    public boolean canTriggerPvp(ItemStack item) {
        if (item == null) return false;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;

        NamespacedKey triggerPvpKey = CustomAttributes.TRIGGER_PVP.getNamespacedKey(plugin);
        if (meta.getPersistentDataContainer().has(triggerPvpKey, PersistentDataType.BOOLEAN)) {
            return meta.getPersistentDataContainer().get(triggerPvpKey, PersistentDataType.BOOLEAN);
        }
        return false;
    }
    public void givePvPItem(Player player) {
        ItemStack pvpItem = createPvPItem();
        player.getInventory().setItem(8, pvpItem); // Set the item to the last slot in the hotbar
    }

    public void clearArmour(Player player) {
        player.getInventory().setBoots(null);
        player.getInventory().setLeggings(null);
        player.getInventory().setChestplate(null);
        player.getInventory().setHelmet(null);
        player.getInventory().setItemInMainHand(null);
    }
    private ItemStack createPvPItem() {
        ItemStack item = new ItemStack(Material.SUNFLOWER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String itemName = plugin.getConfigString("itemName");
            List<String> itemLore = plugin.getConfigList("itemLore");
            meta.setDisplayName(itemName);
            meta.addEnchant(Enchantment.DAMAGE_ALL, 1, true);
            meta.setLore(itemLore);

            meta.getPersistentDataContainer().set(nonDroppableKey, PersistentDataType.BOOLEAN, true);
            meta.getPersistentDataContainer().set(nonMovableKey, PersistentDataType.BOOLEAN, true);
            meta.getPersistentDataContainer().set(triggerPvp, PersistentDataType.BOOLEAN, true);

            item.setItemMeta(meta);
        }
        return item;
    }
    public void giveDiamondGear(Player player) {
        ItemStack diamondBoots = createItem(Material.DIAMOND_BOOTS, "Diamond Boots", Enchantment.PROTECTION_ENVIRONMENTAL, 5);
        ItemStack diamondLeggings = createItem(Material.DIAMOND_LEGGINGS, "Diamond Leggings", Enchantment.PROTECTION_ENVIRONMENTAL, 5);
        ItemStack diamondChestplate = createItem(Material.DIAMOND_CHESTPLATE, "Diamond Chestplate", Enchantment.PROTECTION_ENVIRONMENTAL, 5);
        ItemStack diamondHelmet = createItem(Material.DIAMOND_HELMET, "Diamond Helmet", Enchantment.PROTECTION_ENVIRONMENTAL, 5);
        ItemStack diamondSword = createItem(Material.DIAMOND_SWORD, "Diamond Sword", Enchantment.DAMAGE_ALL, 10);
        diamondSword.getItemMeta().getPersistentDataContainer().set(CustomAttributes.TRIGGER_PVP.getNamespacedKey(plugin), PersistentDataType.BOOLEAN, true);
        player.getInventory().setBoots(diamondBoots);
        player.getInventory().setLeggings(diamondLeggings);
        player.getInventory().setChestplate(diamondChestplate);
        player.getInventory().setHelmet(diamondHelmet);
        player.getInventory().setItemInMainHand(diamondSword);
    }

    private ItemStack createItem(Material material, String displayName, Enchantment enchantment, int enchantmentLevel) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(displayName);

            meta.addEnchant(enchantment, enchantmentLevel, true);

            if (meta instanceof Damageable) {
                ((Damageable) meta).setDamage(0);
            }

            meta.getPersistentDataContainer().set(CustomAttributes.NON_MOVABLE.getNamespacedKey(plugin), PersistentDataType.BOOLEAN, true);
            item.setItemMeta(meta);
        }
        return item;
    }
}
