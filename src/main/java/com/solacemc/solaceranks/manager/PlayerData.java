package com.solacemc.solaceranks.manager;

import java.util.UUID;

public final class PlayerData {

    private final UUID uuid;
    private final String username;
    private final boolean bedrockPlayer;
    private final String currentRank;
    private final String previousRank;
    private final int advancementCount;
    private final String firstJoin;
    private final String lastSeen;

    public PlayerData(
            UUID uuid,
            String username,
            boolean bedrockPlayer,
            String currentRank,
            String previousRank,
            int advancementCount,
            String firstJoin,
            String lastSeen
        ) {
        this.uuid = uuid;
        this.username = username;
        this.bedrockPlayer = bedrockPlayer;
        this.currentRank = currentRank;
        this.previousRank = previousRank;
        this.advancementCount = advancementCount;
        this.firstJoin = firstJoin;
        this.lastSeen = lastSeen;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getUsername() {
        return username;
    }

    public boolean isBedrockPlayer() {
        return bedrockPlayer;
    }

    public String getCurrentRank() {
        return currentRank;
    }

    public String getPreviousRank() {
        return previousRank;
    }

    public int getAdvancementCount() {
        return advancementCount;
    }

    public String getFirstJoin() {
        return firstJoin;
    }

    public String getLastSeen() {
        return lastSeen;
    }
}
