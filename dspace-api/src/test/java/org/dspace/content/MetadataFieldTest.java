/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import mockit.NonStrictExpectations;
import org.apache.log4j.Logger;
import org.dspace.AbstractUnitTest;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.MetadataFieldService;
import org.dspace.content.service.MetadataSchemaService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.sql.SQLException;
import java.util.List;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

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
    
    private MetadataSchema dcSchema;

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

    protected MetadataSchemaService metadataSchemaService = ContentServiceFactory.getInstance().getMetadataSchemaService();
    protected MetadataFieldService metadataFieldService = ContentServiceFactory.getInstance().getMetadataFieldService();

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
            this.dcSchema = metadataSchemaService.find(context, MetadataSchema.DC_SCHEMA);
            this.mf = metadataFieldService.findByElement(context,
                    MetadataSchema.DC_SCHEMA, element, qualifier);
            if(mf == null)
            {
                context.turnOffAuthorisationSystem();
                this.mf = metadataFieldService.create(context, metadataSchemaService.find(context, MetadataSchema.DC_SCHEMA), element, qualifier, scopeNote);
                context.restoreAuthSystemState();
            }

            this.mf.setScopeNote(scopeNote);
        }
        catch (SQLException ex)
        {
            log.error("SQL Error in init", ex);
            fail("SQL Error in init: " + ex.getMessage());
        } catch (NonUniqueMetadataException ex) {
            log.error("NonUniqueMetadataException Error in init", ex);
            fail("NonUniqueMetadataException Error in init: " + ex.getMessage());
        } catch (AuthorizeException ex) {
            log.error("AuthorizeException Error in init", ex);
            fail("AuthorizeException Error in init: " + ex.getMessage());
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
        assertTrue("testGetFieldID 0",mf.getID() >= 0);
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
    public void testGetSchema()
    {
        assertThat("testGetSchemaID 0",mf.getMetadataSchema().getName(), equalTo(MetadataSchema.DC_SCHEMA));
    }

    /**
     * Test of setSchemaID method, of class MetadataField.
     */
    @Test
    public void testSetSchema() throws NonUniqueMetadataException, SQLException, AuthorizeException
    {
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Allow full admin permissions
            authorizeService.isAdmin(context); result = true;
        }};
        MetadataSchema newSchema = metadataSchemaService.create(context, "testSetSchema", "testSetSchemaNS");
        mf.setMetadataSchema(newSchema);
        assertThat("testSetSchemaID 0",mf.getMetadataSchema(), equalTo(newSchema));
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
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Allow full admin permissions
            authorizeService.isAdmin(context); result = true;
        }};

        String elem = "elem1";
        String qual = "qual1";
        MetadataField m = metadataFieldService.create(context, dcSchema, elem, qual, null);

        MetadataField found = metadataFieldService.findByElement(context, dcSchema, elem, qual);
        assertThat("testCreateAuth 0",found.getID(), equalTo(m.getID()));
    }

    /**
     * Test of create method, of class MetadataField.
     */
    @Test(expected=AuthorizeException.class)
    public void testCreateNoAuth() throws Exception
    {
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Disallow full admin permissions
            authorizeService.isAdmin(context); result = false;
        }};

        String elem = "elem1";
        String qual = "qual1";
        metadataFieldService.create(context, dcSchema, elem, qual, null);
        fail("Exception expected");
    }

    /**
     * Test of create method, of class MetadataField.
     */
    @Test(expected=NonUniqueMetadataException.class)
    public void testCreateRepeated() throws Exception
    {
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Allow full admin permissions
            authorizeService.isAdmin(context); result = true;
        }};

        String elem = element;
        String qual = qualifier;
        metadataFieldService.create(context, dcSchema, elem, qual, null);
        fail("Exception expected");
    }

    /**
     * Test of findByElement method, of class MetadataField.
     */
    @Test
    public void testFindByElement() throws Exception
    {
        MetadataField found = metadataFieldService.findByElement(context, MetadataSchema.DC_SCHEMA, element, qualifier);
        assertThat("testFindByElement 0",found, notNullValue());
        assertThat("testFindByElement 1",found.getID(), equalTo(mf.getID()));
        assertThat("testFindByElement 2",found.getElement(), equalTo(mf.getElement()));
        assertThat("testFindByElement 3",found.getQualifier(), equalTo(mf.getQualifier()));        
    }

    /**
     * Test of findAll method, of class MetadataField.
     */
    @Test
    public void testFindAll() throws Exception
    {
        List<MetadataField> found = metadataFieldService.findAll(context);
        assertThat("testFindAll 0",found, notNullValue());
        assertTrue("testFindAll 1",found.size() >= 1);

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
        List<MetadataField> found = metadataFieldService.findAllInSchema(context, metadataSchemaService.find(context, MetadataSchema.DC_SCHEMA));
        assertThat("testFindAllInSchema 0",found, notNullValue());
        assertTrue("testFindAllInSchema 1",found.size() >= 1);
        assertTrue("testFindAllInSchema 2",found.size() <= metadataFieldService.findAll(context).size());

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
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Allow full admin permissions
            authorizeService.isAdmin(context); result = true;
        }};

        String elem = "elem2";
        String qual = "qual2";
        MetadataField m = metadataFieldService.create(context, dcSchema, elem, qual, null);
        metadataFieldService.update(context, m);

        MetadataField found = metadataFieldService.findByElement(context, MetadataSchema.DC_SCHEMA, elem, qual);
        assertThat("testUpdateAuth 0",found.getID(), equalTo(m.getID()));
    }

    /**
     * Test of update method, of class MetadataField.
     */
    @Test(expected=AuthorizeException.class)
    public void testUpdateNoAuth() throws Exception
    {
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Disallow full admin permissions
            authorizeService.isAdmin(context); result = false;
        }};

        String elem = "elem2";
        String qual = "qual2";
        MetadataField m = metadataFieldService.create(context, dcSchema, elem, qual, null);
        metadataFieldService.update(context, m);
        fail("Exception expected");
    }

    /**
     * Test of update method, of class MetadataField.
     */
    @Test(expected=NonUniqueMetadataException.class)
    public void testUpdateRepeated() throws Exception
    {
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Allow full admin permissions
            authorizeService.isAdmin(context); result = true;
        }};

        String elem = element;
        String qual = qualifier;
        MetadataField m = metadataFieldService.create(context, dcSchema, elem, qual, null);

        m.setElement(elem);
        m.setQualifier(qual);
        metadataFieldService.update(context, m);
        fail("Exception expected");
    }

    /**
     * Test of delete method, of class MetadataField.
     */
    @Test
    public void testDeleteAuth() throws Exception
    {
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Allow full admin permissions
            authorizeService.isAdmin(context); result = true;
        }};

        String elem = "elem3";
        String qual = "qual3";
        MetadataField m = metadataFieldService.create(context, dcSchema, elem, qual, null);

        metadataFieldService.delete(context, m);

        MetadataField found = metadataFieldService.findByElement(context, MetadataSchema.DC_SCHEMA, elem, qual);
        assertThat("testDeleteAuth 0",found, nullValue());
    }

    /**
     * Test of delete method, of class MetadataField.
     */
    @Test(expected=AuthorizeException.class)
    public void testDeleteNoAuth() throws Exception
    {
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Disallow full admin permissions
            authorizeService.isAdmin(context); result = false;
        }};

        String elem = "elem3";
        String qual = "qual3";
        MetadataField m = metadataFieldService.create(context, dcSchema, elem, qual, null);

        metadataFieldService.delete(context, m);
        fail("Exception expected");
    }

    /**
     * Test of find method, of class MetadataField.
     */
    @Test
    public void testFind() throws Exception
    {
        int id = mf.getID();
        
        MetadataField found = metadataFieldService.find(context, id);
        assertThat("testFind 0",found, notNullValue());
        assertThat("testFind 1",found.getID(), equalTo(mf.getID()));
    }

}
