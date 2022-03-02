/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

import org.apache.logging.log4j.Logger;
import org.dspace.AbstractUnitTest;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
import org.dspace.content.service.InstallItemService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.WorkspaceItemService;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Unit Tests for class InstallItem
 *
 * @author pvillega
 */
public class InstallItemTest extends AbstractUnitTest {


    protected CommunityService communityService = ContentServiceFactory.getInstance().getCommunityService();
    protected CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();
    protected ItemService itemService = ContentServiceFactory.getInstance().getItemService();
    protected InstallItemService installItemService = ContentServiceFactory.getInstance().getInstallItemService();
    protected WorkspaceItemService workspaceItemService = ContentServiceFactory.getInstance().getWorkspaceItemService();

    private Collection collection;
    private Community owningCommunity;

    /**
     * log4j category
     */
    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(InstallItemTest.class);

    /**
     * Used to check/verify thrown exceptions in below tests
     **/
    @Rule
    public ExpectedException thrown = ExpectedException.none();


    @Before
    @Override
    public void init() {
        super.init();
        try {
            context.turnOffAuthorisationSystem();
            this.owningCommunity = communityService.create(null, context);
            this.collection = collectionService.create(context, owningCommunity);
            context.restoreAuthSystemState();
        } catch (SQLException | AuthorizeException ex) {
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
    public void destroy() {
        try {
            context.turnOffAuthorisationSystem();
            communityService.delete(context, owningCommunity);
            context.restoreAuthSystemState();
        } catch (SQLException | AuthorizeException | IOException ex) {
            log.error("SQL Error in destroy", ex);
            fail("SQL Error in destroy: " + ex.getMessage());
            context.abort();
        }
        super.destroy();
    }


    /**
     * Test of installItem method, of class InstallItem.
     */
    @Test
    public void testInstallItem_Context_InProgressSubmission() throws Exception {
        context.turnOffAuthorisationSystem();
        WorkspaceItem is = workspaceItemService.create(context, collection, false);

        Item result = installItemService.installItem(context, is);
        context.restoreAuthSystemState();
        assertThat("testInstallItem_Context_InProgressSubmission 0", result, equalTo(is.getItem()));
    }

    /**
     * Test of installItem method (with a valid handle), of class InstallItem.
     */
    @Test
    public void testInstallItem_validHandle() throws Exception {
        context.turnOffAuthorisationSystem();
        String handle = "123456789/56789";
        WorkspaceItem is = workspaceItemService.create(context, collection, false);

        //Test assigning a specified handle to an item
        // (this handle should not already be used by system, as it doesn't start with "1234567689" prefix)
        Item result = installItemService.installItem(context, is, handle);
        context.restoreAuthSystemState();
        assertThat("testInstallItem_validHandle", result, equalTo(is.getItem()));
        assertThat("testInstallItem_validHandle", result.getHandle(), equalTo(handle));
    }

    /**
     * Test of installItem method (with an invalid handle), of class InstallItem.
     */
    @Test(expected = IllegalStateException.class)
    public void testInstallItem_invalidHandle() throws Exception {
        // create two items for tests
        context.turnOffAuthorisationSystem();
        try {
            WorkspaceItem is = workspaceItemService.create(context, collection, false);
            WorkspaceItem is2 = workspaceItemService.create(context, collection, false);

            //Test assigning the same Handle to two different items
            String handle = "123456789/56789";
            installItemService.installItem(context, is, handle);

            // Assigning the same handle again should throw a RuntimeException
            installItemService.installItem(context, is2, handle);
        } finally {
            context.restoreAuthSystemState();
        }
        fail("Exception expected");
    }


    /**
     * Test of restoreItem method, of class InstallItem.
     */
    @Test
    public void testRestoreItem() throws Exception {
        context.turnOffAuthorisationSystem();
        String handle = "123456789/56789";
        WorkspaceItem is = workspaceItemService.create(context, collection, false);

        //get current date
        DCDate now = DCDate.getCurrent();
        String dayAndTime = now.toString();
        //parse out just the date, remove the time (format: yyyy-mm-ddT00:00:00Z)
        String date = dayAndTime.substring(0, dayAndTime.indexOf("T"));

        //Build the beginning of a dummy provenance message
        //(restoreItem should NEVER insert a provenance message with today's date)
        String provDescriptionBegins = "Made available in DSpace on " + date;

        Item result = installItemService.restoreItem(context, is, handle);
        context.restoreAuthSystemState();

        //Make sure restore worked
        assertThat("testRestoreItem 0", result, equalTo(is.getItem()));

        //Make sure that restore did NOT insert a new provenance message with today's date
        List<MetadataValue> provMsgValues = itemService
            .getMetadata(result, "dc", "description", "provenance", Item.ANY);
        int i = 1;
        for (MetadataValue val : provMsgValues) {
            assertFalse("testRestoreItem " + i, val.getValue().startsWith(provDescriptionBegins));
            i++;
        }
    }

    /**
     * Test of getBitstreamProvenanceMessage method, of class InstallItem.
     */
    @Test
    public void testGetBitstreamProvenanceMessage() throws Exception {
        File f = new File(testProps.get("test.bitstream").toString());
        context.turnOffAuthorisationSystem();
        WorkspaceItem is = workspaceItemService.create(context, collection, false);
        Item item = installItemService.installItem(context, is);

        Bitstream one = itemService.createSingleBitstream(context, new FileInputStream(f), item);
        one.setName(context, "one");

        Bitstream two = itemService.createSingleBitstream(context, new FileInputStream(f), item);
        two.setName(context, "two");

        context.restoreAuthSystemState();

        // Create provenance description
        String testMessage = "No. of bitstreams: 2\n";
        testMessage += "one: "
            + one.getSizeBytes() + " bytes, checksum: "
            + one.getChecksum() + " ("
            + one.getChecksumAlgorithm() + ")\n";
        testMessage += "two: "
            + two.getSizeBytes() + " bytes, checksum: "
            + two.getChecksum() + " ("
            + two.getChecksumAlgorithm() + ")\n";

        assertThat("testGetBitstreamProvenanceMessage 0",
                   installItemService.getBitstreamProvenanceMessage(context, item), equalTo(testMessage));
    }

    /**
     * Test passing in "today" as an issued date to InstallItem.
     */
    @Test
    public void testInstallItem_todayAsIssuedDate() throws Exception {
        //create a dummy WorkspaceItem
        context.turnOffAuthorisationSystem();
        String handle = "123456789/56789";
        WorkspaceItem is = workspaceItemService.create(context, collection, false);

        // Set "today" as "dc.date.issued"
        itemService.addMetadata(context, is.getItem(), "dc", "date", "issued", Item.ANY, "today");
        itemService.addMetadata(context, is.getItem(), "dc", "date", "issued", Item.ANY, "2011-01-01");

        //get current date
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.setTimeZone(TimeZone.getTimeZone("UTC"));

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

        String date = sdf.format(calendar.getTime());

        Item result = installItemService.installItem(context, is, handle);
        context.restoreAuthSystemState();

        //Make sure the string "today" was replaced with today's date
        List<MetadataValue> issuedDates = itemService.getMetadata(result, "dc", "date", "issued", Item.ANY);

        assertThat("testInstallItem_todayAsIssuedDate 0", issuedDates.get(0).getValue(), equalTo(date));
        assertThat("testInstallItem_todayAsIssuedDate 1", issuedDates.get(1).getValue(), equalTo("2011-01-01"));
    }

    /**
     * Test null issue date (when none set) in InstallItem
     */
    @Test
    public void testInstallItem_nullIssuedDate() throws Exception {
        //create a dummy WorkspaceItem with no dc.date.issued
        context.turnOffAuthorisationSystem();
        String handle = "123456789/56789";
        WorkspaceItem is = workspaceItemService.create(context, collection, false);

        Item result = installItemService.installItem(context, is, handle);
        context.restoreAuthSystemState();

        //Make sure dc.date.issued is NOT set
        List<MetadataValue> issuedDates = itemService.getMetadata(result, "dc", "date", "issued", Item.ANY);
        assertThat("testInstallItem_nullIssuedDate 0", issuedDates.size(), equalTo(0));
    }

    /**
     * Test passing in "today" as an issued date to restoreItem.
     */
    @Test
    public void testRestoreItem_todayAsIssuedDate() throws Exception {
        //create a dummy WorkspaceItem
        context.turnOffAuthorisationSystem();
        String handle = "123456789/56789";
        WorkspaceItem is = workspaceItemService.create(context, collection, false);

        // Set "today" as "dc.date.issued"
        itemService.addMetadata(context, is.getItem(), "dc", "date", "issued", Item.ANY, "today");
        itemService.addMetadata(context, is.getItem(), "dc", "date", "issued", Item.ANY, "2011-01-01");

        //get current date
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.setTimeZone(TimeZone.getTimeZone("UTC"));
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String date = sdf.format(calendar.getTime());

        Item result = installItemService.restoreItem(context, is, handle);
        context.restoreAuthSystemState();

        //Make sure the string "today" was replaced with today's date
        List<MetadataValue> issuedDates = itemService.getMetadata(result, "dc", "date", "issued", Item.ANY);

        assertThat("testRestoreItem_todayAsIssuedDate 0", issuedDates.get(0).getValue(), equalTo(date));
        assertThat("testRestoreItem_todayAsIssuedDate 1", issuedDates.get(1).getValue(), equalTo("2011-01-01"));
    }
}
