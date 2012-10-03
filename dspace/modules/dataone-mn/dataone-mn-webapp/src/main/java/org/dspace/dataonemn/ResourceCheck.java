package org.dspace.dataonemn;

import java.util.TimerTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResourceCheck extends TimerTask {

	private static final Logger LOGGER = LoggerFactory.getLogger(ResourceCheck.class);
	
	public ResourceCheck() {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Starting TimerTask: " + ResourceCheck.class.getName());
		}
	}

	@Override
	public void run() {
		if (LOGGER.isInfoEnabled()) {
			Runtime runtime = Runtime.getRuntime();
			
			LOGGER.info("Used memory: " + runtime.totalMemory());
			LOGGER.info("Free memory: " + runtime.freeMemory());
		}
	}

}
