package spawnradius.client;

import org.junit.jupiter.api.Test;
import spawnradius.SpawnRadiusConfig;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The map maths: sphere slices for the Y level, and which chunks can spawn mobs. */
class GeometryTest {
    private static final double RANGE_SQ = SpawnChunkOverlay.SPAWN_CHUNK_RANGE * SpawnChunkOverlay.SPAWN_CHUNK_RANGE;

    @Test
    void playerHeightShowsTheFullRadius() {
        SpawnRadiusConfig c = new SpawnRadiusConfig();
        assertEquals(128, MapOverlayRenderer.sliceRadius(128, 70, c), 1e-9);
    }

    @Test
    void fixedHeightShowsTheSphereSlice() {
        SpawnRadiusConfig c = new SpawnRadiusConfig();
        c.yMode = SpawnRadiusConfig.YMode.FIXED;
        c.fixedY = 64;
        // 30 blocks below the centre: sqrt(128² - 30²)
        assertEquals(Math.sqrt(128 * 128 - 30 * 30), MapOverlayRenderer.sliceRadius(128, 94, c), 1e-9);
        assertEquals(24, MapOverlayRenderer.sliceRadius(24, 64, c), 1e-9);
        assertEquals(-1, MapOverlayRenderer.sliceRadius(24, 100, c), "the 24-block sphere does not reach 36 blocks down");
        assertEquals(-1, MapOverlayRenderer.sliceRadius(24, 88, c), "exactly touching is not inside");
    }

    private static boolean eligible(int chunkX, int chunkZ, double x, double z, int simulationDistance) {
        MapOverlayRenderer.Center center = new MapOverlayRenderer.Center(x, 64, z, false);
        return SpawnChunkOverlay.eligible(chunkX, chunkZ, center, (int) Math.floor(x) >> 4, (int) Math.floor(z) >> 4,
            RANGE_SQ, simulationDistance);
    }

    @Test
    void spawnChunksNeedTheirCentreWithin128Blocks() {
        // player at the centre of chunk 0,0 (block 8, 8): chunk 8 has its centre at x=136, exactly 128 away
        assertTrue(eligible(0, 0, 8, 8, 0));
        assertTrue(eligible(7, 0, 8, 8, 0));
        assertFalse(eligible(8, 0, 8, 8, 0), "128 is not closer than 128");
        // diagonals reach less far than straight lines
        assertTrue(eligible(5, 5, 8, 8, 0));
        assertFalse(eligible(6, 6, 8, 8, 0));
    }

    @Test
    void spawnChunksStayInsideTheSimulationDistance() {
        assertTrue(eligible(4, 4, 8, 8, 4));
        assertFalse(eligible(5, 0, 8, 8, 4), "outside a simulation distance of 4 even though it is close enough");
        assertFalse(eligible(4, 5, 8, 8, 4));
        assertTrue(eligible(-4, -4, 8, 8, 4));
    }

    @Test
    void negativeCoordinatesUseFloorNotTruncation() {
        // block -1 is in chunk -1, not chunk 0
        assertTrue(eligible(-1, -1, -1, -1, 1));
        assertFalse(eligible(1, 1, -1, -1, 1));
    }
}
