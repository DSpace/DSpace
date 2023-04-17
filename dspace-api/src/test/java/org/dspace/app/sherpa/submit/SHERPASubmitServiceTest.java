/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.sherpa.submit;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.sql.SQLException;

import org.dspace.AbstractUnitTest;
import org.dspace.app.sherpa.v2.SHERPAResponse;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataSchemaEnum;
import org.dspace.content.MetadataValue;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
import org.dspace.content.service.InstallItemService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.MetadataFieldService;
import org.dspace.content.service.MetadataValueService;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * SHERPASubmitServiceTest creates a dummy item with an ISSN in its metadata, and makes sure
 * that the ISSN is detected and passed to SHERPAService for a mock query
 */
public class SHERPASubmitServiceTest extends AbstractUnitTest {

    // Set up services
    protected ItemService itemService = ContentServiceFactory.getInstance().getItemService();
    protected WorkspaceItemService workspaceItemService = ContentServiceFactory.getInstance().getWorkspaceItemService();
    protected InstallItemService installItemService = ContentServiceFactory.getInstance().getInstallItemService();
    protected CommunityService communityService = ContentServiceFactory.getInstance().getCommunityService();
    protected CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();
    protected MetadataFieldService metadataFieldService = ContentServiceFactory.getInstance().getMetadataFieldService();
    protected MetadataValueService metadataValueService = ContentServiceFactory.getInstance().getMetadataValueService();
    SHERPASubmitService sherpaSubmitService = DSpaceServicesFactory.getInstance().getServiceManager()
        .getServiceByName("org.dspace.app.sherpa.submit.SHERPASubmitService", SHERPASubmitService.class);
    Collection testCollection = null;
    Community testCommunity = null;


    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {

    }

    @Before
    public void setUp() throws SQLException, AuthorizeException {
        context.turnOffAuthorisationSystem();
        // Create primary Test community
        testCommunity = communityService.create(null, context);
        communityService
            .addMetadata(context, testCommunity, MetadataSchemaEnum.DC.getName(),
                "title", null, null, "Test Community");
        communityService.update(context, testCommunity);

        // Create our primary Test Collection
        testCollection = collectionService.create(context, testCommunity);
        collectionService.addMetadata(context, testCollection, "dc", "title", null, null,
            "Test Collection");
        collectionService.update(context, testCollection);
    }

    @After
    public void tearDown() {
        context.restoreAuthSystemState();
        testCommunity = null;
        testCollection = null;
    }

    /**
     * Test the ISSN extraction
     */
    @Test
    public void testGetISSNs() throws AuthorizeException, SQLException {
        String validISSN = "0140-6736";
        // Create and install an item with an ISSN
        WorkspaceItem testWorkspaceItem = workspaceItemService.create(context, testCollection, false);
        Item testItem = installItemService.installItem(context, testWorkspaceItem);

        // Set up ISSN metadatavalue
        MetadataField issnField = metadataFieldService.
            findByString(context, "dc.identifier.issn", '.');
        MetadataValue metadataValue = metadataValueService.create(context, testItem, issnField);
        metadataValue.setValue(validISSN);

        // Get responses from SHERPA submit service, which should inspect item ISSNs and perform search
        // on the mock SHERPA service
        SHERPAResponse response = sherpaSubmitService.searchRelatedJournals(context, testItem);

        // Make sure response is not null or empty
        assertTrue("Response should not be null", response != null);

        // For each response (there should be only one based on test data) perform the standard set
        // of thorough parsing tests

        // Assert response is not error, or fail with message
        assertFalse("Response was flagged as 'isError'", response.isError());

        // Skip remainder of parsing tests - these are already done in SHERPAServiceTEst
    }

}
