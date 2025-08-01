package com.purpynaxx.phase;

import com.purpynaxx.phase.commands.Commands;
import com.purpynaxx.phase.events.EventManager;
import com.purpynaxx.phase.modules.Modules;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.loader.impl.launch.FabricLauncherBase;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.Future;


public class Phase implements ClientModInitializer {

    public final static Logger LOGGER = LoggerFactory.getLogger("Phase");
    public final static boolean IS_DEV_ENVIRONMENT = FabricLauncherBase.getLauncher().isDevelopment();
    public final static KeyBinding keyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.phase.open_menu",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_RIGHT_SHIFT,
            "key.categories.phase"
    ));

    @Override
    public void onInitializeClient() {
        Future<?> initializationTask = Executors.newSingleThreadExecutor().submit(this::init);

        Executors.newVirtualThreadPerTaskExecutor().submit(() -> checkForInit(initializationTask));
    }

    private void init() {
        Thread.currentThread().setName("Phase Initialization");

        long startTime = System.currentTimeMillis();

        // Discover and register modules
        Modules.getInstance().init();

        //Discover and register commands
        Commands.getInstance().init();

        // Register events
        EventManager.getInstance().init();

        LOGGER.info("Phase initialized in {}ms", System.currentTimeMillis() - startTime);
    }

    private void checkForInit(Future<?> initializationTask) {
        try {
            initializationTask.get();
        } catch (Exception e) {
            LOGGER.error("A fatal error occurred during Phase initialization", e);
            System.exit(-1);
        }

    }

}
