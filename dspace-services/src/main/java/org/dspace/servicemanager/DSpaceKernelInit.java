/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.servicemanager;

import org.dspace.kernel.DSpaceKernel;
import org.dspace.kernel.DSpaceKernelManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class simplifies the handling of lookup, registration, and 
 * access of a DSpace Kernel MBean.  This class has all static 
 * methods.
 * 
 * @author Aaron Zeckoski (azeckoski @ gmail.com)
 */
public class DSpaceKernelInit {

    private static Logger log = LoggerFactory.getLogger(DSpaceKernelInit.class);
    
    private static final Object staticLock = new Object();

    /**
     * Creates or retrieves a DSpace Kernel with the given name.
     *
     * @param name kernel name (or null for default kernel)
     * @return a DSpace Kernel
     * @throws IllegalStateException if the Kernel cannot be created
     */
    public static DSpaceKernelImpl getKernel(String name) {
        if (name != null) {
            try {
                DSpaceKernel kernel = new DSpaceKernelManager().getKernel(name);
                if (kernel != null) {
                    if (kernel instanceof DSpaceKernelImpl) {
                        return (DSpaceKernelImpl)kernel;
                    }

                    throw new IllegalStateException("Wrong DSpaceKernel implementation");
                }
            } catch (Exception e) {
                // Ignore exceptions here
            }
        }

        synchronized (staticLock) {
            DSpaceKernelImpl kernelImpl = new DSpaceKernelImpl(name);
            log.info("Created new kernel: " + kernelImpl);

            if (name != null) {
                DSpaceKernelManager.registerMBean(kernelImpl.getMBeanName(), kernelImpl);
            } else {
                DSpaceKernelManager.setDefaultKernel(kernelImpl);
            }

            return kernelImpl;
        }
    }
}
