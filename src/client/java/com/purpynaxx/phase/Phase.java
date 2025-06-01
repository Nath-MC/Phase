package com.purpynaxx.phase;

import com.purpynaxx.phase.config.ModuleConfigManager;
import com.purpynaxx.phase.events.EventManager;
import com.purpynaxx.phase.mixin.accessors.TitleScreenMixin;
import com.purpynaxx.phase.modules.impl.ModuleManager;
import com.purpynaxx.phase.modules.visuals.GUI;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.loader.impl.launch.FabricLauncherBase;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Phase implements ClientModInitializer {
    public final static String modId = "phase";
    public final static Logger logger = LoggerFactory.getLogger(Phase.class);
    public final static boolean isDevEnvironment = FabricLauncherBase.getLauncher().isDevelopment();
    public static KeyBinding keyBinding;

    @Override
    public void onInitializeClient() {
        long startTime = System.currentTimeMillis();
        ModuleManager moduleManager = ModuleManager.getInstance();
        if (!moduleManager.init()) // Register modules and check result
            logger.warn("Some modules were not instanced properly ! Please check errors above.");

        EventManager.getInstance().init();

        keyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.phase.open_menu",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                "key.categories.phase"
        ));

        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof TitleScreen titleScreen && ((TitleScreenMixin) titleScreen).getDoBackgroundFade() && moduleManager.isModuleActive(GUI.class))
                ((TitleScreenMixin) titleScreen).setDoBackgroundFade(false);
            ScreenKeyboardEvents.beforeKeyPress(screen).register((screen1, key, scancode, modifiers) -> {
                if (keyBinding.matchesKey(key, scancode))
                    moduleManager.toggleModuleActive(GUI.class);
            });
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (keyBinding.wasPressed())
                moduleManager.toggleModuleActive(GUI.class);
        });

        ClientLifecycleEvents.CLIENT_STOPPING.register(minecraftClient -> ModuleConfigManager.saveAllModules());
        logger.info("Phase initialized in {}ms", System.currentTimeMillis() - startTime);
    }
}
