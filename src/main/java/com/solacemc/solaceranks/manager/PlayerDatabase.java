package com.solacemc.solaceranks.manager;

import com.solacemc.solaceranks.SolaceRanks;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class PlayerDatabase {

    private static final String PLAYERS_PATH = "players";

    private final SolaceRanks plugin;
    private final FloodgateManager floodgateManager;
    private final File databaseFile;

    private FileConfiguration database;
    private BukkitTask pendingSaveTask;
    private boolean dirty;

    public PlayerDatabase(SolaceRanks plugin, FloodgateManager floodgateManager) {
        this.plugin = plugin;
        this.floodgateManager = floodgateManager;
        this.databaseFile = new File(plugin.getDataFolder(), "players.yml");

        load();
    }

    public synchronized void load() {
        if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
            plugin.getLogger().warning("Could not create plugin data folder for player database.");
        }

        database = YamlConfiguration.loadConfiguration(databaseFile);
    }

    public synchronized PlayerData get(UUID uuid) {
        ConfigurationSection section = database.getConfigurationSection(playerPath(uuid));
        if (section == null) {
            return null;
        }

        return read(uuid, section);
    }

    public synchronized PlayerData updateProgress(Player player, String currentRank, int advancementCount) {
        UUID uuid = player.getUniqueId();
        String path = playerPath(uuid);
        ConfigurationSection section = database.getConfigurationSection(path);

        if (section == null) {
            section = database.createSection(path);
        }

        String previousCurrentRank = section.getString("current-rank");
        String previousRank = section.getString("previous-rank");
        boolean rankChanged = previousCurrentRank != null && !previousCurrentRank.equalsIgnoreCase(currentRank);

        if (rankChanged) {
            previousRank = previousCurrentRank;
        } else if (previousRank == null || previousRank.isBlank()) {
            previousRank = previousCurrentRank == null || previousCurrentRank.isBlank() ? currentRank : previousCurrentRank;
        }

        String firstJoin = section.getString("first-join");
        if (firstJoin == null || firstJoin.isBlank()) {
            firstJoin = formatTimestamp(player.getFirstPlayed() > 0 ? player.getFirstPlayed() : System.currentTimeMillis());
        }

        String lastSeen = Instant.now().toString();

        section.set("uuid", uuid.toString());
        section.set("username", floodgateManager.getStoredUsername(player));
        section.set("bedrock", floodgateManager.isFloodgatePlayer(player));
        section.set("bedrock-player", null);
        section.set("floodgate-bedrock", null);
        section.set("current-rank", currentRank);
        section.set("previous-rank", previousRank);
        section.set("advancement-count", advancementCount);
        section.set("total-advancements-completed", null);
        section.set("first-join", firstJoin);
        section.set("last-seen", lastSeen);

        queueSave();
        return read(uuid, section);
    }

    public synchronized PlayerData updateStoredProgress(PlayerData data, String currentRank, int advancementCount) {
        UUID uuid = data.getUuid();
        String path = playerPath(uuid);
        ConfigurationSection section = database.getConfigurationSection(path);

        if (section == null) {
            section = database.createSection(path);
        }

        String previousCurrentRank = section.getString("current-rank", data.getCurrentRank());
        String previousRank = section.getString("previous-rank", data.getPreviousRank());
        boolean rankChanged = previousCurrentRank != null && !previousCurrentRank.equalsIgnoreCase(currentRank);

        if (rankChanged) {
            previousRank = previousCurrentRank;
        } else if (previousRank == null || previousRank.isBlank()) {
            previousRank = previousCurrentRank == null || previousCurrentRank.isBlank() ? currentRank : previousCurrentRank;
        }

        section.set("uuid", uuid.toString());
        section.set("username", data.getUsername());
        section.set("bedrock", data.isBedrockPlayer());
        section.set("bedrock-player", null);
        section.set("floodgate-bedrock", null);
        section.set("current-rank", currentRank);
        section.set("previous-rank", previousRank);
        section.set("advancement-count", advancementCount);
        section.set("total-advancements-completed", null);
        section.set("first-join", data.getFirstJoin());
        section.set("last-seen", data.getLastSeen());

        queueSave();
        return read(uuid, section);
    }

    public synchronized List<PlayerData> getAllPlayers() {
        ConfigurationSection playersSection = database.getConfigurationSection(PLAYERS_PATH);
        if (playersSection == null) {
            return List.of();
        }

        List<PlayerData> players = new ArrayList<>();

        for (String uuidText : playersSection.getKeys(false)) {
            ConfigurationSection section = playersSection.getConfigurationSection(uuidText);
            if (section == null) {
                continue;
            }

            try {
                players.add(read(UUID.fromString(uuidText), section));
            } catch (IllegalArgumentException exception) {
                plugin.getLogger().warning("Skipping invalid player UUID in database: " + uuidText);
            }
        }

        return players;
    }

    public synchronized PlayerData findByUsername(String username) {
        ConfigurationSection playersSection = database.getConfigurationSection(PLAYERS_PATH);
        if (playersSection == null) {
            return null;
        }

        String normalizedUsername = username.toLowerCase(Locale.ROOT);

        for (String uuidText : playersSection.getKeys(false)) {
            ConfigurationSection section = playersSection.getConfigurationSection(uuidText);
            if (section == null || !section.getString("username", "").toLowerCase(Locale.ROOT).equals(normalizedUsername)) {
                continue;
            }

            try {
                return read(UUID.fromString(uuidText), section);
            } catch (IllegalArgumentException exception) {
                plugin.getLogger().warning("Skipping invalid player UUID in database: " + uuidText);
            }
        }

        return null;
    }

    public synchronized List<String> getKnownUsernames() {
        ConfigurationSection playersSection = database.getConfigurationSection(PLAYERS_PATH);
        if (playersSection == null) {
            return List.of();
        }

        List<String> usernames = new ArrayList<>();

        for (String uuidText : playersSection.getKeys(false)) {
            String username = playersSection.getString(uuidText + ".username");
            if (username != null && !username.isBlank()) {
                usernames.add(username);
            }
        }

        return usernames;
    }

    public synchronized void save() {
        if (pendingSaveTask != null) {
            pendingSaveTask.cancel();
            pendingSaveTask = null;
        }

        dirty = false;
        saveNow();
    }

    private synchronized void queueSave() {
        dirty = true;

        if (pendingSaveTask != null) {
            return;
        }

        // Coalesce bursts of join/advancement updates into one disk write.
        pendingSaveTask = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            synchronized (PlayerDatabase.this) {
                pendingSaveTask = null;
                if (!dirty) {
                    return;
                }

                dirty = false;
                saveNow();
            }
        }, 20L);
    }

    private void saveNow() {
        try {
            database.save(databaseFile);
        } catch (IOException exception) {
            plugin.getLogger().warning("Could not save player database: " + exception.getMessage());
        }
    }

    private PlayerData read(UUID uuid, ConfigurationSection section) {
        return new PlayerData(
                uuid,
                section.getString("username", ""),
                section.getBoolean("bedrock", section.getBoolean("bedrock-player", section.getBoolean("floodgate-bedrock", false))),
                section.getString("current-rank", ""),
                section.getString("previous-rank", ""),
                section.getInt("advancement-count", section.getInt("total-advancements-completed", 0)),
                section.getString("first-join", ""),
                section.getString("last-seen", "")
        );
    }

    private String playerPath(UUID uuid) {
        return PLAYERS_PATH + "." + uuid;
    }

    private String formatTimestamp(long epochMillis) {
        return Instant.ofEpochMilli(epochMillis).toString();
    }
}
