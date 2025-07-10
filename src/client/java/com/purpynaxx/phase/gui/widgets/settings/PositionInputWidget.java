package com.purpynaxx.phase.gui.widgets.settings;

import com.purpynaxx.phase.settings.PositionInputSetting;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.glfw.GLFW;

public class PositionInputWidget extends SettingWidget<BlockPos> {

    private static final int FIELD_WIDTH = 28;
    private static final int FIELD_HEIGHT = 16;

    private static final int PADDING = 4;
    private static final int TEXT_PADDING = 2;

    private static final int TEXT_COLOR = 0xFFE0E0E0;
    private static final int PLACEHOLDER_COLOR = 0xFF707070;
    private static final int FIELD_BG_COLOR = 0xFF1E1E1E;
    private static final int FIELD_BORDER_COLOR = 0xFF3C3C3C;
    private static final int SELECTED_BORDER_COLOR = 0xFFFFFFFF;

    private final StringBuilder[] inputs = {new StringBuilder(), new StringBuilder(), new StringBuilder()};
    private final int[] caretPositions = new int[3];
    private final int[] scrollOffsets = new int[3]; // Character index to start rendering from

    private int selectedFieldIndex = -1; // -1: none, 0: x, 1: y, 2: z

    public PositionInputWidget(PositionInputSetting setting, int x, int y) {
        super(setting, x, y);

        if (!setting.isDefault()) {
            BlockPos initialPos = setting.getValue();
            inputs[0].append(initialPos.getX());
            inputs[1].append(initialPos.getY());
            inputs[2].append(initialPos.getZ());
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        int currentX = this.x + PADDING;
        int fieldY = this.y + (this.height - FIELD_HEIGHT) / 2;

        for (int i = 0; i < 3; i++) {
            drawField(context, currentX, fieldY, i);
            currentX += FIELD_WIDTH + PADDING;
        }
    }

    private void drawField(DrawContext context, int x, int y, int fieldIndex) {
        StringBuilder input = inputs[fieldIndex];
        boolean isSelected = selectedFieldIndex == fieldIndex;


        context.fill(x, y, x + FIELD_WIDTH, y + FIELD_HEIGHT, FIELD_BG_COLOR);
        context.drawBorder(x, y, FIELD_WIDTH, FIELD_HEIGHT, isSelected ? SELECTED_BORDER_COLOR : FIELD_BORDER_COLOR);


        context.enableScissor(x + TEXT_PADDING, y, x + FIELD_WIDTH - TEXT_PADDING, y + FIELD_HEIGHT);

        if (input.isEmpty()) {
            drawPlaceholder(context, x, y, fieldIndex);
        } else {
            drawInputText(context, x, y, fieldIndex);
        }

        context.disableScissor();


        if (isSelected) {
            drawCaret(context, x, y, fieldIndex);
        }
    }

    private void drawPlaceholder(DrawContext context, int x, int y, int fieldIndex) {
        String placeholder = switch (fieldIndex) {
            case 0 -> "x";
            case 1 -> "y";
            case 2 -> "z";
            default -> "";
        };
        int placeholderX = x + (FIELD_WIDTH - textRenderer.getWidth(placeholder)) / 2;
        int placeholderY = y + (FIELD_HEIGHT - textRenderer.fontHeight) / 2 + 1;
        context.drawText(textRenderer, Text.literal(placeholder).formatted(Formatting.ITALIC), placeholderX, placeholderY, PLACEHOLDER_COLOR, false);
    }

    private void drawInputText(DrawContext context, int x, int y, int fieldIndex) {
        StringBuilder input = inputs[fieldIndex];
        int scrollOffset = scrollOffsets[fieldIndex];

        String visibleText = input.substring(scrollOffset);

        int textX = x + TEXT_PADDING;
        int textY = y + (FIELD_HEIGHT - textRenderer.fontHeight) / 2 + 1;
        context.drawText(textRenderer, Text.literal(visibleText), textX, textY, TEXT_COLOR, true);
    }

    private void drawCaret(DrawContext context, int x, int y, int fieldIndex) {
        if (System.currentTimeMillis() % 1000 < 500) {
            return;
        }

        int caret = caretPositions[fieldIndex];
        int scroll = scrollOffsets[fieldIndex];

        // Defensive check to prevent rendering crash if state is temporarily invalid
        if (caret < scroll || scroll > inputs[fieldIndex].length()) {
            return;
        }

        String textBeforeCaret = inputs[fieldIndex].substring(scroll, caret);
        int caretX = x + TEXT_PADDING + textRenderer.getWidth(textBeforeCaret);

        int caretY0 = y + (FIELD_HEIGHT - textRenderer.fontHeight) / 2;
        context.fill(caretX, caretY0, caretX + 1, caretY0 + textRenderer.fontHeight, SELECTED_BORDER_COLOR);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isMouseOver(mouseX, mouseY) && button == 0) {
            int currentX = this.x + PADDING;
            int fieldY = this.y + (this.height - FIELD_HEIGHT) / 2;

            for (int i = 0; i < 3; i++) {
                if (mouseX >= currentX && mouseX <= currentX + FIELD_WIDTH && mouseY >= fieldY && mouseY <= fieldY + FIELD_HEIGHT) {
                    selectedFieldIndex = i;
                    setFocused(this);
                    // Move caret to click position
                    int clickX = (int) mouseX - currentX - TEXT_PADDING;
                    int scroll = scrollOffsets[i];
                    String visibleText = inputs[i].substring(scroll);
                    int charIndex = textRenderer.trimToWidth(visibleText, clickX).length();
                    setCurrentCaret(scroll + charIndex);
                    return true;
                }
                currentX += FIELD_WIDTH + PADDING;
            }
        }

        selectedFieldIndex = -1;
        setFocused(null);
        return false;
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (selectedFieldIndex == -1) return false;

        StringBuilder currentInput = inputs[selectedFieldIndex];
        int caret = caretPositions[selectedFieldIndex];

        // Allow digits, or a minus sign only at the beginning
        if (Character.isDigit(chr) || (chr == '-' && currentInput.isEmpty())) {
            currentInput.insert(caret, chr);
            setCurrentCaret(caret + 1);
            updateValue();
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (selectedFieldIndex == -1) return false;

        if (handleClipboard(keyCode)) return true;
        if (handleNavigation(keyCode)) return true;
        return handleDeletion(keyCode);
    }

    private void setFocused(Element element) {
        if (client.currentScreen != null) {
            client.currentScreen.setFocused(element);
        }
    }

    private boolean handleClipboard(int keyCode) {
        if (Screen.isSelectAll(keyCode)) {
            setCurrentCaret(inputs[selectedFieldIndex].length());
            return true;
        }
        if (Screen.isCopy(keyCode)) {
            client.keyboard.setClipboard(inputs[selectedFieldIndex].toString());
            return true;
        }
        if (Screen.isCut(keyCode)) {
            client.keyboard.setClipboard(inputs[selectedFieldIndex].toString());
            inputs[selectedFieldIndex].setLength(0);
            setCurrentCaret(0);
            updateValue();
            return true;
        }
        if (Screen.isPaste(keyCode)) {
            String clipboard = client.keyboard.getClipboard();
            // Sanitize pasted text to only include valid characters
            String sanitized = clipboard.replaceAll("[^0-9-]", "");
            if (!sanitized.isEmpty()) {
                StringBuilder currentInput = inputs[selectedFieldIndex];
                int caret = caretPositions[selectedFieldIndex];
                currentInput.insert(caret, sanitized);
                setCurrentCaret(caret + sanitized.length());
                updateValue();
            }
            return true;
        }
        return false;
    }

    private boolean handleNavigation(int keyCode) {
        int caret = caretPositions[selectedFieldIndex];
        int inputLength = inputs[selectedFieldIndex].length();

        switch (keyCode) {
            case GLFW.GLFW_KEY_LEFT -> {
                if (caret == 0 && selectedFieldIndex > 0) {
                    selectedFieldIndex--;
                    setCurrentCaret(inputs[selectedFieldIndex].length());
                } else {
                    setCurrentCaret(caret - 1);
                }
                return true;
            }
            case GLFW.GLFW_KEY_RIGHT -> {
                if (caret == inputLength && selectedFieldIndex < 2) {
                    selectedFieldIndex++;
                    setCurrentCaret(0);
                } else {
                    setCurrentCaret(caret + 1);
                }
                return true;
            }
            case GLFW.GLFW_KEY_TAB -> {
                selectedFieldIndex = (selectedFieldIndex + 1) % 3;
                setCurrentCaret(inputs[selectedFieldIndex].length());
                return true;
            }
            case GLFW.GLFW_KEY_HOME -> {
                setCurrentCaret(0);
                return true;
            }
            case GLFW.GLFW_KEY_END -> {
                setCurrentCaret(inputLength);
                return true;
            }
            case GLFW.GLFW_KEY_ESCAPE -> {
                selectedFieldIndex = -1;
                this.setFocused(null);
                return true;
            }
        }
        return false;
    }

    private boolean handleDeletion(int keyCode) {
        StringBuilder currentInput = inputs[selectedFieldIndex];
        int caret = caretPositions[selectedFieldIndex];

        if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            if (caret > 0) {
                currentInput.deleteCharAt(caret - 1);
                setCurrentCaret(caret - 1);
                updateValue();
                return true;
            }
        } else if (keyCode == GLFW.GLFW_KEY_DELETE) {
            if (caret < currentInput.length()) { // Delete character after caret
                currentInput.deleteCharAt(caret);
                setCurrentCaret(caret);
                updateValue();
                return true;
            }
        }
        return false;
    }

    /**
     * The core logic for scrolling and caret management.
     * Updates the caret position and adjusts the horizontal scroll offset
     * to ensure the caret remains visible within the text field.
     */
    private void setCurrentCaret(int newCaretPos) {
        if (selectedFieldIndex < 0) return;

        StringBuilder input = inputs[selectedFieldIndex];
        int inputLength = input.length();
        int caret = Math.max(0, Math.min(inputLength, newCaretPos));
        caretPositions[selectedFieldIndex] = caret;

        int scroll = scrollOffsets[selectedFieldIndex];
        int fieldContentWidth = FIELD_WIDTH - TEXT_PADDING * 2; // Inner visible width for text

        // If the text fits entirely, always reset scroll to 0.
        if (textRenderer.getWidth(input.toString()) <= fieldContentWidth) {
            scrollOffsets[selectedFieldIndex] = 0;
            return;
        }

        // If caret is to the left of the visible area, scroll left to show it.
        if (caret < scroll) {
            scrollOffsets[selectedFieldIndex] = caret;
            return;
        }

        // If caret is to the right of the visible area, scroll right.
        // We check this by measuring the width of the text from the current scroll start to the caret.
        int widthToCaret = textRenderer.getWidth(input.substring(scroll, caret));
        if (widthToCaret > fieldContentWidth) {
            // The caret is out of view. We need to find a new scroll position.
            // We do this by finding how much of the text *before* the caret can fit.
            String textBeforeCaret = input.substring(0, caret);
            // trimToWidth with reverse=true gives us the suffix of the string that fits,
            // effectively trimming from the beginning.
            String fittingText = textRenderer.trimToWidth(textBeforeCaret, fieldContentWidth, true);
            // The new scroll position is the index where the fitting text starts.
            scrollOffsets[selectedFieldIndex] = textBeforeCaret.length() - fittingText.length();
        }
    }

    private void updateValue() {
        int x = parseInput(inputs[0]);
        int y = parseInput(inputs[1]);
        int z = parseInput(inputs[2]);
        setting.setValue(new BlockPos(x, y, z));
    }

    private int parseInput(StringBuilder input) {
        String s = input.toString();
        if (s.isEmpty() || "-".equals(s)) {
            return 0;
        }
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            // Handle cases where the number is too large or small for an integer
            return s.startsWith("-") ? Integer.MIN_VALUE : Integer.MAX_VALUE;
        }
    }

}
