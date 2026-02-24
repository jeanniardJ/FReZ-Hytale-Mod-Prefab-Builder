package com.jjeanniard.plugins.prefab_builder.ui;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.prefab.PrefabStore;
import com.hypixel.hytale.server.core.prefab.selection.standard.BlockSelection;
import com.jjeanniard.plugins.prefab_builder.components.BuildSessionComponent;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;

import java.nio.file.Path;

/**
 * Interface de sélection de préfabriqués pour les administrateurs.
 */
public class PrefabSelectionPage extends InteractiveCustomUIPage<PrefabSelectionPage.PrefabEventData> {

    private final Vector3i targetPos;
    private final World world;

    public static class PrefabEventData {
        public String action;
        public String prefabId;

        public static final BuilderCodec<PrefabEventData> CODEC = BuilderCodec.builder(PrefabEventData.class, PrefabEventData::new)
                .append(new KeyedCodec<>("Action", Codec.STRING), (o, v) -> o.action = v, o -> o.action).add()
                .append(new KeyedCodec<>("PrefabId", Codec.STRING), (o, v) -> o.prefabId = v, o -> o.prefabId).add()
                .build();
        
        public PrefabEventData() {}
    }

    public PrefabSelectionPage(PlayerRef playerRef, World world, Vector3i targetPos) {
        super(playerRef, CustomPageLifetime.CanDismiss, PrefabEventData.CODEC);
        this.world = world;
        this.targetPos = targetPos;
    }

    @Override
    public void build(Ref<EntityStore> ref, UICommandBuilder cmd, UIEventBuilder events, Store<EntityStore> store) {
        cmd.append("prefab_builder/SelectionPage.ui");
        events.addEventBinding(CustomUIEventBindingType.Activating, "#CloseButton", EventData.of("Action", "CLOSE"), false);

        var prefabStore = PrefabStore.get();
        var prefabs = prefabStore.getServerPrefabDir("builder/");

        if (prefabs.isEmpty()) {
            cmd.set("#Title.Text", "Aucun préfabriqué trouvé !");
            return;
        }

        int index = 0;
        for (var entry : prefabs.entrySet()) {
            String fileName = entry.getKey().getFileName().toString();
            String prefabId = "builder/" + fileName;
            
            cmd.append("#PrefabList", "prefab_builder/PrefabItem.ui");
            
            String selector = "#PrefabList[" + index + "]";
            cmd.set(selector + " #PrefabName.Text", fileName);

            EventData selectEvent = new EventData();
            selectEvent.append("Action", "SELECT");
            selectEvent.append("PrefabId", prefabId);
            events.addEventBinding(CustomUIEventBindingType.Activating, selector + " #SelectButton", selectEvent, false);
            
            index++;
        }
    }

    @Override
    public void handleDataEvent(Ref<EntityStore> ref, Store<EntityStore> store, PrefabEventData data) {
        if ("CLOSE".equals(data.action)) {
            this.close();
        } else if ("SELECT".equals(data.action)) {
            handlePrefabSelection(data.prefabId);
            this.close();
        }
    }

    private void handlePrefabSelection(String prefabId) {
        world.execute(() -> {
            // Utiliser REGISTRY.newHolder() de EntityStore
            Holder<EntityStore> holder = EntityStore.REGISTRY.newHolder();
            
            BuildSessionComponent session = new BuildSessionComponent(prefabId, targetPos);
            holder.addComponent(BuildSessionComponent.getComponentType(), session);
            
            holder.addComponent(TransformComponent.getComponentType(), new TransformComponent(
                new Vector3d(targetPos.getX(), targetPos.getY(), targetPos.getZ()),
                Vector3f.ZERO
            ));
            
            // world.getEntityStore().getStore() retourne un Store<EntityStore>
            world.getEntityStore().getStore().addEntity(holder, AddReason.SPAWN);
            
            this.playerRef.sendMessage(Message.raw("§a✔ Hologramme du préfab '" + prefabId + "' positionné !"));
        });
    }
}
