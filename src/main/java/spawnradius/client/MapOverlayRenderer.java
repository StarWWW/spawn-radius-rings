package spawnradius.client;

import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelData;
import org.joml.Matrix3x2f;
import spawnradius.SpawnRadiusConfig;
import spawnradius.SpawnRadiusMod;
import spawnradius.SpawnRing;
import spawnradius.mixin.GuiGraphicsExtractorAccessor;

import java.util.ArrayList;
import java.util.List;

/**
 * Draws the spawn / despawn rings on top of Xaero's World Map.
 *
 * Coordinates: Xaero's GuiMap converts a framebuffer pixel to a block with
 * {@code block = cameraX + (pixelX - window.getWidth() / 2) / scale} (same for Z/height). We use the inverse of
 * that and work in framebuffer pixels, which keeps the lines crisp at every GUI scale.
 *
 * All coloured shapes (rings, outlines, chunk shading, pin marker) go into one {@link RingBatch}, i.e. one GUI
 * element per frame. Only the few text labels and the info panel use normal GUI calls.
 */
public final class MapOverlayRenderer {
    public static final int WORLD_SPAWN_COLOR = 0xFFFFFFFF;
    private static final int OUTLINE_COLOR = 0xB0000000;
    /** Target length of one ring segment in pixels. */
    private static final double SEGMENT_PX = 3.0;
    private static final int MIN_SEGMENTS = 48;
    private static final int MAX_SEGMENTS = 8192;

    private MapOverlayRenderer() {
    }

    /** Where the rings are centred, already resolved for the dimension shown on the map, or null if nothing to draw. */
    public record Center(double x, double y, double z, boolean pinned) {
    }

    public static Center resolveCenter(SpawnRadiusConfig config, ResourceKey<Level> mapDimension) {
        return resolveCenter(config, mapDimension, 1.0f);
    }

    /** Uses the same interpolated player position Xaero uses for its arrow, so the rings never lag behind it. */
    public static Center resolveCenter(SpawnRadiusConfig config, ResourceKey<Level> mapDimension, float partialTicks) {
        Minecraft mc = Minecraft.getInstance();
        if (config.centerMode == SpawnRadiusConfig.CenterMode.PINNED) {
            if (mapDimension == null || !DimensionUtil.id(mapDimension).equals(config.pinDimension)) return null;
            return new Center(config.pinX, config.pinY, config.pinZ, true);
        }
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) return null;
        // the rings are about the player's real position, so only draw them on the player's own dimension
        if (mapDimension != null && !mapDimension.equals(mc.level.dimension())) return null;
        var pos = player.getPosition(partialTicks);
        return new Center(pos.x, pos.y, pos.z, false);
    }

    /**
     * Horizontal radius of the spawn/despawn sphere at the configured Y level, or -1 when the sphere
     * does not reach that Y level at all. All spawn and despawn checks in the game are 3D (spherical) distances.
     */
    public static double sliceRadius(double radius, double centerY, SpawnRadiusConfig config) {
        return sliceRadius(radius, centerY, config, centerY);
    }

    /**
     * @param viewY the Y level the map is "looking at" when the slice follows the player
     */
    public static double sliceRadius(double radius, double centerY, SpawnRadiusConfig config, double viewY) {
        double sliceY = config.yMode == SpawnRadiusConfig.YMode.PLAYER ? viewY : config.fixedY;
        double dy = sliceY - centerY;
        double sq = radius * radius - dy * dy;
        return sq <= 0 ? -1 : Math.sqrt(sq);
    }

    public static void render(GuiGraphicsExtractor g, int guiWidth, int guiHeight,
                              double cameraX, double cameraZ, double scale,
                              ResourceKey<Level> mapDimension, boolean hiddenUI, float partialTicks) {
        SpawnRadiusConfig config = SpawnRadiusConfig.get();
        if (!config.enabled) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        Font font = mc.font;

        Center center = resolveCenter(config, mapDimension, partialTicks);
        Window window = mc.getWindow();
        double guiScale = window.getGuiScale();
        View view = new View(cameraX, cameraZ, scale, window.getWidth() / 2, window.getHeight() / 2,
            window.getWidth(), window.getHeight(), guiScale, Math.max(1, config.thickness));
        List<LabelRequest> labels = new ArrayList<>();

        try {
            Matrix3x2f pixelPose = new Matrix3x2f(g.pose()).scale((float) (1.0 / guiScale), (float) (1.0 / guiScale));
            RingBatch batch = new GuiRingBatch(pixelPose, new ScreenRectangle(0, 0, guiWidth, guiHeight));
            drawGeometry(batch, mc, config, center, mapDimension, view, labels);
            if (!batch.isEmpty()) {
                ((GuiGraphicsExtractorAccessor) g).spawnradius$guiRenderState().addGuiElement(batch);
            }
        } catch (Throwable t) {
            SpawnRadiusMod.logOnce("rings", "Failed to draw spawn rings", t);
        }

        // the panel first, so the labels can keep clear of it
        List<ScreenRectangle> taken = new ArrayList<>();
        if (config.legend && !hiddenUI) {
            try {
                taken.add(Legend.draw(g, font, config, center, mapDimension));
            } catch (Throwable t) {
                SpawnRadiusMod.logOnce("legend", "Failed to draw the info panel", t);
            }
        }

        try {
            for (LabelRequest label : labels) {
                drawLabel(g, font, label, guiScale, guiWidth, guiHeight, taken);
            }
        } catch (Throwable t) {
            SpawnRadiusMod.logOnce("labels", "Failed to draw ring labels", t);
        }
    }

    /**
     * Map view in framebuffer pixels. Xaero centres the map on the framebuffer:
     * {@code block = cameraX + (pixelX - window.getWidth() / 2) / scale}; this is the inverse.
     */
    private record View(double cameraX, double cameraZ, double scale, double originX, double originY,
                        double width, double height, double guiScale, int thickness) {
        double x(double blockX) {
            return originX + (blockX - cameraX) * scale;
        }

        double y(double blockZ) {
            return originY + (blockZ - cameraZ) * scale;
        }
    }

    private record LabelRequest(String text, double cx, double cy, double rPx, int color) {
    }

    private static void drawGeometry(RingBatch batch, Minecraft mc, SpawnRadiusConfig config, Center center,
                                     ResourceKey<Level> mapDimension, View view, List<LabelRequest> labels) {
        if (center != null) {
            double cx = view.x(center.x());
            double cy = view.y(center.z());

            if (config.spawnChunks) {
                SpawnChunkOverlay.draw(batch, center, view.originX(), view.originY(), view.cameraX(), view.cameraZ(),
                    view.scale(), view.width(), view.height(), mc.level.getServerSimulationDistance());
            }

            for (SpawnRing ring : SpawnRing.all()) {
                if (!config.isRingEnabled(ring)) continue;
                double r = sliceRadius(ring.radius(), center.y(), config);
                if (r <= 0) continue;
                double rPx = r * view.scale();
                int color = config.getRingColor(ring);
                drawRing(batch, cx, cy, rPx, view.thickness(), color, view.width(), view.height(), config.outline);
                if (config.labels) {
                    labels.add(new LabelRequest(SpawnRing.labelText(ring), cx, cy, rPx, color));
                }
            }
            // small cross on a pinned centre so the AFK spot is visible
            if (center.pinned()) {
                float m = (float) Math.max(3, Math.round(view.guiScale() * 2));
                float x = (float) cx;
                float y = (float) cy;
                batch.rect(x - m, y - 1.5f, x + m, y + 1.5f, 0xFFFFFFFF);
                batch.rect(x - 1.5f, y - m, x + 1.5f, y + m, 0xFFFFFFFF);
            }
        }

        // the game also refuses natural spawns within 24 blocks of the world spawn point
        if (config.worldSpawn && mapDimension != null) {
            LevelData.RespawnData respawn = mc.level.getRespawnData();
            if (respawn != null && mapDimension.equals(respawn.dimension())) {
                BlockPos pos = respawn.pos();
                double sy = pos.getY() + 0.5;
                double viewY = center != null ? center.y() : sy;
                double r = sliceRadius(SpawnRing.NO_SPAWN_RADIUS, sy, config, viewY);
                if (r > 0) {
                    double cx = view.x(pos.getX() + 0.5);
                    double cy = view.y(pos.getZ() + 0.5);
                    double rPx = r * view.scale();
                    drawRing(batch, cx, cy, rPx, view.thickness(), WORLD_SPAWN_COLOR, view.width(), view.height(),
                        config.outline);
                    if (config.labels) {
                        labels.add(new LabelRequest(Component.translatable("spawnradius.label.world_spawn",
                            SpawnRing.NO_SPAWN_RADIUS).getString(), cx, cy, rPx, WORLD_SPAWN_COLOR));
                    }
                }
            }
        }
    }

    /** Ring with an optional dark halo underneath so it stays readable on any terrain colour. */
    static void drawRing(RingBatch batch, double cx, double cy, double r, int thickness, int color,
                         double width, double height, boolean outline) {
        if (outline) {
            drawBand(batch, cx, cy, r, thickness + 2, OUTLINE_COLOR, width, height);
        }
        drawBand(batch, cx, cy, r, thickness, color, width, height);
    }

    /**
     * Circle outline as an annulus of {@code bandWidth} pixels: quads between consecutive angles, with corners
     * on the inner and outer radius. Neighbouring quads share their edges exactly, so there are no gaps and
     * no overlaps (semi-transparent colours stay even). Only on-screen quads are emitted.
     */
    static void drawBand(RingBatch batch, double cx, double cy, double r, double bandWidth, int color,
                         double width, double height) {
        if (r < 1.0) return;
        double half = bandWidth / 2.0;
        double inner = Math.max(0, r - half);
        double outer = r + half;
        // whole circle off screen
        if (cx + outer < 0 || cx - outer > width || cy + outer < 0 || cy - outer > height) return;
        // screen entirely inside the hole of the ring: nothing visible
        double farX = Math.max(Math.abs(cx), Math.abs(width - cx));
        double farY = Math.max(Math.abs(cy), Math.abs(height - cy));
        if (farX * farX + farY * farY < inner * inner) return;

        int segments = (int) Math.ceil(2 * Math.PI * r / SEGMENT_PX);
        segments = Math.max(MIN_SEGMENTS, Math.min(segments, MAX_SEGMENTS));
        double step = 2 * Math.PI / segments;
        double cosStep = Math.cos(step);
        double sinStep = Math.sin(step);

        double cos0 = 1.0;
        double sin0 = 0.0;
        for (int i = 0; i < segments; i++) {
            // rotate the unit vector by one step; exact values at multiples of 64 steps stop drift
            double cos1;
            double sin1;
            if ((i + 1) % 64 == 0 || i + 1 == segments) {
                double a = (i + 1) * step;
                cos1 = Math.cos(a);
                sin1 = Math.sin(a);
            } else {
                cos1 = cos0 * cosStep - sin0 * sinStep;
                sin1 = sin0 * cosStep + cos0 * sinStep;
            }
            double ix0 = cx + inner * cos0, iy0 = cy + inner * sin0;
            double ox0 = cx + outer * cos0, oy0 = cy + outer * sin0;
            double ix1 = cx + inner * cos1, iy1 = cy + inner * sin1;
            double ox1 = cx + outer * cos1, oy1 = cy + outer * sin1;
            double minX = Math.min(Math.min(ix0, ox0), Math.min(ix1, ox1));
            double maxX = Math.max(Math.max(ix0, ox0), Math.max(ix1, ox1));
            double minY = Math.min(Math.min(iy0, oy0), Math.min(iy1, oy1));
            double maxY = Math.max(Math.max(iy0, oy0), Math.max(iy1, oy1));
            if (maxX >= 0 && minX <= width && maxY >= 0 && minY <= height) {
                batch.quad((float) ix0, (float) iy0, (float) ox0, (float) oy0,
                    (float) ox1, (float) oy1, (float) ix1, (float) iy1, color);
            }
            cos0 = cos1;
            sin0 = sin1;
        }
    }

    /**
     * Puts the label on the first of the ring's north, south, east and west points that is on screen and does not
     * cover the info panel or another label; when the rings are close together (zoomed out) some labels are left out
     * rather than drawn on top of each other.
     */
    private static void drawLabel(GuiGraphicsExtractor g, Font font, LabelRequest label, double guiScale,
                                  int guiWidth, int guiHeight, List<ScreenRectangle> taken) {
        String text = label.text();
        int textWidth = font.width(text);
        int textHeight = font.lineHeight;
        // try north, south, east, west points of the ring, take the first one that is on screen
        double[][] candidates = {
            {label.cx(), label.cy() - label.rPx()},
            {label.cx(), label.cy() + label.rPx()},
            {label.cx() + label.rPx(), label.cy()},
            {label.cx() - label.rPx(), label.cy()},
        };
        for (double[] c : candidates) {
            int x = (int) Math.round(c[0] / guiScale) - textWidth / 2;
            int y = (int) Math.round(c[1] / guiScale) - textHeight / 2;
            if (x < 2 || y < 2 || x + textWidth > guiWidth - 2 || y + textHeight > guiHeight - 2) continue;
            // the dark box behind the text, plus a pixel of space around it
            ScreenRectangle box = new ScreenRectangle(x - 3, y - 2, textWidth + 5, textHeight + 3);
            if (taken.stream().anyMatch(box::intersects)) continue;
            taken.add(box);
            g.fill(x - 2, y - 1, x + textWidth + 1, y + textHeight, 0xA0000000);
            g.text(font, text, x, y, label.color());
            return;
        }
    }
}
