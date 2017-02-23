/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.servicemanager;

/**
 * Left due to API compatibility.
 * 
 * @author Aaron Zeckoski (azeckoski @ gmail.com)
 */
public class DSpaceKernelInit {

    /**
     * Executes {@link org.dspace.servicemanager.DSpaceKernel#getInstance DSpaceKernel.getInstance()}
     * Left due to API compatibility
     *
     * @return a DSpace Kernel
     */
    public static DSpaceKernel getKernel() {
        return DSpaceKernel.getInstance();
    }
}
