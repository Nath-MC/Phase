package com.purpynaxx.phase;

import com.purpynaxx.phase.events.EventManager;
import com.purpynaxx.phase.modules.impl.ModuleManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.impl.launch.FabricLauncherBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Phase implements ClientModInitializer {
    public final static Logger logger = LoggerFactory.getLogger(Phase.class);
    public final static boolean isDevEnvironment = FabricLauncherBase.getLauncher().isDevelopment();

    @Override
    public void onInitializeClient() {
        logger.info("Hello Fabric World !");

        ModuleManager moduleManager = ModuleManager.getInstance();
        if (!moduleManager.init()) // Register modules and check result
            logger.warn("Some modules were not instanced properly ! Please check above errors.");

        EventManager.getInstance().init();
    }
}
