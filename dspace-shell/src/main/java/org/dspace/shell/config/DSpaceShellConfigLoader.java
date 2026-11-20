/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.shell.config;

import jakarta.annotation.PreDestroy;
import org.dspace.servicemanager.DSpaceKernelImpl;
import org.dspace.servicemanager.DSpaceKernelInit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Starts the DSpace kernel to load all configurations and services
 */
@Configuration
public class DSpaceShellConfigLoader {

    private static final Logger log =
            LoggerFactory.getLogger(DSpaceShellConfigLoader.class);

    /**
     * DSpace Kernel. Must be started to initialize ConfigurationService and
     * any other services.
     */
    private DSpaceKernelImpl kernel;

    @Bean
    @Profile("!test")
    public DSpaceKernelImpl dspaceKernel() throws IllegalStateException {

        // Initialize the service manager kernel
        try {
            // Get a reference to current Kernel
            kernel = DSpaceKernelInit.getKernel(null);

            // If somehow the kernel is NOT initialized, initialize it.
            if (!kernel.isRunning()) {
                log.debug("Starting DSpace Kernel...");
                kernel.start();
            }
            log.debug("DSpace Kernel started successfully.");

            return kernel;

        } catch (Exception e) {
            // Failed to start so destroy it and log and throw an exception
            log.error("Failed to start DSpace Kernel", e);

            throw new IllegalStateException(
                    "Failed to startup DSpace Kernel", e);
        }

    }

    /** 
     * Shutdown and Destroy DSpace kernel
     */
    @PreDestroy
    public void shutdown() {

        if (kernel != null) {
            try {
                log.debug("Shutting down DSpace Kernel...");
                kernel.destroy();
            } catch (Exception e) {
                log.error("Error shutting down DSpace Kernel", e);
            }
        }

    }
}
