package com.jjeanniard.plugins.prefab_builder.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Définition des matériaux requis pour un préfabriqué spécifique.
 */
public class PrefabMaterialDef {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    @SerializedName("prefab")
    private String prefabId;

    @SerializedName("requirements")
    private List<MaterialRequirement> requirements;

    public PrefabMaterialDef() {
    }

    public static PrefabMaterialDef loadFor(String prefabId) {
        String fileName = prefabId.replace("/", "_") + ".json";
        Path configPath = Paths.get("config", "materials", fileName);
        if (!Files.exists(configPath)) return null;
        try (Reader reader = new InputStreamReader(Files.newInputStream(configPath), StandardCharsets.UTF_8)) {
            return GSON.fromJson(reader, PrefabMaterialDef.class);
        } catch (IOException e) {
            return null;
        }
    }

    public boolean isSatisfiedBy(Inventory inv) {
        return isSatisfiedBy(inv.getStorage());
    }

    public boolean isSatisfiedBy(ItemContainer container) {
        if (requirements == null) return true;
        return requirements.stream().allMatch(req ->
            container.countItemStacks(stack -> stack.getItemId().equals(req.itemType())) >= req.amount()
        );
    }

    public String getMissingItemsReport(Inventory inv) {
        return getMissingItemsReportFromContainer(inv.getStorage());
    }

    public String getMissingItemsReportFromContainer(ItemContainer container) {
        if (requirements == null) return "";
        return requirements.stream()
            .filter(req -> container.countItemStacks(stack -> stack.getItemId().equals(req.itemType())) < req.amount())
            .map(req -> {
                int missing = req.amount() - container.countItemStacks(stack -> stack.getItemId().equals(req.itemType()));
                return req.itemType() + " x" + missing;
            })
            .collect(Collectors.joining(", "));
    }

    /**
     * Retire les items requis du conteneur.
     */
    public void consumeFrom(ItemContainer container) {
        if (requirements == null) return;
        for (MaterialRequirement req : requirements) {
            container.removeItemStack(new ItemStack(req.itemType(), req.amount()));
        }
    }

    public String getPrefabId() { return prefabId; }
    public List<MaterialRequirement> getRequirements() { return requirements; }

    public record MaterialRequirement(
        @SerializedName("item") String itemType,
        @SerializedName("amount") int amount
    ) {}
}
