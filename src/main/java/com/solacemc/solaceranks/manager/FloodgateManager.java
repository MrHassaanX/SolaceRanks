package com.solacemc.solaceranks.manager;

import com.solacemc.solaceranks.SolaceRanks;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.UUID;

public final class FloodgateManager {

    private static final String BEDROCK_PREFIX = "br_";

    private final SolaceRanks plugin;

    private Object floodgateApi;
    private Method isFloodgatePlayerMethod;
    private Method getPlayerMethod;

    public FloodgateManager(SolaceRanks plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        floodgateApi = null;
        isFloodgatePlayerMethod = null;
        getPlayerMethod = null;

        Plugin floodgatePlugin = plugin.getServer().getPluginManager().getPlugin("floodgate");
        if (floodgatePlugin == null || !floodgatePlugin.isEnabled()) {
            return;
        }

        try {
            Class<?> apiClass = Class.forName("org.geysermc.floodgate.api.FloodgateApi");
            floodgateApi = apiClass.getMethod("getInstance").invoke(null);
            isFloodgatePlayerMethod = apiClass.getMethod("isFloodgatePlayer", UUID.class);
            getPlayerMethod = apiClass.getMethod("getPlayer", UUID.class);
            plugin.getLogger().info("Floodgate detected. Bedrock player data tracking is enabled.");
        } catch (ReflectiveOperationException exception) {
            floodgateApi = null;
            plugin.getLogger().warning("Floodgate was detected, but its API could not be loaded: " + exception.getMessage());
        }
    }

    public boolean isFloodgatePlayer(Player player) {
        if (floodgateApi == null || isFloodgatePlayerMethod == null) {
            return hasBedrockUsernamePrefix(player);
        }

        try {
            return Boolean.TRUE.equals(isFloodgatePlayerMethod.invoke(floodgateApi, player.getUniqueId()))
                    || hasBedrockUsernamePrefix(player);
        } catch (ReflectiveOperationException exception) {
            return hasBedrockUsernamePrefix(player);
        }
    }

    public String getStoredUsername(Player player) {
        String serverUsername = player.getName();
        if (serverUsername != null && !serverUsername.isBlank()) {
            return serverUsername;
        }

        Object floodgatePlayer = getFloodgatePlayer(player.getUniqueId());
        if (floodgatePlayer == null) {
            return "";
        }

        String username = readStringMethod(floodgatePlayer, "getCorrectUsername");
        if (username != null && !username.isBlank()) {
            return username;
        }

        username = readStringMethod(floodgatePlayer, "getUsername");
        if (username != null && !username.isBlank()) {
            return username;
        }

        return "";
    }

    private boolean hasBedrockUsernamePrefix(Player player) {
        return player.getName().toLowerCase().startsWith(BEDROCK_PREFIX);
    }

    private Object getFloodgatePlayer(UUID uuid) {
        if (floodgateApi == null || getPlayerMethod == null) {
            return null;
        }

        try {
            return getPlayerMethod.invoke(floodgateApi, uuid);
        } catch (ReflectiveOperationException exception) {
            return null;
        }
    }

    private String readStringMethod(Object target, String methodName) {
        try {
            Object value = target.getClass().getMethod(methodName).invoke(target);
            return value instanceof String stringValue ? stringValue : null;
        } catch (ReflectiveOperationException exception) {
            return null;
        }
    }
}
