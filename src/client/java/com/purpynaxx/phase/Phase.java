package com.purpynaxx.phase;

import com.purpynaxx.phase.events.EventManager;
import com.purpynaxx.phase.modules.Modules;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.impl.launch.FabricLauncherBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;


public class Phase implements ClientModInitializer {

    public final static Logger LOGGER = LoggerFactory.getLogger("Phase");
    public final static boolean IS_DEV_ENVIRONMENT = FabricLauncherBase.getLauncher().isDevelopment();

    @Override
    public void onInitializeClient() {

        Modules modules = Modules.getInstance();
        EventManager eventManager = EventManager.getInstance();

        Executors.newSingleThreadExecutor().submit(() -> {

            Thread.currentThread().setName("Phase Initialization");

            long startTime = System.currentTimeMillis();

            // Discover and register modules
            if (!modules.init()) {
                LOGGER.warn("Some modules were not instanced properly ! Please check errors above.");
            }

            // Register events
            eventManager.init();

            LOGGER.info("Phase initialized in {}ms", System.currentTimeMillis() - startTime);
        });
    }

}
