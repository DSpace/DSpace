/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model.hateoas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.dspace.app.rest.model.RootRest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * This class' purpose is to test the RootResource class
 */
public class RootResourceTest {

    private RootRest rootRest;

    @BeforeEach
    public void setUp() throws Exception {
        rootRest = new RootRest();
    }

    @Test
    public void testConstructorWithNullThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            RootResource rootResource = new RootResource(null);
        });
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
