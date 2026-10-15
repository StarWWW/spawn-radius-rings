package spawnradius.compat;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import spawnradius.client.SpawnRadiusScreen;

/** Opens the settings from Mod Menu's "Configure" button. Only loaded when Mod Menu is installed. */
public class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return SpawnRadiusScreen::new;
    }
}
