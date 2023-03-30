package dev.efnilite.ip.schematic.v2.io;

import dev.efnilite.ip.IP;
import dev.efnilite.ip.schematic.v2.Schematic2;
import dev.efnilite.ip.schematic.v2.state.State;
import dev.efnilite.ip.util.Colls;
import org.bukkit.Bukkit;
import org.bukkit.block.data.BlockData;
import org.bukkit.util.Vector;

import java.io.*;
import java.util.Map;

public class SchematicReader {

    /**
     * @param file The file.
     * @return A new {@link Schematic2} instance based on the read blocks.
     */
    @SuppressWarnings("unchecked")
    public Map<Vector, BlockData> read(File file) {
        try (ObjectInputStream stream = new ObjectInputStream(new BufferedInputStream(new FileInputStream(file)))) {
            var palette = (Map<String, Integer>) stream.readObject();
            var offsets = (Map<String, Object[]>) stream.readObject();

            // inverse map and parse String -> BlockData
            Map<Integer, BlockData> paletteRef = Colls.mapv((k, ov) -> Bukkit.createBlockData(ov), Colls.inverse(palette));

            // create final map by parse Map<String, Object> -> Vector and applying possible State

            return Colls.mapkv(this::fromString, v -> {
                BlockData data = paletteRef.get((int) v[0]);

                if (v.length == 1) {
                    return data;
                }

                State state = State.getState(data);
                String extra = (String) v[1];

                return state != null ? state.deserialize(data, extra) : data;
            }, offsets);
        } catch (IOException | ClassNotFoundException ex) {
            IP.logging().stack("Error while trying to read schematic %s".formatted(file), ex);
        }

        return null;
    }

    private Vector fromString(String string) {
        String[] parts = string.split(",");
        return new Vector(Double.parseDouble(parts[0]), Double.parseDouble(parts[1]), Double.parseDouble(parts[2]));
    }
}