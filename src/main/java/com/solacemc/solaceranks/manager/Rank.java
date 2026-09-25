package com.solacemc.solaceranks.manager;

public final class Rank {

    private final String name;
    private final int requiredAdvancements;

    public Rank(String name, int requiredAdvancements) {
        this.name = name;
        this.requiredAdvancements = requiredAdvancements;
    }

    public String getName() {
        return name;
    }

    public int getRequiredAdvancements() {
        return requiredAdvancements;
    }

    @Override
    public String toString() {
        return name;
    }
}
