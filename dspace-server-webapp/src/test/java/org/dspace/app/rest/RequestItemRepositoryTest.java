/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.app.rest;

import static org.junit.Assert.assertEquals;

import org.dspace.app.rest.model.RequestItemRest;
import org.dspace.app.rest.repository.RequestItemRepository;
import org.junit.Test;

/**
 *
 * @author Mark H. Wood <mwood@iupui.edu>
 */
public class RequestItemRepositoryTest {
    /**
     * Test of getDomainClass method, of class RequestItemRepository.
     */
    @Test
    public void testGetDomainClass() {
        System.out.println("getDomainClass");
        RequestItemRepository instance = new RequestItemRepository();
        Class instanceClass = instance.getDomainClass();
        assertEquals("Wrong domain class", RequestItemRest.class, instanceClass);
    }

    /**
     * Test of wrapResource method, of class RequestItemRepository.
     */
    /*
    @Test
    public void testWrapResource() {
    System.out.println("wrapResource");
    RequestItemRest model = null;
    String[] rels = null;
    RequestItemRepository instance = new RequestItemRepository();
    RequestItemResource expResult = null;
    RequestItemResource result = instance.wrapResource(model, rels);
    assertEquals(expResult, result);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
    }
     */
}
