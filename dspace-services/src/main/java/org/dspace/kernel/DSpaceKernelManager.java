/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.kernel;

import org.dspace.servicemanager.DSpaceKernel;

/**
 * Allows the DSpace kernel to be accessed if desired.
 * 
 * @author Aaron Zeckoski (azeckoski @ gmail.com)
 */
public final class DSpaceKernelManager {

    private static DSpaceKernel defaultKernel = null;
    
    public static void setDefaultKernel(DSpaceKernel kernel) {
        defaultKernel = kernel;
    }

    /**
     * Get the kernel.  This will be a single instance for the JVM, but
     * the method will retrieve the same instance regardless of this 
     * object instance.
     *
     * @return the DSpace kernel
     * @throws IllegalStateException if the kernel is not available
     */
    public DSpaceKernel getKernel() {
        if (defaultKernel == null) {
            throw new IllegalStateException("The DSpace kernel is not started yet, please start it before attempting to use it");
        }

        return defaultKernel;
    }
}
