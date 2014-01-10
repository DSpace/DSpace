/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.dspace.kernel.DSpaceKernel;
import org.dspace.kernel.DSpaceKernelManager;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;


/**
 * This is an abstract class which makes it easier to test things that use the DSpace Kernel,
 * this will start and stop the kernel at the beginning of the group of tests that are 
 * in the junit test class which extends this
 * 
 * @author Aaron Zeckoski (azeckoski @ gmail.com)
 */
public abstract class DSpaceAbstractKernelTest extends DSpaceAbstractTest {

    @BeforeClass
    public static void initKernel() {
        _initializeKernel();
        assertNotNull(kernelImpl);
        assertTrue(kernelImpl.isRunning());
        assertNotNull(kernel);
    }

    @AfterClass
    public static void destroyKernel() {
        _destroyKernel();
    }

    /**
     * Test method for {@link org.dspace.kernel.DSpaceKernelManager#getKernel()}.
     */
    @Test
    public void testKernelIsInitializedAndWorking() {
        assertNotNull(kernel);
        assertTrue(kernel.isRunning());
        DSpaceKernel k2 = new DSpaceKernelManager().getKernel();
        assertNotNull(k2);
        assertEquals(kernel, k2);
    }

}
