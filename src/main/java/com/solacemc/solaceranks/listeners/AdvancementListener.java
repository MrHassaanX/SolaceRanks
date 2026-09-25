package com.solacemc.solaceranks.listeners;

import com.solacemc.solaceranks.SolaceRanks;
import com.solacemc.solaceranks.manager.RankUpdateReason;
import com.solacemc.solaceranks.manager.RankService;
import com.solacemc.solaceranks.utils.AdvancementUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

public final class AdvancementListener implements Listener {

    private final SolaceRanks plugin;
    private final RankService rankService;

    public AdvancementListener(SolaceRanks plugin, RankService rankService) {
        this.plugin = plugin;
        this.rankService = rankService;
    }

    @EventHandler
    public void onAdvancement(PlayerAdvancementDoneEvent event) {
        if (AdvancementUtils.isRecipeAdvancement(event.getAdvancement())) {
            return;
        }

        rankService.updatePlayerRank(event.getPlayer(), true, RankUpdateReason.ADVANCEMENT);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        rankService.recordPlayerData(event.getPlayer());

        UUID uuid = event.getPlayer().getUniqueId();

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                rankService.updatePlayerRank(player, false, RankUpdateReason.JOIN);
            }
        }, 20L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        rankService.recordPlayerData(event.getPlayer());
    }
}
