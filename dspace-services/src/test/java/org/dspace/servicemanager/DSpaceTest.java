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
import org.dspace.utils.DSpace;
import org.junit.Test;

/**
 * Make sure the DSpace static cover works
 * 
 * @author Aaron Zeckoski (azeckoski @ gmail.com)
 */
public class DSpaceTest {

    @Test
    public void testDSpaceObject() {
        try {
            DSpace dspace = new DSpace();
            dspace.getServiceManager();
            fail("should have thrown exception");
        } catch (IllegalStateException e) {
            assertNotNull(e.getMessage());
        }

        DSpaceKernelImpl kernelImpl = DSpaceKernelInit.getKernel(null);
        kernelImpl.start(); // triggers the init
        DSpaceKernel kernel = new DSpaceKernelManager().getKernel();
        assertNotNull(kernel);
        assertEquals(kernel, kernelImpl);

        DSpace dspace = new DSpace();
        Object o = dspace.getServiceManager();
        assertNotNull(o);
        assertEquals(o, kernel.getServiceManager());

        // repeat a few times
        o = dspace.getServiceManager();
        assertNotNull(o);
        assertEquals(o, kernel.getServiceManager());

        o = dspace.getServiceManager();
        assertNotNull(o);
        assertEquals(o, kernel.getServiceManager());

        DSpace dspace2 = new DSpace();
        assertNotNull(dspace2.getServiceManager());
        assertEquals(dspace.getServiceManager(), dspace2.getServiceManager());

        // REPEAT
        kernel = new DSpaceKernelManager().getKernel();

        o = dspace.getServiceManager();
        assertNotNull(o);
        assertEquals(o, kernel.getServiceManager());

        // repeat a few times
        o = dspace.getServiceManager();
        assertNotNull(o);
        assertEquals(o, kernel.getServiceManager());

        o = dspace.getServiceManager();
        assertNotNull(o);
        assertEquals(o, kernel.getServiceManager());

        //trash the references
        kernelImpl.destroy();
        kernelImpl = null;
        kernel = null;
        dspace = null;
        dspace2 = null;
        o = null;
    }
    
/*********
    @Test
    public void testStaticCover() {
        try {
            DSpace.getServiceManager();
            fail("should have thrown exception");
        } catch (IllegalStateException e) {
            assertNotNull(e.getMessage());
        }

        DSpaceKernelImpl kernelImpl = DSpaceKernelInit.getKernel(null);
        kernelImpl.start(); // triggers the init
        DSpaceKernel kernel = new DSpaceKernelManager().getKernel();
        assertNotNull(kernel);
        assertEquals(kernel, kernelImpl);

        Object o = DSpace.getServiceManager();
        assertNotNull(o);
        assertEquals(o, kernel.getServiceManager());

        // repeat a few times
        o = DSpace.getServiceManager();
        assertNotNull(o);
        assertEquals(o, kernel.getServiceManager());

        o = DSpace.getServiceManager();
        assertNotNull(o);
        assertEquals(o, kernel.getServiceManager());

        // REPEAT
        kernel = new DSpaceKernelManager().getKernel(); // init the kernel

        o = DSpace.getServiceManager();
        assertNotNull(o);
        assertEquals(o, kernel.getServiceManager());

        // repeat a few times
        o = DSpace.getServiceManager();
        assertNotNull(o);
        assertEquals(o, kernel.getServiceManager());

        o = DSpace.getServiceManager();
        assertNotNull(o);
        assertEquals(o, kernel.getServiceManager());

        kernelImpl.destroy(); // cleanup the kernel

        try {
            DSpace.getServiceManager();
            fail("should have thrown exception");
        } catch (IllegalStateException e) {
            assertNotNull(e.getMessage());
        }

    }

    @Test
    public void testRestarts() {
        try {
            DSpace.getServiceManager();
            fail("should have thrown exception");
        } catch (IllegalStateException e) {
            assertNotNull(e.getMessage());
        }

        DSpaceKernelImpl kernelImpl = DSpaceKernelInit.getKernel(null);
        DSpaceKernelImpl kernelImpl2 = DSpaceKernelInit.getKernel(null);
        assertEquals(kernelImpl, kernelImpl2);
        kernelImpl2 = null;

        kernelImpl.start(); // triggers the init
        DSpaceKernel kernel = new DSpaceKernelManager().getKernel();
        assertNotNull(kernel);
        assertEquals(kernel, kernelImpl);

        Object o = DSpace.getServiceManager();
        assertNotNull(o);
        assertEquals(o, kernel.getServiceManager());

        kernelImpl.stop(); // stop the kernel

        try {
            DSpace.getServiceManager();
            fail("should have thrown exception");
        } catch (IllegalStateException e) {
            assertNotNull(e.getMessage());
        }

        kernelImpl.start(); // triggers the init
        kernel = new DSpaceKernelManager().getKernel();

        o = DSpace.getServiceManager();
        assertNotNull(o);
        assertEquals(o, kernel.getServiceManager());

        kernelImpl.stop(); // stop the kernel

        try {
            DSpace.getServiceManager();
            fail("should have thrown exception");
        } catch (IllegalStateException e) {
            assertNotNull(e.getMessage());
        }

        kernelImpl2 = DSpaceKernelInit.getKernel(null);
        // check it is the same
        assertEquals(kernelImpl, kernelImpl2);

        kernelImpl2.start(); // triggers the init
        DSpaceKernel kernel2 = new DSpaceKernelManager().getKernel();
        assertNotNull(kernel2);
        assertEquals(kernel2, kernelImpl2);

        assertEquals(kernel, kernel2);

        Object o2 = DSpace.getServiceManager();
        assertNotNull(o2);
        assertEquals(o2, kernel2.getServiceManager());

        // now try to startup the kernel again (should not start again)
        kernelImpl.start();

        assertEquals(kernelImpl2.getServiceManager(), kernelImpl.getServiceManager());

        kernelImpl2.destroy(); // cleanup the kernel
        kernelImpl.destroy(); // should not fail

        try {
            DSpace.getServiceManager();
            fail("should have thrown exception");
        } catch (IllegalStateException e) {
            assertNotNull(e.getMessage());
        }

    }
******/

}
