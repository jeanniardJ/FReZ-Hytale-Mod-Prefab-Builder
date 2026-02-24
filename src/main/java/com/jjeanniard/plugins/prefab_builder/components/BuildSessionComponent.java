package com.jjeanniard.plugins.prefab_builder.components;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.component.ComponentType;

/**
 * Composant stockant les données d'une session de construction de préfabriqué.
 */
public class BuildSessionComponent implements Component<EntityStore> {

    private static ComponentType<EntityStore, BuildSessionComponent> COMPONENT_TYPE;

    private String prefabId;
    private Vector3i origin;
    private Vector3i linkedChestPos;
    private float progressRatio = 0.0f;
    private boolean isBuilding = false;

    public BuildSessionComponent() {
    }

    public BuildSessionComponent(String prefabId, Vector3i origin) {
        this.prefabId = prefabId;
        this.origin = origin;
    }

    public static ComponentType<EntityStore, BuildSessionComponent> getComponentType() {
        return COMPONENT_TYPE;
    }

    public static void setComponentType(ComponentType<EntityStore, BuildSessionComponent> type) {
        COMPONENT_TYPE = type;
    }

    // Getters & Setters
    public String getPrefabId() { return prefabId; }
    public void setPrefabId(String prefabId) { this.prefabId = prefabId; }
    public Vector3i getOrigin() { return origin; }
    public void setOrigin(Vector3i origin) { this.origin = origin; }
    public Vector3i getLinkedChestPos() { return linkedChestPos; }
    public void setLinkedChestPos(Vector3i linkedChestPos) { this.linkedChestPos = linkedChestPos; }
    public float getProgressRatio() { return progressRatio; }
    public void setProgressRatio(float progressRatio) { this.progressRatio = progressRatio; }
    public boolean isBuilding() { return isBuilding; }
    public void setBuilding(boolean building) { isBuilding = building; }

    @Override
    public BuildSessionComponent clone() {
        try {
            return (BuildSessionComponent) super.clone();
        } catch (CloneNotSupportedException e) {
            return null;
        }
    }
}
