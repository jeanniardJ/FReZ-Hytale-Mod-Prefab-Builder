package com.jjeanniard.plugins.prefab_builder.systems;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.system.RefSystem;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.jjeanniard.plugins.prefab_builder.components.BuildSessionComponent;
import com.jjeanniard.plugins.prefab_builder.components.HologramComponent;
import com.hypixel.hytale.server.core.prefab.PrefabStore;
import com.hypixel.hytale.server.core.prefab.selection.standard.BlockSelection;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;

/**
 * Système gérant l'affichage des hologrammes (ghost blocks).
 */
public class HologramSystem extends RefSystem<EntityStore> {

    public HologramSystem() {
        super();
    }

    @Override
    public Query<EntityStore> getQuery() {
        return (Query<EntityStore>) BuildSessionComponent.getComponentType();
    }

    @Override
    public void onEntityAdded(Ref<EntityStore> ref, AddReason reason, Store<EntityStore> store, CommandBuffer<EntityStore> commandBuffer) {
        BuildSessionComponent session = store.getComponent(ref, BuildSessionComponent.getComponentType());
        if (session == null) return;

        BlockSelection selection = PrefabStore.get().getServerPrefab(session.getPrefabId());
        if (selection == null) return;

        long masterId = ref.getIndex();
        Vector3i origin = session.getOrigin();

        selection.forEachBlock((lx, ly, lz, blockHolder) -> {
            if (blockHolder.blockId() == 0) return;

            // Créer une entité ghost pour chaque bloc
            Holder<EntityStore> ghostHolder = EntityStore.REGISTRY.newHolder();
            
            ghostHolder.addComponent(HologramComponent.getComponentType(), new HologramComponent(masterId, blockHolder.blockId()));
            
            ghostHolder.addComponent(TransformComponent.getComponentType(), new TransformComponent(
                new Vector3d(origin.getX() + lx + 0.5, origin.getY() + ly, origin.getZ() + lz + 0.5),
                Vector3f.ZERO
            ));

            // Ajouter l'entité via le CommandBuffer du système
            commandBuffer.addEntity(ghostHolder, AddReason.SPAWN);
        });
    }

    @Override
    public void onEntityRemove(Ref<EntityStore> ref, RemoveReason reason, Store<EntityStore> store, CommandBuffer<EntityStore> commandBuffer) {
        long masterId = ref.getIndex();
        
        // Nettoyer les entités liées
        store.forEachEntityParallel(HologramComponent.getComponentType(), (index, chunk, buffer) -> {
            HologramComponent holo = chunk.getComponent(index, HologramComponent.getComponentType());
            if (holo != null && holo.getMasterEntityId() == masterId) {
                buffer.removeEntity(chunk.getReferenceTo(index), RemoveReason.REMOVE);
            }
        });
    }
}
