/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.test;

import static org.junit.Assert.*;

import org.dspace.test.DSpaceAbstractKernelTest;
import org.junit.Test;

/**
 * A simple class to test that the abstract test case works
 * 
 * @author Aaron Zeckoski (azeckoski @ gmail.com)
 */
public class TestAbstractDSpaceTest extends DSpaceAbstractKernelTest {

    // nothing needed here

    @Test
    public void testThisWorks() {
        assertNotNull(kernel);
        assertTrue(kernel.isRunning());
    }
}
