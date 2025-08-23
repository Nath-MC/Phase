package com.purpynaxx.phase.helpers.input;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;

import java.util.stream.Stream;

public final class InputUtils {

    private static final MinecraftClient client = MinecraftClient.getInstance();

    private static boolean allowMovementKeys = true;

    private InputUtils() {}

    public static boolean shouldAllowKeyPress(InputUtil.Key key) {
        if (allowMovementKeys) return true;

        int code = key.getCode();

        KeyBinding forward = client.options.forwardKey;
        KeyBinding backward = client.options.backKey;
        KeyBinding left = client.options.leftKey;
        KeyBinding right = client.options.rightKey;
        KeyBinding jump = client.options.jumpKey;
        KeyBinding sneak = client.options.sneakKey;

        return Stream.of(forward, backward, left, right, jump, sneak).noneMatch(keyBinding -> keyBinding.matchesKey(code, 0));
    }

    public static void setAllowMovementKeys(boolean allowMovementKeys) {
        InputUtils.allowMovementKeys = allowMovementKeys;
    }

}
