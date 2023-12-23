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
    private static ItemManager instance;
    private final LobbyWars plugin;
    private final NamespacedKey nonDroppableKey;
    private final NamespacedKey nonMovableKey;
    private final NamespacedKey triggerPvp;

    private ItemManager() {
        this.plugin = LobbyWars.getPlugin(LobbyWars.class);
        this.nonDroppableKey = CustomAttributes.NON_DROPPABLE.getNamespacedKey(plugin);
        this.nonMovableKey = CustomAttributes.NON_MOVABLE.getNamespacedKey(plugin);
        this.triggerPvp = CustomAttributes.TRIGGER_PVP.getNamespacedKey(plugin);
    }

    public static ItemManager getInstance() {
        if (instance == null) {
            instance = new ItemManager();
        }
        return instance;
    }
    /**
     * @return true if item mustn't be dropped or placed
     */
    public boolean blockItemDrop(ItemStack item){
        if (item == null) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        PersistentDataContainer container = meta.getPersistentDataContainer();
        return container.has(nonDroppableKey, PersistentDataType.BOOLEAN) && container.get(nonDroppableKey, PersistentDataType.BOOLEAN);
    }
    /**
     * @return true if item mustn't be moved in inventory
     */
    public boolean blockItemMove(ItemStack item){
        if (item == null) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        PersistentDataContainer container = meta.getPersistentDataContainer();
        return container.has(nonMovableKey, PersistentDataType.BOOLEAN) && container.get(nonMovableKey, PersistentDataType.BOOLEAN);

    }
    /**
     * @return true if item can trigger PVP
     */
    public boolean canTriggerPvp(ItemStack item) {
        if (item == null) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        NamespacedKey triggerPvpKey = CustomAttributes.TRIGGER_PVP.getNamespacedKey(plugin);
        PersistentDataContainer container = meta.getPersistentDataContainer();
        return container.has(triggerPvpKey, PersistentDataType.BOOLEAN) && container.get(triggerPvpKey, PersistentDataType.BOOLEAN);
    }
    /**
     * Set player's hotbar slot 8th to an PVP Trigger Item
     */
    public void givePvPItem(Player player) {
        ItemStack pvpItem = createPvPItem();
        player.getInventory().setItem(8, pvpItem); // Set the item to the last slot in the hotbar
    }
    /**
     * Clears player armour slots
     */
    public void clearArmour(Player player) {
        player.getInventory().setBoots(null);
        player.getInventory().setLeggings(null);
        player.getInventory().setChestplate(null);
        player.getInventory().setHelmet(null);
        player.getInventory().setItemInMainHand(null);
    }
    /**
     * This will return an PVP Trigger item.
     */
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
    /**
     * This function will give player a set of enchanted Diamond armour and an enchanted sword.
     * @param player - Player which receives the items.
     */
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
    /**
     * Basic create item function. All created items will have 'NON_MOVABLE' attribute.
     * @param material - Item's material you want to spawn e.g. SUN_FLOWER
     * @param displayName - Item's display name
     * @param enchantment Item's enchantment (accepts only single one)
     * @param enchantmentLevel Item's enchantment level
     */
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
