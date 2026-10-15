package spawnradius.client;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fc;

import java.util.Arrays;

/**
 * All of the overlay's coloured shapes for one frame, submitted to the GUI as a single element.
 *
 * Submitting every ring segment as its own {@code fill} made the game's GUI layering put each overlapping piece
 * on a new layer and search all layers for every new piece, which became very slow when zoomed in (thousands of
 * segments). One element = one layer and one draw call, no matter how many quads it holds.
 */
final class RingBatch implements GuiElementRenderState {
    private final Matrix3x2fc pose;
    private final ScreenRectangle bounds;
    private float[] xy = new float[8 * 256];
    private int[] colors = new int[256];
    private int quads;

    /**
     * @param pose   GUI pose at submission time, already scaled so the coordinates passed in are framebuffer pixels
     * @param bounds area of the screen (GUI units) the batch may cover
     */
    RingBatch(Matrix3x2fc pose, ScreenRectangle bounds) {
        this.pose = new Matrix3x2f(pose);
        this.bounds = bounds;
    }

    boolean isEmpty() {
        return quads == 0;
    }

    /** Axis-aligned rectangle, same vertex order as vanilla {@code fill}. */
    void rect(float x0, float y0, float x1, float y1, int color) {
        if (x1 < x0) { float t = x0; x0 = x1; x1 = t; }
        if (y1 < y0) { float t = y0; y0 = y1; y1 = t; }
        add(x0, y0, x0, y1, x1, y1, x1, y0, color);
    }

    /** Any convex quad; the corners are reordered to vanilla's winding if needed. */
    void quad(float ax, float ay, float bx, float by, float cx, float cy, float dx, float dy, int color) {
        // vanilla fill (x0,y0)->(x0,y1)->(x1,y1)->(x1,y0) has a negative shoelace area in screen space
        float area = (ax * by - bx * ay) + (bx * cy - cx * by) + (cx * dy - dx * cy) + (dx * ay - ax * dy);
        if (area > 0) {
            add(ax, ay, dx, dy, cx, cy, bx, by, color);
        } else {
            add(ax, ay, bx, by, cx, cy, dx, dy, color);
        }
    }

    private void add(float ax, float ay, float bx, float by, float cx, float cy, float dx, float dy, int color) {
        if (quads == colors.length) {
            colors = Arrays.copyOf(colors, quads * 2);
            xy = Arrays.copyOf(xy, quads * 16);
        }
        int i = quads * 8;
        xy[i] = ax;
        xy[i + 1] = ay;
        xy[i + 2] = bx;
        xy[i + 3] = by;
        xy[i + 4] = cx;
        xy[i + 5] = cy;
        xy[i + 6] = dx;
        xy[i + 7] = dy;
        colors[quads++] = color;
    }

    @Override
    public void buildVertices(VertexConsumer consumer) {
        for (int q = 0; q < quads; q++) {
            int i = q * 8;
            int color = colors[q];
            consumer.addVertexWith2DPose(pose, xy[i], xy[i + 1]).setColor(color);
            consumer.addVertexWith2DPose(pose, xy[i + 2], xy[i + 3]).setColor(color);
            consumer.addVertexWith2DPose(pose, xy[i + 4], xy[i + 5]).setColor(color);
            consumer.addVertexWith2DPose(pose, xy[i + 6], xy[i + 7]).setColor(color);
        }
    }

    @Override
    public RenderPipeline pipeline() {
        return RenderPipelines.GUI;
    }

    @Override
    public TextureSetup textureSetup() {
        return TextureSetup.noTexture();
    }

    @Override
    public ScreenRectangle scissorArea() {
        return null;
    }

    @Override
    public ScreenRectangle bounds() {
        return bounds;
    }
}
