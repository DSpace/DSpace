/**
 * Copyright (c) 2002-2010, DuraSpace.  All rights reserved
 * Licensed under the DuraSpace License.
 *
 * A copy of the DuraSpace License has been included in this
 * distribution and is available at: http://www.dspace.org/license
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
    }

}
