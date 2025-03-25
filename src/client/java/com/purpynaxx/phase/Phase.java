package com.purpynaxx.phase;

import com.purpynaxx.phase.events.EventManager;
import com.purpynaxx.phase.modules.impl.ModuleManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.loader.impl.launch.FabricLauncherBase;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Phase implements ClientModInitializer {
    public final static String modId = "phase";
    public final static Logger logger = LoggerFactory.getLogger(Phase.class);
    public final static boolean isDevEnvironment = FabricLauncherBase.getLauncher().isDevelopment();
    private static KeyBinding keyBinding;

    @Override
    public void onInitializeClient() {

        ModuleManager moduleManager = ModuleManager.getInstance();
        if (!moduleManager.init()) // Register modules and check result
            logger.warn("Some modules were not instanced properly ! Please check above errors.");

        EventManager.getInstance().init();

        //TODO translations
        keyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.phase.open_menu",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_V,
                "category.phase.general"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (keyBinding.wasPressed()) {
//                moduleManager.toggleModuleActive(GUI.class);
                //TODO, setup the config windows
            }
        });
    }
}
