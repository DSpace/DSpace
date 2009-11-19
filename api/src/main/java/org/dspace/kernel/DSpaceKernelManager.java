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
package org.dspace.kernel;

import java.lang.management.ManagementFactory;
import java.util.UUID;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;


/**
 * Allows the DSpace kernel to be accessed if desired
 * 
 * @author Aaron Zeckoski (azeckoski @ gmail.com)
 */
public class DSpaceKernelManager {

    /**
     * The current kernel.
     */
    private DSpaceKernel kernel;

    
    /**
     * A lock on the kernel to handle multiple threads getting the first item.
     */
    private Object lock = new Object();

    /**
     * Get the kernel, this will be a single instance for the JVM, but the method will retrieve
     * the same instance regardless of this object instance.
     *
     * @return the DSpace kernel
     * @throws IllegalStateException if the kernel is not available
     */
    public DSpaceKernel getKernel() {
        return getKernel(null);
    }

    /**
     * Get the kernel, this will be a single instance for the JVM, but the method will retrieve
     * the same instance regardless of this object instance.
     *
     * @param name this is the name of this kernel instance, if you do not know what this is then use null
     * @return the DSpace kernel
     * @throws IllegalStateException if the kernel is not available or not running
     */
    public DSpaceKernel getKernel(String name) {
        if (kernel == null) {
            name = checkName(name);
            synchronized (lock) {
                MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
                try {
                    ObjectName kernelName = new ObjectName(name);
                    kernel = (DSpaceKernel) mbs.invoke(kernelName, "getManagedBean", null, null);
                    if (! kernel.isRunning()) {
                        throw new IllegalStateException("The DSpace kernel is not started yet, please start it before attempting to use it");
                    }
                } catch (InstanceNotFoundException e) {
                    throw new IllegalStateException(e);
                } catch (MBeanException e) {
                    throw new IllegalStateException(e);
                } catch (ReflectionException e) {
                    throw new IllegalStateException(e);
                } catch (MalformedObjectNameException e) {
                    throw new IllegalStateException(e);
                } catch (NullPointerException e) {
                    throw new IllegalStateException(e);
                }
            }
        }
        return kernel;
    }

    /**
     * Static initialized random default Kernel name
     */
    private static String defaultKernelName = UUID.randomUUID().toString();
    
    /**
     * @param name the name for the kernel
     * @return a proper mbean name based on the given name
     */
    public static String checkName(String name) {
        String mbeanName = name;
        if (name == null || "".equals(name)) {
            mbeanName = DSpaceKernel.MBEAN_PREFIX + defaultKernelName + DSpaceKernel.MBEAN_SUFFIX;
        } else {
            if (! name.startsWith(DSpaceKernel.MBEAN_PREFIX)) {
                mbeanName = DSpaceKernel.MBEAN_PREFIX + name + DSpaceKernel.MBEAN_SUFFIX;
            }
        }
        return mbeanName;
    }

}
