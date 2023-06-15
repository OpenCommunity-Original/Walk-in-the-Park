package dev.efnilite.ip;

import dev.efnilite.ip.api.Registry;
import dev.efnilite.ip.config.Config;
import dev.efnilite.ip.config.Option;
import dev.efnilite.ip.hook.HoloHook;
import dev.efnilite.ip.hook.PAPIHook;
import dev.efnilite.ip.mode.DefaultMode;
import dev.efnilite.ip.mode.Modes;
import dev.efnilite.ip.mode.SpectatorMode;
import dev.efnilite.ip.player.ParkourUser;
import dev.efnilite.ip.session.SessionChat;
import dev.efnilite.ip.storage.Storage;
import dev.efnilite.ip.storage.StorageDisk;
import dev.efnilite.ip.storage.StorageSQL;
import dev.efnilite.ip.util.LocaleAPI;
import dev.efnilite.ip.world.WorldManager;
import dev.efnilite.vilib.ViPlugin;
import dev.efnilite.vilib.util.Logging;
import dev.efnilite.vilib.util.Time;
import dev.efnilite.vilib.util.elevator.GitElevator;
import dev.efnilite.vilib.util.elevator.VersionComparator;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginLogger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.logging.Logger;

/**
 * Main class of Infinite Parkour
 *
 * @author Efnilite
 * Copyright (c) 2020-2023
 */
public final class IP extends ViPlugin {

    public static final String NAME = "<#FF6464><bold>Infinite Parkour<reset>";
    public static final String PREFIX = NAME + " <dark_gray>» <gray>";
    public static final String REQUIRED_VILIB_VERSION = "1.2.0";

    PluginLogger logging = new PluginLogger(this);
    private static IP instance;
    private static Storage storage;

    @Nullable
    private static PAPIHook placeholderHook;


    @Override
    public void onLoad() {
        instance = this;
    }

    @Override
    public void enable() {
        // ----- Check vilib -----

        Plugin vilib = getServer().getPluginManager().getPlugin("vilib");
        if (vilib == null || !vilib.isEnabled()) {
            logging.severe("##");
            logging.severe("## Infinite Parkour requires vilib to work!");
            logging.severe("##");
            logging.severe("## Please download it here:");
            logging.severe("## https://github.com/Efnilite/vilib/releases/latest");
            logging.severe("##");

            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        if (!VersionComparator.FROM_SEMANTIC.isLatest(REQUIRED_VILIB_VERSION, vilib.getDescription().getVersion())) {
            logging.severe("##");
            logging.severe("## Infinite Parkour requires *a newer version* of vilib to work!");
            logging.severe("##");
            logging.severe("## Please download it here: ");
            logging.severe("## https://github.com/Efnilite/vilib/releases/latest");
            logging.severe("##");

            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // ----- Start time -----

        Time.timerStart("load");

        // ----- Configurations -----

        Config.reload(true);

        // ----- SQL and data -----

        storage = Option.SQL ? new StorageSQL() : new StorageDisk();

        // ----- Registry -----

        Registry.register(new DefaultMode());
        Registry.register(new SpectatorMode());

        Modes.init();

        // hook with hd / papi after gamemode leaderboards have initialized
        if (getServer().getPluginManager().isPluginEnabled("HolographicDisplays")) {
            logging.info("Connecting with Holographic Displays...");
            HoloHook.init();
        }
        if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            logging.info("Connecting with PlaceholderAPI...");
            placeholderHook = new PAPIHook();
            placeholderHook.register();
        }

        if (Option.ON_JOIN) {
            logging.info("Connecting with BungeeCord..");
            getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        }

        // ----- Worlds -----

        if (Option.JOINING) {
            WorldManager.create();
        }

        // ----- Events -----

        registerListener(new Handler());
        registerListener(new SessionChat());
        registerCommand("ip", new ParkourCommand());

        // ----- Locale -----

        LocaleAPI localeAPI = new LocaleAPI();
        Bukkit.getPluginManager().registerEvents(localeAPI, this);
        localeAPI.loadSupportedLocales(this);
    }

    @Override
    public void disable() {
        for (ParkourUser user : ParkourUser.getUsers()) {
            ParkourUser.leave(user);
        }

        // write all IP gamemodes
        Modes.DEFAULT.getLeaderboard().write(false);

        storage.close();
        WorldManager.delete();
    }

    @Override
    @NotNull
    public GitElevator getElevator() {
        return new GitElevator("", this, VersionComparator.FROM_SEMANTIC, false);
    }

    /**
     * @param child The file name.
     * @return A file from within the plugin folder.
     */
    public static File getInFolder(String child) {
        return new File(instance.getDataFolder(), child);
    }
    public static @NotNull Logger logging() {
        return getPlugin().getLogger();
    }

    /**
     * @return The plugin instance.
     */
    public static IP getPlugin() {
        return instance;
    }

    @Nullable
    public static PAPIHook getPlaceholderHook() {
        return placeholderHook;
    }

    public static Storage getStorage() {
        return storage;
    }
}