/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.servicemanager;

import java.util.Date;
import java.util.List;

import org.dspace.kernel.ServiceManager;
import org.dspace.servicemanager.config.DSpaceConfigurationService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.KernelStartupCallbackService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is the kernel implementation which starts up the core of DSpace
 * It also loads up the configuration.  Sets a JRE shutdown hook.
 * <p>
 * Note that this does not start itself and calling the {@link #getInstance()}
 * does not actually start it up either. It has to be explicitly started by
 * calling the start method so something in the system needs to do that. 
 * If the bean is already started then calling start on it again has no 
 * effect.
 * <p>
 * 
 * @author Aaron Zeckoski (azeckoski @ gmail.com)
 */
public final class DSpaceKernel {

    private static Logger log = LoggerFactory.getLogger(DSpaceKernel.class);

    private static class Holder {
        static final DSpaceKernel INSTANCE = new DSpaceKernel();
    }

    private DSpaceKernel() {
        log.info("Created new kernel: " + this);
    }

    /**
     * Returns lazy initialized singleton instance DSpaceKernel. Initialization is thread safe.
     *
     * @return thread safe singleton instance of DSpaceKernel
     */
    public static DSpaceKernel getInstance() {
        return Holder.INSTANCE;
    }

    private boolean running = false;

    private Date lastLoadDate;
    private long loadTime;

    private boolean destroyed = false;
    private final Object lock = new Object();

    private Thread shutdownHook;

    private ConfigurationService configurationService;
    private ServiceManagerSystem serviceManagerSystem;

    public ConfigurationService getConfigurationService() {
        return configurationService;
    }

    public ServiceManager getServiceManager() {
        return serviceManagerSystem;
    }

    public boolean isRunning() {
        synchronized (lock) {
            return running;
        }
    }

    @Deprecated
    public void start(String legacyHome) {
        start();
    }

    public void start() {
        if (isRunning()) {
            return;
        }

        synchronized (lock) {
            lastLoadDate = new Date();
            long startTime = System.currentTimeMillis();

            // create the configuration service and get the configuration
            DSpaceConfigurationService dsConfigService = new DSpaceConfigurationService();
            configurationService = dsConfigService;

            // startup the service manager
            serviceManagerSystem = new DSpaceServiceManager(dsConfigService);
            serviceManagerSystem.startup();

            loadTime = System.currentTimeMillis() - startTime;

            running = true;

            List<KernelStartupCallbackService> callbackServices = serviceManagerSystem.getServicesByType(KernelStartupCallbackService.class);
            for (KernelStartupCallbackService callbackService : callbackServices) {
                callbackService.executeCallback();
            }
            // add in the shutdown hook
            registerShutdownHook();
        }
        log.info("DSpace kernel startup completed in "+loadTime+" ms and registered as MBean: ");
    }

    public void destroy() {
        if (this.destroyed) {
            return;
        }
        synchronized (lock) {
            // stop the kernel
            try {
                stop();
            } catch (Exception e) {
                // oh well
            }

            if (this.shutdownHook != null) {
                try {
                    Runtime.getRuntime().removeShutdownHook(this.shutdownHook);
                    this.shutdownHook = null;
                } catch (Exception e) {
                    // ok, keep going
                }
            }

            this.destroyed = true;
        }
    }

    @Override
    protected void finalize() throws Throwable {
        try {
            doDestroy();
        } catch (Exception e) {
            log.error("WARN Failure attempting to cleanup the DSpace kernel: " + e.getMessage(), e);
        }
        super.finalize();
    }

    @Override
    public String toString() {
        return "DSpaceKernel:lastLoad=" + lastLoadDate + ":loadTime=" + loadTime + ":running=" + running + ":kernel=" + getClass().getName() + "@" + getClass().getClassLoader() + ":" + super.toString();
    }

    private void registerShutdownHook() {
        if (this.shutdownHook == null) {
            synchronized (lock) {
                // No shutdown hook registered yet
                this.shutdownHook = new Thread() {
                    public void run() {
                        doDestroy();
                    }
                };
                Runtime.getRuntime().addShutdownHook(this.shutdownHook);
            }
        }
    }

    /*
     * Called from within the shutdown thread.
     */
    private void doDestroy() {
        if (!this.destroyed) {
            destroy();
        }
    }

    private void stop() {
        if (!running) {
            return;
        }

        synchronized (lock) {
            // wipe all the variables to free everything up
            running = false;
            if (serviceManagerSystem != null) {
                serviceManagerSystem.shutdown();
            }
            serviceManagerSystem = null;
            configurationService = null;
        }
        // log completion (logger may be gone at this point so we cannot really use it)
        log.info("DSpace kernel shutdown completed");
    }
}
