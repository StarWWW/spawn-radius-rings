package spawnradius.client;

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;
import spawnradius.SpawnRadiusConfig;
import spawnradius.SpawnRadiusMod;
import spawnradius.compat.Compat;
import xaero.map.MapProcessor;
import xaero.map.gui.GuiMap;

import java.lang.reflect.Field;
import java.util.List;

/**
 * Puts the rings, two buttons and the mod's keys on Xaero's world map, without a mixin into Xaero's GuiMap.
 * <p>
 * GuiMap's render method is huge: rewriting the class, which any mixin into it makes Mixin do, needs almost all the
 * work ASM 9.11 (Fabric Loader 0.20+) allows for one method, and with injected code it goes over that limit and the
 * game crashes while starting. So the screen is reached through Fabric's screen events and its fields are read by
 * reflection. Drawing still happens at the same point as before (after the map image, before Xaero's text), through
 * a mixin into the small ImprovedFramebuffer class: GuiMap calls its bindDefaultFramebuffer once per frame, right
 * there.
 */
public final class WorldMapHook {
    private static @Nullable GuiMap drawing;
    private static @Nullable GuiGraphicsExtractor graphics;
    private static float partialTicks;

    private static final Field CAMERA_X = field("cameraX");
    private static final Field CAMERA_Z = field("cameraZ");
    private static final Field SCALE = field("scale");
    private static final Field MAP_PROCESSOR = field("mapProcessor");
    private static final Field ATTACHED_CAMERA_BUTTON = field("attachedCameraButton");

    private WorldMapHook() {
    }

    public static void register() {
        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            if (!(screen instanceof GuiMap map)) return;
            // the events of a screen are new after every init, so these are added once per init
            addButtons(map);
            ScreenKeyboardEvents.allowKeyPress(map).register((s, event) -> !keyPressed(map, event));
            ScreenEvents.beforeExtract(map).register((s, g, mouseX, mouseY, delta) -> {
                drawing = map;
                graphics = g;
                partialTicks = delta;
            });
            ScreenEvents.afterExtract(map).register((s, g, mouseX, mouseY, delta) -> {
                drawing = null;
                graphics = null;
            });
        });
    }

    /** Called right after GuiMap has drawn the map image, before its own text overlays (cursor coordinates etc.). */
    public static void afterMapDrawn() {
        GuiMap map = drawing;
        GuiGraphicsExtractor g = graphics;
        // the rings hide with the rest of Xaero's interface (F1)
        if (map == null || g == null || GuiMap.hiddenUI) return;
        if (CAMERA_X == null || CAMERA_Z == null || SCALE == null) return;
        try {
            MapOverlayRenderer.render(g, map.width, map.height, CAMERA_X.getDouble(map), CAMERA_Z.getDouble(map),
                SCALE.getDouble(map), mapDimension(map), false, partialTicks);
        } catch (ReflectiveOperationException e) {
            SpawnRadiusMod.logOnce("map-camera", "Could not read the position of Xaero's world map", e);
        }
    }

    private static @Nullable ResourceKey<Level> mapDimension(GuiMap map) {
        try {
            MapProcessor processor = MAP_PROCESSOR == null ? null : (MapProcessor) MAP_PROCESSOR.get(map);
            if (processor != null && processor.getMapWorld() != null) {
                return processor.getMapWorld().getCurrentDimensionId();
            }
        } catch (Throwable t) {
            SpawnRadiusMod.logOnce("map-dimension", "Could not read the dimension shown on the map", t);
        }
        return null;
    }

    /** Two buttons in Xaero's left button column, right above the "attach camera" button. */
    private static void addButtons(GuiMap map) {
        Button attachedCamera;
        try {
            attachedCamera = ATTACHED_CAMERA_BUTTON == null ? null : (Button) ATTACHED_CAMERA_BUTTON.get(map);
        } catch (ReflectiveOperationException e) {
            SpawnRadiusMod.logOnce("map-buttons", "Could not find Xaero's camera button", e);
            return;
        }
        if (attachedCamera == null) return;
        int x = attachedCamera.getX();
        int y = attachedCamera.getY() - 20 * buttonSlotsAbove();
        List<AbstractWidget> widgets = Screens.getWidgets(map);
        widgets.add(new XaeroIconButton(x, y, XaeroIconButton.RINGS_ON, XaeroIconButton.RINGS_OFF,
            () -> SpawnRadiusConfig.get().enabled,
            b -> SpawnRadiusMod.toggle(),
            () -> XaeroIconButton.tooltip(Component.translatable("spawnradius.button.toggle.tooltip",
                SpawnRadiusConfig.get().enabled ? CommonComponents.OPTION_ON : CommonComponents.OPTION_OFF),
                SpawnRadiusMod.TOGGLE_KEY)));
        widgets.add(new XaeroIconButton(x, y - 20, XaeroIconButton.SETTINGS, XaeroIconButton.SETTINGS,
            () -> true,
            b -> Compat.setScreen(Screens.getMinecraft(map), new SpawnRadiusScreen(map)),
            () -> XaeroIconButton.tooltip(Component.translatable("spawnradius.button.settings.tooltip"),
                SpawnRadiusMod.SETTINGS_KEY)));
    }

    /** The mod's keys also work while the map is open. Returns true when the key was used. */
    private static boolean keyPressed(Screen map, KeyEvent event) {
        if (SpawnRadiusMod.TOGGLE_KEY.matches(event)) {
            SpawnRadiusMod.toggle();
            return true;
        }
        if (SpawnRadiusMod.SETTINGS_KEY.matches(event)) {
            Compat.setScreen(Screens.getMinecraft(map), new SpawnRadiusScreen(map));
            return true;
        }
        return false;
    }

    /** XaeroPlus puts its drawing button right above the camera button; stay clear of it. */
    private static int buttonSlotsAbove() {
        return net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("xaeroplus") ? 2 : 1;
    }

    /** A private field of GuiMap, or null (logged) if this World Map version does not have it. */
    private static @Nullable Field field(String name) {
        try {
            Field f = GuiMap.class.getDeclaredField(name);
            f.setAccessible(true);
            return f;
        } catch (ReflectiveOperationException | RuntimeException e) {
            SpawnRadiusMod.logOnce("map-field-" + name, "Xaero's world map has no field \"" + name + "\"; "
                + "this World Map version is not supported", e);
            return null;
        }
    }
}
