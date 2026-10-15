package spawnradius.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.Level;
import spawnradius.SpawnRadiusConfig;
import spawnradius.SpawnRing;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/** Small info panel in the top-left corner of the world map. */
final class Legend {
    /** Right of Xaero's 20 px wide button column on the left edge, so no map button is covered. */
    private static final int X = 24;
    private static final int Y = 34; // below Xaero's 30x30 settings button in the top-left corner
    private static final int MAX_WIDTH = 260;

    private Legend() {
    }

    /** @param swatch whether a colour square is drawn in front; the text is then white */
    private record Line(String text, int color, boolean swatch) {
    }

    /** One wrapped line ready to draw: an optional colour square, indented text. */
    private record Row(String text, int textColor, int swatchColor, int indent) {
    }

    /** Draws the panel and returns the area it covers. */
    static ScreenRectangle draw(GuiGraphicsExtractor g, Font font, SpawnRadiusConfig config,
                                MapOverlayRenderer.Center center, ResourceKey<Level> mapDimension) {
        List<Line> lines = new ArrayList<>();
        lines.add(new Line(Component.translatable("spawnradius.legend.title").getString(), 0xFFE8C66A, false));

        if (center == null) {
            String key = config.centerMode == SpawnRadiusConfig.CenterMode.PINNED
                ? "spawnradius.legend.pin_other_dimension"
                : "spawnradius.legend.other_dimension";
            lines.add(new Line(Component.translatable(key).getString(), 0xFFAAAAAA, false));
        } else {
            for (SpawnRing ring : SpawnRing.all()) {
                if (!config.isRingEnabled(ring)) continue;
                double r = MapOverlayRenderer.sliceRadius(ring.radius(), center.y(), config);
                String text = describe(ring);
                if (r <= 0) {
                    text += " " + Component.translatable("spawnradius.legend.out_of_reach").getString();
                } else if (config.yMode == SpawnRadiusConfig.YMode.FIXED && Math.abs(r - ring.radius()) > 0.05) {
                    text += " " + Component.translatable("spawnradius.legend.slice", String.format("%.1f", r)).getString();
                }
                lines.add(new Line(text, config.getRingColor(ring), true));
            }
            if (config.spawnChunks) {
                int sim = net.minecraft.client.Minecraft.getInstance().level.getServerSimulationDistance();
                String text = sim > 0
                    ? Component.translatable("spawnradius.legend.spawn_chunks_sim", sim).getString()
                    : Component.translatable("spawnradius.legend.spawn_chunks").getString();
                lines.add(new Line(text, 0xFF33FF66, true));
            }
            if (config.worldSpawn) {
                lines.add(new Line(Component.translatable("spawnradius.legend.world_spawn", SpawnRing.NO_SPAWN_RADIUS).getString(),
                    MapOverlayRenderer.WORLD_SPAWN_COLOR, true));
            }
            String yText = config.yMode == SpawnRadiusConfig.YMode.PLAYER
                ? Component.translatable("spawnradius.legend.y_player", (int) Math.floor(center.y())).getString()
                : Component.translatable("spawnradius.legend.y_fixed", config.fixedY, (int) Math.floor(center.y())).getString();
            lines.add(new Line(yText, 0xFFAAAAAA, false));
            if (center.pinned()) {
                lines.add(new Line(Component.translatable("spawnradius.legend.pinned",
                    (int) Math.floor(center.x()), (int) Math.floor(center.y()), (int) Math.floor(center.z())).getString(),
                    0xFFAAAAAA, false));
            }
        }

        // wrap long lines; continuation lines keep the text colour and indent of their first line
        List<Row> rows = new ArrayList<>();
        for (Line line : lines) {
            int indent = line.swatch() ? 10 : 0;
            int textColor = line.swatch() ? 0xFFFFFFFF : line.color();
            List<String> parts = wrap(font, line.text(), MAX_WIDTH - indent);
            for (int i = 0; i < parts.size(); i++) {
                rows.add(new Row(parts.get(i), textColor, line.swatch() && i == 0 ? line.color() : 0, indent));
            }
        }

        int lineHeight = font.lineHeight + 2;
        int width = 0;
        for (Row row : rows) {
            width = Math.max(width, row.indent() + font.width(row.text()));
        }
        int height = rows.size() * lineHeight + 4;
        g.fill(X, Y, X + width + 6, Y + height, 0x90000000);
        int y = Y + 3;
        for (Row row : rows) {
            int textX = X + 3;
            if (row.swatchColor() != 0) {
                g.fill(textX, y + 1, textX + 7, y + 8, row.swatchColor());
            }
            g.text(font, row.text(), textX + row.indent(), y, row.textColor());
            y += lineHeight;
        }
        return new ScreenRectangle(X, Y, width + 6, height);
    }

    /** e.g. "128 – Anında despawn: Canavarlar, Yarasalar…" */
    static String describe(SpawnRing ring) {
        String kind = Component.translatable("spawnradius.kind." + ring.kind().key, ring.radius()).getString();
        if (ring.kind() == SpawnRing.Kind.INSTANT_DESPAWN) {
            return kind + ": " + categories(ring.categories());
        }
        return kind;
    }

    static String categories(List<MobCategory> categories) {
        return categories.stream()
            .map(c -> Component.translatable(SpawnRing.categoryKey(c)).getString())
            .collect(Collectors.joining(", "));
    }

    private static List<String> wrap(Font font, String text, int maxWidth) {
        List<String> out = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String word : text.split(" ")) {
            String candidate = current.isEmpty() ? word : current + " " + word;
            if (font.width(candidate) > maxWidth && !current.isEmpty()) {
                out.add(current.toString());
                current = new StringBuilder(word);
            } else {
                current = new StringBuilder(candidate);
            }
        }
        if (!current.isEmpty()) out.add(current.toString());
        return out;
    }
}
