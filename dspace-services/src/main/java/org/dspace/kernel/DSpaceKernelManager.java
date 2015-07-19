/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.kernel;

import java.lang.management.ManagementFactory;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.*;

/**
 * Allows the DSpace kernel to be accessed if desired.
 *
 * @author Aaron Zeckoski (azeckoski @ gmail.com)
 */
public final class DSpaceKernelManager {
    private static final Logger log = LoggerFactory.getLogger(DSpaceKernelManager.class);

    private static DSpaceKernel theKernel = null;

    /**
     * Get the kernel.  This will be a single instance for the JVM, but
     * the method will retrieve the same instance regardless of this
     * object instance.
     *
     * @param dspaceDir path to the DSpace home directory.
     * @return the DSpace kernel
     * @throws IllegalStateException if the kernel is not available
     */
    public static DSpaceKernel getKernel(String dspaceDir) {
        if (null == theKernel)
        {
            theKernel = new DSpaceKernelImpl();
            log.info("Created new kernel: " + theKernel);
            ((DSpaceKernelImpl)theKernel).start(dspaceDir);
        }

        return theKernel;
    }

    /**
     * Get the kernel.  This will be a single instance for the JVM, but
     * the method will retrieve the same instance regardless of this
     * object instance.
     *
     * @return the DSpace kernel
     * @throws IllegalStateException if the kernel is not available
     */
    public static DSpaceKernel getKernel() { return getKernel(null); }

    /**
     * Static initialized random Kernel name.
     */
    private static final String KERNEL_NAME = UUID.randomUUID().toString();

    /**
     * Ensure that we have a name suitable for an mbean.
     * @return a proper mbean name based on the given name
     */
    public static String getMBeanName() {
        return DSpaceKernel.MBEAN_PREFIX + KERNEL_NAME + DSpaceKernel.MBEAN_SUFFIX;
    }

    /**
     * Register a new kernel MBean with the given name or fail
     * @param mBeanName the bean name to use
     * @param kernel the kernel bean to register
     * @throws IllegalStateException if the MBean cannot be registered
     */
    public static void registerMBean(String mBeanName, DSpaceKernel kernel) {
        String checkedMBeanName = DSpaceKernelManager.getMBeanName();
        synchronized (mBeanName) {
            MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
            try {
                ObjectName name = new ObjectName(checkedMBeanName);
                if (! mbs.isRegistered(name)) {
                    // register the MBean
                    mbs.registerMBean(kernel, name);
                    log.info("Registered new Kernel MBEAN: {} [{}]",
                            checkedMBeanName, kernel);
                }
            } catch (MalformedObjectNameException
                    | InstanceAlreadyExistsException
                    | MBeanRegistrationException
                    | NotCompliantMBeanException
                    | NullPointerException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    /**
     * Unregister an MBean if possible
     * @param mBeanName the bean name to use
     * @return true if the MBean was unregistered, false otherwise
     */
    public static boolean unregisterMBean(String mBeanName) {
        String checkedMBeanName = DSpaceKernelManager.getMBeanName();
        synchronized (mBeanName) {
            MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
            try {
                mbs.unregisterMBean(new ObjectName(checkedMBeanName));
                return true;
            }
            catch (InstanceNotFoundException ie) {
                //If this exception is thrown, the specified MBean is not currently registered
                //So, we'll ignore the error and return true
                return true;
            }
            catch (MalformedObjectNameException | MBeanRegistrationException e) {
                //log this issue as a System Warning. Also log the underlying error message.
                log.warn("Failed to unregister the MBean: " + checkedMBeanName, e);
                return false;
            }
        }
    }
}
