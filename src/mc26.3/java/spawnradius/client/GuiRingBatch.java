package spawnradius.client;

import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.RenderPipelines;
import org.joml.Matrix3x2fc;

/** {@link RingBatch} drawn with the GUI pipeline. This copy is for 26.3, where RenderPipeline moved to com.mojang.renderpearl. */
final class GuiRingBatch extends RingBatch {
    GuiRingBatch(Matrix3x2fc pose, ScreenRectangle bounds) {
        super(pose, bounds);
    }

    @Override
    public RenderPipeline pipeline() {
        return RenderPipelines.GUI;
    }
}
