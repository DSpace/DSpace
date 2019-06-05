/*
 * Copyright 2019 Indiana University.  All rights reserved.
 *
 * Mark H. Wood, IUPUI University Library, Jun 5, 2019
 */

/*
 * Copyright 2019 Indiana University.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
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
public class RequestItemRepositoryTest
{
    /**
     * Test of getDomainClass method, of class RequestItemRepository.
     */
    @Test
    public void testGetDomainClass()
    {
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
    public void testWrapResource()
    {
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
