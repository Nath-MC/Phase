package com.purpynaxx.phase.helpers.chat;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

public final class ChatHelper {

    public static final String PREFIX = "§l§3[§bPhase§3]§r ";
    private static final MinecraftClient client = MinecraftClient.getInstance();

    private ChatHelper() {}


    public static void send(String message) {
        if (client.player == null) return;
        MutableText text = Text.literal(PREFIX + message);
        client.player.sendMessage(text, false);
    }

}
