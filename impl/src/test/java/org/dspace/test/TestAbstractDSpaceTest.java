/**
 * $Id: TestAbstractDSpaceTest.java 3540 2009-03-09 12:37:46Z azeckoski $
 * $URL: https://scm.dspace.org/svn/repo/dspace2/core/trunk/impl/src/test/java/org/dspace/test/TestAbstractDSpaceTest.java $
 * TestAbstractDSpaceTest.java - DSpace2 - Oct 21, 2008 4:26:40 PM - azeckoski
 **************************************************************************
 * Copyright (c) 2002-2009, The Duraspace Foundation.  All rights reserved.
 * Licensed under the Duraspace Foundation License.
 * 
 * A copy of the Duraspace License has been included in this
 * distribution and is available at: http://scm.dspace.org/svn/repo/licenses/LICENSE.txt
 *
 * 
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
