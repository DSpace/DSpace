/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.servicemanager;

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

    /**
     * Creates or retrieves a DSpace Kernel with the given name.
     *
     * @return a DSpace Kernel
     * @throws IllegalStateException if the Kernel cannot be created
     */
    public static DSpaceKernel getKernel() {
        DSpaceKernel kernelImpl = DSpaceKernel.getInstance();
        log.info("Created new kernel: " + kernelImpl);

        DSpaceKernelManager.setDefaultKernel(kernelImpl);

        return kernelImpl;
    }
}
