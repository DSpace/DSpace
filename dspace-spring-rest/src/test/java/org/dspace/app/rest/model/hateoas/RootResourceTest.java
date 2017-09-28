/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model.hateoas;

import org.dspace.app.rest.model.RootRest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;

/**
 * Created by raf on 26/09/2017.
 */
public class RootResourceTest {

    private RootRest rootRest;

    @Before
    public void setUp() throws Exception{
        rootRest = new RootRest();
    }

    @Test
    public void testConstructorWithNullStillMakesObject() throws Exception{
        RootResource rootResource = new RootResource(null);
        assertNotNull(rootResource);
    }

    @Test
    public void testConstructorWithNullDataIsNull() throws Exception{
        RootResource rootResource = new RootResource(null);
        assertNull(rootResource.getData());
    }

    @Test
    public void testConstructorAndGetterWithProperDataAndObjectNotNull() throws Exception{
        RootResource rootResource = new RootResource(rootRest);
        assertNotNull(rootResource);
        assertNotNull(rootResource.getData());
    }

    @Test
    public void testConstructorAndGetterWithProperDataAndProperDataReturned() throws Exception{
        RootResource rootResource = new RootResource(rootRest);
        assertEquals(rootRest, rootResource.getData());
    }
}
