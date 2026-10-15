package spawnradius.mixin;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import spawnradius.SpawnRadiusConfig;
import spawnradius.SpawnRadiusMod;
import spawnradius.client.MapOverlayRenderer;
import spawnradius.client.SpawnRadiusScreen;
import spawnradius.client.XaeroIconButton;
import xaero.lib.client.gui.ScreenBase;
import xaero.map.MapProcessor;
import xaero.map.gui.GuiMap;

@Mixin(value = GuiMap.class, remap = false)
public abstract class MixinGuiMap extends ScreenBase {
    @Shadow private double cameraX;
    @Shadow private double cameraZ;
    @Shadow private double scale;
    @Shadow public static boolean hiddenUI;
    @Shadow private MapProcessor mapProcessor;
    @Shadow private Button attachedCameraButton;

    protected MixinGuiMap(Screen parent, Screen escape, Component title) {
        super(parent, escape, title);
    }

    /**
     * Drawn right after the map image and before Xaero's own text overlays (cursor coordinates etc.).
     * In 1.47.0 this point sits inside Xaero's "if (!hiddenUI)" block, so the rings hide with the rest of the UI (F1).
     */
    @Inject(method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V", at = @At(
        value = "FIELD",
        target = "Lxaero/map/common/config/option/WorldMapProfiledConfigOptions;COORDINATES:Lxaero/lib/common/config/option/BooleanConfigOption;",
        opcode = 178, // Opcodes.GETSTATIC
        ordinal = 0
    ))
    private void spawnradius$renderRings(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTicks,
                                        CallbackInfo ci) {
        ResourceKey<Level> mapDimension = null;
        try {
            if (mapProcessor != null && mapProcessor.getMapWorld() != null) {
                mapDimension = mapProcessor.getMapWorld().getCurrentDimensionId();
            }
        } catch (Throwable t) {
            SpawnRadiusMod.logOnce("map-dimension", "Could not read the dimension shown on the map", t);
        }
        MapOverlayRenderer.render(guiGraphics, this.width, this.height, cameraX, cameraZ, scale, mapDimension,
            hiddenUI, partialTicks);
    }

    /** Two buttons in Xaero's left button column, right above the "attach camera" button. */
    @Inject(method = "init()V", at = @At("RETURN"))
    private void spawnradius$addButtons(CallbackInfo ci) {
        if (attachedCameraButton == null) return;
        int x = attachedCameraButton.getX();
        int y = attachedCameraButton.getY() - 20 * spawnradius$buttonSlotsAbove();
        addRenderableWidget(new XaeroIconButton(x, y, XaeroIconButton.RINGS_ON, XaeroIconButton.RINGS_OFF,
            () -> SpawnRadiusConfig.get().enabled,
            b -> SpawnRadiusMod.toggle(),
            () -> XaeroIconButton.tooltip(Component.translatable("spawnradius.button.toggle.tooltip",
                SpawnRadiusConfig.get().enabled ? CommonComponents.OPTION_ON : CommonComponents.OPTION_OFF),
                SpawnRadiusMod.TOGGLE_KEY)));
        addRenderableWidget(new XaeroIconButton(x, y - 20, XaeroIconButton.SETTINGS, XaeroIconButton.SETTINGS,
            () -> true,
            b -> this.minecraft.setScreen(new SpawnRadiusScreen((Screen) (Object) this)),
            () -> XaeroIconButton.tooltip(Component.translatable("spawnradius.button.settings.tooltip"),
                SpawnRadiusMod.SETTINGS_KEY)));
    }

    @Inject(method = "keyPressed(Lnet/minecraft/client/input/KeyEvent;)Z", at = @At("HEAD"), cancellable = true)
    private void spawnradius$keyPressed(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (SpawnRadiusMod.TOGGLE_KEY.matches(event)) {
            SpawnRadiusMod.toggle();
            cir.setReturnValue(true);
        } else if (SpawnRadiusMod.SETTINGS_KEY.matches(event)) {
            this.minecraft.setScreen(new SpawnRadiusScreen((Screen) (Object) this));
            cir.setReturnValue(true);
        }
    }

    /** XaeroPlus puts its drawing button right above the camera button; stay clear of it. */
    @Unique
    private static int spawnradius$buttonSlotsAbove() {
        return net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("xaeroplus") ? 2 : 1;
    }
}
