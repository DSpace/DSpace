/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.servicemanager;

import static org.dspace.servicemanager.config.DSpaceConfigurationService.DSPACE_HOME;
import static org.junit.Assert.*;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Paths;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;


/**
 * Test the kernel can fire up correctly
 * 
 * @author Aaron Zeckoski (azeckoski @ gmail.com)
 */
public class DSpaceKernelTest {

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
        kernelImpl.start();
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
    public void testClassLoaders() {
        ClassLoader current = Thread.currentThread().getContextClassLoader();
        ClassLoader cl1 = new URLClassLoader(new URL[0], current);
        cl1.getParent();
        // TODO
        
        cl1 = null;
        current = null;
    }

}
