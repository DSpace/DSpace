/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.deletionprocess;

import static com.jayway.jsonpath.JsonPath.read;
import static org.dspace.builder.CollectionBuilder.createCollection;
import static org.dspace.builder.CommunityBuilder.createCommunity;
import static org.dspace.builder.ItemBuilder.createItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.dspace.app.rest.converter.DSpaceRunnableParameterConverter;
import org.dspace.app.rest.model.ParameterValueRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.app.rest.test.AbstractEntityIntegrationTest;
import org.dspace.builder.EPersonBuilder;
import org.dspace.builder.ProcessBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
import org.dspace.content.service.ItemService;
import org.dspace.discovery.IndexingService;
import org.dspace.eperson.EPerson;
import org.dspace.scripts.DSpaceCommandLineParameter;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

/**
 * Integration tests for DSpaceObjectDeletionProcess run through the ScriptRestRepository.
 * Tests deletion permissions for community admin, collection admin, and item admin.
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk@4science.com)
 */
public class DSpaceObjectDeletionProcessIT extends AbstractEntityIntegrationTest {

    @Autowired
    private DSpaceRunnableParameterConverter dSpaceRunnableParameterConverter;

    @Autowired
    private IndexingService indexingService;

    private ItemService itemService = ContentServiceFactory.getInstance().getItemService();
    private CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();
    private CommunityService communityService = ContentServiceFactory.getInstance().getCommunityService();

    private Community community;
    private Collection collection;
    private Item item;

    private EPerson comAdmin;
    private EPerson colAdmin;
    private EPerson itemAdmin;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        context.turnOffAuthorisationSystem();

        comAdmin = EPersonBuilder.createEPerson(context)
                                 .withEmail("comAdmin@example.com")
                                 .withPassword(password).build();

        colAdmin = EPersonBuilder.createEPerson(context)
                                 .withEmail("colAdmin@example.com")
                                 .withPassword(password).build();

        itemAdmin = EPersonBuilder.createEPerson(context)
                                  .withEmail("itemAdmin@example.com")
                                  .withPassword(password).build();

        context.commit();
        context.restoreAuthSystemState();

        // Commit to Solr so that isComColAdmin() and isItemAdmin() can find the objects
        indexingService.commit();
    }

    /**
     * Test that repository admin can delete a community.
     */
    @Test
    public void testAdminCanDeleteCommunity() throws Exception {
        context.turnOffAuthorisationSystem();
        Community communityToDelete = createCommunity(context)
            .withName("Community To Delete")
            .build();
        context.commit();
        context.restoreAuthSystemState();

        LinkedList<DSpaceCommandLineParameter> parameters = new LinkedList<>();
        parameters.add(new DSpaceCommandLineParameter("-i", communityToDelete.getID().toString()));

        performDeletionScript(parameters, admin, HttpStatus.ACCEPTED, communityToDelete);
    }

    /**
     * Test that community admin can delete a community.
     */
    @Test
    public void testCommunityAdminCanDeleteCommunity() throws Exception {
        context.turnOffAuthorisationSystem();
        Community communityToDelete = createCommunity(context)
            .withName("Community To Delete")
            .withAdminGroup(comAdmin)
            .build();
        context.commit();
        context.restoreAuthSystemState();
        indexingService.commit();

        LinkedList<DSpaceCommandLineParameter> parameters = new LinkedList<>();
        parameters.add(new DSpaceCommandLineParameter("-i", communityToDelete.getID().toString()));

        performDeletionScript(parameters, comAdmin, HttpStatus.ACCEPTED, communityToDelete);
    }

    /**
     * Test that collection admin can delete a collection.
     */
    @Test
    public void testCollectionAdminCanDeleteCollection() throws Exception {
        context.turnOffAuthorisationSystem();
        Community tempCommunity = createCommunity(context)
            .withName("Temp Community for Collection")
            .build();
        Collection collectionToDelete = createCollection(context, tempCommunity)
            .withName("Collection To Delete")
            .withAdminGroup(colAdmin)
            .build();
        context.commit();
        context.restoreAuthSystemState();
        indexingService.commit();

        LinkedList<DSpaceCommandLineParameter> parameters = new LinkedList<>();
        parameters.add(new DSpaceCommandLineParameter("-i", collectionToDelete.getID().toString()));

        performDeletionScript(parameters, colAdmin, HttpStatus.ACCEPTED, collectionToDelete);
    }

    /**
     * Test that item admin can delete an item.
     */
    @Test
    public void testItemAdminCanDeleteItem() throws Exception {
        context.turnOffAuthorisationSystem();
        community = createCommunity(context)
            .withName("Test Community")
            .build();

        collection = createCollection(context, community)
            .withName("Test Collection")
            .build();

        Item itemToDelete = createItem(context, collection)
            .withTitle("Item To Delete")
            .withAdminUser(itemAdmin)
            .build();
        context.commit();
        context.restoreAuthSystemState();
        indexingService.commit();

        LinkedList<DSpaceCommandLineParameter> parameters = new LinkedList<>();
        parameters.add(new DSpaceCommandLineParameter("-i", itemToDelete.getID().toString()));

        performDeletionScript(parameters, itemAdmin, HttpStatus.ACCEPTED, itemToDelete);
    }

    /**
     * Test that community admin cannot delete a collection they don't administer.
     */
    @Test
    public void testCommunityAdminCannotDeleteCollection() throws Exception {
        context.turnOffAuthorisationSystem();
        Community otherCommunity = createCommunity(context)
            .withName("Other Community")
            .build();
        Collection otherCollection = createCollection(context, otherCommunity)
            .withName("Other Collection")
            .build();
        context.commit();
        context.restoreAuthSystemState();
        indexingService.commit();

        LinkedList<DSpaceCommandLineParameter> parameters = new LinkedList<>();
        parameters.add(new DSpaceCommandLineParameter("-i", otherCollection.getID().toString()));

        // Community admin can execute the script (they are a ComColAdmin)
        // but the deletion should fail because they don't administer this specific collection
        performDeletionScript(parameters, comAdmin, HttpStatus.FORBIDDEN, otherCollection);
    }

    /**
     * Test that collection admin cannot delete an item they don't administer.
     */
    @Test
    public void testCollectionAdminCannotDeleteItem() throws Exception {
        context.turnOffAuthorisationSystem();

        Community community = createCommunity(context)
            .withName("Test Community")
            .withAdminGroup(comAdmin)
            .build();

        Collection otherCollection = createCollection(context, community)
            .withName("Other Collection")
            .build();
        Item otherItem = createItem(context, otherCollection)
            .withTitle("Other Item")
            .build();
        context.commit();
        context.restoreAuthSystemState();
        indexingService.commit();

        LinkedList<DSpaceCommandLineParameter> parameters = new LinkedList<>();
        parameters.add(new DSpaceCommandLineParameter("-i", otherItem.getID().toString()));

        // Collection admin can execute the script (they are a ComColAdmin)
        // but the deletion should fail because they don't administer this specific item
        performDeletionScript(parameters, colAdmin, HttpStatus.FORBIDDEN, otherItem);
    }

    /**
     * Test that item admin cannot delete another item they don't administer.
     */
    @Test
    public void testItemAdminCannotDeleteAnotherItem() throws Exception {
        context.turnOffAuthorisationSystem();
        community = createCommunity(context)
            .withName("Test Community")
            .build();
        collection = createCollection(context, community)
            .withName("Test Collection")
            .build();
        Item otherItem = createItem(context, collection)
            .withTitle("Other Item")
            .build();
        context.restoreAuthSystemState();
        indexingService.commit();

        LinkedList<DSpaceCommandLineParameter> parameters = new LinkedList<>();
        parameters.add(new DSpaceCommandLineParameter("-i", otherItem.getID().toString()));

        // Item admin can execute the script (they are an ItemAdmin)
        // but the deletion should fail because they don't administer this specific item
        performDeletionScript(parameters, itemAdmin, HttpStatus.FORBIDDEN, otherItem);
    }

    /**
     * Test that regular eperson cannot delete any object.
     */
    @Test
    public void testRegularEPersonCannotDelete() throws Exception {
        context.turnOffAuthorisationSystem();
        community = createCommunity(context)
            .withName("Test Community")
            .build();

        collection = createCollection(context, community)
            .withName("Test Collection")
            .build();

        item = createItem(context, collection)
            .withTitle("Test Item")
            .build();
        context.restoreAuthSystemState();

        LinkedList<DSpaceCommandLineParameter> parameters = new LinkedList<>();
        parameters.add(new DSpaceCommandLineParameter("-i", item.getID().toString()));

        performDeletionScript(parameters, eperson, HttpStatus.FORBIDDEN, item);
    }

    /**
     * Test that anonymous user cannot delete any object.
     */
    @Test
    public void testAnonymousCannotDelete() throws Exception {

        context.turnOffAuthorisationSystem();
        community = createCommunity(context)
            .withName("Test Community")
            .build();

        collection = createCollection(context, community)
            .withName("Test Collection")
            .build();

        item = createItem(context, collection)
            .withTitle("Test Item")
            .build();
        context.restoreAuthSystemState();

        LinkedList<DSpaceCommandLineParameter> parameters = new LinkedList<>();
        parameters.add(new DSpaceCommandLineParameter("-i", item.getID().toString()));

        List<ParameterValueRest> list = convertParameters(parameters);

        getClient()
            .perform(multipart("/api/system/scripts/object-deletion/processes")
                         .param("properties", new ObjectMapper().writeValueAsString(list)))
            .andExpect(status().isUnauthorized());

        // Verify item still exists
        assertThat("Item should still exist", itemService.find(context, item.getID()), notNullValue());
    }

    private void performDeletionScript(
        LinkedList<DSpaceCommandLineParameter> parameters, EPerson user, HttpStatus expectedHttpStatus,
        DSpaceObject objectToDelete) throws Exception {

        List<ParameterValueRest> list = convertParameters(parameters);
        AtomicReference<Integer> idRef = new AtomicReference<>();

        try {
            String token = getAuthToken(user.getEmail(), password);

            getClient(token)
                .perform(multipart("/api/system/scripts/object-deletion/processes")
                             .param("properties", new ObjectMapper().writeValueAsString(list)))
                .andExpect(status().is(expectedHttpStatus.value()))
                .andDo(result -> {
                    if (expectedHttpStatus == HttpStatus.ACCEPTED) {
                        idRef.set(read(result.getResponse().getContentAsString(), "$.processId"));
                    }
                });

            // Verify deletion status based on expected HTTP status
            if (expectedHttpStatus == HttpStatus.ACCEPTED) {
                // Object should be deleted
                verifyObjectDeleted(objectToDelete);
            } else {
                // Object should still exist
                verifyObjectExists(objectToDelete);
            }
        } finally {
            if (idRef.get() != null) {
                ProcessBuilder.deleteProcess(idRef.get());
            }
        }
    }

    private void verifyObjectDeleted(DSpaceObject object) throws SQLException {
        if (object instanceof Item) {
            assertThat("Item expected to be deleted", itemService.find(context, object.getID()), nullValue());
        } else if (object instanceof Collection) {
            assertThat("Collection expected to be deleted", collectionService.find(context, object.getID()),
                       nullValue());
        } else if (object instanceof Community) {
            assertThat("Community expected to be deleted", communityService.find(context, object.getID()), nullValue());
        }
    }

    private void verifyObjectExists(DSpaceObject object) throws SQLException {
        if (object instanceof Item) {
            assertThat("Item expected to still exist", itemService.find(context, object.getID()), notNullValue());
        } else if (object instanceof Collection) {
            assertThat("Collection expected to still exist", collectionService.find(context, object.getID()),
                       notNullValue());
        } else if (object instanceof Community) {
            assertThat("Community expected to still exist", communityService.find(context, object.getID()),
                       notNullValue());
        }
    }

    private List<ParameterValueRest> convertParameters(LinkedList<DSpaceCommandLineParameter> parameters) {
        return parameters.stream()
                         .map(dSpaceCommandLineParameter ->
                                  dSpaceRunnableParameterConverter.convert(dSpaceCommandLineParameter,
                                                                           Projection.DEFAULT))
                         .collect(Collectors.toList());
    }

}