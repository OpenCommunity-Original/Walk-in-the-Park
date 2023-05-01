package dev.efnilite.ip.mode;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import dev.efnilite.ip.config.Locales;
import dev.efnilite.ip.config.Option;
import dev.efnilite.ip.generator.ParkourGenerator;
import dev.efnilite.ip.leaderboard.Leaderboard;
import dev.efnilite.ip.menu.community.SingleLeaderboardMenu;
import dev.efnilite.ip.player.ParkourPlayer;
import dev.efnilite.ip.player.ParkourUser;
import dev.efnilite.ip.session.Session;
import dev.efnilite.vilib.inventory.item.Item;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static dev.efnilite.ip.util.Util.send;

/**
 * The default parkour mode
 */
public class DefaultMode implements Mode {

    private final Leaderboard leaderboard = new Leaderboard(getName(), SingleLeaderboardMenu.Sort.SCORE);

    @Override
    @NotNull
    public String getName() {
        return "default";
    }

    @Override
    @Nullable
    public Item getItem(Player player) {
        return Locales.getItem(player, "play.single.default");
    }

    @Override
    @NotNull
    public Leaderboard getLeaderboard() {
        return leaderboard;
    }

    @Override
    public void create(Player player) {
        if (!Option.JOINING) {
            send(player, "<red><bold>Joining is currently disabled.");
            return;
        }

        if (Option.SPAWNONLY && Bukkit.getPluginManager().isPluginEnabled("WorldGuard")) {
            Location playerLoc = player.getLocation();
            RegionManager regionManager = WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(player.getWorld()));
            ApplicableRegionSet regions = regionManager.getApplicableRegions(BukkitAdapter.asBlockVector(playerLoc));

            if (!regions.getRegions().stream().anyMatch(region -> region.getId().equalsIgnoreCase("spawn"))) {
                send(player, Locales.getString(player, "other.spawn_only"));
                return;
            }
        }

        ParkourPlayer pp = ParkourPlayer.getPlayer(player);
        if (pp != null && pp.session.generator.getMode() instanceof DefaultMode) {
            return;
        }
        player.closeInventory();

        Session.create(ParkourGenerator::new)
                .addPlayers(ParkourUser.register(player))
                .complete();
    }
}
