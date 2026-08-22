# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project adheres to
[Semantic Versioning](https://semver.org/spec/v2.0.0.html).

Entries under `## [Unreleased]` are written by hand. `scripts/changelog.sh bump` stamps them into a released section and
tags the release; pushing that tag makes CI publish the section as the GitHub release notes.

## [Unreleased]

### Added

- **Developer API:** other plugins can read unplugged player state, and can cancel an unplug before it happens
- **HuskSync support:** inventories now survive both handoffs on servers running HuskSync

### Fixed

- **Inventory handoff:** the unplugged player now receives your inventory directly, before it joins. It no longer starts empty in front of plugins that read inventory on join, and no longer relies on a save file the server may never write
- **Failed spawns:** a spawn that fails no longer leaves a phantom unplugged player holding a slot
- **Proxy sessions:** simultaneous unplugs can no longer corrupt the proxy's routing records

## [0.3.0] - 2026-08-21

### Fixed

- Unplugged bots no longer die a tick after spawning on servers running ProtocolLib alongside PacketEvents. The bot's fake connection now carries a vanilla-shaped Netty pipeline, so ProtocolLib injects into it instead of throwing `NoSuchElementException: encoder` on every unplug, and PacketEvents' injection kick is refused for unplugged bots only.

## [0.2.0] - 2026-08-20

### Added

- **Proxy support:** behind Velocity, unplugging disconnects you from the whole network
- **Companion plugin:** a second JAR for the proxy, which sends you back to the backend your unplugged player is on
- **Session persistence:** the proxy keeps its routing records across a restart or crash

### Changed

- **Logging:** more detail on unplugging, expiry, and proxy session events

### Fixed

- **Duplicate players:** the unplugged player is now created only after the real player has fully disconnected

## [0.1.0] - 2026-08-15

### Added

- **Go green:** Turn off your computer and leave your unplugged player to AFK for you
- **Customizable limits:** Server operators can configure the maximum duration, as well as a global cap of unplugged
  players
- **Admin control:** Server admins have commands allowing them to inspect unplugged players, as well as debug various
  aspects of the plugin
- **Capped:** Configurable limit to how many unplugged players can exist at the same time, to prevent resource
  exhaustion on AFK players

[unreleased]: https://github.com/devpikachu/unplugged-afk-paper/compare/v0.3.0...HEAD
[0.3.0]: https://github.com/devpikachu/unplugged-afk-paper/compare/v0.2.0...v0.3.0
[0.2.0]: https://github.com/devpikachu/unplugged-afk-paper/compare/v0.1.0...v0.2.0
[0.1.0]: https://github.com/devpikachu/unplugged-afk-paper/releases/tag/v0.1.0
