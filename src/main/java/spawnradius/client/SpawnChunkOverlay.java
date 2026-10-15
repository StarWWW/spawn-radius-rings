package spawnradius.client;

/**
 * Shades the chunks in which natural spawning can run for a player standing at the centre.
 * Both of these must hold (checked against the 26.1.2 server code):
 * <ul>
 *   <li>the horizontal distance from the player to the centre of the chunk is below 128 blocks
 *       (ChunkMap#playerIsCloseEnoughForSpawning, 16384.0 = 128²);</li>
 *   <li>the chunk is entity-ticking, i.e. within the server's simulation distance of the player's chunk
 *       (a square: player ticket level 31 - simulationDistance, +1 per chunk step including diagonals).</li>
 * </ul>
 */
final class SpawnChunkOverlay {
    static final double SPAWN_CHUNK_RANGE = 128.0;
    static final int FILL = 0x3033FF66;
    static final int EDGE = 0xA033FF66;

    private SpawnChunkOverlay() {
    }

    static void draw(RingBatch batch, MapOverlayRenderer.Center center, double originX, double originY,
                     double cameraX, double cameraZ, double scale, double pxWidth, double pxHeight,
                     int simulationDistance) {
        int playerChunkX = (int) Math.floor(center.x()) >> 4;
        int playerChunkZ = (int) Math.floor(center.z()) >> 4;
        int reach = (int) Math.ceil(SPAWN_CHUNK_RANGE / 16.0) + 1;
        double rangeSq = SPAWN_CHUNK_RANGE * SPAWN_CHUNK_RANGE;
        for (int chunkX = playerChunkX - reach; chunkX <= playerChunkX + reach; chunkX++) {
            for (int chunkZ = playerChunkZ - reach; chunkZ <= playerChunkZ + reach; chunkZ++) {
                if (!eligible(chunkX, chunkZ, center, playerChunkX, playerChunkZ, rangeSq, simulationDistance)) continue;
                float x0 = px(chunkX * 16, originX, cameraX, scale);
                float x1 = px(chunkX * 16 + 16, originX, cameraX, scale);
                float z0 = px(chunkZ * 16, originY, cameraZ, scale);
                float z1 = px(chunkZ * 16 + 16, originY, cameraZ, scale);
                if (x1 < 0 || x0 > pxWidth || z1 < 0 || z0 > pxHeight) continue;
                batch.rect(x0, z0, x1, z1, FILL);
                // outline only on the border of the eligible area
                if (!eligible(chunkX - 1, chunkZ, center, playerChunkX, playerChunkZ, rangeSq, simulationDistance)) batch.rect(x0, z0, x0 + 1, z1, EDGE);
                if (!eligible(chunkX + 1, chunkZ, center, playerChunkX, playerChunkZ, rangeSq, simulationDistance)) batch.rect(x1 - 1, z0, x1, z1, EDGE);
                if (!eligible(chunkX, chunkZ - 1, center, playerChunkX, playerChunkZ, rangeSq, simulationDistance)) batch.rect(x0, z0, x1, z0 + 1, EDGE);
                if (!eligible(chunkX, chunkZ + 1, center, playerChunkX, playerChunkZ, rangeSq, simulationDistance)) batch.rect(x0, z1 - 1, x1, z1, EDGE);
            }
        }
    }

    static boolean eligible(int chunkX, int chunkZ, MapOverlayRenderer.Center center, int playerChunkX, int playerChunkZ,
                            double rangeSq, int simulationDistance) {
        if (simulationDistance > 0
            && Math.max(Math.abs(chunkX - playerChunkX), Math.abs(chunkZ - playerChunkZ)) > simulationDistance) {
            return false;
        }
        double dx = chunkX * 16 + 8 - center.x();
        double dz = chunkZ * 16 + 8 - center.z();
        return dx * dx + dz * dz < rangeSq;
    }

    private static float px(double block, double origin, double camera, double scale) {
        return (float) Math.round(origin + (block - camera) * scale);
    }
}
