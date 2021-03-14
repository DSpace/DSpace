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

import org.apache.commons.lang3.StringUtils;
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
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * Utility class that will initialize the DSpace Kernel on Spring Boot startup.
 * Used by org.dspace.app.rest.Application
 */
public class DSpaceKernelInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    private static final Logger log = LoggerFactory.getLogger(DSpaceKernelInitializer.class);

    private transient DSpaceKernel dspaceKernel;

    @Override
    public void initialize(final ConfigurableApplicationContext applicationContext) {
        // Check if the kernel is already started
        this.dspaceKernel = DSpaceKernelManager.getDefaultKernel();
        if (this.dspaceKernel == null) {
            DSpaceKernelImpl kernelImpl = null;
            try {
                // Load the kernel with default settings
                kernelImpl = DSpaceKernelInit.getKernel(null);
                if (!kernelImpl.isRunning()) {
                    // Determine configured DSpace home & init the Kernel
                    kernelImpl.start(getDSpaceHome(applicationContext.getEnvironment()));
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
            // Set the DSpace Kernel Application context as a parent of the Spring Boot context so that
            // we can auto-wire all DSpace Kernel services
            applicationContext.setParent(dspaceKernel.getServiceManager().getApplicationContext());

            //Add a listener for Spring Boot application shutdown so that we can nicely cleanup the DSpace kernel.
            applicationContext.addApplicationListener(new DSpaceKernelDestroyer(dspaceKernel));
        }
    }

    /**
     * Find DSpace's "home" directory (from current environment)
     * Initially look for JNDI Resource called "java:/comp/env/dspace.dir".
     * If not found, use value provided in "dspace.dir" in Spring Environment
     */
    private String getDSpaceHome(ConfigurableEnvironment environment) {
        // Load the "dspace.dir" property from Spring Boot's Configuration (application.properties)
        // This gives us the location of our DSpace configurations, necessary to start the kernel
        String providedHome = environment.getProperty(DSpaceConfigurationService.DSPACE_HOME);

        String dspaceHome = null;
        try {
            // Allow ability to override home directory via JNDI
            Context ctx = new InitialContext();
            dspaceHome = (String) ctx.lookup("java:/comp/env/" + DSpaceConfigurationService.DSPACE_HOME);
        } catch (Exception e) {
            // do nothing
        }

        // Otherwise, verify the 'providedHome' value is non-empty, exists and includes DSpace configs
        if (dspaceHome == null) {
            if (StringUtils.isNotBlank(providedHome) &&
                !providedHome.equals("${" + DSpaceConfigurationService.DSPACE_HOME + "}")) {
                File test = new File(providedHome);
                if (test.exists() && new File(test, DSpaceConfigurationService.DSPACE_CONFIG_PATH).exists()) {
                    dspaceHome = providedHome;
                }
            }
        }
        return dspaceHome;
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

