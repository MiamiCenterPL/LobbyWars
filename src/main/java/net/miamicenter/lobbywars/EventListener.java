package net.miamicenter.lobbywars;

import org.bukkit.GameMode;
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

public class EventListener implements Listener {
    LobbyWars plugin = LobbyWars.getPlugin(LobbyWars.class);
    ItemManager itemManager = plugin.getItemManager();
    PvPManager pvpManager = plugin.getPvPManager();

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

        if (previousSlot == newSlot) {
            return;
        }

        ItemStack newItem = player.getInventory().getItem(newSlot);
        boolean canItemTriggerPVP = itemManager.canTriggerPvp(newItem);
        boolean isTriggerPvpTaskRunning = pvpManager.isTriggerPvpTaskRunning(player);
        boolean canDamagePlayer = pvpManager.canDamage(player);

        if (isTriggerPvpTaskRunning && !canItemTriggerPVP) {
            pvpManager.removePvPTask(player);
            return;
        }
        if (canDamagePlayer && !canItemTriggerPVP) {
            pvpManager.disablePlayerPvp(player);
            return;
        }
        if (canItemTriggerPVP && !canDamagePlayer && !isTriggerPvpTaskRunning) {
            pvpManager.startPlayerPvp(player);
        }
    }

    @EventHandler
    private void onEntityDamage(EntityDamageByEntityEvent event) {
        //Affect only Player vs Player
        if (!(event.getEntity() instanceof Player damaged && event.getDamager() instanceof Player damager)) {
            return;
        }

        //Both players have to be in PVP mode
        if (!pvpManager.canDamage(damager) || !pvpManager.canDamage(damaged)) {
            event.setCancelled(true);
            return;
        }

        //Ignore non-lethal damage
        if (event.getFinalDamage() < damaged.getHealth()) {
            return;
        }

        pvpManager.handlePlayerDeath(damager, damaged);
    }

    @EventHandler
    private void onItemDrop(PlayerDropItemEvent event) {
        ItemStack droppedItem = event.getItemDrop().getItemStack();
        ItemMeta meta = droppedItem.getItemMeta();
        if (itemManager.blockItemDrop(meta)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        ItemMeta meta = event.getItemInHand().getItemMeta();
        //Block placing placable objects with block attribute NON_DROPPABLE - for use of Sunflowers etc.
        if (itemManager.blockItemDrop(meta)) {
            event.setCancelled(true);
        }
    }
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getClickedInventory() != null && event.getCurrentItem() != null) {
            ItemStack clickedItem = event.getCurrentItem();
            if (itemManager.blockItemMove(clickedItem)) {
                event.setCancelled(true);
            }
        }
    }
    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (event.getHand() == EquipmentSlot.HAND) {
            ItemStack itemInHand = event.getPlayer().getInventory().getItemInMainHand();
            if (itemManager.blockItemMove(itemInHand)) {
                event.setCancelled(true);
            }
        }
    }
    @EventHandler
    public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent event) {
        ItemStack mainItem = event.getMainHandItem();
        ItemStack offItem = event.getOffHandItem();
        if (itemManager.blockItemMove(mainItem) || itemManager.blockItemMove(offItem)) {
            event.setCancelled(true);
        }
    }
    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        ItemStack item = event.getCursor();

        if (itemManager.blockItemMove(item)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryMoveItem(InventoryMoveItemEvent event) {
        ItemStack movedItem = event.getItem();

        if (itemManager.blockItemMove(movedItem)) {
            event.setCancelled(true);
        }
    }
}
