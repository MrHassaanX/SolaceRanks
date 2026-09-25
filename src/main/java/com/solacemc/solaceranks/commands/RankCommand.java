package com.solacemc.solaceranks.commands;

import com.solacemc.solaceranks.SolaceRanks;
import com.solacemc.solaceranks.manager.PlayerData;
import com.solacemc.solaceranks.manager.PlayerDatabase;
import com.solacemc.solaceranks.manager.Rank;
import com.solacemc.solaceranks.manager.RankService;
import com.solacemc.solaceranks.manager.RankSyncResult;
import com.solacemc.solaceranks.manager.RankUpdateReason;
import com.solacemc.solaceranks.utils.AdvancementUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

public final class RankCommand implements CommandExecutor, TabCompleter {

    private final SolaceRanks plugin;
    private final RankService rankService;
    private final PlayerDatabase playerDatabase;

    public RankCommand(SolaceRanks plugin, RankService rankService, PlayerDatabase playerDatabase) {
        this.plugin = plugin;
        this.rankService = rankService;
        this.playerDatabase = playerDatabase;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length > 0) {
            return switch (args[0].toLowerCase(Locale.ROOT)) {
                case "help" -> handleHelp(sender);
                case "reload" -> handleReload(sender);
                case "top" -> handleTop(sender);
                case "sync" -> handleSync(sender, args);
                case "recalculate" -> handleRecalculate(sender, args);
                case "info" -> handleInfo(sender, args);
                default -> {
                    sender.sendMessage(plugin.formatMessage("usage"));
                    yield true;
                }
            };
        }

        return handleOwnRank(sender);
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> subcommands = new ArrayList<>();
            subcommands.add("top");

            if (sender.hasPermission("solaceranks.admin")) {
                subcommands.add("help");
                subcommands.add("reload");
                subcommands.add("sync");
                subcommands.add("recalculate");
                subcommands.add("info");
            }

            return matching(subcommands, args[0]);
        }

        if (args.length == 2 && sender.hasPermission("solaceranks.admin")
                && (args[0].equalsIgnoreCase("sync") || args[0].equalsIgnoreCase("info"))) {
            Set<String> names = new LinkedHashSet<>(playerDatabase.getKnownUsernames());
            for (Player player : Bukkit.getOnlinePlayers()) {
                names.add(player.getName());
            }

            return matching(new ArrayList<>(names), args[1]);
        }

        return List.of();
    }

    private boolean handleOwnRank(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.formatMessage("player-only"));
            return true;
        }

        if (!player.hasPermission("solaceranks.rank")) {
            player.sendMessage(plugin.formatMessage("no-permission"));
            return true;
        }

        int advancements = AdvancementUtils.getCompletedAdvancements(player);
        Rank currentRank = rankService.getRankForAdvancements(advancements);
        Rank nextRank = rankService.getNextRank(advancements);

        player.sendMessage(plugin.formatMessage("current-rank", Map.of("%rank%", currentRank.getName())));
        player.sendMessage(plugin.formatMessage("advancements", Map.of("%count%", String.valueOf(advancements))));

        if (nextRank == null) {
            player.sendMessage(plugin.formatMessage("max-rank"));
        } else {
            int remaining = nextRank.getRequiredAdvancements() - advancements;
            player.sendMessage(plugin.formatMessage("next-rank", Map.of(
                    "%rank%", nextRank.getName(),
                    "%needed%", String.valueOf(remaining)
            )));
        }

        rankService.updatePlayerRank(player, false, RankUpdateReason.COMMAND);
        return true;
    }

    private boolean handleHelp(CommandSender sender) {
        sender.sendMessage(plugin.formatText("&8-----------------------------", Map.of()));
        sender.sendMessage(plugin.formatText("&6SolaceRanks Commands", Map.of()));
        sender.sendMessage(plugin.formatText("&e/rank &7- Show your current rank.", Map.of()));
        sender.sendMessage(plugin.formatText("&e/rank top &7- Show the top 10 stored players.", Map.of()));
        if (sender.hasPermission("solaceranks.playerrank")) {
            sender.sendMessage(plugin.formatText("&e/playerrank <player> &7- Show a stored player's rank.", Map.of()));
        }

        if (sender.hasPermission("solaceranks.admin")) {
            sender.sendMessage(plugin.formatText("&e/rank help &7- Show this help menu.", Map.of()));
            sender.sendMessage(plugin.formatText("&e/rank reload &7- Reload configuration.", Map.of()));
            sender.sendMessage(plugin.formatText("&e/rank sync <player> &7- Recount and sync a stored player.", Map.of()));
            sender.sendMessage(plugin.formatText("&e/rank recalculate &7- Recalculate all stored ranks.", Map.of()));
            sender.sendMessage(plugin.formatText("&e/rank info <player> &7- Show detailed stored rank data.", Map.of()));
        }

        sender.sendMessage(plugin.formatText("&8-----------------------------", Map.of()));
        return true;
    }

    private boolean handleReload(CommandSender sender) {
        if (!hasAdminPermission(sender)) {
            return true;
        }

        plugin.reloadPlugin();
        rankService.updateOnlinePlayers(false, RankUpdateReason.COMMAND);
        sender.sendMessage(plugin.formatMessage("reload"));
        return true;
    }

    private boolean handleTop(CommandSender sender) {
        List<PlayerData> topPlayers = playerDatabase.getAllPlayers().stream()
                .sorted(Comparator.comparingInt(PlayerData::getAdvancementCount).reversed()
                        .thenComparing(PlayerData::getUsername, String.CASE_INSENSITIVE_ORDER))
                .limit(10)
                .toList();

        if (topPlayers.isEmpty()) {
            sender.sendMessage(plugin.formatText("&cNo stored player rank data was found.", Map.of()));
            return true;
        }

        sender.sendMessage(plugin.formatText("&8-----------------------------", Map.of()));
        sender.sendMessage(plugin.formatText("&6Top Advancement Ranks", Map.of()));

        int position = 1;
        for (PlayerData data : topPlayers) {
            String rankName = data.getCurrentRank().isBlank()
                    ? rankService.getRankForAdvancements(data.getAdvancementCount()).getName()
                    : data.getCurrentRank();
            sender.sendMessage(plugin.formatText("&e%position%. &f%player% &7- &e%rank% &8(&f%advancements%&8)", Map.of(
                    "%position%", String.valueOf(position),
                    "%player%", data.getUsername(),
                    "%rank%", displayRankName(rankName),
                    "%advancements%", String.valueOf(data.getAdvancementCount())
            )));
            position++;
        }

        sender.sendMessage(plugin.formatText("&8-----------------------------", Map.of()));
        return true;
    }

    private boolean handleSync(CommandSender sender, String[] args) {
        if (!hasAdminPermission(sender)) {
            return true;
        }

        if (args.length != 2) {
            sender.sendMessage(plugin.formatText("&cUsage: /rank sync <player>", Map.of()));
            return true;
        }

        Player onlinePlayer = findOnlinePlayer(args[1]);
        if (onlinePlayer != null) {
            sender.sendMessage(plugin.formatText("&7Syncing rank data for &e%player%&7...", Map.of("%player%", onlinePlayer.getName())));
            rankService.syncOnlinePlayer(onlinePlayer).whenComplete((result, throwable) -> sendSyncResult(sender, result, throwable));
            return true;
        }

        PlayerData data = playerDatabase.findByUsername(args[1]);
        if (data == null) {
            sender.sendMessage(plugin.formatText("&cNo stored rank data was found for &e%player%&c.", Map.of("%player%", args[1])));
            return true;
        }

        sender.sendMessage(plugin.formatText("&7Syncing stored rank data for &e%player%&7...", Map.of("%player%", data.getUsername())));
        rankService.syncStoredPlayer(data).whenComplete((result, throwable) -> sendSyncResult(sender, result, throwable));
        return true;
    }

    private boolean handleRecalculate(CommandSender sender, String[] args) {
        if (!hasAdminPermission(sender)) {
            return true;
        }

        if (args.length != 1) {
            sender.sendMessage(plugin.formatText("&cUsage: /rank recalculate", Map.of()));
            return true;
        }

        List<PlayerData> storedPlayers = playerDatabase.getAllPlayers();
        if (storedPlayers.isEmpty()) {
            sender.sendMessage(plugin.formatText("&cNo stored player rank data was found.", Map.of()));
            return true;
        }

        AtomicInteger completed = new AtomicInteger();
        AtomicInteger failed = new AtomicInteger();
        List<CompletableFuture<?>> futures = new ArrayList<>();

        sender.sendMessage(plugin.formatText("&7Recalculating &e%count% &7stored player ranks...", Map.of(
                "%count%", String.valueOf(storedPlayers.size())
        )));

        for (PlayerData data : storedPlayers) {
            Player onlinePlayer = Bukkit.getPlayer(data.getUuid());
            CompletableFuture<RankSyncResult> future = onlinePlayer == null
                    ? rankService.recalculateStoredPlayer(data)
                    : rankService.syncOnlinePlayer(onlinePlayer);

            futures.add(future.whenComplete((result, throwable) -> {
                if (throwable == null) {
                    completed.incrementAndGet();
                } else {
                    failed.incrementAndGet();
                    plugin.getLogger().warning("Failed to recalculate rank for " + data.getUsername() + ": " + throwable.getMessage());
                }
            }));
        }

        CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).whenComplete((ignored, throwable) ->
                Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(plugin.formatText(
                        "&aRecalculated &e%completed% &aplayers. &cFailed: &e%failed%&c.",
                        Map.of(
                                "%completed%", String.valueOf(completed.get()),
                                "%failed%", String.valueOf(failed.get())
                        )
                )))
        );
        return true;
    }

    private boolean handleInfo(CommandSender sender, String[] args) {
        if (!hasAdminPermission(sender)) {
            return true;
        }

        if (args.length != 2) {
            sender.sendMessage(plugin.formatText("&cUsage: /rank info <player>", Map.of()));
            return true;
        }

        PlayerData data = playerDatabase.findByUsername(args[1]);
        if (data == null) {
            sender.sendMessage(plugin.formatText("&cNo stored rank data was found for &e%player%&c.", Map.of("%player%", args[1])));
            return true;
        }

        int advancements = data.getAdvancementCount();
        String currentRankName = data.getCurrentRank().isBlank()
                ? rankService.getRankForAdvancements(advancements).getName()
                : data.getCurrentRank();
        Rank nextRank = rankService.getNextRank(advancements);
        String nextRankName = nextRank == null ? "None" : displayRankName(nextRank.getName());
        String remaining = nextRank == null ? "0" : String.valueOf(nextRank.getRequiredAdvancements() - advancements);

        sender.sendMessage(plugin.formatText("&8-----------------------------", Map.of()));
        sender.sendMessage(plugin.formatText("&7Player Name: &f%player%", Map.of("%player%", data.getUsername())));
        sender.sendMessage(plugin.formatText("&7UUID: &f%uuid%", Map.of("%uuid%", data.getUuid().toString())));
        sender.sendMessage(plugin.formatText("&7Edition: &f%edition%", Map.of("%edition%", data.isBedrockPlayer() ? "Bedrock" : "Java")));
        sender.sendMessage(plugin.formatText("&7Current Rank: &e%rank%", Map.of("%rank%", displayRankName(currentRankName))));
        sender.sendMessage(plugin.formatText("&7Previous Rank: &e%rank%", Map.of("%rank%", displayRankName(blankAsNone(data.getPreviousRank())))));
        sender.sendMessage(plugin.formatText("&7Completed Advancements: &f%advancements%", Map.of("%advancements%", String.valueOf(advancements))));
        sender.sendMessage(plugin.formatText("&7Next Rank: &e%rank%", Map.of("%rank%", nextRankName)));
        sender.sendMessage(plugin.formatText("&7Remaining Advancements: &f%remaining%", Map.of("%remaining%", remaining)));
        sender.sendMessage(plugin.formatText("&7First Join: &f%first_join%", Map.of("%first_join%", blankAsNone(data.getFirstJoin()))));
        sender.sendMessage(plugin.formatText("&7Last Seen: &f%last_seen%", Map.of("%last_seen%", blankAsNone(data.getLastSeen()))));
        sender.sendMessage(plugin.formatText("&8-----------------------------", Map.of()));
        return true;
    }

    private void sendSyncResult(CommandSender sender, RankSyncResult result, Throwable throwable) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (throwable != null) {
                String message = throwable.getMessage() == null ? throwable.getClass().getSimpleName() : throwable.getMessage();
                sender.sendMessage(plugin.formatText("&cRank sync failed: &e%error%", Map.of("%error%", message)));
                return;
            }

            sender.sendMessage(plugin.formatText("&aSynced &e%player%&a: &e%rank% &8(&f%advancements% &7advancements&8)&a.", Map.of(
                    "%player%", result.playerData().getUsername(),
                    "%rank%", displayRankName(result.rank().getName()),
                    "%advancements%", String.valueOf(result.advancements())
            )));

            if (!result.countRecalculated()) {
                sender.sendMessage(plugin.formatText("&7No offline advancement file was found; the stored advancement count was used.", Map.of()));
            }
        });
    }

    private boolean hasAdminPermission(CommandSender sender) {
        if (sender.hasPermission("solaceranks.admin")) {
            return true;
        }

        sender.sendMessage(plugin.formatMessage("no-permission"));
        return false;
    }

    private Player findOnlinePlayer(String name) {
        Player exactPlayer = Bukkit.getPlayerExact(name);
        if (exactPlayer != null) {
            return exactPlayer;
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getName().equalsIgnoreCase(name)) {
                return player;
            }
        }

        return null;
    }

    private List<String> matching(List<String> values, String input) {
        String normalizedInput = input.toLowerCase(Locale.ROOT);
        List<String> matches = new ArrayList<>();

        for (String value : values) {
            if (value.toLowerCase(Locale.ROOT).startsWith(normalizedInput)) {
                matches.add(value);
            }
        }

        return matches;
    }

    private String displayRankName(String rankName) {
        if (rankName == null || rankName.isBlank() || rankName.equalsIgnoreCase("none")) {
            return "None";
        }

        String[] words = rankName.replace('-', '_').split("_");
        List<String> displayWords = new ArrayList<>();

        for (String word : words) {
            if (word.isBlank()) {
                continue;
            }

            displayWords.add(word.substring(0, 1).toUpperCase(Locale.ROOT) + word.substring(1).toLowerCase(Locale.ROOT));
        }

        return String.join(" ", displayWords);
    }

    private String blankAsNone(String value) {
        return value == null || value.isBlank() ? "None" : value;
    }
}
