package spawnradius.client;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.RenderPipelines;
import org.joml.Matrix3x2fc;

/** {@link RingBatch} drawn with the GUI pipeline. This copy is for 26.2; the other versions have their own. */
final class GuiRingBatch extends RingBatch {
    GuiRingBatch(Matrix3x2fc pose, ScreenRectangle bounds) {
        super(pose, bounds);
    }

    @Override
    public RenderPipeline pipeline() {
        return RenderPipelines.GUI;
    }
}
