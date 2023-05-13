package dev.efnilite.ip.util;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLocaleChangeEvent;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.bukkit.Bukkit.getServer;

/**
 * The LocaleAPI class provides a simple and efficient way to manage player
 * locales and retrieve localized messages based on the player's locale.
 */
public class LocaleAPI implements Listener {
    private static final Map<Player, Locale> playerLocales = new HashMap<>();
    private static final Locale DEFAULT_LOCALE = Locale.US;
    private static final List<Locale> SUPPORTED_LOCALES = new ArrayList<>();
    private static String baseName;
    private static final Map<String, YamlConfiguration> configurationCache = new ConcurrentHashMap<>();

    /**
     * Sets the locale for a player.
     *
     * @param player The player to set the locale for
     * @param locale The locale to set
     */
    private static void setPlayerLocale(Player player, Locale locale) {
        playerLocales.put(player, locale);
    }

    /**
     * Loads a configuration file asynchronously.
     *
     * @param file The file to load
     * @return A CompletableFuture that will complete with the loaded configuration file or null if there was an error
     * @implNote The loaded configuration file is stored in cache for future use.
     */
    private static CompletableFuture<YamlConfiguration> loadConfigurationAsync(File file) {
        String key = file.getAbsolutePath();
        YamlConfiguration cachedConfig = configurationCache.get(key);
        if (cachedConfig != null) {
            return CompletableFuture.completedFuture(cachedConfig);
        }
        return CompletableFuture.supplyAsync(() -> {
            try {
                YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                configurationCache.put(key, config);
                return config;
            } catch (Exception e) {
                Bukkit.getLogger().warning("Error loading configuration file: " + e.getMessage());
                return null;
            }
        });
    }

    /**
     * Gets the player's locale. If the player doesn't have a locale set,
     * it will try to get the locale from the player's settings. If the
     * locale is supported, it will be set for the player. Otherwise, the
     * default locale will be set for the player.
     *
     * @param player the player
     * @return the player's locale
     */
    private static Locale getPlayerLocale(Player player) {
        if (playerLocales.containsKey(player)) {
            return playerLocales.get(player);
        }

        Locale locale = player.locale();
        if (isLocaleSupported(locale)) {
            setPlayerLocale(player, locale);
        } else {
            setPlayerLocale(player, DEFAULT_LOCALE);
            locale = DEFAULT_LOCALE;
        }

        return locale;
    }

    /**
     * Gets the message for the specified key and player's locale. If the
     * message isn't found for the player's locale, it will try to find it
     * for the default locale. If the message still isn't found, it will
     * return null.
     *
     * @param player the player
     * @param key    the message key
     * @return the message or null if not found
     */
    public static String getMessage(Player player, String key) {
        Locale locale = getPlayerLocale(player);
        String lang = locale.toLanguageTag();
        File file = new File(baseName, lang + ".lang");

        CompletableFuture<YamlConfiguration> future = loadConfigurationAsync(file);
        String message = future.thenApply(config -> config.getString(key))
                .exceptionally(e -> {
                    Bukkit.getLogger().warning("Error getting message: " + e.getMessage());
                    return null;
                })
                .join();

        if (message == null) {
            CompletableFuture<YamlConfiguration> fallback = loadConfigurationAsync(new File(baseName, DEFAULT_LOCALE.toLanguageTag() + ".lang"));
            message = fallback.thenApply(config -> config.getString(key))
                    .exceptionally(e -> {
                        Bukkit.getLogger().warning("Error getting message: " + e.getMessage());
                        return null;
                    })
                    .join();
        }

        return message;
    }


    /**
     * Checks if the locale is supported.
     *
     * @param locale the locale
     * @return true if supported, false otherwise
     */
    private static boolean isLocaleSupported(Locale locale) {
        return SUPPORTED_LOCALES.contains(locale);
    }

    /**
     * Copies the Messages folder from the plugin resources to the plugin folder.
     * This is used to provide default message files.
     *
     * @param plugin The plugin
     */
    private static void copyMessages(Plugin plugin) {
        File sourceFolder = new File(plugin.getDataFolder(), "Messages");
        if (!sourceFolder.exists()) {
            sourceFolder.mkdirs();
        }

        try {
            // Open the plugin jar file as a ZipFile
            File pluginFile = new File(plugin.getClass().getProtectionDomain().getCodeSource().getLocation().toURI());
            ZipFile zipFile = new ZipFile(pluginFile);

            // Loop through the contents of the jar file to find the Messages folder
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String entryName = entry.getName();
                if (entryName.startsWith("Messages/") && !entry.isDirectory()) {
                    // Extract the file to the Messages folder in the plugin folder
                    File targetFile = new File(sourceFolder, entryName.substring("Messages/".length()));
                    if (!targetFile.getParentFile().exists()) {
                        targetFile.getParentFile().mkdirs();
                    }
                    InputStream inputStream = zipFile.getInputStream(entry);
                    Files.copy(inputStream, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    inputStream.close();
                }
            }

            zipFile.close();
        } catch (Exception e) {
            Bukkit.getLogger().warning("Error copying Messages folder from plugin resources: " + e.getMessage());
        }
    }


    /**
     * Loads the supported locales from the plugin's messages folder.
     * If no locales are found, the plugin will be disabled.
     *
     * @param plugin the plugin
     */
    public void loadSupportedLocales(Plugin plugin) {
        baseName = plugin.getDataFolder().getAbsolutePath() + File.separator + "Messages" + File.separator;
        File messagesFolder = new File(baseName);
        if (messagesFolder.listFiles() == null) {
            copyMessages(plugin);
        }
        File[] messageFiles = messagesFolder.listFiles();
        if (messageFiles != null) {
            for (File file : messageFiles) {
                String fileName = file.getName();
                if (fileName.endsWith(".lang")) {
                    String localeString = fileName.substring(0, fileName.indexOf(".lang"));
                    Locale locale = Locale.forLanguageTag(localeString.replace("_", "-"));
                    if (locale != null) {
                        SUPPORTED_LOCALES.add(locale);
                    }
                }
            }
        }
        if (SUPPORTED_LOCALES.isEmpty()) {
            Bukkit.getLogger().warning("Failed to load any language files.");
            getServer().getPluginManager().disablePlugin(plugin);
        }
    }

    /**
     * Gets a string list for the specified key and player's locale. If the
     * string list isn't found for the player's locale, it will try to find it
     * for the default locale. If the string list still isn't found, it will
     * return an empty list.
     *
     * @param player the player
     * @param key    the string list key
     * @return the string list or an empty list if not found
     */
    public static List<String> getStringList(Player player, String key) {
        Locale locale = getPlayerLocale(player);
        String lang = locale.toLanguageTag();
        File file = new File(baseName, lang + ".lang");

        CompletableFuture<YamlConfiguration> future = loadConfigurationAsync(file);
        List<String> stringList = future.thenApply(config -> config.getStringList(key))
                .exceptionally(e -> {
                    Bukkit.getLogger().warning("Error getting message: " + e.getMessage());
                    return null;
                })
                .join();

        if (stringList == null) {
            CompletableFuture<YamlConfiguration> fallback = loadConfigurationAsync(new File(baseName, DEFAULT_LOCALE.toLanguageTag() + ".lang"));
            stringList = fallback.thenApply(config -> config.getStringList(key))
                    .exceptionally(e -> {
                        Bukkit.getLogger().warning("Error getting message: " + e.getMessage());
                        return null;
                    })
                    .join();
        }

        return stringList != null ? stringList : Collections.emptyList();
    }

    /**
     * Handles the PlayerLocaleChangeEvent.
     * Updates the locale for the player if it is supported.
     * Otherwise sets the default locale.
     *
     * @param event The PlayerLocaleChangeEvent
     */
    @EventHandler
    public void onPlayerLocaleChange(PlayerLocaleChangeEvent event) {
        Player player = event.getPlayer();
        Locale locale = event.locale();
        if (isLocaleSupported(locale)) {
            setPlayerLocale(player, locale);
        } else {
            setPlayerLocale(player, DEFAULT_LOCALE);
        }
    }
}
