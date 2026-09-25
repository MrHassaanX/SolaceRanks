package com.solacemc.solaceranks.utils;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.solacemc.solaceranks.SolaceRanks;
import org.bukkit.World;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;

public final class AdvancementFileUtils {

    private static final String RECIPE_ADVANCEMENT_PREFIX = "recipes/";

    private AdvancementFileUtils() {
    }

    public static OptionalInt countCompletedAdvancements(SolaceRanks plugin, UUID uuid) {
        Set<String> completedAdvancements = new HashSet<>();
        boolean foundFile = false;

        for (World world : plugin.getServer().getWorlds()) {
            // Offline players store completed advancement state in per-world JSON files.
            File advancementFile = new File(new File(world.getWorldFolder(), "advancements"), uuid + ".json");
            if (!advancementFile.isFile()) {
                continue;
            }

            foundFile = true;
            readCompletedAdvancements(plugin, advancementFile, completedAdvancements);
        }

        return foundFile ? OptionalInt.of(completedAdvancements.size()) : OptionalInt.empty();
    }

    private static void readCompletedAdvancements(SolaceRanks plugin, File advancementFile, Set<String> completedAdvancements) {
        try (Reader reader = Files.newBufferedReader(advancementFile.toPath(), StandardCharsets.UTF_8)) {
            JsonElement rootElement = JsonParser.parseReader(reader);
            if (!rootElement.isJsonObject()) {
                return;
            }

            JsonObject root = rootElement.getAsJsonObject();

            for (String advancementKey : root.keySet()) {
                JsonElement advancementElement = root.get(advancementKey);
                if (!advancementElement.isJsonObject()) {
                    continue;
                }

                JsonObject advancementObject = advancementElement.getAsJsonObject();
                JsonElement doneElement = advancementObject.get("done");
                if (doneElement == null || !doneElement.isJsonPrimitive() || !doneElement.getAsBoolean()) {
                    continue;
                }

                if (!isRecipeAdvancementKey(advancementKey)) {
                    completedAdvancements.add(advancementKey);
                }
            }
        } catch (IOException | IllegalStateException exception) {
            plugin.getLogger().warning("Could not read advancement file " + advancementFile.getPath() + ": " + exception.getMessage());
        }
    }

    private static boolean isRecipeAdvancementKey(String advancementKey) {
        int namespaceSeparator = advancementKey.indexOf(':');
        String path = namespaceSeparator >= 0 ? advancementKey.substring(namespaceSeparator + 1) : advancementKey;

        return path.startsWith(RECIPE_ADVANCEMENT_PREFIX);
    }
}
