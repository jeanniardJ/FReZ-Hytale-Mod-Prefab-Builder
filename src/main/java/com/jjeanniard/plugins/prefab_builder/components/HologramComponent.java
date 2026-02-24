package com.jjeanniard.plugins.prefab_builder.components;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.component.ComponentType;

/**
 * Composant identifiant une entité comme faisant partie d'un hologramme.
 */
public class HologramComponent implements Component<EntityStore> {

    private static ComponentType<EntityStore, HologramComponent> COMPONENT_TYPE;

    private long masterEntityId;
    private int blockId;

    public HologramComponent() {
    }

    public HologramComponent(long masterEntityId, int blockId) {
        this.masterEntityId = masterEntityId;
        this.blockId = blockId;
    }

    public static ComponentType<EntityStore, HologramComponent> getComponentType() {
        return COMPONENT_TYPE;
    }

    public static void setComponentType(ComponentType<EntityStore, HologramComponent> type) {
        COMPONENT_TYPE = type;
    }

    public long getMasterEntityId() { return masterEntityId; }
    public void setMasterEntityId(long masterEntityId) { this.masterEntityId = masterEntityId; }
    public int getBlockId() { return blockId; }
    public void setBlockId(int blockId) { this.blockId = blockId; }

    @Override
    public HologramComponent clone() {
        try {
            return (HologramComponent) super.clone();
        } catch (CloneNotSupportedException e) {
            return null;
        }
    }
}
