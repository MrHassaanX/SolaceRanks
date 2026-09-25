package com.solacemc.solaceranks.manager;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.InheritanceNode;

import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public final class LuckPermsManager {

    private final LuckPerms luckPerms;

    public LuckPermsManager(LuckPerms luckPerms) {
        this.luckPerms = luckPerms;
    }

    public CompletableFuture<Boolean> setPrimaryGroup(UUID uuid, String username, String groupName, Set<String> managedGroups) {
        Set<String> normalizedManagedGroups = managedGroups.stream()
                .map(group -> group.toLowerCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());

        return luckPerms.getUserManager().loadUser(uuid, username).thenCompose(user -> {
            boolean changed = !user.getPrimaryGroup().equalsIgnoreCase(groupName);

            // Keep unrelated LuckPerms inheritance untouched.
            user.data().clear(node -> isManagedRankGroup(node, normalizedManagedGroups));
            user.data().add(InheritanceNode.builder(groupName).build());
            user.setPrimaryGroup(groupName);

            return luckPerms.getUserManager().saveUser(user).thenApply(ignored -> changed);
        });
    }

    private boolean isManagedRankGroup(Node node, Set<String> managedGroups) {
        if (!NodeType.INHERITANCE.matches(node)) {
            return false;
        }

        String groupName = NodeType.INHERITANCE.cast(node).getGroupName();
        return managedGroups.contains(groupName.toLowerCase(Locale.ROOT));
    }
}
