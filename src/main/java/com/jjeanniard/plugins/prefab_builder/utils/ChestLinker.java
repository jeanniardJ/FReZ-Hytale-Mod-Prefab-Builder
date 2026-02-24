package com.jjeanniard.plugins.prefab_builder.utils;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.jjeanniard.plugins.prefab_builder.components.BuildSessionComponent;
import com.jjeanniard.plugins.prefab_builder.config.PrefabMaterialDef;
import com.jjeanniard.plugins.prefab_builder.systems.AutoBuilderSystem;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.meta.BlockState;
import com.hypixel.hytale.server.core.universe.world.meta.state.ItemContainerState;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.server.core.Message;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Utilitaire pour lier les coffres aux sessions de construction.
 */
public class ChestLinker {

    public static void tryLinkChest(World world, Vector3i chestPos, Store<EntityStore> store) {
        AtomicReference<Ref<EntityStore>> closestSession = new AtomicReference<>();
        AtomicReference<Double> minDistance = new AtomicReference<>(10.0);

        store.forEachEntityParallel(BuildSessionComponent.getComponentType(), (index, chunk, commandBuffer) -> {
            Ref<EntityStore> ref = chunk.getReferenceTo(index);
            BuildSessionComponent session = chunk.getComponent(index, BuildSessionComponent.getComponentType());
            
            if (session != null && session.getLinkedChestPos() == null) {
                double dist = session.getOrigin().distanceTo(chestPos);
                if (dist < minDistance.get()) {
                    minDistance.set(dist);
                    closestSession.set(ref);
                }
            }
        });

        if (closestSession.get() != null) {
            Ref<EntityStore> sessionRef = closestSession.get();
            BuildSessionComponent session = store.getComponent(sessionRef, BuildSessionComponent.getComponentType());
            session.setLinkedChestPos(chestPos);
            store.putComponent(sessionRef, BuildSessionComponent.getComponentType(), session);
            
            world.sendMessage(Message.raw("§a✔ Coffre lié au préfabriqué !"));
        }
    }

    public static void checkAndStartBuild(Ref<EntityStore> sessionRef, Store<EntityStore> store) {
        BuildSessionComponent session = store.getComponent(sessionRef, BuildSessionComponent.getComponentType());
        if (session == null || session.isBuilding() || session.getLinkedChestPos() == null) return;

        World world = ((EntityStore)store.getExternalData()).getWorld();
        
        long chunkIndex = ChunkUtil.indexChunkFromBlock(session.getLinkedChestPos().getX(), session.getLinkedChestPos().getZ());
        WorldChunk chunk = world.getChunkIfLoaded(chunkIndex);
        if (chunk == null) return;

        BlockState state = chunk.getState(
            session.getLinkedChestPos().getX() & 31, 
            session.getLinkedChestPos().getY(), 
            session.getLinkedChestPos().getZ() & 31
        );

        if (state instanceof ItemContainerState containerState) {
            ItemContainer container = containerState.getItemContainer();
            PrefabMaterialDef materials = PrefabMaterialDef.loadFor(session.getPrefabId());
            if (materials == null) return;

            if (materials.isSatisfiedBy(container)) {
                materials.consumeFrom(container); // On consomme les ressources
                session.setBuilding(true);
                store.putComponent(sessionRef, BuildSessionComponent.getComponentType(), session);
                AutoBuilderSystem.startBuilding(sessionRef, store, materials, container);
                world.sendMessage(Message.raw("§6Construction en cours..."));
            } else {
                String missing = materials.getMissingItemsReportFromContainer(container);
                world.sendMessage(Message.raw("§cRessources manquantes : " + missing));
            }
        }
    }
}
