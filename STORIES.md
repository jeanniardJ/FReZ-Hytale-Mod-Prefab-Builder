# User Stories - FReZ Hytale Mod Prefab Builder

Ce document répertorie les fonctionnalités attendues du plugin sous forme d'User Stories pour guider le développement.

## 🛠️ Rôles Utilisateurs
- **Administrateur** : Gère les préfabs et initialise les chantiers.
- **Joueur** : Contribue aux ressources pour la construction.

---

## 🏗️ Phase d'Initialisation (Admin)

### US-01 : Obtention de l'outil de construction
**En tant qu'** Administrateur,
**Je veux** pouvoir me donner un item spécial "Outil Préfab Builder" via une commande,
**Afin de** pouvoir interagir avec le système de placement de préfabriqués.

### US-02 : Sélection du préfabriqué
**En tant qu'** Administrateur,
**Je veux** qu'un clic droit avec l'outil ouvre une interface de sélection des préfabriqués disponibles sur le serveur,
**Afin de** choisir quel bâtiment je souhaite construire.

### US-03 : Visualisation par hologramme
**En tant qu'** Administrateur,
**Je veux** voir apparaître une version translucide (hologramme) du préfabriqué à l'endroit sélectionné,
**Afin de** valider son emplacement et son orientation avant de lancer les travaux.

---

## 📦 Phase de Préparation (Joueur)

### US-04 : Liaison du coffre de ressources
**En tant que** Joueur,
**Je veux** qu'en posant un coffre à proximité d'un hologramme, celui-ci soit automatiquement lié au chantier,
**Afin de** servir de point de dépôt pour les matériaux de construction.

### US-05 : Information sur les ressources
**En tant que** Joueur,
**Je veux** recevoir un message listant les ressources nécessaires (types et quantités) lors de la liaison du coffre ou en interagissant avec l'hologramme,
**Afin de** savoir exactement quoi collecter.

---

## 🔧 Phase de Construction (Automatique)

### US-06 : Déclenchement de la construction
**En tant que** Joueur,
**Je veux** que la construction démarre automatiquement dès que les ressources requises sont présentes dans le coffre lié et que je le ferme,
**Afin de** voir le bâtiment s'élever sans intervention manuelle supplémentaire.

### US-07 : Suivi de progression
**En tant que** Joueur/Admin,
**Je veux** voir une barre de progression dans mon HUD (Action Bar) m'indiquant le pourcentage d'avancement du chantier,
**Afin de** suivre l'état de la construction en temps réel.

### US-08 : Consommation progressive des ressources
**En tant que** Propriétaire du serveur,
**Je veux** que les ressources soient retirées du coffre au fur et à mesure de l'avancement de la construction,
**Afin de** garantir une simulation de construction réaliste et éviter les abus.

### US-09 : Remplacement des blocs fantômes
**En tant que** Joueur,
**Je veux** que les blocs de l'hologramme disparaissent à mesure que les vrais blocs sont placés,
**Afin de** voir la transition nette entre le projet et la réalité.

---

## ⚙️ Administration & Maintenance

### US-10 : Configuration des matériaux
**En tant qu'** Administrateur,
**Je veux** pouvoir définir les ressources nécessaires pour chaque préfabriqué via des fichiers de configuration JSON,
**Afin de** personnaliser le coût de construction de chaque structure.

### US-11 : Annulation d'un chantier
**En tant qu'** Administrateur,
**Je veux** pouvoir supprimer un hologramme actif (par exemple en cassant le coffre lié),
**Afin d'** annuler un projet de construction erroné.
