/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.service;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.sql.SQLException;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;
import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.authorize.AuthorizeException;
import org.dspace.builder.EntityTypeBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.builder.RelationshipBuilder;
import org.dspace.builder.RelationshipTypeBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.EntityType;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.Relationship;
import org.dspace.content.RelationshipType;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.versioning.Version;
import org.dspace.versioning.factory.VersionServiceFactory;
import org.dspace.versioning.service.VersioningService;
import org.junit.Before;
import org.junit.Test;

public class ItemServiceTest extends AbstractIntegrationTestWithDatabase {
    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(ItemServiceTest.class);

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
    protected VersioningService versioningService = VersionServiceFactory.getInstance().getVersionService();

    Community community;
    Collection col;

    Item item;

    String authorQualifier = "author";
    String contributorElement = "contributor";
    String dcSchema = "dc";

    /**
     * This method will be run before every test as per @Before. It will
     * initialize resources required for the tests.
     */
    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        try {
            context.turnOffAuthorisationSystem();
            community = communityService.create(null, context);

            col = collectionService.create(context, community);
            WorkspaceItem is = workspaceItemService.create(context, col, false);
            WorkspaceItem authorIs = workspaceItemService.create(context, col, false);

            item = installItemService.installItem(context, is);
            itemService.addMetadata(context, item, "dspace", "entity", "type", null, "Publication");

            context.restoreAuthSystemState();
        } catch (AuthorizeException ex) {
            log.error("Authorization Error in init", ex);
            fail("Authorization Error in init: " + ex.getMessage());
        } catch (SQLException ex) {
            log.error("SQL Error in init", ex);
            fail("SQL Error in init: " + ex.getMessage());
        }
    }

    @Test
    public void InsertAndMoveMetadataShiftPlaceTest() throws Exception {
        context.turnOffAuthorisationSystem();

        // Here we add the first set of metadata to the item
        itemService.addMetadata(context, item, dcSchema, contributorElement, authorQualifier, null, "test, one");
        itemService.addMetadata(context, item, dcSchema, contributorElement, authorQualifier, null, "test, two");
        itemService.addMetadata(context, item, dcSchema, contributorElement, authorQualifier, null, "test, three");

        context.restoreAuthSystemState();

        // The code below performs the mentioned assertions to ensure the place is correct
        List<MetadataValue> list = itemService
            .getMetadata(item, dcSchema, contributorElement, authorQualifier, Item.ANY);
        assertThat(list.size(), equalTo(3));

        assertMetadataValue(authorQualifier, contributorElement, dcSchema, "test, one", null, 0, list.get(0));
        assertMetadataValue(authorQualifier, contributorElement, dcSchema, "test, two", null, 1, list.get(1));
        assertMetadataValue(authorQualifier, contributorElement, dcSchema, "test, three", null, 2, list.get(2));

        context.turnOffAuthorisationSystem();

        // This is where we add metadata at place=1
        itemService.addAndShiftRightMetadata(
            context, item, dcSchema, contributorElement, authorQualifier, null, "test, four", null, -1, 1
        );

        // Here we retrieve the list of metadata again to perform the assertions on the places below as mentioned
        list = itemService.getMetadata(item, dcSchema, contributorElement, authorQualifier, Item.ANY)
                          .stream()
                          .sorted(Comparator.comparingInt(MetadataValue::getPlace))
                          .collect(Collectors.toList());
        assertThat(list.size(), equalTo(4));
        assertMetadataValue(authorQualifier, contributorElement, dcSchema, "test, one", null, 0, list.get(0));
        assertMetadataValue(authorQualifier, contributorElement, dcSchema, "test, four", null, 1, list.get(1));
        assertMetadataValue(authorQualifier, contributorElement, dcSchema, "test, two", null, 2, list.get(2));
        assertMetadataValue(authorQualifier, contributorElement, dcSchema, "test, three", null, 3, list.get(3));

        // And move metadata from place=2 to place=0
        itemService.moveMetadata(context, item, dcSchema, contributorElement, authorQualifier, 2, 0);

        context.restoreAuthSystemState();

        // Here we retrieve the list of metadata again to perform the assertions on the places below as mentioned
        list = itemService.getMetadata(item, dcSchema, contributorElement, authorQualifier, Item.ANY)
                          .stream()
                          .sorted(Comparator.comparingInt(MetadataValue::getPlace))
                          .collect(Collectors.toList());
        assertThat(list.size(), equalTo(4));
        assertMetadataValue(authorQualifier, contributorElement, dcSchema, "test, two", null, 0, list.get(0));
        assertMetadataValue(authorQualifier, contributorElement, dcSchema, "test, one", null, 1, list.get(1));
        assertMetadataValue(authorQualifier, contributorElement, dcSchema, "test, four", null, 2, list.get(2));
        assertMetadataValue(authorQualifier, contributorElement, dcSchema, "test, three", null, 3, list.get(3));
    }

    @Test
    public void InsertAndMoveMetadataOnePlaceForwardTest() throws Exception {
        context.turnOffAuthorisationSystem();

        // Here we add the first set of metadata to the item
        itemService.addMetadata(context, item, dcSchema, contributorElement, authorQualifier, null, "test, one");
        itemService.addMetadata(context, item, dcSchema, contributorElement, authorQualifier, null, "test, two");
        itemService.addMetadata(context, item, dcSchema, contributorElement, authorQualifier, null, "test, three");

        context.restoreAuthSystemState();

        // The code below performs the mentioned assertions to ensure the place is correct
        List<MetadataValue> list = itemService
            .getMetadata(item, dcSchema, contributorElement, authorQualifier, Item.ANY);
        assertThat(list.size(), equalTo(3));

        assertMetadataValue(authorQualifier, contributorElement, dcSchema, "test, one", null, 0, list.get(0));
        assertMetadataValue(authorQualifier, contributorElement, dcSchema, "test, two", null, 1, list.get(1));
        assertMetadataValue(authorQualifier, contributorElement, dcSchema, "test, three", null, 2, list.get(2));

        context.turnOffAuthorisationSystem();

        // This is where we add metadata at place=1
        itemService.addAndShiftRightMetadata(
            context, item, dcSchema, contributorElement, authorQualifier, null, "test, four", null, -1, 1
        );

        // Here we retrieve the list of metadata again to perform the assertions on the places below as mentioned
        list = itemService.getMetadata(item, dcSchema, contributorElement, authorQualifier, Item.ANY)
                          .stream()
                          .sorted(Comparator.comparingInt(MetadataValue::getPlace))
                          .collect(Collectors.toList());
        assertThat(list.size(), equalTo(4));
        assertMetadataValue(authorQualifier, contributorElement, dcSchema, "test, one", null, 0, list.get(0));
        assertMetadataValue(authorQualifier, contributorElement, dcSchema, "test, four", null, 1, list.get(1));
        assertMetadataValue(authorQualifier, contributorElement, dcSchema, "test, two", null, 2, list.get(2));
        assertMetadataValue(authorQualifier, contributorElement, dcSchema, "test, three", null, 3, list.get(3));

        // And move metadata from place=1 to place=2
        itemService.moveMetadata(context, item, dcSchema, contributorElement, authorQualifier, 1, 2);

        context.restoreAuthSystemState();

        // Here we retrieve the list of metadata again to perform the assertions on the places below as mentioned
        list = itemService.getMetadata(item, dcSchema, contributorElement, authorQualifier, Item.ANY)
                          .stream()
                          .sorted(Comparator.comparingInt(MetadataValue::getPlace))
                          .collect(Collectors.toList());
        assertThat(list.size(), equalTo(4));
        assertMetadataValue(authorQualifier, contributorElement, dcSchema, "test, one", null, 0, list.get(0));
        assertMetadataValue(authorQualifier, contributorElement, dcSchema, "test, two", null, 1, list.get(1));
        assertMetadataValue(authorQualifier, contributorElement, dcSchema, "test, four", null, 2, list.get(2));
        assertMetadataValue(authorQualifier, contributorElement, dcSchema, "test, three", null, 3, list.get(3));
    }

    @Test
    public void testDeleteItemWithMultipleVersions() throws Exception {
        context.turnOffAuthorisationSystem();

        EntityType publicationEntityType = EntityTypeBuilder.createEntityTypeBuilder(context, "Publication")
            .build();

        EntityType personEntityType = EntityTypeBuilder.createEntityTypeBuilder(context, "Person")
            .build();

        RelationshipType isAuthorOfPublication = RelationshipTypeBuilder.createRelationshipTypeBuilder(
                context, publicationEntityType, personEntityType, "isAuthorOfPublication", "isPublicationOfAuthor",
                null, null, null, null
            )
            .withCopyToLeft(false)
            .withCopyToRight(false)
            .build();

        Item publication1 = ItemBuilder.createItem(context, col)
            .withTitle("publication 1")
            .withEntityType("Publication")
            .build();

        Item person1 = ItemBuilder.createItem(context, col)
            .withTitle("person 2")
            .withEntityType("Person")
            .build();

        RelationshipBuilder.createRelationshipBuilder(context, publication1, person1, isAuthorOfPublication);

        // create a new version, which results in a non-latest relationship attached person 1.
        Version newVersion = versioningService.createNewVersion(context, publication1);
        Item newPublication1 = newVersion.getItem();
        WorkspaceItem newPublication1WSI = workspaceItemService.findByItem(context, newPublication1);
        installItemService.installItem(context, newPublication1WSI);
        context.dispatchEvents();

        // verify person1 has a non-latest relationship, which should also be removed
        List<Relationship> relationships1 = relationshipService.findByItem(context, person1, -1, -1, false, true);
        assertEquals(1, relationships1.size());
        List<Relationship> relationships2 = relationshipService.findByItem(context, person1, -1, -1, false, false);
        assertEquals(2, relationships2.size());

        itemService.delete(context, person1);

        context.restoreAuthSystemState();
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
