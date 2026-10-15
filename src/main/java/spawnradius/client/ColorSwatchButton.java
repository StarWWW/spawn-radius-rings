package spawnradius.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.network.chat.Component;

import java.util.function.IntSupplier;

/** Vanilla-styled button that shows a colour swatch; clicking it cycles to the next palette colour. */
class ColorSwatchButton extends AbstractButton {
    private final IntSupplier color;
    private final Runnable action;

    ColorSwatchButton(int x, int y, int width, int height, Component narration, IntSupplier color, Runnable action) {
        super(x, y, width, height, narration);
        this.color = color;
        this.action = action;
    }

    @Override
    public void onPress(InputWithModifiers input) {
        action.run();
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        extractDefaultSprite(g);
        int pad = 4;
        int x0 = getX() + pad;
        int y0 = getY() + pad;
        int x1 = getRight() - pad;
        int y1 = getBottom() - pad;
        g.fill(x0 - 1, y0 - 1, x1 + 1, y1 + 1, 0xFF000000);
        g.fill(x0, y0, x1, y1, color.getAsInt() | 0xFF000000);
        // small highlight so light and dark colours both read as a "chip"
        g.fill(x0, y0, x1, y0 + 1, 0x40FFFFFF);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
