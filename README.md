# Player Detector

A Fabric mod that adds a redstone block that detects when players stand on it.

## Features

- **Player Detection**: Emits a redstone signal (level 15) when a player stands on it
- **Flat Profile**: Only 2 pixels tall, similar to a pressure plate
- **Waterloggable**: Can be placed underwater
- **Player-Only**: Only detects players, not other entities

## Screenshots

![Player Detector](img.png)
![Player Detector Recipe](img2.png)

## Requirements

- Targets the Minecraft, Fabric Loader, and Fabric API versions declared in this mod's `gradle.properties`; check there for the exact currently-supported version
- Java version as declared in `fabric.mod.json`'s `depends` block
- Pandorical (see below)

## Pandorical

Player Detector registers its block and item models through Pandorical's content sync.

**The Pandorical mod must be installed client-side** to see the Player Detector block and item rendered with their custom textures. Without it, the block still functions (emits redstone signal when a player stands on it), but a connecting client cannot see it rendered correctly.

## Installation

Install alongside its declared dependencies (see `fabric.mod.json`), including Pandorical on connecting clients.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
