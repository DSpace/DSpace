/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import java.util.Iterator;

import org.apache.commons.collections.CollectionUtils;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import mockit.NonStrictExpectations;
import java.io.FileInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import org.apache.log4j.Logger;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
    private Item item;
    private Collection collection;
    private Community owningCommunity;


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
            context.turnOffAuthorisationSystem();
            this.owningCommunity = communityService.create(null, context);
            this.collection = collectionService.create(context, owningCommunity);
            WorkspaceItem workspaceItem = workspaceItemService.create(context, collection, false);
            this.item = installItemService.installItem(context, workspaceItem);
            this.b = bundleService.create(context, item, "TESTBUNDLE");
            this.dspaceObject = b;

            //we need to commit the changes so we don't block the table for testing
            context.restoreAuthSystemState();
        }
        catch (SQLException | AuthorizeException ex)
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
//        try {
//            context.turnOffAuthorisationSystem();
//            b = bundleService.find(context, b.getID());
//            if(b != null)
//            {
//                itemService.removeBundle(context, item, b);
//            }
//            collectionService.removeItem(context, collection, item);
//            communityService.removeCollection(context, owningCommunity, collection);
//            communityService.delete(context, owningCommunity);
//            context.restoreAuthSystemState();
//        } catch (SQLException | AuthorizeException | IOException ex) {
//            log.error("SQL Error in destroy", ex);
//            fail("SQL Error in destroy: " + ex.getMessage());
//        }
        b = null;
        item = null;
        collection = null;
        owningCommunity = null;
        super.destroy();
    }

    @Test
    public void testDeleteParents() throws Exception
    {
        try {
            context.turnOffAuthorisationSystem();
            b = bundleService.find(context, b.getID());
            if(b != null)
            {
                itemService.removeBundle(context, item, b);
            }
            item = itemService.find(context, item.getID());
            collection = collectionService.find(context, collection.getID());
            owningCommunity = communityService.find(context, owningCommunity.getID());

            collectionService.removeItem(context, collection, item);
            communityService.removeCollection(context, owningCommunity, collection);
            communityService.delete(context, owningCommunity);
            context.restoreAuthSystemState();
        } catch (SQLException | AuthorizeException | IOException ex) {
            log.error("SQL Error in destroy", ex);
            fail("SQL Error in destroy: " + ex.getMessage());
        }
    }

    /**
     * Test of find method, of class Bundle.
     */
    @Test
    public void testBundleFind() throws SQLException
    {
        UUID id = b.getID();
        Bundle found =  bundleService.find(context, id);
        assertThat("testBundleFind 0", found, notNullValue());
        assertThat("testBundleFind 1", found.getID(), equalTo(id));
    }

    /**
     * Test of create method, of class Bundle.
     */
    @Test
    public void testCreate() throws SQLException, AuthorizeException
    {
        new NonStrictExpectations(authorizeService.getClass())
        {
            {
                // Allow Bundle ADD perms
                authorizeService.authorizeAction((Context) any, (Bundle) any,
                        Constants.ADD);
                result = null;
        }};
        Bundle created = bundleService.create(context, item, "testCreateBundle");
        //the item created by default has no name nor type set
        assertThat("testCreate 0", created, notNullValue());
        assertTrue("testCreate 1", created.getID() != null);
        assertTrue("testCreate 2", created.getBitstreams().size() == 0);
        assertEquals("testCreate 3", created.getName(), "testCreateBundle");
    }

    /**
     * Test of getID method, of class Bundle.
     */
    @Override
    @Test
    public void testGetID()
    {
        assertTrue("testGetID 0", b.getID() != null);
    }

    @Test
    public void testLegacyID() { assertTrue("testGetLegacyID 0", b.getLegacyId() == null);}

    /**
     * Test of getName method, of class Bundle.
     */
    @Override
    @Test
    public void testGetName()
    {
        //created bundle has no name
        assertThat("testGetName 0", b.getName(), equalTo("TESTBUNDLE"));
    }

    /**
     * Test of setName method, of class Bundle.
     */
    @Test
    public void testSetName() throws SQLException
    {
        String name = "new name";
        b.setName(context, name);
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
        assertThat("testGetPrimaryBitstreamID 0", b.getPrimaryBitstream(), equalTo(null));
    }

    /**
     * Test of setPrimaryBitstreamID method, of class Bundle.
     */
    @Test
    public void testSetPrimaryBitstreamID() throws SQLException, AuthorizeException, IOException
    {
        new NonStrictExpectations(authorizeService.getClass())
        {
            {
                // Allow Bundle ADD perms
                authorizeService.authorizeAction((Context) any, (Bundle) any,
                        Constants.ADD);
                authorizeService.authorizeAction((Context) any, (Bitstream) any,
                        Constants.WRITE);
                result = null;
        }};
        File f = new File(testProps.get("test.bitstream").toString());
        Bitstream bs = bitstreamService.create(context, new FileInputStream(f));
        bundleService.addBitstream(context, b, bs);
        b.setPrimaryBitstreamID(bs);
        assertThat("testSetPrimaryBitstreamID 0", b.getPrimaryBitstream(), equalTo(bs));
    }

    /**
     * Test of unsetPrimaryBitstreamID method, of class Bundle.
     */
    @Test
    public void testUnsetPrimaryBitstreamID() throws IOException, SQLException, AuthorizeException
    {
        new NonStrictExpectations(authorizeService.getClass())
        {
            {
                // Allow Bundle ADD perms
                authorizeService.authorizeAction((Context) any, (Bundle) any,
                        Constants.ADD);
                authorizeService.authorizeAction((Context) any, (Bitstream) any,
                        Constants.WRITE);
                result = null;
        }};
        //set a value different than default
        File f = new File(testProps.get("test.bitstream").toString());
        Bitstream bs = bitstreamService.create(context, new FileInputStream(f));
        bundleService.addBitstream(context, b, bs);
        b.setPrimaryBitstreamID(bs);
        //unset
        b.unsetPrimaryBitstreamID();
        //is -1 when not set
        assertThat("testUnsetPrimaryBitstreamID 0", b.getPrimaryBitstream(), equalTo(null));
    }

    /**
     * Test of getHandle method, of class Bundle.
     */
    @Override
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
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Allow Bundle ADD perms
                authorizeService.authorizeAction((Context) any, (Bundle) any,
                    Constants.ADD); result = null;
                authorizeService.authorizeAction((Context) any, (Bitstream) any,
                    Constants.WRITE); result = null;

        }};

        String name = "name";
        //by default there is no bitstream
        assertThat("testGetHandle 0", bundleService.getBitstreamByName(b, name), nullValue());
        
        //let's add a bitstream
        File f = new File(testProps.get("test.bitstream").toString());
        Bitstream bs = bitstreamService.create(context, new FileInputStream(f));
        bs.setName(context, name);
        bundleService.addBitstream(context, b, bs);
        bundleService.update(context, b);
        assertThat("testGetHandle 1", bundleService.getBitstreamByName(b, name), notNullValue());
        assertThat("testGetHandle 2", bundleService.getBitstreamByName(b, name), equalTo(bs));
        assertThat("testGetHandle 3", bundleService.getBitstreamByName(b, name).getName(), equalTo(name));
    }

    /**
     * Test of getBitstreams method, of class Bundle.
     */
    @Test
    public void testGetBitstreams() throws FileNotFoundException, SQLException, IOException, AuthorizeException
    {
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Allow Bundle ADD perms
                authorizeService.authorizeAction((Context) any, (Bundle) any,
                    Constants.ADD); result = null;
                authorizeService.authorizeAction((Context) any, (Bitstream) any,
                    Constants.WRITE); result = null;

        }};

        //default bundle has no bitstreams
        assertThat("testGetBitstreams 0", b.getBitstreams(), notNullValue());
        assertThat("testGetBitstreams 1", b.getBitstreams().size(), equalTo(0));

        //let's add a bitstream
        String name = "name";
        File f = new File(testProps.get("test.bitstream").toString());
        Bitstream bs = bitstreamService.create(context, new FileInputStream(f));
        bs.setName(context, name);
        bundleService.addBitstream(context, b, bs);
        assertThat("testGetBitstreams 2", b.getBitstreams(), notNullValue());
        assertThat("testGetBitstreams 3", b.getBitstreams().size(), equalTo(1));
        assertThat("testGetBitstreams 4", b.getBitstreams().get(0).getName(), equalTo(name));
    }

    /**
     * Test of getItems method, of class Bundle.
     */
    @Test
    public void testGetItems() throws SQLException
    {
        //by default this bundle belong to no item
        assertThat("testGetItems 0", b.getItems(), notNullValue());
        assertThat("testGetItems 1", b.getItems().size(), equalTo(1));
    }

    /**
     * Test of createBitstream method, of class Bundle.
     */
    @Test(expected=AuthorizeException.class)
    public void testCreateBitstreamNoAuth() throws FileNotFoundException, AuthorizeException, SQLException, IOException
    {
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Disallow Bundle ADD perms
                authorizeService.authorizeAction((Context) any, (Bundle) any,
                    Constants.ADD); result = new AuthorizeException();

        }};

        File f = new File(testProps.get("test.bitstream").toString());
        Bitstream bs = bitstreamService.create(context, b, new FileInputStream(f));
        fail("Exception should be thrown");
    }

    /**
     * Test of createBitstream method, of class Bundle.
     */
    @Test
    public void testCreateBitstreamAuth() throws FileNotFoundException, AuthorizeException, SQLException, IOException
    {
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Allow Bundle ADD perms
                authorizeService.authorizeAction((Context) any, (Bundle) any,
                    Constants.ADD); result = null;
                authorizeService.authorizeAction((Context) any, (Bitstream) any,
                    Constants.WRITE); result = null;

        }};

        String name = "name";
        File f = new File(testProps.get("test.bitstream").toString());
        Bitstream bs = bitstreamService.create(context, b, new FileInputStream(f));
        bs.setName(context, name);
        assertThat("testCreateBitstreamAuth 0", bundleService.getBitstreamByName(b, name), notNullValue());
        assertThat("testCreateBitstreamAuth 1", bundleService.getBitstreamByName(b, name), equalTo(bs));
        assertThat("testCreateBitstreamAuth 2", bundleService.getBitstreamByName(b, name).getName(), equalTo(name));
    }
    
    /**
     * Test of registerBitstream method, of class Bundle.
     */
    @Test(expected=AuthorizeException.class)
    public void testRegisterBitstreamNoAuth() throws AuthorizeException, IOException, SQLException 
    {
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Disallow Bundle ADD perms
                authorizeService.authorizeAction((Context) any, (Bundle) any,
                    Constants.ADD); result = new AuthorizeException();

        }};

        int assetstore = 0; //default assetstore
        File f = new File(testProps.get("test.bitstream").toString());
        Bitstream bs = bitstreamService.register(context, b, assetstore, f.getAbsolutePath());
        fail("Exception should be thrown");
    }

    /**
     * Test of registerBitstream method, of class Bundle.
     */
    @Test
    public void testRegisterBitstreamAuth() throws AuthorizeException, IOException, SQLException 
    {
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Allow Bundle ADD perms
                authorizeService.authorizeAction((Context) any, (Bundle) any,
                    Constants.ADD); result = null;

                authorizeService.authorizeAction((Context) any, (Bitstream) any,
                    Constants.WRITE); result = null;
        }};

        int assetstore = 0;  //default assetstore
        String name = "name bitstream";
        File f = new File(testProps.get("test.bitstream").toString());        
        Bitstream bs = bitstreamService.register(context, b, assetstore, f.getName());
        bs.setName(context, name);
        assertThat("testRegisterBitstream 0", bundleService.getBitstreamByName(b, name), notNullValue());
        assertThat("testRegisterBitstream 1", bundleService.getBitstreamByName(b, name), equalTo(bs));
        assertThat("testRegisterBitstream 2", bundleService.getBitstreamByName(b, name).getName(), equalTo(name));
    }

    /**
     * Test of addBitstream method, of class Bundle.
     */
    @Test(expected=AuthorizeException.class)
    public void testAddBitstreamNoAuth() throws SQLException, AuthorizeException, IOException
    {
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Disallow Bundle ADD perms
                authorizeService.authorizeAction((Context) any, (Bundle) any,
                    Constants.ADD); result = new AuthorizeException();

        }};

        // create a new Bitstream to add to Bundle
        File f = new File(testProps.get("test.bitstream").toString());
        Bitstream bs = bitstreamService.create(context, new FileInputStream(f));
        bs.setName(context, "name");
        bundleService.addBitstream(context, b, bs);
        fail("Exception should have been thrown");
    }

    /**
     * Test of addBitstream method, of class Bundle.
     */
    @Test
    public void testAddBitstreamAuth() throws SQLException, AuthorizeException, FileNotFoundException, IOException
    {
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Allow Bundle ADD perms
                authorizeService.authorizeAction((Context) any, (Bundle) any,
                    Constants.ADD); result = null;
                authorizeService.authorizeAction((Context) any, (Bitstream) any,
                    Constants.WRITE); result = null;

        }};
        
        File f = new File(testProps.get("test.bitstream").toString());
        Bitstream bs = bitstreamService.create(context, new FileInputStream(f));
        bs.setName(context, "name");
        bundleService.addBitstream(context, b, bs);
        assertThat("testAddBitstreamAuth 0", bundleService.getBitstreamByName(b, bs.getName()), notNullValue());
        assertThat("testAddBitstreamAuth 1", bundleService.getBitstreamByName(b, bs.getName()), equalTo(bs));
        assertThat("testAddBitstreamAuth 2", bundleService.getBitstreamByName(b, bs.getName()).getName(), equalTo(bs.getName()));
    }

    /**
     * Test of removeBitstream method, of class Bundle.
     */
    @Test(expected=AuthorizeException.class)
    public void testRemoveBitstreamNoAuth() throws SQLException, AuthorizeException, IOException
    {
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Disallow Bundle REMOVE perms
            authorizeService.authorizeAction((Context) any, (Bundle) any,
                    Constants.REMOVE); result = new AuthorizeException();

        }};

        File f = new File(testProps.get("test.bitstream").toString());
        Bitstream bs = bitstreamService.create(context, new FileInputStream(f));
        bs.setName(context, "name");
        bundleService.removeBitstream(context, b, bs);
        fail("Exception should have been thrown");
    }

    /**
     * Test of removeBitstream method, of class Bundle.
     */
    @Test
    public void testRemoveBitstreamAuth() throws SQLException, AuthorizeException, IOException
    {
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Allow Bundle ADD perms (to create a new Bitstream and add it)
            authorizeService.authorizeAction((Context) any, (Bundle) any,
                    Constants.ADD); result = null;
            // Allow Bundle REMOVE perms (to test remove)
            authorizeService.authorizeAction((Context) any, (Bundle) any,
                    Constants.REMOVE); result = null;
                authorizeService.authorizeAction((Context) any, (Bitstream) any,
                    Constants.WRITE); result = null;
                authorizeService.authorizeAction(context, (Bitstream) any,
                    Constants.DELETE); result = null;

        }};

        // Create a new Bitstream to test with
        File f = new File(testProps.get("test.bitstream").toString());
        Bitstream bs = bitstreamService.create(context, new FileInputStream(f));
        bundleService.addBitstream(context, b, bs);
        bundleService.removeBitstream(context, b, bs);
        assertThat("testRemoveBitstreamAuth 0", bundleService.getBitstreamByName(b, bs.getName()), nullValue());
    }


    /**
     * Test of update method, of class Bundle.
     */
    @Test
    public void testUpdate() throws SQLException, AuthorizeException 
    {
        //TODO: we only check for sql errors
        //TODO: note that update can't throw authorize exception!!
        bundleService.update(context, b);
    }

    /**
     * Test of delete method, of class Bundle.
     */
    @Test
    public void testDelete() throws SQLException, AuthorizeException, IOException
    {
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Allow Bundle ADD perms (to create a new Bitstream and add it)
            authorizeService.authorizeAction((Context) any, (Bundle) any,
                    Constants.ADD); result = null;
            // Allow Bundle REMOVE perms (to test remove)
            authorizeService.authorizeAction((Context) any, (Bundle) any,
                    Constants.REMOVE); result = null;
            authorizeService.authorizeAction((Context) any, (Bundle) any,
                    Constants.DELETE); result = null;
        }};

        UUID id = b.getID();
        itemService.removeBundle(context, item, b);
        assertThat("testDelete 0", bundleService.find(context, id), nullValue());
    }

    /**
     * Test of getType method, of class Bundle.
     */
    @Override
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
        //TODO: we would need a method to get policies from collection, probably better!
        List<ResourcePolicy> defaultCollectionPolicies = authorizeService.getPoliciesActionFilter(context, collection,
                Constants.DEFAULT_BITSTREAM_READ);
        Iterator<ResourcePolicy> it = defaultCollectionPolicies.iterator();

        bundleService.inheritCollectionDefaultPolicies(context, b, collection);

        while (it.hasNext())
        {
            ResourcePolicy rp = (ResourcePolicy) it.next();
            rp.setAction(Constants.READ);
        }

        List<ResourcePolicy> bspolicies = bundleService.getBundlePolicies(context, b);
        assertTrue("testInheritCollectionDefaultPolicies 0", defaultCollectionPolicies.size() == bspolicies.size());

        boolean equals = false;
        for(int i=0; i < defaultCollectionPolicies.size(); i++)
        {
            ResourcePolicy collectionPolicy = defaultCollectionPolicies.get(i);
            ResourcePolicy bundlePolicy = bspolicies.get(i);
            if(collectionPolicy.getAction() == bundlePolicy.getAction() && collectionPolicy.getGroup().equals(bundlePolicy.getGroup()))
            {
                equals = true;
            }
        }
        assertTrue("testInheritCollectionDefaultPolicies 1", equals);

        bspolicies = bundleService.getBitstreamPolicies(context, b);
        boolean exists = true;
        for(int i=0; bspolicies.size() > 0 && i < defaultCollectionPolicies.size(); i++)
        {
            ResourcePolicy collectionPolicy = defaultCollectionPolicies.get(i);
            ResourcePolicy bitstreamPolicy = bspolicies.get(i);
            if(collectionPolicy.getAction() == bitstreamPolicy.getAction() && collectionPolicy.getGroup().equals(bitstreamPolicy.getGroup()))
            {
                exists = true;
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
        newpolicies.add(resourcePolicyService.create(context));
        newpolicies.add(resourcePolicyService.create(context));
        newpolicies.add(resourcePolicyService.create(context));
        bundleService.replaceAllBitstreamPolicies(context, b, newpolicies);
        
        List<ResourcePolicy> bspolicies = bundleService.getBundlePolicies(context, b);
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

        bspolicies = bundleService.getBitstreamPolicies(context, b);
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
        List<ResourcePolicy> bspolicies = bundleService.getBundlePolicies(context, b);
        assertTrue("testGetBundlePolicies 0", CollectionUtils.isNotEmpty(bspolicies));
    }

    /**
     * Test of getBundlePolicies method, of class Bundle.
     */
    @Test
    public void testGetBitstreamPolicies() throws SQLException
    {
        //empty by default
        List<ResourcePolicy> bspolicies = bundleService.getBitstreamPolicies(context, b);
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
        assertThat("testGetAdminObject 0", bundleService.getAdminObject(context, b, Constants.REMOVE), instanceOf(Item.class));
        assertThat("testGetAdminObject 1", bundleService.getAdminObject(context, b, Constants.ADD), instanceOf(Item.class));
    }

    /**
     * Test of getParentObject method, of class Bundle.
     */
    @Test
    @Override
    public void testGetParentObject() throws SQLException
    {
        //default bundle has no parent
        assertThat("testGetParentObject 0", bundleService.getParentObject(context, b), notNullValue());
        assertThat("testGetParentObject 0", bundleService.getParentObject(context, b), instanceOf(Item.class));
    }

}
