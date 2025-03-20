package com.purpynaxx.phase.helpers;

import net.minecraft.client.MinecraftClient;

import java.util.Objects;

public final class Player {

    private static final MinecraftClient client = MinecraftClient.getInstance();


    private Player() {
        throw new UnsupportedOperationException("Helper class, duh");
    }

    // or not in the main menu
    public static boolean isPlayerInWorld() {
        return !Objects.isNull(client.world);
    }

}
