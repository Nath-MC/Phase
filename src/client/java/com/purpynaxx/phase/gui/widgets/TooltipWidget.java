package com.purpynaxx.phase.gui.widgets;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Drawable;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;

import java.awt.*;
import java.util.List;

public class TooltipWidget implements Drawable {

    private static final int PADDING = 4;

    private final TextRenderer textRenderer;

    private final int width;
    private final int height;

    private final List<OrderedText> wrappedText;

    private int x;
    private int y;

    public TooltipWidget(Text content) {
        this.textRenderer = MinecraftClient.getInstance().textRenderer;
        this.wrappedText = textRenderer.wrapLines(content, 120);

        int maxLineWidth = 0;
        for (OrderedText line : wrappedText)
            maxLineWidth = Math.max(maxLineWidth, textRenderer.getWidth(line));


        this.width = maxLineWidth + (PADDING * 2);
        this.height = (wrappedText.size() * textRenderer.fontHeight) + (PADDING * 2);
    }

    public void refreshPos(int x, int y, int offsetX, int offsetY, int screenWidth, int screenHeight) {
        int newX = x + offsetX;
        int newY = y + offsetY - this.height / 2;
        this.x = Math.clamp(newX, 0, screenWidth);
        this.y = Math.clamp(newY, 0, screenHeight);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(x, y, x + width, y + height, new Color(26, 26, 26, 255).getRGB());

        int textY = y + PADDING;
        for (OrderedText line : wrappedText) {
            context.drawText(textRenderer, line, x + PADDING, textY, Color.WHITE.getRGB(), false);
            textY += textRenderer.fontHeight;
        }
    }

}
