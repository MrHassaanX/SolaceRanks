package com.solacemc.solaceranks.utils;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.advancement.Advancement;
import org.bukkit.entity.Player;

import java.util.Iterator;

public final class AdvancementUtils {

    private static final String RECIPE_ADVANCEMENT_PREFIX = "recipes/";

    private AdvancementUtils() {
    }

    public static int getCompletedAdvancements(Player player) {
        int completed = 0;
        Iterator<Advancement> iterator = Bukkit.advancementIterator();

        while (iterator.hasNext()) {
            Advancement advancement = iterator.next();

            if (isCountedAdvancement(advancement) && player.getAdvancementProgress(advancement).isDone()) {
                completed++;
            }
        }

        return completed;
    }

    public static boolean isCountedAdvancement(Advancement advancement) {
        return !isRecipeAdvancement(advancement);
    }

    public static boolean isRecipeAdvancement(Advancement advancement) {
        NamespacedKey key = advancement.getKey();

        return key.getKey().startsWith(RECIPE_ADVANCEMENT_PREFIX);
    }
}
