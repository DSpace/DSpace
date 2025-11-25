/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.shell.config;

import org.dspace.servicemanager.DSpaceKernelImpl;
import org.dspace.servicemanager.DSpaceKernelInit;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Starts the DSpace kernel to load all configurations and services
 */
@Configuration
public class DSpaceConfigLoader {

    /**
     * DSpace Kernel. Must be started to initialize ConfigurationService and
     * any other services.
     */
    protected static transient DSpaceKernelImpl kernelImpl;

    @Bean
    public DSpaceKernelImpl dspaceKernel() throws IllegalStateException {

        // Initialize the service manager kernel
        try {
            // Get a reference to current Kernel
            kernelImpl = DSpaceKernelInit.getKernel(null);
            // If somehow the kernel is NOT initialized, initialize it.
            // NOTE: This is likely never going to occur, as Spring Boot initializes it
            if (!kernelImpl.isRunning()) {
                kernelImpl.start(); // init the kernel
            }
        } catch (Exception e) {
            // Failed to start so destroy it and log and throw an exception
            try {
                kernelImpl.destroy();
            } catch (Exception e1) {
                String message = "Failed to destroy DSpace Kernel: " + e.getMessage();
                throw new IllegalStateException(message, e);
            }
            String message = "Failed to startup DSpace Kernel: " + e.getMessage();
            System.err.println(message);
            e.printStackTrace();
            throw new IllegalStateException(message, e);
        }

        return kernelImpl;
    }
}
