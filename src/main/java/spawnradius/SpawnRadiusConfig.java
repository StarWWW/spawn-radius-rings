package spawnradius;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Simple properties-backed settings. Stored at config/spawnradius.properties.
 */
public final class SpawnRadiusConfig {
    public enum YMode { PLAYER, FIXED }
    public enum CenterMode { PLAYER, PINNED }

    /** Colour palette the per-ring colour button cycles through (ARGB, fully opaque). */
    public static final int[] PALETTE = {
        0xFFFF5050, // red
        0xFFFF9A3C, // orange
        0xFFFFD23F, // yellow
        0xFF5BE06A, // green
        0xFF4FC3F7, // cyan
        0xFF4F7CFF, // blue
        0xFFB066FF, // purple
        0xFFFF6FC8, // pink
        0xFFFFFFFF, // white
    };

    /** Range accepted for the fixed Y level (covers every possible world height). */
    public static final int MIN_Y = -2048;
    public static final int MAX_Y = 2048;

    public boolean enabled = true;
    public boolean labels = true;
    public boolean legend = true;
    public boolean spawnChunks = false;
    public boolean worldSpawn = false;
    public boolean outline = true;
    public int thickness = 2;
    public YMode yMode = YMode.PLAYER;
    public int fixedY = 64;
    public CenterMode centerMode = CenterMode.PLAYER;
    public double pinX, pinY, pinZ;
    public String pinDimension = "";

    private final Map<String, Boolean> ringEnabled = new HashMap<>();
    private final Map<String, Integer> ringColor = new HashMap<>();

    private static SpawnRadiusConfig instance;

    public static SpawnRadiusConfig get() {
        if (instance == null) {
            instance = new SpawnRadiusConfig();
            instance.load();
        }
        return instance;
    }

    /** Only for tests: config folder to use instead of the game's. */
    static Path testConfigDir;

    private static Path file() {
        Path dir = testConfigDir != null ? testConfigDir : FabricLoader.getInstance().getConfigDir();
        return dir.resolve("spawnradius.properties");
    }

    public boolean isRingEnabled(SpawnRing ring) {
        return ringEnabled.getOrDefault(ring.id(), true);
    }

    public void setRingEnabled(SpawnRing ring, boolean value) {
        ringEnabled.put(ring.id(), value);
    }

    public int getRingColor(SpawnRing ring) {
        return ringColor.getOrDefault(ring.id(), ring.defaultColor());
    }

    public void cycleRingColor(SpawnRing ring) {
        int current = getRingColor(ring);
        int index = -1;
        for (int i = 0; i < PALETTE.length; i++) {
            if (PALETTE[i] == current) {
                index = i;
                break;
            }
        }
        ringColor.put(ring.id(), PALETTE[(index + 1) % PALETTE.length]);
    }

    /** Back to the defaults; the pinned position is kept so it is not lost by accident. */
    public void resetToDefaults() {
        SpawnRadiusConfig d = new SpawnRadiusConfig();
        enabled = d.enabled;
        labels = d.labels;
        legend = d.legend;
        spawnChunks = d.spawnChunks;
        worldSpawn = d.worldSpawn;
        outline = d.outline;
        thickness = d.thickness;
        yMode = d.yMode;
        fixedY = d.fixedY;
        centerMode = d.centerMode;
        ringEnabled.clear();
        ringColor.clear();
    }

    public void load() {
        Path path = file();
        if (!Files.exists(path)) return;
        Properties p = new Properties();
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            p.load(reader);
        } catch (IOException e) {
            SpawnRadiusMod.LOGGER.warn("Could not read {}", path, e);
            return;
        }
        enabled = bool(p, "enabled", enabled);
        labels = bool(p, "labels", labels);
        legend = bool(p, "legend", legend);
        spawnChunks = bool(p, "spawnChunks", spawnChunks);
        worldSpawn = bool(p, "worldSpawn", worldSpawn);
        outline = bool(p, "outline", outline);
        thickness = clamp(integer(p, "thickness", thickness), 1, 4);
        yMode = enumValue(p, "yMode", YMode.class, yMode);
        fixedY = clamp(integer(p, "fixedY", fixedY), MIN_Y, MAX_Y);
        centerMode = enumValue(p, "centerMode", CenterMode.class, centerMode);
        pinX = dbl(p, "pinX", pinX);
        pinY = dbl(p, "pinY", pinY);
        pinZ = dbl(p, "pinZ", pinZ);
        pinDimension = p.getProperty("pinDimension", pinDimension);
        for (String key : p.stringPropertyNames()) {
            if (key.startsWith("ring.") && key.endsWith(".enabled")) {
                String id = key.substring(5, key.length() - 8);
                ringEnabled.put(id, Boolean.parseBoolean(p.getProperty(key)));
            } else if (key.startsWith("ring.") && key.endsWith(".color")) {
                String id = key.substring(5, key.length() - 6);
                try {
                    ringColor.put(id, (int) Long.parseLong(p.getProperty(key), 16));
                } catch (NumberFormatException ignored) {
                }
            }
        }
    }

    public void save() {
        Properties p = new Properties();
        p.setProperty("enabled", Boolean.toString(enabled));
        p.setProperty("labels", Boolean.toString(labels));
        p.setProperty("legend", Boolean.toString(legend));
        p.setProperty("spawnChunks", Boolean.toString(spawnChunks));
        p.setProperty("worldSpawn", Boolean.toString(worldSpawn));
        p.setProperty("outline", Boolean.toString(outline));
        p.setProperty("thickness", Integer.toString(thickness));
        p.setProperty("yMode", yMode.name());
        p.setProperty("fixedY", Integer.toString(fixedY));
        p.setProperty("centerMode", centerMode.name());
        p.setProperty("pinX", Double.toString(pinX));
        p.setProperty("pinY", Double.toString(pinY));
        p.setProperty("pinZ", Double.toString(pinZ));
        p.setProperty("pinDimension", pinDimension);
        ringEnabled.forEach((id, v) -> p.setProperty("ring." + id + ".enabled", Boolean.toString(v)));
        ringColor.forEach((id, v) -> p.setProperty("ring." + id + ".color", String.format("%08X", v)));
        Path path = file();
        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                p.store(writer, "Spawn Radius Rings for Xaero's World Map");
            }
        } catch (IOException e) {
            SpawnRadiusMod.LOGGER.warn("Could not write {}", path, e);
        }
    }

    private static boolean bool(Properties p, String key, boolean def) {
        String v = p.getProperty(key);
        return v == null ? def : Boolean.parseBoolean(v.trim());
    }

    private static int integer(Properties p, String key, int def) {
        try {
            String v = p.getProperty(key);
            return v == null ? def : Integer.parseInt(v.trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private static double dbl(Properties p, String key, double def) {
        try {
            String v = p.getProperty(key);
            return v == null ? def : Double.parseDouble(v.trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private static <E extends Enum<E>> E enumValue(Properties p, String key, Class<E> type, E def) {
        try {
            String v = p.getProperty(key);
            return v == null ? def : Enum.valueOf(type, v.trim());
        } catch (IllegalArgumentException e) {
            return def;
        }
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }
}
