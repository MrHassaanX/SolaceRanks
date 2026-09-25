package com.solacemc.solaceranks.manager;

public record RankSyncResult(
        PlayerData playerData,
        Rank rank,
        int advancements,
        boolean countRecalculated,
        boolean luckPermsChanged
) {
}
