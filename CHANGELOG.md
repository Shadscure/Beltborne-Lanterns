# Changelog

## [1.2.7] - 2026-11-10

### Added
- Common lifecycle facades for server and client events (`BeltLanternServerLifecycle`, `BeltLanternClientLifecycle`), eliminating platform duplication between Fabric and NeoForge.
- Dedicated server bytecode isolation helper `BLNeoForgeClientPayloadHandler`.
- Target matrix updates and build support for Minecraft 26.1.1 and 26.1.2.

### Fixed
- Configuration versioning and migration fix for #24 (`CURRENT_CONFIG_VERSION = 2`).
- Server override protection preserving local player configuration integrity.
- DCL `HOLDER` synchronization in client configuration access.
- Coordinate bounds clamping in `LanternDebugScreen`.
- Bytecode audit exclusions and NeoForge 26.1.x version watcher prefix.

## [1.2.6] - 2026-11-06
### Fixed
- **Multiplayer (`1.21.2`–`1.21.11`)**: Fixed a bug where other players in multiplayer could visually inherit the local player's belt lantern when `PlayerRenderState` lacked an attached player entity reference.
- **Custom Lantern Offset (`#22`)**: Fixed custom lantern position, rotation, and scale settings (`offsetX/Y/Z`, `pivot`, `scale`) not being applied on game startup until the config screen was opened.
- **Config Migration**: Added explicit schema versioning (`configVersion = 2`) to prevent small custom scale/offset values from being re-multiplied on subsequent launches.
- **Keybind Feedback (`#21`)**: Pressing the equip/unequip keybind with an empty hand and no lantern on the belt now displays a localized hint in the action bar (`en_us`, `ru_ru`, `uk_ua`, `de_de`, `fr_fr`, `es_es`, `zh_cn`).
- **Thread Safety & Lifecycle Cleanup**: Switched `LampRegistry` to a thread-safe copy-on-write snapshot during datapack reloads and cleared client/server lantern state caches on disconnect and server stop.
- **Runtime Datapack & Debug Export**: Ensured the generated runtime datapack is automatically enabled before server reload and fixed debug screen clipboard export formatting on non-English locales (`Locale.ROOT`).
- **Loader Metadata**: Tightened `minecraft` and `neoforge` version ranges in mod descriptors so builds only load on their target Minecraft version.

## [1.2.5] - 2026-11-04
### Fixed
- Fixed lantern duplication on death when compatibility layer addons (e.g. Accessories Layer) manage their own inventory on death.
