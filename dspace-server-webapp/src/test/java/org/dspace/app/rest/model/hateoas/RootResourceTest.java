/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model.hateoas;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.dspace.app.rest.model.RootRest;
import org.junit.Before;
import org.junit.Test;

/**
 * This class' purpose is to test the RootResource class
 */
public class RootResourceTest {

    private RootRest rootRest;

    @Before
    public void setUp() throws Exception {
        rootRest = new RootRest();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWithNullThrowsException() throws Exception {
        RootResource rootResource = new RootResource(null);
    }


    @Test
    public void testConstructorAndGetterWithProperDataAndObjectNotNull() throws Exception {
        RootResource rootResource = new RootResource(rootRest);
        assertNotNull(rootResource);
        assertNotNull(rootResource.getContent());
    }

    @Test
    public void testConstructorAndGetterWithProperDataAndProperDataReturned() throws Exception {
        RootResource rootResource = new RootResource(rootRest);
        assertEquals(rootRest, rootResource.getContent());
    }
}
