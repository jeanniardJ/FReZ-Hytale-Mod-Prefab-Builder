package com.jjeanniard.plugins.prefab_builder.tool;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.jjeanniard.plugins.prefab_builder.ui.PrefabSelectionPage;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.math.vector.Vector3i;

/**
 * Gère l'outil d'administration pour la sélection et le placement des hologrammes.
 */
public class AdminToolHandler {

    public static final String BUILDER_BLOCK_ID = "prefab_builder:builder_block";

    public static boolean isBuilderBlock(ItemStack item) {
        if (item == null) return false;
        return BUILDER_BLOCK_ID.equals(item.getItemId());
    }

    public static void handleInteract(Player player, Ref<EntityStore> playerRef, Vector3i targetPos) {
        World world = player.getWorld();
        player.getPageManager().openCustomPage(
            playerRef, 
            playerRef.getStore(), 
            new PrefabSelectionPage(player.getPlayerRef(), world, targetPos)
        );
    }
}
