package spawnradius.mixin;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import spawnradius.client.WorldMapHook;
import xaero.map.graphics.ImprovedFramebuffer;

/**
 * Xaero's GuiMap calls bindDefaultFramebuffer exactly once per frame: right after it has drawn the map image and before
 * its text overlays, which is where the rings belong. See {@link WorldMapHook} for why GuiMap itself is not mixed into.
 */
@Mixin(value = ImprovedFramebuffer.class, remap = false)
public abstract class MixinImprovedFramebuffer {
    @Inject(method = "bindDefaultFramebuffer(Lnet/minecraft/client/Minecraft;)V", at = @At("RETURN"))
    private void spawnradius$afterMapDrawn(Minecraft minecraft, CallbackInfo ci) {
        WorldMapHook.afterMapDrawn();
    }
}
