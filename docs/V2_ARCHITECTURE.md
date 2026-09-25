# SolaceRanks v2 Architecture Plan

This document records the Phase 1 audit and architecture plan for SolaceRanks v2. It is intentionally planning-only: no runtime behavior, source code, configuration, tags, or preservation branches are changed by this document.

## Current v1 Architecture

SolaceRanks v1 is a compact Paper plugin centered around `SolaceRanks`, `RankService`, a YAML player database, and small command/listener layers.

- `com.solacemc.solaceranks.SolaceRanks` is the plugin bootstrap. It loads config/messages, resolves LuckPerms, creates managers/services, registers listeners, and wires commands.
- `commands.RankCommand` handles `/rank`, admin subcommands, leaderboard display, sync/recalculate flows, and detailed player info.
- `commands.PlayerRankCommand` handles `/playerrank <player>` by reading stored player data.
- `listeners.AdvancementListener` updates player records and ranks on join, quit, and completed advancement events.
- `manager.RankService` owns rank loading, rank lookup, online/offline sync, database updates, LuckPerms updates, and rank-up effects.
- `manager.LuckPermsManager` wraps LuckPerms user loading, managed rank group cleanup, primary group assignment, and user saving.
- `manager.FloodgateManager` detects Floodgate by reflection and falls back to the `br_` username prefix.
- `manager.PlayerDatabase` stores player data in `players.yml` keyed by UUID.
- `utils.AdvancementUtils` counts online completed advancements through Bukkit APIs.
- `utils.AdvancementFileUtils` counts offline completed advancements from world advancement JSON files.

## Runtime Flow

### Startup

1. `SolaceRanks#onEnable` saves default config, loads `messages.yml`, and requires a LuckPerms service provider.
2. Floodgate support is initialized optionally through reflection.
3. `PlayerDatabase`, `LuckPermsManager`, and `RankService` are created.
4. Advancement listeners and command executors/tab completers are registered.

### Player Join

1. `AdvancementListener#onJoin` records the player's current data immediately.
2. A delayed task runs one second later and calls `RankService#updatePlayerRank` without announcements.
3. `RankService` counts completed advancements, calculates the rank, updates `players.yml`, and writes the primary LuckPerms group.

### Advancement Earned

1. `PlayerAdvancementDoneEvent` is ignored when the advancement key path starts with `recipes/`.
2. The player's completed, non-recipe advancements are recounted.
3. The new rank is calculated from configured thresholds.
4. Player data and LuckPerms primary group are updated.
5. Promotion effects run only when the rank changed upward because of an advancement event.

### Player Quit

1. The plugin records username, Bedrock status, rank, advancement count, and timestamps.
2. Database writes are coalesced by a delayed save task.

### Commands

- `/rank` recounts the sender's live advancements and shows current/next rank.
- `/playerrank <player>` reads stored rank data by username.
- `/rank top` reads stored players and sorts by advancement count.
- `/rank reload`, `/rank sync <player>`, `/rank recalculate`, and `/rank info <player>` require `solaceranks.admin`.

## Hard-Coded v1 Behavior

The current implementation is usable for SolaceMC v1 but still contains assumptions that should become configurable or isolated in v2.

- Default rank IDs and thresholds are hard-coded in `RankService.DEFAULT_RANKS`: `newbie`, `wanderer`, `explorer`, `guardian`, `luminary`, `eclipse`, `celestial`, `eternal`, `divine`, `ascendant`, `archon`.
- Rank IDs are treated as LuckPerms group names. There is no separate display name, storage ID, or LuckPerms group field.
- Managed LuckPerms groups are derived directly from loaded rank names.
- Bedrock fallback detection uses a fixed `br_` prefix in `FloodgateManager`.
- `plugin.yml` description refers specifically to SolaceMC.
- `build.gradle` uses group `com.solacemc` and version `1.0.0`.
- Command display formatting is mostly inline in `RankCommand` and `PlayerRankCommand`.
- Player lookup for stored data is username-based for commands, while UUID is the storage key.
- Storage is fixed to `players.yml`; there is no storage provider interface.
- Offline advancement sync assumes the current world folder advancement JSON layout.
- Config has no `config-version`, migration system, or validation report.

## Technical Debt

- `RankService` currently mixes rank calculation, config loading, persistence coordination, LuckPerms integration, and player feedback effects.
- Command classes format rich output directly instead of using a shared message/rendering service.
- Rank display-name logic is duplicated in command classes.
- `PlayerDatabase` combines YAML storage, schema migration cleanup, player identity normalization, and save batching.
- Offline advancement counting is static utility code rather than a service with clear platform assumptions.
- Integration code is concrete and directly referenced by the main service, making future PlaceholderAPI, TAB, LPC, and DiscordSRV hooks harder to add cleanly.
- There is no formal internal event model for rank changes or player data updates.
- There are no automated tests for rank threshold selection, promotion suppression, storage migration, or command permissions.

## Areas to Preserve

The v2 implementation should retain the successful behavior from v1 while making it more general.

- UUID remains the primary player identifier.
- Completed advancements only are counted.
- Recipe advancements remain excluded.
- LuckPerms API should be used directly; commands should not execute `/lp`.
- Floodgate must stay optional.
- Offline-mode servers must remain supported.
- Bedrock player tracking must not break Java player compatibility.
- Rank updates should run on join and advancement completion.
- Promotion messages, broadcast mode, sounds, and titles should only run for real rank-ups.
- Existing v1 data should be migrated safely rather than discarded.

## Proposed v2 Package Structure

The package root can remain `com.solacemc.solaceranks` for continuity, while internals become more modular.

```text
com.solacemc.solaceranks
  SolaceRanks
  api
    RankChangeEvent
    SolaceRanksProvider
  command
    RankCommand
    PlayerRankCommand
    CommandPermissions
    CommandViews
  config
    ConfigManager
    ConfigMigrator
    Messages
    RankConfigLoader
  integration
    IntegrationManager
    luckperms
      LuckPermsIntegration
    floodgate
      FloodgateIntegration
    placeholderapi
      PlaceholderApiIntegration
    tab
      TabIntegration
    lpc
      LpcIntegration
    discordsrv
      DiscordSrvIntegration
  listener
    AdvancementListener
    PlayerConnectionListener
  model
    PlayerRankData
    RankDefinition
    RankProgress
    RankRequirement
  service
    AdvancementCounter
    PlayerIdentityService
    RankRegistry
    RankService
    RankSyncService
  storage
    PlayerStorage
    StorageProvider
    StorageResult
    yaml
      YamlPlayerStorage
  platform
    Scheduler
    PaperAdvancementCounter
    OfflineAdvancementFileReader
  util
    TextFormatter
    TimeFormatter
```

The existing package names do not need to change all at once. The recommended approach is to introduce new boundaries gradually, keeping public behavior stable.

## Proposed Rank Model

Ranks should become data-driven definitions instead of implicit LuckPerms group names.

```yaml
config-version: 2

ranks:
  newbie:
    display-name: "&7Newbie"
    luckperms-group: "newbie"
    requirement:
      type: "advancements"
      completed: 0
  wanderer:
    display-name: "&aWanderer"
    luckperms-group: "wanderer"
    requirement:
      type: "advancements"
      completed: 10
```

Recommended model fields:

- `id`: stable storage/config key, such as `newbie`.
- `displayName`: formatted user-facing name.
- `luckPermsGroup`: group to assign in LuckPerms.
- `requirement`: requirement object, initially completed advancement count.
- `order`: optional explicit ordering when requirements are equal or future requirement types are added.
- `metadata`: optional future extension point for placeholders, icons, Discord role mappings, or TAB formatting.

Initial v2 should preserve the v1 thresholds as defaults but should not require servers to use those names.

## Proposed Storage Architecture

Create a `PlayerStorage` or `StorageProvider` interface so YAML is one implementation rather than the entire persistence model.

Recommended operations:

- `load(UUID uuid)`
- `save(PlayerRankData data)`
- `findByUsername(String username)`
- `listAll()`
- `topByAdvancements(int limit)`
- `close()`

The first v2 storage implementation can remain YAML-backed and continue using UUID keys. The abstraction should make future SQLite or MySQL support possible without changing command or rank logic.

Stored player data should include:

- UUID
- current username
- Java/Bedrock identity
- current rank ID
- previous rank ID
- completed advancement count
- first join timestamp
- last seen timestamp
- schema version for future migration

## Proposed Integration Architecture

Create an `IntegrationManager` that owns optional hooks and exposes stable capabilities to services.

- `LuckPermsIntegration` should handle group lookup, managed group cleanup, primary group changes, and clear result reporting.
- `FloodgateIntegration` should detect Bedrock players through the Floodgate API when available, then fall back to a configurable prefix.
- PlaceholderAPI support should be read-only and derive values from `RankService`/storage.
- TAB and LPC support should be optional listeners or sync adapters, not core rank logic.
- DiscordSRV support should react to rank change events rather than being called directly by rank calculation.

Rank changes should produce an internal event/result object containing old rank, new rank, player identity, advancement count, and reason. Integrations can consume that object independently.

## Proposed Config Version Strategy

Add explicit config versions and non-destructive migrations.

- Add `config-version: 2` to all managed config files.
- Load defaults from bundled resources.
- Validate user config at startup and log actionable warnings.
- Add missing defaults without removing unknown user keys.
- Before any migration rewrite, create timestamped backups.
- Preserve v1 rank defaults during migration.
- Convert v1 `ranks.<id>.advancements` into v2 `ranks.<id>.requirement.completed`.
- Keep legacy keys readable for at least one major release.

Messages should also be versioned or checked for missing keys so new command output can be added without replacing a server owner's custom wording.

## Minecraft and Paper Compatibility Concerns

The following APIs and assumptions are version-sensitive and should be isolated:

- `Bukkit.advancementIterator()` and `Player#getAdvancementProgress(Advancement)` are central to online advancement counting.
- `PlayerAdvancementDoneEvent` behavior should be verified across supported Paper versions.
- Recipe filtering currently depends on advancement key paths starting with `recipes/`.
- Offline advancement files are assumed to live at `<world>/advancements/<uuid>.json` and contain a top-level `done` boolean per advancement.
- `Registry.SOUNDS` and sound key parsing can vary by Minecraft/Paper version.
- Adventure title and component APIs are available through modern Paper, but should remain wrapped for easier compatibility testing.
- `plugin.yml api-version: '1.21'` and the Gradle dependency `io.papermc.paper:paper-api:1.21.8-R0.1-SNAPSHOT` identify the current compile target.
- Java 21 is the current toolchain and should remain the baseline until a deliberate compatibility decision is made.
- LuckPerms API 5.4 is stable, but primary group behavior should still be guarded by clear error handling.
- Floodgate remains optional and should stay behind reflection or a soft dependency adapter.

Confirmed compatibility should only list versions that have actually been tested. Additional versions may work when the used Bukkit/Paper APIs remain compatible, but should not be advertised as confirmed without testing.

## Recommended v2 Migration Sequence

1. Add tests around v1 rank threshold behavior, advancement filtering, command permissions, and promotion suppression.
2. Extract rank definitions into a `RankRegistry` while preserving the current config shape.
3. Introduce display names and explicit LuckPerms group fields with v1-compatible defaults.
4. Extract player storage behind an interface and keep YAML as the first provider.
5. Add config and messages versioning with backup-based migration.
6. Extract LuckPerms and Floodgate into integration adapters.
7. Add internal rank change result/event objects.
8. Move command rendering into shared view/message helpers.
9. Add PlaceholderAPI/TAB/LPC/DiscordSRV integrations one at a time behind optional adapters.
10. Expand manual server testing across the confirmed Paper versions before publishing v2.

## Risks and Regression Areas

- Accidentally announcing rank changes on join, reload, or sync.
- Demoting or removing unrelated LuckPerms inheritance groups.
- Breaking offline-mode UUID handling for Java or Bedrock players.
- Counting recipe advancements or partially completed advancements.
- Losing customized messages/config during migration.
- Failing to load when optional integrations are absent.
- Blocking the server thread with large database saves or offline file scans.
- Regressing `/playerrank`, `/rank top`, or admin command permissions.

## Testing Strategy

Recommended automated tests:

- Rank selection at every threshold boundary.
- Next-rank and remaining-advancement calculations.
- Promotion detection for same-rank, rank-up, and non-advancement updates.
- Recipe advancement filtering by namespaced key path.
- YAML storage read/write and legacy key compatibility.
- Username lookup with UUID as primary identity.
- Permission-gated command visibility.
- Config migration from v1 rank format to v2 format.

Recommended manual server tests:

- Java player join, advancement completion, quit, and rejoin.
- Bedrock player detection with Floodgate installed.
- Bedrock prefix fallback when Floodgate is absent.
- Offline-mode UUID persistence.
- LuckPerms primary group changes without removing unrelated groups.
- `/rank`, `/playerrank`, `/rank top`, `/rank sync`, `/rank recalculate`, `/rank info`, and `/rank reload`.
- Broadcast/private promotion modes, title display, and sound playback.

## Suggested v2 Development Phases

### Phase 2: Safety Net

Add targeted tests and small helper extraction without changing user-facing behavior.

### Phase 3: Configurable Rank Core

Introduce rank definitions with display names, explicit LuckPerms groups, validation, and migration from v1 defaults.

### Phase 4: Storage Provider Boundary

Move YAML persistence behind a storage interface and preserve existing `players.yml` data.

### Phase 5: Integration Boundaries

Separate LuckPerms and Floodgate adapters, then add optional hooks for PlaceholderAPI, TAB, LPC, and DiscordSRV.

### Phase 6: Compatibility and Release Prep

Test confirmed Paper versions, document compatibility honestly, prepare migration notes, and publish v2 artifacts through GitHub Releases.
