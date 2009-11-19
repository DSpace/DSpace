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
package org.dspace.test;

import static org.junit.Assert.*;

import org.dspace.test.DSpaceAbstractRequestTest;
import org.junit.Test;

/**
 * A simple class to test that the abstract request test case works
 * 
 * @author Aaron Zeckoski (azeckoski @ gmail.com)
 */
public class TestAbstractDSpaceRequestTest extends DSpaceAbstractRequestTest {

    // nothing needed here

    @Test
    public void testThisWorks() {
        assertNotNull(kernel);
        assertTrue(kernel.isRunning());
        assertNotNull(getRequestService());
        assertNotNull(getRequestId());
        assertNotNull( getRequestService().getCurrentRequestId() );
        assertEquals(getRequestId(), getRequestService().getCurrentRequestId());
    }
}
