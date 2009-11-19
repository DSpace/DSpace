/**
 * $Id$
 * $URL$
 * *************************************************************************
 * Copyright (c) 2002-2009, DuraSpace.  All rights reserved
 * Licensed under the DuraSpace License.
 *
 * A copy of the DuraSpace License has been included in this
 * distribution and is available at: http://scm.dspace.org/svn/repo/licenses/LICENSE.txt
 */
package org.dspace.servicemanager;

import static org.junit.Assert.*;

import java.net.URL;
import java.net.URLClassLoader;

import org.dspace.kernel.DSpaceKernel;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;


/**
 * Test the kernel can fire up correctly
 * 
 * @author Aaron Zeckoski (azeckoski @ gmail.com)
 */
public class DSpaceKernelImplTest {

    DSpaceKernelImpl kernelImpl;

    @Before
    public void init() {
        kernelImpl = DSpaceKernelInit.getKernel(null); // checks for the existing kernel but does not init
    }

    @After
    public void destroy() {
        if (kernelImpl != null) {
            kernelImpl.destroy();
        }
        kernelImpl = null;
    }

    /**
     * Test method for {@link org.dspace.servicemanager.DSpaceKernelImpl#init()}.
     */
    @Test
    public void testKernel() {
        kernelImpl.start();
        assertNotNull(kernelImpl);
        DSpaceKernel kernel = kernelImpl.getManagedBean();
        assertNotNull(kernel);
        assertNotNull(kernelImpl.getConfigurationService());
        assertNotNull(kernelImpl.getServiceManager());
        assertNotNull(kernel.getConfigurationService());
        assertNotNull(kernel.getServiceManager());
        assertEquals(kernel.getConfigurationService(), kernelImpl.getConfigurationService());
        assertEquals(kernel.getServiceManager(), kernelImpl.getServiceManager());
        kernelImpl.stop();
    }

    @Test
    public void testMultipleKernels() {
        assertNotNull(kernelImpl);
        kernelImpl.start();
        DSpaceKernel kernel = kernelImpl.getManagedBean();
        assertNotNull(kernel);
        assertNotNull(kernelImpl.getConfigurationService());
        assertNotNull(kernelImpl.getServiceManager());
        assertNotNull(kernel.getConfigurationService());
        assertNotNull(kernel.getServiceManager());
        assertEquals(kernel.getConfigurationService(), kernelImpl.getConfigurationService());
        assertEquals(kernel.getServiceManager(), kernelImpl.getServiceManager());
        
        DSpaceKernelImpl kernelImpl2 = DSpaceKernelInit.getKernel("AZ-kernel"); // checks for the existing kernel but does not init
        kernelImpl2.start();
        DSpaceKernel kernel2 = kernelImpl2.getManagedBean();
        assertNotNull(kernel2);
        assertNotNull(kernelImpl2.getConfigurationService());
        assertNotNull(kernelImpl2.getServiceManager());
        assertNotNull(kernel2.getConfigurationService());
        assertNotNull(kernel2.getServiceManager());
        assertEquals(kernel2.getConfigurationService(), kernelImpl2.getConfigurationService());
        assertEquals(kernel2.getServiceManager(), kernelImpl2.getServiceManager());

        assertNotSame(kernel, kernel2);
        assertNotSame(kernel.getConfigurationService(), kernel2.getConfigurationService());
        assertNotSame(kernel.getServiceManager(), kernel2.getServiceManager());

        kernelImpl2.stop();
        kernelImpl.stop();
    }

    @Test
    public void testClassLoaders() {
        ClassLoader current = Thread.currentThread().getContextClassLoader();
        ClassLoader cl1 = new URLClassLoader(new URL[0], current);
        cl1.getParent();
        // TODO
    }

}
