<div align="center">

# SolaceRanks

### v2 Development Branch 🚧

Advancement-powered rank progression for Minecraft servers, originally created for SolaceMC.

[![Stable release](https://img.shields.io/badge/stable-v1.0.0-2f6f9f)](https://github.com/MrHassaanX/SolaceRanks/releases/tag/v1.0.0)
[![Status](https://img.shields.io/badge/v2-planning-e76f00)](docs/V2_ARCHITECTURE.md)
[![Java](https://img.shields.io/badge/Java-21-e76f00)](build.gradle)
[![Paper](https://img.shields.io/badge/Paper-1.21.8-26804a)](build.gradle)
[![License](https://img.shields.io/badge/license-GPL--3.0--only-555)](LICENSE)

</div>

---

> [!WARNING]
> **Active v2 development branch:** `develop` is for planning and upcoming changes, not a v2 production release. For a stable download, use the [v1.0.0 GitHub Release](https://github.com/MrHassaanX/SolaceRanks/releases/tag/v1.0.0) or browse [`main`](https://github.com/MrHassaanX/SolaceRanks/tree/main).

## 🚧 Development Status

SolaceRanks v2 is in its planning phase. This branch currently contains the v1 implementation plus an architecture plan; the proposed v2 capabilities below are **not implemented yet**.

The original v1 release remains preserved through its tag, legacy branch, and GitHub Release.

## 🎯 v2 Goals

| Direction | Status |
| --- | --- |
| UUID-keyed player records, completed advancement counting, LuckPerms API, optional Floodgate | ✅ Existing in v1 |
| Configurable rank definitions with independent display names and LuckPerms groups | 🧭 Planned |
| Clearer service boundaries and a storage provider abstraction | 🧭 Planned |
| Versioned config and non-destructive migration from v1 | 🧭 Planned |
| Optional PlaceholderAPI, TAB, LPC, and DiscordSRV integrations | 🧭 Planned |
| Isolated Paper compatibility layer and wider verified testing | 🧭 Planned |

These goals are described in the [v2 architecture plan](docs/V2_ARCHITECTURE.md). They are design targets, not claims of available features.

## 📚 Architecture Documentation

Read [docs/V2_ARCHITECTURE.md](docs/V2_ARCHITECTURE.md) for the v1 audit, proposed packages and models, migration strategy, compatibility concerns, test plan, and staged implementation sequence.

## 🧪 Testing Status

The existing v1 project builds with Java 21 and the Gradle wrapper. The architecture plan recommends automated threshold, permission, persistence, and migration tests plus manual Java/Bedrock server checks before any v2 release. No v2 compatibility or feature test result is claimed here.

## 📦 Stable Download

Production users should download the [v1.0.0 release](https://github.com/MrHassaanX/SolaceRanks/releases/tag/v1.0.0). Its assets include the JAR and `SHA256SUMS.txt`; GitHub also provides source archives.

```text
SolaceRanks-1.0.0.jar
```

## ✨ Current Stable Features

- Advancement-based rank progression
- Counts completed advancements
- Ignores recipe advancements
- LuckPerms API integration
- Automatic rank synchronization
- Rank updates when players join
- Rank updates when advancements are completed
- Java Edition support
- Optional Floodgate detection
- Bedrock player support
- `br_` fallback detection when Floodgate is unavailable
- UUID-based player storage
- Persistent player database
- Rank promotion broadcasts
- Configurable promotion titles
- Configurable promotion sounds
- Rank leaderboard
- Offline-player lookup where supported by stored data and advancement files
- Administrative synchronization and recalculation tools
- Configurable messages
- Permission-based administrative commands

## 🧱 Confirmed v1 Compatibility

SolaceRanks v1.0.0 was originally developed against:

- Paper `1.21.8`
- Java `21`

It was also successfully used on:

- Minecraft/Paper `26.2`

Minecraft/Paper `26.3` and other versions have not yet been formally tested.

SolaceRanks may work on additional versions where the Bukkit/Paper APIs it uses remain compatible, but only tested versions are listed as confirmed.

LuckPerms is a required plugin dependency. Floodgate is optional and is detected when installed.

## 🚀 Installing v1

1. Download `SolaceRanks-1.0.0.jar` from the [v1.0.0 GitHub Release](https://github.com/MrHassaanX/SolaceRanks/releases/tag/v1.0.0).
2. Install LuckPerms on your server.
3. Place `SolaceRanks-1.0.0.jar` into the server's `plugins/` directory.
4. Start or restart the server.
5. Configure ranks, messages, promotion broadcasts, sounds, and titles as needed.
6. Restart the server or use the available reload command after configuration changes.

## 📊 Current v1 Rank Progression

These are the original v1 SolaceMC rank thresholds.

| Advancements | Rank |
| --- | --- |
| 0-9 | Newbie |
| 10-19 | Wanderer |
| 20-29 | Explorer |
| 30-39 | Guardian |
| 40-49 | Luminary |
| 50-59 | Eclipse |
| 60-69 | Celestial |
| 70-79 | Eternal |
| 80-89 | Divine |
| 90-99 | Ascendant |
| 100+ | Archon |

## 🎮 Current v1 Commands

| Command | Description | Permission |
| --- | --- | --- |
| `/rank` | Shows your current rank, advancement count, and next rank. | `solaceranks.rank` |
| `/rank top` | Shows the top 10 stored players by completed advancements. | None beyond command access |
| `/rank help` | Shows the command help menu. Admin-only entries are shown only to admins. | None for the command itself |
| `/rank reload` | Reloads plugin configuration and updates online players quietly. | `solaceranks.admin` |
| `/rank sync <player>` | Recalculates and synchronizes a stored or online player's rank. | `solaceranks.admin` |
| `/rank recalculate` | Recalculates all stored player ranks against the current thresholds. | `solaceranks.admin` |
| `/rank info <player>` | Shows detailed stored rank information for a player. | `solaceranks.admin` |
| `/playerrank <player>` | Shows another stored player's rank, advancement count, next rank, and remaining advancements. | `solaceranks.playerrank` |

```text
/rank
/rank top
/rank help
/playerrank <player>
/rank reload
/rank sync <player>
/rank recalculate
/rank info <player>
```

## 🔐 Current v1 Permissions

| Permission | Default | Description |
| --- | --- | --- |
| `solaceranks.rank` | `true` | Allows players to use `/rank`. |
| `solaceranks.playerrank` | `true` | Allows players to use `/playerrank <player>`. |
| `solaceranks.admin` | `op` | Allows administrative commands. |

Administrative commands requiring `solaceranks.admin`:

- `/rank reload`
- `/rank sync <player>`
- `/rank recalculate`
- `/rank info <player>`

Server owners can grant these permissions through LuckPerms.

```text
solaceranks.rank
solaceranks.playerrank
solaceranks.admin
```

## ⚙️ Current v1 Configuration

SolaceRanks v1 uses the following files:

- `config.yml` - rank thresholds, promotion broadcast toggle, promotion title settings, promotion sound settings, and `/playerrank` display formatting.
- `messages.yml` - configurable plugin messages.
- `players.yml` - runtime database generated automatically by the plugin.

`players.yml` stores player progression information using UUIDs as the primary identifier, including:

- UUID
- Username
- Java or Bedrock status
- Current rank
- Previous rank
- Advancement count
- First join
- Last seen

Do not manually edit `players.yml` while the server is running.

```yaml
broadcast-rankup: true
sound:
  enabled: true
  type: ENTITY_PLAYER_LEVELUP
title:
  enabled: true
  title: "&6Rank Up!"
  subtitle: "&fYou are now %rank%"
```

## 🌐 Bedrock / Geyser / Floodgate in v1

Floodgate is optional. When Floodgate is installed, SolaceRanks uses the Floodgate API to detect Bedrock players.

When Floodgate is unavailable, the v1 implementation can fall back to the original `br_` username convention. Player data is still stored by UUID, not by username, so Java and Bedrock players can both be tracked safely in the persistent database.

## Version History

### v1.0.0

Original SolaceMC release.

This version represents the original server-specific implementation before the future universal redesign.

### Future

SolaceRanks is planned to evolve into a more flexible public plugin in future releases. Future features and compatibility targets will be documented when they are implemented.

## 🛠️ Building From Source

SolaceRanks v1 requires Java 21.

Windows:

```powershell
.\gradlew.bat clean build
```

Linux/macOS:

```bash
./gradlew clean build
```

Build outputs are written to:

```text
build/libs/
```

## 🕰️ Legacy Preservation

The original SolaceRanks implementation is intentionally preserved through:

- Git tag `v1.0.0`
- Branch `legacy/1.x`
- GitHub Release `v1.0.0`

This preservation keeps the first SolaceMC-specific implementation available before later public/universal development begins.

## 📜 License

Copyright (C) 2026 Mohammed Hassaan

SolaceRanks is licensed under the GNU General Public License v3.0 only (`GPL-3.0-only`).

You may use, study, modify, and redistribute this software under the terms of the GPL v3. Distributed modified versions must comply with the GPL's source-code and licensing requirements.

## ❤️ Credits

Created by Mohammed Hassaan / MrHassaanX.

Originally built for SolaceMC.
