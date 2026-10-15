package spawnradius.client;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import xaero.lib.client.gui.widget.Tooltip;
import xaero.map.gui.GuiTexturedButton;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * A map button drawn exactly like Xaero's own World Map buttons: it is Xaero's {@link GuiTexturedButton}
 * (white pixel icon with a dark drop shadow, lifted by 1px and tinted on hover, Xaero tooltip) using this mod's
 * icon atlas. The icon can switch between an "on" and an "off" variant.
 */
public class XaeroIconButton extends GuiTexturedButton {
    public static final Identifier ATLAS = Identifier.fromNamespaceAndPath("spawnradius", "textures/gui/buttons.png");
    private static final int ATLAS_SIZE = 64;
    private static final int ICON = 16;

    /** Atlas columns (16px each). */
    public static final int RINGS_ON = 0;
    public static final int RINGS_OFF = 1;
    public static final int SETTINGS = 2;

    private final BooleanSupplier state;
    private final int onIcon;
    private final int offIcon;

    public XaeroIconButton(int x, int y, int onIcon, int offIcon, BooleanSupplier state, OnPress onPress,
                           Supplier<Tooltip> tooltip) {
        super(x, y, 20, 20, onIcon * ICON, 0, ICON, ICON, ATLAS, onPress, tooltip, ATLAS_SIZE, ATLAS_SIZE);
        this.state = state;
        this.onIcon = onIcon;
        this.offIcon = offIcon;
    }

    @Override
    public void extractContents(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTicks) {
        this.textureX = (state.getAsBoolean() ? onIcon : offIcon) * ICON;
        super.extractContents(guiGraphics, mouseX, mouseY, partialTicks);
    }

    /** Xaero-style tooltip: the bound key in dark green in front of the text, if a key is bound. */
    public static Tooltip tooltip(Component text, KeyMapping key) {
        Component line = text;
        if (key != null && !key.isUnbound()) {
            line = Component.empty()
                .append(Component.literal(key.getTranslatedKeyMessage().getString() + " ")
                    .withStyle(net.minecraft.ChatFormatting.DARK_GREEN))
                .append(text);
        }
        return new Tooltip(line);
    }
}
