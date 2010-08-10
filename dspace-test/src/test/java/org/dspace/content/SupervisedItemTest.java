/*
 * SupervisedItemTest.java
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

import org.dspace.eperson.Supervisor;
import java.io.IOException;
import java.sql.SQLException;
import org.dspace.authorize.AuthorizeException;
import org.dspace.AbstractUnitTest;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.junit.*;
import static org.hamcrest.CoreMatchers.*;
import org.apache.log4j.Logger;
import static org.junit.Assert.*;

/**
 *
 * @author pvillega
 */
public class SupervisedItemTest extends AbstractUnitTest
{

    /** log4j category */
    private static final Logger log = Logger.getLogger(SupervisedItemTest.class);

    /**
     * SupervisedItem instance for the tests
     */
    private SupervisedItem si;

    /**
     * Group instance for the tests
     */
    private Group gr;

    /**
     * WorkspaceItem instance for the tests
     */
    private WorkspaceItem wi;

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
            Collection col = Collection.create(context);
            wi = WorkspaceItem.create(context, col, false);
            gr = Group.create(context);
            gr.addMember(context.getCurrentUser());
            gr.update();

            //set a supervisor as editor
            Supervisor.add(context, gr.getID(), wi.getID(), 1);

            SupervisedItem[] found = SupervisedItem.getAll(context);
            for(SupervisedItem sia: found)
            {
                if(sia.getID() == wi.getID())
                {
                    si = sia;
                }
            }

            //we need to commit the changes so we don't block the table for testing
            context.restoreAuthSystemState();
            context.commit();
        }
        catch (IOException ex) {
            log.error("IO Error in init", ex);
            fail("IO Error in init");
        }
        catch (AuthorizeException ex)
        {
            log.error("Authorization Error in init", ex);
            fail("Authorization Error in init");
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
        si = null;
        wi = null;
        gr = null;
        super.destroy();
    }

    /**
     * Test of getAll method, of class SupervisedItem.
     */
    @Test
    public void testGetAll() throws Exception
    {
        SupervisedItem[] found = SupervisedItem.getAll(context);
        assertThat("testGetAll 0", found, notNullValue());
        assertTrue("testGetAll 1", found.length >= 1);

        boolean added = false;
        for(SupervisedItem sia: found)
        {
            if(sia.equals(si))
            {
                added = true;
            }
        }
        assertTrue("testGetAll 2",added);
    }

    /**
     * Test of getSupervisorGroups method, of class SupervisedItem.
     */
    @Test
    public void testGetSupervisorGroups_Context_int() throws Exception
    {
        Group[] found = si.getSupervisorGroups(context, wi.getID());
        assertThat("testGetSupervisorGroups_Context_int 0", found, notNullValue());
        assertTrue("testGetSupervisorGroups_Context_int 1", found.length == 1);
        assertThat("testGetSupervisorGroups_Context_int 2", found[0].getID(), equalTo(gr.getID()));
    }

    /**
     * Test of getSupervisorGroups method, of class SupervisedItem.
     */
    @Test
    public void testGetSupervisorGroups_0args() throws Exception 
    {
        Group[] found = si.getSupervisorGroups();
        assertThat("testGetSupervisorGroups_0args 0", found, notNullValue());
        assertTrue("testGetSupervisorGroups_0args 1", found.length == 1);

        boolean added = false;
        for(Group g: found)
        {
            if(g.equals(gr))
            {
                added = true;
            }
        }
        assertTrue("testGetSupervisorGroups_0args 2",added);
    }

    /**
     * Test of findbyEPerson method, of class SupervisedItem.
     */
    @Test
    public void testFindbyEPerson() throws Exception
    {
        context.turnOffAuthorisationSystem();
        SupervisedItem[] found = SupervisedItem.findbyEPerson(context, EPerson.create(context));
        assertThat("testFindbyEPerson 0", found, notNullValue());
        assertTrue("testFindbyEPerson 1", found.length == 0);

        found = SupervisedItem.findbyEPerson(context, context.getCurrentUser());
        assertThat("testFindbyEPerson 2", found, notNullValue());        
        assertTrue("testFindbyEPerson 3", found.length >= 1);

        boolean added = false;
        for(SupervisedItem sia: found)
        {
            if(sia.equals(si))
            {
                added = true;
            }
        }
        assertTrue("testFindbyEPerson 4",added);

        context.restoreAuthSystemState();
    }

}