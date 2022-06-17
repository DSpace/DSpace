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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
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
import org.dspace.core.Constants;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class RelationshipServiceImplPlaceTest extends AbstractUnitTest {

    private static final Logger log = org.apache.logging.log4j.LogManager
        .getLogger(RelationshipServiceImplPlaceTest.class);

    protected RelationshipService relationshipService = ContentServiceFactory.getInstance().getRelationshipService();
    protected RelationshipTypeService relationshipTypeService = ContentServiceFactory.getInstance()
                                                                                     .getRelationshipTypeService();
    protected RelationshipMetadataService relationshipMetadataService =
        ContentServiceFactory.getInstance().getRelationshipMetadataService();
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

    Item author1;
    Item author2;
    Item author3;
    Item author4;
    Item author5;
    Item author6;
    Item publication1;
    Item publication2;
    Item publication3;
    Item publication4;
    Item publication5;
    Item publication6;
    Item project1;
    Item project2;
    Item project3;
    Item project4;
    Item project5;
    Item project6;

    RelationshipType isAuthorOfPublication;
    RelationshipType isProjectOfPublication;
    RelationshipType isProjectOfPerson;

    EntityType publicationEntityType;
    EntityType projectEntityType;
    EntityType personEntityType;

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
            itemService.addMetadata(context, item, "dspace", "entity", "type", null, "Publication");

            authorItem = installItemService.installItem(context, authorIs);
            itemService.addMetadata(context, authorItem, "dspace", "entity", "type", null, "Person");
            itemService.addMetadata(context, authorItem, "person", "familyName", null, null, "familyName");
            itemService.addMetadata(context, authorItem, "person", "givenName", null, null, "firstName");

            WorkspaceItem wi;

            wi = workspaceItemService.create(context, col, false);
            author1 = installItemService.installItem(context, wi);
            itemService.addMetadata(context, author1, "dspace", "entity", "type", null, "Person");
            itemService.addMetadata(context, author1, "person", "familyName", null, null, "Author");
            itemService.addMetadata(context, author1, "person", "givenName", null, null, "First");

            wi = workspaceItemService.create(context, col, false);
            author2 = installItemService.installItem(context, wi);
            itemService.addMetadata(context, author2, "dspace", "entity", "type", null, "Person");
            itemService.addMetadata(context, author2, "person", "familyName", null, null, "Author");
            itemService.addMetadata(context, author2, "person", "givenName", null, null, "Second");

            wi = workspaceItemService.create(context, col, false);
            author3 = installItemService.installItem(context, wi);
            itemService.addMetadata(context, author3, "dspace", "entity", "type", null, "Person");
            itemService.addMetadata(context, author3, "person", "familyName", null, null, "Author");
            itemService.addMetadata(context, author3, "person", "givenName", null, null, "Third");

            wi = workspaceItemService.create(context, col, false);
            author4 = installItemService.installItem(context, wi);
            itemService.addMetadata(context, author4, "dspace", "entity", "type", null, "Person");
            itemService.addMetadata(context, author4, "person", "familyName", null, null, "Author");
            itemService.addMetadata(context, author4, "person", "givenName", null, null, "Fourth");

            wi = workspaceItemService.create(context, col, false);
            author5 = installItemService.installItem(context, wi);
            itemService.addMetadata(context, author5, "dspace", "entity", "type", null, "Person");
            itemService.addMetadata(context, author5, "person", "familyName", null, null, "Author");
            itemService.addMetadata(context, author5, "person", "givenName", null, null, "Fifth");

            wi = workspaceItemService.create(context, col, false);
            author6 = installItemService.installItem(context, wi);
            itemService.addMetadata(context, author6, "dspace", "entity", "type", null, "Person");
            itemService.addMetadata(context, author6, "person", "familyName", null, null, "Author");
            itemService.addMetadata(context, author6, "person", "givenName", null, null, "Sixth");

            wi = workspaceItemService.create(context, col, false);
            publication1 = installItemService.installItem(context, wi);
            itemService.addMetadata(context, publication1, "dspace", "entity", "type", null, "Publication");
            itemService.addMetadata(context, publication1, "dc", "title", null, null, "Publication 1");

            wi = workspaceItemService.create(context, col, false);
            publication2 = installItemService.installItem(context, wi);
            itemService.addMetadata(context, publication2, "dspace", "entity", "type", null, "Publication");
            itemService.addMetadata(context, publication2, "dc", "title", null, null, "Publication 2");

            wi = workspaceItemService.create(context, col, false);
            publication3 = installItemService.installItem(context, wi);
            itemService.addMetadata(context, publication3, "dspace", "entity", "type", null, "Publication");
            itemService.addMetadata(context, publication3, "dc", "title", null, null, "Publication 3");

            wi = workspaceItemService.create(context, col, false);
            publication4 = installItemService.installItem(context, wi);
            itemService.addMetadata(context, publication4, "dspace", "entity", "type", null, "Publication");
            itemService.addMetadata(context, publication4, "dc", "title", null, null, "Publication 4");

            wi = workspaceItemService.create(context, col, false);
            publication5 = installItemService.installItem(context, wi);
            itemService.addMetadata(context, publication5, "dspace", "entity", "type", null, "Publication");
            itemService.addMetadata(context, publication5, "dc", "title", null, null, "Publication 5");

            wi = workspaceItemService.create(context, col, false);
            publication6 = installItemService.installItem(context, wi);
            itemService.addMetadata(context, publication6, "dspace", "entity", "type", null, "Publication");
            itemService.addMetadata(context, publication6, "dc", "title", null, null, "Publication 6");

            wi = workspaceItemService.create(context, col, false);
            project1 = installItemService.installItem(context, wi);
            itemService.addMetadata(context, project1, "dspace", "entity", "type", null, "Project");
            itemService.addMetadata(context, project1, "dc", "title", null, null, "Project 1");

            wi = workspaceItemService.create(context, col, false);
            project2 = installItemService.installItem(context, wi);
            itemService.addMetadata(context, project2, "dspace", "entity", "type", null, "Project");
            itemService.addMetadata(context, project2, "dc", "title", null, null, "Project 2");

            wi = workspaceItemService.create(context, col, false);
            project3 = installItemService.installItem(context, wi);
            itemService.addMetadata(context, project3, "dspace", "entity", "type", null, "Project");
            itemService.addMetadata(context, project3, "dc", "title", null, null, "Project 3");

            wi = workspaceItemService.create(context, col, false);
            project4 = installItemService.installItem(context, wi);
            itemService.addMetadata(context, project4, "dspace", "entity", "type", null, "Project");
            itemService.addMetadata(context, project4, "dc", "title", null, null, "Project 4");

            wi = workspaceItemService.create(context, col, false);
            project5 = installItemService.installItem(context, wi);
            itemService.addMetadata(context, project5, "dspace", "entity", "type", null, "Project");
            itemService.addMetadata(context, project5, "dc", "title", null, null, "Project 5");

            wi = workspaceItemService.create(context, col, false);
            project6 = installItemService.installItem(context, wi);
            itemService.addMetadata(context, project6, "dspace", "entity", "type", null, "Project");
            itemService.addMetadata(context, project6, "dc", "title", null, null, "Project 6");


            publicationEntityType = entityTypeService.create(context, "Publication");
            projectEntityType = entityTypeService.create(context, "Project");
            personEntityType = entityTypeService.create(context, "Person");
            isAuthorOfPublication = relationshipTypeService
                .create(context, publicationEntityType, personEntityType,
                        "isAuthorOfPublication", "isPublicationOfAuthor",
                        null, null, null, null);
            isProjectOfPublication = relationshipTypeService
                .create(context, publicationEntityType, projectEntityType,
                    "isProjectOfPublication", "isPublicationOfProject",
                    null, null, null, null);
            isProjectOfPerson = relationshipTypeService
                .create(context, personEntityType, projectEntityType,
                        "isProjectOfPerson", "isPersonOfProject",
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
        itemService.addMetadata(context, secondAuthorItem, "dspace", "entity", "type", null, "Person");
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
        itemService.addMetadata(context, secondAuthorItem, "dspace", "entity", "type", null, "Person");
        itemService.addMetadata(context, secondAuthorItem, "person", "familyName", null, null, "familyNameTwo");
        itemService.addMetadata(context, secondAuthorItem, "person", "givenName", null, null, "firstNameTwo");
        Relationship relationshipTwo = relationshipService
            .create(context, item, secondAuthorItem, isAuthorOfPublication, 1, -1);

        context.restoreAuthSystemState();

        // Here we retrieve the list of metadata again to perform the assertions on the places below as mentioned
        list = itemService.getMetadata(item, dcSchema, contributorElement, authorQualifier, Item.ANY);

        assertMetadataValue(authorQualifier, contributorElement, dcSchema, "test, one", null, 0, list.get(0));

        assertMetadataValue(authorQualifier, contributorElement, dcSchema, "familyNameTwo, firstNameTwo",
                            "virtual::" + relationshipTwo.getID(), 1, list.get(1));
        assertThat(relationshipTwo.getLeftPlace(), equalTo(1));

        assertMetadataValue(authorQualifier, contributorElement, dcSchema, "test, two", null, 2, list.get(2));

        assertMetadataValue(authorQualifier, contributorElement, dcSchema, "familyName, firstName",
                            "virtual::" + relationship.getID(), 3, list.get(3));
        assertThat(relationship.getLeftPlace(), equalTo(3));

        assertMetadataValue(authorQualifier, contributorElement, dcSchema, "test, three", null, 4, list.get(4));
        assertMetadataValue(authorQualifier, contributorElement, dcSchema, "test, four", null, 5, list.get(5));

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
        itemService.addMetadata(context, secondAuthorItem, "dspace", "entity", "type", null, "Person");
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


    /* RelationshipService#create */

    @Test
    public void createUseForPlaceRelationshipAppendingLeftNoMetadataTest() throws Exception {
        context.turnOffAuthorisationSystem();

        // Add three Authors to the same Publication, appending to the end
        Relationship r1 = relationshipService.create(context, publication1, author1, isAuthorOfPublication, -1, -1);
        Relationship r2 = relationshipService.create(context, publication1, author2, isAuthorOfPublication, -1, -1);
        Relationship r3 = relationshipService.create(context, publication1, author3, isAuthorOfPublication, -1, -1);

        context.restoreAuthSystemState();

        // Check relationship order
        assertLeftPlace(r1, 0);
        assertLeftPlace(r2, 1);
        assertLeftPlace(r3, 2);
        assertRelationMetadataOrder(publication1, isAuthorOfPublication, List.of(r1, r2, r3));
    }

    @Test
    public void createUseForPlaceRelationshipWithLeftPlaceAtTheStartNoMetadataTest() throws Exception {
        context.turnOffAuthorisationSystem();

        // Add two Authors to the same Publication, appending to the end
        Relationship r1 = relationshipService.create(context, publication1, author1, isAuthorOfPublication, -1, -1);
        Relationship r2 = relationshipService.create(context, publication1, author2, isAuthorOfPublication, -1, -1);

        // Add another Author @ leftPlace 0. The existing relationships should get pushed one place forward
        Relationship r3 = relationshipService.create(context, publication1, author3, isAuthorOfPublication, 0, -1);

        context.restoreAuthSystemState();

        // Check relationship order
        assertLeftPlace(r3, 0);
        assertLeftPlace(r1, 1);
        assertLeftPlace(r2, 2);
        assertRelationMetadataOrder(publication1, isAuthorOfPublication, List.of(r3, r1, r2));
    }

    @Test
    public void createUseForPlaceRelationshipWithLeftPlaceInTheMiddleNoMetadataTest() throws Exception {
        context.turnOffAuthorisationSystem();

        // Add two Authors to the same Publication, appending to the end
        Relationship r1 = relationshipService.create(context, publication1, author1, isAuthorOfPublication, -1, -1);
        Relationship r2 = relationshipService.create(context, publication1, author2, isAuthorOfPublication, -1, -1);

        // Add another Author @ leftPlace 1. The second relationship should get pushed by one place
        Relationship r3 = relationshipService.create(context, publication1, author3, isAuthorOfPublication, 1, -1);

        context.restoreAuthSystemState();

        // Check relationship order
        assertLeftPlace(r1, 0);
        assertLeftPlace(r3, 1);
        assertLeftPlace(r2, 2);
        assertRelationMetadataOrder(publication1, isAuthorOfPublication, List.of(r1, r3, r2));
    }

    @Test
    public void createUseForPlaceRelationshipWithLeftPlaceAtTheEndNoMetadataTest() throws Exception {
        context.turnOffAuthorisationSystem();

        // Add three Authors to the same Publication, appending to the end
        Relationship r1 = relationshipService.create(context, publication1, author1, isAuthorOfPublication, -1, -1);
        Relationship r2 = relationshipService.create(context, publication1, author2, isAuthorOfPublication, -1, -1);

        // Add another Author @ leftPlace 2. This should have the same effect as just appending it
        Relationship r3 = relationshipService.create(context, publication1, author3, isAuthorOfPublication, 2, -1);

        context.restoreAuthSystemState();

        // Check relationship order
        assertLeftPlace(r1, 0);
        assertLeftPlace(r2, 1);
        assertLeftPlace(r3, 2);
        assertRelationMetadataOrder(publication1, isAuthorOfPublication, List.of(r1, r2, r3));
    }

    @Test
    public void createUseForPlaceRelationshipAppendingLeftWithMetadataTest() throws Exception {
        context.turnOffAuthorisationSystem();

        // Add a dc.contributor.author MDV
        itemService.addMetadata(context, publication1, dcSchema, contributorElement, authorQualifier, null, "MDV 1");

        // Add two Authors to the same Publication, appending to the end
        Relationship r1 = relationshipService.create(context, publication1, author1, isAuthorOfPublication, -1, -1);
        Relationship r2 = relationshipService.create(context, publication1, author2, isAuthorOfPublication, -1, -1);

        // Add another dc.contributor.author MDV
        itemService.addMetadata(context, publication1, dcSchema, contributorElement, authorQualifier, null, "MDV 2");

        // Add another Author to the same Publication, appending to the end
        Relationship r3 = relationshipService.create(context, publication1, author3, isAuthorOfPublication, -1, -1);

        context.restoreAuthSystemState();

        // Check relationship order
        assertLeftPlace(r1, 1);
        assertLeftPlace(r2, 2);
        assertLeftPlace(r3, 4);
        assertRelationMetadataOrder(publication1, isAuthorOfPublication, List.of(r1, r2, r3));
        assertMetadataOrder(publication1, "dc.contributor.author", List.of(
            "MDV 1",
            "Author, First",
            "Author, Second",
            "MDV 2",
            "Author, Third"
        ));
    }

    @Test
    public void createUseForPlaceRelationshipWithLeftPlaceAtTheStartWithMetadataTest() throws Exception {
        context.turnOffAuthorisationSystem();

        // Add a dc.contributor.author MDV
        itemService.addMetadata(context, publication1, dcSchema, contributorElement, authorQualifier, null, "MDV 1");

        // Add two Authors to the same Publication, appending to the end
        Relationship r1 = relationshipService.create(context, publication1, author1, isAuthorOfPublication, -1, -1);
        Relationship r2 = relationshipService.create(context, publication1, author2, isAuthorOfPublication, -1, -1);

        // Add another dc.contributor.author MDV
        itemService.addMetadata(context, publication1, dcSchema, contributorElement, authorQualifier, null, "MDV 2");

        // Add another Author @ leftPlace 0. All MDVs & relationships after it should get pushed by one place
        Relationship r3 = relationshipService.create(context, publication1, author3, isAuthorOfPublication, 0, -1);

        context.restoreAuthSystemState();

        // Check relationship order
        assertLeftPlace(r3, 0);
        assertLeftPlace(r1, 2);
        assertLeftPlace(r2, 3);
        assertRelationMetadataOrder(publication1, isAuthorOfPublication, List.of(r3, r1, r2));
        assertMetadataOrder(publication1, "dc.contributor.author", List.of(
            "Author, Third",
            "MDV 1",
            "Author, First",
            "Author, Second",
            "MDV 2"
        ));
    }

    @Test
    public void createUseForPlaceRelationshipWithLeftPlaceInTheMiddleWithMetadataTest() throws Exception {
        context.turnOffAuthorisationSystem();

        // Add a dc.contributor.author MDV
        itemService.addMetadata(context, publication1, dcSchema, contributorElement, authorQualifier, null, "MDV 1");

        // Add two Authors to the same Publication, appending to the end
        Relationship r1 = relationshipService.create(context, publication1, author1, isAuthorOfPublication, -1, -1);
        Relationship r2 = relationshipService.create(context, publication1, author2, isAuthorOfPublication, -1, -1);

        // Add another dc.contributor.author MDV
        itemService.addMetadata(context, publication1, dcSchema, contributorElement, authorQualifier, null, "MDV 2");

        // Add another Author @ leftPlace 2. All MDVs & relationships after it should get pushed by one place
        Relationship r3 = relationshipService.create(context, publication1, author3, isAuthorOfPublication, 2, -1);

        context.restoreAuthSystemState();

        // Check relationship order
        assertLeftPlace(r1, 1);
        assertLeftPlace(r3, 2);
        assertLeftPlace(r2, 3);
        assertRelationMetadataOrder(publication1, isAuthorOfPublication, List.of(r1, r3, r2));
        assertMetadataOrder(publication1, "dc.contributor.author", List.of(
            "MDV 1",
            "Author, First",
            "Author, Third",
            "Author, Second",
            "MDV 2"
        ));
    }

    @Test
    public void createUseForPlaceRelationshipWithLeftPlaceInTheMiddleWithMetadataTest_ignoreOtherRels(
    ) throws Exception {
        context.turnOffAuthorisationSystem();

        // Add a dc.contributor.author MDV
        itemService.addMetadata(context, publication1, dcSchema, contributorElement, authorQualifier, null, "MDV 1");

        // Add two Authors to the same Publication, appending to the end
        Relationship r1 = relationshipService.create(context, publication1, author1, isAuthorOfPublication, -1, -1);
        Relationship r2 = relationshipService.create(context, publication1, author2, isAuthorOfPublication, -1, -1);

        // NOTE: unrelated relationship => should not be affected
        Relationship ur1 = relationshipService.create(context, publication1, project1, isProjectOfPublication, -1, -1);

        // Add another dc.contributor.author MDV
        itemService.addMetadata(context, publication1, dcSchema, contributorElement, authorQualifier, null, "MDV 2");

        // NOTE: unrelated relationship => should not be affected
        Relationship ur2 = relationshipService.create(context, author2, project2, isProjectOfPerson, -1, -1);

        // Add another Author @ leftPlace 2. All MDVs & relationships after it should get pushed by one place
        Relationship r3 = relationshipService.create(context, publication1, author3, isAuthorOfPublication, 2, -1);

        context.restoreAuthSystemState();

        // Check relationship order
        assertLeftPlace(r1, 1);
        assertLeftPlace(r3, 2);
        assertLeftPlace(r2, 3);
        assertRelationMetadataOrder(publication1, isAuthorOfPublication, List.of(r1, r3, r2));
        assertMetadataOrder(publication1, "dc.contributor.author", List.of(
            "MDV 1",
            "Author, First",
            "Author, Third",
            "Author, Second",
            "MDV 2"
        ));

        // check unaffected relationships
        assertLeftPlace(ur1, 0);
        assertRightPlace(ur1, 0);
        assertLeftPlace(ur2, 0);
        assertRightPlace(ur2, 0);
    }

    @Test
    public void createUseForPlaceRelationshipWithLeftPlaceAtTheEndWithMetadataTest() throws Exception {
        context.turnOffAuthorisationSystem();

        // Add a dc.contributor.author MDV
        itemService.addMetadata(context, publication1, dcSchema, contributorElement, authorQualifier, null, "MDV 1");

        // Add two Authors to the same Publication, appending to the end
        Relationship r1 = relationshipService.create(context, publication1, author1, isAuthorOfPublication, -1, -1);
        Relationship r2 = relationshipService.create(context, publication1, author2, isAuthorOfPublication, -1, -1);

        // Add another dc.contributor.author MDV
        itemService.addMetadata(context, publication1, dcSchema, contributorElement, authorQualifier, null, "MDV 2");

        // Add another Author @ leftPlace 4. This should have the same effect as just appending it
        Relationship r3 = relationshipService.create(context, publication1, author3, isAuthorOfPublication, 4, -1);

        context.restoreAuthSystemState();

        // Check relationship order
        assertLeftPlace(r1, 1);
        assertLeftPlace(r2, 2);
        assertLeftPlace(r3, 4);
        assertRelationMetadataOrder(publication1, isAuthorOfPublication, List.of(r1, r2, r3));
        assertMetadataOrder(publication1, "dc.contributor.author", List.of(
            "MDV 1",
            "Author, First",
            "Author, Second",
            "MDV 2",
            "Author, Third"
        ));
    }

    @Test
    public void createUseForPlaceRelationshipAppendingRightNoMetadataTest() throws Exception {
        context.turnOffAuthorisationSystem();

        // Add three Publications to the same Author, appending to the end
        Relationship r1 = relationshipService.create(context, publication1, author1, isAuthorOfPublication, -1, -1);
        Relationship r2 = relationshipService.create(context, publication2, author1, isAuthorOfPublication, -1, -1);
        Relationship r3 = relationshipService.create(context, publication3, author1, isAuthorOfPublication, -1, -1);

        context.restoreAuthSystemState();

        // Check relationship order
        assertRightPlace(r1, 0);
        assertRightPlace(r2, 1);
        assertRightPlace(r3, 2);
        assertRelationMetadataOrder(author1, isAuthorOfPublication, List.of(r1, r2, r3));
    }

    @Test
    public void createUseForPlaceRelationshipWithRightPlaceAtTheStartNoMetadataTest() throws Exception {
        context.turnOffAuthorisationSystem();

        // Add two Publications to the same Author, appending to the end
        Relationship r1 = relationshipService.create(context, publication1, author1, isAuthorOfPublication, -1, -1);
        Relationship r2 = relationshipService.create(context, publication2, author1, isAuthorOfPublication, -1, -1);

        // Add another Publication @ rightPlace 0. The existing relationships should get pushed one place forward
        Relationship r3 = relationshipService.create(context, publication3, author1, isAuthorOfPublication, -1, 0);

        context.restoreAuthSystemState();

        // Check relationship order
        assertRightPlace(r3, 0);
        assertRightPlace(r1, 1);
        assertRightPlace(r2, 2);
        assertRelationMetadataOrder(author1, isAuthorOfPublication, List.of(r3, r1, r2));
    }

    @Test
    public void createUseForPlaceRelationshipWithRightPlaceInTheMiddleNoMetadataTest() throws Exception {
        context.turnOffAuthorisationSystem();

        // Add two Publications to the same Author, appending to the end
        Relationship r1 = relationshipService.create(context, publication1, author1, isAuthorOfPublication, -1, -1);
        Relationship r2 = relationshipService.create(context, publication2, author1, isAuthorOfPublication, -1, -1);

        // Add another Publication @ rightPlace 1. The second relationship should get pushed by one place
        Relationship r3 = relationshipService.create(context, publication3, author1, isAuthorOfPublication, -1, 1);

        context.restoreAuthSystemState();

        // Check relationship order
        assertRightPlace(r1, 0);
        assertRightPlace(r3, 1);
        assertRightPlace(r2, 2);
        assertRelationMetadataOrder(author1, isAuthorOfPublication, List.of(r1, r3, r2));
    }

    @Test
    public void createUseForPlaceRelationshipWithRightPlaceInTheMiddleNoMetadataTest_ignoreOtherRels(
    ) throws Exception {
        context.turnOffAuthorisationSystem();

        // Add two Publications to the same Author, appending to the end
        Relationship r1 = relationshipService.create(context, publication1, author1, isAuthorOfPublication, -1, -1);
        Relationship r2 = relationshipService.create(context, publication2, author1, isAuthorOfPublication, -1, -1);

        // NOTE: unrelated relationship => should not be affected
        Relationship ur1 = relationshipService.create(context, author1, project1, isProjectOfPerson, -1, -1);

        // NOTE: unrelated relationship => should not be affected
        Relationship ur2 = relationshipService.create(context, author1, project2, isProjectOfPerson, -1, -1);

        // Add another Publication @ rightPlace 1. The second relationship should get pushed by one place
        Relationship r3 = relationshipService.create(context, publication3, author1, isAuthorOfPublication, -1, 1);

        context.restoreAuthSystemState();

        // Check relationship order
        assertRightPlace(r1, 0);
        assertRightPlace(r3, 1);
        assertRightPlace(r2, 2);
        assertRelationMetadataOrder(author1, isAuthorOfPublication, List.of(r1, r3, r2));

        // check unaffected relationships
        assertLeftPlace(ur1, 0);
        assertRightPlace(ur1, 0);
        assertLeftPlace(ur2, 1);
        assertRightPlace(ur2, 0);
    }

    @Test
    public void createUseForPlaceRelationshipWithRightPlaceAtTheEndNoMetadataTest() throws Exception {
        context.turnOffAuthorisationSystem();

        // Add three Publications to the same Author, appending to the end
        Relationship r1 = relationshipService.create(context, publication1, author1, isAuthorOfPublication, -1, -1);
        Relationship r2 = relationshipService.create(context, publication2, author1, isAuthorOfPublication, -1, -1);

        // Add another Publication @ rightPlace 2. This should have the same effect as just appending it
        Relationship r3 = relationshipService.create(context, publication3, author1, isAuthorOfPublication, -1, 2);

        context.restoreAuthSystemState();

        // Check relationship order
        assertRightPlace(r1, 0);
        assertRightPlace(r2, 1);
        assertRightPlace(r3, 2);
        assertRelationMetadataOrder(author1, isAuthorOfPublication, List.of(r1, r2, r3));
    }

    @Test
    public void createNonUseForPlaceRelationshipAppendingLeftTest() throws Exception {
        context.turnOffAuthorisationSystem();

        // Add three Projects to the same Author, appending to the end
        Relationship r1 = relationshipService.create(context, author1, project1, isProjectOfPerson, -1, -1);
        Relationship r2 = relationshipService.create(context, author1, project2, isProjectOfPerson, -1, -1);
        Relationship r3 = relationshipService.create(context, author1, project3, isProjectOfPerson, -1, -1);

        context.restoreAuthSystemState();

        // Check relationship order
        assertLeftPlace(r1, 0);
        assertLeftPlace(r2, 1);
        assertLeftPlace(r3, 2);
        assertRelationMetadataOrder(author1, isProjectOfPerson, List.of(r1, r2, r3));
    }

    @Test
    public void createNonUseForPlaceRelationshipWithLeftPlaceAtTheStartTest() throws Exception {
        context.turnOffAuthorisationSystem();

        // Add two Projects to the same Author, appending to the end
        Relationship r1 = relationshipService.create(context, author1, project1, isProjectOfPerson, -1, -1);
        Relationship r2 = relationshipService.create(context, author1, project2, isProjectOfPerson, -1, -1);

        // Add another Project @ leftPlace 0. The existing relationships should get pushed one place forward
        Relationship r3 = relationshipService.create(context, author1, project3, isProjectOfPerson, 0, -1);

        context.restoreAuthSystemState();

        // Check relationship order
        assertLeftPlace(r3, 0);
        assertLeftPlace(r1, 1);
        assertLeftPlace(r2, 2);
        assertRelationMetadataOrder(author1, isProjectOfPerson, List.of(r3, r1, r2));
    }

    @Test
    public void createNonUseForPlaceRelationshipWithLeftPlaceInTheMiddleTest() throws Exception {
        context.turnOffAuthorisationSystem();

        // Add two Projects to the same Author, appending to the end
        Relationship r1 = relationshipService.create(context, author1, project1, isProjectOfPerson, -1, -1);
        Relationship r2 = relationshipService.create(context, author1, project2, isProjectOfPerson, -1, -1);

        // Add another Project @ leftPlace 1. The second relationship should get pushed by one place
        Relationship r3 = relationshipService.create(context, author1, project3, isProjectOfPerson, 1, -1);

        context.restoreAuthSystemState();

        // Check relationship order
        assertLeftPlace(r1, 0);
        assertLeftPlace(r3, 1);
        assertLeftPlace(r2, 2);
        assertRelationMetadataOrder(author1, isProjectOfPerson, List.of(r1, r3, r2));
    }

    @Test
    public void createNonUseForPlaceRelationshipWithLeftPlaceInTheMiddleTest_ignoreOtherRels() throws Exception {
        context.turnOffAuthorisationSystem();

        // Add two Projects to the same Author, appending to the end
        Relationship r1 = relationshipService.create(context, author1, project1, isProjectOfPerson, -1, -1);
        Relationship r2 = relationshipService.create(context, author1, project2, isProjectOfPerson, -1, -1);

        // NOTE: unrelated relationship => should not be affected
        Relationship ur1 = relationshipService.create(context, publication1, author1, isAuthorOfPublication, -1, -1);

        // Add another Project @ leftPlace 1. The second relationship should get pushed by one place
        Relationship r3 = relationshipService.create(context, author1, project3, isProjectOfPerson, 1, -1);

        context.restoreAuthSystemState();

        // Check relationship order
        assertLeftPlace(r1, 0);
        assertLeftPlace(r3, 1);
        assertLeftPlace(r2, 2);
        assertRelationMetadataOrder(author1, isProjectOfPerson, List.of(r1, r3, r2));

        // check unaffected relationships
        assertLeftPlace(ur1, 0);
        assertRightPlace(ur1, 0);
    }

    @Test
    public void createNonUseForPlaceRelationshipWithLeftPlaceAtTheEndTest() throws Exception {
        context.turnOffAuthorisationSystem();

        // Add two Projects to the same Author, appending to the end
        Relationship r1 = relationshipService.create(context, author1, project1, isProjectOfPerson, -1, -1);
        Relationship r2 = relationshipService.create(context, author1, project2, isProjectOfPerson, -1, -1);

        // Add another Project @ leftPlace 2. This should have the same effect as just appending it
        Relationship r3 = relationshipService.create(context, author1, project3, isProjectOfPerson, 2, -1);

        context.restoreAuthSystemState();

        // Check relationship order
        assertLeftPlace(r1, 0);
        assertLeftPlace(r2, 1);
        assertLeftPlace(r3, 2);
        assertRelationMetadataOrder(author1, isProjectOfPerson, List.of(r1, r2, r3));
    }

    @Test
    public void createNonUseForPlaceRelationshipAppendingRightTest() throws Exception {
        context.turnOffAuthorisationSystem();

        // Add three Authors to the same Project, appending to the end
        Relationship r1 = relationshipService.create(context, author1, project1, isProjectOfPerson, -1, -1);
        Relationship r2 = relationshipService.create(context, author2, project1, isProjectOfPerson, -1, -1);
        Relationship r3 = relationshipService.create(context, author3, project1, isProjectOfPerson, -1, -1);

        context.restoreAuthSystemState();

        // Check relationship order
        assertRightPlace(r1, 0);
        assertRightPlace(r2, 1);
        assertRightPlace(r3, 2);
        assertRelationMetadataOrder(project1, isProjectOfPerson, List.of(r1, r2, r3));
    }

    @Test
    public void createNonUseForPlaceRelationshipWithRightPlaceAtTheStartTest() throws Exception {
        context.turnOffAuthorisationSystem();

        // Add two Authors to the same Project, appending to the end
        Relationship r1 = relationshipService.create(context, author1, project1, isProjectOfPerson, -1, -1);
        Relationship r2 = relationshipService.create(context, author2, project1, isProjectOfPerson, -1, -1);

        // Add another Author @ rightPlace 0. The existing relationships should get pushed one place forward
        Relationship r3 = relationshipService.create(context, author3, project1, isProjectOfPerson, -1, 0);

        context.restoreAuthSystemState();

        // Check relationship order
        assertRightPlace(r3, 0);
        assertRightPlace(r1, 1);
        assertRightPlace(r2, 2);
        assertRelationMetadataOrder(project1, isProjectOfPerson, List.of(r3, r1, r2));
    }

    @Test
    public void createNonUseForPlaceRelationshipWithRightPlaceInTheMiddleTest() throws Exception {
        context.turnOffAuthorisationSystem();

        // Add two Authors to the same Project, appending to the end
        Relationship r1 = relationshipService.create(context, author1, project1, isProjectOfPerson, -1, -1);
        Relationship r2 = relationshipService.create(context, author2, project1, isProjectOfPerson, -1, -1);

        // Add another Author @ rightPlace 1. The second relationship should get pushed by one place
        Relationship r3 = relationshipService.create(context, author3, project1, isProjectOfPerson, -1, 1);

        context.restoreAuthSystemState();

        // Check relationship order
        assertRightPlace(r1, 0);
        assertRightPlace(r3, 1);
        assertRightPlace(r2, 2);
        assertRelationMetadataOrder(project1, isProjectOfPerson, List.of(r1, r3, r2));
    }

    @Test
    public void createNonUseForPlaceRelationshipWithRightPlaceInTheMiddleTest_ignoreOtherRels() throws Exception {
        context.turnOffAuthorisationSystem();

        // Add two Authors to the same Project, appending to the end
        Relationship r1 = relationshipService.create(context, author1, project1, isProjectOfPerson, -1, -1);
        Relationship r2 = relationshipService.create(context, author2, project1, isProjectOfPerson, -1, -1);

        // Add another Author @ rightPlace 1. The second relationship should get pushed by one place
        Relationship r3 = relationshipService.create(context, author3, project1, isProjectOfPerson, -1, 1);

        // NOTE: unrelated relationship => should not be affected
        Relationship ur1 = relationshipService.create(context, publication1, project1, isProjectOfPublication, -1, -1);

        context.restoreAuthSystemState();

        // Check relationship order
        assertRightPlace(r1, 0);
        assertRightPlace(r3, 1);
        assertRightPlace(r2, 2);
        assertRelationMetadataOrder(project1, isProjectOfPerson, List.of(r1, r3, r2));

        // check unaffected relationships
        assertLeftPlace(ur1, 0);
        assertRightPlace(ur1, 0);
    }

    @Test
    public void createNonUseForPlaceRelationshipWithRightPlaceAtTheEndTest() throws Exception {
        context.turnOffAuthorisationSystem();

        // Add two Authors to the same Project, appending to the end
        Relationship r1 = relationshipService.create(context, author1, project1, isProjectOfPerson, -1, -1);
        Relationship r2 = relationshipService.create(context, author2, project1, isProjectOfPerson, -1, -1);

        // Add another Author @ rightPlace 2. This should have the same effect as just appending it
        Relationship r3 = relationshipService.create(context, author3, project1, isProjectOfPerson, -1, 2);

        context.restoreAuthSystemState();

        // Check relationship order
        assertRightPlace(r1, 0);
        assertRightPlace(r2, 1);
        assertRightPlace(r3, 2);
        assertRelationMetadataOrder(project1, isProjectOfPerson, List.of(r1, r2, r3));
    }

    /* RelationshipService#move */

    @Test
    public void moveUseForPlaceRelationshipToCurrentLeftPlaceNoMetadataTest() throws Exception {
        context.turnOffAuthorisationSystem();

        // Add three Authors to the same Publication, appending to the end
        Relationship r1 = relationshipService.create(context, publication1, author1, isAuthorOfPublication, -1, -1);
        Relationship r2 = relationshipService.create(context, publication1, author2, isAuthorOfPublication, -1, -1);
        Relationship r3 = relationshipService.create(context, publication1, author3, isAuthorOfPublication, -1, -1);

        relationshipService.move(context, r1, 0, null);
        relationshipService.move(context, r2, 1, null);
        relationshipService.move(context, r3, 2, null);

        context.restoreAuthSystemState();

        // Check relationship order
        assertLeftPlace(r1, 0);
        assertLeftPlace(r2, 1);
        assertLeftPlace(r3, 2);
        assertRelationMetadataOrder(publication1, isAuthorOfPublication, List.of(r1, r2, r3));
    }

    @Test
    public void moveUseForPlaceRelationshipToLeftPlaceAtTheStartNoMetadataTest() throws Exception {
        context.turnOffAuthorisationSystem();

        // Add three Authors to the same Publication, appending to the end
        Relationship r1 = relationshipService.create(context, publication1, author1, isAuthorOfPublication, -1, -1);
        Relationship r2 = relationshipService.create(context, publication1, author2, isAuthorOfPublication, -1, -1);
        Relationship r3 = relationshipService.create(context, publication1, author3, isAuthorOfPublication, -1, -1);

        relationshipService.move(context, r3, 0, null);

        context.restoreAuthSystemState();

        // Check relationship order
        assertLeftPlace(r3, 0);
        assertLeftPlace(r1, 1);
        assertLeftPlace(r2, 2);
        assertRelationMetadataOrder(publication1, isAuthorOfPublication, List.of(r3, r1, r2));
    }

    @Test
    public void moveUseForPlaceRelationshipUpToLeftPlaceInTheMiddleNoMetadataTest() throws Exception {
        context.turnOffAuthorisationSystem();

        // Add three Authors to the same Publication, appending to the end
        Relationship r1 = relationshipService.create(context, publication1, author1, isAuthorOfPublication, -1, -1);
        Relationship r2 = relationshipService.create(context, publication1, author2, isAuthorOfPublication, -1, -1);
        Relationship r3 = relationshipService.create(context, publication1, author3, isAuthorOfPublication, -1, -1);

        // Move the first Author to leftPlace=1
        relationshipService.move(context, r1, 1, null);

        context.restoreAuthSystemState();

        // Check relationship order
        assertLeftPlace(r2, 0);
        assertLeftPlace(r1, 1);
        assertLeftPlace(r3, 2);
        assertRelationMetadataOrder(publication1, isAuthorOfPublication, List.of(r2, r1, r3));
    }

    @Test
    public void moveUseForPlaceRelationshipDownToLeftPlaceInTheMiddleNoMetadataTest() throws Exception {
        context.turnOffAuthorisationSystem();

        // Add three Authors to the same Publication, appending to the end
        Relationship r1 = relationshipService.create(context, publication1, author1, isAuthorOfPublication, -1, -1);
        Relationship r2 = relationshipService.create(context, publication1, author2, isAuthorOfPublication, -1, -1);
        Relationship r3 = relationshipService.create(context, publication1, author3, isAuthorOfPublication, -1, -1);

        // Move the last Author to leftPlace=1
        relationshipService.move(context, r3, 1, null);

        context.restoreAuthSystemState();

        // Check relationship order
        assertLeftPlace(r1, 0);
        assertLeftPlace(r3, 1);
        assertLeftPlace(r2, 2);
        assertRelationMetadataOrder(publication1, isAuthorOfPublication, List.of(r1, r3, r2));
    }

    @Test
    public void moveUseForPlaceRelationshipToLeftPlaceAtTheEndNoMetadataTest() throws Exception {
        context.turnOffAuthorisationSystem();

        // Add three Authors to the same Publication, appending to the end
        Relationship r1 = relationshipService.create(context, publication1, author1, isAuthorOfPublication, -1, -1);
        Relationship r2 = relationshipService.create(context, publication1, author2, isAuthorOfPublication, -1, -1);
        Relationship r3 = relationshipService.create(context, publication1, author3, isAuthorOfPublication, -1, -1);

        // Move the first Author to the back
        relationshipService.move(context, r1, -1, null);

        context.restoreAuthSystemState();

        // Check relationship order
        assertLeftPlace(r2, 0);
        assertLeftPlace(r3, 1);
        assertLeftPlace(r1, 2);
        assertRelationMetadataOrder(publication1, isAuthorOfPublication, List.of(r2, r3, r1));
    }

    @Test
    public void moveUseForPlaceRelationshipToLeftPlaceAtTheEndOverlapNoMetadataTest() throws Exception {
        context.turnOffAuthorisationSystem();

        // Add three Authors to the same Publication, appending to the end
        Relationship r1 = relationshipService.create(context, publication1, author1, isAuthorOfPublication, -1, -1);
        Relationship r2 = relationshipService.create(context, publication1, author2, isAuthorOfPublication, -1, -1);
        Relationship r3 = relationshipService.create(context, publication1, author3, isAuthorOfPublication, -1, -1);

        // Move the first Author to the back
        relationshipService.move(context, r1, 2, null);

        context.restoreAuthSystemState();

        // Check relationship order
        assertLeftPlace(r2, 0);
        assertLeftPlace(r3, 1);
        assertLeftPlace(r1, 2);
        assertRelationMetadataOrder(publication1, isAuthorOfPublication, List.of(r2, r3, r1));
    }

    @Test
    public void moveUseForPlaceRelationshipToCurrentLeftPlaceWithMetadataTest() throws Exception {
        context.turnOffAuthorisationSystem();

        // Initialize MDVs and Relationships
        itemService.addMetadata(context, publication1, dcSchema, contributorElement, authorQualifier, null, "MDV 1");
        Relationship r1 = relationshipService.create(context, publication1, author1, isAuthorOfPublication, -1, -1);
        Relationship r2 = relationshipService.create(context, publication1, author2, isAuthorOfPublication, -1, -1);
        itemService.addMetadata(context, publication1, dcSchema, contributorElement, authorQualifier, null, "MDV 2");
        Relationship r3 = relationshipService.create(context, publication1, author3, isAuthorOfPublication, -1, -1);

        relationshipService.move(context, r1, 1, null);
        relationshipService.move(context, r2, 2, null);
        relationshipService.move(context, r3, 4, null);

        context.restoreAuthSystemState();

        // Check relationship order
        assertLeftPlace(r1, 1);
        assertLeftPlace(r2, 2);
        assertLeftPlace(r3, 4);
        assertRelationMetadataOrder(publication1, isAuthorOfPublication, List.of(r1, r2, r3));
        assertMetadataOrder(publication1, "dc.contributor.author", List.of(
            "MDV 1",
            "Author, First",
            "Author, Second",
            "MDV 2",
            "Author, Third"
        ));
    }

    @Test
    public void moveUseForPlaceRelationshipToLeftPlaceAtTheStartWithMetadataTest() throws Exception {
        context.turnOffAuthorisationSystem();

        // Initialize MDVs and Relationships
        itemService.addMetadata(context, publication1, dcSchema, contributorElement, authorQualifier, null, "MDV 1");
        Relationship r1 = relationshipService.create(context, publication1, author1, isAuthorOfPublication, -1, -1);
        Relationship r2 = relationshipService.create(context, publication1, author2, isAuthorOfPublication, -1, -1);
        itemService.addMetadata(context, publication1, dcSchema, contributorElement, authorQualifier, null, "MDV 2");
        Relationship r3 = relationshipService.create(context, publication1, author3, isAuthorOfPublication, -1, -1);

        relationshipService.move(context, r3, 0, null);

        context.restoreAuthSystemState();

        // Check relationship order
        assertLeftPlace(r3, 0);
        assertLeftPlace(r1, 2);
        assertLeftPlace(r2, 3);
        assertRelationMetadataOrder(publication1, isAuthorOfPublication, List.of(r3, r1, r2));
        assertMetadataOrder(publication1, "dc.contributor.author", List.of(
            "Author, Third",
            "MDV 1",
            "Author, First",
            "Author, Second",
            "MDV 2"
        ));
    }

    @Test
    public void moveUseForPlaceRelationshipUpToLeftPlaceInTheMiddleWithTest() throws Exception {
        context.turnOffAuthorisationSystem();

        // Initialize MDVs and Relationships
        itemService.addMetadata(context, publication1, dcSchema, contributorElement, authorQualifier, null, "MDV 1");
        Relationship r1 = relationshipService.create(context, publication1, author1, isAuthorOfPublication, -1, -1);
        Relationship r2 = relationshipService.create(context, publication1, author2, isAuthorOfPublication, -1, -1);
        itemService.addMetadata(context, publication1, dcSchema, contributorElement, authorQualifier, null, "MDV 2");
        Relationship r3 = relationshipService.create(context, publication1, author3, isAuthorOfPublication, -1, -1);

        // Move the first Author to leftPlace=3
        relationshipService.move(context, r1, 3, null);

        context.restoreAuthSystemState();

        // Check relationship order
        assertLeftPlace(r2, 1);
        assertLeftPlace(r1, 3);
        assertLeftPlace(r3, 4);
        assertRelationMetadataOrder(publication1, isAuthorOfPublication, List.of(r2, r1, r3));
    }

    @Test
    public void moveUseForPlaceRelationshipUpToLeftPlaceInTheMiddleWithTest_ignoreOtherRels() throws Exception {
        context.turnOffAuthorisationSystem();

        // NOTE: unrelated relationship => should not be affected
        Relationship ur1 = relationshipService.create(context, publication1, project1, isProjectOfPublication, -1, -1);

        // Initialize MDVs and Relationships
        itemService.addMetadata(context, publication1, dcSchema, contributorElement, authorQualifier, null, "MDV 1");
        Relationship r1 = relationshipService.create(context, publication1, author1, isAuthorOfPublication, -1, -1);
        Relationship r2 = relationshipService.create(context, publication1, author2, isAuthorOfPublication, -1, -1);

        // NOTE: unrelated relationship => should not be affected
        Relationship ur2 = relationshipService.create(context, publication1, project2, isProjectOfPublication, -1, -1);

        itemService.addMetadata(context, publication1, dcSchema, contributorElement, authorQualifier, null, "MDV 2");
        Relationship r3 = relationshipService.create(context, publication1, author3, isAuthorOfPublication, -1, -1);

        // Move the first Author to leftPlace=3
        relationshipService.move(context, r1, 3, null);

        context.restoreAuthSystemState();

        // Check relationship order
        assertLeftPlace(r2, 1);
        assertLeftPlace(r1, 3);
        assertLeftPlace(r3, 4);
        assertRelationMetadataOrder(publication1, isAuthorOfPublication, List.of(r2, r1, r3));

        // check unaffected relationships
        assertLeftPlace(ur1, 0);
        assertRightPlace(ur1, 0);
        assertLeftPlace(ur2, 1);
        assertRightPlace(ur2, 0);
    }

    @Test
    public void moveUseForPlaceRelationshipDownToLeftPlaceInTheMiddleWithMetadataTest() throws Exception {
        context.turnOffAuthorisationSystem();

        // Initialize MDVs and Relationships
        itemService.addMetadata(context, publication1, dcSchema, contributorElement, authorQualifier, null, "MDV 1");
        Relationship r1 = relationshipService.create(context, publication1, author1, isAuthorOfPublication, -1, -1);
        Relationship r2 = relationshipService.create(context, publication1, author2, isAuthorOfPublication, -1, -1);
        itemService.addMetadata(context, publication1, dcSchema, contributorElement, authorQualifier, null, "MDV 2");
        Relationship r3 = relationshipService.create(context, publication1, author3, isAuthorOfPublication, -1, -1);

        // Move the last Author to leftPlace=2
        relationshipService.move(context, r3, 2, null);

        context.restoreAuthSystemState();

        // Check relationship order
        assertLeftPlace(r1, 1);
        assertLeftPlace(r3, 2);
        assertLeftPlace(r2, 3);
        assertRelationMetadataOrder(publication1, isAuthorOfPublication, List.of(r1, r3, r2));
    }

    @Test
    public void moveUseForPlaceRelationshipDownToLeftPlaceInTheMiddleWithMetadataTest_ignoreOtherRels(
    ) throws Exception {
        context.turnOffAuthorisationSystem();

        // Initialize MDVs and Relationships
        itemService.addMetadata(context, publication1, dcSchema, contributorElement, authorQualifier, null, "MDV 1");
        Relationship r1 = relationshipService.create(context, publication1, author1, isAuthorOfPublication, -1, -1);

        // NOTE: unrelated relationship => should not be affected
        Relationship ur1 = relationshipService.create(context, publication1, project1, isProjectOfPublication, -1, -1);

        Relationship r2 = relationshipService.create(context, publication1, author2, isAuthorOfPublication, -1, -1);
        itemService.addMetadata(context, publication1, dcSchema, contributorElement, authorQualifier, null, "MDV 2");
        Relationship r3 = relationshipService.create(context, publication1, author3, isAuthorOfPublication, -1, -1);

        // NOTE: unrelated relationship => should not be affected
        Relationship ur2 = relationshipService.create(context, author2, project2, isProjectOfPerson, -1, -1);

        // Move the last Author to leftPlace=2
        relationshipService.move(context, r3, 2, null);

        context.restoreAuthSystemState();

        // Check relationship order
        assertLeftPlace(r1, 1);
        assertLeftPlace(r3, 2);
        assertLeftPlace(r2, 3);
        assertRelationMetadataOrder(publication1, isAuthorOfPublication, List.of(r1, r3, r2));

        // check unaffected relationships
        assertLeftPlace(ur1, 0);
        assertRightPlace(ur1, 0);
        assertLeftPlace(ur2, 0);
        assertRightPlace(ur2, 0);
    }

    @Test
    public void moveUseForPlaceRelationshipToLeftPlaceAtTheEndWithMetadataTest() throws Exception {
        context.turnOffAuthorisationSystem();

        // Initialize MDVs and Relationships
        itemService.addMetadata(context, publication1, dcSchema, contributorElement, authorQualifier, null, "MDV 1");
        Relationship r1 = relationshipService.create(context, publication1, author1, isAuthorOfPublication, -1, -1);
        Relationship r2 = relationshipService.create(context, publication1, author2, isAuthorOfPublication, -1, -1);
        itemService.addMetadata(context, publication1, dcSchema, contributorElement, authorQualifier, null, "MDV 2");
        Relationship r3 = relationshipService.create(context, publication1, author3, isAuthorOfPublication, -1, -1);

        // Move the first Author to the back
        relationshipService.move(context, r1, -1, null);

        context.restoreAuthSystemState();

        // Check relationship order
        assertLeftPlace(r2, 1);
        assertLeftPlace(r3, 3);
        assertLeftPlace(r1, 4);
        assertRelationMetadataOrder(publication1, isAuthorOfPublication, List.of(r2, r3, r1));
    }

    @Test
    public void moveUseForPlaceRelationshipToLeftPlaceAtTheEndOverlapWithMetadataTest() throws Exception {
        context.turnOffAuthorisationSystem();

        // Initialize MDVs and Relationships
        itemService.addMetadata(context, publication1, dcSchema, contributorElement, authorQualifier, null, "MDV 1");
        Relationship r1 = relationshipService.create(context, publication1, author1, isAuthorOfPublication, -1, -1);
        Relationship r2 = relationshipService.create(context, publication1, author2, isAuthorOfPublication, -1, -1);
        itemService.addMetadata(context, publication1, dcSchema, contributorElement, authorQualifier, null, "MDV 2");
        Relationship r3 = relationshipService.create(context, publication1, author3, isAuthorOfPublication, -1, -1);

        // Move the first Author to the back
        relationshipService.move(context, r1, 4, null);

        context.restoreAuthSystemState();

        // Check relationship order
        assertLeftPlace(r2, 1);
        assertLeftPlace(r3, 3);
        assertLeftPlace(r1, 4);
        assertRelationMetadataOrder(publication1, isAuthorOfPublication, List.of(r2, r3, r1));
    }

    @Test
    public void moveUseForPlaceRelationshipToCurrentRightPlaceNoMetadataTest() throws Exception {
        context.turnOffAuthorisationSystem();

        // Add three Publications to the same Author, appending to the end
        Relationship r1 = relationshipService.create(context, publication1, author1, isAuthorOfPublication, -1, -1);
        Relationship r2 = relationshipService.create(context, publication2, author1, isAuthorOfPublication, -1, -1);
        Relationship r3 = relationshipService.create(context, publication3, author1, isAuthorOfPublication, -1, -1);


        relationshipService.move(context, r1, null, 0);
        relationshipService.move(context, r2, null, 1);
        relationshipService.move(context, r3, null, 2);

        context.restoreAuthSystemState();

        // Check relationship order
        assertRightPlace(r1, 0);
        assertRightPlace(r2, 1);
        assertRightPlace(r3, 2);
        assertRelationMetadataOrder(author1, isAuthorOfPublication, List.of(r1, r2, r3));
    }

    @Test
    public void moveUseForPlaceRelationshipToRightPlaceAtTheStartNoMetadataTest() throws Exception {
        context.turnOffAuthorisationSystem();

        // Add three Publications to the same Author, appending to the end
        Relationship r1 = relationshipService.create(context, publication1, author1, isAuthorOfPublication, -1, -1);
        Relationship r2 = relationshipService.create(context, publication2, author1, isAuthorOfPublication, -1, -1);
        Relationship r3 = relationshipService.create(context, publication3, author1, isAuthorOfPublication, -1, -1);


        relationshipService.move(context, r3, null, 0);

        context.restoreAuthSystemState();

        // Check relationship order
        assertRightPlace(r3, 0);
        assertRightPlace(r1, 1);
        assertRightPlace(r2, 2);
        assertRelationMetadataOrder(author1, isAuthorOfPublication, List.of(r3, r1, r2));
    }

    @Test
    public void moveUseForPlaceRelationshipUpToRightPlaceInTheMiddleNoMetadataTest() throws Exception {
        context.turnOffAuthorisationSystem();

        // Add three Authors to the same Publication, appending to the end
        Relationship r1 = relationshipService.create(context, publication1, author1, isAuthorOfPublication, -1, -1);
        Relationship r2 = relationshipService.create(context, publication2, author1, isAuthorOfPublication, -1, -1);
        Relationship r3 = relationshipService.create(context, publication3, author1, isAuthorOfPublication, -1, -1);

        // Move the first Author to leftPlace=1
        relationshipService.move(context, r1, null, 1);

        context.restoreAuthSystemState();

        // Check relationship order
        assertRightPlace(r2, 0);
        assertRightPlace(r1, 1);
        assertRightPlace(r3, 2);
        assertRelationMetadataOrder(author1, isAuthorOfPublication, List.of(r2, r1, r3));
    }

    @Test
    public void moveUseForPlaceRelationshipDownToRightPlaceInTheMiddleNoMetadataTest() throws Exception {
        context.turnOffAuthorisationSystem();

        // Add three Authors to the same Publication, appending to the end
        Relationship r1 = relationshipService.create(context, publication1, author1, isAuthorOfPublication, -1, -1);
        Relationship r2 = relationshipService.create(context, publication2, author1, isAuthorOfPublication, -1, -1);
        Relationship r3 = relationshipService.create(context, publication3, author1, isAuthorOfPublication, -1, -1);

        // Move the last Author to leftPlace=1
        relationshipService.move(context, r3, null, 1);

        context.restoreAuthSystemState();

        // Check relationship order
        assertRightPlace(r1, 0);
        assertRightPlace(r3, 1);
        assertRightPlace(r2, 2);
        assertRelationMetadataOrder(author1, isAuthorOfPublication, List.of(r1, r3, r2));
    }

    @Test
    public void moveUseForPlaceRelationshipToRightPlaceAtTheEndNoMetadataTest() throws Exception {
        context.turnOffAuthorisationSystem();

        // Add three Authors to the same Publication, appending to the end
        Relationship r1 = relationshipService.create(context, publication1, author1, isAuthorOfPublication, -1, -1);
        Relationship r2 = relationshipService.create(context, publication2, author1, isAuthorOfPublication, -1, -1);
        Relationship r3 = relationshipService.create(context, publication3, author1, isAuthorOfPublication, -1, -1);

        // Move the first Author to the back
        relationshipService.move(context, r1, null, -1);

        context.restoreAuthSystemState();

        // Check relationship order
        assertRightPlace(r2, 0);
        assertRightPlace(r3, 1);
        assertRightPlace(r1, 2);
        assertRelationMetadataOrder(author1, isAuthorOfPublication, List.of(r2, r3, r1));
    }

    @Test
    public void moveUseForPlaceRelationshipToRightPlaceAtTheEndOverlapNoMetadataTest() throws Exception {
        context.turnOffAuthorisationSystem();

        // Add three Authors to the same Publication, appending to the end
        Relationship r1 = relationshipService.create(context, publication1, author1, isAuthorOfPublication, -1, -1);
        Relationship r2 = relationshipService.create(context, publication2, author1, isAuthorOfPublication, -1, -1);
        Relationship r3 = relationshipService.create(context, publication3, author1, isAuthorOfPublication, -1, -1);

        // Move the first Author to the back
        relationshipService.move(context, r1, null, 2);

        context.restoreAuthSystemState();

        // Check relationship order
        assertRightPlace(r2, 0);
        assertRightPlace(r3, 1);
        assertRightPlace(r1, 2);
        assertRelationMetadataOrder(author1, isAuthorOfPublication, List.of(r2, r3, r1));
    }

    @Test
    public void moveNonUseForPlaceRelationshipToCurrentLeftPlaceNoMetadataTest() throws Exception {
        context.turnOffAuthorisationSystem();

        // Add three Projects to the same Author, appending to the end
        Relationship r1 = relationshipService.create(context, author1, project1, isProjectOfPerson, -1, -1);
        Relationship r2 = relationshipService.create(context, author1, project2, isProjectOfPerson, -1, -1);
        Relationship r3 = relationshipService.create(context, author1, project3, isProjectOfPerson, -1, -1);

        // Move the last Project to the front
        relationshipService.move(context, r1, 0, null);
        relationshipService.move(context, r2, 1, null);
        relationshipService.move(context, r3, 2, null);

        context.restoreAuthSystemState();

        // Check relationship order
        assertLeftPlace(r1, 0);
        assertLeftPlace(r2, 1);
        assertLeftPlace(r3, 2);
        assertRelationMetadataOrder(author1, isProjectOfPerson, List.of(r1, r2, r3));
    }

    @Test
    public void moveNonUseForPlaceRelationshipToLeftPlaceAtTheStartNoMetadataTest() throws Exception {
        context.turnOffAuthorisationSystem();

        // Add three Projects to the same Author, appending to the end
        Relationship r1 = relationshipService.create(context, author1, project1, isProjectOfPerson, -1, -1);
        Relationship r2 = relationshipService.create(context, author1, project2, isProjectOfPerson, -1, -1);
        Relationship r3 = relationshipService.create(context, author1, project3, isProjectOfPerson, -1, -1);

        // Move the last Project to the front
        relationshipService.move(context, r3, 0, null);

        context.restoreAuthSystemState();

        // Check relationship order
        assertLeftPlace(r3, 0);
        assertLeftPlace(r1, 1);
        assertLeftPlace(r2, 2);
        assertRelationMetadataOrder(author1, isProjectOfPerson, List.of(r3, r1, r2));
    }

    @Test
    public void moveNonUseForPlaceRelationshipUpToLeftPlaceInTheMiddleNoMetadataTest() throws Exception {
        context.turnOffAuthorisationSystem();

        // Add three Projects to the same Author, appending to the end
        Relationship r1 = relationshipService.create(context, author1, project1, isProjectOfPerson, -1, -1);
        Relationship r2 = relationshipService.create(context, author1, project2, isProjectOfPerson, -1, -1);
        Relationship r3 = relationshipService.create(context, author1, project3, isProjectOfPerson, -1, -1);

        // Move the first Author to leftPlace=1
        relationshipService.move(context, r1, 1, null);

        context.restoreAuthSystemState();

        // Check relationship order
        assertLeftPlace(r2, 0);
        assertLeftPlace(r1, 1);
        assertLeftPlace(r3, 2);
        assertRelationMetadataOrder(author1, isProjectOfPerson, List.of(r2, r1, r3));
    }

    @Test
    public void moveNonUseForPlaceRelationshipDownToLeftPlaceInTheMiddleNoMetadataTest() throws Exception {
        context.turnOffAuthorisationSystem();

        // Add three Projects to the same Author, appending to the end
        Relationship r1 = relationshipService.create(context, author1, project1, isProjectOfPerson, -1, -1);
        Relationship r2 = relationshipService.create(context, author1, project2, isProjectOfPerson, -1, -1);
        Relationship r3 = relationshipService.create(context, author1, project3, isProjectOfPerson, -1, -1);

        // Move the last Author to leftPlace=1
        relationshipService.move(context, r3, 1, null);

        context.restoreAuthSystemState();

        // Check relationship order
        assertLeftPlace(r1, 0);
        assertLeftPlace(r3, 1);
        assertLeftPlace(r2, 2);
        assertRelationMetadataOrder(author1, isProjectOfPerson, List.of(r1, r3, r2));
    }

    @Test
    public void moveNonUseForPlaceRelationshipToLeftPlaceAtTheEndNoMetadataTest() throws Exception {
        context.turnOffAuthorisationSystem();

        // Add three Projects to the same Author, appending to the end
        Relationship r1 = relationshipService.create(context, author1, project1, isProjectOfPerson, -1, -1);
        Relationship r2 = relationshipService.create(context, author1, project2, isProjectOfPerson, -1, -1);
        Relationship r3 = relationshipService.create(context, author1, project3, isProjectOfPerson, -1, -1);

        // Move the first Author to the back
        relationshipService.move(context, r1, -1, null);

        context.restoreAuthSystemState();

        // Check relationship order
        assertLeftPlace(r2, 0);
        assertLeftPlace(r3, 1);
        assertLeftPlace(r1, 2);
        assertRelationMetadataOrder(author1, isProjectOfPerson, List.of(r2, r3, r1));
    }

    @Test
    public void moveNonUseForPlaceRelationshipToLeftPlaceAtTheEndOverlapNoMetadataTest() throws Exception {
        context.turnOffAuthorisationSystem();

        // Add three Projects to the same Author, appending to the end
        Relationship r1 = relationshipService.create(context, author1, project1, isProjectOfPerson, -1, -1);
        Relationship r2 = relationshipService.create(context, author1, project2, isProjectOfPerson, -1, -1);
        Relationship r3 = relationshipService.create(context, author1, project3, isProjectOfPerson, -1, -1);

        // Move the first Author to the back
        relationshipService.move(context, r1, 2, null);

        context.restoreAuthSystemState();

        // Check relationship order
        assertLeftPlace(r2, 0);
        assertLeftPlace(r3, 1);
        assertLeftPlace(r1, 2);
        assertRelationMetadataOrder(author1, isProjectOfPerson, List.of(r2, r3, r1));
    }

    @Test
    public void moveNonUseForPlaceRelationshipToCurrentRightPlaceNoMetadataTest() throws Exception {
        context.turnOffAuthorisationSystem();

        // Add three Authors to the same Project, appending to the end
        Relationship r1 = relationshipService.create(context, author1, project1, isProjectOfPerson, -1, -1);
        Relationship r2 = relationshipService.create(context, author2, project1, isProjectOfPerson, -1, -1);
        Relationship r3 = relationshipService.create(context, author3, project1, isProjectOfPerson, -1, -1);


        relationshipService.move(context, r1, null, 0);
        relationshipService.move(context, r2, null, 1);
        relationshipService.move(context, r3, null, 2);

        context.restoreAuthSystemState();

        // Check relationship order
        assertRightPlace(r1, 0);
        assertRightPlace(r2, 1);
        assertRightPlace(r3, 2);
        assertRelationMetadataOrder(project1, isProjectOfPerson, List.of(r1, r2, r3));
    }

    @Test
    public void moveNonUseForPlaceRelationshipToRightPlaceAtTheStartNoMetadataTest() throws Exception {
        context.turnOffAuthorisationSystem();

        // Add three Authors to the same Project, appending to the end
        Relationship r1 = relationshipService.create(context, author1, project1, isProjectOfPerson, -1, -1);
        Relationship r2 = relationshipService.create(context, author2, project1, isProjectOfPerson, -1, -1);
        Relationship r3 = relationshipService.create(context, author3, project1, isProjectOfPerson, -1, -1);


        relationshipService.move(context, r3, null, 0);

        context.restoreAuthSystemState();

        // Check relationship order
        assertRightPlace(r3, 0);
        assertRightPlace(r1, 1);
        assertRightPlace(r2, 2);
        assertRelationMetadataOrder(project1, isProjectOfPerson, List.of(r3, r1, r2));
    }

    @Test
    public void moveNonUseForPlaceRelationshipUpToRightPlaceInTheMiddleNoMetadataTest() throws Exception {
        context.turnOffAuthorisationSystem();

        // Add three Authors to the same Project, appending to the end
        Relationship r1 = relationshipService.create(context, author1, project1, isProjectOfPerson, -1, -1);
        Relationship r2 = relationshipService.create(context, author2, project1, isProjectOfPerson, -1, -1);
        Relationship r3 = relationshipService.create(context, author3, project1, isProjectOfPerson, -1, -1);

        // Move the first Author to leftPlace=1
        relationshipService.move(context, r1, null, 1);

        context.restoreAuthSystemState();

        // Check relationship order
        assertRightPlace(r2, 0);
        assertRightPlace(r1, 1);
        assertRightPlace(r3, 2);
        assertRelationMetadataOrder(project1, isProjectOfPerson, List.of(r2, r1, r3));
    }

    @Test
    public void moveNonUseForPlaceRelationshipUpToRightPlaceInTheMiddleNoMetadataTest_ignoreOtherRels(
    ) throws Exception {
        context.turnOffAuthorisationSystem();

        // Add three Authors to the same Project, appending to the end
        Relationship r1 = relationshipService.create(context, author1, project1, isProjectOfPerson, -1, -1);

        // NOTE: unrelated relationship => should not be affected
        Relationship ur1 = relationshipService.create(context, publication1, project1, isProjectOfPublication, -1, -1);

        Relationship r2 = relationshipService.create(context, author2, project1, isProjectOfPerson, -1, -1);

        // NOTE: unrelated relationship => should not be affected
        Relationship ur2 = relationshipService.create(context, publication2, project1, isProjectOfPublication, -1, -1);

        Relationship r3 = relationshipService.create(context, author3, project1, isProjectOfPerson, -1, -1);

        // NOTE: unrelated relationship => should not be affected
        Relationship ur3 = relationshipService.create(context, publication3, project1, isProjectOfPublication, -1, -1);

        // Move the first Author to leftPlace=1
        relationshipService.move(context, r1, null, 1);

        context.restoreAuthSystemState();

        // Check relationship order
        assertRightPlace(r2, 0);
        assertRightPlace(r1, 1);
        assertRightPlace(r3, 2);
        assertRelationMetadataOrder(project1, isProjectOfPerson, List.of(r2, r1, r3));

        // check unaffected relationships
        assertLeftPlace(ur1, 0);
        assertRightPlace(ur1, 0);
        assertLeftPlace(ur2, 0);
        assertRightPlace(ur2, 1);
        assertLeftPlace(ur3, 0);
        assertRightPlace(ur3, 2);
    }

    @Test
    public void moveNonUseForPlaceRelationshipDownToRightPlaceInTheMiddleNoMetadataTest() throws Exception {
        context.turnOffAuthorisationSystem();

        // Add three Authors to the same Project, appending to the end
        Relationship r1 = relationshipService.create(context, author1, project1, isProjectOfPerson, -1, -1);
        Relationship r2 = relationshipService.create(context, author2, project1, isProjectOfPerson, -1, -1);
        Relationship r3 = relationshipService.create(context, author3, project1, isProjectOfPerson, -1, -1);

        // Move the last Author to leftPlace=1
        relationshipService.move(context, r3, null, 1);

        context.restoreAuthSystemState();

        // Check relationship order
        assertRightPlace(r1, 0);
        assertRightPlace(r3, 1);
        assertRightPlace(r2, 2);
        assertRelationMetadataOrder(project1, isProjectOfPerson, List.of(r1, r3, r2));
    }

    @Test
    public void moveNonUseForPlaceRelationshipDownToRightPlaceInTheMiddleNoMetadataTest_ignoreOtherRels(
    ) throws Exception {
        context.turnOffAuthorisationSystem();

        // Add three Authors to the same Project, appending to the end
        Relationship r1 = relationshipService.create(context, author1, project1, isProjectOfPerson, -1, -1);
        Relationship r2 = relationshipService.create(context, author2, project1, isProjectOfPerson, -1, -1);

        // NOTE: unrelated relationship => should not be affected
        Relationship ur1 = relationshipService.create(context, publication1, project1, isProjectOfPublication, -1, -1);

        Relationship r3 = relationshipService.create(context, author3, project1, isProjectOfPerson, -1, -1);

        // Move the last Author to leftPlace=1
        relationshipService.move(context, r3, null, 1);

        context.restoreAuthSystemState();

        // Check relationship order
        assertRightPlace(r1, 0);
        assertRightPlace(r3, 1);
        assertRightPlace(r2, 2);
        assertRelationMetadataOrder(project1, isProjectOfPerson, List.of(r1, r3, r2));

        // check unaffected relationships
        assertLeftPlace(ur1, 0);
        assertRightPlace(ur1, 0);
    }

    @Test
    public void moveNonUseForPlaceRelationshipToRightPlaceAtTheEndNoMetadataTest() throws Exception {
        context.turnOffAuthorisationSystem();

        // Add three Authors to the same Project, appending to the end
        Relationship r1 = relationshipService.create(context, author1, project1, isProjectOfPerson, -1, -1);
        Relationship r2 = relationshipService.create(context, author2, project1, isProjectOfPerson, -1, -1);
        Relationship r3 = relationshipService.create(context, author3, project1, isProjectOfPerson, -1, -1);

        // Move the first Author to the back
        relationshipService.move(context, r1, null, -1);

        context.restoreAuthSystemState();

        // Check relationship order
        assertRightPlace(r2, 0);
        assertRightPlace(r3, 1);
        assertRightPlace(r1, 2);
        assertRelationMetadataOrder(project1, isProjectOfPerson, List.of(r2, r3, r1));
    }

    /* RelationshipService#delete */

    @Test
    public void deleteUseForPlaceRelationshipFromLeftStartNoMetadataTest() throws Exception {
        context.turnOffAuthorisationSystem();

        // Add three Authors to the same Publication, appending to the end
        Relationship r1 = relationshipService.create(context, publication1, author1, isAuthorOfPublication, -1, -1);
        Relationship r2 = relationshipService.create(context, publication1, author2, isAuthorOfPublication, -1, -1);
        Relationship r3 = relationshipService.create(context, publication1, author3, isAuthorOfPublication, -1, -1);

        // Delete the first Author
        relationshipService.delete(context, r1);

        context.restoreAuthSystemState();

        // Check relationship order
        assertLeftPlace(r2, 0);
        assertLeftPlace(r3, 1);
        assertRelationMetadataOrder(publication1, isAuthorOfPublication, List.of(r2, r3));
    }

    @Test
    public void deleteUseForPlaceRelationshipFromLeftMiddleNoMetadataTest() throws Exception {
        context.turnOffAuthorisationSystem();

        // Add three Authors to the same Publication, appending to the end
        Relationship r1 = relationshipService.create(context, publication1, author1, isAuthorOfPublication, -1, -1);
        Relationship r2 = relationshipService.create(context, publication1, author2, isAuthorOfPublication, -1, -1);
        Relationship r3 = relationshipService.create(context, publication1, author3, isAuthorOfPublication, -1, -1);

        // Delete the second Author
        relationshipService.delete(context, r2);

        context.restoreAuthSystemState();

        // Check relationship order
        assertLeftPlace(r1, 0);
        assertLeftPlace(r3, 1);
        assertRelationMetadataOrder(publication1, isAuthorOfPublication, List.of(r1, r3));
    }

    @Test
    public void deleteUseForPlaceRelationshipFromLeftEndNoMetadataTest() throws Exception {
        context.turnOffAuthorisationSystem();

        // Add three Authors to the same Publication, appending to the end
        Relationship r1 = relationshipService.create(context, publication1, author1, isAuthorOfPublication, -1, -1);
        Relationship r2 = relationshipService.create(context, publication1, author2, isAuthorOfPublication, -1, -1);
        Relationship r3 = relationshipService.create(context, publication1, author3, isAuthorOfPublication, -1, -1);

        // Delete the third Author
        relationshipService.delete(context, r3);

        context.restoreAuthSystemState();

        // Check relationship order
        assertLeftPlace(r1, 0);
        assertLeftPlace(r2, 1);
        assertRelationMetadataOrder(publication1, isAuthorOfPublication, List.of(r1, r2));
    }

    @Test
    public void deleteUseForPlaceRelationshipFromLeftStartWithMetadataNoCopyTest() throws Exception {
        context.turnOffAuthorisationSystem();

        // Initialize MDVs and Relationships
        Relationship r1 = relationshipService.create(context, publication1, author1, isAuthorOfPublication, -1, -1);
        itemService.addMetadata(context, publication1, dcSchema, contributorElement, authorQualifier, null, "MDV 1");
        Relationship r2 = relationshipService.create(context, publication1, author2, isAuthorOfPublication, -1, -1);
        itemService.addMetadata(context, publication1, dcSchema, contributorElement, authorQualifier, null, "MDV 2");
        Relationship r3 = relationshipService.create(context, publication1, author3, isAuthorOfPublication, -1, -1);

        relationshipService.delete(context, r1, false, false);

        context.restoreAuthSystemState();

        // Check relationship order
        assertLeftPlace(r2, 1);
        assertLeftPlace(r3, 3);
        assertRelationMetadataOrder(publication1, isAuthorOfPublication, List.of(r2, r3));
        assertMetadataOrder(publication1, "dc.contributor.author", List.of(
            "MDV 1",
            "Author, Second",
            "MDV 2",
            "Author, Third"
        ));
    }

    @Test
    public void deleteUseForPlaceRelationshipFromLeftStartWithMetadataCopyTest() throws Exception {
        context.turnOffAuthorisationSystem();

        // Initialize MDVs and Relationships
        Relationship r1 = relationshipService.create(context, publication1, author1, isAuthorOfPublication, -1, -1);
        itemService.addMetadata(context, publication1, dcSchema, contributorElement, authorQualifier, null, "MDV 1");
        Relationship r2 = relationshipService.create(context, publication1, author2, isAuthorOfPublication, -1, -1);
        itemService.addMetadata(context, publication1, dcSchema, contributorElement, authorQualifier, null, "MDV 2");
        Relationship r3 = relationshipService.create(context, publication1, author3, isAuthorOfPublication, -1, -1);

        relationshipService.delete(context, r1, true, false);

        context.restoreAuthSystemState();

        // Check relationship order
        // NOTE: since R1 has been removed, but copied to left, this place remains at 2 (instead of 1)
        assertLeftPlace(r2, 2);
        // NOTE: since R1 has been removed, but copied to left, this place remains at 4 (instead of 3)
        assertLeftPlace(r3, 4);
        assertRelationMetadataOrder(publication1, isAuthorOfPublication, Arrays.asList(null, r2, r3));
        assertMetadataOrder(publication1, "dc.contributor.author", List.of(
            "Author, First",    // this is not longer a relationship
            "MDV 1",
            "Author, Second",
            "MDV 2",
            "Author, Third"
        ));
    }

    @Test
    public void deleteUseForPlaceRelationshipFromLeftMiddleWithMetadataNoCopyTest() throws Exception {
        context.turnOffAuthorisationSystem();

        // Initialize MDVs and Relationships
        Relationship r1 = relationshipService.create(context, publication1, author1, isAuthorOfPublication, -1, -1);
        itemService.addMetadata(context, publication1, dcSchema, contributorElement, authorQualifier, null, "MDV 1");
        Relationship r2 = relationshipService.create(context, publication1, author2, isAuthorOfPublication, -1, -1);
        itemService.addMetadata(context, publication1, dcSchema, contributorElement, authorQualifier, null, "MDV 2");
        Relationship r3 = relationshipService.create(context, publication1, author3, isAuthorOfPublication, -1, -1);

        relationshipService.delete(context, r2, false, false);

        context.restoreAuthSystemState();

        // Check relationship order
        assertLeftPlace(r1, 0);
        assertLeftPlace(r3, 3);
        assertRelationMetadataOrder(publication1, isAuthorOfPublication, Arrays.asList(r1, r3));
        assertMetadataOrder(publication1, "dc.contributor.author", List.of(
            "Author, First",
            "MDV 1",
            "MDV 2",
            "Author, Third"
        ));
    }

    @Test
    public void deleteUseForPlaceRelationshipFromLeftMiddleWithMetadataNoCopyTest_ignoreOtherRels() throws Exception {
        context.turnOffAuthorisationSystem();

        // Initialize MDVs and Relationships
        Relationship r1 = relationshipService.create(context, publication1, author1, isAuthorOfPublication, -1, -1);
        itemService.addMetadata(context, publication1, dcSchema, contributorElement, authorQualifier, null, "MDV 1");

        // NOTE: unrelated relationship => should not be affected
        Relationship ur1 = relationshipService.create(context, publication1, project1, isProjectOfPublication, -1, -1);

        Relationship r2 = relationshipService.create(context, publication1, author2, isAuthorOfPublication, -1, -1);
        itemService.addMetadata(context, publication1, dcSchema, contributorElement, authorQualifier, null, "MDV 2");
        Relationship r3 = relationshipService.create(context, publication1, author3, isAuthorOfPublication, -1, -1);

        relationshipService.delete(context, r2, false, false);

        context.restoreAuthSystemState();

        // Check relationship order
        assertLeftPlace(r1, 0);
        assertLeftPlace(r3, 3);
        assertRelationMetadataOrder(publication1, isAuthorOfPublication, Arrays.asList(r1, r3));
        assertMetadataOrder(publication1, "dc.contributor.author", List.of(
            "Author, First",
            "MDV 1",
            "MDV 2",
            "Author, Third"
        ));

        // check unaffected relationships
        assertLeftPlace(ur1, 0);
        assertRightPlace(ur1, 0);
    }

    @Test
    public void deleteUseForPlaceRelationshipFromLeftMiddleWithMetadataCopyTest() throws Exception {
        context.turnOffAuthorisationSystem();

        // Initialize MDVs and Relationships
        Relationship r1 = relationshipService.create(context, publication1, author1, isAuthorOfPublication, -1, -1);
        itemService.addMetadata(context, publication1, dcSchema, contributorElement, authorQualifier, null, "MDV 1");
        Relationship r2 = relationshipService.create(context, publication1, author2, isAuthorOfPublication, -1, -1);
        itemService.addMetadata(context, publication1, dcSchema, contributorElement, authorQualifier, null, "MDV 2");
        Relationship r3 = relationshipService.create(context, publication1, author3, isAuthorOfPublication, -1, -1);

        relationshipService.delete(context, r2, true, false);

        context.restoreAuthSystemState();

        // Check relationship order
        assertLeftPlace(r1, 0);
        // NOTE: since R2 has been removed, but copied to left, this place remains at 4 (instead of 3)
        assertLeftPlace(r3, 4);
        assertRelationMetadataOrder(publication1, isAuthorOfPublication, Arrays.asList(r1, null, r3));
        assertMetadataOrder(publication1, "dc.contributor.author", List.of(
            "Author, First",
            "MDV 1",
            "Author, Second",   // this is not longer a relationship
            "MDV 2",
            "Author, Third"
        ));
    }

    @Test
    public void deleteUseForPlaceRelationshipFromLeftMiddleWithMetadataCopyTest_ignoreOtherRels() throws Exception {
        context.turnOffAuthorisationSystem();

        // Initialize MDVs and Relationships
        Relationship r1 = relationshipService.create(context, publication1, author1, isAuthorOfPublication, -1, -1);

        // NOTE: unrelated relationship => should not be affected
        Relationship ur1 = relationshipService.create(context, publication1, project1, isProjectOfPublication, -1, -1);

        itemService.addMetadata(context, publication1, dcSchema, contributorElement, authorQualifier, null, "MDV 1");

        // NOTE: unrelated relationship => should not be affected
        Relationship ur2 = relationshipService.create(context, publication1, project2, isProjectOfPublication, -1, -1);

        Relationship r2 = relationshipService.create(context, publication1, author2, isAuthorOfPublication, -1, -1);

        // NOTE: unrelated relationship => should not be affected
        Relationship ur3 = relationshipService.create(context, author2, project1, isProjectOfPerson, -1, -1);

        itemService.addMetadata(context, publication1, dcSchema, contributorElement, authorQualifier, null, "MDV 2");
        Relationship r3 = relationshipService.create(context, publication1, author3, isAuthorOfPublication, -1, -1);

        relationshipService.delete(context, r2, true, false);

        context.restoreAuthSystemState();

        // Check relationship order
        assertLeftPlace(r1, 0);
        // NOTE: since R2 has been removed, but copied to left, this place remains at 4 (instead of 3)
        assertLeftPlace(r3, 4);
        assertRelationMetadataOrder(publication1, isAuthorOfPublication, Arrays.asList(r1, null, r3));
        assertMetadataOrder(publication1, "dc.contributor.author", List.of(
            "Author, First",
            "MDV 1",
            "Author, Second",   // this is not longer a relationship
            "MDV 2",
            "Author, Third"
        ));

        // check unaffected relationships
        assertLeftPlace(ur1, 0);
        assertRightPlace(ur1, 0);
        assertLeftPlace(ur2, 1);
        assertRightPlace(ur2, 0);
        assertLeftPlace(ur3, 0);
        assertRightPlace(ur3, 0);
    }

    @Test
    public void deleteUseForPlaceRelationshipFromLeftEndWithMetadataNoCopyTest() throws Exception {
        context.turnOffAuthorisationSystem();

        // Initialize MDVs and Relationships
        Relationship r1 = relationshipService.create(context, publication1, author1, isAuthorOfPublication, -1, -1);
        itemService.addMetadata(context, publication1, dcSchema, contributorElement, authorQualifier, null, "MDV 1");
        Relationship r2 = relationshipService.create(context, publication1, author2, isAuthorOfPublication, -1, -1);
        itemService.addMetadata(context, publication1, dcSchema, contributorElement, authorQualifier, null, "MDV 2");
        Relationship r3 = relationshipService.create(context, publication1, author3, isAuthorOfPublication, -1, -1);


        relationshipService.delete(context, r3, false, false);

        context.restoreAuthSystemState();

        // Check relationship order
        assertLeftPlace(r1, 0);
        assertLeftPlace(r2, 2);
        assertRelationMetadataOrder(publication1, isAuthorOfPublication, Arrays.asList(r1, r2));
        assertMetadataOrder(publication1, "dc.contributor.author", List.of(
            "Author, First",
            "MDV 1",
            "Author, Second",
            "MDV 2"
        ));
    }

    @Test
    public void deleteUseForPlaceRelationshipFromLeftEndWithMetadataCopyTest() throws Exception {
        context.turnOffAuthorisationSystem();

        // Initialize MDVs and Relationships
        Relationship r1 = relationshipService.create(context, publication1, author1, isAuthorOfPublication, -1, -1);
        itemService.addMetadata(context, publication1, dcSchema, contributorElement, authorQualifier, null, "MDV 1");
        Relationship r2 = relationshipService.create(context, publication1, author2, isAuthorOfPublication, -1, -1);
        itemService.addMetadata(context, publication1, dcSchema, contributorElement, authorQualifier, null, "MDV 2");
        Relationship r3 = relationshipService.create(context, publication1, author3, isAuthorOfPublication, -1, -1);

        relationshipService.delete(context, r3, true, false);

        context.restoreAuthSystemState();

        // Check relationship order
        assertLeftPlace(r1, 0);
        assertLeftPlace(r2, 2);
        assertRelationMetadataOrder(publication1, isAuthorOfPublication, Arrays.asList(r1, r2, null));
        assertMetadataOrder(publication1, "dc.contributor.author", List.of(
            "Author, First",
            "MDV 1",
            "Author, Second",
            "MDV 2",
            "Author, Third"     // this is not longer a relationship
        ));
    }

    @Test
    public void deleteUseForPlaceRelationshipFromRightStartNoMetadataTest() throws Exception {
        context.turnOffAuthorisationSystem();

        // Add three Publications to the same Author, appending to the end
        Relationship r1 = relationshipService.create(context, publication1, author1, isAuthorOfPublication, -1, -1);
        Relationship r2 = relationshipService.create(context, publication2, author1, isAuthorOfPublication, -1, -1);
        Relationship r3 = relationshipService.create(context, publication3, author1, isAuthorOfPublication, -1, -1);

        // Delete the first Publication
        relationshipService.delete(context, r1);

        context.restoreAuthSystemState();

        // Check relationship order
        assertRightPlace(r2, 0);
        assertRightPlace(r3, 1);
        assertRelationMetadataOrder(author1, isAuthorOfPublication, List.of(r2, r3));
    }

    @Test
    public void deleteUseForPlaceRelationshipFromRightMiddleNoMetadataTest() throws Exception {
        context.turnOffAuthorisationSystem();

        // Add three Publications to the same Author, appending to the end
        Relationship r1 = relationshipService.create(context, publication1, author1, isAuthorOfPublication, -1, -1);
        Relationship r2 = relationshipService.create(context, publication2, author1, isAuthorOfPublication, -1, -1);
        Relationship r3 = relationshipService.create(context, publication3, author1, isAuthorOfPublication, -1, -1);

        // Delete the second Publication
        relationshipService.delete(context, r2);

        context.restoreAuthSystemState();

        // Check relationship order
        assertRightPlace(r1, 0);
        assertRightPlace(r3, 1);
        assertRelationMetadataOrder(author1, isAuthorOfPublication, List.of(r1, r3));
    }

    @Test
    public void deleteUseForPlaceRelationshipFromRightEndNoMetadataTest() throws Exception {
        context.turnOffAuthorisationSystem();

        // Add three Publications to the same Author, appending to the end
        Relationship r1 = relationshipService.create(context, publication1, author1, isAuthorOfPublication, -1, -1);
        Relationship r2 = relationshipService.create(context, publication2, author1, isAuthorOfPublication, -1, -1);
        Relationship r3 = relationshipService.create(context, publication3, author1, isAuthorOfPublication, -1, -1);

        // Delete the third Publication
        relationshipService.delete(context, r3);

        context.restoreAuthSystemState();

        // Check relationship order
        assertRightPlace(r1, 0);
        assertRightPlace(r2, 1);
        assertRelationMetadataOrder(author1, isAuthorOfPublication, List.of(r1, r2));
    }

    @Test
    public void deleteNonUseForPlaceRelationshipFromLeftStartNoMetadataTest() throws Exception {
        context.turnOffAuthorisationSystem();

        // Add three Projects to the same Author, appending to the end
        Relationship r1 = relationshipService.create(context, author1, project1, isProjectOfPerson, -1, -1);
        Relationship r2 = relationshipService.create(context, author1, project2, isProjectOfPerson, -1, -1);
        Relationship r3 = relationshipService.create(context, author1, project3, isProjectOfPerson, -1, -1);

        // Delete the first Author
        relationshipService.delete(context, r1);

        context.restoreAuthSystemState();

        // Check relationship order
        assertLeftPlace(r2, 0);
        assertLeftPlace(r3, 1);
        assertRelationMetadataOrder(author1, isProjectOfPerson, List.of(r2, r3));
    }

    @Test
    public void deleteNonUseForPlaceRelationshipFromLeftMiddleNoMetadataTest() throws Exception {
        context.turnOffAuthorisationSystem();

        // Add three Projects to the same Author, appending to the end
        Relationship r1 = relationshipService.create(context, author1, project1, isProjectOfPerson, -1, -1);
        Relationship r2 = relationshipService.create(context, author1, project2, isProjectOfPerson, -1, -1);
        Relationship r3 = relationshipService.create(context, author1, project3, isProjectOfPerson, -1, -1);

        // Delete the second Author
        relationshipService.delete(context, r2);

        context.restoreAuthSystemState();

        // Check relationship order
        assertLeftPlace(r1, 0);
        assertLeftPlace(r3, 1);
        assertRelationMetadataOrder(author1, isProjectOfPerson, List.of(r1, r3));
    }

    @Test
    public void deleteNonUseForPlaceRelationshipFromLeftEndNoMetadataTest() throws Exception {
        context.turnOffAuthorisationSystem();

        // Add three Projects to the same Author, appending to the end
        Relationship r1 = relationshipService.create(context, author1, project1, isProjectOfPerson, -1, -1);
        Relationship r2 = relationshipService.create(context, author1, project2, isProjectOfPerson, -1, -1);
        Relationship r3 = relationshipService.create(context, author1, project3, isProjectOfPerson, -1, -1);

        // Delete the third Author
        relationshipService.delete(context, r3);

        context.restoreAuthSystemState();

        // Check relationship order
        assertLeftPlace(r1, 0);
        assertLeftPlace(r2, 1);
        assertRelationMetadataOrder(author1, isProjectOfPerson, List.of(r1, r2));
    }

    @Test
    public void deleteNonUseForPlaceRelationshipFromRightStartNoMetadataTest() throws Exception {
        context.turnOffAuthorisationSystem();

        // Add three Authors to the same Project, appending to the end
        Relationship r1 = relationshipService.create(context, author1, project1, isProjectOfPerson, -1, -1);
        Relationship r2 = relationshipService.create(context, author2, project1, isProjectOfPerson, -1, -1);
        Relationship r3 = relationshipService.create(context, author3, project1, isProjectOfPerson, -1, -1);

        // Delete the first Publication
        relationshipService.delete(context, r1);

        context.restoreAuthSystemState();

        // Check relationship order
        assertRightPlace(r2, 0);
        assertRightPlace(r3, 1);
        assertRelationMetadataOrder(project1, isProjectOfPerson, List.of(r2, r3));
    }

    @Test
    public void deleteNonUseForPlaceRelationshipFromRightMiddleNoMetadataTest() throws Exception {
        context.turnOffAuthorisationSystem();

        // Add three Authors to the same Project, appending to the end
        Relationship r1 = relationshipService.create(context, author1, project1, isProjectOfPerson, -1, -1);
        Relationship r2 = relationshipService.create(context, author2, project1, isProjectOfPerson, -1, -1);
        Relationship r3 = relationshipService.create(context, author3, project1, isProjectOfPerson, -1, -1);

        // Delete the second Publication
        relationshipService.delete(context, r2);

        context.restoreAuthSystemState();

        // Check relationship order
        assertRightPlace(r1, 0);
        assertRightPlace(r3, 1);
        assertRelationMetadataOrder(project1, isProjectOfPerson, List.of(r1, r3));
    }

    @Test
    public void deleteNonUseForPlaceRelationshipFromRightMiddleNoMetadataTest_ignoreOtherRels() throws Exception {
        context.turnOffAuthorisationSystem();

        // Add three Authors to the same Project, appending to the end
        Relationship r1 = relationshipService.create(context, author1, project1, isProjectOfPerson, -1, -1);
        Relationship r2 = relationshipService.create(context, author2, project1, isProjectOfPerson, -1, -1);

        // NOTE: unrelated relationship => should not be affected
        Relationship ur1 = relationshipService.create(context, publication1, project1, isProjectOfPublication, -1, -1);

        Relationship r3 = relationshipService.create(context, author3, project1, isProjectOfPerson, -1, -1);

        // Delete the second Publication
        relationshipService.delete(context, r2);

        context.restoreAuthSystemState();

        // Check relationship order
        assertRightPlace(r1, 0);
        assertRightPlace(r3, 1);
        assertRelationMetadataOrder(project1, isProjectOfPerson, List.of(r1, r3));

        // check unaffected relationships
        assertLeftPlace(ur1, 0);
        assertRightPlace(ur1, 0);
    }

    @Test
    public void deleteNonUseForPlaceRelationshipFromRightEndNoMetadataTest() throws Exception {
        context.turnOffAuthorisationSystem();

        // Add three Authors to the same Project, appending to the end
        Relationship r1 = relationshipService.create(context, author1, project1, isProjectOfPerson, -1, -1);
        Relationship r2 = relationshipService.create(context, author2, project1, isProjectOfPerson, -1, -1);
        Relationship r3 = relationshipService.create(context, author3, project1, isProjectOfPerson, -1, -1);

        // Delete the third Publication
        relationshipService.delete(context, r3);

        context.restoreAuthSystemState();

        // Check relationship order
        assertRightPlace(r1, 0);
        assertRightPlace(r2, 1);
        assertRelationMetadataOrder(project1, isProjectOfPerson, List.of(r1, r2));
    }

    @Test
    public void changeLeftItemInUseForPlaceRelationshipAtTheStartNoMetadataTest() throws Exception {
        context.turnOffAuthorisationSystem();

        // Add three Authors to publication1, appending to the end
        Relationship r1 = relationshipService.create(context, publication1, author1, isAuthorOfPublication, -1, -1);
        Relationship r2 = relationshipService.create(context, publication1, author2, isAuthorOfPublication, -1, -1);
        Relationship r3 = relationshipService.create(context, publication1, author3, isAuthorOfPublication, -1, -1);

        // Add three Authors to publication2, appending to the end
        Relationship r4 = relationshipService.create(context, publication2, author4, isAuthorOfPublication, -1, -1);
        Relationship r5 = relationshipService.create(context, publication2, author5, isAuthorOfPublication, -1, -1);
        Relationship r6 = relationshipService.create(context, publication2, author6, isAuthorOfPublication, -1, -1);

        // Move r1 to publication 2
        relationshipService.move(context, r1, publication2, null);

        context.restoreAuthSystemState();

        // Check relationship order for publication1
        assertLeftPlace(r2, 0);     // should both move down as the first Relationship was removed
        assertLeftPlace(r3, 1);
        assertRelationMetadataOrder(publication1, isAuthorOfPublication, List.of(r2, r3));

        // Check relationship order for publication2
        assertLeftPlace(r4, 0);     // previous Relationships should stay where they were
        assertLeftPlace(r5, 1);
        assertLeftPlace(r6, 2);
        assertLeftPlace(r1, 3);     // moved Relationship should be appended to the end
        assertRelationMetadataOrder(publication2, isAuthorOfPublication, List.of(r4, r5, r6, r1));
    }

    @Test
    public void changeLeftItemInUseForPlaceRelationshipInTheMiddleNoMetadataTest() throws Exception {
        context.turnOffAuthorisationSystem();

        // Add three Authors to publication1, appending to the end
        Relationship r1 = relationshipService.create(context, publication1, author1, isAuthorOfPublication, -1, -1);
        Relationship r2 = relationshipService.create(context, publication1, author2, isAuthorOfPublication, -1, -1);
        Relationship r3 = relationshipService.create(context, publication1, author3, isAuthorOfPublication, -1, -1);

        // Add three Authors to publication2, appending to the end
        Relationship r4 = relationshipService.create(context, publication2, author4, isAuthorOfPublication, -1, -1);
        Relationship r5 = relationshipService.create(context, publication2, author5, isAuthorOfPublication, -1, -1);
        Relationship r6 = relationshipService.create(context, publication2, author6, isAuthorOfPublication, -1, -1);

        // Move r2 to publication 2
        relationshipService.move(context, r2, publication2, null);

        context.restoreAuthSystemState();

        // Check relationship order for publication1
        assertLeftPlace(r1, 0);
        assertLeftPlace(r3, 1);     // should move down as the second Relationship was removed
        assertRelationMetadataOrder(publication1, isAuthorOfPublication, List.of(r1, r3));

        // Check relationship order for publication2
        assertLeftPlace(r4, 0);     // previous Relationships should stay where they were
        assertLeftPlace(r5, 1);
        assertLeftPlace(r6, 2);
        assertLeftPlace(r2, 3);     // moved Relationship should be appended to the end
        assertRelationMetadataOrder(publication2, isAuthorOfPublication, List.of(r4, r5, r6, r2));
    }

    @Test
    public void changeLeftItemInUseForPlaceRelationshipAtTheEndNoMetadataTest() throws Exception {
        context.turnOffAuthorisationSystem();

        // Add three Authors to publication1, appending to the end
        Relationship r1 = relationshipService.create(context, publication1, author1, isAuthorOfPublication, -1, -1);
        Relationship r2 = relationshipService.create(context, publication1, author2, isAuthorOfPublication, -1, -1);
        Relationship r3 = relationshipService.create(context, publication1, author3, isAuthorOfPublication, -1, -1);

        // Add three Authors to publication2, appending to the end
        Relationship r4 = relationshipService.create(context, publication2, author4, isAuthorOfPublication, -1, -1);
        Relationship r5 = relationshipService.create(context, publication2, author5, isAuthorOfPublication, -1, -1);
        Relationship r6 = relationshipService.create(context, publication2, author6, isAuthorOfPublication, -1, -1);

        // Move r3 to publication 2
        relationshipService.move(context, r3, publication2, null);

        context.restoreAuthSystemState();

        // Check relationship order for publication1
        assertLeftPlace(r1, 0);
        assertLeftPlace(r2, 1);
        assertRelationMetadataOrder(publication1, isAuthorOfPublication, List.of(r1, r2));

        // Check relationship order for publication2
        assertLeftPlace(r4, 0);     // previous Relationships should stay where they were
        assertLeftPlace(r5, 1);
        assertLeftPlace(r6, 2);
        assertLeftPlace(r3, 3);     // moved Relationship should be appended to the end
        assertRelationMetadataOrder(publication2, isAuthorOfPublication, List.of(r4, r5, r6, r3));
    }

    @Test
    public void changeLeftItemInUseForPlaceRelationshipAtTheStartWithMetadataTest() throws Exception {
        context.turnOffAuthorisationSystem();

        // Add three Authors to publication1, with regular MDVs in between
        Relationship r1 = relationshipService.create(context, publication1, author1, isAuthorOfPublication, -1, -1);
        itemService.addMetadata(context, publication1, dcSchema, contributorElement, authorQualifier, null, "MDV 1");
        Relationship r2 = relationshipService.create(context, publication1, author2, isAuthorOfPublication, -1, -1);
        itemService.addMetadata(context, publication1, dcSchema, contributorElement, authorQualifier, null, "MDV 2");
        Relationship r3 = relationshipService.create(context, publication1, author3, isAuthorOfPublication, -1, -1);

        // Add three Authors to publication2, with regular MDVs in between
        itemService.addMetadata(context, publication2, dcSchema, contributorElement, authorQualifier, null, "MDV 3");
        Relationship r4 = relationshipService.create(context, publication2, author4, isAuthorOfPublication, -1, -1);
        Relationship r5 = relationshipService.create(context, publication2, author5, isAuthorOfPublication, -1, -1);
        itemService.addMetadata(context, publication2, dcSchema, contributorElement, authorQualifier, null, "MDV 4");
        Relationship r6 = relationshipService.create(context, publication2, author6, isAuthorOfPublication, -1, -1);

        // Move r1 to publication 2
        relationshipService.move(context, r1, publication2, null);

        context.restoreAuthSystemState();

        // Check relationship order for publication1
        assertLeftPlace(r2, 1);     // should both move down as the first Relationship was removed
        assertLeftPlace(r3, 3);
        assertRelationMetadataOrder(publication1, isAuthorOfPublication, List.of(r2, r3));
        assertMetadataOrder(publication1, "dc.contributor.author", List.of(
            "MDV 1",
            "Author, Second",
            "MDV 2",
            "Author, Third"
        ));

        // Check relationship order for publication2
        assertLeftPlace(r4, 1);     // previous Relationships should stay where they were
        assertLeftPlace(r5, 2);
        assertLeftPlace(r6, 4);
        assertLeftPlace(r1, 5);     // moved Relationship should be appended to the end
        assertRelationMetadataOrder(publication2, isAuthorOfPublication, List.of(r4, r5, r6, r1));
        assertMetadataOrder(publication2, "dc.contributor.author", List.of(
            "MDV 3",
            "Author, Fourth",
            "Author, Fifth",
            "MDV 4",
            "Author, Sixth",
            "Author, First"
        ));
    }

    @Test
    public void changeLeftItemInUseForPlaceRelationshipInTheMiddleWithMetadataTest() throws Exception {
        context.turnOffAuthorisationSystem();

        // Add three Authors to publication1, with regular MDVs in between
        Relationship r1 = relationshipService.create(context, publication1, author1, isAuthorOfPublication, -1, -1);
        itemService.addMetadata(context, publication1, dcSchema, contributorElement, authorQualifier, null, "MDV 1");
        Relationship r2 = relationshipService.create(context, publication1, author2, isAuthorOfPublication, -1, -1);
        itemService.addMetadata(context, publication1, dcSchema, contributorElement, authorQualifier, null, "MDV 2");
        Relationship r3 = relationshipService.create(context, publication1, author3, isAuthorOfPublication, -1, -1);

        // Add three Authors to publication2, with regular MDVs in between
        itemService.addMetadata(context, publication2, dcSchema, contributorElement, authorQualifier, null, "MDV 3");
        Relationship r4 = relationshipService.create(context, publication2, author4, isAuthorOfPublication, -1, -1);
        Relationship r5 = relationshipService.create(context, publication2, author5, isAuthorOfPublication, -1, -1);
        itemService.addMetadata(context, publication2, dcSchema, contributorElement, authorQualifier, null, "MDV 4");
        Relationship r6 = relationshipService.create(context, publication2, author6, isAuthorOfPublication, -1, -1);

        // Move r2 to publication 2
        relationshipService.move(context, r2, publication2, null);

        context.restoreAuthSystemState();

        // Check relationship order for publication1
        assertLeftPlace(r1, 0);     // should both move down as the first Relationship was removed
        assertLeftPlace(r3, 3);
        assertRelationMetadataOrder(publication1, isAuthorOfPublication, List.of(r1, r3));
        assertMetadataOrder(publication1, "dc.contributor.author", List.of(
            "Author, First",
            "MDV 1",
            "MDV 2",
            "Author, Third"
        ));

        // Check relationship order for publication2
        assertLeftPlace(r4, 1);     // previous Relationships should stay where they were
        assertLeftPlace(r5, 2);
        assertLeftPlace(r6, 4);
        assertLeftPlace(r2, 5);     // moved Relationship should be appended to the end
        assertRelationMetadataOrder(publication2, isAuthorOfPublication, List.of(r4, r5, r6, r2));
        assertMetadataOrder(publication2, "dc.contributor.author", List.of(
            "MDV 3",
            "Author, Fourth",
            "Author, Fifth",
            "MDV 4",
            "Author, Sixth",
            "Author, Second"
        ));
    }

    @Test
    public void changeLeftItemInUseForPlaceRelationshipInTheMiddleWithMetadataTest_ignoreOtherRels() throws Exception {
        context.turnOffAuthorisationSystem();

        // Add three Authors to publication1, with regular MDVs in between
        Relationship r1 = relationshipService.create(context, publication1, author1, isAuthorOfPublication, -1, -1);
        itemService.addMetadata(context, publication1, dcSchema, contributorElement, authorQualifier, null, "MDV 1");

        // NOTE: unrelated relationship => should not be affected
        Relationship ur1 = relationshipService.create(context, publication1, project1, isProjectOfPublication, -1, -1);

        Relationship r2 = relationshipService.create(context, publication1, author2, isAuthorOfPublication, -1, -1);
        itemService.addMetadata(context, publication1, dcSchema, contributorElement, authorQualifier, null, "MDV 2");

        // NOTE: unrelated relationship => should not be affected
        Relationship ur2 = relationshipService.create(context, publication1, project3, isProjectOfPublication, -1, -1);

        Relationship r3 = relationshipService.create(context, publication1, author3, isAuthorOfPublication, -1, -1);

        // NOTE: unrelated relationship => should not be affected
        Relationship ur3 = relationshipService.create(context, publication2, project2, isProjectOfPublication, -1, -1);

        // Add three Authors to publication2, with regular MDVs in between
        itemService.addMetadata(context, publication2, dcSchema, contributorElement, authorQualifier, null, "MDV 3");
        Relationship r4 = relationshipService.create(context, publication2, author4, isAuthorOfPublication, -1, -1);
        Relationship r5 = relationshipService.create(context, publication2, author5, isAuthorOfPublication, -1, -1);

        // NOTE: unrelated relationship => should not be affected
        Relationship ur4 = relationshipService.create(context, publication2, project1, isProjectOfPublication, -1, -1);

        itemService.addMetadata(context, publication2, dcSchema, contributorElement, authorQualifier, null, "MDV 4");
        Relationship r6 = relationshipService.create(context, publication2, author6, isAuthorOfPublication, -1, -1);

        // Move r2 to publication 2
        relationshipService.move(context, r2, publication2, null);

        context.restoreAuthSystemState();

        // Check relationship order for publication1
        assertLeftPlace(r1, 0);     // should both move down as the first Relationship was removed
        assertLeftPlace(r3, 3);
        assertRelationMetadataOrder(publication1, isAuthorOfPublication, List.of(r1, r3));
        assertMetadataOrder(publication1, "dc.contributor.author", List.of(
            "Author, First",
            "MDV 1",
            "MDV 2",
            "Author, Third"
        ));

        // Check relationship order for publication2
        assertLeftPlace(r4, 1);     // previous Relationships should stay where they were
        assertLeftPlace(r5, 2);
        assertLeftPlace(r6, 4);
        assertLeftPlace(r2, 5);     // moved Relationship should be appended to the end
        assertRelationMetadataOrder(publication2, isAuthorOfPublication, List.of(r4, r5, r6, r2));
        assertMetadataOrder(publication2, "dc.contributor.author", List.of(
            "MDV 3",
            "Author, Fourth",
            "Author, Fifth",
            "MDV 4",
            "Author, Sixth",
            "Author, Second"
        ));

        // check unaffected relationships
        assertLeftPlace(ur1, 0);
        assertRightPlace(ur1, 0);
        assertLeftPlace(ur2, 1);
        assertRightPlace(ur2, 0);
        assertLeftPlace(ur3, 0);
        assertRightPlace(ur3, 0);
        assertLeftPlace(ur4, 1);
        assertRightPlace(ur4, 1);
    }

    @Test
    public void changeLeftItemInUseForPlaceRelationshipAtTheEndWithMetadataTest() throws Exception {
        context.turnOffAuthorisationSystem();

        // Add three Authors to publication1, with regular MDVs in between
        Relationship r1 = relationshipService.create(context, publication1, author1, isAuthorOfPublication, -1, -1);
        itemService.addMetadata(context, publication1, dcSchema, contributorElement, authorQualifier, null, "MDV 1");
        Relationship r2 = relationshipService.create(context, publication1, author2, isAuthorOfPublication, -1, -1);
        itemService.addMetadata(context, publication1, dcSchema, contributorElement, authorQualifier, null, "MDV 2");
        Relationship r3 = relationshipService.create(context, publication1, author3, isAuthorOfPublication, -1, -1);

        // Add three Authors to publication2, with regular MDVs in between
        itemService.addMetadata(context, publication2, dcSchema, contributorElement, authorQualifier, null, "MDV 3");
        Relationship r4 = relationshipService.create(context, publication2, author4, isAuthorOfPublication, -1, -1);
        Relationship r5 = relationshipService.create(context, publication2, author5, isAuthorOfPublication, -1, -1);
        itemService.addMetadata(context, publication2, dcSchema, contributorElement, authorQualifier, null, "MDV 4");
        Relationship r6 = relationshipService.create(context, publication2, author6, isAuthorOfPublication, -1, -1);

        // Move r3 to publication 2
        relationshipService.move(context, r3, publication2, null);

        context.restoreAuthSystemState();

        // Check relationship order for publication1
        assertLeftPlace(r1, 0);
        assertLeftPlace(r2, 2);
        assertRelationMetadataOrder(publication1, isAuthorOfPublication, List.of(r1, r2));
        assertMetadataOrder(publication1, "dc.contributor.author", List.of(
            "Author, First",
            "MDV 1",
            "Author, Second",
            "MDV 2"
        ));

        // Check relationship order for publication2
        assertLeftPlace(r4, 1);     // previous Relationships should stay where they were
        assertLeftPlace(r5, 2);
        assertLeftPlace(r6, 4);
        assertLeftPlace(r3, 5);     // moved Relationship should be appended to the end
        assertRelationMetadataOrder(publication2, isAuthorOfPublication, List.of(r4, r5, r6, r3));
        assertMetadataOrder(publication2, "dc.contributor.author", List.of(
            "MDV 3",
            "Author, Fourth",
            "Author, Fifth",
            "MDV 4",
            "Author, Sixth",
            "Author, Third"
        ));
    }

    @Test
    public void changeRightItemInUseForPlaceRelationshipAtTheStartNoMetadataTest() throws Exception {
        context.turnOffAuthorisationSystem();

        // Add three Publications to author1, appending to the end
        Relationship r1 = relationshipService.create(context, publication1, author1, isAuthorOfPublication, -1, -1);
        Relationship r2 = relationshipService.create(context, publication2, author1, isAuthorOfPublication, -1, -1);
        Relationship r3 = relationshipService.create(context, publication3, author1, isAuthorOfPublication, -1, -1);

        // Add three Publications to author2, appending to the end
        Relationship r4 = relationshipService.create(context, publication4, author2, isAuthorOfPublication, -1, -1);
        Relationship r5 = relationshipService.create(context, publication5, author2, isAuthorOfPublication, -1, -1);
        Relationship r6 = relationshipService.create(context, publication6, author2, isAuthorOfPublication, -1, -1);

        // Move r1 to author2
        relationshipService.move(context, r1, null, author2);

        context.restoreAuthSystemState();

        // Check relationship order for author1
        assertRightPlace(r2, 0);     // should both move down as the first Relationship was removed
        assertRightPlace(r3, 1);
        assertRelationMetadataOrder(author1, isAuthorOfPublication, List.of(r2, r3));

        // Check relationship order for author2
        assertRightPlace(r4, 0);     // previous Relationships should stay where they were
        assertRightPlace(r5, 1);
        assertRightPlace(r6, 2);
        assertRightPlace(r1, 3);     // moved Relationship should be appended to the end
        assertRelationMetadataOrder(author2, isAuthorOfPublication, List.of(r4, r5, r6, r1));
    }

    @Test
    public void changeRightItemInUseForPlaceRelationshipInTheMiddleNoMetadataTest() throws Exception {
        context.turnOffAuthorisationSystem();

        // Add three Publications to author1, appending to the end
        Relationship r1 = relationshipService.create(context, publication1, author1, isAuthorOfPublication, -1, -1);
        Relationship r2 = relationshipService.create(context, publication2, author1, isAuthorOfPublication, -1, -1);
        Relationship r3 = relationshipService.create(context, publication3, author1, isAuthorOfPublication, -1, -1);

        // Add three Publications to author2, appending to the end
        Relationship r4 = relationshipService.create(context, publication4, author2, isAuthorOfPublication, -1, -1);
        Relationship r5 = relationshipService.create(context, publication5, author2, isAuthorOfPublication, -1, -1);
        Relationship r6 = relationshipService.create(context, publication6, author2, isAuthorOfPublication, -1, -1);

        // Move r2 to author2
        relationshipService.move(context, r2, null, author2);

        context.restoreAuthSystemState();

        // Check relationship order for author1
        assertRightPlace(r1, 0);
        assertRightPlace(r3, 1);    // should move down as the first Relationship was removed
        assertRelationMetadataOrder(author1, isAuthorOfPublication, List.of(r1, r3));

        // Check relationship order for author2
        assertRightPlace(r4, 0);     // previous Relationships should stay where they were
        assertRightPlace(r5, 1);
        assertRightPlace(r6, 2);
        assertRightPlace(r2, 3);     // moved Relationship should be appended to the end
        assertRelationMetadataOrder(author2, isAuthorOfPublication, List.of(r4, r5, r6, r2));
    }

    @Test
    public void changeRightItemInUseForPlaceRelationshipInTheMiddleNoMetadataTest_ignoreOtherRels() throws Exception {
        context.turnOffAuthorisationSystem();

        // Add three Publications to author1, appending to the end
        Relationship r1 = relationshipService.create(context, publication1, author1, isAuthorOfPublication, -1, -1);
        Relationship r2 = relationshipService.create(context, publication2, author1, isAuthorOfPublication, -1, -1);

        // NOTE: unrelated relationship => should not be affected
        Relationship ur1 = relationshipService.create(context, author1, project1, isProjectOfPerson, -1, -1);

        Relationship r3 = relationshipService.create(context, publication3, author1, isAuthorOfPublication, -1, -1);

        // Add three Publications to author2, appending to the end
        Relationship r4 = relationshipService.create(context, publication4, author2, isAuthorOfPublication, -1, -1);
        Relationship r5 = relationshipService.create(context, publication5, author2, isAuthorOfPublication, -1, -1);
        Relationship r6 = relationshipService.create(context, publication6, author2, isAuthorOfPublication, -1, -1);

        // NOTE: unrelated relationship => should not be affected
        Relationship ur2 = relationshipService.create(context, author2, project1, isProjectOfPerson, -1, -1);

        // Move r2 to author2
        relationshipService.move(context, r2, null, author2);

        context.restoreAuthSystemState();

        // Check relationship order for author1
        assertRightPlace(r1, 0);
        assertRightPlace(r3, 1);    // should move down as the first Relationship was removed
        assertRelationMetadataOrder(author1, isAuthorOfPublication, List.of(r1, r3));

        // Check relationship order for author2
        assertRightPlace(r4, 0);     // previous Relationships should stay where they were
        assertRightPlace(r5, 1);
        assertRightPlace(r6, 2);
        assertRightPlace(r2, 3);     // moved Relationship should be appended to the end
        assertRelationMetadataOrder(author2, isAuthorOfPublication, List.of(r4, r5, r6, r2));

        // check unaffected relationships
        assertLeftPlace(ur1, 0);
        assertRightPlace(ur1, 0);
        assertLeftPlace(ur2, 0);
        assertRightPlace(ur2, 1);
    }

    @Test
    public void changeRightItemInUseForPlaceRelationshipAtTheEndNoMetadataTest() throws Exception {
        context.turnOffAuthorisationSystem();

        // Add three Publications to author1, appending to the end
        Relationship r1 = relationshipService.create(context, publication1, author1, isAuthorOfPublication, -1, -1);
        Relationship r2 = relationshipService.create(context, publication2, author1, isAuthorOfPublication, -1, -1);
        Relationship r3 = relationshipService.create(context, publication3, author1, isAuthorOfPublication, -1, -1);

        // Add three Publications to author2, appending to the end
        Relationship r4 = relationshipService.create(context, publication4, author2, isAuthorOfPublication, -1, -1);
        Relationship r5 = relationshipService.create(context, publication5, author2, isAuthorOfPublication, -1, -1);
        Relationship r6 = relationshipService.create(context, publication6, author2, isAuthorOfPublication, -1, -1);

        // Move r3 to author2
        relationshipService.move(context, r3, null, author2);

        context.restoreAuthSystemState();

        // Check relationship order for author1
        assertRightPlace(r1, 0);
        assertRightPlace(r2, 1);
        assertRelationMetadataOrder(author1, isAuthorOfPublication, List.of(r1, r2));

        // Check relationship order for author2
        assertRightPlace(r4, 0);     // previous Relationships should stay where they were
        assertRightPlace(r5, 1);
        assertRightPlace(r6, 2);
        assertRightPlace(r3, 3);     // moved Relationship should be appended to the end
        assertRelationMetadataOrder(author2, isAuthorOfPublication, List.of(r4, r5, r6, r3));
    }

    @Test
    public void changeLeftItemInNonUseForPlaceRelationshipAtTheStart() throws Exception {
        context.turnOffAuthorisationSystem();

        // Add three Projects to author1, appending to the end
        Relationship r1 = relationshipService.create(context, author1, project1, isProjectOfPerson, -1, -1);
        Relationship r2 = relationshipService.create(context, author1, project2, isProjectOfPerson, -1, -1);
        Relationship r3 = relationshipService.create(context, author1, project3, isProjectOfPerson, -1, -1);

        // Add three Projects to author2, appending to the end
        Relationship r4 = relationshipService.create(context, author2, project4, isProjectOfPerson, -1, -1);
        Relationship r5 = relationshipService.create(context, author2, project5, isProjectOfPerson, -1, -1);
        Relationship r6 = relationshipService.create(context, author2, project6, isProjectOfPerson, -1, -1);

        // Move r1 to publication 2
        relationshipService.move(context, r1, author2, null);

        context.restoreAuthSystemState();

        // Check relationship order for author1
        assertLeftPlace(r2, 0);     // should both move down as the first Relationship was removed
        assertLeftPlace(r3, 1);
        assertRelationMetadataOrder(author1, isProjectOfPerson, List.of(r2, r3));

        // Check relationship order for author2
        assertLeftPlace(r4, 0);     // previous Relationships should stay where they were
        assertLeftPlace(r5, 1);
        assertLeftPlace(r6, 2);
        assertLeftPlace(r1, 3);     // moved Relationship should be appended to the end
        assertRelationMetadataOrder(author2, isProjectOfPerson, List.of(r4, r5, r6, r1));
    }

    @Test
    public void changeLeftItemInNonUseNonForPlaceRelationshipInTheMiddle() throws Exception {
        context.turnOffAuthorisationSystem();

        // Add three Projects to author1, appending to the end
        Relationship r1 = relationshipService.create(context, author1, project1, isProjectOfPerson, -1, -1);
        Relationship r2 = relationshipService.create(context, author1, project2, isProjectOfPerson, -1, -1);
        Relationship r3 = relationshipService.create(context, author1, project3, isProjectOfPerson, -1, -1);

        // Add three Projects to author2, appending to the end
        Relationship r4 = relationshipService.create(context, author2, project4, isProjectOfPerson, -1, -1);
        Relationship r5 = relationshipService.create(context, author2, project5, isProjectOfPerson, -1, -1);
        Relationship r6 = relationshipService.create(context, author2, project6, isProjectOfPerson, -1, -1);

        // Move r2 to author2
        relationshipService.move(context, r2, author2, null);

        context.restoreAuthSystemState();

        // Check relationship order for author1
        assertLeftPlace(r1, 0);
        assertLeftPlace(r3, 1);     // should move down as the second Relationship was removed
        assertRelationMetadataOrder(author1, isProjectOfPerson, List.of(r1, r3));

        // Check relationship order for author2
        assertLeftPlace(r4, 0);     // previous Relationships should stay where they were
        assertLeftPlace(r5, 1);
        assertLeftPlace(r6, 2);
        assertLeftPlace(r2, 3);     // moved Relationship should be appended to the end
        assertRelationMetadataOrder(author2, isProjectOfPerson, List.of(r4, r5, r6, r2));
    }

    @Test
    public void changeLeftItemInNonUseForPlaceRelationshipAtTheEnd() throws Exception {
        context.turnOffAuthorisationSystem();

        // Add three Projects to author1, appending to the end
        Relationship r1 = relationshipService.create(context, author1, project1, isProjectOfPerson, -1, -1);
        Relationship r2 = relationshipService.create(context, author1, project2, isProjectOfPerson, -1, -1);
        Relationship r3 = relationshipService.create(context, author1, project3, isProjectOfPerson, -1, -1);

        // Add three Projects to author2, appending to the end
        Relationship r4 = relationshipService.create(context, author2, project4, isProjectOfPerson, -1, -1);
        Relationship r5 = relationshipService.create(context, author2, project5, isProjectOfPerson, -1, -1);
        Relationship r6 = relationshipService.create(context, author2, project6, isProjectOfPerson, -1, -1);

        // Move r3 to author2
        relationshipService.move(context, r3, author2, null);

        context.restoreAuthSystemState();

        // Check relationship order for publication1
        assertLeftPlace(r1, 0);
        assertLeftPlace(r2, 1);
        assertRelationMetadataOrder(author1, isProjectOfPerson, List.of(r1, r2));

        // Check relationship order for publication2
        assertLeftPlace(r4, 0);     // previous Relationships should stay where they were
        assertLeftPlace(r5, 1);
        assertLeftPlace(r6, 2);
        assertLeftPlace(r3, 3);     // moved Relationship should be appended to the end
        assertRelationMetadataOrder(author2, isProjectOfPerson, List.of(r4, r5, r6, r3));
    }

    @Test
    public void changeRightItemInUseNonForPlaceRelationshipAtTheStartTest() throws Exception {
        context.turnOffAuthorisationSystem();

        // Add three Authors to project1, appending to the end
        Relationship r1 = relationshipService.create(context, author1, project1, isProjectOfPerson, -1, -1);
        Relationship r2 = relationshipService.create(context, author2, project1, isProjectOfPerson, -1, -1);
        Relationship r3 = relationshipService.create(context, author3, project1, isProjectOfPerson, -1, -1);

        // Add three Authors to project2, appending to the end
        Relationship r4 = relationshipService.create(context, author4, project2, isProjectOfPerson, -1, -1);
        Relationship r5 = relationshipService.create(context, author5, project2, isProjectOfPerson, -1, -1);
        Relationship r6 = relationshipService.create(context, author6, project2, isProjectOfPerson, -1, -1);

        // Move r1 to project2
        relationshipService.move(context, r1, null, project2);

        context.restoreAuthSystemState();

        // Check relationship order for project1
        assertRightPlace(r2, 0);     // should both move down as the first Relationship was removed
        assertRightPlace(r3, 1);
        assertRelationMetadataOrder(project1, isProjectOfPerson, List.of(r2, r3));

        // Check relationship order for project2
        assertRightPlace(r4, 0);     // previous Relationships should stay where they were
        assertRightPlace(r5, 1);
        assertRightPlace(r6, 2);
        assertRightPlace(r1, 3);     // moved Relationship should be appended to the end
        assertRelationMetadataOrder(project2, isProjectOfPerson, List.of(r4, r5, r6, r1));
    }

    @Test
    public void changeRightItemInNonUseForPlaceRelationshipInTheMiddleTest() throws Exception {
        context.turnOffAuthorisationSystem();

        // Add three Authors to project1, appending to the end
        Relationship r1 = relationshipService.create(context, author1, project1, isProjectOfPerson, -1, -1);
        Relationship r2 = relationshipService.create(context, author2, project1, isProjectOfPerson, -1, -1);
        Relationship r3 = relationshipService.create(context, author3, project1, isProjectOfPerson, -1, -1);

        // Add three Authors to project2, appending to the end
        Relationship r4 = relationshipService.create(context, author4, project2, isProjectOfPerson, -1, -1);
        Relationship r5 = relationshipService.create(context, author5, project2, isProjectOfPerson, -1, -1);
        Relationship r6 = relationshipService.create(context, author6, project2, isProjectOfPerson, -1, -1);

        // Move r2 to project2
        relationshipService.move(context, r2, null, project2);

        context.restoreAuthSystemState();

        // Check relationship order for project1
        assertRightPlace(r1, 0);
        assertRightPlace(r3, 1);    // should move down as the first Relationship was removed
        assertRelationMetadataOrder(project1, isProjectOfPerson, List.of(r1, r3));

        // Check relationship order for project2
        assertRightPlace(r4, 0);     // previous Relationships should stay where they were
        assertRightPlace(r5, 1);
        assertRightPlace(r6, 2);
        assertRightPlace(r2, 3);     // moved Relationship should be appended to the end
        assertRelationMetadataOrder(project2, isProjectOfPerson, List.of(r4, r5, r6, r2));
    }

    @Test
    public void changeRightItemInNonUseForPlaceRelationshipInTheMiddleTest_ignoreOtherRels() throws Exception {
        context.turnOffAuthorisationSystem();

        // Add three Authors to project1, appending to the end
        Relationship r1 = relationshipService.create(context, author1, project1, isProjectOfPerson, -1, -1);
        Relationship r2 = relationshipService.create(context, author2, project1, isProjectOfPerson, -1, -1);

        // NOTE: unrelated relationship => should not be affected
        Relationship ur1 = relationshipService.create(context, publication1, project1, isProjectOfPublication, -1, -1);

        // NOTE: unrelated relationship => should not be affected
        Relationship ur2 = relationshipService.create(context, publication2, project1, isProjectOfPublication, -1, -1);

        Relationship r3 = relationshipService.create(context, author3, project1, isProjectOfPerson, -1, -1);

        // Add three Authors to project2, appending to the end
        Relationship r4 = relationshipService.create(context, author4, project2, isProjectOfPerson, -1, -1);
        Relationship r5 = relationshipService.create(context, author5, project2, isProjectOfPerson, -1, -1);

        // NOTE: unrelated relationship => should not be affected
        Relationship ur3 = relationshipService.create(context, author5, project3, isProjectOfPerson, -1, -1);

        Relationship r6 = relationshipService.create(context, author6, project2, isProjectOfPerson, -1, -1);

        // Move r2 to project2
        relationshipService.move(context, r2, null, project2);

        context.restoreAuthSystemState();

        // Check relationship order for project1
        assertRightPlace(r1, 0);
        assertRightPlace(r3, 1);    // should move down as the first Relationship was removed
        assertRelationMetadataOrder(project1, isProjectOfPerson, List.of(r1, r3));

        // Check relationship order for project2
        assertRightPlace(r4, 0);     // previous Relationships should stay where they were
        assertRightPlace(r5, 1);
        assertRightPlace(r6, 2);
        assertRightPlace(r2, 3);     // moved Relationship should be appended to the end
        assertRelationMetadataOrder(project2, isProjectOfPerson, List.of(r4, r5, r6, r2));

        // check unaffected relationships
        assertLeftPlace(ur1, 0);
        assertRightPlace(ur1, 0);
        assertLeftPlace(ur2, 0);
        assertRightPlace(ur2, 1);
        assertLeftPlace(ur3, 1);
        assertRightPlace(ur3, 0);
    }

    @Test
    public void changeRightItemInNonUseForPlaceRelationshipAtTheEndTest() throws Exception {
        context.turnOffAuthorisationSystem();

        // Add three Authors to project1, appending to the end
        Relationship r1 = relationshipService.create(context, author1, project1, isProjectOfPerson, -1, -1);
        Relationship r2 = relationshipService.create(context, author2, project1, isProjectOfPerson, -1, -1);
        Relationship r3 = relationshipService.create(context, author3, project1, isProjectOfPerson, -1, -1);

        // Add three Authors to project2, appending to the end
        Relationship r4 = relationshipService.create(context, author4, project2, isProjectOfPerson, -1, -1);
        Relationship r5 = relationshipService.create(context, author5, project2, isProjectOfPerson, -1, -1);
        Relationship r6 = relationshipService.create(context, author6, project2, isProjectOfPerson, -1, -1);

        // Move r3 to project2
        relationshipService.move(context, r3, null, project2);

        context.restoreAuthSystemState();

        // Check relationship order for author1
        assertRightPlace(r1, 0);
        assertRightPlace(r2, 1);
        assertRelationMetadataOrder(project1, isProjectOfPerson, List.of(r1, r2));

        // Check relationship order for author2
        assertRightPlace(r4, 0);     // previous Relationships should stay where they were
        assertRightPlace(r5, 1);
        assertRightPlace(r6, 2);
        assertRightPlace(r3, 3);     // moved Relationship should be appended to the end
        assertRelationMetadataOrder(project2, isProjectOfPerson, List.of(r4, r5, r6, r3));
    }

    @Test
    public void changeLeftItemInNonUseForPlaceRelationshipAtTheStartToSameItemNoChanges() throws Exception {
        context.turnOffAuthorisationSystem();

        // Add three Projects to author1, appending to the end
        Relationship r1 = relationshipService.create(context, author1, project1, isProjectOfPerson, -1, -1);
        Relationship r2 = relationshipService.create(context, author1, project2, isProjectOfPerson, -1, -1);
        Relationship r3 = relationshipService.create(context, author1, project3, isProjectOfPerson, -1, -1);

        // Move r1 to author1
        relationshipService.move(context, r1, author1, null);

        context.restoreAuthSystemState();

        // Check relationship order for author1 -> should remain unchanged
        assertLeftPlace(r1, 0);
        assertLeftPlace(r2, 1);
        assertLeftPlace(r3, 2);
        assertRelationMetadataOrder(author1, isProjectOfPerson, List.of(r1, r2, r3));
    }

    @Test
    public void changeRightItemInNonUseForPlaceRelationshipAtTheStartToSameItemNoChanges() throws Exception {
        context.turnOffAuthorisationSystem();

        // Add three Projects to author1, appending to the end
        Relationship r1 = relationshipService.create(context, author1, project1, isProjectOfPerson, -1, -1);
        Relationship r2 = relationshipService.create(context, author1, project2, isProjectOfPerson, -1, -1);
        Relationship r3 = relationshipService.create(context, author1, project3, isProjectOfPerson, -1, -1);

        // Move r1 to author1
        relationshipService.move(context, r1, null, project1);

        context.restoreAuthSystemState();

        // Check relationship order for author1 -> should remain unchanged
        assertLeftPlace(r1, 0);
        assertLeftPlace(r2, 1);
        assertLeftPlace(r3, 2);
        assertRelationMetadataOrder(author1, isProjectOfPerson, List.of(r1, r2, r3));
    }

    @Test
    public void changeLeftItemInNonUseForPlaceRelationshipAtTheStartWithSiblingsInOldLeftItem() throws Exception {
        context.turnOffAuthorisationSystem();

        // Add three Projects to author1, appending to the end
        Relationship r1 = relationshipService.create(context, author1, project1, isProjectOfPerson, -1, -1);
        Relationship r2 = relationshipService.create(context, author1, project2, isProjectOfPerson, -1, -1);
        Relationship r3 = relationshipService.create(context, author1, project3, isProjectOfPerson, -1, -1);

        // Add three Authors to project1, appending to the end
        Relationship r4 = relationshipService.create(context, author4, project1, isProjectOfPerson, -1, -1);
        Relationship r5 = relationshipService.create(context, author5, project1, isProjectOfPerson, -1, -1);
        Relationship r6 = relationshipService.create(context, author6, project1, isProjectOfPerson, -1, -1);

        // Move r1 to author2
        relationshipService.move(context, r1, author2, null);

        context.restoreAuthSystemState();

        // Check relationship order for author1 -> should shift down by one
        assertLeftPlace(r2, 0);
        assertLeftPlace(r3, 1);
        assertRelationMetadataOrder(author1, isProjectOfPerson, List.of(r2, r3));

        // Check relationship order for project 1 -> should remain unchanged
        assertRightPlace(r1, 0);
        assertRightPlace(r4, 1);
        assertRightPlace(r5, 2);
        assertRightPlace(r6, 3);
        assertRelationMetadataOrder(project1, isProjectOfPerson, List.of(r1, r4, r5, r6));
    }

    @Test
    public void changeRightItemInNonUseForPlaceRelationshipAtTheStartWithSiblingsInOldRightItem() throws Exception {
        context.turnOffAuthorisationSystem();

        // Add three Authors to project1, appending to the end
        Relationship r1 = relationshipService.create(context, author1, project1, isProjectOfPerson, -1, -1);
        Relationship r2 = relationshipService.create(context, author2, project1, isProjectOfPerson, -1, -1);
        Relationship r3 = relationshipService.create(context, author3, project1, isProjectOfPerson, -1, -1);

        // Add three Projects to author1, appending to the end
        Relationship r4 = relationshipService.create(context, author1, project4, isProjectOfPerson, -1, -1);
        Relationship r5 = relationshipService.create(context, author1, project5, isProjectOfPerson, -1, -1);
        Relationship r6 = relationshipService.create(context, author1, project6, isProjectOfPerson, -1, -1);

        // Move r1 to project2
        relationshipService.move(context, r1, null, project2);

        context.restoreAuthSystemState();

        // Check relationship order for project1 -> should remain unchanged
        assertRightPlace(r2, 0);
        assertRightPlace(r3, 1);
        assertRelationMetadataOrder(project1, isProjectOfPerson, List.of(r2, r3));

        // Check relationship order for author1 -> should remain unchanged
        assertLeftPlace(r1, 0);
        assertLeftPlace(r4, 1);
        assertLeftPlace(r5, 2);
        assertLeftPlace(r6, 3);
        assertRelationMetadataOrder(author1, isProjectOfPerson, List.of(r1, r4, r5, r6));
    }


    private void assertLeftPlace(Relationship relationship, int leftPlace) {
        assertEquals(leftPlace, relationship.getLeftPlace());
    }

    private void assertRightPlace(Relationship relationship, int rightPlace) {
        assertEquals(rightPlace, relationship.getRightPlace());
    }


    private void assertRelationMetadataOrder(
        Item item, RelationshipType relationshipType, List<Relationship> relationships
    ) {
        String element = getRelationshipTypeStringForEntity(relationshipType, item);
        List<MetadataValue> mdvs = itemService.getMetadata(
            item,
            MetadataSchemaEnum.RELATION.getName(), element, null,
            Item.ANY
        );

        assertEquals(
            "Metadata authorities should match relationship IDs",
            relationships.stream()
                         .map(r -> {
                             if (r != null) {
                                 return Constants.VIRTUAL_AUTHORITY_PREFIX + r.getID();
                             } else {
                                 return null;   // To match relationships that have been deleted and copied to MDVs
                             }
                         })
                         .collect(Collectors.toList()),
            mdvs.stream()
                .map(MetadataValue::getAuthority)
                .collect(Collectors.toList())
        );
    }

    private void assertMetadataOrder(
        Item item, String metadataField, List<String> metadataValues
    ) {
        List<MetadataValue> mdvs = itemService.getMetadataByMetadataString(item, metadataField);

        assertEquals(
            metadataValues,
            mdvs.stream()
                .map(MetadataValue::getValue)
                .collect(Collectors.toList())
        );
    }

    private String getRelationshipTypeStringForEntity(RelationshipType relationshipType, Item item) {
        String entityType = itemService.getEntityTypeLabel(item);

        if (StringUtils.equals(entityType, relationshipType.getLeftType().getLabel())) {
            return relationshipType.getLeftwardType();
        } else if (StringUtils.equals(entityType, relationshipType.getRightType().getLabel())) {
            return relationshipType.getRightwardType();
        } else {
            throw new IllegalArgumentException(
                entityType + "is not a valid entity for " + relationshipType.getLeftwardType() + ", must be either "
                    + relationshipType.getLeftType().getLabel() + " or " + relationshipType.getRightType().getLabel()
            );
        }
    }
}
