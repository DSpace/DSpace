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
import org.dspace.content.service.MetadataSchemaService;
import org.junit.Before;
import org.junit.Test;

import java.sql.SQLException;
import java.util.List;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * Unit Tests for class MetadataSchema
 * @author pvillega
 */
public class MetadataSchemaTest extends AbstractUnitTest
{

    /** log4j category */
    private static final Logger log = Logger.getLogger(MetadataSchemaTest.class);

    /**
     * MetadataSchema instance for the tests
     */
    private MetadataSchema ms;

    protected MetadataSchemaService metadataSchemaService = ContentServiceFactory.getInstance().getMetadataSchemaService();

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
            this.ms = metadataSchemaService.find(context, MetadataSchema.DC_SCHEMA);
        }
        catch (SQLException ex)
        {
            log.error("SQL Error in init", ex);
            fail("SQL Error in init: " + ex.getMessage());
        }
    }

    /**
     * Test of getNamespace method, of class MetadataSchema.
     */
    @Test
    public void testGetNamespace() 
    {
        assertThat("testGetNamespace 0",ms.getNamespace(),notNullValue());
        assertThat("testGetNamespace 1",ms.getNamespace(),not(equalTo("")));
    }

    /**
     * Test of setNamespace method, of class MetadataSchema.
     */
    @Test
    public void testSetNamespace()
    {
        String oldnamespace = ms.getNamespace();
        String namespace = "new namespace";
        ms.setNamespace(namespace);
        assertThat("testSetNamespace 0",ms.getNamespace(),notNullValue());
        assertThat("testSetNamespace 1",ms.getNamespace(),not(equalTo("")));
        assertThat("testSetNamespace 2",ms.getNamespace(),equalTo(namespace));

        //we restore the old namespace to avoid issues in other tests
        ms.setNamespace(oldnamespace);
    }

    /**
     * Test of getName method, of class MetadataSchema.
     */
    @Test
    public void testGetName() 
    {
        assertThat("testGetName 0",ms.getName(),notNullValue());
        assertThat("testGetName 1",ms.getName(),not(equalTo("")));
    }

    /**
     * Test of setName method, of class MetadataSchema.
     */
    @Test
    public void testSetName()
    {
        String oldname = ms.getName();
        String name = "new name";
        ms.setName(name);
        assertThat("testSetName 0",ms.getName(),notNullValue());
        assertThat("testSetName 1",ms.getName(),not(equalTo("")));
        assertThat("testSetName 2",ms.getName(),equalTo(name));

        //we restore the old name to avoid issues in other tests
        ms.setName(oldname);
    }

    /**
     * Test of getSchemaID method, of class MetadataSchema.
     */
    @Test
    public void testGetSchemaID() throws SQLException
    {
        assertThat("testGetSchemaID 0",ms.getID(), equalTo(metadataSchemaService.find(context, MetadataSchema.DC_SCHEMA).getID()));
    }

    /**
     * Test of create method, of class MetadataSchema.
     */
    @Test
    public void testCreateAuth() throws Exception
    {
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Allow full admin permissions
                authorizeService.isAdmin(context); result = true;
        }};

        String namespace = "namespace";
        String name = "name";
        metadataSchemaService.create(context, name, namespace);

        MetadataSchema found = metadataSchemaService.findByNamespace(context, namespace);
        assertThat("testCreateAuth 0",found, notNullValue());
    }

    /**
     * Test of create method, of class MetadataSchema.
     */
    @Test(expected=AuthorizeException.class)
    public void testCreateNoAuth() throws Exception
    {
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Disallow full admin permissions
                authorizeService.isAdmin(context); result = false;
        }};

        String namespace = "namespace";
        String name = "name";
        metadataSchemaService.create(context, name, namespace);
        fail("Exception expected");
    }

    /**
     * Test of create method, of class MetadataSchema.
     */
    @Test(expected=NonUniqueMetadataException.class)
    public void testCreateRepeated() throws Exception
    {
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Allow full admin permissions
            authorizeService.isAdmin(context); result = true;
        }};

        String namespace = ms.getNamespace();
        String name = ms.getName();
        metadataSchemaService.create(context, name, namespace);
        fail("Exception expected");
    }

    /**
     * Test of findByNamespace method, of class MetadataSchema.
     */
    @Test
    public void testFindByNamespace() throws Exception
    {
        log.info(">>"+ms.getNamespace()+" "+ms.getName());
        MetadataSchema found = metadataSchemaService.findByNamespace(context, ms.getNamespace());
        assertThat("testFindByNamespace 0",found, notNullValue());
        assertThat("testFindByNamespace 1",found.getID(), equalTo(ms.getID()));
    }

    /**
     * Test of update method, of class MetadataSchema.
     */
    @Test
    public void testUpdateAuth() throws Exception
    {
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Allow full admin permissions
            authorizeService.isAdmin(context); result = true;
        }};

        String namespace = "namespace2";
        String name = "name2";
        MetadataSchema metadataSchema = metadataSchemaService.create(context, name, namespace);

        metadataSchemaService.update(context, metadataSchema);

        MetadataSchema found = metadataSchemaService.findByNamespace(context, namespace);
        assertThat("testUpdateAuth 0",found.getID(), equalTo(metadataSchema.getID()));
    }

    /**
     * Test of update method, of class MetadataSchema.
     */
    @Test(expected=AuthorizeException.class)
    public void testUpdateNoAuth() throws Exception
    {
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Disallow full admin permissions
            authorizeService.isAdmin(context);
                result = false;
        }};

        metadataSchemaService.update(context, ms);
        fail("Exception expected");
    }

    /**
     * Test of update method, of class MetadataSchema.
     */
    @Test(expected=NonUniqueMetadataException.class)
    public void testUpdateRepeated() throws Exception
    {
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Allow full admin permissions
            authorizeService.isAdmin(context); result = true;
        }};

        String namespace = ms.getNamespace();
        String name = ms.getName();
        MetadataSchema m = metadataSchemaService.create(context, name, namespace);

        m.setName(name);
        m.setNamespace(namespace);
        metadataSchemaService.update(context, m);
        fail("Exception expected");
    }

    /**
     * Test of delete method, of class MetadataSchema.
     */
    @Test
    public void testDeleteAuth() throws Exception
    {
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Allow full admin permissions
            authorizeService.isAdmin(context); result = true;
        }};

        String namespace = "namespace3";
        String name = "name3";
        MetadataSchema m = metadataSchemaService.create(context, name, namespace);

        metadataSchemaService.delete(context, m);

        MetadataSchema found = metadataSchemaService.findByNamespace(context, namespace);
        assertThat("testDeleteAuth 0",found, nullValue());
    }

    /**
     * Test of delete method, of class MetadataSchema.
     */
    @Test(expected=AuthorizeException.class)
    public void testDeleteNoAuth() throws Exception
    {
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Disallow full admin permissions
            authorizeService.isAdmin(context); result = false;
        }};

        String namespace = "namespace3";
        String name = "name3";
        MetadataSchema m = metadataSchemaService.create(context, name, namespace);

        metadataSchemaService.delete(context, m);
        fail("Exception expected");
    }

    /**
     * Test of findAll method, of class MetadataSchema.
     */
    @Test
    public void testFindAll() throws Exception
    {
        List<MetadataSchema> found = metadataSchemaService.findAll(context);
        assertThat("testFindAll 0",found, notNullValue());
        assertTrue("testFindAll 1",found.size() >= 1);

        boolean added = false;
        for(MetadataSchema msc: found)
        {
            if(msc.equals(ms))
            {
                added = true;
            }
        }
        assertTrue("testFindAll 2",added);
    }

    /**
     * Test of find method, of class MetadataSchema.
     */
    @Test
    public void testFind_Context_int() throws Exception
    {
        MetadataSchema found = metadataSchemaService.find(context, ms.getID());
        assertThat("testFind_Context_int 0",found, notNullValue());
        assertThat("testFind_Context_int 1",found.getID(), equalTo(ms.getID()));
        assertThat("testFind_Context_int 2",found.getName(), equalTo(ms.getName()));
        assertThat("testFind_Context_int 3",found.getNamespace(), equalTo(ms.getNamespace()));
    }

    /**
     * Test of find method, of class MetadataSchema.
     */
    @Test
    public void testFind_Context_String() throws Exception
    {
        String shortName = ms.getName();
        MetadataSchema found = metadataSchemaService.find(context, shortName);
        assertThat("testFind_Context_String 0",found, notNullValue());
        assertThat("testFind_Context_String 1",found.getID(), equalTo(ms.getID()));
        assertThat("testFind_Context_String 2",found.getName(), equalTo(ms.getName()));
        assertThat("testFind_Context_String 3",found.getNamespace(), equalTo(ms.getNamespace()));

        found = metadataSchemaService.find(context, null);
        assertThat("testFind_Context_String 4",found, nullValue());
    }

}
