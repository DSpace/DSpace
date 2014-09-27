/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import java.io.IOException;
import org.databene.contiperf.Required;
import org.databene.contiperf.PerfTest;
import java.sql.SQLException;
import java.util.List;

import org.apache.log4j.Logger;
import org.dspace.AbstractIntegrationTest;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Constants;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

/**
 * This is an integration test to validate the metadata classes
 * @author pvillega
 */
public class MetadataIntegrationTest  extends AbstractIntegrationTest
{
    /** log4j category */
    private static final Logger log = Logger.getLogger(MetadataIntegrationTest.class);


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
     * Tests the creation of a new metadata schema with some values
     */
    @Test
    @PerfTest(invocations = 50, threads = 1)
    @Required(percentile95 = 500, average= 200)
    public void testCreateSchema() throws SQLException, AuthorizeException, NonUniqueMetadataException, IOException
    {
        String schemaName = "integration";

        //we create the structure
        context.turnOffAuthorisationSystem();
        Item it = Item.create(context);

        MetadataSchema schema = new MetadataSchema("htpp://test/schema/", schemaName);
        schema.create(context);

        MetadataField field1 = new MetadataField(schema, "elem1", "qual1", "note 1");
        field1.create(context);

        MetadataField field2 = new MetadataField(schema, "elem2", "qual2", "note 2");
        field2.create(context);

        MetadataValue value1 = new MetadataValue(field1);
        value1.setResourceId(it.getID());
        value1.setResourceTypeId(Constants.ITEM);
        value1.setValue("value1");
        value1.create(context);

        MetadataValue value2 = new MetadataValue(field2);
        value2.setResourceId(it.getID());
        value2.setResourceTypeId(Constants.ITEM);
        value2.setValue("value2");
        value2.create(context);
        
        context.commit();

        //verify it works as expected
        assertThat("testCreateSchema 0", schema.getName(), equalTo(schemaName));
        assertThat("testCreateSchema 1", field1.getSchemaID(), equalTo(schema.getSchemaID()));
        assertThat("testCreateSchema 2", field2.getSchemaID(), equalTo(schema.getSchemaID()));

        MetadataField[] fields = MetadataField.findAllInSchema(context, schema.getSchemaID());
        assertTrue("testCreateSchema 3", fields.length == 2);
        boolean exist = true;
        for(MetadataField f : fields)
        {
           if(!f.equals(field1) && !f.equals(field2))
           {
               exist = false;
           }
        }
        assertTrue("testCreateSchema 4", exist);

        List<MetadataValue> col1 = MetadataValue.findByField(context, field1.getFieldID());
        assertTrue("testCreateSchema 5", col1.contains(value1));

        List<MetadataValue> col2 = MetadataValue.findByField(context, field2.getFieldID());
        assertTrue("testCreateSchema 6", col2.contains(value2));

        //clean database
        value1.delete(context);
        col1 = MetadataValue.findByField(context, field1.getFieldID());
        assertFalse("testCreateSchema 7", col1.contains(value1));

        value2.delete(context);
        field1.delete(context);
        field2.delete(context);
        schema.delete(context);

        context.restoreAuthSystemState();
        context.commit();
    }

}

