package com.purpynaxx.phase;

import com.purpynaxx.phase.modules.ModuleRegister;
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

		ModuleRegister moduleRegister = ModuleRegister.getInstance();
		if (!moduleRegister.register()) // Register modules and check result
			logger.warn("Some modules were not instanced properly ! Please check above errors.");
	}
}
