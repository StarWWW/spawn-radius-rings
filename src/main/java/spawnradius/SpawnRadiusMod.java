package spawnradius;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spawnradius.client.SpawnRadiusScreen;
import spawnradius.compat.Compat;

public class SpawnRadiusMod implements ClientModInitializer {
    public static final String MOD_ID = "spawnradius";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static final KeyMapping.Category KEY_CATEGORY =
        KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "keybindings"));
    /** Unbound by default (-1) so it never clashes with another mod; set it in Controls. */
    public static final KeyMapping TOGGLE_KEY = new KeyMapping("key.spawnradius.toggle", -1, KEY_CATEGORY);
    public static final KeyMapping SETTINGS_KEY = new KeyMapping("key.spawnradius.settings", -1, KEY_CATEGORY);

    @Override
    public void onInitializeClient() {
        SpawnRadiusConfig.get();
        KeyMappingHelper.registerKeyMapping(TOGGLE_KEY);
        KeyMappingHelper.registerKeyMapping(SETTINGS_KEY);
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (TOGGLE_KEY.consumeClick()) {
                toggle();
            }
            while (SETTINGS_KEY.consumeClick()) {
                Compat.setScreen(client, new SpawnRadiusScreen(Compat.screen(client)));
            }
        });
    }

    private static final java.util.Set<String> LOGGED = java.util.concurrent.ConcurrentHashMap.newKeySet();

    /** Logs a rendering problem once instead of every frame. */
    public static void logOnce(String key, String message, Throwable t) {
        if (LOGGED.add(key)) {
            LOGGER.error(message + " (further errors of this kind are not logged)", t);
        }
    }

    public static void toggle() {
        SpawnRadiusConfig config = SpawnRadiusConfig.get();
        config.enabled = !config.enabled;
        config.save();
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.sendOverlayMessage(Component.translatable(config.enabled ? "spawnradius.toggled.on" : "spawnradius.toggled.off"));
        }
    }
}
