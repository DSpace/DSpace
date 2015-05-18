/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.servicemanager;

import static org.junit.Assert.*;

import org.dspace.kernel.DSpaceKernel;
import org.dspace.kernel.DSpaceKernelManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;


/**
 * Testing the kernel manager
 * 
 * @author Aaron Zeckoski (azeckoski @ gmail.com)
 */
public class DSpaceKernelManagerTest {

    DSpaceKernelManager kernelManager;
    DSpaceKernelImpl kernelImpl;

    @Before
    public void init() {
        kernelImpl = DSpaceKernelInit.getKernel(null);
        kernelImpl.start(); // init the kernel
        kernelManager = new DSpaceKernelManager();
    }

    @After
    public void destroy() {
        if (kernelImpl != null) {
            // cleanup the kernel
            kernelImpl.stop();
            kernelImpl.destroy();
        }
        kernelImpl = null;
        kernelManager = null;
    }

    /**
     * Test method for {@link org.dspace.kernel.DSpaceKernelManager#getKernel()}.
     */
    @Test
    public void testGetKernel() {
        DSpaceKernel kernel = kernelManager.getKernel();
        assertNotNull(kernel);
        DSpaceKernel k2 = kernelManager.getKernel();
        assertNotNull(k2);
        assertEquals(kernel, k2);
        
        kernel = k2 = null;
    }

}
