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
package org.dspace.utils.servicemanager;

import static org.junit.Assert.*;

import org.dspace.utils.servicemanager.ProviderHolder;
import org.dspace.utils.servicemanager.ProviderNotFoundException;
import org.junit.Test;

/**
 * Tests the ability of a provider holder to release a reference correctly
 * 
 * @author Aaron Zeckoski (azeckoski @ gmail.com)
 */
public class ProviderHolderTest {

    public class Thing {
        public String stuff;
    }

    @Test
    public void testHolder() {
        ProviderHolder<Thing> holder = new ProviderHolder<Thing>();
        assertNull(holder.getProvider());

        Thing t = new Thing();
        holder.setProvider( t );

        Thing t2 = holder.getProvider();
        assertNotNull(t2);
        assertEquals(t, t2);
    }

    @Test
    public void testHolderRelease() {
        ProviderHolder<Thing> holder = new ProviderHolder<Thing>();
        Thing t = new Thing();
        holder.setProvider( t );

        Thing t2 = holder.getProvider();
        assertNotNull(t2);
        assertEquals(t, t2);

        // trash the references
        t = null;
        t2 = null;

        System.gc(); // yuck but it works

        Thing t3 = holder.getProvider();
        assertNull(t3);
    }

    @Test
    public void testHolderException() {
        ProviderHolder<Thing> holder = new ProviderHolder<Thing>();
        try {
            holder.getProviderOrFail();
            fail("Should have died");
        } catch (ProviderNotFoundException e) {
            assertNotNull(e.getMessage());
        }

        Thing t = new Thing();
        holder.setProvider( t );

        Thing t2 = holder.getProviderOrFail();
        assertNotNull(t2);
        assertEquals(t, t2);
    }

    @Test
    public void testHolderHashEqualsString() {
        ProviderHolder<Thing> holder = new ProviderHolder<Thing>();
        assertNotNull( holder.hashCode() );
        assertFalse( holder.equals(null) );
        assertNotNull( holder.toString() );

        holder.setProvider( new Thing() );
        assertNotNull( holder.hashCode() );
        assertFalse( holder.equals(null) );
        assertNotNull( holder.toString() );
    }
    
}
