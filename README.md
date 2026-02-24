# FReZ Hytale Mod Prefab Builder

A powerful Hytale plugin for automated prefab construction with holographic previews and resource-based progression.

> **🏗️ Build your world, block by block, automatically!** Admins place holograms, players fill chests, and the plugin builds the structure while consuming resources.

## Features

✅ **Holographic Previews** - Real-time translucent "ghost" rendering of prefabs before construction.  
✅ **Automated Building** - Progressive block placement using the native Hytale `FeedbackConsumer` API.  
✅ **Resource Linking** - Link chests to construction sites to provide necessary materials.  
✅ **JSON-Based Material Definitions** - Easily configure required items for any prefab structure.  
✅ **Admin Selection UI** - Intuitive in-game tool to select and position server prefabs.  
✅ **Modern Tech Stack** - Built with Java 25 and optimized for the Hytale ECS architecture.

---

## 🚀 Quick Start

### Prerequisites

- **Java 25 JDK** - [Download here](https://www.oracle.com/java/technologies/downloads/)
- **Hytale Server API** - Included as a submodule in `libs/Hytale-Server-Unpacked`.
- **Hytale Docs** - Reference documentation in `hytale_docs/`.

### 1. Project Setup

```bash
# Clone the repository with submodules
git clone --recursive https://github.com/jeanniardJ/FReZ-Hytale-Mod-Prefab-Builder.git
cd FReZ-Hytale-Mod-Prefab-Builder
```

### 2. Build the Plugin

```bash
# Windows
gradlew.bat shadowJar

# Linux/Mac
./gradlew shadowJar
```

Your plugin JAR will be in: `build/libs/PrefabBuilder-1.0.0.jar`

---

## 🎮 How it Works

### 1. Admin: Place a Hologram
As an admin, use the `/pb give` command to get the **Prefab Builder Tool**. Right-click on the ground to open the selection UI, choose a prefab, and an hologram will appear.

### 2. Player: Link a Chest
Place a chest near the hologram (< 10 blocks). A message will appear confirming the link and listing the required materials.

### 3. Construction: Fill and Build
Fill the linked chest with the requested items. Once the resources are satisfied and the chest is closed, the **AutoBuilder** starts placing blocks and consuming items from the chest.

---

## 📂 Project Structure

```
FReZ-Hytale-Mod-Prefab-Builder/
├── hytale_docs/             # Git Submodule: Documentation
├── libs/
│   └── Hytale-Server-Unpacked/ # Git Submodule: Server API reference
├── src/main/java/com/yourname/prefabbuilder/
│   ├── PrefabBuilderPlugin.java # Main class
│   ├── tool/                # Admin Tool logic
│   ├── hologram/            # Hologram rendering
│   ├── chest/               # Chest linking & inventory logic
│   ├── builder/             # Progressive building core
│   ├── command/             # Admin commands (/pb)
│   └── data/                # Material & Session models
├── config/materials/        # JSON material configuration per prefab
└── README.md                # This file
```

---

## 🛠️ Configuration

Each prefab requires a JSON file in `config/materials/<prefab_name>.json`:

```json
{
  "prefab": "oak_house",
  "requirements": [
    { "item": "OAK_PLANKS", "amount": 64 },
    { "item": "OAK_LOG",    "amount": 24 }
  ]
}
```

---

## 🧪 Testing

```bash
# Run server with your plugin
./gradlew runServer
```

---

## 📜 Documentation

Refer to the `hytale_docs/` folder for detailed API usage and modding guides.
For the specific project requirements and architecture, see `prefab_builder.md`.

---

## License

This project is licensed under the MIT License.

---

**Happy Building! 🛠️**

