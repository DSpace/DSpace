/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.kernel;

import java.util.Date;

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

import org.dspace.servicemanager.DSpaceServiceManager;
import org.dspace.servicemanager.ServiceManagerSystem;
import org.dspace.servicemanager.config.DSpaceConfigurationService;
import org.dspace.services.ConfigurationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is the kernel implementation which starts up the core of DSpace,
 * registers the mbean, and initializes the DSpace object.
 * It also loads up the configuration and sets a JRE shutdown hook.
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
public final class DSpaceKernelImpl
        implements DSpaceKernel, DynamicMBean, CommonLifecycle<DSpaceKernel> {

    private static final Logger log = LoggerFactory.getLogger(DSpaceKernelImpl.class);

    /**
     * Creates a DSpace Kernel, does not do any checks though.
     * Do not call this; use {@link DSpaceKernelManager#getKernel()}.
     */
    protected DSpaceKernelImpl() {
        this.mBeanName = DSpaceKernelManager.getMBeanName();
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
                    @Override
                    public void run() {
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
            return;
        }

        synchronized (lock) {
            lastLoadDate = new Date();
            long startTime = System.currentTimeMillis();

            // Register the MBean.
            DSpaceKernelManager.registerMBean(mBeanName, kernel);

            // create the configuration service and get the configuration
            DSpaceConfigurationService dsConfigService = new DSpaceConfigurationService(dspaceHome);
            configurationService = dsConfigService;

            // startup the service manager
            serviceManagerSystem = new DSpaceServiceManager(dsConfigService);
            serviceManagerSystem.startup();

            loadTime = System.currentTimeMillis() - startTime;

            kernel = this;
            running = true;
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
        if (! running) {
            //log.warn("Kernel ("+this+") is already stopped");
            return;
        }

        synchronized (lock) {
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
        log.info("DSpace kernel shutdown completed and unregistered MBean: {}", mBeanName);
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
        }
    }

    /**
     * Called from within the shutdown thread.
     */
    protected void doDestroy() {
        if (! this.destroyed) {
            destroy();
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
        StringBuilder rv = new StringBuilder(256);

        rv.append("DSpaceKernel:").append(mBeanName)
                .append(":lastLoad=").append(lastLoadDate)
                .append(":loadTime=").append(loadTime)
                .append(":running=").append(running)
                .append(":kernel=");

        if (kernel == null)
            rv.append("null");
        else
            rv.append(kernel.getClass().getName())
                    .append('@').append(kernel.getClass().getClassLoader())
                    .append(':').append(super.toString());

        return rv.toString();
    }

    // MBEAN methods

    private Date lastLoadDate;
    /**
     * Time that this kernel was started, as a java.util.Date.
     * @see getLoadTime().
     */
    public Date getLastLoadDate() {
        return new Date(lastLoadDate.getTime());
    }

    private long loadTime;
    /**
     * Time that this kernel was started, as seconds since the epoch.
     * @see getLastLoadDate().
     */
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
        "getMethod=getLoadTime" });

        ModelMBeanAttributeInfo[] mmbai = new ModelMBeanAttributeInfo[2];
        mmbai[0] = new ModelMBeanAttributeInfo("LastLoadDate", "java.util.Date", "Last Load Date",
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
