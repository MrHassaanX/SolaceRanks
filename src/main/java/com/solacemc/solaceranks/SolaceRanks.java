package com.solacemc.solaceranks;

import com.solacemc.solaceranks.commands.PlayerRankCommand;
import com.solacemc.solaceranks.commands.RankCommand;
import com.solacemc.solaceranks.listeners.AdvancementListener;
import com.solacemc.solaceranks.manager.FloodgateManager;
import com.solacemc.solaceranks.manager.LuckPermsManager;
import com.solacemc.solaceranks.manager.PlayerDatabase;
import com.solacemc.solaceranks.manager.RankService;
import net.luckperms.api.LuckPerms;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Map;
import java.util.Objects;

public final class SolaceRanks extends JavaPlugin {

    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.legacyAmpersand();

    private FileConfiguration messages;
    private FloodgateManager floodgateManager;
    private PlayerDatabase playerDatabase;
    private RankService rankService;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadMessages();

        RegisteredServiceProvider<LuckPerms> provider = getServer().getServicesManager().getRegistration(LuckPerms.class);
        if (provider == null) {
            getLogger().severe("LuckPerms is required, but it was not found.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        floodgateManager = new FloodgateManager(this);
        playerDatabase = new PlayerDatabase(this, floodgateManager);
        LuckPermsManager luckPermsManager = new LuckPermsManager(provider.getProvider());
        rankService = new RankService(this, luckPermsManager, playerDatabase);

        getServer().getPluginManager().registerEvents(new AdvancementListener(this, rankService), this);

        RankCommand rankCommand = new RankCommand(this, rankService, playerDatabase);
        PluginCommand command = Objects.requireNonNull(getCommand("rank"), "rank command is missing from plugin.yml");
        command.setExecutor(rankCommand);
        command.setTabCompleter(rankCommand);

        PlayerRankCommand playerRankCommand = new PlayerRankCommand(this, rankService, playerDatabase);
        PluginCommand playerRankPluginCommand = Objects.requireNonNull(getCommand("playerrank"), "playerrank command is missing from plugin.yml");
        playerRankPluginCommand.setExecutor(playerRankCommand);
        playerRankPluginCommand.setTabCompleter(playerRankCommand);

        getLogger().info("====================================");
        getLogger().info(" SolaceRanks Enabled");
        getLogger().info("====================================");
    }

    @Override
    public void onDisable() {
        if (playerDatabase != null) {
            playerDatabase.save();
        }

        getLogger().info("SolaceRanks Disabled");
    }

    public RankService getRankService() {
        return rankService;
    }

    public PlayerDatabase getPlayerDatabase() {
        return playerDatabase;
    }

    public void reloadPlugin() {
        reloadConfig();
        loadMessages();
        floodgateManager.reload();
        rankService.reload();
    }

    public Component formatMessage(String key) {
        return formatMessage(key, Map.of());
    }

    public Component formatMessage(String key, Map<String, String> placeholders) {
        String message = messages.getString(key, key);
        String prefix = messages.getString("prefix", "");

        return formatText(prefix + message, placeholders);
    }

    public Component formatText(String message, Map<String, String> placeholders) {
        String formattedMessage = message;

        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            formattedMessage = formattedMessage.replace(entry.getKey(), entry.getValue());
        }

        return LEGACY_SERIALIZER.deserialize(formattedMessage);
    }

    private void loadMessages() {
        File messagesFile = new File(getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            saveResource("messages.yml", false);
        }

        messages = YamlConfiguration.loadConfiguration(messagesFile);
    }

}
