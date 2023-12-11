package net.miamicenter.lobbywars;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataHolder;
import org.bukkit.persistence.PersistentDataType;

public class EventListener implements Listener {
    LobbyWars plugin = LobbyWars.getPlugin(LobbyWars.class);
    ItemManager itemManager = plugin.getItemManager();
    TimerManager timerManager = new TimerManager();
    NamespacedKey nonMovableKey = CustomAttributes.NON_MOVABLE.getNamespacedKey(plugin);
    NamespacedKey nonDroppableKey = CustomAttributes.NON_DROPPABLE.getNamespacedKey(plugin);

    private final NamespacedKey triggerPvpTaskKey = new NamespacedKey(plugin, "TriggerPvpTask");
    //Booleans;
    private boolean isTriggerPvpTaskRunning(Player player) {
        return player.getPersistentDataContainer().has(triggerPvpTaskKey, PersistentDataType.INTEGER);
    }
    private boolean canTriggerPvp(ItemMeta meta) {
        NamespacedKey triggerPvpKey = CustomAttributes.TRIGGER_PVP.getNamespacedKey(plugin);
        if (meta != null && meta.getPersistentDataContainer().has(triggerPvpKey, PersistentDataType.BOOLEAN)) {
            return meta.getPersistentDataContainer().get(triggerPvpKey, PersistentDataType.BOOLEAN);
        }
        return false;
    }
    private boolean canDamage(Player player){
        NamespacedKey canPvpKey = CustomAttributes.CAN_PVP.getNamespacedKey(plugin);
        if (player.getPersistentDataContainer().has(canPvpKey, PersistentDataType.BOOLEAN)){
            return player.getPersistentDataContainer().get(canPvpKey, PersistentDataType.BOOLEAN);
        } else {
            return false;
        }
    }
    private boolean blockMove(ItemMeta meta){
        if (meta != null) {
            PersistentDataContainer container = meta.getPersistentDataContainer();
            return container.has(nonMovableKey, PersistentDataType.BOOLEAN) && container.get(nonMovableKey, PersistentDataType.BOOLEAN);
        }
        return false;
    }
    //Functions;
    private void disablePlayerPvp(Player player) {
        NamespacedKey canPvp = CustomAttributes.CAN_PVP.getNamespacedKey(plugin);
        player.getPersistentDataContainer().set(canPvp, PersistentDataType.BOOLEAN, false);
        String quitMessage = plugin.getConfigString("quitPVPMessage");
        ActionBarMessage.sendActionBarMessage(player, quitMessage);
        itemManager.clearArmour(player);
        itemManager.givePvPItem(player);
    }

    //Events;
    @EventHandler
    private void onPlayerJoin(PlayerJoinEvent event){
        Player player = event.getPlayer();
        //Apply only to Survival player;
        if (player.getGameMode() == GameMode.SURVIVAL) {
            itemManager.givePvPItem(player);
       }
    }

    @EventHandler
    public void onPlayerItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        int previousSlot = event.getPreviousSlot();
        int newSlot = event.getNewSlot();

        if (previousSlot != newSlot) {
            ItemStack newItem = player.getInventory().getItem(newSlot);

            if (isTriggerPvpTaskRunning(player) && (newItem == null || !canTriggerPvp(newItem.getItemMeta()))) {
                int taskId = player.getPersistentDataContainer().get(triggerPvpTaskKey, PersistentDataType.INTEGER);
                timerManager.stopTimer(taskId);
                player.getPersistentDataContainer().remove(triggerPvpTaskKey);
            } else if (canDamage(player) && (newItem == null || !canTriggerPvp(newItem.getItemMeta()))) {
                disablePlayerPvp(player);
            } else if (newItem != null && canTriggerPvp(newItem.getItemMeta()) && !canDamage(player) && !isTriggerPvpTaskRunning(player)) {
                int taskId = timerManager.startTimer(60, 2, (duration) -> sendWarWarning(duration, player), () -> startWar(player), () -> ActionBarMessage.sendActionBarMessage(player, ""));
                player.getPersistentDataContainer().set(triggerPvpTaskKey, PersistentDataType.INTEGER, taskId);
            }
        }
    }
    private void sendWarWarning(int duration, Player player) {
        String message = plugin.getConfigString("countdownMessage");
        float time = (float) duration / 20;
        message = message.replace("%timer%", String.valueOf(time));
        ActionBarMessage.sendActionBarMessage(player, message);
    }
    private void startWar(Player player) {
        timerManager.stopTimer(player.getPersistentDataContainer().get(triggerPvpTaskKey, PersistentDataType.INTEGER));
        String warMessage = plugin.getConfigString("joinPVPMessage");
        NamespacedKey canPvp = CustomAttributes.CAN_PVP.getNamespacedKey(plugin);
        player.getPersistentDataContainer().remove(triggerPvpTaskKey);
        player.getPersistentDataContainer().set(canPvp, PersistentDataType.BOOLEAN, true);
        ActionBarMessage.sendActionBarMessage(player, warMessage);
        plugin.getItemManager().giveDiamondGear(player);
    }

    @EventHandler
    private void onEntityDamage(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player damaged && event.getDamager() instanceof Player damager) {
            if (canDamage(damager) && canDamage(damaged)) {
                if (event.getFinalDamage() > damaged.getHealth()) {
                    damaged.teleport(Bukkit.getWorlds().get(0).getSpawnLocation());
                    disablePlayerPvp(damaged);
                    damaged.setHealth(damaged.getHealthScale());

                    plugin.getDBManager().updatePlayerStats(
                            damager.getUniqueId(),
                            damager.getName(),
                            false,
                            true
                    );

                    plugin.getDBManager().updatePlayerStats(
                            damaged.getUniqueId(),
                            damaged.getName(),
                            true,
                            false
                    );
                    String deathLogMessage = plugin.getConfigString("deathLogMessage")
                            .replace("%killer%", damager.getDisplayName())
                            .replace("%victim%", damaged.getDisplayName());
                    Bukkit.broadcastMessage(deathLogMessage);
                } else {
                    return;
                }
            }
            event.setCancelled(true);
        }
    }

    @EventHandler
    private void onItemDrop(PlayerDropItemEvent event) {
        ItemStack droppedItem = event.getItemDrop().getItemStack();
        ItemMeta meta = droppedItem.getItemMeta();
        if (meta != null && meta.getPersistentDataContainer().has(nonDroppableKey, PersistentDataType.BOOLEAN)) {
            boolean noDrop = meta.getPersistentDataContainer().get(nonDroppableKey, PersistentDataType.BOOLEAN);
            if (noDrop) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (event.getItemInHand() != null) {
            ItemMeta meta = event.getItemInHand().getItemMeta();
            if (canDrop(meta)) {
                event.setCancelled(true);
            }
        }
    }
    private boolean canDrop(ItemMeta meta){
        if (meta != null) {
            PersistentDataContainer container = meta.getPersistentDataContainer();
            if (container.has(nonDroppableKey, PersistentDataType.BOOLEAN)) return container.get(nonDroppableKey, PersistentDataType.BOOLEAN);
        }
        return false;
    }
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getClickedInventory() != null && event.getCurrentItem() != null) {
            ItemStack clickedItem = event.getCurrentItem();
            ItemMeta meta = clickedItem.getItemMeta();
            if (blockMove(meta)) {
                event.setCancelled(true);
            }
        }
    }
    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (event.getRightClicked().getType() == EntityType.ITEM_FRAME) {
            if (event.getHand() == EquipmentSlot.HAND) {
                ItemStack itemInHand = event.getPlayer().getInventory().getItemInMainHand();
                if (blockMove(itemInHand.getItemMeta())) {
                    event.setCancelled(true);
                }
            }
        }
    }
    @EventHandler
    public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent event) {
        ItemStack mainItem = event.getMainHandItem();
        ItemStack offItem = event.getOffHandItem();
        if (mainItem != null && blockMove(mainItem.getItemMeta()) || offItem != null && blockMove(offItem.getItemMeta())) {
            event.setCancelled(true);
        }
    }
    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        ItemMeta meta = event.getCursor().getItemMeta();

        if (blockMove(meta)) {
            event.setCancelled(true);
        }
    }


    @EventHandler
    public void onInventoryMoveItem(InventoryMoveItemEvent event) {
        ItemStack movedItem = event.getItem();
        ItemMeta meta = movedItem.getItemMeta();

        if (blockMove(meta)) {
            event.setCancelled(true);
        }
    }
}
