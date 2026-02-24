<img src="https://r2cdn.perplexity.ai/pplx-full-logo-primary-dark%402x.png" style="height:64px;margin-right:32px"/>

# Je veux Faire un  plugin pour hytale, qui va permettre de cree un prefab que l'on vera en ologramme. Pour cela l'admin prendra un bloc outils dans hytale, sur lequel il choisie le  prefabrique qui devras etre visible en hologram. Puis pour que le prefab se construit tout seul, un joueur lambda devras remplire un coffre avec les items nessésaire definie dans le prefab. Pour que le prefab ce genere et consomme les items au fure et a mesure de sa construction automatique

Voici une architecture complète pour ton plugin Hytale de construction automatique par préfab holographique. Le système s'appuie sur le `PrefabStore`, le `FeedbackConsumer`, et l'architecture ECS native de Hytale.

***

## Architecture globale

Le plugin se découpe en **4 modules interdépendants** :

1. **AdminTool** — l'item admin pour sélectionner et positionner un préfab
2. **HologramRenderer** — affiche le préfab en ghost translucide
3. **ChestLinker** — relie un coffre au hologramme et vérifie les ressources
4. **AutoBuilder** — déclenche la construction progressive via `FeedbackConsumer`

***

## Structure du projet

```
prefab-builder-plugin/
├── src/main/java/
│   ├── PrefabBuilderPlugin.java
│   ├── tool/
│   │   └── AdminToolHandler.java
│   ├── hologram/
│   │   └── HologramManager.java
│   ├── chest/
│   │   └── ChestLinker.java
│   ├── builder/
│   │   └── AutoBuilder.java
│   └── data/
│       ├── PrefabBuildSession.java
│       └── PrefabMaterialDef.java
├── prefabs/           ← .json des préfabs (via PrefabStore)
└── config/
    └── prefab_materials.json
```


***

## Plugin principal

```java
public class PrefabBuilderPlugin {
    private HologramManager hologramManager;
    private ChestLinker chestLinker;
    private AutoBuilder autoBuilder;

    public void setup() {
        hologramManager = new HologramManager();
        chestLinker = new ChestLinker();
        autoBuilder = new AutoBuilder(hologramManager, chestLinker);

        // Enregistrement des événements
        EventBus.register(PlayerInteractEvent.class, this::onPlayerInteract);
        EventBus.register(BlockPlaceEvent.class, this::onBlockPlace);
        EventBus.register(ChestCloseEvent.class, this::onChestClose);
    }

    private void onPlayerInteract(PlayerInteractEvent e) {
        if (AdminToolHandler.isAdminTool(e.getPlayer().getHeldItem())) {
            AdminToolHandler.handleInteract(e, hologramManager);
        }
    }

    private void onBlockPlace(BlockPlaceEvent e) {
        if (e.getBlockType() == BlockType.CHEST) {
            chestLinker.tryLinkChest(e.getPosition(), e.getWorld());
        }
    }

    private void onChestClose(ChestCloseEvent e) {
        chestLinker.checkAndStartBuild(e.getChestPosition(), autoBuilder);
    }
}
```


***

## 1. AdminToolHandler — L'outil admin

L'admin reçoit un item spécial. En faisant un clic droit sur le sol, une UI s'ouvre pour choisir le préfab.[^1_1][^1_2]

```java
public class AdminToolHandler {

    public static boolean isAdminTool(ItemStack item) {
        return item != null
            && item.hasTag("prefab_builder_tool");
    }

    public static void handleInteract(PlayerInteractEvent e, HologramManager hm) {
        Player player = e.getPlayer();
        World world = e.getWorld();
        Vector3i targetPos = e.getTargetBlock();

        // Ouvre une UI de sélection de préfab (liste des prefabs serveur)
        List<String> availablePrefabs = PrefabStore.getInstance()
            .getServerPrefabDir("builder/")   // dossier prefabs/builder/
            .stream()
            .map(BlockSelection::getName)
            .toList();

        player.openSelectionUI("Choisir un préfab", availablePrefabs, (chosen) -> {
            BlockSelection selection = PrefabStore.getInstance()
                .getServerPrefab("builder/" + chosen);

            if (selection != null) {
                hm.spawnHologram(world, targetPos, selection, chosen);
                player.sendMessage("Hologramme du préfab '" + chosen + "' positionné !");
            }
        });
    }
}
```


***

## 2. HologramManager — Le rendu holographique

Hytale ne dispose pas encore d'un rendu hologramme natif documenté publiquement.  La meilleure approche actuelle consiste à placer des **entités "display"** transparentes bloc par bloc via `BlockSelection.forEachBlock()`, ou à utiliser des particules périodiques pour simuler le contour.[^1_3][^1_1]

```java
public class HologramManager {
    // Map Position → session active
    private final Map<Vector3i, PrefabBuildSession> activeSessions = new ConcurrentHashMap<>();

    public void spawnHologram(World world, Vector3i origin,
                               BlockSelection selection, String prefabName) {
        // Itère chaque bloc du préfab pour spawner une entité "ghost"
        selection.forEachBlock((lx, ly, lz, blockHolder) -> {
            Vector3i worldPos = new Vector3i(
                origin.x + lx,
                origin.y + ly,
                origin.z + lz
            );
            // Spawne un bloc fantôme (translucide) via une entité display
            spawnGhostBlock(world, worldPos, blockHolder.blockId());
        });

        // Enregistre la session
        PrefabBuildSession session = new PrefabBuildSession(
            prefabName, selection, origin, world
        );
        activeSessions.put(origin, session);
    }

    private void spawnGhostBlock(World world, Vector3i pos, int blockId) {
        EntityStore ghost = EntityFactory.create("hologram_block");
        ghost.setComponent(PositionComponent.of(pos));
        ghost.setComponent(GhostBlockComponent.of(blockId, 0.4f)); // 40% opacité
        world.spawnEntity(ghost);
    }

    public void removeHologram(Vector3i origin) {
        PrefabBuildSession session = activeSessions.get(origin);
        if (session != null) {
            session.getGhostEntities().forEach(Entity::remove);
            activeSessions.remove(origin);
        }
    }

    public @Nullable PrefabBuildSession getSession(Vector3i pos) {
        // Cherche une session dont l'origine est à moins de 10 blocs
        return activeSessions.values().stream()
            .filter(s -> s.getOrigin().distanceTo(pos) < 10)
            .findFirst().orElse(null);
    }
}
```


***

## 3. ChestLinker — Liaison coffre ↔ hologramme

```java
public class ChestLinker {
    private final Map<Vector3i, PrefabBuildSession> linkedChests = new HashMap<>();

    public void tryLinkChest(Vector3i chestPos, World world) {
        // Délégué à HologramManager pour trouver le hologramme le plus proche
        // (injection via constructeur en pratique)
    }

    public void tryLinkChest(Vector3i chestPos, World world,
                              HologramManager hm) {
        PrefabBuildSession session = hm.getSession(chestPos);
        if (session != null && session.getLinkedChest() == null) {
            session.setLinkedChest(chestPos);
            linkedChests.put(chestPos, session);
            // Notifie les joueurs proches
            world.getNearbyPlayers(chestPos, 15).forEach(p ->
                p.sendMessage("Coffre lié au préfab '" + session.getPrefabName()
                    + "'. Remplis-le pour commencer la construction !")
            );
        }
    }

    public void checkAndStartBuild(Vector3i chestPos, AutoBuilder builder) {
        PrefabBuildSession session = linkedChests.get(chestPos);
        if (session == null || session.isBuilding()) return;

        Inventory chestInv = session.getWorld()
            .getChestInventory(chestPos);

        // Charge les matériaux requis pour ce préfab
        PrefabMaterialDef materials = PrefabMaterialDef
            .loadFor(session.getPrefabName());

        if (materials.isSatisfiedBy(chestInv)) {
            session.setBuilding(true);
            builder.startBuild(session, chestInv, materials);
        } else {
            // Affiche ce qui manque
            String missing = materials.getMissingItemsReport(chestInv);
            session.getWorld()
                .getNearbyPlayers(chestPos, 15)
                .forEach(p -> p.sendMessage("Ressources manquantes : " + missing));
        }
    }
}
```


***

## 4. AutoBuilder — Construction progressive

C'est le cœur du plugin. Il utilise `placeNoReturn()` avec un `FeedbackConsumer` personnalisé pour placer les blocs **par batch** et consommer les items du coffre au fur et à mesure.[^1_4]

```java
public class AutoBuilder {

    public void startBuild(PrefabBuildSession session,
                           Inventory chestInv,
                           PrefabMaterialDef materials) {

        BlockSelection prefab = session.getSelection();
        World world = session.getWorld();
        Vector3i origin = session.getOrigin();
        int totalBlocks = prefab.getBlockCount();

        // FeedbackConsumer : appelé à chaque batch de blocs placés
        FeedbackConsumer progressCallback = (feedbackKey, total, current,
                                              sender, accessor) -> {
            // Calcule le ratio de progression
            float ratio = (float) current / total;

            // Consomme les items proportionnellement
            consumeItemsForProgress(chestInv, materials, ratio,
                                    session.getLastConsumeRatio());
            session.setLastConsumeRatio(ratio);

            // Supprime les blocs fantômes déjà construits
            session.removeGhostBlocksUpTo(current);

            // Feedback aux joueurs proches
            int percent = (int)(ratio * 100);
            world.getNearbyPlayers(origin, 30)
                 .forEach(p -> p.sendActionBar("Construction : " + percent + "%"));

            // Construction terminée
            if (current >= total) {
                session.setBuilding(false);
                session.getHologramManager().removeHologram(origin);
                world.getNearbyPlayers(origin, 30)
                     .forEach(p -> p.sendMessage("✅ Construction terminée !"));
            }
        };

        // Lance la construction asynchrone avec progress
        prefab.placeNoReturn(
            "prefab_build_" + session.getPrefabName(),  // feedbackKey
            null,                                        // CommandSender (null = serveur)
            progressCallback,
            world,
            origin,
            BlockMask.AIR_ONLY,                          // ne remplace que l'air
            world.getComponentAccessor()
        );
    }

    private void consumeItemsForProgress(Inventory inv, PrefabMaterialDef materials,
                                          float newRatio, float oldRatio) {
        float delta = newRatio - oldRatio;
        if (delta <= 0) return;

        for (MaterialRequirement req : materials.getRequirements()) {
            int toConsume = Math.round(req.amount() * delta);
            if (toConsume > 0) {
                inv.removeItems(req.itemType(), toConsume);
            }
        }
    }
}
```


***

## 5. Définition des matériaux (JSON)

Chaque préfab a un fichier `config/materials/<prefab_name>.json` :[^1_5]

```json
{
  "prefab": "maison_bois",
  "requirements": [
    { "item": "OAK_PLANKS",  "amount": 64 },
    { "item": "OAK_LOG",     "amount": 24 },
    { "item": "GLASS_PANE",  "amount": 16 },
    { "item": "STONE_SLAB",  "amount": 32 },
    { "item": "OAK_DOOR",    "amount": 2  }
  ]
}
```

```java
public record MaterialRequirement(String itemType, int amount) {}

public class PrefabMaterialDef {
    private final List<MaterialRequirement> requirements;

    public static PrefabMaterialDef loadFor(String prefabName) {
        // Lit le JSON depuis config/materials/<prefabName>.json
        return JsonLoader.load("config/materials/" + prefabName + ".json",
                               PrefabMaterialDef.class);
    }

    public boolean isSatisfiedBy(Inventory inv) {
        return requirements.stream().allMatch(req ->
            inv.countItems(req.itemType()) >= req.amount()
        );
    }

    public String getMissingItemsReport(Inventory inv) {
        return requirements.stream()
            .filter(req -> inv.countItems(req.itemType()) < req.amount())
            .map(req -> req.itemType() + " x"
                + (req.amount() - inv.countItems(req.itemType())))
            .collect(Collectors.joining(", "));
    }
}
```


***

## Flux complet résumé

```
[Admin] Clic outil → Sélection préfab → HologramManager.spawnHologram()
                                              ↓
                                   Hologramme visible (ghost blocks)
                                              ↓
[Joueur] Pose un coffre à proximité → ChestLinker.tryLinkChest()
                                              ↓
                                   Coffre lié + message ressources
                                              ↓
[Joueur] Remplit le coffre + ferme → ChestLinker.checkAndStartBuild()
                                              ↓
                               PrefabMaterialDef.isSatisfiedBy() ✅
                                              ↓
                          AutoBuilder.startBuild() → placeNoReturn()
                                              ↓
                         FeedbackConsumer → bloc par bloc :
                           • Consomme items du coffre (ratio)
                           • Retire les ghost blocks construits
                           • Barre d'action "Construction X%"
                                              ↓
                                    Construction terminée ✅
```


***

## Points d'attention

- **Hologramme** : L'API de rendu client-side n'est pas encore publiquement documentée dans Hytale, donc les ghost blocks via des entités display sont la solution la plus robuste à ce stade.[^1_3]
- **Thread safety** : Le `BlockSelection` utilise un `ReentrantReadWriteLock`, assure-toi que la consommation d'items dans le coffre passe par le thread principal du monde concerné.[^1_4]
- **`BlockMask.AIR_ONLY`** : Utilise ce masque pour éviter que la construction écrase des blocs existants placés par les joueurs.[^1_4]
- **`FeedbackConsumer`** : Le paramètre `current` n'est pas nécessairement incrémenté de 1 à chaque appel (batches), donc calcule toujours via `ratio = current / total` plutôt qu'en comptant les appels.[^1_4]
<span style="display:none">[^1_10][^1_11][^1_12][^1_13][^1_14][^1_15][^1_6][^1_7][^1_8][^1_9]</span>

<div align="center">⁂</div>

[^1_1]: https://hytale-docs.pages.dev/modding/content/prefabs/

[^1_2]: https://www.hytalecommands.com/prefab

[^1_3]: https://hytale.com/news/2025/11/hytale-modding-strategy-and-status

[^1_4]: https://deepwiki.com/vulpeslab/hytale-docs/8.2-prefab-system

[^1_5]: https://hytale.game/en/prefab/

[^1_6]: https://hytale.game/en/guide-create-your-first-hytale-plugin/

[^1_7]: https://www.youtube.com/watch?v=qKI_6gFmnzA

[^1_8]: https://support.hytale.com/hc/en-us/articles/45326769420827-Hytale-Server-Manual

[^1_9]: https://www.hytale-dev.com/plugin-development

[^1_10]: https://hytale.game/le-mode-creatif-sur-hytale/

[^1_11]: https://hytaledocs.dev

[^1_12]: https://hytale.game/en/blockbench-and-asset-creation/

[^1_13]: https://www.hytalevault.dev/en/

[^1_14]: https://nexus-games.com/fr/blog/top-10-meilleurs-mods-hytale/

[^1_15]: https://www.reddit.com/r/HytaleInfo/comments/1qc8f9n/the_hytale_modding_bible_full_server_api_reference/


---

# donne moi le reste de code

Voici toutes les classes manquantes et les fichiers de configuration complets du plugin.

***

## `PrefabBuildSession.java`

```java
package com.yourname.prefabbuilder.data;

import com.hypixel.hytale.server.world.block.selection.BlockSelection;
import com.hypixel.hytale.server.world.World;
import com.hypixel.hytale.server.entity.EntityStore;
import com.hypixel.hytale.server.core.util.Holder;
import com.yourname.prefabbuilder.hologram.HologramManager;
import org.joml.Vector3i;

import java.util.ArrayList;
import java.util.List;

public class PrefabBuildSession {

    private final String prefabName;
    private final BlockSelection selection;
    private final Vector3i origin;
    private final World world;
    private final HologramManager hologramManager;

    private Vector3i linkedChest = null;
    private boolean building = false;
    private float lastConsumeRatio = 0f;

    // Stocke les entités ghost pour suppression progressive
    private final List<Holder<EntityStore>> ghostEntities = new ArrayList<>();
    // Associe un index de bloc à une entité ghost (ordre d'itération du préfab)
    private final List<Vector3i> ghostPositions = new ArrayList<>();

    public PrefabBuildSession(String prefabName, BlockSelection selection,
                               Vector3i origin, World world,
                               HologramManager hologramManager) {
        this.prefabName = prefabName;
        this.selection = selection;
        this.origin = origin;
        this.world = world;
        this.hologramManager = hologramManager;
    }

    /**
     * Supprime les entités ghost pour les blocs déjà construits.
     * Appelé par AutoBuilder via FeedbackConsumer à chaque batch.
     */
    public void removeGhostBlocksUpTo(int currentIndex) {
        int limit = Math.min(currentIndex, ghostPositions.size());
        for (int i = 0; i < limit; i++) {
            if (i < ghostEntities.size()) {
                Holder<EntityStore> ghost = ghostEntities.get(i);
                if (ghost != null) {
                    world.removeEntity(ghost);
                    ghostEntities.set(i, null); // évite double suppression
                }
            }
        }
    }

    // ─── Getters ───────────────────────────────────────────────

    public String getPrefabName()             { return prefabName; }
    public BlockSelection getSelection()      { return selection; }
    public Vector3i getOrigin()               { return origin; }
    public World getWorld()                   { return world; }
    public HologramManager getHologramManager() { return hologramManager; }
    public Vector3i getLinkedChest()          { return linkedChest; }
    public boolean isBuilding()               { return building; }
    public float getLastConsumeRatio()        { return lastConsumeRatio; }
    public List<Holder<EntityStore>> getGhostEntities() { return ghostEntities; }
    public List<Vector3i> getGhostPositions() { return ghostPositions; }

    // ─── Setters ───────────────────────────────────────────────

    public void setLinkedChest(Vector3i pos)      { this.linkedChest = pos; }
    public void setBuilding(boolean building)     { this.building = building; }
    public void setLastConsumeRatio(float ratio)  { this.lastConsumeRatio = ratio; }

    public void addGhostEntity(Vector3i worldPos, Holder<EntityStore> entity) {
        ghostPositions.add(worldPos);
        ghostEntities.add(entity);
    }
}
```


***

## `PrefabMaterialDef.java` (complet)

```java
package com.yourname.prefabbuilder.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import com.hypixel.hytale.server.world.inventory.Inventory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.List;
import java.util.stream.Collectors;

public class PrefabMaterialDef {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    @SerializedName("prefab")
    private String prefabName;

    @SerializedName("requirements")
    private List<MaterialRequirement> requirements;

    // ─── Chargement ────────────────────────────────────────────

    public static PrefabMaterialDef loadFor(String prefabName) {
        Path configPath = Paths.get("config", "materials", prefabName + ".json");

        if (!Files.exists(configPath)) {
            throw new IllegalStateException(
                "[PrefabBuilder] Fichier matériaux introuvable : " + configPath
            );
        }

        try (Reader reader = new InputStreamReader(
                Files.newInputStream(configPath), StandardCharsets.UTF_8)) {
            return GSON.fromJson(reader, PrefabMaterialDef.class);
        } catch (IOException e) {
            throw new RuntimeException(
                "[PrefabBuilder] Erreur lecture matériaux pour " + prefabName, e
            );
        }
    }

    // ─── Création d'un template vide ──────────────────────────

    public static void generateTemplate(String prefabName) {
        Path dir = Paths.get("config", "materials");
        Path file = dir.resolve(prefabName + ".json");
        try {
            Files.createDirectories(dir);
            if (!Files.exists(file)) {
                PrefabMaterialDef template = new PrefabMaterialDef();
                template.prefabName = prefabName;
                template.requirements = List.of(
                    new MaterialRequirement("OAK_PLANKS", 64),
                    new MaterialRequirement("OAK_LOG", 16)
                );
                try (Writer w = new OutputStreamWriter(
                        Files.newOutputStream(file), StandardCharsets.UTF_8)) {
                    GSON.toJson(template, w);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(
                "[PrefabBuilder] Impossible de créer le template : " + prefabName, e
            );
        }
    }

    // ─── Validation ────────────────────────────────────────────

    /**
     * Vérifie que le coffre contient TOUS les items nécessaires.
     */
    public boolean isSatisfiedBy(Inventory inv) {
        return requirements.stream().allMatch(req ->
            inv.countItems(req.itemType()) >= req.amount()
        );
    }

    /**
     * Retourne un message listant les items manquants et leurs quantités.
     */
    public String getMissingItemsReport(Inventory inv) {
        return requirements.stream()
            .filter(req -> inv.countItems(req.itemType()) < req.amount())
            .map(req -> {
                int manque = req.amount() - inv.countItems(req.itemType());
                return "§c" + req.itemType() + " x" + manque;
            })
            .collect(Collectors.joining(", "));
    }

    /**
     * Calcule le total de tous les items requis (pour progress display).
     */
    public int getTotalItemCount() {
        return requirements.stream().mapToInt(MaterialRequirement::amount).sum();
    }

    // ─── Getters ───────────────────────────────────────────────

    public String getPrefabName()                    { return prefabName; }
    public List<MaterialRequirement> getRequirements() { return requirements; }
}
```


***

## `MaterialRequirement.java`

```java
package com.yourname.prefabbuilder.data;

import com.google.gson.annotations.SerializedName;

public record MaterialRequirement(
    @SerializedName("item")   String itemType,
    @SerializedName("amount") int amount
) {}
```


***

## `HologramManager.java` (complet \& corrigé)

```java
package com.yourname.prefabbuilder.hologram;

import com.hypixel.hytale.server.world.World;
import com.hypixel.hytale.server.world.block.selection.BlockSelection;
import com.hypixel.hytale.server.entity.EntityStore;
import com.hypixel.hytale.server.core.util.Holder;
import com.yourname.prefabbuilder.data.PrefabBuildSession;
import org.joml.Vector3i;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class HologramManager {

    // Clé = position d'origine du hologramme
    private final Map<Vector3i, PrefabBuildSession> activeSessions = new ConcurrentHashMap<>();

    /**
     * Crée le hologramme ghost pour un préfab à l'origine donnée.
     * Itère chaque bloc du préfab et spawne une entité display translucide.
     */
    public PrefabBuildSession spawnHologram(World world, Vector3i origin,
                                             BlockSelection selection, String prefabName) {
        PrefabBuildSession session = new PrefabBuildSession(
            prefabName, selection, origin, world, this
        );

        selection.forEachBlock((lx, ly, lz, blockHolder) -> {
            // Ignore les blocs AIR dans le hologramme
            if (blockHolder.blockId() == 0) return;

            Vector3i worldPos = new Vector3i(
                origin.x + lx,
                origin.y + ly,
                origin.z + lz
            );

            // Crée une entité display ghost (translucide) pour ce bloc
            Holder<EntityStore> ghostEntity = spawnGhostBlock(
                world, worldPos, blockHolder.blockId()
            );
            session.addGhostEntity(worldPos, ghostEntity);
        });

        activeSessions.put(origin, session);
        return session;
    }

    /**
     * Spawne un bloc fantôme via une entité GhostBlock display.
     * L'opacité est réglée à 40% pour l'effet hologramme.
     */
    private Holder<EntityStore> spawnGhostBlock(World world, Vector3i pos, int blockId) {
        EntityStore ghost = world.createEntity();

        ghost.setComponent(
            world.getComponentType("position"),
            pos.x + 0.5, pos.y, pos.z + 0.5  // centré dans le bloc
        );
        ghost.setComponent(
            world.getComponentType("ghost_block_display"),
            blockId, 0.4f // blockId + opacité 40%
        );

        return world.spawnEntity(ghost);
    }

    /**
     * Supprime complètement le hologramme (fin de construction).
     */
    public void removeHologram(Vector3i origin) {
        PrefabBuildSession session = activeSessions.remove(origin);
        if (session != null) {
            session.getGhostEntities().forEach(entity -> {
                if (entity != null) {
                    session.getWorld().removeEntity(entity);
                }
            });
        }
    }

    /**
     * Trouve la session la plus proche d'une position (rayon 10 blocs).
     * Utilisé pour relier un coffre à son hologramme.
     */
    public PrefabBuildSession getSessionNear(Vector3i pos) {
        return activeSessions.values().stream()
            .filter(s -> s.getOrigin().distance(pos) < 10.0)
            .findFirst()
            .orElse(null);
    }

    public Collection<PrefabBuildSession> getAllSessions() {
        return activeSessions.values();
    }

    public boolean hasSession(Vector3i origin) {
        return activeSessions.containsKey(origin);
    }
}
```


***

## `ChestLinker.java` (complet \& corrigé)

```java
package com.yourname.prefabbuilder.chest;

import com.hypixel.hytale.server.world.World;
import com.hypixel.hytale.server.world.inventory.Inventory;
import com.yourname.prefabbuilder.builder.AutoBuilder;
import com.yourname.prefabbuilder.data.PrefabBuildSession;
import com.yourname.prefabbuilder.data.PrefabMaterialDef;
import com.yourname.prefabbuilder.hologram.HologramManager;
import org.joml.Vector3i;

import java.util.HashMap;
import java.util.Map;

public class ChestLinker {

    // coffre → session
    private final Map<Vector3i, PrefabBuildSession> linkedChests = new HashMap<>();
    private final HologramManager hologramManager;

    public ChestLinker(HologramManager hologramManager) {
        this.hologramManager = hologramManager;
    }

    /**
     * Appelé quand un joueur pose un coffre dans le monde.
     * Essaie de le lier au hologramme le plus proche (<10 blocs).
     */
    public void tryLinkChest(Vector3i chestPos, World world) {
        PrefabBuildSession session = hologramManager.getSessionNear(chestPos);

        if (session == null) return;              // pas de hologramme proche
        if (session.getLinkedChest() != null) return; // déjà un coffre lié
        if (linkedChests.containsKey(chestPos)) return; // coffre déjà lié

        session.setLinkedChest(chestPos);
        linkedChests.put(chestPos, session);

        // Charge les matériaux pour construire le rapport de ressources
        PrefabMaterialDef materials;
        try {
            materials = PrefabMaterialDef.loadFor(session.getPrefabName());
        } catch (IllegalStateException e) {
            // Génère un template si le fichier n'existe pas encore
            PrefabMaterialDef.generateTemplate(session.getPrefabName());
            world.getNearbyPlayers(chestPos, 15).forEach(p ->
                p.sendMessage("§eTemplate de matériaux généré pour '"
                    + session.getPrefabName()
                    + "'. Éditez config/materials/"
                    + session.getPrefabName() + ".json")
            );
            return;
        }

        // Informe les joueurs à proximité
        String itemList = materials.getRequirements().stream()
            .map(r -> "§f" + r.amount() + "x §e" + r.itemType())
            .reduce((a, b) -> a + "§7, " + b)
            .orElse("aucun item");

        world.getNearbyPlayers(chestPos, 15).forEach(p -> {
            p.sendMessage("§a✔ Coffre lié au préfab §6'" + session.getPrefabName() + "'§a !");
            p.sendMessage("§7Items requis : " + itemList);
            p.sendMessage("§7Ferme le coffre une fois rempli pour démarrer la construction.");
        });
    }

    /**
     * Appelé quand un joueur ferme un coffre.
     * Lance la construction si les ressources sont suffisantes.
     */
    public void checkAndStartBuild(Vector3i chestPos, AutoBuilder builder) {
        PrefabBuildSession session = linkedChests.get(chestPos);
        if (session == null || session.isBuilding()) return;

        Inventory chestInv = session.getWorld().getChestInventory(chestPos);
        if (chestInv == null) return;

        PrefabMaterialDef materials;
        try {
            materials = PrefabMaterialDef.loadFor(session.getPrefabName());
        } catch (Exception e) {
            return;
        }

        if (materials.isSatisfiedBy(chestInv)) {
            session.setBuilding(true);
            session.getWorld().getNearbyPlayers(chestPos, 20).forEach(p ->
                p.sendMessage("§b⚙ Construction du préfab '§6"
                    + session.getPrefabName() + "§b' démarrée !")
            );
            builder.startBuild(session, chestInv, materials);

        } else {
            String missing = materials.getMissingItemsReport(chestInv);
            session.getWorld().getNearbyPlayers(chestPos, 15).forEach(p ->
                p.sendMessage("§c✘ Ressources insuffisantes : " + missing)
            );
        }
    }

    public void removeChestLink(Vector3i chestPos) {
        linkedChests.remove(chestPos);
    }
}
```


***

## `AutoBuilder.java` (complet \& corrigé)

```java
package com.yourname.prefabbuilder.builder;

import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.world.block.selection.BlockMask;
import com.hypixel.hytale.server.world.block.selection.FeedbackConsumer;
import com.yourname.prefabbuilder.data.MaterialRequirement;
import com.yourname.prefabbuilder.data.PrefabBuildSession;
import com.yourname.prefabbuilder.data.PrefabMaterialDef;
import com.hypixel.hytale.server.world.inventory.Inventory;

public class AutoBuilder {

    /**
     * Démarre la construction progressive du préfab.
     * Les items du coffre sont consommés proportionnellement à l'avancement.
     */
    public void startBuild(PrefabBuildSession session,
                           Inventory chestInv,
                           PrefabMaterialDef materials) {

        String feedbackKey = "build_" + session.getPrefabName()
                             + "_" + System.currentTimeMillis();

        // FeedbackConsumer : reçoit (feedbackKey, total, current, sender, accessor)
        FeedbackConsumer progressCallback =
            (key, total, current, sender, accessor) -> {

                // Calcul du ratio d'avancement (0.0 → 1.0)
                float newRatio = (total > 0) ? (float) current / total : 0f;
                float delta = newRatio - session.getLastConsumeRatio();

                // Consomme les items du coffre proportionnellement
                if (delta > 0f) {
                    consumeItemsForDelta(chestInv, materials, delta);
                    session.setLastConsumeRatio(newRatio);
                }

                // Retire les blocs ghost déjà construits
                session.removeGhostBlocksUpTo(current);

                // Barre d'action pour les joueurs proches
                int percent = (int)(newRatio * 100);
                String bar = buildProgressBar(percent);
                session.getWorld()
                    .getNearbyPlayers(session.getOrigin(), 30)
                    .forEach(p -> p.sendActionBar("§b Construction §7" + bar + " §b" + percent + "%"));

                // Fin de construction
                if (current >= total) {
                    onBuildComplete(session);
                }
            };

        // Lance le placement asynchrone via placeNoReturn avec BlockMask AIR_ONLY
        // (ne remplace QUE les blocs d'air, préserve les blocs existants)
        session.getSelection().placeNoReturn(
            feedbackKey,
            (CommandSender) null,   // pas de sender spécifique
            progressCallback,
            session.getWorld(),
            session.getOrigin(),
            BlockMask.AIR_ONLY,
            session.getWorld().getComponentAccessor()
        );
    }

    /**
     * Consomme les items du coffre pour un delta de progression donné.
     * Ex: delta=0.1 → consomme 10% de chaque stack requis.
     */
    private void consumeItemsForDelta(Inventory inv,
                                       PrefabMaterialDef materials,
                                       float delta) {
        for (MaterialRequirement req : materials.getRequirements()) {
            int toConsume = Math.round(req.amount() * delta);
            if (toConsume > 0) {
                inv.removeItems(req.itemType(), toConsume);
            }
        }
    }

    /**
     * Appelé une fois la construction terminée à 100%.
     */
    private void onBuildComplete(PrefabBuildSession session) {
        session.setBuilding(false);

        // Supprime tous les ghost restants
        session.getHologramManager().removeHologram(session.getOrigin());

        session.getWorld()
            .getNearbyPlayers(session.getOrigin(), 30)
            .forEach(p -> {
                p.sendMessage("§a§l✔ Construction de '"
                    + session.getPrefabName() + "' terminée !");
                p.sendActionBar("§a§l✔ Construction terminée !");
            });
    }

    /**
     * Génère une barre de progression ASCII.
     * Ex: [████████░░] 80%
     */
    private String buildProgressBar(int percent) {
        int filled = percent / 10;
        StringBuilder bar = new StringBuilder("§7[");
        for (int i = 0; i < 10; i++) {
            bar.append(i < filled ? "§b█" : "§8░");
        }
        bar.append("§7]");
        return bar.toString();
    }
}
```


***

## Commande admin `/prefabbuilder`

```java
package com.yourname.prefabbuilder.command;

import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.entity.player.Player;
import com.hypixel.hytale.server.world.item.ItemStack;

import java.util.concurrent.CompletableFuture;

public class PrefabBuilderCommand extends AbstractCommand {

    public PrefabBuilderCommand() {
        super("prefabbuilder", "Gère le plugin PrefabBuilder.");
        setPermission("prefabbuilder.admin");
        addAlias("pb");
    }

    @Override
    protected CompletableFuture<Void> execute(CommandContext ctx) {
        if (!(ctx.sender() instanceof Player player)) {
            ctx.sender().sendMessage(Message.raw("§cCommande réservée aux joueurs."));
            return null;
        }

        String[] args = ctx.args();

        if (args.length == 0) {
            sendHelp(player);
            return null;
        }

        switch (args[^2_0].toLowerCase()) {

            // /pb give → donne l'outil admin au joueur
            case "give" -> {
                ItemStack tool = ItemStack.of("WAND_ITEM") // item de type baguette
                    .withTag("prefab_builder_tool", true)
                    .withDisplayName("§6⚙ Outil Préfab Builder")
                    .withLore(
                        "§7Clic droit sur le sol pour",
                        "§7choisir et placer un hologramme.",
                        "§eAdmin uniquement."
                    )
                    .build();
                player.getInventory().addItem(tool);
                player.sendMessage("§a✔ Outil Préfab Builder ajouté à votre inventaire.");
            }

            // /pb list → liste les préfabs disponibles
            case "list" -> {
                var prefabs = com.hypixel.hytale.server.world.block.selection.PrefabStore
                    .getInstance()
                    .getServerPrefabDir("builder/");

                if (prefabs.isEmpty()) {
                    player.sendMessage("§cAucun préfab dans prefabs/builder/");
                } else {
                    player.sendMessage("§6Préfabs disponibles :");
                    prefabs.forEach(p ->
                        player.sendMessage("§7 - §e" + p.getName())
                    );
                }
            }

            // /pb reload → recharge les matériaux depuis les JSON
            case "reload" -> {
                player.sendMessage("§aMatériaux rechargés (cache JSON vidé).");
                // PrefabMaterialDef utilise Files.newInputStream à chaque appel,
                // pas de cache à vider côté plugin
            }

            default -> sendHelp(player);
        }

        return null;
    }

    private void sendHelp(Player player) {
        player.sendMessage("§6§lPrefab Builder §7v1.0");
        player.sendMessage("§e/pb give §7- Obtenir l'outil admin");
        player.sendMessage("§e/pb list §7- Lister les préfabs disponibles");
        player.sendMessage("§e/pb reload §7- Recharger les configs");
    }
}
```


***

## `PrefabBuilderPlugin.java` (classe principale)

```java
package com.yourname.prefabbuilder;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.yourname.prefabbuilder.builder.AutoBuilder;
import com.yourname.prefabbuilder.chest.ChestLinker;
import com.yourname.prefabbuilder.command.PrefabBuilderCommand;
import com.yourname.prefabbuilder.hologram.HologramManager;
import com.yourname.prefabbuilder.tool.AdminToolHandler;

import java.util.logging.Level;

public class PrefabBuilderPlugin extends JavaPlugin {

    private HologramManager hologramManager;
    private ChestLinker chestLinker;
    private AutoBuilder autoBuilder;

    @Override
    public void start() {
        getLogger().at(Level.INFO).log("PrefabBuilder - Démarrage...");

        hologramManager = new HologramManager();
        autoBuilder     = new AutoBuilder();
        chestLinker     = new ChestLinker(hologramManager);

        // Commandes
        getCommandRegistry().registerCommand(new PrefabBuilderCommand());

        // Événements
        getEventBus().register(PlayerInteractEvent.class, this::onPlayerInteract);
        getEventBus().register(BlockPlaceEvent.class,     this::onBlockPlace);
        getEventBus().register(ChestCloseEvent.class,     this::onChestClose);
        getEventBus().register(BlockBreakEvent.class,     this::onBlockBreak);

        getLogger().at(Level.INFO).log("PrefabBuilder - Prêt !");
    }

    @Override
    public void shutdown() {
        // Nettoie tous les holograms actifs
        hologramManager.getAllSessions().forEach(session ->
            hologramManager.removeHologram(session.getOrigin())
        );
        getLogger().at(Level.INFO).log("PrefabBuilder - Arrêté proprement.");
    }

    // ─── Handlers ──────────────────────────────────────────────

    private void onPlayerInteract(PlayerInteractEvent e) {
        if (AdminToolHandler.isAdminTool(e.getPlayer().getHeldItem())) {
            AdminToolHandler.handleInteract(e, hologramManager);
        }
    }

    private void onBlockPlace(BlockPlaceEvent e) {
        if (e.getBlockType() == BlockType.CHEST) {
            chestLinker.tryLinkChest(e.getPosition(), e.getWorld());
        }
    }

    private void onChestClose(ChestCloseEvent e) {
        chestLinker.checkAndStartBuild(e.getChestPosition(), autoBuilder);
    }

    private void onBlockBreak(BlockBreakEvent e) {
        // Si on casse un coffre lié, nettoie le lien
        chestLinker.removeChestLink(e.getPosition());
    }
}
```


***

## `manifest.json`

```json
{
  "Name": "PrefabBuilder",
  "Version": "1.0.0",
  "Main": "com.yourname.prefabbuilder.PrefabBuilderPlugin",
  "Description": "Construit des préfabs automatiquement via un coffre lié à un hologramme.",
  "Authors": [{ "Name": "TonPseudo" }],
  "Dependencies": {},
  "OptionalDependencies": {}
}
```


***

## `build.gradle`

```groovy
plugins {
    id 'java'
}

group   = 'com.yourname'
version = '1.0.0'

repositories {
    mavenCentral()
    maven { url 'https://repo.hytale.com/repository/public/' }
}

dependencies {
    compileOnly 'com.hypixel.hytale:server-api:latest.release'
    implementation 'com.google.code.gson:gson:2.11.0'
}

java {
    toolchain { languageVersion = JavaLanguageVersion.of(25) }
}

jar {
    archiveFileName = "PrefabBuilder-${version}.jar"
    from configurations.runtimeClasspath.collect {
        it.isDirectory() ? it : zipTree(it)
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
```


***

## Structure finale du projet

```
prefab-builder-plugin/
├── src/main/
│   ├── java/com/yourname/prefabbuilder/
│   │   ├── PrefabBuilderPlugin.java
│   │   ├── tool/
│   │   │   └── AdminToolHandler.java
│   │   ├── hologram/
│   │   │   └── HologramManager.java
│   │   ├── chest/
│   │   │   └── ChestLinker.java
│   │   ├── builder/
│   │   │   └── AutoBuilder.java
│   │   ├── command/
│   │   │   └── PrefabBuilderCommand.java
│   │   └── data/
│   │       ├── PrefabBuildSession.java
│   │       ├── PrefabMaterialDef.java
│   │       └── MaterialRequirement.java
│   └── resources/
│       └── manifest.json
├── prefabs/builder/          ← tes fichiers .prefab
├── config/materials/         ← JSON de matériaux par préfab
│   └── maison_bois.json
├── build.gradle
└── settings.gradle
```


***

## Points clés à retenir

- **`placeNoReturn()` est asynchrone** : le `FeedbackConsumer` peut être appelé depuis un thread différent — accède toujours à l'inventaire du coffre via le thread principal du world en utilisant `world.runOnMainThread()` si tu observes des race conditions.[^2_1]
- **`BlockMask.AIR_ONLY`** empêche d'écraser les blocs posés par les joueurs pendant la construction.[^2_1]
- **`PrefabLoadException`** doit être catchée avec son type `NOT_FOUND` lors du `getServerPrefab()` pour une gestion d'erreur propre.[^2_1]
- **Java 25 JDK** est obligatoire pour compiler ce plugin.[^2_2]

<div align="center">⁂</div>

[^2_1]: https://deepwiki.com/vulpeslab/hytale-docs/8.2-prefab-system

[^2_2]: https://hytale.game/en/guide-create-your-first-hytale-plugin/

