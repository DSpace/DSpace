package org.dspace.identifier.doi;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.AbstractUnitTest;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.identifier.DOI;
import org.dspace.identifier.DOIIdentifierProvider;
import org.dspace.identifier.factory.IdentifierServiceFactory;
import org.dspace.identifier.service.DOIService;
import org.dspace.utils.DSpace;
import org.dspace.workflow.WorkflowException;
import org.dspace.workflow.WorkflowItem;
import org.dspace.workflow.factory.WorkflowServiceFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * UMD custom class for {@link DOIOrganiser} tests.
 */
public class DOIOrganiserTest
    extends AbstractUnitTest {
    /**
     * log4j category
     */
    private static final Logger log = LogManager.getLogger(DOIOrganiserTest.class);

    private static final String PREFIX = "10.5072";
    private static final String NAMESPACE_SEPARATOR = "dspaceUnitTests-";

    protected DOIService doiService = IdentifierServiceFactory.getInstance().getDOIService();
    protected CommunityService communityService = ContentServiceFactory.getInstance().getCommunityService();
    protected CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();
    protected ItemService itemService = ContentServiceFactory.getInstance().getItemService();
    protected WorkspaceItemService workspaceItemService = ContentServiceFactory.getInstance().getWorkspaceItemService();


    private static Community community;
    private static Collection collection;

    public DOIOrganiserTest() {
    }

    /**
     * This method will be run before every test as per @Before. It will
     * initialize resources required for the tests.
     *
     * Other methods can be annotated with @Before here or in subclasses
     * but no execution order is guaranteed
     */
    @Before
    @Override
    public void init() {
        super.init();

        try {
            context.turnOffAuthorisationSystem();
            // Create an environment for our test objects to live in.
            community = communityService.create(null, context);
            communityService.setMetadataSingleValue(context, community,
                    CommunityService.MD_NAME, null, "A Test Community");
            communityService.update(context, community);
            collection = collectionService.create(context, community);
            collectionService.setMetadataSingleValue(context, collection,
                    CollectionService.MD_NAME, null, "A Test Collection");
            collectionService.update(context, collection);
            //we need to commit the changes so we don't block the table for testing
            context.restoreAuthSystemState();
        } catch (AuthorizeException ex) {
            log.error("Authorization Error in init", ex);
            fail("Authorization Error in init: " + ex.getMessage());
        } catch (SQLException ex) {
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
        community = null;
        collection = null;
        super.destroy();
    }

    /**
     * Create a fresh Item, installed in the repository.
     *
     * @throws SQLException       if database error
     * @throws AuthorizeException if authorization error
     * @throws IOException        if IO error
     */
    private Item newItem()
        throws SQLException, AuthorizeException, IOException, IllegalAccessException, WorkflowException {
        context.turnOffAuthorisationSystem();

        WorkspaceItem wsItem = workspaceItemService.create(context, collection, false);

        WorkflowItem wfItem = WorkflowServiceFactory.getInstance().getWorkflowService().start(context, wsItem);

        Item item = wfItem.getItem();
        itemService.addMetadata(context, item, "dc", "contributor", "author", null, "Author, A. N.");
        itemService.addMetadata(context, item, "dc", "title", null, null, "A Test Object");
        itemService.addMetadata(context, item, "dc", "publisher", null, null, "DSpace Test Harness");

        itemService.update(context, item);
        //we need to commit the changes so we don't block the table for testing
        context.restoreAuthSystemState();

        return item;
    }

    /**
     * Create a new, randomly generate, DOI with the given status
     *
     * @param item the Item to associate with the DOI
     * @param status the status of the DOI (such as DOIIdentifierProvider.TO_BE_REGISTERED
     * @return the String representing the DOI
     * @throws SQLException if a database error occurs
     * @throws AuthorizeException if an authorization error occurs
     */
    protected String createDOI(Item item, Integer status)
        throws SQLException, AuthorizeException {
        return this.createDOI(item, status, null);
    }

    /**
     * Create a DOI to an item.
     *
     * @param item     Item the DOI should be created for.
     * @param status   The status of the DOI.
     * @param doi      The DOI or null if we should generate one.
     * @return the DOI
     * @throws SQLException if database error
     */
    protected String createDOI(Item item, Integer status, String doi)
        throws SQLException {
        context.turnOffAuthorisationSystem();
        // we need some random data. UUIDs would be bloated here
        Random random = new Random();
        if (null == doi) {
            doi = DOI.SCHEME + PREFIX + "/" + NAMESPACE_SEPARATOR
                + Long.toHexString(Instant.now().toEpochMilli()) + "-"
                + random.nextInt(997);
        }

        DOI doiRow = doiService.create(context);
        doiRow.setDoi(doi.substring(DOI.SCHEME.length()));
        doiRow.setDSpaceObject(item);
        doiRow.setStatus(status);
        doiService.update(context, doiRow);

        //we need to commit the changes so we don't block the table for testing
        context.restoreAuthSystemState();
        return doi;
    }

    @Test
    public void testPurgeEmptyDOIsWithStatus()
        throws AuthorizeException, IllegalAccessException, IOException, SQLException, WorkflowException {
        Item item = newItem();

        // Create two DOIs, one with an item, the other with a null item
        createDOI(item, DOIIdentifierProvider.TO_BE_REGISTERED, null);
        createDOI(null, DOIIdentifierProvider.TO_BE_REGISTERED, null);

        List<DOI> dois = doiService.getDOIsByStatus(context, Arrays.asList(DOIIdentifierProvider.TO_BE_REGISTERED));

        assertThat(
            "No DOI with null dspace_object found", dois,
            hasItem(hasProperty("DSpaceObject", is(nullValue())))
        );

        DOIOrganiser organiser = new DOIOrganiser(context,
            new DSpace().getSingletonService(DOIIdentifierProvider.class));
        organiser.purgeEmptyDOIsWithStatus(context, DOIIdentifierProvider.TO_BE_REGISTERED);

        dois = doiService.getDOIsByStatus(context, Arrays.asList(DOIIdentifierProvider.TO_BE_REGISTERED));

        // Verify that the DOI with the null item has been removed.
        assertThat(
            "DOI with null dspace_object found", dois,
            not(hasItem(hasProperty("DSpaceObject", is(nullValue()))))
        );
    }
}
