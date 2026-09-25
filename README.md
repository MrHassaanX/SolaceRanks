# SolaceRanks

[![Version](https://img.shields.io/badge/version-v1.0.0-blue)](https://github.com/MrHassaanX/SolaceRanks/releases/tag/v1.0.0)
[![Java](https://img.shields.io/badge/Java-21-orange)](https://www.oracle.com/java/)
[![Paper](https://img.shields.io/badge/Paper-1.21.8-2ea44f)](https://papermc.io/)
[![Latest Release](https://img.shields.io/badge/release-v1.0.0-blue)](https://github.com/MrHassaanX/SolaceRanks/releases/tag/v1.0.0)
[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](LICENSE)

An advancement-based rank progression plugin for Minecraft servers, originally created for SolaceMC.

SolaceRanks automatically tracks completed Minecraft advancements and updates players' LuckPerms ranks.

## About

SolaceRanks was originally developed specifically for the SolaceMC Minecraft server. Version 1.0.0 represents the original SolaceMC implementation and has been preserved before the project evolves into a more universal public plugin in future versions.

The original v1 release remains permanently archived through Git history, a release tag, a legacy branch, and a GitHub Release.

## Features

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

## Compatibility

SolaceRanks v1.0.0 was originally developed against:

- Paper `1.21.8`
- Java `21`

It was also successfully used on:

- Minecraft/Paper `26.2`

Minecraft/Paper `26.3` and other versions have not yet been formally tested.

SolaceRanks may work on additional versions where the Bukkit/Paper APIs it uses remain compatible, but only tested versions are listed as confirmed.

LuckPerms is a required plugin dependency. Floodgate is optional and is detected when installed.

## Installation

1. Download `SolaceRanks-1.0.0.jar` from the [v1.0.0 GitHub Release](https://github.com/MrHassaanX/SolaceRanks/releases/tag/v1.0.0).
2. Install LuckPerms on your server.
3. Place `SolaceRanks-1.0.0.jar` into the server's `plugins/` directory.
4. Start or restart the server.
5. Configure ranks, messages, promotion broadcasts, sounds, and titles as needed.
6. Restart the server or use the available reload command after configuration changes.

## Default Ranks

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

## Commands

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

## Permissions

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

## Configuration

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

## Bedrock / Geyser / Floodgate

Floodgate is optional. When Floodgate is installed, SolaceRanks uses the Floodgate API to detect Bedrock players.

When Floodgate is unavailable, the v1 implementation can fall back to the original `br_` username convention. Player data is still stored by UUID, not by username, so Java and Bedrock players can both be tracked safely in the persistent database.

## Download

Download the original v1.0.0 release here:

[https://github.com/MrHassaanX/SolaceRanks/releases/tag/v1.0.0](https://github.com/MrHassaanX/SolaceRanks/releases/tag/v1.0.0)

The release includes:

- `SolaceRanks-1.0.0.jar`
- `SHA256SUMS.txt`
- Source archives generated by GitHub

## Version History

### v1.0.0

Original SolaceMC release.

This version represents the original server-specific implementation before the future universal redesign.

### Future

SolaceRanks is planned to evolve into a more flexible public plugin in future releases. Future features and compatibility targets will be documented when they are implemented.

## Building From Source

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

## Project History

The original SolaceRanks implementation is intentionally preserved through:

- Git tag `v1.0.0`
- Branch `legacy/1.x`
- GitHub Release `v1.0.0`

This preservation keeps the first SolaceMC-specific implementation available before later public/universal development begins.

## License

Copyright (C) 2026 Mohammed Hassaan

SolaceRanks is licensed under the GNU General Public License v3.0 only (`GPL-3.0-only`).

You may use, study, modify, and redistribute this software under the terms of the GPL v3. Distributed modified versions must comply with the GPL's source-code and licensing requirements.

## Credits

Created by Mohammed Hassaan / MrHassaanX.

Originally built for SolaceMC.
