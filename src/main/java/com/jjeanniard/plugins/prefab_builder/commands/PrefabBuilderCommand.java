package com.jjeanniard.plugins.prefab_builder.commands;

import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.codec.Codec;
import com.jjeanniard.plugins.prefab_builder.tool.AdminToolHandler;
import com.hypixel.hytale.server.core.prefab.PrefabStore;

import java.util.concurrent.CompletableFuture;

/**
 * Commande d'administration pour le plugin Prefab Builder.
 */
public class PrefabBuilderCommand extends AbstractCommand {

    public PrefabBuilderCommand() {
        super("prefabbuilder", "Commandes d'administration pour Prefab Builder");
        requirePermission("prefabbuilder.admin");
    }

    @Override
    protected CompletableFuture<Void> execute(CommandContext ctx) {
        if (ctx.sender() instanceof Player player) {
            String[] args = ctx.getInputString().split(" ");
            if (args.length < 2) {
                sendHelp(player);
                return CompletableFuture.completedFuture(null);
            }

            String subCommand = args[1].toLowerCase();
            switch (subCommand) {
                case "give" -> handleGive(player);
                default -> sendHelp(player);
            }
        } else {
            ctx.sender().sendMessage(Message.raw("§cCette commande est réservée aux joueurs."));
        }

        return CompletableFuture.completedFuture(null);
    }

    private void handleGive(Player player) {
        ItemStack tool = new ItemStack(AdminToolHandler.BUILDER_BLOCK_ID);

        player.getInventory().getStorage().addItemStack(tool);
        player.sendMessage(Message.raw("§a✔ Vous avez reçu le bloc de Chantier !"));
    }

    private void sendHelp(Player player) {
        player.sendMessage(Message.raw("§6§lAide Prefab Builder :"));
        player.sendMessage(Message.raw("§e/prefabbuilder give §7- Obtenir le bloc admin"));
    }
}
