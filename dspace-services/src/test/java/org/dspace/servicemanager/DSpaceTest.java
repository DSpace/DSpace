/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.servicemanager;

import static org.junit.Assert.*;

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
        DSpaceKernel kernel = DSpaceKernel.getInstance();
        kernel.start();

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
        kernel.destroy();
    }
}
