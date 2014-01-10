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
import org.dspace.core.Context;
import org.junit.*;
import static org.junit.Assert.* ;
import static org.hamcrest.CoreMatchers.*;
import mockit.*;
import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.core.Constants;

/**
 * Unit Tests for class Bitstream
 * @author pvillega
 */
public class BitstreamTest extends AbstractDSpaceObjectTest
{
    /** log4j category */
    private static final Logger log = Logger.getLogger(BitstreamTest.class);

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
            this.bs = Bitstream.create(context, new FileInputStream(f));            
            this.dspaceObject = bs;   
            //we need to commit the changes so we don't block the table for testing
            context.commit();
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
        int id = 1;
        Bitstream found =  Bitstream.find(context, id);
        assertThat("testBSFind 0", found, notNullValue());
        //the item created by default has no name nor type set
        assertThat("testBSFind 1", found.getFormat().getMIMEType(), equalTo("application/octet-stream"));
        assertThat("testBSFind 2", found.getName(), nullValue());
        assertThat("testBSFind 3", found.getID(), equalTo(id));
    }

    /**
     * Test of findAll method, of class Bitstream.
     */
    @Test
    public void testFindAll() throws SQLException
    {
        Bitstream[] found =  Bitstream.findAll(context);
        assertThat("testFindAll 0", found, notNullValue());
        //we have many bs, one created per test run, so at least we have 1 if
        //this test is run first
        assertTrue("testFindAll 1", found.length >= 1);

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
        Bitstream created = Bitstream.create(context, new FileInputStream(f));
        context.commit();
        
        //the item created by default has no name nor type set
        assertThat("testCreate 0", created.getFormat().getMIMEType(), equalTo("application/octet-stream"));
        assertThat("testCreate 1", created.getName(), nullValue());
    }

    /**
     * Test of register method, of class Bitstream.
     */
    @Test
    public void testRegister() throws IOException, SQLException
    {
        int assetstore = 0;
        File f = new File(testProps.get("test.bitstream").toString());
        Bitstream registered = Bitstream.register(context,assetstore, f.getName());
        //the item created by default has no name nor type set
        assertThat("testRegister 0", registered.getFormat().getMIMEType(), equalTo("application/octet-stream"));
        assertThat("testRegister 1", registered.getName(), nullValue());
    }

    /**
     * Test of getID method, of class Bitstream.
     */
    @Test
    public void testGetID()
    {
        assertTrue("testGetID 0", bs.getID() >= 0);
    }

    /**
     * Test of getHandle method, of class Bitstream.
     */
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
    public void testSetName()
    {
        String name = "new name";
        bs.setName(name);
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
    public void testSetSource()
    {
        String source = "new source";
        bs.setSource(source);
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
    public void testSetDescription()
    {
        String description = "new description";
        bs.setDescription(description);
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
     * Test of getSize method, of class Bitstream.
     */
    @Test
    public void testGetSize()
    {
        long size = 238413;
        assertThat("testGetSize 0", bs.getSize(), equalTo(size));
    }

    /**
     * Test of setUserFormatDescription method, of class Bitstream.
     */
    @Test
    public void testSetUserFormatDescription() throws SQLException
    {
        String userdescription = "user format description";
        bs.setUserFormatDescription(userdescription);
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
    public void testGetFormatDescription()
    {
        //format is unknown by default
        String format = "Unknown";
        assertThat("testGetFormatDescription 0", bs.getFormatDescription(),
                notNullValue());
        assertThat("testGetFormatDescription 1", bs.getFormatDescription(),
                not(equalTo("")));
        assertThat("testGetFormatDescription 2", bs.getFormatDescription(),
                equalTo(format));
    }

    /**
     * Test of getFormat method, of class Bitstream.
     */
    @Test
    public void testGetFormat() throws SQLException
    {
        assertThat("testGetFormat 0", bs.getFormat(), notNullValue());
        assertThat("testGetFormat 1", bs.getFormat(), equalTo(BitstreamFormat.findUnknown(context)));
    }

    /**
     * Test of setFormat method, of class Bitstream.
     */
    @Test
    public void testSetFormat() throws SQLException
    {
        int id = 3;
        BitstreamFormat format = BitstreamFormat.find(context, id);
        bs.setFormat(format);
        assertThat("testSetFormat 0", bs.getFormat(), notNullValue());
        assertThat("testSetFormat 1", bs.getFormat(), equalTo(BitstreamFormat.find(context, id)));
    }

    /**
     * Test of update method, of class Bitstream.
     */
    @Test(expected=AuthorizeException.class)
    public void testUpdateNotAdmin() throws SQLException, AuthorizeException
    {

        new NonStrictExpectations()
        {
            AuthorizeManager authManager;
            {
                AuthorizeManager.authorizeAction((Context) any, (Bitstream) any,
                        Constants.WRITE); result = new AuthorizeException();
            }
        };
        //TODO: we need to verify the update, how?
        bs.update();
    }

    /**
     * Test of update method, of class Bitstream.
     */
    @Test
    public void testUpdateAdmin() throws SQLException, AuthorizeException
    {

        new NonStrictExpectations()
        {
            AuthorizeManager authManager;
            {
                AuthorizeManager.authorizeAction((Context) any, (Bitstream) any,
                        Constants.WRITE); result = null;
            }
        };
        //TODO: we need to verify the update, how?
        bs.update();
    }

    /**
     * Test of delete method, of class Bitstream.
     */
    @Test
    public void testDelete() throws SQLException, AuthorizeException
    {      
        bs.delete();
        assertTrue("testDelete 0", bs.isDeleted());
    }

    /**
     * Test of isDeleted method, of class Bitstream.
     */
    @Test
    public void testIsDeleted() throws SQLException, AuthorizeException
    {
        assertFalse("testIsDeleted 0", bs.isDeleted());
        bs.delete();        
        assertTrue("testIsDeleted 1", bs.isDeleted());
    }


    /**
     * Test of retrieve method, of class Bitstream.
     */
    @Test
    public void testRetrieveCanRead() throws IOException, SQLException,
            AuthorizeException
    {
        new NonStrictExpectations()
        {
            AuthorizeManager authManager;
            {
                AuthorizeManager.authorizeAction((Context) any, (Bitstream) any,
                        Constants.READ); result = null;
            }
        };

        assertThat("testRetrieveCanRead 0", bs.retrieve(), notNullValue());
    }

    /**
     * Test of retrieve method, of class Bitstream.
     */
    @Test(expected=AuthorizeException.class)
    public void testRetrieveNoRead() throws IOException, SQLException,
            AuthorizeException
    {
        new NonStrictExpectations()
        {
            AuthorizeManager authManager;
            {
                AuthorizeManager.authorizeAction((Context) any, (Bitstream) any,
                        Constants.READ); result = new AuthorizeException();
            }
        };

        assertThat("testRetrieveNoRead 0", bs.retrieve(), notNullValue());
    }

    /**
     * Test of getBundles method, of class Bitstream.
     */
    @Test
    public void testGetBundles() throws SQLException
    {
        assertThat("testGetBundles 0", bs.getBundles(), notNullValue());
        //by default no bundles
        assertTrue("testGetBundles 1", bs.getBundles().length == 0);
    }

    /**
     * Test of getType method, of class Bitstream.
     */
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
        assertThat("testIsRegisteredBitstream 0", bs.isRegisteredBitstream(),
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
        assertThat("testGetParentObject 0", bs.getParentObject(),
                    nullValue());
    }
    
}
