/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.utils;

import java.io.File;
import javax.naming.Context;
import javax.naming.InitialContext;

import org.dspace.kernel.DSpaceKernel;
import org.dspace.kernel.DSpaceKernelManager;
import org.dspace.servicemanager.DSpaceKernelImpl;
import org.dspace.servicemanager.DSpaceKernelInit;
import org.dspace.servicemanager.config.DSpaceConfigurationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextClosedEvent;

/**
 * Utility class that will initialize the DSpace Kernel on Spring Boot startup.
 * Used by org.dspace.app.rest.Application
 */
public class DSpaceKernelInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    private static final Logger log = LoggerFactory.getLogger(DSpaceKernelInitializer.class);

    private transient DSpaceKernel dspaceKernel;

    @Override
    public void initialize(final ConfigurableApplicationContext applicationContext) {

        String dspaceHome = applicationContext.getEnvironment().getProperty("dspace.dir");

        this.dspaceKernel = DSpaceKernelManager.getDefaultKernel();
        if (this.dspaceKernel == null) {
            DSpaceKernelImpl kernelImpl = null;
            try {
                kernelImpl = DSpaceKernelInit.getKernel(null);
                if (!kernelImpl.isRunning()) {
                    kernelImpl.start(getProvidedHome(dspaceHome)); // init the kernel
                }
                this.dspaceKernel = kernelImpl;

            } catch (Exception e) {
                // failed to start so destroy it and log and throw an exception
                try {
                    if (kernelImpl != null) {
                        kernelImpl.destroy();
                    }
                    this.dspaceKernel = null;
                } catch (Exception e1) {
                    // nothing
                }
                String message = "Failure during ServletContext initialisation: " + e.getMessage();
                log.error(message, e);
                throw new RuntimeException(message, e);
            }
        }

        if (applicationContext.getParent() == null) {
            //Set the DSpace Kernel Application context as a parent of the Spring Boot context so that
            //we can auto-wire all DSpace Kernel services
            applicationContext.setParent(dspaceKernel.getServiceManager().getApplicationContext());

            //Add a listener for Spring Boot application shutdown so that we can nicely cleanup the DSpace kernel.
            applicationContext.addApplicationListener(new DSpaceKernelDestroyer(dspaceKernel));
        }
    }

    /**
     * Find DSpace's "home" directory.
     * Initially look for JNDI Resource called "java:/comp/env/dspace.dir".
     * If not found, look for "dspace.dir" initial context parameter.
     */
    private String getProvidedHome(String dspaceHome) {
        String providedHome = null;
        try {
            Context ctx = new InitialContext();
            providedHome = (String) ctx.lookup("java:/comp/env/" + DSpaceConfigurationService.DSPACE_HOME);
        } catch (Exception e) {
            // do nothing
        }

        if (providedHome == null) {
            if (dspaceHome != null && !dspaceHome.equals("") &&
                !dspaceHome.equals("${" + DSpaceConfigurationService.DSPACE_HOME + "}")) {
                File test = new File(dspaceHome);
                if (test.exists() && new File(test, DSpaceConfigurationService.DSPACE_CONFIG_PATH).exists()) {
                    providedHome = dspaceHome;
                }
            }
        }
        return providedHome;
    }


    /**
     * Utility class that will destroy the DSpace Kernel on Spring Boot shutdown
     */
    private class DSpaceKernelDestroyer implements ApplicationListener<ContextClosedEvent> {
        private DSpaceKernel kernel;

        public DSpaceKernelDestroyer(DSpaceKernel kernel) {
            this.kernel = kernel;
        }

        public void onApplicationEvent(final ContextClosedEvent event) {
            if (this.kernel != null) {
                this.kernel.destroy();
                this.kernel = null;
            }
        }
    }
}

