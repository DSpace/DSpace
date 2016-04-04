package org.dspace.content.authority;

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.AbstractDSpaceObjectTest;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.core.Constants;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.sql.SQLException;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.*;

/**
 * Created by mdiggory
 */
public class ConceptTest extends AbstractDSpaceObjectTest
{

    /** log4j category */
    private static final Logger log = Logger.getLogger(ConceptTest.class);

    /**
     * Instance for the tests
     */
    private Concept c;


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
            //we have to create a new concept in the database
            context.turnOffAuthorisationSystem();
            this.c = Concept.create(context);
            this.dspaceObject = c;
            //we need to commit the changes so we don't block the table for testing
            context.restoreAuthSystemState();
            context.commit();
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
        c = null;
        super.destroy();
    }

    /**
     * Test of getID method
     */
    @Test
    @Override
    public void testGetID()
    {
        assertTrue("testGetID 0", c.getID() >= 0);
    }

    /**
     * Test of getHandle method
     */
    @Test
    @Override
    public void testGetHandle()
    {
        // Concepts don't have handles, but this method is required due to subclassing
    }

    /**
     * Test of getType method
     */
    @Test
    @Override
    public void testGetType()
    {
        assertThat("testGetType 0", c.getType(), equalTo(Constants.CONCEPT));
    }


    /**
     * Test of getName method, of class Community.
     */
    @Test
    @Override
    public void testGetName()
    {
        assertThat("testGetName", c.getName(), notNullValue());

    }


    /**
     * Test of getAdminObject method
     */
    @Test
    @Override
    public void testGetAdminObject() throws SQLException
    {
        // This method is required due to subclassing
    }

    /**
     * Test of getParentObject method
     */
    @Test
    @Override
    public void testGetParentObject() throws SQLException
    {
        try
        {
            //default has no parent
            assertThat("testGetParentObject 0", c.getParentObject(), nullValue());

            context.turnOffAuthorisationSystem();
            Scheme parent = Scheme.create(context);
            parent.addConcept(context, c);

            context.restoreAuthSystemState();
            assertThat("testGetParentObject 1", c.getParentObject(), notNullValue());
            assertThat("testGetParentObject 2", (Scheme)c.getParentObject(), equalTo(parent));
        }
        catch(AuthorizeException ex)
        {
            fail("Authorize exception catched");
        }
    }


}
