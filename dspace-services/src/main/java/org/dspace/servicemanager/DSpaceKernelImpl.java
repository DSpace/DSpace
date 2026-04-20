/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.servicemanager;

import java.lang.ref.Cleaner;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.Descriptor;
import javax.management.DynamicMBean;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.ReflectionException;
import javax.management.modelmbean.DescriptorSupport;
import javax.management.modelmbean.ModelMBeanAttributeInfo;
import javax.management.modelmbean.ModelMBeanInfoSupport;
import javax.management.modelmbean.ModelMBeanOperationInfo;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.kernel.DSpaceKernel;
import org.dspace.kernel.DSpaceKernelManager;
import org.dspace.kernel.ServiceManager;
import org.dspace.servicemanager.config.DSpaceConfigurationService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.KernelStartupCallbackService;
import org.dspace.services.factory.DSpaceServicesFactory;

/**
 * This is the kernel implementation which starts up the core of DSpace,
 * registers the mbean, and initializes the DSpace object.
 * It also loads up the configuration.  Sets a JRE shutdown hook.
 * <p>
 * Note that this does not start itself and calling the constructor does
 * not actually start it up either. It has to be explicitly started by
 * calling the start method so something in the system needs to do that.
 * If the bean is already started then calling start on it again has no
 * effect.
 * <p>
 * The name of this instance can be specified if desired.
 *
 * @author Aaron Zeckoski (azeckoski @ gmail.com)
 */
public final class DSpaceKernelImpl implements DSpaceKernel, DynamicMBean {

    private static final Logger log = LogManager.getLogger();

    /**
     * Cleaner for Java 21+ compatibility (replaces deprecated finalize() method).
     * Used to clean up kernel resources if DSpaceKernelImpl is garbage-collected without being properly destroyed.
     */
    private static final Cleaner cleaner = Cleaner.create();

    /**
     * Handle to the registered cleanup action. Used to prevent double-cleanup when destroy() is called normally.
     */
    private Cleaner.Cleanable cleanable;

    /**
     * Holder for the kernel reference, used by the Cleaner.
     * Using AtomicReference allows the cleanup action to safely access the kernel
     * without preventing GC of the DSpaceKernelImpl object.
     */
    private final AtomicReference<DSpaceKernelImpl> kernelHolder = new AtomicReference<>();

    /**
     * Creates a DSpace Kernel, does not do any checks though.
     * Do not call this; use {@link DSpaceKernelInit#getKernel(String)}.
     *
     * @param name the name for the kernel
     */
    protected DSpaceKernelImpl(String name) {
        this.mBeanName = DSpaceKernelManager.checkName(name);
        // Register this kernel with the Cleaner for cleanup if GC'd without proper destruction
        kernelHolder.set(this);
        cleanable = cleaner.register(this, new KernelCleanup(kernelHolder));
    }

    private String mBeanName = MBEAN_NAME;
    private boolean running = false;
    private boolean destroyed = false;
    private final Object lock = new Object();
    private DSpaceKernel kernel = null;

    private Thread shutdownHook;

    protected void registerShutdownHook() {
        if (this.shutdownHook == null) {
            synchronized (lock) {
                // No shutdown hook registered yet
                this.shutdownHook = new Thread() {
                    @Override public void run() {
                        doDestroy();
                    }
                };
                Runtime.getRuntime().addShutdownHook(this.shutdownHook);
            }
        }
    }

    private ConfigurationService configurationService;

    @Override
    public ConfigurationService getConfigurationService() {
        return configurationService;
    }

    private ServiceManagerSystem serviceManagerSystem;

    @Override
    public ServiceManager getServiceManager() {
        return serviceManagerSystem;
    }

    /* (non-Javadoc)
     * @see org.dspace.kernel.DSpaceKernel#getMBeanName()
     */
    @Override
    public String getMBeanName() {
        return mBeanName;
    }

    /* (non-Javadoc)
     * @see org.dspace.kernel.DSpaceKernel#isRunning()
     */
    @Override
    public boolean isRunning() {
        synchronized (lock) {
            return running;
        }
    }

    /* (non-Javadoc)
     * @see org.dspace.kernel.CommonLifecycle#getManagedBean()
     */
    @Override
    public DSpaceKernel getManagedBean() {
        synchronized (lock) {
            return kernel;
        }
    }

    /* (non-Javadoc)
     * @see org.dspace.kernel.CommonLifecycle#start()
     */
    @Override
    public void start() {
        start(null);
    }

    /**
     * This starts up the entire core system.  May be called more than
     * once:  subsequent calls return without effect.
     *
     * @param dspaceHome path to DSpace home directory
     */
    public void start(String dspaceHome) {
        if (running) {
            //log.warn("Kernel ("+this+") is already started");
            return;
        }

        synchronized (lock) {
            lastLoadDate = Instant.now();
            long startTime = lastLoadDate.toEpochMilli();

            // create the configuration service and get the configuration
            DSpaceConfigurationService dsConfigService = new DSpaceConfigurationService(dspaceHome);
            configurationService = dsConfigService;

            // startup the service manager
            serviceManagerSystem = new DSpaceServiceManager(dsConfigService);
            serviceManagerSystem.startup();

            // initialize the static
//            DSpace.initialize(serviceManagerSystem);

            loadTime = Instant.now().toEpochMilli() - startTime;

            kernel = this;
            running = true;

            List<KernelStartupCallbackService> callbackServices =
                DSpaceServicesFactory.getInstance().getServiceManager()
                                     .getServicesByType(KernelStartupCallbackService.class);

            for (KernelStartupCallbackService callbackService : callbackServices) {
                callbackService.executeCallback();
            }
            // add in the shutdown hook
            registerShutdownHook();
        }
        log.info("DSpace kernel startup completed in {} ms and registered as MBean: {}",
                loadTime, mBeanName);
    }

    /* (non-Javadoc)
     * @see org.dspace.kernel.CommonLifecycle#stop()
     */
    @Override
    public void stop() {
        if (!running) {
            //log.warn("Kernel ("+this+") is already stopped");
            return;
        }

        synchronized (lock) {
//            DSpace.initialize(null); // clear out the static cover
            // wipe all the variables to free everything up
            running = false;
            kernel = null;
            if (serviceManagerSystem != null) {
                serviceManagerSystem.shutdown();
            }
            serviceManagerSystem = null;
            configurationService = null;
        }
        // log completion (logger may be gone at this point so we cannot really use it)
        log.info("DSpace kernel shutdown completed and unregistered MBean: " + mBeanName);
    }

    /* (non-Javadoc)
     * @see org.dspace.kernel.CommonLifecycle#destroy()
     */
    @Override
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

            // If this was the default kernel, clear it
            if (DSpaceKernelManager.getDefaultKernel() == this) {
                DSpaceKernelManager.setDefaultKernel(null);
            }

            try {
                // remove the mbean
                DSpaceKernelManager.unregisterMBean(mBeanName);
            } finally {
                // trash the shutdown hook as we do not need it anymore
                if (this.shutdownHook != null) {
                    try {
                        Runtime.getRuntime().removeShutdownHook(this.shutdownHook);
                        this.shutdownHook = null;
                    } catch (Exception e) {
                        // ok, keep going
                    }
                }
            }

            this.destroyed = true;

            // Clear the holder and unregister the Cleaner to prevent double-cleanup
            kernelHolder.set(null);
            if (cleanable != null) {
                cleanable.clean();
            }
        }
    }

    /**
     * Called from within the shutdown thread.
     */
    protected void doDestroy() {
        if (!this.destroyed) {
            destroy();
        }
    }

    /**
     * Static cleanup class for use with java.lang.ref.Cleaner (Java 21+ replacement for finalize()).
     * This class must be static to avoid preventing garbage collection of the DSpaceKernelImpl object.
     * It holds a reference to the AtomicReference containing the kernel, which is cleared
     * when destroy() is called normally, preventing double-cleanup.
     */
    private static class KernelCleanup implements Runnable {
        private static final Logger cleanupLog = LogManager.getLogger(KernelCleanup.class);
        private final AtomicReference<DSpaceKernelImpl> kernelRef;

        KernelCleanup(AtomicReference<DSpaceKernelImpl> kernelRef) {
            this.kernelRef = kernelRef;
        }

        @Override
        public void run() {
            // Get and clear the reference atomically
            DSpaceKernelImpl kernel = kernelRef.getAndSet(null);
            if (kernel != null && !kernel.destroyed) {
                cleanupLog.warn("DSpaceKernelImpl was garbage-collected without being properly destroyed. " +
                    "Attempting cleanup.");
                try {
                    kernel.doDestroy();
                } catch (Exception e) {
                    cleanupLog.error("Failure attempting to cleanup the DSpace kernel during GC: {}",
                        e.getMessage(), e);
                }
            }
        }
    }

    @Override
    public String toString() {
        return "DSpaceKernel:" + mBeanName + ":lastLoad=" + lastLoadDate + ":loadTime=" + loadTime + ":running=" +
            running + ":kernel=" + (kernel == null ? null : kernel
            .getClass().getName() + "@" + kernel.getClass().getClassLoader() + ":" + super.toString());
    }

    // MBEAN methods

    private Instant lastLoadDate;

    /**
     * Time that this kernel was started, as an Instant
     *
     * @return date object
     **/
    public Instant getLastLoadDate() {
        return lastLoadDate;
    }

    private long loadTime;

    /**
     * Time that this kernel was started, as seconds since the epoch.
     *
     * @return seconds since epoch (as a long)
     **/
    public long getLoadTime() {
        return loadTime;
    }

    @Override
    public Object invoke(String actionName, Object[] params, String[] signature)
        throws MBeanException, ReflectionException {
        return this;
    }

    @Override
    public MBeanInfo getMBeanInfo() {
        Descriptor lastLoadDateDesc = new DescriptorSupport(new String[] {"name=LastLoadDate",
            "descriptorType=attribute", "default=0", "displayName=Last Load Date",
            "getMethod=getLastLoadDate"});
        Descriptor lastLoadTimeDesc = new DescriptorSupport(new String[] {"name=LastLoadTime",
            "descriptorType=attribute", "default=0", "displayName=Last Load Time",
            "getMethod=getLoadTime"});

        ModelMBeanAttributeInfo[] mmbai = new ModelMBeanAttributeInfo[2];
        mmbai[0] = new ModelMBeanAttributeInfo("LastLoadDate", "java.time.Instant", "Last Load Date",
                                               true, false, false, lastLoadDateDesc);

        mmbai[1] = new ModelMBeanAttributeInfo("LastLoadTime", "java.lang.Long", "Last Load Time",
                                               true, false, false, lastLoadTimeDesc);

        ModelMBeanOperationInfo[] mmboi = new ModelMBeanOperationInfo[7];

        mmboi[0] = new ModelMBeanOperationInfo("start", "Start DSpace Kernel", null, "void",
                                               ModelMBeanOperationInfo.ACTION);
        mmboi[1] = new ModelMBeanOperationInfo("stop", "Stop DSpace Kernel", null, "void",
                                               ModelMBeanOperationInfo.ACTION);
        mmboi[2] = new ModelMBeanOperationInfo("getManagedBean", "Get the Current Kernel", null,
                                               DSpaceKernel.class.getName(), ModelMBeanOperationInfo.INFO);

        return new ModelMBeanInfoSupport(this.getClass().getName(), "DSpace Kernel", mmbai, null, mmboi, null);
    }

    @Override
    public Object getAttribute(String attribute)
        throws AttributeNotFoundException, MBeanException, ReflectionException {
        if ("LastLoadDate".equals(attribute)) {
            return getLastLoadDate();
        } else if ("LastLoadTime".equals(attribute)) {
            return getLoadTime();
        }
        throw new AttributeNotFoundException("invalid attribute: " + attribute);
    }

    @Override
    public AttributeList getAttributes(String[] attributes) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setAttribute(Attribute attribute) throws AttributeNotFoundException,
        InvalidAttributeValueException, MBeanException, ReflectionException {
        throw new InvalidAttributeValueException("Cannot set attribute: " + attribute);
    }

    @Override
    public AttributeList setAttributes(AttributeList attributes) {
        // TODO Auto-generated method stub
        return null;
    }

}
