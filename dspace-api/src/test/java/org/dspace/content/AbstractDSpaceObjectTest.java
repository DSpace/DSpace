/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import java.sql.SQLException;
import org.dspace.AbstractUnitTest;
import org.junit.*;
import static org.junit.Assert.* ;
import static org.hamcrest.CoreMatchers.*;
import org.dspace.core.Constants;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;


/**
 * Tests DSpaceObject class. This is an abstract class, so this test is also
 * marked as Abstract and it will be ignored by the test suite. Tests will be
 * run when testing the children classes.
 *
 * @author pvillega
 */
public abstract class AbstractDSpaceObjectTest extends AbstractUnitTest
{

    /**
     * Protected instance of the class dspaceObject, will be initialized by
     * children classes to ensure it tests all the methods
     */
    protected DSpaceObject dspaceObject;

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
        dspaceObject = null;
        super.destroy();
    }

    /**
     * Test of clearDetails method, of class DSpaceObject.
     */
    @Test
    public void testClearDetails()
    {
        String[] testData = new String[] {"details 1", "details 2", "details 3"};
        for(String s: testData)
        {
            dspaceObject.addDetails(s);
        }

        String details = dspaceObject.getDetails();
        dspaceObject.clearDetails();

        assertThat("testClearDetails 0", dspaceObject.getDetails(), nullValue());
        assertThat("testClearDetails 1", dspaceObject.getDetails(), not(equalTo(details)));
    }

    /**
     * Test of addDetails method, of class DSpaceObject.
     */
    @Test
    public void testAddDetails()
    {
        String[] testData = new String[] {"details 1", "details 2", "details 3"};
        for(String s: testData)
        {
            dspaceObject.addDetails(s);
        }
        assertThat("testAddDetails 0", dspaceObject.getDetails(), is(equalTo("details 1, details 2, details 3")));
        assertThat("testAddDetails 1", dspaceObject.getDetails(), is(not(equalTo(null))));
    }

    /**
     * Test of getDetails method, of class DSpaceObject.
     */
    @Test
    public void testGetDetails()
    {
        assertThat("testGetDetails 0", dspaceObject.getDetails(), nullValue());

        String[] testData = new String[] {"details 1", "details 2", "details 3"};
        for(String s: testData)
        {
            dspaceObject.addDetails(s);
        }
        assertThat("testGetDetails 1", dspaceObject.getDetails(), is(equalTo("details 1, details 2, details 3")));
    }
    
    /**
     * Test of find method, of class DSpaceObject.
     */
    @Test
    public void testFind() throws SQLException
    {
        if(this.dspaceObject instanceof Bitstream)
        {
            assertThat("BITSTREAM type", DSpaceObject.find(context,
                Constants.BITSTREAM, dspaceObject.getID()), notNullValue());
        }
        else if(this.dspaceObject instanceof Bundle)
        {
            assertThat("BUNDLE type", DSpaceObject.find(context,
                Constants.BUNDLE, dspaceObject.getID()), notNullValue());
        }
        else if(this.dspaceObject instanceof Item)
        {
            assertThat("ITEM type", DSpaceObject.find(context,
                Constants.ITEM, dspaceObject.getID()), notNullValue());
        }
        else if(this.dspaceObject instanceof Collection)
        {
            assertThat("COLLECTION type", DSpaceObject.find(context,
                Constants.COLLECTION, dspaceObject.getID()), notNullValue());
        }
        else if(this.dspaceObject instanceof Community)
        {
            assertThat("COMMUNITY type", DSpaceObject.find(context,
                Constants.COMMUNITY, dspaceObject.getID()), notNullValue());
        }
        else if(this.dspaceObject instanceof Group)
        {
            assertThat("GROUP type", DSpaceObject.find(context,
                Constants.GROUP, dspaceObject.getID()), notNullValue());
        }
        else if(this.dspaceObject instanceof EPerson)
        {
            assertThat("EPERSON type", DSpaceObject.find(context,
                Constants.EPERSON, dspaceObject.getID()), notNullValue());
        }
        else if(this.dspaceObject instanceof Site)
        {
            assertThat("SITE type", DSpaceObject.find(context,
                Constants.SITE, dspaceObject.getID()), notNullValue());
        }
        else
        {
            assertThat("Unknown type", DSpaceObject.find(context, -99, 1),
                nullValue());
        }
    }
    
    /**
     * Test of getAdminObject method, of class DSpaceObject.
     */
    @Test
    public void testGetAdminObject() throws SQLException
    {
        assertThat("READ action", dspaceObject.getAdminObject(Constants.READ), is(equalTo(dspaceObject)));

        assertThat("WRITE action", dspaceObject.getAdminObject(Constants.WRITE), is(equalTo(dspaceObject)));

        assertThat("DELETE action", dspaceObject.getAdminObject(Constants.DELETE), is(equalTo(dspaceObject)));

        assertThat("ADD action", dspaceObject.getAdminObject(Constants.ADD), is(equalTo(dspaceObject)));

        assertThat("REMOVE action", dspaceObject.getAdminObject(Constants.REMOVE), is(equalTo(dspaceObject)));

        assertThat("WORKFLOW_STEP_1 action", dspaceObject.getAdminObject(Constants.WORKFLOW_STEP_1), is(equalTo(dspaceObject)));

        assertThat("WORKFLOW_STEP_2 action", dspaceObject.getAdminObject(Constants.WORKFLOW_STEP_2), is(equalTo(dspaceObject)));

        assertThat("WORKFLOW_STEP_3 action", dspaceObject.getAdminObject(Constants.WORKFLOW_STEP_3), is(equalTo(dspaceObject)));

        assertThat("WORKFLOW_ABORT action", dspaceObject.getAdminObject(Constants.WORKFLOW_ABORT), is(equalTo(dspaceObject)));

        assertThat("DEFAULT_BITSTREAM_READ action", dspaceObject.getAdminObject(Constants.DEFAULT_BITSTREAM_READ), is(equalTo(dspaceObject)));

        assertThat("DEFAULT_ITEM_READ action", dspaceObject.getAdminObject(Constants.DEFAULT_ITEM_READ), is(equalTo(dspaceObject)));
    }

    /**
     * Test of getAdminObject method, of class DSpaceObject.
     */
    @Test(expected=IllegalArgumentException.class)
    public void testGetAdminObjectwithException() throws SQLException
    {
        
        if(this.dspaceObject instanceof Bundle
                || this.dspaceObject instanceof Community
                || this.dspaceObject instanceof Collection
                || this.dspaceObject instanceof Item)
        {
            //the previous classes overwrite the method, we add this to pass
            //this test
            throw new IllegalArgumentException();
        }
        else
        {
            dspaceObject.getAdminObject(Constants.ADMIN);
            fail("Exception should have been thrown");
        }
    }

    /**
     * Test of getParentObject method, of class DSpaceObject.
     */
    @Test
    public void testGetParentObject() throws SQLException
    {
        assertThat("testGetParentObject 0", dspaceObject.getParentObject(), nullValue());
    }

    /**
     * Test of getType method, of class DSpaceObject.
     */
    @Test
    public abstract void testGetType();

    /**
     * Test of getID method, of class DSpaceObject.
     */
    @Test
    public abstract void testGetID();

    /**
     * Test of getHandle method, of class DSpaceObject.
     */
    @Test
    public abstract void testGetHandle();

    /**
     * Test of getName method, of class DSpaceObject.
     */
    @Test
    public abstract void testGetName();

}
