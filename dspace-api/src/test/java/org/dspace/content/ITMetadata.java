/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import org.apache.log4j.Logger;
import org.databene.contiperf.PerfTest;
import org.databene.contiperf.Required;
import org.dspace.AbstractIntegrationTest;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.*;

/**
 * This is an integration test to validate the metadata classes
 * @author pvillega
 */
public class ITMetadata  extends AbstractIntegrationTest
{
    /** log4j category */
    private static final Logger log = Logger.getLogger(ITMetadata.class);


    protected CommunityService communityService = ContentServiceFactory.getInstance().getCommunityService();
    protected CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();
    protected ItemService itemService = ContentServiceFactory.getInstance().getItemService();
    protected WorkspaceItemService workspaceItemService = ContentServiceFactory.getInstance().getWorkspaceItemService();
    protected MetadataSchemaService metadataSchemaService = ContentServiceFactory.getInstance().getMetadataSchemaService();
    protected MetadataFieldService metadataFieldService = ContentServiceFactory.getInstance().getMetadataFieldService();
    protected MetadataValueService metadataValueService = ContentServiceFactory.getInstance().getMetadataValueService();

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
        Community owningCommunity = communityService.create(null, context);
        Collection collection = collectionService.create(context, owningCommunity);
        Item it = workspaceItemService.create(context, collection, false).getItem();

        MetadataSchema schema = metadataSchemaService.create(context, schemaName, "htpp://test/schema/");

        MetadataField field1 = metadataFieldService.create(context, schema, "elem1", "qual1", "note 1");

        MetadataField field2 = metadataFieldService.create(context, schema, "elem2", "qual2", "note 2");

        MetadataValue value1 = metadataValueService.create(context, it, field1);
        value1.setValue("value1");
        metadataValueService.update(context, value1);

        MetadataValue value2 = metadataValueService.create(context, it, field2);
        value2.setValue("value2");
        metadataValueService.update(context, value2);


        //verify it works as expected
        assertThat("testCreateSchema 0", schema.getName(), equalTo(schemaName));
        assertThat("testCreateSchema 1", field1.getMetadataSchema(), equalTo(schema));
        assertThat("testCreateSchema 2", field2.getMetadataSchema(), equalTo(schema));

        List<MetadataField> fields = metadataFieldService.findAllInSchema(context, schema);
        assertTrue("testCreateSchema 3", fields.size() == 2);
        boolean exist = true;
        for(MetadataField f : fields)
        {
           if(!f.equals(field1) && !f.equals(field2))
           {
               exist = false;
           }
        }
        assertTrue("testCreateSchema 4", exist);

        List<MetadataValue> col1 = metadataValueService.findByField(context, field1);
        assertTrue("testCreateSchema 5", col1.contains(value1));

        List<MetadataValue> col2 = metadataValueService.findByField(context, field2);
        assertTrue("testCreateSchema 6", col2.contains(value2));

        //clean database
        it.removeMetadata(value1);
        col1 = metadataValueService.findByField(context, field1);
        assertFalse("testCreateSchema 7", col1.contains(value1));

        it.removeMetadata(value2);
        metadataFieldService.delete(context, field1);
        metadataFieldService.delete(context, field2);
        metadataSchemaService.delete(context, schema);

        communityService.delete(context, owningCommunity);

        context.restoreAuthSystemState();
    }

}

