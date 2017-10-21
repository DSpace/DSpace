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
 * Left due to API compatibility.
 * 
 * @author Aaron Zeckoski (azeckoski @ gmail.com)
 */
public final class DSpaceKernelManager {

    /**
     * Executes {@link org.dspace.servicemanager.DSpaceKernel#getInstance DSpaceKernel.getInstance()}
     * Left due to API compatibility
     *
     * @return a DSpace Kernel
     */
    public DSpaceKernel getKernel() {
        return DSpaceKernel.getInstance();
    }
}
