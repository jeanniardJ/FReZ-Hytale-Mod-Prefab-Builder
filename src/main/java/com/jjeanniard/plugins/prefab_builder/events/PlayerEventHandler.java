package com.jjeanniard.plugins.prefab_builder.events;

import com.hypixel.hytale.server.core.event.events.player.PlayerInteractEvent;
import com.hypixel.hytale.server.core.event.events.ecs.PlaceBlockEvent;
import com.jjeanniard.plugins.prefab_builder.tool.AdminToolHandler;
import com.jjeanniard.plugins.prefab_builder.utils.ChestLinker;
import com.jjeanniard.plugins.prefab_builder.components.BuildSessionComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * Gère les événements liés aux actions des joueurs.
 */
public class PlayerEventHandler {

    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Vector3i targetPos = event.getTargetBlock();
        
        if (targetPos != null) {
            World world = player.getWorld();
            
            // Correction de l'accès au BlockState pour Hytale
            long chunkIndex = com.hypixel.hytale.math.util.ChunkUtil.indexChunkFromBlock(targetPos.getX(), targetPos.getZ());
            com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk chunk = world.getChunkIfLoaded(chunkIndex);
            
            if (chunk != null) {
                com.hypixel.hytale.server.core.universe.world.meta.BlockState state = chunk.getState(targetPos.getX() & 31, targetPos.getY(), targetPos.getZ() & 31);
                
                // Si le joueur clique sur un coffre, on essaie de le lier
                if (state instanceof com.hypixel.hytale.server.core.universe.world.meta.state.ItemContainerState) {
                    ChestLinker.tryLinkChest(world, targetPos, world.getEntityStore().getStore());
                }
            }

            // Vérification si on clique sur un coffre déjà lié pour lancer/continuer la construction
            world.getEntityStore().getStore().forEachEntityParallel(BuildSessionComponent.getComponentType(), (index, archetypeChunk, commandBuffer) -> {
                Ref<EntityStore> ref = archetypeChunk.getReferenceTo(index);
                BuildSessionComponent session = archetypeChunk.getComponent(index, BuildSessionComponent.getComponentType());
                if (session != null && targetPos.equals(session.getLinkedChestPos())) {
                    ChestLinker.checkAndStartBuild(ref, world.getEntityStore().getStore());
                }
            });
        }
    }

    public void onBlockPlace(PlaceBlockEvent event) {
        com.hypixel.hytale.server.core.inventory.ItemStack inHand = event.getItemInHand();
        if (inHand != null && AdminToolHandler.BUILDER_BLOCK_ID.equals(inHand.getItemId())) {
            Vector3i pos = event.getTargetBlock();
            
            // On récupère le monde par défaut (hack nécessaire car l'event ECS n'a pas de contexte de monde direct ici)
            com.hypixel.hytale.server.core.universe.world.World world = com.hypixel.hytale.server.core.universe.Universe.get().getDefaultWorld();
            
            // On cherche le joueur le plus proche pour lui ouvrir l'UI
            Player closestPlayer = null;
            double minDist = 10.0;
            
            for (com.hypixel.hytale.server.core.universe.PlayerRef playerRef : world.getPlayerRefs()) {
                Player p = playerRef.getComponent(Player.getComponentType());
                if (p != null) {
                    com.hypixel.hytale.server.core.modules.entity.component.TransformComponent tc = playerRef.getComponent(com.hypixel.hytale.server.core.modules.entity.component.TransformComponent.getComponentType());
                    if (tc != null) {
                        double dist = tc.getPosition().distanceTo(new com.hypixel.hytale.math.vector.Vector3d(pos.getX(), pos.getY(), pos.getZ()));
                        if (dist < minDist) {
                            minDist = dist;
                            closestPlayer = p;
                        }
                    }
                }
            }

            if (closestPlayer != null) {
                AdminToolHandler.handleInteract(closestPlayer, closestPlayer.getPlayerRef().getReference(), pos);
            }
        }
    }
}
