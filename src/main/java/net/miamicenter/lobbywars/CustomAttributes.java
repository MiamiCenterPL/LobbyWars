package net.miamicenter.lobbywars;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;

public enum CustomAttributes {
    //ITEM ATTRIBUTES:
    TRIGGER_PVP("TriggerPvp"),
    NON_DROPPABLE("NonDroppable"),
    NON_MOVABLE("NonMovable"),
    //PLAYER ATTRIBUTES
    CAN_PVP("CanPvp");

    private final String key;

    CustomAttributes(String key) {
        this.key = key;
    }

    public NamespacedKey getNamespacedKey(JavaPlugin plugin) {
        return new NamespacedKey(plugin, key);
    }
}
