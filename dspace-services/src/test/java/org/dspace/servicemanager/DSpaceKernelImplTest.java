/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.servicemanager;

import static org.junit.Assert.*;

import java.net.URL;
import java.net.URLClassLoader;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;


/**
 * Test the kernel can fire up correctly
 * 
 * @author Aaron Zeckoski (azeckoski @ gmail.com)
 */
public class DSpaceKernelImplTest {

    private DSpaceKernel kernelImpl;

    @Before
    public void init() {
        kernelImpl = DSpaceKernelInit.getKernel(); // checks for the existing kernel but does not init
    }

    @After
    public void destroy() {
        if (kernelImpl != null) {
            kernelImpl.destroy();
        }
        kernelImpl = null;
    }

    /**
     * Test method for {@link DSpaceKernel#start(String)}.
     */
    @Test
    public void testKernel() {
        kernelImpl.start(null);
        assertNotNull(kernelImpl);
        assertNotNull(kernelImpl);
        assertNotNull(this.kernelImpl.getConfigurationService());
        assertNotNull(this.kernelImpl.getServiceManager());
        assertNotNull(kernelImpl.getConfigurationService());
        assertNotNull(kernelImpl.getServiceManager());
        assertEquals(kernelImpl.getConfigurationService(), this.kernelImpl.getConfigurationService());
        assertEquals(kernelImpl.getServiceManager(), this.kernelImpl.getServiceManager());
        this.kernelImpl.destroy();
        
        kernelImpl = null;
    }

    @Test
    public void testMultipleKernels() {
        assertNotNull(kernelImpl);
        kernelImpl.start(null);
        assertNotNull(kernelImpl);
        assertNotNull(this.kernelImpl.getConfigurationService());
        assertNotNull(this.kernelImpl.getServiceManager());
        assertNotNull(kernelImpl.getConfigurationService());
        assertNotNull(kernelImpl.getServiceManager());
        assertEquals(kernelImpl.getConfigurationService(), this.kernelImpl.getConfigurationService());
        assertEquals(kernelImpl.getServiceManager(), this.kernelImpl.getServiceManager());
        
        DSpaceKernel kernel2 = DSpaceKernelInit.getKernel(); // checks for the existing kernelImpl but does not init
        kernel2.start(null);
        assertNotNull(kernel2);
        assertNotNull(kernel2.getConfigurationService());
        assertNotNull(kernel2.getServiceManager());
        assertNotNull(kernel2.getConfigurationService());
        assertNotNull(kernel2.getServiceManager());
        assertEquals(kernel2.getConfigurationService(), kernel2.getConfigurationService());
        assertEquals(kernel2.getServiceManager(), kernel2.getServiceManager());

        assertNotSame(kernelImpl, kernel2);
        assertNotSame(kernelImpl.getConfigurationService(), kernel2.getConfigurationService());
        assertNotSame(kernelImpl.getServiceManager(), kernel2.getServiceManager());

        kernel2.destroy();
        kernelImpl.destroy();
        kernelImpl = kernel2 = null;
    }

    @Test
    public void testClassLoaders() {
        ClassLoader current = Thread.currentThread().getContextClassLoader();
        ClassLoader cl1 = new URLClassLoader(new URL[0], current);
        cl1.getParent();
        // TODO
        
        cl1 = null;
        current = null;
    }

}
