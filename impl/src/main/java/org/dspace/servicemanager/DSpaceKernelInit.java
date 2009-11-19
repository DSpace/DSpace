/**
 * $Id$
 * $URL$
 * *************************************************************************
 * Copyright (c) 2002-2009, DuraSpace.  All rights reserved
 * Licensed under the DuraSpace License.
 *
 * A copy of the DuraSpace License has been included in this
 * distribution and is available at: http://scm.dspace.org/svn/repo/licenses/LICENSE.txt
 */
package org.dspace.servicemanager;

import java.lang.management.ManagementFactory;

import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import javax.management.ReflectionException;

import org.dspace.kernel.DSpaceKernel;
import org.dspace.kernel.DSpaceKernelManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class simplifies the handling of MBean lookup, registration, etc. of the DSpace Kernel MBean.
 * This class has all static methods
 * 
 * @author Aaron Zeckoski (azeckoski @ gmail.com)
 */
public class DSpaceKernelInit {

    private static Logger log = LoggerFactory.getLogger(DSpaceKernelInit.class);
    
    private static Object staticLock = new Object();

    /**
     * Creates or retrieves the existing DSpace Kernel with the given name
     * @return a DSpace Kernel
     * @throws IllegalStateException if the Kernel cannot be created
     */
    public static DSpaceKernelImpl getKernel(String name) {
        String mBeanName = DSpaceKernelManager.checkName(name);
        synchronized (staticLock) {
            DSpaceKernelImpl kernel = null;
            try {
                kernel = (DSpaceKernelImpl) getMBean(mBeanName);
            } catch (IllegalStateException e) {
                kernel = null;
            }
            if (kernel == null) {
                DSpaceKernelImpl kernelImpl = new DSpaceKernelImpl(mBeanName);
                log.info("Created new kernel: " + kernelImpl);
                // register the bean
                String beanName = kernelImpl.getMBeanName();
                register(beanName, kernelImpl);
                kernel = kernelImpl;
            }
            return kernel;
        }
    }

    /**
     * Register a new kernel MBean with the given name or fail
     * @param mBeanName the bean name to use
     * @param kernel the kernel bean to register
     * @throws IllegalStateException if the MBean cannot be registered
     */
    public static void register(String mBeanName, DSpaceKernel kernel) {
        mBeanName = DSpaceKernelManager.checkName(mBeanName);
        synchronized (staticLock) {
            MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
            try {
                ObjectName name = new ObjectName(mBeanName);
                if (! mbs.isRegistered(name)) {
                    // register the MBean
                    mbs.registerMBean(kernel, name);
                    log.info("Registered new Kernel MBEAN: " + mBeanName + " ["+kernel+"]");
                }
            } catch (MalformedObjectNameException e) {
                throw new IllegalStateException(e);
            } catch (InstanceAlreadyExistsException e) {
                throw new IllegalStateException(e);
            } catch (MBeanRegistrationException e) {
                throw new IllegalStateException(e);
            } catch (NotCompliantMBeanException e) {
                throw new IllegalStateException(e);
            } catch (NullPointerException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    /**
     * Unregister an MBean if possible
     * @param mBeanName the bean name to use
     * @return true if the MBean was unregistered, false otherwise
     */
    public static boolean unregister(String mBeanName) {
        mBeanName = DSpaceKernelManager.checkName(mBeanName);
        synchronized (staticLock) {
            MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
            try {
                ObjectName name = new ObjectName(mBeanName);
                mbs.unregisterMBean(name);
                return true;
            } catch (Exception e) {
                log.error("WARN Failed to unregister the MBean: " + mBeanName);
                return false;
            }
        }
    }

    /**
     * Check if an MBean is registered already
     * @param mBeanName the bean name to check
     * @return true if registered OR false if not
     * @throws IllegalStateException if a failure occurs
     */
    public static boolean isRegistered(String mBeanName) {
        mBeanName = DSpaceKernelManager.checkName(mBeanName);
        boolean registered = false;
        synchronized (staticLock) {
            // register the mbean
            MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
            try {
                ObjectName name = new ObjectName(mBeanName);
                if (mbs.isRegistered(name)) {
                    registered = true;
                }
            } catch (MalformedObjectNameException e) {
                throw new IllegalStateException(e);
            } catch (NullPointerException e) {
                throw new IllegalStateException(e);
            }
        }
        return registered;
    }

    /**
     * Gets the Kernel MBean if possible
     * @param mBeanName the bean name to use
     * @return the Kernel if found OR null if not found
     * @throws IllegalStateException if a failure occurs
     */
    public static DSpaceKernel getMBean(String mBeanName) {
        mBeanName = DSpaceKernelManager.checkName(mBeanName);
        DSpaceKernel kernel = null;
        synchronized (staticLock) {
            MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
            try {
                ObjectName kernelName = new ObjectName(mBeanName);
                kernel = (DSpaceKernel) mbs.invoke(kernelName, "getManagedBean", null, null);
                kernel.isRunning(); // throws exception if the kernel is not running
            } catch (InstanceNotFoundException e) {
                kernel = null;
            } catch (MBeanException e) {
                kernel = null;
            } catch (ReflectionException e) {
                kernel = null;
            } catch (MalformedObjectNameException e) {
                kernel = null;
            } catch (NullPointerException e) {
                kernel = null;
            }
        }
        return kernel;
    }

}
