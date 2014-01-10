/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import mockit.NonStrictExpectations;
import java.sql.SQLException;
import org.dspace.AbstractUnitTest;
import org.apache.log4j.Logger;
import org.junit.*;
import static org.junit.Assert.* ;
import static org.hamcrest.CoreMatchers.*;

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
            this.ms = MetadataSchema.find(context, MetadataSchema.DC_SCHEMA_ID);
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
        super.destroy();
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

        //we restore the old namespace to avoid issues
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
        String name = "new name";
        ms.setName(name);
        assertThat("testSetName 0",ms.getName(),notNullValue());
        assertThat("testSetName 1",ms.getName(),not(equalTo("")));
        assertThat("testSetName 2",ms.getName(),equalTo(name));
    }

    /**
     * Test of getSchemaID method, of class MetadataSchema.
     */
    @Test
    public void testGetSchemaID() 
    {
        assertThat("testGetSchemaID 0",ms.getSchemaID(), equalTo(MetadataSchema.DC_SCHEMA_ID));
    }

    /**
     * Test of create method, of class MetadataSchema.
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

        String namespace = "namespace";
        String name = "name";
        MetadataSchema m = new MetadataSchema();
        m.setName(name);
        m.setNamespace(namespace);
        m.create(context);

        MetadataSchema found = MetadataSchema.findByNamespace(context, namespace);
        assertThat("testCreateAuth 0",found.getSchemaID(), equalTo(m.getSchemaID()));
    }

    /**
     * Test of create method, of class MetadataSchema.
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

        String namespace = "namespace";
        String name = "name";
        MetadataSchema m = new MetadataSchema();
        m.setName(name);
        m.setNamespace(namespace);
        m.create(context);
        fail("Exception expected");
    }

    /**
     * Test of create method, of class MetadataSchema.
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

        String namespace = ms.getNamespace();
        String name = ms.getName();
        MetadataSchema m = new MetadataSchema();
        m.setName(name);
        m.setNamespace(namespace);
        m.create(context);
        fail("Exception expected");
    }

    /**
     * Test of findByNamespace method, of class MetadataSchema.
     */
    @Test
    public void testFindByNamespace() throws Exception
    {
        log.info(">>"+ms.getNamespace()+" "+ms.getName());
        MetadataSchema found = MetadataSchema.findByNamespace(context, ms.getNamespace());
        assertThat("testFindByNamespace 0",found, notNullValue());
        assertThat("testFindByNamespace 1",found.getSchemaID(), equalTo(ms.getSchemaID()));
    }

    /**
     * Test of update method, of class MetadataSchema.
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

        String namespace = "namespace2";
        String name = "name2";
        MetadataSchema m = new MetadataSchema();
        m.setName(name);
        m.setNamespace(namespace);
        m.create(context);

        m.update(context);

        MetadataSchema found = MetadataSchema.findByNamespace(context, namespace);
        assertThat("testUpdateAuth 0",found.getSchemaID(), equalTo(m.getSchemaID()));
    }

    /**
     * Test of update method, of class MetadataSchema.
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

        String namespace = "namespace2";
        String name = "name2";
        MetadataSchema m = new MetadataSchema();
        m.setName(name);
        m.setNamespace(namespace);
        m.update(context);
        fail("Exception expected");
    }

    /**
     * Test of update method, of class MetadataSchema.
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

        String namespace = ms.getNamespace();
        String name = ms.getName();
        MetadataSchema m = new MetadataSchema();
        m.create(context);

        m.setName(name);
        m.setNamespace(namespace);
        m.update(context);
        fail("Exception expected");
    }

    /**
     * Test of delete method, of class MetadataSchema.
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

        String namespace = "namespace3";
        String name = "name3";
        MetadataSchema m = new MetadataSchema();
        m.setName(name);
        m.setNamespace(namespace);
        m.create(context);
        context.commit();

        m.delete(context);

        MetadataSchema found = MetadataSchema.findByNamespace(context, namespace);
        assertThat("testDeleteAuth 0",found, nullValue());
    }

    /**
     * Test of delete method, of class MetadataSchema.
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

        String namespace = "namespace3";
        String name = "name3";
        MetadataSchema m = new MetadataSchema();
        m.setName(name);
        m.setNamespace(namespace);
        m.create(context);
        context.commit();

        m.delete(context);
        fail("Exception expected");
    }

    /**
     * Test of findAll method, of class MetadataSchema.
     */
    @Test
    public void testFindAll() throws Exception
    {
        MetadataSchema[] found = MetadataSchema.findAll(context);
        assertThat("testFindAll 0",found, notNullValue());
        assertTrue("testFindAll 1",found.length >= 1);

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
        int id = MetadataSchema.DC_SCHEMA_ID;
        MetadataSchema found = MetadataSchema.find(context, id);
        assertThat("testFind_Context_int 0",found, notNullValue());
        assertThat("testFind_Context_int 1",found.getSchemaID(), equalTo(ms.getSchemaID()));
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
        MetadataSchema found = MetadataSchema.find(context, shortName);
        assertThat("testFind_Context_String 0",found, notNullValue());
        assertThat("testFind_Context_String 1",found.getSchemaID(), equalTo(ms.getSchemaID()));
        assertThat("testFind_Context_String 2",found.getName(), equalTo(ms.getName()));
        assertThat("testFind_Context_String 3",found.getNamespace(), equalTo(ms.getNamespace()));

        found = MetadataSchema.find(context, null);
        assertThat("testFind_Context_String 4",found, nullValue());
    }

}