package com.purpynaxx.phase.helpers.color;

import net.minecraft.util.math.MathHelper;

public final class ColorHelper {

    private ColorHelper() {}

    /**
     * Interpolates between two colors based on a progress value.
     *
     * @param color1   The starting color.
     * @param color2   The ending color.
     * @param progress The interpolation progress (0.0 to 1.0).
     * @return The interpolated color.
     */
    public static int interpolateColor(int color1, int color2, float progress) {
        int a1 = (color1 >> 24) & 0xFF;
        int r1 = (color1 >> 16) & 0xFF;
        int g1 = (color1 >> 8) & 0xFF;
        int b1 = color1 & 0xFF;

        int a2 = (color2 >> 24) & 0xFF;
        int r2 = (color2 >> 16) & 0xFF;
        int g2 = (color2 >> 8) & 0xFF;
        int b2 = color2 & 0xFF;

        int a = MathHelper.lerp(progress, a1, a2);
        int r = MathHelper.lerp(progress, r1, r2);
        int g = MathHelper.lerp(progress, g1, g2);
        int b = MathHelper.lerp(progress, b1, b2);

        return (a << 24) | (r << 16) | (g << 8) | b;
    }

}
