package com.jjeanniard.plugins.prefab_builder.components;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class GhostBlockComponent implements Component<EntityStore> {
    private int blockId;
    private float opacity;

    public GhostBlockComponent(int blockId, float opacity) {
        this.blockId = blockId;
        this.opacity = opacity;
    }
    
    public GhostBlockComponent() {} // Default constructor for serialization if needed

    public int getBlockId() { return blockId; }
    public void setBlockId(int blockId) { this.blockId = blockId; }

    public float getOpacity() { return opacity; }
    public void setOpacity(float opacity) { this.opacity = opacity; }

    @Override
    public GhostBlockComponent clone() {
        try {
            return (GhostBlockComponent) super.clone();
        } catch (CloneNotSupportedException e) {
            return new GhostBlockComponent(this.blockId, this.opacity);
        }
    }
}
