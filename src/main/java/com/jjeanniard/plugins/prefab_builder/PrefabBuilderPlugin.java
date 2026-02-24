package com.jjeanniard.plugins.prefab_builder;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.event.events.player.PlayerInteractEvent;
import com.hypixel.hytale.server.core.event.events.ecs.PlaceBlockEvent;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.jjeanniard.plugins.prefab_builder.components.BuildSessionComponent;
import com.jjeanniard.plugins.prefab_builder.components.HologramComponent;
import java.util.logging.Level;

/**
 * Classe principale du plugin Prefab Builder.
 */
public class PrefabBuilderPlugin extends JavaPlugin {

    public PrefabBuilderPlugin(JavaPluginInit init) {
        super(init);
    }

    @Override
    public void setup() {
        getLogger().at(Level.INFO).log("Configuration du plugin Prefab Builder...");
        
        // Enregistrement des composants ECS et stockage des types
        BuildSessionComponent.setComponentType(getEntityStoreRegistry().registerComponent(BuildSessionComponent.class, BuildSessionComponent::new));
        HologramComponent.setComponentType(getEntityStoreRegistry().registerComponent(HologramComponent.class, HologramComponent::new));
    }

    @Override
    public void start() {
        getLogger().at(Level.INFO).log("Démarrage du plugin Prefab Builder...");

        com.jjeanniard.plugins.prefab_builder.events.PlayerEventHandler playerHandler = new com.jjeanniard.plugins.prefab_builder.events.PlayerEventHandler();
        
        // Utilisation de registerGlobal pour éviter les problèmes de KeyType
        getEventRegistry().registerGlobal(PlayerInteractEvent.class, playerHandler::onPlayerInteract);
        getEventRegistry().registerGlobal(PlaceBlockEvent.class, playerHandler::onBlockPlace);

        getEntityStoreRegistry().registerSystem(new com.jjeanniard.plugins.prefab_builder.systems.HologramSystem());

        getCommandRegistry().registerCommand(new com.jjeanniard.plugins.prefab_builder.commands.PrefabBuilderCommand());
    }

    @Override
    public void shutdown() {
        getLogger().at(Level.INFO).log("Arrêt du plugin Prefab Builder...");
    }
}
