package com.jjeanniard.plugins.prefab_builder.systems;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.prefab.selection.mask.BlockMask;
import com.hypixel.hytale.server.core.prefab.PrefabStore;
import com.hypixel.hytale.server.core.prefab.selection.standard.BlockSelection;
import com.hypixel.hytale.server.core.prefab.selection.standard.FeedbackConsumer;
import com.jjeanniard.plugins.prefab_builder.components.BuildSessionComponent;
import com.jjeanniard.plugins.prefab_builder.config.PrefabMaterialDef;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.command.system.CommandSender;

/**
 * Système gérant la construction progressive des préfabriqués.
 */
public class AutoBuilderSystem {

    public static void startBuilding(Ref<EntityStore> sessionRef, Store<EntityStore> store, 
                                     PrefabMaterialDef materials, ItemContainer container) {
        
        BuildSessionComponent session = store.getComponent(sessionRef, BuildSessionComponent.getComponentType());
        if (session == null) return;

        World world = ((EntityStore)store.getExternalData()).getWorld();
        BlockSelection prefab = PrefabStore.get().getServerPrefab(session.getPrefabId());
        if (prefab == null) return;

        FeedbackConsumer progressCallback = (key, total, current, sender, accessor) -> {
            float progress = (float) current / total;
            world.execute(() -> {
                BuildSessionComponent s = store.getComponent(sessionRef, BuildSessionComponent.getComponentType());
                if (s != null) {
                    s.setProgressRatio(progress);
                    
                    // TODO: Consommer les items du container proportionnellement ici
                    
                    if (current >= total) {
                        s.setBuilding(false);
                    }
                    store.putComponent(sessionRef, BuildSessionComponent.getComponentType(), s);
                }
            });
        };

        prefab.placeNoReturn(
            "prefab_build_" + sessionRef.getIndex(),
            (CommandSender) null,
            progressCallback,
            world,
            session.getOrigin(),
            BlockMask.parse("air"),
            world.getEntityStore().getStore()
        );
    }
}
