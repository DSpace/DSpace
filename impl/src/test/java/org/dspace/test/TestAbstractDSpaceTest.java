/**
 * Copyright (c) 2002-2010, DuraSpace.  All rights reserved
 * Licensed under the DuraSpace License.
 *
 * A copy of the DuraSpace License has been included in this
 * distribution and is available at: http://www.dspace.org/license
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
