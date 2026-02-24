# Instructions pour Gemini CLI - Projet Plugin Hytale

## ⚠️ IMPORTANT : Hytale n'est PAS Minecraft
Hytale est un jeu complètement distinct avec sa propre API et ses propres conventions. **NE JAMAIS** appliquer les patterns Minecraft (Bukkit/Spigot/Paper/Forge/Fabric).

## Informations Générales du Projet
- **Type de projet** : Plugin Hytale
- **Langage** : Java
- **Build Tool** : Gradle
- **IDE** : IntelliJ IDEA
- **API** : Hytale Plugin API

## ⚠️ Différences Critiques avec Minecraft
- ❌ **PAS de fichiers YAML** (plugin.yml, config.yml) - Hytale utilise son propre système
- ❌ **PAS de CommandExecutor** - Système de commandes différent
- ❌ **PAS d'annotations @EventHandler** - Système d'événements propre à Hytale
- ❌ **PAS de plugin.yml** - Configuration différente
- ✅ **Architecture ECS** (Entité-Composant-Système) - Propre à Hytale

---

## Documentation Hytale

### Mise à Jour de la Documentation
**OBLIGATOIRE** : Après chaque développement significatif, mettre à jour `hytale_docs/` avec :
- Nouvelles découvertes sur l'API Hytale
- Comportements observés spécifiques à Hytale
- Limitations rencontrées
- Exemples de code fonctionnels testés

### Ordre de Consultation (du plus important au moins important)
1. **📁 Documentation locale Hytale** : `hytale_docs/` (submodule Git)
2. **📁 Code source du jeu décompilé** : `C:\Users\jonat\Documents\Hytal\Plugins\Hytale-Server-Unpacked`
3. **🌐 Documentation officielle** : `https://hytalemodding.dev/en/docs`
4. **📁 Exemples du projet** : `FReZ-Hytale-mod-HelloWorld/src`

### Processus de Recherche Obligatoire
Avant TOUTE modification de code ou UI :
1. ✅ Consulter `hytale_docs/**` pour la fonctionnalité concernée
2. ✅ Vérifier les exemples existants dans le projet
3. ✅ Consulter le code source décompilé si nécessaire
4. ❌ **NE JAMAIS** faire d'hypothèses sur l'existence de propriétés/méthodes
5. ❌ **NE JAMAIS** inventer des API ou comportements
6. ❌ **NE JAMAIS** appliquer des patterns Minecraft

## Implémentation du Code

Lors de l'implémentation de code, veuillez respecter les directives suivantes :

- **Recommandations Hytale :** Suivez scrupuleusement les recommandations et les meilleures pratiques fournies par Hytale pour le développement de mods et de plugins.
- **Consignes des Tâches :** Référez-vous au dossier `tasks/` pour les spécifications fonctionnelles et les récits d'utilisateurs (stories) qui guident le développement.
- **Architecture du Projet :** Consultez le dossier `docs/architecture/` (en commençant par le `README.md`) pour comprendre la structure architecturale du projet et vous y conformer.
- **Commentaires pour développeur junior :** Tout le code doit être abondamment commenté, de manière claire et concise, afin qu'un développeur junior puisse facilement comprendre la logique, l'objectif de chaque section et les choix d'implémentation. Les commentaires doivent expliquer *pourquoi* le code fait ce qu'il fait, et non seulement *ce qu'il fait*.
- **Codes Couleur :** Ne pas utiliser les codes couleur Minecraft ('§c', '§a', etc.). Utiliser les utilitaires de formatage et de couleur fournis par l'API Hytale (ex: `MessageUtil` ou objets `Message`) pour assurer la compatibilité et le rendu correct des couleurs en jeu.

Ces instructions sont essentielles pour maintenir la cohérence, la qualité et la maintenabilité du projet.

### Règle #1 : Architecture ECS (Entité-Composant-Système)

### Principe Fondamental
**Priorité absolue à l'architecture ECS pour Hytale**

### Structure de Base d'un Plugin Hytale

#### Classe Principale du Plugin[.gitmodules](.gitmodules)
Tout plugin Hytale commence par une classe qui étend `JavaPlugin` :

```java
package com.exemple.monplugin;

import com.hytale.plugin.JavaPlugin;

/**
 * Classe principale du plugin.
 *
 * Cette classe sert de point d'entrée pour le plugin Hytale.
 * Elle gère le cycle de vie du plugin à travers trois méthodes principales :
 * - setup() : Initialisation avant le démarrage complet
 * - start() : Démarrage effectif du plugin
 * - shutdown() : Arrêt propre du plugin
 */
public class MonPluginPlugin extends JavaPlugin {

   /**
    * Constructeur du plugin.
    *
    * Appelé lors de l'instanciation du plugin par Hytale.
    * Utilisé pour l'initialisation de base des variables membres.
    */
   public MonPluginPlugin() {
      // Initialisation de base ici
      // Pas de logique lourde dans le constructeur
   }

   /**
    * Phase de configuration du plugin.
    *
    * Appelée AVANT start() pour permettre l'initialisation des configurations,
    * l'enregistrement des composants ECS et la préparation des ressources.
    *
    * À ce stade, le serveur n'est pas encore complètement démarré.
    */
   @Override
   public void setup() {
      // Charger les configurations
      // Enregistrer les composants ECS
      // Initialiser les systèmes
      // Préparer les ressources

      getLogger().info("Configuration du plugin " + getName());
   }

   /**
    * Démarrage du plugin.
    *
    * Appelée après setup() lorsque le serveur est prêt.
    * C'est ici qu'on active les fonctionnalités du plugin.
    *
    * Le serveur et les autres plugins sont maintenant accessibles.
    */
   @Override
   public void start() {
      // Activer les systèmes ECS
      // Enregistrer les event handlers
      // Démarrer les tâches périodiques
      // Activer les commandes

      getLogger().info("Démarrage du plugin " + getName());
   }

   /**
    * Arrêt du plugin.
    *
    * Appelée lors de l'arrêt du serveur ou du rechargement du plugin.
    * Nettoyer toutes les ressources et sauvegarder les données.
    *
    * IMPORTANT : Cette méthode doit s'exécuter rapidement pour ne pas
    * bloquer l'arrêt du serveur.
    */
   @Override
   public void shutdown() {
      // Arrêter les tâches en cours
      // Sauvegarder les données
      // Libérer les ressources
      // Désactiver les systèmes

      getLogger().info("Arrêt du plugin " + getName());
   }
}
```

### Bonnes Pratiques ECS dans Hytale

✅ **FAIRE** :
- Créer des composants réutilisables et composables
- Séparer données (Composants) et logique (Systèmes)
- Favoriser la composition plutôt que l'héritage
- Utiliser des systèmes pour la logique métier
- Enregistrer les composants dans `setup()`
- Activer les systèmes dans `start()`

❌ **ÉVITER** :
- Hiérarchies d'héritage profondes
- Logique dans les entités elles-mêmes
- Couplage fort entre composants
- God objects ou classes monolithiques
- Logique lourde dans le constructeur
- Blocage dans `shutdown()`

---

### Règle #2 : Organisation Fichiers/Dossiers

### Structure de Référence
**Adopter la structure de** : `FReZ-Hytale-mod-HelloWorld/src`

### Architecture Recommandée pour Plugin Hytale

```
src/main/
├── java/
│   └── com/exemple/monplugin/
│       ├── MonPluginPlugin.java       # Classe principale (extends JavaPlugin)
│       │
│       ├── components/                 # Composants ECS (données pures)
│       │   ├── HealthComponent.java
│       │   ├── PositionComponent.java
│       │   └── InventoryComponent.java
│       │
│       ├── systems/                    # Systèmes ECS (logique métier)
│       │   ├── CombatSystem.java
│       │   ├── MovementSystem.java
│       │   └── InventorySystem.java
│       │
│       ├── entities/                   # Définitions entités (assemblage)
│       │   ├── CustomEntity.java
│       │   └── EntityFactory.java
│       │
│       ├── events/                     # Gestionnaires d'événements Hytale
│       │   ├── PlayerEventHandler.java
│       │   └── EntityEventHandler.java
│       │
│       ├── commands/                   # Système de commandes Hytale
│       │   └── CustomCommand.java
│       │
│       ├── ui/                         # Interfaces utilisateur
│       │   ├── screens/
│       │   └── widgets/
│       │
│       ├── config/                     # Gestion de la configuration
│       │   └── ConfigManager.java
│       │
│       └── utils/                      # Utilitaires
│           ├── MessageUtil.java        # Formatage messages/couleurs
│           └── Logger.java
│
└── resources/
    └── Common/
        └── UI/
            └── Custom/
                └── Pages/              # Fichiers .ui pour les écrans
                    └── example_screen.ui

# Dossiers de documentation et spécifications
tasks/                                  # Spécifications fonctionnelles et stories
docs/
└── architecture/
    └── README.md                       # Architecture détaillée du projet
hytale_docs/                            # Submodule Git - Documentation Hytale
```
### Notes Importantes sur la Structure

#### Pas de Fichiers YAML
- ❌ **PAS de plugin.yml** (contrairement à Minecraft)
- ❌ **PAS de config.yml** par défaut
- ✅ Configuration gérée via l'API Hytale dans `setup()`

#### Point d'Entrée Unique
- La classe `MonPluginPlugin extends JavaPlugin` est le seul point d'entrée
- Nom de la classe : `{NomDuPlugin}Plugin` (convention)
- Trois méthodes clés : `setup()`, `start()`, `shutdown()`

---

### Règle #3 : Documentation Hytale
- Utiliser `hytale_docs` (qui est un submodule Git) ou `C:\Users\jonat\Documents\Hytal\Plugins\Hytale-Server-Unpacked` pour les références API.
- Mettre à jour la documentation Hytale systématiquement.

### Règle #4 : Validation et Tests
- Après chaque modification significative du code, exécuter la commande `./gradlew build` pour vérifier la compilation.
- Si des erreurs surviennent, les corriger en consultant le code source du jeu (si disponible) et la documentation Hytale.
### En Cas d'Erreur de Compilation

1. 📖 **Consulter `hytale_docs/`** pour l'API concernée
2. 🔍 **Vérifier le code source décompilé** dans `C:\Users\jonat\Documents\Hytal\Plugins\Hytale-Server-Unpacked`
3. 📚 **Consulter la documentation officielle** : https://hytalemodding.dev/en/docs
4. 💡 **Analyser les messages d'erreur** précis du compilateur
5. 🐛 **Corriger** en respectant les conventions Hytale
6. ✅ **Re-tester** avec `./gradlew build`

### Règle #5 : Recherche Préalable et Documentation Spécifique
Avant toute modification du code ou de l'UI, je m'engage à :
- Prioriser et consulter systématiquement la documentation fournie dans `@hytale_docs/**`.
- Me référer aux exemples concrets existants dans le projet, comme `@src/main/resources/Common/UI/Custom/Pages/player_management_screen.ui`.
- Vérifier la documentation officielle de Hytale à l'adresse `https://hytalemodding.dev/en/docs` pour les informations générales et les meilleures pratiques.
- Ne jamais faire d'hypothèses sur l'existence ou la validité de propriétés ou de comportements sans confirmation par ces sources.

### Règle #6 : Gestion de l'UI Hytale
❌ **NE JAMAIS utiliser** les codes couleur Minecraft (`§c`, `§a`, `§4`, etc.)
- Pour l'espacement dans les interfaces utilisateur Hytale, utilisez toujours la propriété `Padding` sur l'élément parent. N'utilisez **JAMAIS** la propriété `Margin` directement sur les éléments enfants, car elle n'est pas supportée de cette manière et peut entraîner des erreurs de compilation ou des comportements inattendus.
### Bonnes Pratiques UI
1. **Structure hiérarchique claire** avec indentation cohérente
2. **Noms descriptifs** pour les éléments (`playerListPanel`, `confirmButton`)
3. **Réutilisation** via composants UI personnalisés
4. **Documentation** des écrans complexes avec commentaires XML

---

## 📝 RÈGLE #7 : Commentaires et Documentation du Code

### Principe : Code Compréhensible par un Développeur Junior
**Tout le code doit être parfaitement compréhensible par un développeur junior qui découvre Hytale**

### Style de Commentaires Obligatoire

```java
package com.exemple.monplugin.systems;

import com.exemple.monplugin.components.HealthComponent;
import com.exemple.monplugin.components.RegenerationComponent;
import com.hytale.ecs.System;

/**
 * Système de régénération de santé pour les entités.
 *
 * Ce système parcourt toutes les entités possédant à la fois un HealthComponent
 * et un RegenerationComponent pour leur appliquer une régénération progressive.
 *
 * Architecture ECS :
 * - Composants requis : HealthComponent, RegenerationComponent
 * - Fréquence d'exécution : Chaque tick (20 fois par seconde)
 * - Priorité : Normale (exécuté après le système de dégâts)
 *
 * @see HealthComponent Pour les points de vie des entités
 * @see RegenerationComponent Pour les paramètres de régénération
 * @author Votre Nom
 * @since 1.0.0
 */
public class HealthRegenerationSystem extends System {

   // Temps écoulé depuis la dernière régénération (en ticks)
   // Utilisé pour éviter de régénérer trop fréquemment
   private int tickCounter = 0;

   // Intervalle entre chaque régénération (en ticks)
   // 20 ticks = 1 seconde dans Hytale
   private static final int REGENERATION_INTERVAL = 20;

   /**
    * Initialise le système de régénération.
    *
    * Appelé automatiquement par Hytale lors de l'enregistrement du système
    * dans la méthode setup() du plugin principal.
    */
   public HealthRegenerationSystem() {
      super();
      // Le constructeur reste léger
      // L'initialisation lourde se fait dans onEnable()
   }

   /**
    * Méthode appelée à chaque tick du serveur.
    *
    * PERFORMANCE : Cette méthode est appelée 20 fois par seconde.
    * Il est crucial de garder le traitement léger pour ne pas ralentir le serveur.
    *
    * LOGIQUE :
    * 1. Incrémenter le compteur de ticks
    * 2. Si l'intervalle est atteint, appliquer la régénération
    * 3. Réinitialiser le compteur
    */
   @Override
   public void update() {
      tickCounter++;

      // Ne régénérer que tous les REGENERATION_INTERVAL ticks
      // Cela évite de surcharger le serveur avec des calculs inutiles
      if (tickCounter < REGENERATION_INTERVAL) {
         return; // Sortir tôt si l'intervalle n'est pas atteint
      }

      // Réinitialiser le compteur pour le prochain cycle
      tickCounter = 0;

      // Parcourir toutes les entités ayant les composants requis
      // L'ECS de Hytale optimise cette requête automatiquement
      forEachEntity(HealthComponent.class, RegenerationComponent.class, (entity, health, regen) -> {
         applyRegeneration(entity, health, regen);
      });
   }

   /**
    * Applique la régénération à une entité spécifique.
    *
    * @param entity L'entité à régénérer
    * @param health Le composant de santé de l'entité
    * @param regen Le composant de régénération de l'entité
    *
    * VALIDATION :
    * - Vérifie que l'entité n'est pas déjà à santé max
    * - Vérifie que la régénération est activée
    * - Applique un plafond pour éviter de dépasser la santé max
    */
   private void applyRegeneration(Entity entity, HealthComponent health, RegenerationComponent regen) {
      // Cas 1 : L'entité est déjà en pleine santé
      if (health.getCurrentHealth() >= health.getMaxHealth()) {
         return; // Pas besoin de régénérer
      }

      // Cas 2 : La régénération est désactivée (par exemple, en combat)
      if (!regen.isEnabled()) {
         return; // Ne pas régénérer si désactivé
      }

      // Calculer les nouveaux points de vie
      // On utilise Math.min pour s'assurer de ne pas dépasser le maximum
      float newHealth = Math.min(
              health.getCurrentHealth() + regen.getRegenerationRate(),
              health.getMaxHealth()
      );

      // Appliquer la nouvelle santé
      health.setCurrentHealth(newHealth);

      // Log pour le debugging (seulement en mode debug)
      // Aide les développeurs à tracer les problèmes de régénération
      if (isDebugEnabled()) {
         getLogger().debug(String.format(
                 "Entité %s régénérée : %.1f -> %.1f HP",
                 entity.getId(),
                 health.getCurrentHealth(),
                 newHealth
         ));
      }
   }
}
```

### Que Commenter (OBLIGATOIRE)

✅ **TOUJOURS commenter** :
1. **Le POURQUOI** : Pourquoi ce code existe, quelle est sa logique métier
2. **Décisions d'architecture** : Pourquoi ECS plutôt qu'héritage ici
3. **Limitations API Hytale** : "Cette méthode ne fonctionne pas en async"
4. **Pièges et cas limites** : "Attention : null si l'entité n'a pas de joueur"
5. **Ordre d'exécution critique** : "Doit être appelé APRÈS l'initialisation"
6. **Performance** : "Méthode appelée 20 fois/seconde, garder légère"
7. **Comportements non-évidents** : "Retourne false si l'entité est morte"

❌ **NE PAS commenter** :
1. Ce qui est évident par le nom (`getUserName()` → pas besoin de commenter)
2. Répéter le code en français
3. Commentaires obsolètes ou faux

### Format JavaDoc Obligatoire

Toutes les classes et méthodes publiques doivent avoir une JavaDoc complète :

```java
/**
 * Description concise sur une ligne.
 *
 * Description détaillée sur plusieurs lignes si nécessaire.
 * Expliquer le contexte, l'utilisation, les cas particuliers.
 *
 * @param nomParametre Description du paramètre
 * @return Description de la valeur de retour
 * @throws TypeException Description de quand cette exception est levée
 * @see AutreClasse Pour plus d'informations
 * @since 1.0.0
 */
```

---

## 🎯 Conventions de Code

### Naming Conventions (Nommage)

- **Classes** : `PascalCase`
   - Exemples : `PlayerManagementSystem`, `HealthComponent`, `CustomEntity`

- **Méthodes** : `camelCase`
   - Exemples : `onPlayerJoin()`, `calculateDamage()`, `isEntityAlive()`

- **Constantes** : `UPPER_SNAKE_CASE`
   - Exemples : `MAX_PLAYERS`, `DEFAULT_HEALTH`, `REGENERATION_RATE`

- **Packages** : `lowercase`
   - Exemples : `com.exemple.monplugin.systems`, `com.exemple.monplugin.components`

- **Variables** : `camelCase`
   - Exemples : `playerName`, `currentHealth`, `isAlive`

### Conventions Spécifiques ECS

- **Composants ECS** : Suffixe `Component`
   - Exemples : `HealthComponent`, `PositionComponent`, `InventoryComponent`

- **Systèmes ECS** : Suffixe `System`
   - Exemples : `CombatSystem`, `MovementSystem`, `RenderSystem`

- **Entités** : Nom descriptif sans suffixe
   - Exemples : `CustomEntity`, `Boss`, `NPC`

### Convention Plugin Principal

La classe principale du plugin **DOIT** :
- Porter le nom : `{NomDuPlugin}Plugin`
- Hériter de : `JavaPlugin`
- Être dans le package racine : `com.exemple.monplugin`

Exemple :
```java
package com.exemple.admintools;

public class AdminToolsPlugin extends JavaPlugin {
   // ...
}
```

### Format de Réponse pour Génération de Code

Lorsque Gemini génère du code, **TOUJOURS inclure** :

1. **📋 Commentaire d'en-tête** avec description complète de la classe
2. **📦 Imports** organisés (groupés par package)
3. **💻 Code** avec commentaires explicatifs pour développeur junior
4. **⚠️ TODO/FIXME** si certains aspects nécessitent attention
5. **📖 Exemple d'usage** en commentaire JavaDoc si pertinent
6. **🔗 Références** vers `hytale_docs/` si une API spécifique est utilisée
7. **🧪 Suggestions de tests** si applicable

---

### Optimisations Gradle

Quand je demande des optimisations build :
- Suggestions de plugins Gradle utiles
- Configuration de tâches personnalisées
- Optimisation des dépendances
- Configuration de tests

### "Optimise la config Gradle"
→ Analyse build.gradle + suggestions d'amélioration

### "Debug ce code"
→ Analyser, identifier problèmes, proposer corrections avec explications

## ⚠️ Anti-Patterns et Erreurs à Éviter ABSOLUMENT

### ❌ NE JAMAIS (Erreurs Critiques)

1. **Confondre Hytale avec Minecraft**
   - Ne JAMAIS utiliser patterns Bukkit/Spigot/Paper/Forge/Fabric
   - Ne JAMAIS chercher des solutions Minecraft pour Hytale
   - Ne JAMAIS utiliser `@EventHandler` de Bukkit

2. **Fichiers YAML inexistants**
   - ❌ Créer un `plugin.yml` (n'existe pas dans Hytale)
   - ❌ Créer un `config.yml` par défaut (système différent)
   - ❌ Chercher ces fichiers dans la documentation

3. **Utiliser `Margin` dans les UI**
   - ❌ `<Button Margin="10,10,10,10">` provoque des erreurs
   - ✅ Utiliser `Padding` sur le parent à la place

4. **Codes couleur Minecraft**
   - ❌ Utiliser `§c`, `§a`, `§4`, etc.
   - ✅ Utiliser `MessageUtil` et l'API Hytale

5. **Bloquer le thread principal**
   - ❌ Opérations I/O synchrones dans `update()`
   - ❌ Boucles infinies ou longues dans les événements
   - ✅ Utiliser l'API async de Hytale

6. **Ignorer les exceptions silencieusement**
   ```java
   // ❌ MAUVAIS
   try {
       riskyOperation();
   } catch (Exception e) {
       // Silence = bugs invisibles
   }
   
   // ✅ BON
   try {
       riskyOperation();
   } catch (Exception e) {
       getLogger().error("Échec de l'opération risquée", e);
       // Gestion appropriée de l'erreur
   }
   ```

7. **Hardcoder les valeurs**
   ```java
   // ❌ MAUVAIS
   if (player.getHealth() < 20) {
   
   // ✅ BON
   private static final int MAX_HEALTH = 20;
   if (player.getHealth() < MAX_HEALTH) {
   ```

8. **Hiérarchies d'héritage profondes (anti-ECS)**
   ```java
   // ❌ MAUVAIS - Héritage profond
   class Entity { }
   class LivingEntity extends Entity { }
   class Monster extends LivingEntity { }
   class Zombie extends Monster { }
   
   // ✅ BON - Composition ECS
   class Entity {
       HealthComponent health;
       AIComponent ai;
       RenderComponent render;
   }
   ```

9. **Oublier de mettre à jour `hytale_docs/`**
   - Après chaque découverte API importante
   - Après avoir résolu un problème difficile
   - Après avoir trouvé une limitation

10. **Faire des hypothèses sur l'API**
   - ❌ "Ça doit fonctionner comme dans Minecraft"
   - ❌ "Cette méthode existe probablement"
   - ✅ Toujours consulter `hytale_docs/` d'abord

### ✅ TOUJOURS (Bonnes Pratiques Obligatoires)

1. **Suivre l'architecture ECS**
   - Composants = Données pures
   - Systèmes = Logique métier
   - Composition > Héritage

2. **Consulter la documentation locale en priorité**
   - `hytale_docs/` est la source de vérité
   - Code source décompilé en cas de doute
   - Documentation officielle en complément

3. **Valider toutes les entrées utilisateur**
   ```java
   public void handleCommand(String[] args) {
       // Toujours valider
       if (args == null || args.length == 0) {
           sendMessage("Usage: /command <arg>");
           return;
       }
       // ...
   }
   ```

4. **Utiliser des constantes pour les valeurs magiques**
   ```java
   // Les nombres magiques doivent avoir un nom explicite
   private static final int TICKS_PER_SECOND = 20;
   private static final int REGENERATION_INTERVAL = TICKS_PER_SECOND * 5; // 5 secondes
   ```

5. **Logger les erreurs importantes**
   ```java
   try {
       loadConfiguration();
   } catch (IOException e) {
       getLogger().error("Impossible de charger la configuration", e);
       // Fallback sur configuration par défaut
   }
   ```

6. **Commenter pour développeur junior**
   - Expliquer le POURQUOI, pas le QUOI
   - Documenter les limitations API
   - Marquer les pièges potentiels

7. **Tester après chaque modification**
   ```bash
   ./gradlew build
   ```

8. **Documenter les comportements non-évidents**
   ```java
   /**
    * ATTENTION : Cette méthode retourne null si l'entité n'a pas de joueur associé.
    * Toujours vérifier le retour avant utilisation.
    */
   public Player getPlayer() {
       // ...
   }
   ```

9. **Nettoyer les ressources**
   ```java
   @Override
   public void shutdown() {
       // Arrêter les tâches
       stopAllTasks();
       // Sauvegarder les données
       saveAllData();
       // Libérer les ressources
       closeConnections();
   }
   ```

10. **Respecter le cycle de vie du plugin**
   - `setup()` : Initialisation, enregistrement
   - `start()` : Activation des fonctionnalités
   - `shutdown()` : Nettoyage rapide et efficace

---

## 🚀 Workflow de Développement Complet

### 📖 Phase 1 : AVANT de Coder

1. **Lire la spécification** (si applicable)
   - Consulter `tasks/` pour les user stories et spécifications
   - Identifier les exigences fonctionnelles et techniques

2. **Comprendre l'architecture**
   - Lire `docs/architecture/README.md`
   - Identifier comment la nouvelle fonctionnalité s'intègre

3. **Rechercher dans la documentation**
   - Consulter `hytale_docs/` pour les API nécessaires
   - Vérifier le code source décompilé si besoin
   - Consulter la documentation officielle en complément

4. **Étudier les exemples existants**
   - Examiner `FReZ-Hytale-mod-HelloWorld/src`
   - Trouver des patterns similaires dans le projet

5. **Concevoir la solution ECS**
   - Identifier les composants nécessaires
   - Définir les systèmes requis
   - Planifier l'intégration avec le code existant

### 💻 Phase 2 : PENDANT le Développement

1. **Écrire le code avec commentaires**
   - Commenter pour qu'un développeur junior comprenne
   - Expliquer le POURQUOI, pas seulement le QUOI
   - Documenter les limitations API Hytale

2. **Respecter les règles UI** (si applicable)
   - Utiliser `Padding` sur le parent, jamais `Margin`
   - Utiliser `MessageUtil` pour les couleurs
   - Pas de codes couleur Minecraft

3. **Suivre l'architecture ECS**
   - Séparer données (Composants) et logique (Systèmes)
   - Favoriser composition plutôt qu'héritage
   - Respecter le principe de responsabilité unique

4. **Utiliser le bon cycle de vie**
   - Enregistrer dans `setup()`
   - Activer dans `start()`
   - Nettoyer dans `shutdown()`

5. **Tester régulièrement**
   ```bash
   ./gradlew build
   ```

### ✅ Phase 3 : APRÈS le Développement

1. **Relire le code**
   - Est-il clair pour un développeur junior ?
   - Les commentaires expliquent-ils le pourquoi ?
   - Y a-t-il des valeurs magiques à transformer en constantes ?

2. **Mettre à jour `hytale_docs/`**
   - Documenter toute nouvelle découverte API
   - Noter les limitations rencontrées
   - Ajouter des exemples de code fonctionnels

3. **Build final propre**
   ```bash
   ./gradlew clean build
   ```

4. **Tester en jeu** (si possible)
   - Vérifier le chargement du plugin
   - Tester les fonctionnalités ajoutées
   - Vérifier les logs pour les erreurs

5. **Documenter les limitations**
   - Y a-t-il des cas limites non gérés ?
   - Y a-t-il des dépendances sur d'autres plugins ?
   - Y a-t-il des problèmes de performance potentiels ?

---

## 📚 Ressources du Projet

### Sources de Documentation (par ordre de priorité)

1. **Documentation Hytale locale**
   - Chemin : `hytale_docs/` (submodule Git)
   - Usage : Référence principale pour toutes les API

2. **Code source décompilé**
   - Chemin : `C:\Users\jonat\Documents\Hytal\Plugins\Hytale-Server-Unpacked`
   - Usage : Quand la documentation est insuffisante

3. **Documentation officielle**
   - URL : https://hytalemodding.dev/en/docs
   - Usage : Vue d'ensemble et guides généraux

4. **Exemples de référence**
   - Chemin : `FReZ-Hytale-mod-HelloWorld/src`
   - Usage : Patterns et bonnes pratiques

5. **Architecture du projet**
   - Chemin : `docs/architecture/README.md`
   - Usage : Comprendre la structure globale

6. **Spécifications fonctionnelles**
   - Chemin : `tasks/`
   - Usage : User stories et exigences

### Outils de Développement

- **Build System** : Gradle 8.x
- **Langage** : Java 17+
- **IDE** : IntelliJ IDEA
- **Commandes essentielles** :
  ```bash
  ./gradlew build          # Compiler le projet
  ./gradlew clean build    # Recompilation complète
  ./gradlew build -x test  # Build sans tests
  ```

---

## 🎓 Instructions Finales pour Gemini CLI

### Processus à Suivre pour Chaque Demande

Quand un utilisateur demande du code ou de l'aide :

1. **📚 Recherche OBLIGATOIRE**
   - Consulter `hytale_docs/` en priorité
   - Vérifier les exemples existants dans le projet
   - Consulter le code source si nécessaire
   - ❌ NE JAMAIS inventer ou supposer

2. **🏗️ Architecture ECS**
   - Appliquer systématiquement l'approche ECS
   - Composants = données pures
   - Systèmes = logique métier

3. **💬 Commentaires Détaillés**
   - Code lisible par un développeur junior
   - Expliquer le POURQUOI de chaque décision
   - Documenter les limitations API

4. **✅ Validation des Règles**
   - Vérifier conformité avec TOUTES les 6 règles
   - Pas de `Margin` dans les UI
   - Pas de codes couleur Minecraft
   - Cycle de vie correct (setup/start/shutdown)

5. **🔍 Qualité Production**
   - Code production-ready, pas de raccourcis
   - Gestion d'erreurs appropriée
   - Tests suggérés si pertinent

6. **📖 Documentation**
   - Expliquer les choix techniques importants
   - Référencer `hytale_docs/` si API utilisée
   - Indiquer ce qu'il faut documenter après

7. **⚠️ Alertes et Warnings**
   - Signaler les problèmes potentiels
   - Suggérer des améliorations
   - Mentionner les limitations

8. **❓ Clarifications**
   - Si quelque chose n'est pas clair, DEMANDER
   - Ne pas supposer ou inventer
   - Être explicite sur les incertitudes

### ✓ Checklist Avant de Répondre

Avant de fournir une réponse, Gemini **DOIT** vérifier :

- [ ] J'ai consulté `hytale_docs/` pour toute API utilisée
- [ ] J'ai vérifié les exemples existants dans le projet
- [ ] Le code suit strictement l'architecture ECS
- [ ] Les commentaires sont clairs et détaillés (niveau junior)
- [ ] Aucun code couleur Minecraft (`§`) n'est utilisé
- [ ] Aucun `Margin` n'est utilisé dans les UI (seulement `Padding`)
- [ ] La classe principale utilise le pattern `{Nom}Plugin extends JavaPlugin`
- [ ] Les méthodes `setup()`, `start()`, `shutdown()` sont correctement utilisées
- [ ] Aucune référence à Minecraft/Bukkit/Spigot n'est faite
- [ ] Le code compile (`./gradlew build` passerait)
- [ ] J'ai indiqué ce qui doit être ajouté à `hytale_docs/`
- [ ] Les valeurs magiques sont transformées en constantes
- [ ] La gestion d'erreurs est appropriée
- [ ] Le code est en français (commentaires et messages)

### ⚠️ Si la Checklist n'est Pas Complète

**Si Gemini ne peut pas cocher TOUS ces points** :
1. ❓ Poser des questions clarificatrices à l'utilisateur
2. 📚 Demander quelle documentation consulter
3. 🔍 Demander des exemples similaires dans le projet
4. ⚠️ Expliquer quelles informations manquent
5. ❌ NE PAS générer de code basé sur des suppositions

### 🎯 Objectif Final

Fournir du code **Hytale natif**, **production-ready**, **bien documenté**, suivant **strictement l'architecture ECS** et **compréhensible par un développeur junior découvrant Hytale**.

**Tout code généré doit pouvoir être intégré directement dans le projet sans modification majeure.**