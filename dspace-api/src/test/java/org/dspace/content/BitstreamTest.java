/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.BitstreamFormatService;
import org.dspace.core.Context;
import org.junit.*;
import static org.junit.Assert.* ;
import static org.hamcrest.CoreMatchers.*;
import mockit.*;
import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Constants;

/**
 * Unit Tests for class Bitstream
 * @author pvillega
 */
public class BitstreamTest extends AbstractDSpaceObjectTest
{
    /** log4j category */
    private static final Logger log = Logger.getLogger(BitstreamTest.class);


    protected BitstreamFormatService bitstreamFormatService = ContentServiceFactory.getInstance().getBitstreamFormatService();

    /**
     * BitStream instance for the tests
     */
    private Bitstream bs;

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
            //we have to create a new bitstream in the database
            File f = new File(testProps.get("test.bitstream").toString());
            this.bs = bitstreamService.create(context, new FileInputStream(f));
            this.dspaceObject = bs;   
            //we need to commit the changes so we don't block the table for testing
        }
        catch (IOException ex) {
            log.error("IO Error in init", ex);
            fail("SQL Error in init: " + ex.getMessage());
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
        bs = null;
        super.destroy();
    }

    /**
     * Test of find method, of class Bitstream.
     */
    @Test
    public void testBSFind() throws SQLException
    {
        UUID id = this.bs.getID();
        Bitstream found =  bitstreamService.find(context, id);
        assertThat("testBSFind 0", found, notNullValue());
        //the item created by default has no name nor type set
        assertThat("testBSFind 1", found.getFormat(context).getMIMEType(), equalTo("application/octet-stream"));
        assertThat("testBSFind 2", found.getName(), nullValue());
        assertThat("testBSFind 3", found.getID(), equalTo(id));
    }

    /**
     * Test of findAll method, of class Bitstream.
     */
    @Test
    public void testFindAll() throws SQLException
    {
        List<Bitstream> found =  bitstreamService.findAll(context);
        assertThat("testFindAll 0", found, notNullValue());
        //we have many bs, one created per test run, so at least we have 1 if
        //this test is run first
        assertTrue("testFindAll 1", found.size() >= 1);

        boolean added = false;
        for(Bitstream b: found)
        {
            if(b.equals(bs))
            {
                added = true;
            }
        }
        assertTrue("testFindAll 2",added);
    }

    /**
     * Test of create method, of class Bitstream.
     */
    @Test
    public void testCreate() throws IOException, SQLException
    {
        File f = new File(testProps.get("test.bitstream").toString());
        Bitstream created = bitstreamService.create(context, new FileInputStream(f));

        //the item created by default has no name nor type set
        assertThat("testCreate 0", created.getFormat(context).getMIMEType(), equalTo("application/octet-stream"));
        assertThat("testCreate 1", created.getName(), nullValue());
    }

    /**
     * Test of register method, of class Bitstream.
     */
    @Test
    public void testRegister() throws IOException, SQLException, AuthorizeException
    {
        new NonStrictExpectations(authorizeService.getClass())
        {{
            authorizeService.authorizeAction((Context) any, (Bitstream) any,
                    Constants.WRITE); result = null;
        }};
        int assetstore = 0;
        File f = new File(testProps.get("test.bitstream").toString());
        Bitstream registered = bitstreamService.register(context,assetstore, f.getName());
        //the item created by default has no name nor type set
        assertThat("testRegister 0", registered.getFormat(context).getMIMEType(), equalTo("application/octet-stream"));
        assertThat("testRegister 1", registered.getName(), nullValue());
    }

    /**
     * Test of getID method, of class Bitstream.
     */
    @Override
    @Test
    public void testGetID()
    {
        assertTrue("testGetID 0", bs.getID() != null);
    }

    @Test
    public void testLegacyID() { assertTrue("testGetLegacyID 0", bs.getLegacyId() == null);}

    /**
     * Test of getHandle method, of class Bitstream.
     */
    @Override
    @Test
    public void testGetHandle()
    {
        //no handle for bitstreams
        assertThat("testGetHandle 0", bs.getHandle(), nullValue());
    }

    /**
     * Test of getSequenceID method, of class Bitstream.
     */
    @Test
    public void testGetSequenceID()
    {
        //sequence id is -1 when not set
        assertThat("testGetSequenceID 0", bs.getSequenceID(), equalTo(-1));
    }

    /**
     * Test of setSequenceID method, of class Bitstream.
     */
    @Test
    public void testSetSequenceID()
    {
        int val = 2;
        bs.setSequenceID(val);
        assertThat("testSetSequenceID 0", bs.getSequenceID(), equalTo(val));
    }

    /**
     * Test of getName method, of class Bitstream.
     */
    @Override
    @Test
    public void testGetName()
    {
        //name is null when not set
        assertThat("testGetName 0", bs.getName(), nullValue());
    }

    /**
     * Test of setName method, of class Bitstream.
     */
    @Test
    public void testSetName() throws SQLException
    {
        String name = "new name";
        bs.setName(context, name);
        assertThat("testGetName 0", bs.getName(), notNullValue());
        assertThat("testGetName 1", bs.getName(), not(equalTo("")));
        assertThat("testGetName 2", bs.getName(), equalTo(name));
    }

    /**
     * Test of getSource method, of class Bitstream.
     */
    @Test
    public void testGetSource()
    {
        //source is null if not set
        assertThat("testGetSource 0", bs.getSource(), nullValue());
    }

    /**
     * Test of setSource method, of class Bitstream.
     */
    @Test
    public void testSetSource() throws SQLException
    {
        String source = "new source";
        bs.setSource(context, source);
        assertThat("testSetSource 0", bs.getSource(), notNullValue());
        assertThat("testSetSource 1", bs.getSource(), not(equalTo("")));
        assertThat("testSetSource 2", bs.getSource(), equalTo(source));
    }

    /**
     * Test of getDescription method, of class Bitstream.
     */
    @Test
    public void testGetDescription()
    {
        //default description is null if not set
        assertThat("testGetDescription 0", bs.getDescription(), nullValue());
    }

    /**
     * Test of setDescription method, of class Bitstream.
     */
    @Test
    public void testSetDescription() throws SQLException
    {
        String description = "new description";
        bs.setDescription(context, description);
        assertThat("testSetDescription 0", bs.getDescription(), notNullValue());
        assertThat("testSetDescription 1", bs.getDescription(), not(equalTo("")));
        assertThat("testSetDescription 2", bs.getDescription(), equalTo(description));
    }

    /**
     * Test of getChecksum method, of class Bitstream.
     */
    @Test
    public void testGetChecksum()
    {
        String checksum = "75a060bf6eb63fd0aad88b7d757728d3";
        assertThat("testGetChecksum 0", bs.getChecksum(), notNullValue());
        assertThat("testGetChecksum 1", bs.getChecksum(), not(equalTo("")));
        assertThat("testGetChecksum 2", bs.getChecksum(), equalTo(checksum));
    }

    /**
     * Test of getChecksumAlgorithm method, of class Bitstream.
     */
    @Test
    public void testGetChecksumAlgorithm()
    {
        String alg = "MD5";
        assertThat("testGetChecksumAlgorithm 0", bs.getChecksumAlgorithm(),
                notNullValue());
        assertThat("testGetChecksumAlgorithm 1", bs.getChecksumAlgorithm(),
                not(equalTo("")));
        assertThat("testGetChecksumAlgorithm 2", bs.getChecksumAlgorithm(),
                equalTo(alg));
    }

    /**
     * Test of getSizeBytes method, of class Bitstream.
     */
    @Test
    public void testGetSize()
    {
        long size = 238413;  // yuck, hardcoded!
        assertThat("testGetSize 0", bs.getSizeBytes(), equalTo(size));
    }

    /**
     * Test of setUserFormatDescription method, of class Bitstream.
     */
    @Test
    public void testSetUserFormatDescription() throws SQLException
    {
        String userdescription = "user format description";
        bs.setUserFormatDescription(context, userdescription);
        assertThat("testSetUserFormatDescription 0", bs.getUserFormatDescription()
                , notNullValue());
        assertThat("testSetUserFormatDescription 1", bs.getUserFormatDescription()
                , not(equalTo("")));
        assertThat("testSetUserFormatDescription 2", bs.getUserFormatDescription()
                , equalTo(userdescription));
    }

    /**
     * Test of getUserFormatDescription method, of class Bitstream.
     */
    @Test
    public void testGetUserFormatDescription()
    {
        //null by default if not set
        assertThat("testGetUserFormatDescription 0", bs.getUserFormatDescription()
                , nullValue());
    }

    /**
     * Test of getFormatDescription method, of class Bitstream.
     */
    @Test
    public void testGetFormatDescription() throws SQLException {
        //format is unknown by default
        String format = "Unknown";
        assertThat("testGetFormatDescription 0", bs.getFormatDescription(context),
                notNullValue());
        assertThat("testGetFormatDescription 1", bs.getFormatDescription(context),
                not(equalTo("")));
        assertThat("testGetFormatDescription 2", bs.getFormatDescription(context),
                equalTo(format));
    }

    /**
     * Test of getFormat method, of class Bitstream.
     */
    @Test
    public void testGetFormat() throws SQLException
    {
        assertThat("testGetFormat 0", bs.getFormat(context), notNullValue());
        assertThat("testGetFormat 1", bs.getFormat(context), equalTo(bitstreamFormatService.findUnknown(context)));
    }

    /**
     * Test of setFormat method, of class Bitstream.
     */
    @Test
    public void testSetFormat() throws SQLException
    {
        int id = 3;
        BitstreamFormat format = bitstreamFormatService.find(context, id);
        bs.setFormat(format);
        assertThat("testSetFormat 0", bs.getFormat(context), notNullValue());
        assertThat("testSetFormat 1", bs.getFormat(context), equalTo(bitstreamFormatService.find(context, id)));
    }

    /**
     * Test of update method, of class Bitstream.
     */
    @Test(expected=AuthorizeException.class)
    public void testUpdateNotAdmin() throws SQLException, AuthorizeException
    {
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Disallow Bitstream WRITE perms
                authorizeService.authorizeAction((Context) any, (Bitstream) any,
                    Constants.WRITE); result = new AuthorizeException();

        }};
        //TODO: we need to verify the update, how?
        bitstreamService.update(context, bs);
    }

    /**
     * Test of update method, of class Bitstream.
     */
    @Test
    public void testUpdateAdmin() throws SQLException, AuthorizeException
    {
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Allow Bitstream WRITE perms
                authorizeService.authorizeAction((Context) any, (Bitstream) any,
                    Constants.WRITE); result = null;

        }};
        
        //TODO: we need to verify the update, how?
        bitstreamService.update(context, bs);
    }

    /**
     * Test of delete method, of class Bitstream.
     */
    @Test
    public void testDeleteAndExpunge() throws IOException, SQLException, AuthorizeException
    {
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Allow Bitstream WRITE perms
                authorizeService.authorizeAction((Context) any, (Bitstream) any,
                    Constants.WRITE); result = null;
                authorizeService.authorizeAction((Context) any, (Bitstream) any,
                    Constants.DELETE); result = null;

        }};
        // Create a new bitstream, which we can delete. As ordering of these
        // tests is unpredictable we don't want to delete the global bitstream
        File f = new File(testProps.get("test.bitstream").toString());
        Bitstream delBS = bitstreamService.create(context, new FileInputStream(f));
        UUID bitstreamId = delBS.getID();

        // Test that delete will flag the bitstream as deleted
        assertFalse("testIsDeleted 0", delBS.isDeleted());
        bitstreamService.delete(context, delBS);
        assertTrue("testDelete 0", delBS.isDeleted());

        // Now test expunge actually removes the bitstream
        bitstreamService.expunge(context, delBS);
        assertThat("testExpunge 0", bitstreamService.find(context, bitstreamId), nullValue());
    }

    /**
     * Test of retrieve method, of class Bitstream.
     */
    @Test
    public void testRetrieveCanRead() throws IOException, SQLException,
            AuthorizeException
    {
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Allow Bitstream READ perms
                authorizeService.authorizeAction((Context) any, (Bitstream) any,
                    Constants.READ); result = null;
        }};

        assertThat("testRetrieveCanRead 0", bitstreamService.retrieve(context, bs), notNullValue());
    }

    /**
     * Test of retrieve method, of class Bitstream.
     */
    @Test(expected=AuthorizeException.class)
    public void testRetrieveNoRead() throws IOException, SQLException,
            AuthorizeException
    {
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Disallow Bitstream READ perms
            authorizeService.authorizeAction((Context) any, (Bitstream) any,
                    Constants.READ); result = new AuthorizeException();
        }};

        assertThat("testRetrieveNoRead 0", bitstreamService.retrieve(context, bs), notNullValue());
    }

    /**
     * Test of getBundles method, of class Bitstream.
     */
    @Test
    public void testGetBundles() throws SQLException
    {
        assertThat("testGetBundles 0", bs.getBundles(), notNullValue());
        //by default no bundles
        assertTrue("testGetBundles 1", bs.getBundles().size() == 0);
    }

    /**
     * Test of getType method, of class Bitstream.
     */
    @Override
    @Test
    public void testGetType()
    {
       assertThat("testGetType 0", bs.getType(), equalTo(Constants.BITSTREAM));
    }

    /**
     * Test of isRegisteredBitstream method, of class Bitstream.
     */
    @Test
    public void testIsRegisteredBitstream()
    {
        //false by default
        assertThat("testIsRegisteredBitstream 0", bitstreamService.isRegisteredBitstream(bs),
                equalTo(false));
    }

    /**
     * Test of getStoreNumber method, of class Bitstream.
     */
    @Test
    public void testGetStoreNumber() {
        //stored in store 0 by default
        assertTrue("testGetStoreNumber 0", bs.getStoreNumber() == 0);
    }

    /**
     * Test of getParentObject method, of class Bitstream.
     */
    @Test
    @Override
    public void testGetParentObject() throws SQLException
    {
        //by default this bitstream is not linked to any object
        assertThat("testGetParentObject 0", bitstreamService.getParentObject(context, bs),
                    nullValue());
    }
    
}
