/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.sql.SQLException;
import java.util.List;

import org.apache.logging.log4j.Logger;
import org.dspace.AbstractUnitTest;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
import org.dspace.content.service.EntityTypeService;
import org.dspace.content.service.InstallItemService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.MetadataValueService;
import org.dspace.content.service.RelationshipService;
import org.dspace.content.service.RelationshipTypeService;
import org.dspace.content.service.WorkspaceItemService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class RelationshipServiceImplPlaceTest extends AbstractUnitTest {

    private static final Logger log = org.apache.logging.log4j.LogManager
        .getLogger(RelationshipServiceImplPlaceTest.class);

    protected RelationshipService relationshipService = ContentServiceFactory.getInstance().getRelationshipService();
    protected RelationshipTypeService relationshipTypeService = ContentServiceFactory.getInstance()
                                                                                     .getRelationshipTypeService();
    protected EntityTypeService entityTypeService = ContentServiceFactory.getInstance().getEntityTypeService();
    protected CommunityService communityService = ContentServiceFactory.getInstance().getCommunityService();
    protected CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();
    protected ItemService itemService = ContentServiceFactory.getInstance().getItemService();
    protected InstallItemService installItemService = ContentServiceFactory.getInstance().getInstallItemService();
    protected WorkspaceItemService workspaceItemService = ContentServiceFactory.getInstance().getWorkspaceItemService();
    protected MetadataValueService metadataValueService = ContentServiceFactory.getInstance().getMetadataValueService();

    Community community;
    Collection col;

    Item item;
    Item authorItem;
    RelationshipType isAuthorOfPublication;
    EntityType publicationEntityType;
    EntityType authorEntityType;

    String authorQualifier = "author";
    String contributorElement = "contributor";
    String dcSchema = "dc";

    /**
     * This method will be run before every test as per @Before. It will
     * initialize resources required for the tests.
     */
    @Before
    @Override
    public void init() {
        super.init();
        try {
            context.turnOffAuthorisationSystem();
            community = communityService.create(null, context);

            col = collectionService.create(context, community);
            WorkspaceItem is = workspaceItemService.create(context, col, false);
            WorkspaceItem authorIs = workspaceItemService.create(context, col, false);

            item = installItemService.installItem(context, is);
            itemService.addMetadata(context, item, "relationship", "type", null, null, "Publication");

            authorItem = installItemService.installItem(context, authorIs);
            itemService.addMetadata(context, authorItem, "relationship", "type", null, null, "Person");
            itemService.addMetadata(context, authorItem, "person", "familyName", null, null, "familyName");
            itemService.addMetadata(context, authorItem, "person", "givenName", null, null, "firstName");

            publicationEntityType = entityTypeService.create(context, "Publication");
            authorEntityType = entityTypeService.create(context, "Person");
            isAuthorOfPublication = relationshipTypeService
                .create(context, publicationEntityType, authorEntityType,
                        "isAuthorOfPublication", "isPublicationOfAuthor",
                        null, null, null, null);

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
     */
    @After
    @Override
    public void destroy() {
        context.abort();
        super.destroy();
    }

    /**
     * This test will test the use case of having an item to which we add some metadata. After that, we'll add a single
     * relationship to this Item. We'll test whether the places are correct, in this case it'll be the two metadata
     * values that we created first, has to have place 0 and 1. The Relationship that we just created needs to have
     * leftPlace 2 and the metadata value resulting from that Relationship needs to also have place 2.
     * Once these assertions succeed, we basically repeat said process with new metadata values and a new relationship.
     * We then test if the old assertions still hold true like they should and that the new ones behave as expected
     * as well.
     * @throws Exception    If something goes wrong
     */
    @Test
    public void addMetadataAndRelationshipTest() throws Exception {
        context.turnOffAuthorisationSystem();

        // Here we add the first set of metadata to the item
        itemService.addMetadata(context, item, dcSchema, contributorElement, authorQualifier, null, "test, one");
        itemService.addMetadata(context, item, dcSchema, contributorElement, authorQualifier, null, "test, two");

        // Here we create the first Relationship to the item
        Relationship relationship = relationshipService
            .create(context, item, authorItem, isAuthorOfPublication, -1, -1);

        context.restoreAuthSystemState();

        // The code below performs the mentioned assertions to ensure the place is correct
        List<MetadataValue> list = itemService
            .getMetadata(item, dcSchema, contributorElement, authorQualifier, Item.ANY);
        assertThat(list.size(), equalTo(3));

        assertMetadataValue(authorQualifier, contributorElement, dcSchema, "test, one", null, 0, list.get(0));
        assertMetadataValue(authorQualifier, contributorElement, dcSchema, "test, two", null, 1, list.get(1));
        assertMetadataValue(authorQualifier, contributorElement, dcSchema, "familyName, firstName",
                            "virtual::" + relationship.getID(), 2, list.get(2));
        assertThat(relationship.getLeftPlace(), equalTo(2));

        context.turnOffAuthorisationSystem();

        // This is where we add the second set of metadata values
        itemService.addMetadata(context, item, dcSchema, contributorElement, authorQualifier, null, "test, three");
        itemService.addMetadata(context, item, dcSchema, contributorElement, authorQualifier, null, "test, four");

        // Here we create an Item so that we can create another relationship with this item
        WorkspaceItem authorIs = workspaceItemService.create(context, col, false);
        Item secondAuthorItem = installItemService.installItem(context, authorIs);
        itemService.addMetadata(context, secondAuthorItem, "relationship", "type", null, null, "Person");
        itemService.addMetadata(context, secondAuthorItem, "person", "familyName", null, null, "familyNameTwo");
        itemService.addMetadata(context, secondAuthorItem, "person", "givenName", null, null, "firstNameTwo");
        Relationship relationshipTwo = relationshipService
            .create(context, item, secondAuthorItem, isAuthorOfPublication, -1, -1);

        context.restoreAuthSystemState();

        // Here we retrieve the list of metadata again to perform the assertions on the places below as mentioned
        list = itemService.getMetadata(item, dcSchema, contributorElement, authorQualifier, Item.ANY);

        assertMetadataValue(authorQualifier, contributorElement, dcSchema, "test, one", null, 0, list.get(0));
        assertMetadataValue(authorQualifier, contributorElement, dcSchema, "test, two", null, 1, list.get(1));
        assertMetadataValue(authorQualifier, contributorElement, dcSchema, "familyName, firstName",
                            "virtual::" + relationship.getID(), 2, list.get(2));
        assertThat(relationship.getLeftPlace(), equalTo(2));
        assertMetadataValue(authorQualifier, contributorElement, dcSchema, "test, three", null, 3, list.get(3));
        assertMetadataValue(authorQualifier, contributorElement, dcSchema, "test, four", null, 4, list.get(4));
        assertMetadataValue(authorQualifier, contributorElement, dcSchema, "familyNameTwo, firstNameTwo",
                            "virtual::" + relationshipTwo.getID(), 5, list.get(5));
        assertThat(relationshipTwo.getLeftPlace(), equalTo(5));

    }

    /**
     * This test is virtually the same as above, only this time we'll add Relationships with leftPlaces already set
     * equal to what they HAVE to be. So in the first test addMetadataAndRelationshipTest, we didn't specify a place
     * and left it up to the Service to determine it, here we provide a correct place already.
     * We perform the exact same logic except that we give a proper place already to the Relationships and we
     * perform the same checks
     * @throws Exception    If something goes wrong
     */
    @Test
    public void AddMetadataAndRelationshipWithSpecificPlaceTest() throws Exception {
        context.turnOffAuthorisationSystem();

        // Here we add the first set of metadata to the item
        itemService.addMetadata(context, item, dcSchema, contributorElement, authorQualifier, null, "test, one");
        itemService.addMetadata(context, item, dcSchema, contributorElement, authorQualifier, null, "test, two");

        // Here we create the first Relationship to the item with the specific leftPlace: 2
        Relationship relationship = relationshipService.create(context, item, authorItem, isAuthorOfPublication, 2, -1);

        context.restoreAuthSystemState();

        // The code below performs the mentioned assertions to ensure the place is correct
        List<MetadataValue> list = itemService
            .getMetadata(item, dcSchema, contributorElement, authorQualifier, Item.ANY);
        assertThat(list.size(), equalTo(3));

        assertMetadataValue(authorQualifier, contributorElement, dcSchema, "test, one", null, 0, list.get(0));
        assertMetadataValue(authorQualifier, contributorElement, dcSchema, "test, two", null, 1, list.get(1));
        assertMetadataValue(authorQualifier, contributorElement, dcSchema, "familyName, firstName",
                            "virtual::" + relationship.getID(), 2, list.get(2));
        assertThat(relationship.getLeftPlace(), equalTo(2));

        context.turnOffAuthorisationSystem();

        // This is where we add the second set of metadata values
        itemService.addMetadata(context, item, dcSchema, contributorElement, authorQualifier, null, "test, three");
        itemService.addMetadata(context, item, dcSchema, contributorElement, authorQualifier, null, "test, four");

        // Here we create an Item so that we can create another relationship with this item. We'll give this
        // Relationship a specific place as well
        WorkspaceItem authorIs = workspaceItemService.create(context, col, false);
        Item secondAuthorItem = installItemService.installItem(context, authorIs);
        itemService.addMetadata(context, secondAuthorItem, "relationship", "type", null, null, "Person");
        itemService.addMetadata(context, secondAuthorItem, "person", "familyName", null, null, "familyNameTwo");
        itemService.addMetadata(context, secondAuthorItem, "person", "givenName", null, null, "firstNameTwo");
        Relationship relationshipTwo = relationshipService
            .create(context, item, secondAuthorItem, isAuthorOfPublication, 5, -1);

        context.restoreAuthSystemState();

        // Here we retrieve the list of metadata again to perform the assertions on the places below as mentioned
        list = itemService.getMetadata(item, dcSchema, contributorElement, authorQualifier, Item.ANY);

        assertMetadataValue(authorQualifier, contributorElement, dcSchema, "test, one", null, 0, list.get(0));
        assertMetadataValue(authorQualifier, contributorElement, dcSchema, "test, two", null, 1, list.get(1));
        assertMetadataValue(authorQualifier, contributorElement, dcSchema, "familyName, firstName",
                            "virtual::" + relationship.getID(), 2, list.get(2));
        assertThat(relationship.getLeftPlace(), equalTo(2));

        assertMetadataValue(authorQualifier, contributorElement, dcSchema, "test, three", null, 3, list.get(3));
        assertMetadataValue(authorQualifier, contributorElement, dcSchema, "test, four", null, 4, list.get(4));
        assertMetadataValue(authorQualifier, contributorElement, dcSchema, "familyNameTwo, firstNameTwo",
                            "virtual::" + relationshipTwo.getID(), 5, list.get(5));
        assertThat(relationshipTwo.getLeftPlace(), equalTo(5));

    }


    /**
     * In this test, our goal will be to add a bunch of metadata values to then remove one of them. We'll check the list
     * of metadata values for the item and check that the places have not been altered for the metadata values IF an
     * item.update hadn't been called yet. We'll then create a Relationship (by which an item.update will be called)
     * and then we check that the places are set correctly.
     * We then repeat this process once more and check that everything works as intended
     * @throws Exception
     */
    @Test
    public void AddAndRemoveMetadataAndRelationshipsTest() throws Exception {
        context.turnOffAuthorisationSystem();

        // Here we add the first set of metadata to the item
        itemService.addMetadata(context, item, dcSchema, contributorElement, authorQualifier, null, "test, one");
        itemService.addMetadata(context, item, dcSchema, contributorElement, authorQualifier, null, "test, two");
        itemService.addMetadata(context, item, dcSchema, contributorElement, authorQualifier, null, "test, three");

        // Get a specific metadatavlaue to remove
        MetadataValue metadataValueToRemove = itemService.getMetadata(item, "dc", "contributor", "author", Item.ANY)
                                                         .get(1);
        // Remove the actual metadata value
        item.removeMetadata(metadataValueToRemove);
        metadataValueService.delete(context, metadataValueToRemove);

        context.restoreAuthSystemState();

        // Retrieve the list of mdv again
        List<MetadataValue> list = itemService
            .getMetadata(item, dcSchema, contributorElement, authorQualifier, Item.ANY);
        // Verify we only have 2 mdv left
        assertThat(list.size(), equalTo(2));

        // Check that these places are still intact after the deletion as the place doesn't get updated until an
        // item.update has been called
        assertMetadataValue(authorQualifier, contributorElement, dcSchema, "test, one", null, 0, list.get(0));
        assertMetadataValue(authorQualifier, contributorElement, dcSchema, "test, three", null, 2, list.get(1));

        context.turnOffAuthorisationSystem();

        // Create a relationship with this item with a spcific place
        Relationship relationship = relationshipService.create(context, item, authorItem, isAuthorOfPublication, 1, -1);

        context.restoreAuthSystemState();

        // Retrieve the list again and verify that the creation of the Relationship added an additional mdv
        list = itemService.getMetadata(item, dcSchema, contributorElement, authorQualifier, Item.ANY);
        assertThat(list.size(), equalTo(3));

        // Assert that the mdv are well placed
        assertMetadataValue(authorQualifier, contributorElement, dcSchema, "test, one", null, 0, list.get(0));
        assertMetadataValue(authorQualifier, contributorElement, dcSchema, "familyName, firstName",
                            "virtual::" + relationship.getID(), 1, list.get(1));
        assertThat(relationship.getLeftPlace(), equalTo(1));

        assertMetadataValue(authorQualifier, contributorElement, dcSchema, "test, three", null, 2, list.get(2));

        context.turnOffAuthorisationSystem();

        // Add two extra mdv
        itemService.addMetadata(context, item, dcSchema, contributorElement, authorQualifier, null, "test, four");
        itemService.addMetadata(context, item, dcSchema, contributorElement, authorQualifier, null, "test, five");

        //This is author "test, four" that we're removing
        metadataValueToRemove = itemService.getMetadata(item, "dc", "contributor", "author", Item.ANY).get(3);
        item.removeMetadata(metadataValueToRemove);
        metadataValueService.delete(context, metadataValueToRemove);

        context.restoreAuthSystemState();

        list = itemService.getMetadata(item, dcSchema, contributorElement, authorQualifier, Item.ANY);

        // Check that these places are still intact after the deletion as the place doesn't get updated until an
        // item.update has been called
        assertMetadataValue(authorQualifier, contributorElement, dcSchema, "test, one", null, 0, list.get(0));
        assertMetadataValue(authorQualifier, contributorElement, dcSchema, "familyName, firstName",
                            "virtual::" + relationship.getID(), 1, list.get(1));
        assertThat(relationship.getLeftPlace(), equalTo(1));

        assertMetadataValue(authorQualifier, contributorElement, dcSchema, "test, three", null, 2, list.get(2));
        assertMetadataValue(authorQualifier, contributorElement, dcSchema, "test, five", null, 4, list.get(3));

        context.turnOffAuthorisationSystem();

        // Create an additional item for another relationship
        WorkspaceItem authorIs = workspaceItemService.create(context, col, false);
        Item secondAuthorItem = installItemService.installItem(context, authorIs);
        itemService.addMetadata(context, secondAuthorItem, "relationship", "type", null, null, "Person");
        itemService.addMetadata(context, secondAuthorItem, "person", "familyName", null, null, "familyNameTwo");
        itemService.addMetadata(context, secondAuthorItem, "person", "givenName", null, null, "firstNameTwo");
        Relationship relationshipTwo = relationshipService
            .create(context, item, secondAuthorItem, isAuthorOfPublication, 3, -1);

        context.restoreAuthSystemState();

        // Check that the other mdv are still okay and that the creation of the relationship added
        // another correct mdv to the item
        list = itemService.getMetadata(item, dcSchema, contributorElement, authorQualifier, Item.ANY);

        assertMetadataValue(authorQualifier, contributorElement, dcSchema, "test, one", null, 0, list.get(0));
        assertMetadataValue(authorQualifier, contributorElement, dcSchema, "familyName, firstName",
                            "virtual::" + relationship.getID(), 1, list.get(1));
        assertThat(relationship.getLeftPlace(), equalTo(1));

        assertMetadataValue(authorQualifier, contributorElement, dcSchema, "test, three", null, 2, list.get(2));
        assertMetadataValue(authorQualifier, contributorElement, dcSchema, "familyNameTwo, firstNameTwo",
                            "virtual::" + relationshipTwo.getID(), 3, list.get(3));
        assertThat(relationshipTwo.getLeftPlace(), equalTo(3));

        assertMetadataValue(authorQualifier, contributorElement, dcSchema, "test, five", null, 4, list.get(4));

    }

    @Test
    public void AddAndUpdateMetadataAndRelationshipsTest() throws Exception {
        context.turnOffAuthorisationSystem();

        // Add metadata and relationships to the item
        itemService.addMetadata(context, item, dcSchema, contributorElement, authorQualifier, null, "test, one");
        itemService.addMetadata(context, item, dcSchema, contributorElement, authorQualifier, null, "test, two");
        itemService.addMetadata(context, item, dcSchema, contributorElement, authorQualifier, null, "test, three");

        Relationship relationship = relationshipService
            .create(context, item, authorItem, isAuthorOfPublication, -1, -1);

        context.restoreAuthSystemState();

        // Get the list of mdv and assert that they're correct
        List<MetadataValue> list = itemService
            .getMetadata(item, dcSchema, contributorElement, authorQualifier, Item.ANY);
        assertThat(list.size(), equalTo(4));

        assertMetadataValue(authorQualifier, contributorElement, dcSchema, "test, one", null, 0, list.get(0));
        assertMetadataValue(authorQualifier, contributorElement, dcSchema, "test, two", null, 1, list.get(1));
        assertMetadataValue(authorQualifier, contributorElement, dcSchema, "test, three", null, 2, list.get(2));
        assertMetadataValue(authorQualifier, contributorElement, dcSchema, "familyName, firstName",
                            "virtual::" + relationship.getID(), 3, list.get(3));
        assertThat(relationship.getLeftPlace(), equalTo(3));


        context.turnOffAuthorisationSystem();

        MetadataValue metadataValueToUpdate = itemService.getMetadata(item, "dc", "contributor", "author", Item.ANY)
                                                         .get(1);

        // Switching the places of this relationship and metadata value to verify in the test later on that this
        // updating works
        metadataValueToUpdate.setPlace(3);
        metadataValueService.update(context, metadataValueToUpdate);
        relationship.setLeftPlace(1);
        relationshipService.update(context, relationship);

        context.restoreAuthSystemState();

        // Retrieve the list again and verify that the updating did indeed work
        list = itemService.getMetadata(item, dcSchema, contributorElement, authorQualifier, Item.ANY);

        assertMetadataValue(authorQualifier, contributorElement, dcSchema, "test, one", null, 0, list.get(0));
        assertMetadataValue(authorQualifier, contributorElement, dcSchema, "test, two", null, 3, list.get(3));
        assertMetadataValue(authorQualifier, contributorElement, dcSchema, "test, three", null, 2, list.get(2));
        assertMetadataValue(authorQualifier, contributorElement, dcSchema, "familyName, firstName",
                            "virtual::" + relationship.getID(), 1, list.get(1));
        assertThat(relationship.getLeftPlace(), equalTo(1));


    }

    private void assertMetadataValue(String authorQualifier, String contributorElement, String dcSchema, String value,
                                     String authority, int place, MetadataValue metadataValue) {
        assertThat(metadataValue.getValue(), equalTo(value));
        assertThat(metadataValue.getMetadataField().getMetadataSchema().getName(), equalTo(dcSchema));
        assertThat(metadataValue.getMetadataField().getElement(), equalTo(contributorElement));
        assertThat(metadataValue.getMetadataField().getQualifier(), equalTo(authorQualifier));
        assertThat(metadataValue.getAuthority(), equalTo(authority));
        assertThat(metadataValue.getPlace(), equalTo(place));
    }


}
