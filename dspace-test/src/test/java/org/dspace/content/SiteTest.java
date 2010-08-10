/*
 * SiteTest.java
 *
 * Copyright (c) 2002-2009, The DSpace Foundation.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * - Neither the name of the DSpace Foundation nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
 */

package org.dspace.content;

import org.dspace.AbstractUnitTest;
import org.dspace.core.ConfigurationManager;
import java.sql.SQLException;
import org.dspace.core.Constants;
import org.junit.*;
import static org.hamcrest.CoreMatchers.*;
import org.apache.log4j.Logger;
import static org.junit.Assert.*;

/**
 * Unit Tests for class Site
 * @author pvillega
 */
public class SiteTest extends AbstractUnitTest
{

    /** log4j category */
    private static final Logger log = Logger.getLogger(SiteTest.class);

    /**
     * Site instance for the tests
     */
    private Site s;

    /**
     * This method will be run before every test as per @Before. It will
     * initialize resources required for the tests.
     *
     * Other methods can be annotated with @Before here or in subclasses
     * but no execution order is guaranteed
     */
    @Before
    @Override
    public void init()
    {
        super.init();
         try
        {
            //we have to create a new community in the database
            context.turnOffAuthorisationSystem();
            int id = 0;
            this.s = (Site) Site.find(context, id);          
            //we need to commit the changes so we don't block the table for testing
            context.restoreAuthSystemState();
            context.commit();
        }
        catch (SQLException ex)
        {
            log.error("SQL Error in init", ex);
            fail("SQL Error in init");
        }
    }

    /**
     * This method will be run after every test as per @After. It will
     * clean resources initialized by the @Before methods.
     *
     * Other methods can be annotated with @After here or in subclasses
     * but no execution order is guaranteed
     */
    @After
    @Override
    public void destroy()
    {
        s = null;
        super.destroy();
    }

    /**
     * Test of getType method, of class Site.
     */
    @Test
    public void testGetType()
    {
        assertThat("testGetType 0", s.getType(), equalTo(Constants.SITE));
    }

    /**
     * Test of getID method, of class Site.
     */
    @Test
    public void testGetID() 
    {
        assertTrue("testGetID 0", s.getID() == Site.SITE_ID);
    }

    /**
     * Test of getHandle method, of class Site.
     */
    @Test
    public void testGetHandle()
    {
        assertThat("testGetHandle 0", s.getHandle(), equalTo(ConfigurationManager.getProperty("handle.prefix")
                +"/"+String.valueOf(Site.SITE_ID)));
    }

    /**
     * Test of getSiteHandle method, of class Site.
     */
    @Test
    public void testGetSiteHandle()
    {
        assertThat("testGetSiteHandle 0", s.getHandle(), equalTo(ConfigurationManager.getProperty("handle.prefix")
                +"/"+String.valueOf(Site.SITE_ID)));
    }

    /**
     * Test of find method, of class Site.
     */
    @Test
    public void testSiteFind() throws Exception
    {
        int id = 0;
        Site found = (Site)Site.find(context, id);
        assertThat("testSiteFind 0",found, notNullValue());
        assertThat("testSiteFind 1",found, equalTo(s));
    }

    /**
     * Test of delete method, of class Site.
     */
    @Test
    public void testDelete() throws Exception
    {
        //The method is empty
        s.delete();
    }

    /**
     * Test of update method, of class Site.
     */
    @Test
    public void testUpdate() throws Exception
    {
        //the method is empty
        s.update();
    }

    /**
     * Test of getName method, of class Site.
     */
    @Test
    public void testGetName() 
    {
        assertThat("testGetName 0",s.getName(), equalTo(ConfigurationManager.getProperty("dspace.name")));
    }

}