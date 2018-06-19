/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import java.io.IOException;
import java.sql.SQLException;
import org.apache.log4j.Logger;
import org.dspace.AbstractUnitTest;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.*;
import org.junit.*;
import static org.junit.Assert.* ;

/**
 * Unit Tests for class ItemComparator
 * @author pvillega
 */
public class ItemComparatorTest extends AbstractUnitTest
{

    /** log4j category */
    private static final Logger log = Logger.getLogger(ItemComparatorTest.class);

    protected CommunityService communityService = ContentServiceFactory.getInstance().getCommunityService();
    protected CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();
    private ItemService itemService = ContentServiceFactory.getInstance().getItemService();
    protected WorkspaceItemService workspaceItemService = ContentServiceFactory.getInstance().getWorkspaceItemService();
    protected InstallItemService installItemService = ContentServiceFactory.getInstance().getInstallItemService();
    protected MetadataSchemaService metadataSchemaService = ContentServiceFactory.getInstance().getMetadataSchemaService();
    protected MetadataFieldService metadataFieldService = ContentServiceFactory.getInstance().getMetadataFieldService();
    private MetadataValueService metadataValueService = ContentServiceFactory.getInstance().getMetadataValueService();

    /**
     * Item instance for the tests
     */
    private Item one;

    /**
     * Item instance for the tests
     */
    private Item two;


    private Collection collection;
    private Community owningCommunity;
    private MetadataField metadataField;

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
        try
        {
            super.init();

            context.turnOffAuthorisationSystem();
            MetadataSchema testSchema = metadataSchemaService.find(context, "dc");
            metadataField = metadataFieldService.create(context, testSchema, "test", "one", null);
            this.owningCommunity = communityService.create(null, context);
            this.collection = collectionService.create(context, owningCommunity);
            WorkspaceItem workspaceItem = workspaceItemService.create(context, collection, false);
            this.one = installItemService.installItem(context, workspaceItem);
            workspaceItem = workspaceItemService.create(context, collection, false);
            this.two = installItemService.installItem(context, workspaceItem);
            context.restoreAuthSystemState();
        }
        catch (AuthorizeException ex)
        {
            log.error("Authorization Error in init", ex);
            fail("Authorization Error in init: " + ex.getMessage());
        }
        catch (SQLException ex)
        {
            log.error("SQL Error in init", ex);
            fail("SQL Error in init:" + ex.getMessage());
        } catch (NonUniqueMetadataException ex) {
            log.error("Error in init", ex);
            fail("Error in init:" + ex.getMessage());
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
        context.turnOffAuthorisationSystem();
          try{
              // Remove all values added to the test MetadataField (MetadataField cannot be deleted if it is still used)
              metadataValueService.deleteByMetadataField(context, metadataField);
              // Delete the (unused) metadataField
              metadataFieldService.delete(context, metadataField);
              communityService.delete(context, owningCommunity);
              context.restoreAuthSystemState();
        } catch (SQLException | AuthorizeException | IOException ex) {
            log.error("SQL Error in destroy", ex);
            fail("SQL Error in destroy: " + ex.getMessage());
        }
        super.destroy();
    }

    /**
     * Test of compare method, of class ItemComparator.
     */
    @Test
    public void testCompare() throws SQLException {
        int result = 0;
        ItemComparator ic = null;

        //one of the tiems has no value
        ic = new ItemComparator("test", "one", Item.ANY, true);
        result = ic.compare(one, two);
        assertTrue("testCompare 0",result == 0);

        ic = new ItemComparator("test", "one", Item.ANY, true);
        itemService.addMetadata(context, one, "dc", "test", "one", Item.ANY, "1");
        result = ic.compare(one, two);
        assertTrue("testCompare 1",result >= 1);
        itemService.clearMetadata(context, one, "dc", "test", "one", Item.ANY);
        
        ic = new ItemComparator("test", "one", Item.ANY, true);
        itemService.addMetadata(context, two, "dc", "test", "one", Item.ANY, "1");        
        result = ic.compare(one, two);
        assertTrue("testCompare 2",result <= -1);
        itemService.clearMetadata(context, two, "dc", "test", "one", Item.ANY);
        
        //value in both items
        ic = new ItemComparator("test", "one", Item.ANY, true);
        itemService.addMetadata(context, one, "dc", "test", "one", Item.ANY, "1");
        itemService.addMetadata(context, two, "dc", "test", "one", Item.ANY, "2");
        result = ic.compare(one, two);
        assertTrue("testCompare 3",result <= -1);
        itemService.clearMetadata(context, one, "dc", "test", "one", Item.ANY);
        itemService.clearMetadata(context, two, "dc", "test", "one", Item.ANY);
        
        ic = new ItemComparator("test", "one", Item.ANY, true);
        itemService.addMetadata(context, one, "dc", "test", "one", Item.ANY, "1");
        itemService.addMetadata(context, two, "dc", "test", "one", Item.ANY, "1");
        result = ic.compare(one, two);
        assertTrue("testCompare 4",result == 0);
        itemService.clearMetadata(context, one, "dc", "test", "one", Item.ANY);
        itemService.clearMetadata(context, two, "dc", "test", "one", Item.ANY);
        
        ic = new ItemComparator("test", "one", Item.ANY, true);
        itemService.addMetadata(context, one, "dc", "test", "one", Item.ANY, "2");
        itemService.addMetadata(context, two, "dc", "test", "one", Item.ANY, "1");
        result = ic.compare(one, two);
        assertTrue("testCompare 5",result >= 1);
        itemService.clearMetadata(context, one, "dc", "test", "one", Item.ANY);
        itemService.clearMetadata(context, two, "dc", "test", "one", Item.ANY);

        //multiple values (min, max)
        ic = new ItemComparator("test", "one", Item.ANY, true);
        itemService.addMetadata(context, one, "dc", "test", "one", Item.ANY, "0");
        itemService.addMetadata(context, one, "dc", "test", "one", Item.ANY, "1");
        itemService.addMetadata(context, two, "dc", "test", "one", Item.ANY, "2");
        itemService.addMetadata(context, two, "dc", "test", "one", Item.ANY, "3");
        result = ic.compare(one, two);
        assertTrue("testCompare 3",result <= -1);
        itemService.clearMetadata(context, one, "dc", "test", "one", Item.ANY);
        itemService.clearMetadata(context, two, "dc", "test", "one", Item.ANY);

        ic = new ItemComparator("test", "one", Item.ANY, true);
        itemService.addMetadata(context, one, "dc", "test", "one", Item.ANY, "0");
        itemService.addMetadata(context, one, "dc", "test", "one", Item.ANY, "1");
        itemService.addMetadata(context, two, "dc", "test", "one", Item.ANY, "-1");
        itemService.addMetadata(context, two, "dc", "test", "one", Item.ANY, "1");
        result = ic.compare(one, two);
        assertTrue("testCompare 4",result == 0);
        itemService.clearMetadata(context, one, "dc", "test", "one", Item.ANY);
        itemService.clearMetadata(context, two, "dc", "test", "one", Item.ANY);

        ic = new ItemComparator("test", "one", Item.ANY, true);
        itemService.addMetadata(context, one, "dc", "test", "one", Item.ANY, "1");
        itemService.addMetadata(context, one, "dc", "test", "one", Item.ANY, "2");
        itemService.addMetadata(context, two, "dc", "test", "one", Item.ANY, "1");
        itemService.addMetadata(context, two, "dc", "test", "one", Item.ANY, "-1");
        result = ic.compare(one, two);
        assertTrue("testCompare 5",result >= 1);
        itemService.clearMetadata(context, one, "dc", "test", "one", Item.ANY);
        itemService.clearMetadata(context, two, "dc", "test", "one", Item.ANY);

        ic = new ItemComparator("test", "one", Item.ANY, false);
        itemService.addMetadata(context, one, "dc", "test", "one", Item.ANY, "1");
        itemService.addMetadata(context, one, "dc", "test", "one", Item.ANY, "2");
        itemService.addMetadata(context, two, "dc", "test", "one", Item.ANY, "2");
        itemService.addMetadata(context, two, "dc", "test", "one", Item.ANY, "3");
        result = ic.compare(one, two);
        assertTrue("testCompare 3",result <= -1);
        itemService.clearMetadata(context, one, "dc", "test", "one", Item.ANY);
        itemService.clearMetadata(context, two, "dc", "test", "one", Item.ANY);

        ic = new ItemComparator("test", "one", Item.ANY, false);
        itemService.addMetadata(context, one, "dc", "test", "one", Item.ANY, "1");
        itemService.addMetadata(context, one, "dc", "test", "one", Item.ANY, "2");
        itemService.addMetadata(context, two, "dc", "test", "one", Item.ANY, "1");
        itemService.addMetadata(context, two, "dc", "test", "one", Item.ANY, "5");
        result = ic.compare(one, two);
        assertTrue("testCompare 4",result == 0);
        itemService.clearMetadata(context, one, "dc", "test", "one", Item.ANY);
        itemService.clearMetadata(context, two, "dc", "test", "one", Item.ANY);

        ic = new ItemComparator("test", "one", Item.ANY, false);
        itemService.addMetadata(context, one, "dc", "test", "one", Item.ANY, "2");
        itemService.addMetadata(context, one, "dc", "test", "one", Item.ANY, "3");
        itemService.addMetadata(context, two, "dc", "test", "one", Item.ANY, "1");
        itemService.addMetadata(context, two, "dc", "test", "one", Item.ANY, "4");
        result = ic.compare(one, two);
        assertTrue("testCompare 5",result >= 1);
        itemService.clearMetadata(context, one, "dc", "test", "one", Item.ANY);
        itemService.clearMetadata(context, two, "dc", "test", "one", Item.ANY);
    }

    /**
     * Test of equals method, of class ItemComparator.
     */
    @Test
    @SuppressWarnings({"ObjectEqualsNull", "IncompatibleEquals"})
    public void testEquals()
    {
        ItemComparator ic = new ItemComparator("test", "one", Item.ANY, true);
        ItemComparator target = null;

        assertFalse("testEquals 0", ic.equals(null));
        assertFalse("testEquals 1", ic.equals("test one"));

        target = new ItemComparator("test1", "one", Item.ANY, true);
        assertFalse("testEquals 2", ic.equals(target));

        target = new ItemComparator("test", "one1", Item.ANY, true);
        assertFalse("testEquals 3", ic.equals(target));

        target = new ItemComparator("test", "one", "es", true);
        assertFalse("testEquals 4", ic.equals(target));

        target = new ItemComparator("test1", "one", Item.ANY, false);
        assertFalse("testEquals 5", ic.equals(target));

        target = new ItemComparator("test", "one", Item.ANY, true);
        assertTrue("testEquals 6", ic.equals(target));
    }

}