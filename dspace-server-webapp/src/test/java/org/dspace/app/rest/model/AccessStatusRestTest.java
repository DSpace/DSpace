/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.dspace.access.status.DefaultAccessStatusHelper;
import org.junit.Before;
import org.junit.Test;

/**
 * Test the AccessStatusRestTest class
 */
public class AccessStatusRestTest {

    AccessStatusRest accessStatusRest;

    @Before
    public void setUp() throws Exception {
        accessStatusRest = new AccessStatusRest();
    }

    @Test
    public void testAccessStatusIsNullBeforeStatusSet() throws Exception {
        assertNull(accessStatusRest.getStatus());
    }

    @Test
    public void testAccessStatusIsNotNullAfterStatusSet() throws Exception {
        accessStatusRest.setStatus(DefaultAccessStatusHelper.UNKNOWN);
        assertNotNull(accessStatusRest.getStatus());
    }
}
