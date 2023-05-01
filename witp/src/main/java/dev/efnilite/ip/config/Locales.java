package dev.efnilite.ip.config;

import dev.efnilite.ip.player.ParkourUser;
import dev.efnilite.ip.util.LocaleAPI;
import dev.efnilite.vilib.inventory.item.Item;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Locale message/item handler.
 */
public class Locales {

    // a list of all nodes, used to check against missing nodes
    private static List<String> resourceNodes;

    /**
     * A map of all locales with their respective yml trees
     */
    public static final Map<String, FileConfiguration> locales = new HashMap<>();

    /**
     * Initializes this Locale handler.
     */
    public static void init() {
        Plugin plugin = IP.getPlugin();

        Task.create(plugin).async().execute(() -> {
            locales.clear();

            FileConfiguration embedded = YamlConfiguration.loadConfiguration(new InputStreamReader(plugin.getResource("locales/en.yml"), StandardCharsets.UTF_8));

            // get all nodes from the plugin's english resource, aka the most updated version
            resourceNodes = Util.getChildren(embedded, "", true);

            File folder = IP.getInFolder("locales");

            // download files to locales folder
            if (!folder.exists()) {
                folder.mkdirs();
            }

            String[] files = folder.list();

            // create non-existent files
            if (files != null && files.length == 0) {
                plugin.saveResource("locales/en.yml", false);
                plugin.saveResource("locales/nl.yml", false);
                plugin.saveResource("locales/fr.yml", false);
                plugin.saveResource("locales/zh_cn.yml", false);
            }

            // get all files in locales folder
            try (Stream<Path> stream = Files.list(folder.toPath())) {
                stream.forEach(path -> {
                    File file = path.toFile();

                    // get locale from file name
                    String locale = file.getName().split("\\.")[0];

                    FileConfiguration config = YamlConfiguration.loadConfiguration(file);
                    validate(embedded, config, file);

                    locales.put(locale, config);
                });
            } catch (Exception ex) {
                IP.logging().stack("Error while trying to read locale files", "restart/reload your server", ex);
            }
        }).run();
    }

    // validates whether a lang file contains all required keys.
    // if it doesn't, automatically add them
    private static void validate(FileConfiguration provided, FileConfiguration user, File localPath) {
        List<String> userNodes = Util.getChildren(user, "", true);

        for (String node : resourceNodes) {
            if (userNodes.contains(node)) {
                continue;
            }

            IP.logging().info("Fixing missing config node %s (%s)".formatted(node, localPath.getName()));

            user.set(node, provided.get(node));
        }

        try {
            user.save(localPath);
        } catch (IOException ex) {
            IP.logging().stack("Error while trying to save fixed config file %s".formatted(localPath), "delete this file and restart your server", ex);
        }
    }

    /**
     * Gets a String from the provided path in the provided player's locale.
     * If the player is a {@link ParkourUser}, their locale value will be used.
     * If not, the default locale will be used.
     *
     * @param player The player
     * @param path   The path
     * @return a String
     */
    public static String getString(Player player, String path) {
        return LocaleAPI.getMessage(player, path);
    }

    /**
     * Gets an uncoloured String list from the provided path in the provided locale file
     */
    public static List<String> getStringList(Player player, String path) {
        return LocaleAPI.getStringList(player, path);
    }

    private static <T> T getValue(String locale, Function<FileConfiguration, T> f, T def) {
        if (locales.isEmpty()) {
            return def;
        }

        FileConfiguration config = locales.get(locale);

        return config != null ? f.apply(config) : def;
    }

    /**
     * Returns an item from a json locale file.
     * The locale is derived from the player.
     * If the player is a {@link ParkourUser}, their locale value will be used.
     * If not, the default locale will be used.
     *
     * @param player The player
     * @param path   The full path of the item in the locale file
     * @return a non-null {@link Item} instance built from the description in the locale file
     */
    public static @NotNull Item getItem(@NotNull Player player, String path, String... replace) {
        ParkourUser user = ParkourUser.getUser(player);
        String locale = user == null ? Option.OPTIONS_DEFAULTS.get(ParkourOption.LANG) : user.locale;

        return getItem(locale, path, replace);
    }

    private static final Pattern pattern = Pattern.compile("%[a-z]");

    /**
     * Returns an item from a provided json locale file with possible replacements.
     *
     * @param locale  The locale
     * @param path    The path in the json file
     * @param replace The Strings that will replace any appearances of a String following the regex "%[a-z]"
     * @return a non-null {@link Item} instance built from the description in the locale file
     */
    @NotNull
    public static Item getItem(Player player, String key, String... replace) {
        String material = LocaleAPI.getMessage(player, key + ".material");
        String name = LocaleAPI.getMessage(player, key + ".name");
        String lore = LocaleAPI.getMessage(player, key + ".lore");

        if (material == null) {
            material = "";
        }
        if (name == null) {
            name = "";
        }

        int idx = 0;
        Matcher matcher = pattern.matcher(name);
        while (matcher.find()) {
            if (idx == replace.length) {
                break;
            }

            name = name.replaceFirst(matcher.group(), replace[idx]);
            idx++;
        }

        matcher = pattern.matcher(lore);

        while (matcher.find()) {
            if (idx == replace.length) {
                break;
            }

            lore = lore.replaceFirst(matcher.group(), replace[idx]);
            idx++;
        }

        return item;
    }
}