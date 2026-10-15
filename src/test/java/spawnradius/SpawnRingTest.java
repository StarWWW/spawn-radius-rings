package spawnradius;

import net.minecraft.world.entity.MobCategory;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The rings come from the game's own MobCategory values; this pins down what 26.1.2 uses. */
class SpawnRingTest {
    private static SpawnRing ring(String id) {
        return SpawnRing.all().stream().filter(r -> r.id().equals(id)).findFirst().orElseThrow(() ->
            new AssertionError("no ring " + id + " in " + SpawnRing.all().stream().map(SpawnRing::id).toList()));
    }

    @Test
    void ringsMatchTheGame() {
        assertEquals(List.of(24, 32, 64, 128), SpawnRing.all().stream().map(SpawnRing::radius).toList());
        assertEquals(SpawnRing.Kind.NO_SPAWN, ring("no_spawn_24").kind());
        assertEquals(SpawnRing.Kind.RANDOM_DESPAWN, ring("random_despawn_32").kind());
        assertEquals(SpawnRing.Kind.INSTANT_DESPAWN, ring("despawn_64").kind());
        assertEquals(SpawnRing.Kind.INSTANT_DESPAWN, ring("despawn_128").kind());
    }

    @Test
    void categoriesAreSplitByDistance() {
        assertEquals(List.of(MobCategory.WATER_AMBIENT), ring("despawn_64").categories(), "fish despawn at 64");
        assertTrue(ring("despawn_128").categories().contains(MobCategory.MONSTER));
        assertFalse(ring("no_spawn_24").categories().contains(MobCategory.MISC), "MISC never spawns naturally");
    }

    @Test
    void translationKeysDoNotDependOnTheSystemLanguage() {
        Locale before = Locale.getDefault();
        try {
            // Turkish lower-cases "I" to a dotless "ı", which used to break "instant_despawn"
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            for (SpawnRing.Kind kind : SpawnRing.Kind.values()) {
                assertTrue(kind.key.matches("[a-z_]+"), kind.key);
            }
            assertEquals("instant_despawn", SpawnRing.Kind.INSTANT_DESPAWN.key);
        } finally {
            Locale.setDefault(before);
        }
    }
}
