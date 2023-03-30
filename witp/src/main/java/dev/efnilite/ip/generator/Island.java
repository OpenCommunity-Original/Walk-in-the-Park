package dev.efnilite.ip.generator;

import dev.efnilite.ip.IP;
import dev.efnilite.ip.config.Config;
import dev.efnilite.ip.player.ParkourPlayer;
import dev.efnilite.ip.schematic.Schematic;
import dev.efnilite.ip.session.Session;
import dev.efnilite.ip.util.Colls;
import dev.efnilite.ip.world.WorldDivider;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;

import java.util.List;

/**
 * Spawn island handler.
 */
public final class Island {

    /**
     * The blocks that have been affected by the schematic.
     */
    public List<Block> blocks;

    /**
     * The session.
     */
    public final Session session;

    /**
     * The schematic.
     */
    public final Schematic schematic;

    public Island(Session session, Schematic schematic) {
        this.session = session;
        this.schematic = schematic;
    }

    /**
     * Builds the island and teleports the player.
     */
    public void build() {
        blocks = schematic.paste(WorldDivider.toLocation(session).subtract(0, schematic.dimensions.getY(), 0));

        Material playerMaterial = Material.getMaterial(Config.GENERATION.getString("advanced.island.spawn.player-block").toUpperCase());
        Material parkourMaterial = Material.getMaterial(Config.GENERATION.getString("advanced.island.parkour.begin-block").toUpperCase());

        try {
            Block player = blocks.stream().filter(block -> block.getType() == playerMaterial).findFirst().get();
            Block parkour = Colls.filter(block -> block.getType() == parkourMaterial, blocks).get(0);

            player.setType(Material.AIR);
            parkour.setType(Material.AIR);

            ParkourPlayer pp = session.getPlayers().get(0);

            // todo remove ugliness
            Location ps = player.getLocation().add(0.5, 0, 0.5);
            ps.setYaw(Config.GENERATION.getInt("advanced.island.spawn.yaw"));
            ps.setPitch(Config.GENERATION.getInt("advanced.island.spawn.pitch"));

            pp.generator.generateFirst(player.getLocation(), parkour.getLocation());
            pp.setup(ps, true);
        } catch (IndexOutOfBoundsException ex) {
            IP.logging().stack("Error while trying to find parkour or player spawn in schematic %s".formatted(schematic.getName()),
                    "check if you used the same material as the one in generation.yml", ex);

            blocks.forEach(block -> block.setType(Material.AIR));
        }
    }

    /**
     * Destroys the island.
     */
    public void destroy() {
        if (blocks == null) {
            return;
        }

        for (Block block : blocks) {
            block.setType(Material.AIR, false);
        }
    }
}