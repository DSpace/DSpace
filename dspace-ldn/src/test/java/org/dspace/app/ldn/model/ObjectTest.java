/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.ldn.model;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 *
 */
public class ObjectTest {

    @Test
    public void testObject() {
        Object object = new Object();

        object.setTitle("Test");
        assertEquals("Test", object.getTitle());
    }

}
