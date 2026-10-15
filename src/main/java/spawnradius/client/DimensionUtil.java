package spawnradius.client;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

final class DimensionUtil {
    private DimensionUtil() {
    }

    /** Stable string id of a dimension, e.g. "minecraft:overworld". */
    static String id(ResourceKey<Level> key) {
        return key.identifier().toString();
    }
}
