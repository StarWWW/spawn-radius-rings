package spawnradius.client;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;
import spawnradius.SpawnRadiusConfig;
import spawnradius.SpawnRing;
import spawnradius.client.ui.SettingsList;
import spawnradius.client.ui.SettingsScreen;

/** Settings: the rings and their colours, extras, appearance, and where the rings are centred. */
public class SpawnRadiusScreen extends SettingsScreen {
    private final SpawnRadiusConfig config = SpawnRadiusConfig.get();

    public SpawnRadiusScreen(@Nullable Screen parent) {
        super(Component.translatable("spawnradius.screen.title"), parent);
    }

    @Override
    protected void buildList(SettingsList list) {
        SpawnRadiusConfig c = config;

        list.header(Component.translatable("spawnradius.section.rings"));
        list.row(list.checkbox(Component.translatable("spawnradius.screen.enabled"), c.enabled, v -> c.enabled = v, null));
        for (SpawnRing ring : SpawnRing.all()) {
            ColorSwatchButton swatch = new ColorSwatchButton(0, 0, 20, 20,
                Component.translatable("spawnradius.screen.color.tooltip"), () -> c.getRingColor(ring),
                () -> c.cycleRingColor(ring));
            swatch.setTooltip(Tooltip.create(Component.translatable("spawnradius.screen.color.tooltip")));
            list.rowWithEnd(list.checkbox(ringLabel(ring), c.isRingEnabled(ring), v -> c.setRingEnabled(ring, v),
                ringTooltip(ring)), swatch);
        }

        list.header(Component.translatable("spawnradius.section.extra"));
        list.row(list.checkbox(Component.translatable("spawnradius.screen.spawn_chunks"), c.spawnChunks,
            v -> c.spawnChunks = v, Component.translatable("spawnradius.screen.spawn_chunks.tooltip")));
        list.row(list.checkbox(Component.translatable("spawnradius.screen.world_spawn"), c.worldSpawn,
            v -> c.worldSpawn = v,
            Component.translatable("spawnradius.screen.world_spawn.tooltip", SpawnRing.NO_SPAWN_RADIUS)));

        list.header(Component.translatable("spawnradius.section.look"));
        list.row(
            list.checkbox(Component.translatable("spawnradius.screen.labels"), c.labels, v -> c.labels = v, null),
            list.checkbox(Component.translatable("spawnradius.screen.legend"), c.legend, v -> c.legend = v,
                Component.translatable("spawnradius.screen.legend.tooltip")));
        list.row(
            list.checkbox(Component.translatable("spawnradius.screen.outline"), c.outline, v -> c.outline = v,
                Component.translatable("spawnradius.screen.outline.tooltip")),
            Button.builder(thicknessLabel(), b -> {
                c.thickness = c.thickness % 4 + 1;
                b.setMessage(thicknessLabel());
            }).build());

        list.header(Component.translatable("spawnradius.section.position"));
        EditBox fixedY = new EditBox(this.font, 0, 0, 54, 20, Component.translatable("spawnradius.screen.fixed_y"));
        fixedY.setMaxLength(5);
        fixedY.setValue(Integer.toString(c.fixedY));
        fixedY.setResponder(s -> {
            try {
                int value = Integer.parseInt(s.trim());
                if (value >= SpawnRadiusConfig.MIN_Y && value <= SpawnRadiusConfig.MAX_Y) {
                    c.fixedY = value;
                    fixedY.setTextColor(0xFFE0E0E0);
                    return;
                }
            } catch (NumberFormatException ignored) {
            }
            fixedY.setTextColor(0xFFFF5555); // invalid input: the last valid value is kept
        });
        fixedY.setTooltip(Tooltip.create(Component.translatable("spawnradius.screen.fixed_y.tooltip")));
        Button yMode = Button.builder(yModeLabel(), b -> {
            c.yMode = c.yMode == SpawnRadiusConfig.YMode.PLAYER ? SpawnRadiusConfig.YMode.FIXED : SpawnRadiusConfig.YMode.PLAYER;
            b.setMessage(yModeLabel());
            updateFixedY(fixedY);
        }).tooltip(Tooltip.create(Component.translatable("spawnradius.screen.y_mode.tooltip"))).build();
        updateFixedY(fixedY);
        list.rowWithEnd(yMode, fixedY);

        Button centerMode = Button.builder(centerLabel(), b -> {
            if (c.centerMode == SpawnRadiusConfig.CenterMode.PINNED) {
                c.centerMode = SpawnRadiusConfig.CenterMode.PLAYER;
            } else if (!c.pinDimension.isEmpty()) {
                c.centerMode = SpawnRadiusConfig.CenterMode.PINNED;
            }
            b.setMessage(centerLabel());
            b.setTooltip(Tooltip.create(centerTooltip()));
        }).build();
        centerMode.setTooltip(Tooltip.create(centerTooltip()));
        Button pinHere = Button.builder(Component.translatable("spawnradius.screen.pin_here"), b -> {
            LocalPlayer player = this.minecraft.player;
            if (player == null || this.minecraft.level == null) return;
            c.pinX = player.getX();
            c.pinY = player.getY();
            c.pinZ = player.getZ();
            c.pinDimension = DimensionUtil.id(this.minecraft.level.dimension());
            c.centerMode = SpawnRadiusConfig.CenterMode.PINNED;
            centerMode.setMessage(centerLabel());
            centerMode.setTooltip(Tooltip.create(centerTooltip()));
        }).width(84).tooltip(Tooltip.create(Component.translatable("spawnradius.screen.pin_here.tooltip"))).build();
        pinHere.active = this.minecraft.player != null;
        list.rowWithEnd(centerMode, pinHere);
    }

    private void updateFixedY(EditBox box) {
        boolean fixed = config.yMode == SpawnRadiusConfig.YMode.FIXED;
        box.active = fixed;
        box.setEditable(fixed);
    }

    @Override
    protected void save() {
        config.save();
    }

    @Override
    protected Component resetLabel() {
        return Component.translatable("spawnradius.screen.reset");
    }

    @Override
    protected Component resetQuestion() {
        return Component.translatable("spawnradius.screen.reset.title");
    }

    @Override
    protected Component resetWarning() {
        return Component.translatable("spawnradius.screen.reset.tooltip");
    }

    @Override
    protected void reset() {
        config.resetToDefaults();
    }

    @Override
    protected Tooltip doneTooltip() {
        return Tooltip.create(Component.translatable("spawnradius.screen.note"));
    }

    // ---- labels ----

    private static Component ringLabel(SpawnRing ring) {
        return Component.translatable("spawnradius.short." + ring.kind().key, ring.radius());
    }

    private static Component ringTooltip(SpawnRing ring) {
        return Component.translatable("spawnradius.tooltip." + ring.kind().key, ring.radius())
            .append("\n\n")
            .append(Component.translatable("spawnradius.tooltip.applies_to", Legend.categories(ring.categories())));
    }

    private Component thicknessLabel() {
        return Component.translatable("spawnradius.screen.thickness", config.thickness);
    }

    private Component yModeLabel() {
        return Component.translatable(config.yMode == SpawnRadiusConfig.YMode.PLAYER
            ? "spawnradius.screen.y_mode.player" : "spawnradius.screen.y_mode.fixed");
    }

    private Component centerLabel() {
        return Component.translatable(config.centerMode == SpawnRadiusConfig.CenterMode.PINNED
            ? "spawnradius.screen.center.pinned_short" : "spawnradius.screen.center.player");
    }

    private Component centerTooltip() {
        Component tip = Component.translatable("spawnradius.screen.center.tooltip");
        if (!config.pinDimension.isEmpty()) {
            tip = Component.empty().append(tip).append("\n\n").append(Component.translatable(
                "spawnradius.screen.center.pinned", (int) Math.floor(config.pinX), (int) Math.floor(config.pinY),
                (int) Math.floor(config.pinZ)));
        }
        return tip;
    }
}
