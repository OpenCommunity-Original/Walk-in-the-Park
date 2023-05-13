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

        Item item = new Item(Material.getMaterial(material.toUpperCase()), name);

        if (!lore.isEmpty()) {
            item.lore(lore.split("\\|\\|"));
        }

        return item;
    }

    private static final Pattern pattern = Pattern.compile("%[a-z]");
}