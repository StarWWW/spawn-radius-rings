package spawnradius;

import net.minecraft.world.entity.MobCategory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * One distance ring around the player. The distances are read from the game's own
 * {@link MobCategory} values at startup, so they always match the running Minecraft version.
 */
public record SpawnRing(String id, Kind kind, int radius, List<MobCategory> categories, int defaultColor) {

    public enum Kind {
        /** Natural spawning never places a mob this close to a player (sphere, strictly greater than). */
        NO_SPAWN,
        /** Beyond this distance a despawnable mob that is not near a player has a 1/800 chance per tick to despawn. */
        RANDOM_DESPAWN,
        /** Beyond this distance a despawnable mob is removed immediately. */
        INSTANT_DESPAWN;

        /**
         * Part of the translation keys. Locale.ROOT matters: with a Turkish system locale a plain toLowerCase()
         * turns "INSTANT" into "ınstant" (dotless ı) and the text would not be found.
         */
        public final String key = name().toLowerCase(Locale.ROOT);
    }

    /**
     * Minimum distance between a player and a natural spawn position.
     * NaturalSpawner rejects any position with distanceToSqr &lt;= 24*24 (576.0).
     */
    public static final int NO_SPAWN_RADIUS = 24;

    private static List<SpawnRing> rings;

    public static List<SpawnRing> all() {
        if (rings == null) rings = build();
        return rings;
    }

    private static List<SpawnRing> build() {
        Map<Integer, List<MobCategory>> randomDespawn = new TreeMap<>();
        Map<Integer, List<MobCategory>> instantDespawn = new TreeMap<>();
        List<MobCategory> spawning = new ArrayList<>();
        for (MobCategory category : MobCategory.values()) {
            if (category == MobCategory.MISC) continue; // MISC is never naturally spawned
            spawning.add(category);
            randomDespawn.computeIfAbsent(category.getNoDespawnDistance(), k -> new ArrayList<>()).add(category);
            instantDespawn.computeIfAbsent(category.getDespawnDistance(), k -> new ArrayList<>()).add(category);
        }

        List<SpawnRing> list = new ArrayList<>();
        list.add(new SpawnRing("no_spawn_" + NO_SPAWN_RADIUS, Kind.NO_SPAWN, NO_SPAWN_RADIUS,
            Collections.unmodifiableList(spawning), 0xFFFF5050));

        int[] randomColors = {0xFFFFD23F, 0xFFFF9A3C};
        int i = 0;
        for (var entry : randomDespawn.entrySet()) {
            list.add(new SpawnRing("random_despawn_" + entry.getKey(), Kind.RANDOM_DESPAWN, entry.getKey(),
                Collections.unmodifiableList(entry.getValue()), randomColors[Math.min(i++, randomColors.length - 1)]));
        }

        // smaller instant-despawn distance (fish) gets cyan, the common one green
        int[] instantColors = {0xFF4FC3F7, 0xFF5BE06A, 0xFFB066FF};
        i = 0;
        int count = instantDespawn.size();
        for (var entry : instantDespawn.entrySet()) {
            int color = count == 1 ? 0xFF5BE06A : instantColors[Math.min(i, instantColors.length - 1)];
            i++;
            list.add(new SpawnRing("despawn_" + entry.getKey(), Kind.INSTANT_DESPAWN, entry.getKey(),
                Collections.unmodifiableList(entry.getValue()), color));
        }
        list.sort((a, b) -> Integer.compare(a.radius, b.radius));
        return Collections.unmodifiableList(list);
    }

    /** Short text drawn next to the ring on the map. */
    public static String labelText(SpawnRing ring) {
        return net.minecraft.network.chat.Component.translatable(
            "spawnradius.label." + ring.kind().key, ring.radius()).getString();
    }

    /** Translation key for a category's display name. */
    public static String categoryKey(MobCategory category) {
        return "spawnradius.category." + category.getName();
    }
}
