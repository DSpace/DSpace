/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import java.sql.SQLException;
import mockit.NonStrictExpectations;
import org.apache.log4j.Logger;
import org.dspace.core.Context;
import org.junit.*;
import static org.junit.Assert.* ;
import static org.hamcrest.CoreMatchers.*;
import org.dspace.AbstractUnitTest;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;

/**
 * This class tests BitstreamFormat. Due to it being tighly coupled with the
 * database, most of the methods use mock objects, which only proved a very
 * basic test level (ensure the method doesn't throw an exception). The real
 * testing of the class will be done in the Integration Tests.
 * @author pvillega
 */
public class BitstreamFormatTest extends AbstractUnitTest
{
    /** log4j category */
    private final static Logger log = Logger.getLogger(BitstreamFormatTest.class);

    /**
     * Object to use in the tests
     */
    private BitstreamFormat bf;
        
    /**
     * Object to use in the tests
     */
    private BitstreamFormat bunknown;
    

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
            bf =  BitstreamFormat.find(context, 5);            
            bunknown = BitstreamFormat.findUnknown(context);
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
        bf = null;
        bunknown = null;
        super.destroy();
    }

    /**
     * Test of find method, of class BitstreamFormat.
     */
    @Test
    public void testFind() throws SQLException
    {
        BitstreamFormat found =  BitstreamFormat.find(context, 1);
        assertThat("testFind 0", found, notNullValue());
        assertThat("testFind 1", found.getShortDescription(), equalTo("Unknown"));

        found =  BitstreamFormat.find(context, 2);
        assertThat("testFind 2", found, notNullValue());
        assertThat("testFind 3", found.getShortDescription(), equalTo("License"));
        assertTrue("testFind 4", found.isInternal());
    }

    /**
     * Test of findByMIMEType method, of class BitstreamFormat.
     */
    @Test
    public void testFindByMIMEType() throws SQLException
    {
        BitstreamFormat found =  BitstreamFormat.findByMIMEType(context, "text/plain");
        assertThat("testFindByMIMEType 0", found, notNullValue());
        assertThat("testFindByMIMEType 1", found.getMIMEType(), equalTo("text/plain"));
        assertFalse("testFindByMIMEType 2", found.isInternal());

        found =  BitstreamFormat.findByMIMEType(context, "text/xml");
        assertThat("testFindByMIMEType 3", found, notNullValue());
        assertThat("testFindByMIMEType 4", found.getMIMEType(), equalTo("text/xml"));
        assertFalse("testFindByMIMEType 5", found.isInternal());
    }

    /**
     * Test of findByShortDescription method, of class BitstreamFormat.
     */
    @Test
    public void testFindByShortDescription() throws SQLException
    {
        BitstreamFormat found =  BitstreamFormat.findByShortDescription(context, "Adobe PDF");
        assertThat("testFindByShortDescription 0", found, notNullValue());
        assertThat("testFindByShortDescription 1", found.getShortDescription(), equalTo("Adobe PDF"));
        assertFalse("testFindByShortDescription 2", found.isInternal());

        found =  BitstreamFormat.findByShortDescription(context, "XML");
        assertThat("testFindByShortDescription 3", found, notNullValue());
        assertThat("testFindByShortDescription 4", found.getShortDescription(), equalTo("XML"));
        assertFalse("testFindByShortDescription 5", found.isInternal());
    }

    /**
     * Test of findUnknown method, of class BitstreamFormat.
     */
    @Test
    public void testFindUnknown()
            throws SQLException
    {
        BitstreamFormat found =  BitstreamFormat.findUnknown(context);
        assertThat("testFindUnknown 0", found, notNullValue());
        assertThat("testFindUnknown 1", found.getShortDescription(), equalTo("Unknown"));
        assertFalse("testFindUnknown 2", found.isInternal());
        assertThat("testFindUnknown 3", found.getSupportLevel(), equalTo(0));
    }

    /**
     * Test of findAll method, of class BitstreamFormat.
     */
    @Test
    public void testFindAll() throws SQLException
    {
        
        BitstreamFormat[] found =  BitstreamFormat.findAll(context);
        assertThat("testFindAll 0", found, notNullValue());

        //check pos 0 is Unknown
        assertThat("testFindAll 1", found[0].getShortDescription(), equalTo("Unknown"));
        assertFalse("testFindAll 2", found[0].isInternal());
        assertThat("testFindAll 3", found[0].getSupportLevel(), equalTo(0));

        boolean added = false;
        for(BitstreamFormat bsf: found)
        {
            if(bsf.equals(bf))
            {
                added = true;
            }
        }
        assertTrue("testFindAll 4",added);
    }

    /**
     * Test of findNonInternal method, of class BitstreamFormat.
     */
    @Test
    public void testFindNonInternal() throws SQLException
    {

        BitstreamFormat[] found =  BitstreamFormat.findNonInternal(context);
        assertThat("testFindNonInternal 0", found, notNullValue());
        int i = 0;
        for(BitstreamFormat b: found)
        {
            i++;
            assertFalse("testFindNonInternal "+i+" ("+b.getShortDescription()+")", b.isInternal());
        }
    }

    /**
     * Test of create method, of class BitstreamFormat.
     */
    @Test
    public void testCreateAdmin() throws SQLException,AuthorizeException
    {
        new NonStrictExpectations()
        {
            AuthorizeManager authManager;
            {
                AuthorizeManager.isAdmin((Context)any); result = true;
            }
        };
        
        BitstreamFormat found =  BitstreamFormat.create(context);
        assertThat("testCreate 0", found, notNullValue());
        assertThat("testCreate 1", found.getDescription(), nullValue());
        assertThat("testCreate 2", found.getMIMEType(), nullValue());
        assertThat("testCreate 3", found.getSupportLevel(), equalTo(-1));
        assertFalse("testCreate 4", found.isInternal());
    }

    /**
     * Test of create method, of class BitstreamFormat.
     */
    @Test(expected=AuthorizeException.class)
    public void testCreateNotAdmin() throws SQLException,AuthorizeException
    {
        new NonStrictExpectations()
        {
            AuthorizeManager authManager;
            {
                AuthorizeManager.isAdmin((Context)any); result = false;
            }
        };

        BitstreamFormat found =  BitstreamFormat.create(context);
        fail("Exception should have been thrown");
    }

    /**
     * Test of getID method, of class BitstreamFormat.
     */
    @Test
    public void testGetID()
    {
        assertTrue("testGetID 0", bf.getID() == 5);
        assertTrue("testGetID 1", bunknown.getID() == 1);
    }

    /**
     * Test of getShortDescription method, of class BitstreamFormat.
     */
    @Test
    public void testGetShortDescription()
    {
        assertThat("getShortDescription 0", bf.getShortDescription(),
                notNullValue());
        assertThat("getShortDescription 1", bf.getShortDescription(),
                not(equalTo("")));
        assertThat("getShortDescription 2", bf.getShortDescription(),
                equalTo("XML"));
    }

    /**
     * Test of setShortDescription method, of class BitstreamFormat.
     */
    @Test
    public void testSetShortDescription() throws SQLException
    {
        String desc = "short";
        bf.setShortDescription(desc);

        assertThat("testSetShortDescription 0", bf.getShortDescription(),
                notNullValue());
        assertThat("testSetShortDescription 1", bf.getShortDescription(),
                not(equalTo("")));
        assertThat("testSetShortDescription 2", bf.getShortDescription(),
                equalTo(desc));
    }

    /**
     * Test of getDescription method, of class BitstreamFormat.
     */
    @Test
    public void testGetDescription()
    {
        assertThat("getDescription 0", bf.getDescription(),
                notNullValue());
        assertThat("getDescription 1", bf.getDescription(),
                not(equalTo("")));
        assertThat("getDescription 2", bf.getDescription(),
                equalTo("Extensible Markup Language"));
    }

    /**
     * Test of setDescription method, of class BitstreamFormat.
     */
    @Test
    public void testSetDescription()
    {
        String desc = "long description stored here";
        bf.setDescription(desc);

        assertThat("testSetDescription 0", bf.getDescription(),
                notNullValue());
        assertThat("testSetDescription 1", bf.getDescription(),
                not(equalTo("")));
        assertThat("testSetDescription 2", bf.getDescription(),
                equalTo(desc));
    }

    /**
     * Test of getMIMEType method, of class BitstreamFormat.
     */
    @Test
    public void testGetMIMEType()
    {
        assertThat("testGetMIMEType 0", bf.getMIMEType(),
                notNullValue());
        assertThat("testGetMIMEType 1", bf.getMIMEType(),
                not(equalTo("")));
        assertThat("testGetMIMEType 2", bf.getMIMEType(),
                equalTo("text/xml"));
    }

    /**
     * Test of setMIMEType method, of class BitstreamFormat.
     */
    @Test
    public void testSetMIMEType()
    {
        String mime = "text/plain";
        bf.setMIMEType(mime);

        assertThat("testSetMIMEType 0", bf.getMIMEType(),
                notNullValue());
        assertThat("testSetMIMEType 1", bf.getMIMEType(),
                not(equalTo("")));
        assertThat("testSetMIMEType 2", bf.getMIMEType(),
                equalTo(mime));
    }

    /**
     * Test of getSupportLevel method, of class BitstreamFormat.
     */
    @Test
    public void testGetSupportLevel() throws SQLException
    {

        assertTrue("testGetSupportLevel 0", bf.getSupportLevel() >= 0);
        assertTrue("testGetSupportLevel 1", bf.getSupportLevel() <= 2);
        
        assertTrue("testGetSupportLevel 2", bunknown.getSupportLevel() >= 0);
        assertTrue("testGetSupportLevel 3", bunknown.getSupportLevel() <= 2);

        BitstreamFormat[] found =  BitstreamFormat.findAll(context);
        int i = 0;
        for(BitstreamFormat b: found)
        {
            i++;
            assertTrue("testGetSupportLevel "+i+" ("+b.getMIMEType()+")", b.getSupportLevel() >= 0);
            i++;
            assertTrue("testGetSupportLevel "+i+" ("+b.getMIMEType()+")", b.getSupportLevel() <= 2);
        }
    }

    /**
     * Test of setSupportLevel method, of class BitstreamFormat.
     */
    @Test(expected=IllegalArgumentException.class)
    public void testSetSupportLevelInvalidValue()
    {
        bf.setSupportLevel(5);
        fail("Exception should be thrown");
    }

    /**
     * Test of setSupportLevel method, of class BitstreamFormat.
     */
    @Test(expected=IllegalArgumentException.class)
    public void testSetSupportLevelNegativeValue()
    {
        bf.setSupportLevel(-1);
        fail("Exception should be thrown");
    }

    /**
     * Test of setSupportLevel method, of class BitstreamFormat.
     */
    @Test
    public void testSetSupportLevelValidValues()
    {
        bf.setSupportLevel(BitstreamFormat.UNKNOWN);
        assertThat("testSetSupportLevelValidValues 0", bf.getSupportLevel(), equalTo(BitstreamFormat.UNKNOWN));
        bf.setSupportLevel(BitstreamFormat.KNOWN);
        assertThat("testSetSupportLevelValidValues 1", bf.getSupportLevel(), equalTo(BitstreamFormat.KNOWN));
        bf.setSupportLevel(BitstreamFormat.SUPPORTED);
        assertThat("testSetSupportLevelValidValues 2", bf.getSupportLevel(), equalTo(BitstreamFormat.SUPPORTED));
    }


    /**
     * Test of getSupportLevelID method, of class BitstreamFormat.
     */
    @Test
    public void testGetSupportLevelIDValid()
    {
        int id1 = BitstreamFormat.getSupportLevelID("UNKNOWN");
        assertThat("testGetSupportLevelIDValid 0", id1, equalTo(BitstreamFormat.UNKNOWN));
        int id2 = BitstreamFormat.getSupportLevelID("KNOWN");
        assertThat("testGetSupportLevelIDValid 1", id2, equalTo(BitstreamFormat.KNOWN));
        int id3 = BitstreamFormat.getSupportLevelID("SUPPORTED");
        assertThat("testGetSupportLevelIDValid 2", id3, equalTo(BitstreamFormat.SUPPORTED));
    }

    /**
     * Test of getSupportLevelID method, of class BitstreamFormat.
     */
    @Test
    public void testGetSupportLevelIDInvalid()
    {
        int id1 = BitstreamFormat.getSupportLevelID("IAmNotAValidSupportLevel");
        assertThat("testGetSupportLevelIDInvalid 0", id1, equalTo(-1));
    }


    /**
     * Test of isInternal method, of class BitstreamFormat.
     */
    @Test
    public void testIsInternal() throws SQLException
    {
        assertThat("testIsInternal 0", bf.isInternal(), equalTo(false));

        BitstreamFormat found = BitstreamFormat.findByShortDescription(context, "License");
        assertThat("testIsInternal 1", found.isInternal(), equalTo(true));

        found = BitstreamFormat.findByShortDescription(context, "CC License");
        assertThat("testIsInternal 2", found.isInternal(), equalTo(true));

        assertThat("testIsInternal 3", bunknown.isInternal(), equalTo(false));
    }

    /**
     * Test of setInternal method, of class BitstreamFormat.
     */
    @Test
    public void testSetInternal()
    {
        assertFalse("testSetInternal 0", bf.isInternal());

        bf.setInternal(true);

        assertThat("testSetInternal 1", bf.isInternal(), equalTo(true));
    }

    /**
     * Test of update method, of class BitstreamFormat.
     */
    @Test(expected=AuthorizeException.class)
    public void testUpdateNotAdmin() throws SQLException, AuthorizeException
    {

        new NonStrictExpectations()
        {
            AuthorizeManager authManager;
            {
                AuthorizeManager.isAdmin((Context)any); result = false;
            }
        };

        bf.update();
        fail("Exception should have been thrown");
    }

    /**
     * Test of update method, of class BitstreamFormat.
     */
    @Test
    public void testUpdateAdmin() throws SQLException, AuthorizeException
    {

        new NonStrictExpectations()
        {
            AuthorizeManager authManager;
            {
                AuthorizeManager.isAdmin((Context)any); result = true;
            }
        };

        String desc = "Test description";
        bf.setDescription(desc);
        bf.update();

        BitstreamFormat b =  BitstreamFormat.find(context, 5);
        assertThat("testUpdateAdmin 0", b.getDescription(), equalTo(desc));
    }

    /**
     * Test of delete method, of class BitstreamFormat.
     */
    @Test(expected=AuthorizeException.class)
    public void testDeleteNotAdmin() throws SQLException, AuthorizeException
    {

        new NonStrictExpectations()
        {
            AuthorizeManager authManager;
            {
                AuthorizeManager.isAdmin((Context)any); result = false;
            }
        };

        bf.delete();
        fail("Exception should have been thrown");
    }

    /**
     * Test of delete method, of class BitstreamFormat.
     */
    @Test
    public void testDeleteAdmin() throws SQLException, AuthorizeException
    {

        new NonStrictExpectations()
        {
            AuthorizeManager authManager;
            BitstreamFormat unknown;
            {
                AuthorizeManager.isAdmin((Context)any); result = true;                
            }
        };

        bf.delete();
        BitstreamFormat b =  BitstreamFormat.find(context, 5);
        assertThat("testDeleteAdmin 0", b, nullValue());
    }

    /**
     * Test of delete method, of class BitstreamFormat.
     */
    @Test(expected=IllegalArgumentException.class)
    public void testDeleteUnknown() throws SQLException, AuthorizeException
    {

        new NonStrictExpectations()
        {
            AuthorizeManager authManager;
            {
                AuthorizeManager.isAdmin((Context)any); result = true;
            }
        };
        
        bunknown.delete();
        fail("Exception should have been thrown");
    }

    /**
     * Test of getExtensions method, of class BitstreamFormat.
     */
    @Test
    public void testGetExtensions()
    {
        assertThat("testGetExtensions 0", bf.getExtensions(), notNullValue());
        assertTrue("testGetExtensions 1", bf.getExtensions().length == 1);
        assertThat("testGetExtensions 2", bf.getExtensions()[0], equalTo("xml"));
    }

    /**
     * Test of setExtensions method, of class BitstreamFormat.
     */
    @Test
    public void setExtensions(String[] exts)
    {
        assertThat("setExtensions 0", bf.getExtensions()[0], equalTo("xml"));

        bf.setExtensions(new String[]{"1", "2", "3"});
        assertThat("setExtensions 1", bf.getExtensions(), notNullValue());
        assertTrue("setExtensions 2", bf.getExtensions().length == 3);
        assertThat("setExtensions 3", bf.getExtensions()[0], equalTo("1"));
        assertThat("setExtensions 4", bf.getExtensions()[1], equalTo("2"));
        assertThat("setExtensions 5", bf.getExtensions()[2], equalTo("3"));

        bf.setExtensions(new String[0]);
        assertThat("setExtensions 6", bf.getExtensions(), notNullValue());
        assertTrue("setExtensions 7", bf.getExtensions().length == 0);
    }
}