package com.purpynaxx.phase;

import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Phase implements ClientModInitializer {
	public final static Logger logger = LoggerFactory.getLogger(Phase.class);

	@Override
	public void onInitializeClient() {
		logger.info("Hello Fabric World !");
	}
}
