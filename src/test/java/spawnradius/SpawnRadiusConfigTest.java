package spawnradius;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpawnRadiusConfigTest {
    @TempDir
    Path dir;

    @AfterEach
    void cleanUp() {
        SpawnRadiusConfig.testConfigDir = null;
    }

    private static SpawnRing first() {
        return SpawnRing.all().get(0);
    }

    @Test
    void savesAndLoadsEverything() {
        SpawnRadiusConfig.testConfigDir = dir;
        SpawnRadiusConfig c = new SpawnRadiusConfig();
        c.enabled = false;
        c.labels = false;
        c.spawnChunks = true;
        c.thickness = 3;
        c.yMode = SpawnRadiusConfig.YMode.FIXED;
        c.fixedY = -40;
        c.centerMode = SpawnRadiusConfig.CenterMode.PINNED;
        c.pinX = 100.5;
        c.pinY = 64;
        c.pinZ = -200.25;
        c.pinDimension = "minecraft:the_nether";
        c.setRingEnabled(first(), false);
        c.cycleRingColor(first());
        int color = c.getRingColor(first());
        c.save();

        SpawnRadiusConfig loaded = new SpawnRadiusConfig();
        loaded.load();
        assertFalse(loaded.enabled);
        assertFalse(loaded.labels);
        assertTrue(loaded.spawnChunks);
        assertEquals(3, loaded.thickness);
        assertEquals(SpawnRadiusConfig.YMode.FIXED, loaded.yMode);
        assertEquals(-40, loaded.fixedY);
        assertEquals(SpawnRadiusConfig.CenterMode.PINNED, loaded.centerMode);
        assertEquals(-200.25, loaded.pinZ);
        assertEquals("minecraft:the_nether", loaded.pinDimension);
        assertFalse(loaded.isRingEnabled(first()));
        assertEquals(color, loaded.getRingColor(first()));
    }

    @Test
    void badValuesFallBackOrAreClamped() throws IOException {
        SpawnRadiusConfig.testConfigDir = dir;
        Files.writeString(dir.resolve("spawnradius.properties"),
            "thickness=99\nfixedY=999999\nyMode=SIDEWAYS\nlabels=maybe\nring.no_spawn_24.color=zzz\n");
        SpawnRadiusConfig c = new SpawnRadiusConfig();
        c.load();
        assertEquals(4, c.thickness);
        assertEquals(SpawnRadiusConfig.MAX_Y, c.fixedY);
        assertEquals(SpawnRadiusConfig.YMode.PLAYER, c.yMode);
        assertFalse(c.labels, "anything but \"true\" is false, like Boolean.parseBoolean");
        assertEquals(first().defaultColor(), c.getRingColor(first()));
    }

    @Test
    void colorsCycleThroughThePalette() {
        SpawnRadiusConfig c = new SpawnRadiusConfig();
        int start = c.getRingColor(first());
        for (int i = 0; i < SpawnRadiusConfig.PALETTE.length; i++) {
            c.cycleRingColor(first());
            if (i < SpawnRadiusConfig.PALETTE.length - 1) assertNotEquals(start, c.getRingColor(first()));
        }
        assertEquals(start, c.getRingColor(first()), "back to the start after a full cycle");
    }

    @Test
    void resetKeepsThePinnedPosition() {
        SpawnRadiusConfig c = new SpawnRadiusConfig();
        c.thickness = 4;
        c.outline = false;
        c.pinX = 7;
        c.pinDimension = "minecraft:overworld";
        c.centerMode = SpawnRadiusConfig.CenterMode.PINNED;
        c.resetToDefaults();
        assertEquals(2, c.thickness);
        assertTrue(c.outline);
        assertEquals(SpawnRadiusConfig.CenterMode.PLAYER, c.centerMode);
        assertEquals(7, c.pinX);
        assertEquals("minecraft:overworld", c.pinDimension);
    }
}
