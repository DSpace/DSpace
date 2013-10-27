/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import org.dspace.authorize.AuthorizeManager;
import mockit.NonStrictExpectations;
import java.sql.SQLException;
import org.dspace.AbstractUnitTest;
import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.junit.*;
import static org.junit.Assert.* ;
import static org.hamcrest.CoreMatchers.*;

/**
 * Unit Tests for class MetadataFieldTest
 * @author pvillega
 */
public class MetadataFieldTest extends AbstractUnitTest
{

    /** log4j category */
    private static final Logger log = Logger.getLogger(MetadataFieldTest.class);

    /**
     * MetadataField instance for the tests
     */
    private MetadataField mf;

    /**
     * Element of the metadata element
     */
    private String element = "contributor";

    /**
     * Qualifier of the metadata element
     */
    private String qualifier = "author";

    /**
     * Scope note of the metadata element
     */
    private String scopeNote = "scope note";

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
            this.mf = MetadataField.findByElement(context,
                    MetadataSchema.DC_SCHEMA_ID, element, qualifier);
            this.mf.setScopeNote(scopeNote);
            context.restoreAuthSystemState();
        }
        catch (AuthorizeException ex)
        {
            log.error("Authorize Error in init", ex);
            fail("Authorize Error in init: " + ex.getMessage());
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
        mf = null;
        super.destroy();
    }

    /**
     * Test of getElement method, of class MetadataField.
     */
    @Test
    public void testGetElement() 
    {
        assertThat("testGetElement 0",mf.getElement(), equalTo(element));
    }

    /**
     * Test of setElement method, of class MetadataField.
     */
    @Test
    public void testSetElement()
    {
        String elem = "newelem";
        mf.setElement(elem);
        assertThat("testSetElement 0",mf.getElement(), equalTo(elem));
    }

    /**
     * Test of getFieldID method, of class MetadataField.
     */
    @Test
    public void testGetFieldID()
    {
        assertTrue("testGetFieldID 0",mf.getFieldID() >= 0);
    }

    /**
     * Test of getQualifier method, of class MetadataField.
     */
    @Test
    public void testGetQualifier() 
    {
        assertThat("testGetQualifier 0",mf.getQualifier(), equalTo(qualifier));
    }

    /**
     * Test of setQualifier method, of class MetadataField.
     */
    @Test
    public void testSetQualifier()
    {
        String qual = "qualif";
        mf.setQualifier(qual);
        assertThat("testSetQualifier 0",mf.getQualifier(), equalTo(qual));
    }

    /**
     * Test of getSchemaID method, of class MetadataField.
     */
    @Test
    public void testGetSchemaID() 
    {
        assertThat("testGetSchemaID 0",mf.getSchemaID(), equalTo(MetadataSchema.DC_SCHEMA_ID));
    }

    /**
     * Test of setSchemaID method, of class MetadataField.
     */
    @Test
    public void testSetSchemaID()
    {
        int schemaID = 3;
        mf.setSchemaID(schemaID);
        assertThat("testSetSchemaID 0",mf.getSchemaID(), equalTo(schemaID));
    }

    /**
     * Test of getScopeNote method, of class MetadataField.
     */
    @Test
    public void testGetScopeNote()
    {
        assertThat("testGetScopeNote 0",mf.getScopeNote(), equalTo(scopeNote));
    }

    /**
     * Test of setScopeNote method, of class MetadataField.
     */
    @Test
    public void testSetScopeNote()
    {
        String scn = "new scope note";
        mf.setScopeNote(scn);
        assertThat("testSetScopeNote 0",mf.getScopeNote(), equalTo(scn));
    }

    /**
     * Test of create method, of class MetadataField.
     */
    @Test
    public void testCreateAuth() throws Exception
    {
        new NonStrictExpectations()
        {
            AuthorizeManager authManager;
            {
                AuthorizeManager.isAdmin(context); result = true;
            }
        };

        String elem = "elem1";
        String qual = "qual1";
        MetadataField m = new MetadataField();
        m.setSchemaID(MetadataSchema.DC_SCHEMA_ID);
        m.setElement(elem);
        m.setQualifier(qual);
        m.create(context);

        MetadataField found = MetadataField.findByElement(context, MetadataSchema.DC_SCHEMA_ID, elem, qual);
        assertThat("testCreateAuth 0",found.getFieldID(), equalTo(m.getFieldID()));
    }

    /**
     * Test of create method, of class MetadataField.
     */
    @Test(expected=AuthorizeException.class)
    public void testCreateNoAuth() throws Exception
    {
        new NonStrictExpectations()
        {
            AuthorizeManager authManager;
            {
                AuthorizeManager.isAdmin(context); result = false;
            }
        };

        String elem = "elem1";
        String qual = "qual1";
        MetadataField m = new MetadataField();
        m.setSchemaID(MetadataSchema.DC_SCHEMA_ID);
        m.setElement(elem);
        m.setQualifier(qual);
        m.create(context);
        fail("Exception expected");
    }

    /**
     * Test of create method, of class MetadataField.
     */
    @Test(expected=NonUniqueMetadataException.class)
    public void testCreateRepeated() throws Exception
    {
        new NonStrictExpectations()
        {
            AuthorizeManager authManager;
            {
                AuthorizeManager.isAdmin(context); result = true;
            }
        };

        String elem = element;
        String qual = qualifier;
        MetadataField m = new MetadataField();
        m.setSchemaID(MetadataSchema.DC_SCHEMA_ID);
        m.setElement(elem);
        m.setQualifier(qual);
        m.create(context);
        fail("Exception expected");
    }

    /**
     * Test of findByElement method, of class MetadataField.
     */
    @Test
    public void testFindByElement() throws Exception
    {
        MetadataField found = MetadataField.findByElement(context, MetadataSchema.DC_SCHEMA_ID, element, qualifier);
        assertThat("testFindByElement 0",found, notNullValue());
        assertThat("testFindByElement 1",found.getFieldID(), equalTo(mf.getFieldID()));
        assertThat("testFindByElement 2",found.getElement(), equalTo(mf.getElement()));
        assertThat("testFindByElement 3",found.getQualifier(), equalTo(mf.getQualifier()));        
    }

    /**
     * Test of findAll method, of class MetadataField.
     */
    @Test
    public void testFindAll() throws Exception
    {
        MetadataField[] found = MetadataField.findAll(context);
        assertThat("testFindAll 0",found, notNullValue());
        assertTrue("testFindAll 1",found.length >= 1);

        boolean added = false;
        for(MetadataField mdf: found)
        {
            if(mdf.equals(mf))
            {
                added = true;
            }
        }        
        assertTrue("testFindAll 2",added);
    }

    /**
     * Test of findAllInSchema method, of class MetadataField.
     */
    @Test
    public void testFindAllInSchema() throws Exception 
    {
        MetadataField[] found = MetadataField.findAllInSchema(context, MetadataSchema.DC_SCHEMA_ID);
        assertThat("testFindAllInSchema 0",found, notNullValue());
        assertTrue("testFindAllInSchema 1",found.length >= 1);
        assertTrue("testFindAllInSchema 2",found.length <= MetadataField.findAll(context).length);

        boolean added = false;
        for(MetadataField mdf: found)
        {
            if(mdf.equals(mf))
            {
                added = true;
            }
        }        
        assertTrue("testFindAllInSchema 3",added);
    }

    /**
     * Test of update method, of class MetadataField.
     */
    @Test
    public void testUpdateAuth() throws Exception
    {
        new NonStrictExpectations()
        {
            AuthorizeManager authManager;
            {
                AuthorizeManager.isAdmin(context); result = true;
            }
        };

        String elem = "elem2";
        String qual = "qual2";
        MetadataField m = new MetadataField();
        m.setSchemaID(MetadataSchema.DC_SCHEMA_ID);
        m.setElement(elem);
        m.setQualifier(qual);
        m.create(context);
        m.update(context);

        MetadataField found = MetadataField.findByElement(context, MetadataSchema.DC_SCHEMA_ID, elem, qual);
        assertThat("testUpdateAuth 0",found.getFieldID(), equalTo(m.getFieldID()));
    }

    /**
     * Test of update method, of class MetadataField.
     */
    @Test(expected=AuthorizeException.class)
    public void testUpdateNoAuth() throws Exception
    {
        new NonStrictExpectations()
        {
            AuthorizeManager authManager;
            {
                AuthorizeManager.isAdmin(context); result = false;
            }
        };

        String elem = "elem2";
        String qual = "qual2";
        MetadataField m = new MetadataField();
        m.setSchemaID(MetadataSchema.DC_SCHEMA_ID);
        m.setElement(elem);
        m.setQualifier(qual);
        m.update(context);
        fail("Exception expected");
    }

    /**
     * Test of update method, of class MetadataField.
     */
    @Test(expected=NonUniqueMetadataException.class)
    public void testUpdateRepeated() throws Exception
    {
        new NonStrictExpectations()
        {
            AuthorizeManager authManager;
            {
                AuthorizeManager.isAdmin(context); result = true;
            }
        };

        String elem = element;
        String qual = qualifier;
        MetadataField m = new MetadataField();
        m.setSchemaID(MetadataSchema.DC_SCHEMA_ID);
        m.create(context);        
        
        m.setElement(elem);
        m.setQualifier(qual);
        m.update(context);
        fail("Exception expected");
    }

    /**
     * Test of delete method, of class MetadataField.
     */
    @Test
    public void testDeleteAuth() throws Exception
    {
        new NonStrictExpectations()
        {
            AuthorizeManager authManager;
            {
                AuthorizeManager.isAdmin(context); result = true;
            }
        };

        String elem = "elem3";
        String qual = "qual3";
        MetadataField m = new MetadataField();
        m.setSchemaID(MetadataSchema.DC_SCHEMA_ID);
        m.setElement(elem);
        m.setQualifier(qual);
        m.create(context);
        context.commit();

        m.delete(context);

        MetadataField found = MetadataField.findByElement(context, MetadataSchema.DC_SCHEMA_ID, elem, qual);
        assertThat("testDeleteAuth 0",found, nullValue());
    }

    /**
     * Test of delete method, of class MetadataField.
     */
    @Test(expected=AuthorizeException.class)
    public void testDeleteNoAuth() throws Exception
    {
        new NonStrictExpectations()
        {
            AuthorizeManager authManager;
            {
                AuthorizeManager.isAdmin(context); result = false;
            }
        };

        String elem = "elem3";
        String qual = "qual3";
        MetadataField m = new MetadataField();
        m.setSchemaID(MetadataSchema.DC_SCHEMA_ID);
        m.setElement(elem);
        m.setQualifier(qual);
        m.create(context);
        context.commit();

        m.delete(context);
        fail("Exception expected");
    }

    /**
     * Test of formKey method, of class MetadataField.
     */
    @Test
    public void testFormKey()
    {
        assertThat("testFormKey 0",MetadataField.formKey("dc", "elem", null), equalTo("dc_elem"));
        assertThat("testFormKey 1",MetadataField.formKey("dc", "elem", "qual"), equalTo("dc_elem_qual"));
    }

    /**
     * Test of find method, of class MetadataField.
     */
    @Test
    public void testFind() throws Exception
    {
        context.turnOffAuthorisationSystem();

        mf.update(context);
        int id = mf.getFieldID();
        
        MetadataField found = MetadataField.find(context, id);
        assertThat("testFind 0",found, notNullValue());
        assertThat("testFind 1",found.getFieldID(), equalTo(mf.getFieldID()));
        context.restoreAuthSystemState();
    }

}