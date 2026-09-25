package com.solacemc.solaceranks.commands;

import com.solacemc.solaceranks.SolaceRanks;
import com.solacemc.solaceranks.manager.PlayerData;
import com.solacemc.solaceranks.manager.PlayerDatabase;
import com.solacemc.solaceranks.manager.Rank;
import com.solacemc.solaceranks.manager.RankService;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class PlayerRankCommand implements CommandExecutor, TabCompleter {

    private final SolaceRanks plugin;
    private final RankService rankService;
    private final PlayerDatabase playerDatabase;

    public PlayerRankCommand(SolaceRanks plugin, RankService rankService, PlayerDatabase playerDatabase) {
        this.plugin = plugin;
        this.rankService = rankService;
        this.playerDatabase = playerDatabase;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("solaceranks.playerrank")) {
            sender.sendMessage(plugin.formatMessage("no-permission"));
            return true;
        }

        if (args.length != 1) {
            sender.sendMessage(plugin.formatMessage("playerrank-usage"));
            return true;
        }

        PlayerData data = playerDatabase.findByUsername(args[0]);
        if (data == null) {
            sender.sendMessage(plugin.formatMessage("playerrank-not-found", Map.of("%player%", args[0])));
            return true;
        }

        int advancements = data.getAdvancementCount();
        Rank currentRank = data.getCurrentRank().isBlank()
                ? rankService.getRankForAdvancements(advancements)
                : new Rank(data.getCurrentRank(), rankService.getRankForAdvancements(advancements).getRequiredAdvancements());
        Rank nextRank = rankService.getNextRank(advancements);
        String nextRankName = nextRank == null ? "None" : displayRankName(nextRank.getName());
        String remaining = nextRank == null ? "0" : String.valueOf(nextRank.getRequiredAdvancements() - advancements);

        Map<String, String> placeholders = Map.of(
                "%player%", data.getUsername(),
                "%rank%", displayRankName(currentRank.getName()),
                "%advancements%", String.valueOf(advancements),
                "%next_rank%", nextRankName,
                "%remaining%", remaining
        );

        sender.sendMessage(plugin.formatText("&8-----------------------------", Map.of()));
        sender.sendMessage(plugin.formatText(plugin.getConfig().getString("playerrank-format.player", "&7Player: &f%player%"), placeholders));
        sender.sendMessage(plugin.formatText(plugin.getConfig().getString("playerrank-format.rank", "&7Rank: &e%rank%"), placeholders));
        sender.sendMessage(plugin.formatText(plugin.getConfig().getString("playerrank-format.advancements", "&7Advancements: &f%advancements%"), placeholders));
        sender.sendMessage(plugin.formatText(plugin.getConfig().getString("playerrank-format.next-rank", "&7Next Rank: &e%next_rank%"), placeholders));
        sender.sendMessage(plugin.formatText(plugin.getConfig().getString("playerrank-format.remaining", "&7Remaining: &f%remaining%"), placeholders));
        sender.sendMessage(plugin.formatText("&8-----------------------------", Map.of()));
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length != 1 || !sender.hasPermission("solaceranks.playerrank")) {
            return List.of();
        }

        String input = args[0].toLowerCase(Locale.ROOT);
        Set<String> names = new LinkedHashSet<>(playerDatabase.getKnownUsernames());

        for (Player player : Bukkit.getOnlinePlayers()) {
            names.add(player.getName());
        }

        List<String> completions = new ArrayList<>();
        for (String name : names) {
            if (name.toLowerCase(Locale.ROOT).startsWith(input)) {
                completions.add(name);
            }
        }

        return completions;
    }

    private String displayRankName(String rankName) {
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
}
