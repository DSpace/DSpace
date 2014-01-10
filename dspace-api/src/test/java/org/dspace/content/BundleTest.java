/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import java.util.Iterator;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.authorize.AuthorizeManager;
import mockit.NonStrictExpectations;
import java.io.FileInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import org.apache.log4j.Logger;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.ResourcePolicy;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

/**
 * Units tests for class Bundle
 * @author pvillega
 */
public class BundleTest extends AbstractDSpaceObjectTest
{
    /** log4j category */
    private static final Logger log = Logger.getLogger(BundleTest.class);
 
    /**
     * Bundle instance for the tests
     */
    private Bundle b;

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
            this.b = Bundle.create(context);            
            this.dspaceObject = b;

            //we need to commit the changes so we don't block the table for testing
            context.commit();
        }
        catch (SQLException ex)
        {
            log.error("SQL Error in init", ex);
            fail("SQL Error in init: " + ex.getMessage());
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
        b = null;
        super.destroy();
    }

    /**
     * Test of find method, of class Bundle.
     */
    @Test
    public void testBundleFind() throws SQLException
    {
        int id = b.getID();
        Bundle found =  Bundle.find(context, id);
        assertThat("testBundleFind 0", found, notNullValue());
        assertThat("testBundleFind 1", found.getID(), equalTo(id));
    }

    /**
     * Test of create method, of class Bundle.
     */
    @Test
    public void testCreate() throws SQLException
    {
        Bundle created = Bundle.create(context);
        //the item created by default has no name nor type set
        assertThat("testCreate 0", created, notNullValue());
        assertTrue("testCreate 1", created.getID() >= 0);
        assertTrue("testCreate 2", created.getBitstreams().length == 0);
        assertThat("testCreate 3", created.getName(), nullValue());
    }

    /**
     * Test of getID method, of class Bundle.
     */
    @Test
    public void testGetID()
    {
        assertTrue("testGetID 0", b.getID() >= 0);
    }

    /**
     * Test of getName method, of class Bundle.
     */
    @Test
    public void testGetName()
    {
        //created bundle has no name
        assertThat("testGetName 0", b.getName(), nullValue());
    }

    /**
     * Test of setName method, of class Bundle.
     */
    @Test
    public void testSetName()
    {
        String name = "new name";
        b.setName(name);
        assertThat("testSetName 0", b.getName(), notNullValue());
        assertThat("testSetName 1", b.getName(), not(equalTo("")));
        assertThat("testSetName 2", b.getName(), equalTo(name));
    }

    /**
     * Test of getPrimaryBitstreamID method, of class Bundle.
     */
    @Test
    public void testGetPrimaryBitstreamID() 
    {
        //is -1 when not set
        assertThat("testGetPrimaryBitstreamID 0", b.getPrimaryBitstreamID(), equalTo(-1));
    }

    /**
     * Test of setPrimaryBitstreamID method, of class Bundle.
     */
    @Test
    public void testSetPrimaryBitstreamID()
    {
        int id = 1;
        b.setPrimaryBitstreamID(id);
        assertThat("testSetPrimaryBitstreamID 0", b.getPrimaryBitstreamID(), equalTo(id));
    }

    /**
     * Test of unsetPrimaryBitstreamID method, of class Bundle.
     */
    @Test
    public void testUnsetPrimaryBitstreamID()
    {
        //set a value different than default
        int id = 6;
        b.setPrimaryBitstreamID(id);
        //unset
        b.unsetPrimaryBitstreamID();
        //is -1 when not set
        assertThat("testUnsetPrimaryBitstreamID 0", b.getPrimaryBitstreamID(), equalTo(-1));
    }

    /**
     * Test of getHandle method, of class Bundle.
     */
    @Test
    public void testGetHandle() 
    {
        //no handle for bundles
        assertThat("testGetHandle 0", b.getHandle(), nullValue());
    }

    /**
     * Test of getBitstreamByName method, of class Bundle.
     */
    @Test
    public void testGetBitstreamByName() throws FileNotFoundException, SQLException, IOException, AuthorizeException
    {
        new NonStrictExpectations()
        {
            AuthorizeManager authManager;
            {
                AuthorizeManager.authorizeAction((Context) any, (Bundle) any,
                        Constants.ADD); result = null;
            }
        };

        String name = "name";
        //by default there is no bitstream
        assertThat("testGetHandle 0", b.getBitstreamByName(name), nullValue());
        
        //let's add a bitstream
        File f = new File(testProps.get("test.bitstream").toString());
        Bitstream bs = Bitstream.create(context, new FileInputStream(f));
        bs.setName(name);
        b.addBitstream(bs);
        assertThat("testGetHandle 1", b.getBitstreamByName(name), notNullValue());
        assertThat("testGetHandle 2", b.getBitstreamByName(name), equalTo(bs));
        assertThat("testGetHandle 3", b.getBitstreamByName(name).getName(), equalTo(name));    
        context.commit();
    }

    /**
     * Test of getBitstreams method, of class Bundle.
     */
    @Test
    public void testGetBitstreams() throws FileNotFoundException, SQLException, IOException, AuthorizeException
    {
        new NonStrictExpectations()
        {
            AuthorizeManager authManager;
            {
                AuthorizeManager.authorizeAction((Context) any, (Bundle) any,
                        Constants.ADD); result = null;
            }
        };

        //default bundle has no bitstreams
        assertThat("testGetBitstreams 0", b.getBitstreams(), notNullValue());
        assertThat("testGetBitstreams 1", b.getBitstreams().length, equalTo(0));

        //let's add a bitstream
        String name = "name";
        File f = new File(testProps.get("test.bitstream").toString());
        Bitstream bs = Bitstream.create(context, new FileInputStream(f));
        bs.setName(name);
        b.addBitstream(bs);
        assertThat("testGetBitstreams 2", b.getBitstreams(), notNullValue());
        assertThat("testGetBitstreams 3", b.getBitstreams().length, equalTo(1));
        assertThat("testGetBitstreams 4", b.getBitstreams()[0].getName(), equalTo(name));
        context.commit();
    }

    /**
     * Test of getItems method, of class Bundle.
     */
    @Test
    public void testGetItems() throws SQLException
    {
        //by default this bundle belong to no item
        assertThat("testGetItems 0", b.getItems(), notNullValue());
        assertThat("testGetItems 1", b.getItems().length, equalTo(0));
    }

    /**
     * Test of createBitstream method, of class Bundle.
     */
    @Test(expected=AuthorizeException.class)
    public void testCreateBitstreamNoAuth() throws FileNotFoundException, AuthorizeException, SQLException, IOException
    {
        new NonStrictExpectations()
        {
            AuthorizeManager authManager;
            {
                AuthorizeManager.authorizeAction((Context) any, (Bundle) any,
                        Constants.ADD); result = new AuthorizeException();
            }
        };

        String name = "name";
        File f = new File(testProps.get("test.bitstream").toString());
        Bitstream bs = b.createBitstream(new FileInputStream(f));
        fail("Exception should be thrown");
    }

    /**
     * Test of createBitstream method, of class Bundle.
     */
    @Test
    public void testCreateBitstreamAuth() throws FileNotFoundException, AuthorizeException, SQLException, IOException
    {
        new NonStrictExpectations()
        {
            AuthorizeManager authManager;
            {
                AuthorizeManager.authorizeAction((Context) any, (Bundle) any,
                        Constants.ADD); result = null;
            }
        };

        String name = "name";
        File f = new File(testProps.get("test.bitstream").toString());
        Bitstream bs = b.createBitstream(new FileInputStream(f));
        bs.setName(name);
        assertThat("testCreateBitstreamAuth 0", b.getBitstreamByName(name), notNullValue());
        assertThat("testCreateBitstreamAuth 1", b.getBitstreamByName(name), equalTo(bs));
        assertThat("testCreateBitstreamAuth 2", b.getBitstreamByName(name).getName(), equalTo(name));
    }
    
    /**
     * Test of registerBitstream method, of class Bundle.
     */
    @Test(expected=AuthorizeException.class)
    public void testRegisterBitstreamNoAuth() throws AuthorizeException, IOException, SQLException 
    {

        new NonStrictExpectations()
        {
            AuthorizeManager authManager;
            {
                AuthorizeManager.authorizeAction((Context) any, (Bundle) any, Constants.ADD);
                result = new AuthorizeException();
            }
        };

        int assetstore = 0;
        File f = new File(testProps.get("test.bitstream").toString());
        Bitstream bs = b.registerBitstream(assetstore, f.getAbsolutePath());
        fail("Exception should be thrown");
    }

    /**
     * Test of registerBitstream method, of class Bundle.
     */
    @Test
    public void testRegisterBitstreamAuth() throws AuthorizeException, IOException, SQLException 
    {

        new NonStrictExpectations()
        {
            AuthorizeManager authManager;
            {
                AuthorizeManager.authorizeAction((Context) any, (Bundle) any, Constants.ADD);
                result = null;
            }
        };

        int assetstore = 0;
        String name = "name bitstream";
        File f = new File(testProps.get("test.bitstream").toString());        
        Bitstream bs = b.registerBitstream(assetstore, f.getName());
        bs.setName(name);
        assertThat("testRegisterBitstream 0", b.getBitstreamByName(name), notNullValue());
        assertThat("testRegisterBitstream 1", b.getBitstreamByName(name), equalTo(bs));
        assertThat("testRegisterBitstream 2", b.getBitstreamByName(name).getName(), equalTo(name));
    }

    /**
     * Test of addBitstream method, of class Bundle.
     */
    @Test(expected=AuthorizeException.class)
    public void testAddBitstreamNoAuth() throws SQLException, AuthorizeException
    {
        new NonStrictExpectations()
        {
            AuthorizeManager authManager;
            {
                AuthorizeManager.authorizeAction((Context) any, (Bundle) any, Constants.ADD);
                result = new AuthorizeException();
            }
        };

        int id = 1;
        Bitstream bs = Bitstream.find(context, id);
        b.addBitstream(bs);
        fail("Exception should have been thrown");
    }

    /**
     * Test of addBitstream method, of class Bundle.
     */
    @Test
    public void testAddBitstreamAuth() throws SQLException, AuthorizeException, FileNotFoundException, IOException
    {
        new NonStrictExpectations()
        {
            AuthorizeManager authManager;
            {
                AuthorizeManager.authorizeAction((Context) any, (Bundle) any, Constants.ADD);
                result = null;
            }
        };


        File f = new File(testProps.get("test.bitstream").toString());
        Bitstream bs = Bitstream.create(context, new FileInputStream(f));
        bs.setName("name");
        b.addBitstream(bs);
        assertThat("testAddBitstreamAuth 0", b.getBitstreamByName(bs.getName()), notNullValue());
        assertThat("testAddBitstreamAuth 1", b.getBitstreamByName(bs.getName()), equalTo(bs));
        assertThat("testAddBitstreamAuth 2", b.getBitstreamByName(bs.getName()).getName(), equalTo(bs.getName()));
    }

    /**
     * Test of removeBitstream method, of class Bundle.
     */
    @Test(expected=AuthorizeException.class)
    public void testRemoveBitstreamNoAuth() throws SQLException, AuthorizeException, IOException
    {
        new NonStrictExpectations()
        {
            AuthorizeManager authManager;
            {
                AuthorizeManager.authorizeAction((Context) any, (Bundle) any, Constants.REMOVE);
                result = new AuthorizeException();
            }
        };

        File f = new File(testProps.get("test.bitstream").toString());
        Bitstream bs = Bitstream.create(context, new FileInputStream(f));
        bs.setName("name");
        b.removeBitstream(bs);
        fail("Exception should have been thrown");
    }

    /**
     * Test of removeBitstream method, of class Bundle.
     */
    @Test
    public void testRemoveBitstreamAuth() throws SQLException, AuthorizeException, IOException
    {
        new NonStrictExpectations()
        {
            AuthorizeManager authManager;
            {
                AuthorizeManager.authorizeAction((Context) any, (Bundle) any, Constants.REMOVE);
                result = null;
            }
        };

        int id = 1;
        File f = new File(testProps.get("test.bitstream").toString());
        Bitstream bs = Bitstream.find(context, id);
        b.addBitstream(bs);
        context.commit();
        b.removeBitstream(bs);
        assertThat("testRemoveBitstreamAuth 0", b.getBitstreamByName(bs.getName()), nullValue());
    }


    /**
     * Test of update method, of class Bundle.
     */
    @Test
    public void testUpdate() throws SQLException, AuthorizeException 
    {
        //TODO: we only check for sql errors
        //TODO: note that update can't throw authorize exception!!
        b.update();
    }

    /**
     * Test of delete method, of class Bundle.
     */
    @Test
    public void testDelete() throws SQLException, AuthorizeException, IOException
    {
        int id = b.getID();
        b.delete();
        context.commit();
        assertThat("testDelete 0", Bundle.find(context, id), nullValue());
    }

    /**
     * Test of getType method, of class Bundle.
     */
    @Test
    public void testGetType()
    {
        assertThat("testGetType 0", b.getType(), equalTo(Constants.BUNDLE));
    }

    /**
     * Test of inheritCollectionDefaultPolicies method, of class Bundle.
     */
    @Test
    public void testInheritCollectionDefaultPolicies() throws AuthorizeException, SQLException
    {
        Collection c = Collection.create(context);

        //TODO: we would need a method to get policies from collection, probably better!
        List<ResourcePolicy> newpolicies = AuthorizeManager.getPoliciesActionFilter(context, c,
                Constants.DEFAULT_BITSTREAM_READ);
        Iterator<ResourcePolicy> it = newpolicies.iterator();
        while (it.hasNext())
        {
            ResourcePolicy rp = (ResourcePolicy) it.next();
            rp.setAction(Constants.READ);
        }

        b.inheritCollectionDefaultPolicies(c);

        List<ResourcePolicy> bspolicies = b.getBundlePolicies();
        assertTrue("testInheritCollectionDefaultPolicies 0", newpolicies.size() == bspolicies.size());

        boolean equals = true;
        for(int i=0; i < newpolicies.size() && equals; i++)
        {
            if(!newpolicies.contains(bspolicies.get(i)))
            {
                equals = false;
            }
        }
        assertTrue("testInheritCollectionDefaultPolicies 1", equals);

        bspolicies = b.getBitstreamPolicies();
        boolean exists = true;
        for(int i=0; bspolicies.size() > 0 && i < newpolicies.size() && exists; i++)
        {
            if(!bspolicies.contains(newpolicies.get(i)))
            {
                exists = false;
            }
        }
        assertTrue("testInheritCollectionDefaultPolicies 2", exists);
        
    }

    /**
     * Test of replaceAllBitstreamPolicies method, of class Bundle.
     */
    @Test
    public void testReplaceAllBitstreamPolicies() throws SQLException, AuthorizeException
    {
        List<ResourcePolicy> newpolicies = new ArrayList<ResourcePolicy>();
        newpolicies.add(ResourcePolicy.create(context));
        newpolicies.add(ResourcePolicy.create(context));
        newpolicies.add(ResourcePolicy.create(context));
        b.replaceAllBitstreamPolicies(newpolicies);
        
        List<ResourcePolicy> bspolicies = b.getBundlePolicies();
        assertTrue("testReplaceAllBitstreamPolicies 0", newpolicies.size() == bspolicies.size());

        boolean equals = true;
        for(int i=0; i < newpolicies.size() && equals; i++)
        {
            if(!newpolicies.contains(bspolicies.get(i)))
            {
                equals = false;
            }
        }
        assertTrue("testReplaceAllBitstreamPolicies 1", equals);

        bspolicies = b.getBitstreamPolicies();
        boolean exists = true;
        for(int i=0; bspolicies.size() > 0 && i < newpolicies.size() && exists; i++)
        {
            if(!bspolicies.contains(newpolicies.get(i)))
            {
                exists = false;
            }
        }
        assertTrue("testReplaceAllBitstreamPolicies 2", exists);
    }

    /**
     * Test of getBundlePolicies method, of class Bundle.
     */
    @Test
    public void testGetBundlePolicies() throws SQLException
    {
        //empty by default
        List<ResourcePolicy> bspolicies = b.getBundlePolicies();
        assertTrue("testGetBundlePolicies 0", bspolicies.isEmpty());
    }

    /**
     * Test of getBundlePolicies method, of class Bundle.
     */
    @Test
    public void testGetBitstreamPolicies() throws SQLException
    {
        //empty by default
        List<ResourcePolicy> bspolicies = b.getBitstreamPolicies();
        assertTrue("testGetBitstreamPolicies 0", bspolicies.isEmpty());
    }

    /**
     * Test of getAdminObject method, of class Bundle.
     */
    @Test
    @Override
    public void testGetAdminObject() throws SQLException
    {
        //default bundle has no admin object
        assertThat("testGetAdminObject 0", b.getAdminObject(Constants.REMOVE), nullValue());
        assertThat("testGetAdminObject 1", b.getAdminObject(Constants.ADD), nullValue());
    }

    /**
     * Test of getParentObject method, of class Bundle.
     */
    @Test
    @Override
    public void testGetParentObject() throws SQLException
    {
        //default bundle has no parent
        assertThat("testGetParentObject 0", b.getParentObject(), nullValue());
    }

}