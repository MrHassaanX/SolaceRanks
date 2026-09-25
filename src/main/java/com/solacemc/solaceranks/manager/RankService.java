package com.solacemc.solaceranks.manager;

import com.solacemc.solaceranks.SolaceRanks;
import com.solacemc.solaceranks.utils.AdvancementFileUtils;
import com.solacemc.solaceranks.utils.AdvancementUtils;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;

public final class RankService {

    private static final List<Rank> DEFAULT_RANKS = List.of(
            new Rank("newbie", 0),
            new Rank("wanderer", 10),
            new Rank("explorer", 20),
            new Rank("guardian", 30),
            new Rank("luminary", 40),
            new Rank("eclipse", 50),
            new Rank("celestial", 60),
            new Rank("eternal", 70),
            new Rank("divine", 80),
            new Rank("ascendant", 90),
            new Rank("archon", 100)
    );

    private final SolaceRanks plugin;
    private final LuckPermsManager luckPermsManager;
    private final PlayerDatabase playerDatabase;

    private List<Rank> ranks = DEFAULT_RANKS;
    private Set<String> managedGroups = groupNames(DEFAULT_RANKS);

    public RankService(SolaceRanks plugin, LuckPermsManager luckPermsManager, PlayerDatabase playerDatabase) {
        this.plugin = plugin;
        this.luckPermsManager = luckPermsManager;
        this.playerDatabase = playerDatabase;
        reload();
    }

    public void reload() {
        List<Rank> loadedRanks = loadRanks();
        ranks = List.copyOf(loadedRanks);
        managedGroups = groupNames(loadedRanks);
    }

    public Rank getRankForAdvancements(int advancements) {
        Rank current = ranks.getFirst();

        for (Rank rank : ranks) {
            if (advancements < rank.getRequiredAdvancements()) {
                break;
            }

            current = rank;
        }

        return current;
    }

    public Rank getNextRank(int advancements) {
        for (Rank rank : ranks) {
            if (rank.getRequiredAdvancements() > advancements) {
                return rank;
            }
        }

        return null;
    }

    public Collection<Rank> getRanks() {
        return ranks;
    }

    public Rank getRankByName(String name) {
        for (Rank rank : ranks) {
            if (rank.getName().equalsIgnoreCase(name)) {
                return rank;
            }
        }

        return null;
    }

    public void updatePlayerRank(Player player, boolean announceChange, RankUpdateReason reason) {
        int advancements = AdvancementUtils.getCompletedAdvancements(player);
        Rank rank = getRankForAdvancements(advancements);
        UUID uuid = player.getUniqueId();
        String username = player.getName();
        PlayerData previousData = playerDatabase.get(uuid);
        // Only advancement events can produce public promotion effects.
        boolean promoted = announceChange && reason == RankUpdateReason.ADVANCEMENT && isPromotion(previousData, rank);

        playerDatabase.updateProgress(player, rank.getName(), advancements);

        luckPermsManager.setPrimaryGroup(uuid, username, rank.getName(), managedGroups).whenComplete((changed, throwable) -> {
            if (throwable != null) {
                Throwable cause = throwable instanceof CompletionException && throwable.getCause() != null
                        ? throwable.getCause()
                        : throwable;
                plugin.getLogger().warning("Failed to update LuckPerms rank for " + username + ": " + cause.getMessage());
                return;
            }

            if (!promoted) {
                return;
            }

            Bukkit.getScheduler().runTask(plugin, () -> {
                Player onlinePlayer = Bukkit.getPlayer(uuid);
                if (onlinePlayer == null || !onlinePlayer.isOnline()) {
                    return;
                }

                announcePromotion(onlinePlayer, rank);
            });
        });
    }

    public CompletableFuture<RankSyncResult> syncOnlinePlayer(Player player) {
        int advancements = AdvancementUtils.getCompletedAdvancements(player);
        Rank rank = getRankForAdvancements(advancements);
        PlayerData updatedData = playerDatabase.updateProgress(player, rank.getName(), advancements);

        return luckPermsManager.setPrimaryGroup(player.getUniqueId(), player.getName(), rank.getName(), managedGroups)
                .thenApply(changed -> new RankSyncResult(updatedData, rank, advancements, true, changed));
    }

    public CompletableFuture<RankSyncResult> syncStoredPlayer(PlayerData data) {
        OptionalInt advancementCount = AdvancementFileUtils.countCompletedAdvancements(plugin, data.getUuid());
        int advancements = advancementCount.orElse(data.getAdvancementCount());

        return updateStoredPlayer(data, advancements, advancementCount.isPresent());
    }

    public CompletableFuture<RankSyncResult> recalculateStoredPlayer(PlayerData data) {
        return updateStoredPlayer(data, data.getAdvancementCount(), false);
    }

    public void recordPlayerData(Player player) {
        int advancements = AdvancementUtils.getCompletedAdvancements(player);
        Rank rank = getRankForAdvancements(advancements);

        playerDatabase.updateProgress(player, rank.getName(), advancements);
    }

    public void updateOnlinePlayers(boolean announceChange, RankUpdateReason reason) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            updatePlayerRank(player, announceChange, reason);
        }
    }

    private List<Rank> loadRanks() {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("ranks");
        if (section == null) {
            plugin.getLogger().warning("No ranks section found in config.yml. Using default thresholds.");
            return DEFAULT_RANKS;
        }

        List<Rank> loadedRanks = new ArrayList<>();

        for (String key : section.getKeys(false)) {
            int requiredAdvancements = section.getInt(key + ".advancements", -1);

            if (requiredAdvancements < 0) {
                plugin.getLogger().warning("Skipping rank '" + key + "' because its advancement threshold is invalid.");
                continue;
            }

            loadedRanks.add(new Rank(key.toLowerCase(Locale.ROOT), requiredAdvancements));
        }

        if (loadedRanks.isEmpty()) {
            plugin.getLogger().warning("No valid ranks found in config.yml. Using default thresholds.");
            return DEFAULT_RANKS;
        }

        loadedRanks.sort(Comparator.comparingInt(Rank::getRequiredAdvancements));
        return loadedRanks;
    }

    private boolean isPromotion(PlayerData previousData, Rank newRank) {
        if (previousData == null || previousData.getCurrentRank().isBlank()) {
            return false;
        }

        if (previousData.getCurrentRank().equalsIgnoreCase(newRank.getName())) {
            return false;
        }

        Rank previousRank = getRankByName(previousData.getCurrentRank());
        return previousRank != null && newRank.getRequiredAdvancements() > previousRank.getRequiredAdvancements();
    }

    private void announcePromotion(Player player, Rank rank) {
        Map<String, String> placeholders = Map.of(
                "%player%", player.getName(),
                "%rank%", rank.getName()
        );

        if (plugin.getConfig().getBoolean("broadcast-rankup", true)) {
            Bukkit.broadcast(plugin.formatMessage("rank-up-broadcast", placeholders));
        } else {
            player.sendMessage(plugin.formatMessage("rank-up", placeholders));
        }

        playPromotionSound(player);
        showPromotionTitle(player, placeholders);
    }

    private void playPromotionSound(Player player) {
        if (!plugin.getConfig().getBoolean("sound.enabled", true)) {
            return;
        }

        String soundName = plugin.getConfig().getString("sound.type", "ENTITY_PLAYER_LEVELUP");
        if (soundName == null || soundName.isBlank()) {
            return;
        }

        Sound sound = getConfiguredSound(soundName);
        if (sound == null) {
            plugin.getLogger().warning("Invalid rank-up sound in config.yml: " + soundName);
            return;
        }

        player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
    }

    private void showPromotionTitle(Player player, Map<String, String> placeholders) {
        if (!plugin.getConfig().getBoolean("title.enabled", true)) {
            return;
        }

        String title = plugin.getConfig().getString("title.title", "&6Rank Up!");
        String subtitle = plugin.getConfig().getString("title.subtitle", "&fYou are now %rank%");

        player.showTitle(net.kyori.adventure.title.Title.title(
                plugin.formatText(title == null ? "" : title, placeholders),
                plugin.formatText(subtitle == null ? "" : subtitle, placeholders)
        ));
    }

    private Sound getConfiguredSound(String soundName) {
        String keyName = soundName.toLowerCase(Locale.ROOT);
        NamespacedKey key = keyName.contains(":")
                ? NamespacedKey.fromString(keyName)
                : NamespacedKey.minecraft(keyName.replace('_', '.'));

        return key == null ? null : Registry.SOUNDS.get(key);
    }

    private CompletableFuture<RankSyncResult> updateStoredPlayer(PlayerData data, int advancements, boolean advancementFileUsed) {
        Rank rank = getRankForAdvancements(advancements);
        PlayerData updatedData = playerDatabase.updateStoredProgress(data, rank.getName(), advancements);
        String username = updatedData.getUsername().isBlank() ? updatedData.getUuid().toString() : updatedData.getUsername();

        return luckPermsManager.setPrimaryGroup(updatedData.getUuid(), username, rank.getName(), managedGroups)
                .thenApply(changed -> new RankSyncResult(updatedData, rank, advancements, advancementFileUsed, changed));
    }

    private static Set<String> groupNames(Collection<Rank> ranks) {
        return ranks.stream()
                .map(Rank::getName)
                .map(name -> name.toLowerCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());
    }
}
