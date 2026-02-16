/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.customurl.consumer;

import static org.dspace.app.matcher.MetadataValueMatcher.with;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.sql.SQLException;
import java.util.Optional;

import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.app.customurl.CustomUrlService;
import org.dspace.authorize.AuthorizeException;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.builder.VersionBuilder;
import org.dspace.builder.WorkspaceItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.InstallItemService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.utils.DSpace;
import org.dspace.versioning.Version;
import org.junit.Before;
import org.junit.Test;

/**
 * Integration tests for {@link CustomUrlConsumer}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class CustomUrlConsumerIT extends AbstractIntegrationTestWithDatabase {

    private final ConfigurationService configurationService = DSpaceServicesFactory.getInstance()
                                                                                   .getConfigurationService();

    private final ItemService itemService = ContentServiceFactory.getInstance().getItemService();

    private final InstallItemService installItemService = ContentServiceFactory.getInstance().getInstallItemService();

    private final WorkspaceItemService workspaceItemService =
        ContentServiceFactory.getInstance().getWorkspaceItemService();

    private CustomUrlConsumerConfig customUrlConsumerConfig = new DSpace()
        .getSingletonService(CustomUrlConsumerConfig.class);

    private final CustomUrlService customUrlService = new DSpace().getSingletonService(CustomUrlService.class);


    private Collection collection;


    @Before
    public void setup() throws Exception {

        configurationService.setProperty("dspace.custom-url.consumer.supported-entities", "Person");
        configurationService.setProperty("dspace.custom-url.consumer.entity-metadata-mapping.Person",
                                         "person.familyName,person.givenName");
        customUrlConsumerConfig.reload();

        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();


        collection = CollectionBuilder.createCollection(context, parentCommunity)
                                      .withName("Collection 1")
                                      .withEntityType("Person")
                                      .build();

        context.restoreAuthSystemState();
    }

    @Test
    public void testCustomUrlAddition() throws SQLException {

        context.turnOffAuthorisationSystem();

        Item item = ItemBuilder.createItem(context, collection)
                               .withMetadata("person", "familyName", null, "Giamminonni")
                               .withMetadata("person", "givenName", null, "Lucà $£ %!/")
                               .build();

        context.restoreAuthSystemState();
        context.commit();

        item = context.reloadEntity(item);

        assertThat(item.getMetadata(), hasItem(with("dspace.customurl", "giamminonni-luca")));

    }

    @Test
    public void testNoCustomUrlAdditionOccursOnWorkspaceItem() throws SQLException {

        context.turnOffAuthorisationSystem();

        // For Person entity type, test workspace item with title (workspace items don't get custom URLs)
        WorkspaceItem workspaceItem = WorkspaceItemBuilder.createWorkspaceItem(context, collection)
                                                          .withTitle("Giamminonni, Luca")
                                                          .build();

        context.restoreAuthSystemState();
        context.commit();

        Item item = context.reloadEntity(workspaceItem.getItem());

        assertThat(itemService.getMetadataByMetadataString(item, "dspace.customurl"), empty());

    }

    @Test
    public void testNoCustomUrlAdditionOccursOnItemWithAlreadyAnUrl() throws SQLException {

        context.turnOffAuthorisationSystem();

        // For Person entity type, use person metadata but with existing custom URL
        Item item = ItemBuilder.createItem(context, collection)
                               .withMetadata("person", "familyName", null, "Giamminonni")
                               .withMetadata("person", "givenName", null, "Luca")
                               .withCustomUrl("my-custom-url")
                               .build();

        context.restoreAuthSystemState();
        context.commit();

        item = context.reloadEntity(item);

        assertThat(itemService.getMetadataByMetadataString(item, "dspace.customurl"), hasSize(1));
        assertThat(item.getMetadata(), hasItem(with("dspace.customurl", "my-custom-url")));

    }

    @Test
    public void testNoCustomUrlAdditionOccursOnItemWithoutTitle() throws SQLException {

        context.turnOffAuthorisationSystem();

        // For Person entity type, we need to test without person metadata fields
        Item item = ItemBuilder.createItem(context, collection)
                               .build();

        context.restoreAuthSystemState();
        context.commit();

        item = context.reloadEntity(item);

        assertThat(itemService.getMetadataByMetadataString(item, "dspace.customurl"), empty());

    }

    @Test
    public void testNoCustomUrlAdditionOccursOnItemWithTitleContainingOnlyInvalidCharacters() throws SQLException {

        context.turnOffAuthorisationSystem();

        // For Person entity type, test with invalid person metadata
        Item item = ItemBuilder.createItem(context, collection)
                               .withMetadata("person", "familyName", null, "$$$$$")
                               .build();

        context.restoreAuthSystemState();
        context.commit();

        item = context.reloadEntity(item);

        assertThat(itemService.getMetadataByMetadataString(item, "dspace.customurl"), empty());

    }

    @Test
    public void testNoCustomUrlAdditionOccursOnNotSupportedEntities() throws SQLException {

        context.turnOffAuthorisationSystem();

        Collection publications = CollectionBuilder.createCollection(context, parentCommunity)
                                                   .withName("Collection 2")
                                                   .withEntityType("Publication")
                                                   .build();

        Item item = ItemBuilder.createItem(context, publications)
                               .withTitle("Test publication")
                               .build();

        context.restoreAuthSystemState();
        context.commit();

        item = context.reloadEntity(item);

        assertThat(itemService.getMetadataByMetadataString(item, "dspace.customurl"), empty());

    }

    @Test
    public void testNoCustomUrlAdditionOccursOnItemWithoutEntityType() throws SQLException {

        context.turnOffAuthorisationSystem();

        Collection publications = CollectionBuilder.createCollection(context, parentCommunity)
                                                   .withName("Collection 2")
                                                   .build();

        Item item = ItemBuilder.createItem(context, publications)
                               .withTitle("Giamminonni, Luca")
                               .build();

        context.restoreAuthSystemState();
        context.commit();

        item = context.reloadEntity(item);

        assertThat(itemService.getMetadataByMetadataString(item, "dspace.customurl"), empty());

    }

    @Test
    public void testCustomUrlAdditionWithDefaultMetadataFields() throws SQLException {

        context.turnOffAuthorisationSystem();

        // Create a collection with a different entity type that uses default dc.title
        Collection publications = CollectionBuilder.createCollection(context, parentCommunity)
                                                   .withName("Collection 2")
                                                   .withEntityType("Publication")
                                                   .build();

        // Update configuration to support Publication entity type
        configurationService.setProperty("dspace.custom-url.consumer.supported-entities", "Person,Publication");
        configurationService.setProperty("dspace.custom-url.consumer.entity-metadata-mapping.Person",
                                         "person.familyName,person.givenName");
        configurationService.setProperty("dspace.custom-url.consumer.entity-metadata-mapping.Publication",
                                         "dc.title");
        customUrlConsumerConfig.reload();

        Item item = ItemBuilder.createItem(context, publications)
                               .withTitle("Test Publication Title")
                               .build();

        context.restoreAuthSystemState();
        context.commit();

        item = context.reloadEntity(item);

        assertThat(item.getMetadata(), hasItem(with("dspace.customurl", "test-publication-title")));

    }

    @Test
    public void testCustomUrlAdditionWithQualifiedMetadataFields() throws SQLException {

        context.turnOffAuthorisationSystem();

        // Create a collection for an entity type that uses qualified metadata fields
        Collection publications = CollectionBuilder.createCollection(context, parentCommunity)
                                                   .withName("Collection 4")
                                                   .withEntityType("Publication")
                                                   .build();

        // Update configuration to use qualified metadata field (dc.contributor.author)
        configurationService.setProperty("dspace.custom-url.consumer.supported-entities", "Person,Publication");
        configurationService.setProperty("dspace.custom-url.consumer.entity-metadata-mapping.Person",
                                         "person.familyName,person.givenName");
        configurationService.setProperty("dspace.custom-url.consumer.entity-metadata-mapping.Publication",
                                         "dc.contributor.author");
        customUrlConsumerConfig.reload();

        Item item = ItemBuilder.createItem(context, publications)
                               .withAuthor("John Doe")
                               .build();

        context.restoreAuthSystemState();
        context.commit();

        item = context.reloadEntity(item);

        assertThat(item.getMetadata(), hasItem(with("dspace.customurl", "john-doe")));

    }

    @Test
    public void testCustomUrlAdditionWithWildcardMapping() throws SQLException {

        context.turnOffAuthorisationSystem();

        // Create a collection with an entity type not explicitly mapped
        Collection orgUnits = CollectionBuilder.createCollection(context, parentCommunity)
                                               .withName("Collection 3")
                                               .withEntityType("OrgUnit")
                                               .build();

        // Update configuration to support OrgUnit
        configurationService.setProperty("dspace.custom-url.consumer.supported-entities", "Person,OrgUnit");
        configurationService.setProperty("dspace.custom-url.consumer.entity-metadata-mapping.Person",
                                         "person.familyName,person.givenName");
        configurationService.setProperty("dspace.custom-url.consumer.entity-metadata-mapping.OrgUnit",
                                         "dc.title");
        customUrlConsumerConfig.reload();

        Item item = ItemBuilder.createItem(context, orgUnits)
                               .withTitle("Test Org Unit")
                               .build();

        context.restoreAuthSystemState();
        context.commit();

        item = context.reloadEntity(item);

        assertThat(item.getMetadata(), hasItem(with("dspace.customurl", "test-org-unit")));

    }

    @Test
    public void testCustomUrlAdditionWithMixedMetadataFields() throws Exception {

        context.turnOffAuthorisationSystem();

        // Update configuration to use mixed metadata fields
        configurationService.setProperty("dspace.custom-url.consumer.supported-entities", "Person,Publication");
        configurationService.setProperty("dspace.custom-url.consumer.entity-metadata-mapping.Person",
                                         "person.familyName,person.givenName");
        configurationService.setProperty("dspace.custom-url.consumer.entity-metadata-mapping.Publication",
                                         "dc.title,dc.contributor.author");
        customUrlConsumerConfig.reload();

        // Create a collection for an entity type that uses both qualified and unqualified metadata fields
        Collection publications = CollectionBuilder.createCollection(context, parentCommunity)
                                                   .withName("Collection 5")
                                                   .withEntityType("Publication")
                                                   .build();

        Item item = ItemBuilder.createItem(context, publications)
                               .withTitle("Test Publication")
                               .withAuthor("Jane Smith")
                               .build();

        context.restoreAuthSystemState();
        context.commit();

        item = context.reloadEntity(item);

        // Should combine both title and author: "test publication jane smith"
        assertThat(item.getMetadata(), hasItem(with("dspace.customurl", "test-publication-jane-smith")));

    }

    @Test
    public void testCustomUrlAdditionWithProgressiveNumbering() throws SQLException {

        context.turnOffAuthorisationSystem();

        // Create a collection for testing progressive numbering
        Collection publications = CollectionBuilder.createCollection(context, parentCommunity)
                                                   .withName("Collection 6")
                                                   .withEntityType("Publication")
                                                   .build();

        // Update configuration to support Publication entity type
        configurationService.setProperty("dspace.custom-url.consumer.supported-entities", "Publication");
        configurationService.setProperty("dspace.custom-url.consumer.entity-metadata-mapping.Publication",
                                         "dc.title");
        customUrlConsumerConfig.reload();

        // Create first item with title "Test Publication"
        Item item1 = ItemBuilder.createItem(context, publications)
                                .withTitle("Test Publication")
                                .build();

        // Create second item with the same title - should get progressive URL
        Item item2 = ItemBuilder.createItem(context, publications)
                                .withTitle("Test Publication")
                                .build();

        // Create third item with the same title - should get next progressive URL
        Item item3 = ItemBuilder.createItem(context, publications)
                                .withTitle("Test Publication")
                                .build();

        context.restoreAuthSystemState();
        context.commit();

        item1 = context.reloadEntity(item1);
        item2 = context.reloadEntity(item2);
        item3 = context.reloadEntity(item3);

        // First item should get the base URL
        assertThat(item1.getMetadata(), hasItem(with("dspace.customurl", "test-publication")));
        // Second item should get progressive URL "test-publication-1"
        assertThat(item2.getMetadata(), hasItem(with("dspace.customurl", "test-publication-1")));
        // Third item should get progressive URL "test-publication-2"
        assertThat(item3.getMetadata(), hasItem(with("dspace.customurl", "test-publication-2")));

    }

    @Test
    public void testDuplicateCustomUrlCreationAcrossDifferentEntityTypes() throws SQLException {

        context.turnOffAuthorisationSystem();

        // Create collections for different entity types
        Collection publications = CollectionBuilder.createCollection(context, parentCommunity)
                                                   .withName("Publications Collection")
                                                   .withEntityType("Publication")
                                                   .build();

        Collection orgUnits = CollectionBuilder.createCollection(context, parentCommunity)
                                               .withName("OrgUnits Collection")
                                               .withEntityType("OrgUnit")
                                               .build();

        // Configure both entity types to use dc.title, which will create the same custom URL
        configurationService.setProperty("dspace.custom-url.consumer.supported-entities", "Publication,OrgUnit");
        configurationService.setProperty("dspace.custom-url.consumer.entity-metadata-mapping.Publication", "dc.title");
        configurationService.setProperty("dspace.custom-url.consumer.entity-metadata-mapping.OrgUnit", "dc.title");
        customUrlConsumerConfig.reload();

        // Create first item (Publication) with a specific title
        Item publication = ItemBuilder.createItem(context, publications)
                                      .withTitle("Machine Learning Research")
                                      .build();

        // Create second item (OrgUnit) with the SAME title
        Item orgUnit = ItemBuilder.createItem(context, orgUnits)
                                  .withTitle("Machine Learning Research")
                                  .build();

        context.restoreAuthSystemState();
        context.commit();

        publication = context.reloadEntity(publication);
        orgUnit = context.reloadEntity(orgUnit);

        // Verify both items got the SAME custom URL
        assertThat(publication.getMetadata(), hasItem(with("dspace.customurl", "machine-learning-research")));
        assertThat(orgUnit.getMetadata(), hasItem(with("dspace.customurl", "machine-learning-research-1")));
    }

    @Test
    public void testDuplicateCustomUrlCreationWithItemVersioning() throws SQLException, AuthorizeException {

        context.turnOffAuthorisationSystem();

        // Create a publication collection
        Collection publications = CollectionBuilder.createCollection(context, parentCommunity)
                                                   .withName("Publications Collection")
                                                   .withEntityType("Publication")
                                                   .build();

        // Configure Publication entity type to use dc.title
        configurationService.setProperty("dspace.custom-url.consumer.supported-entities", "Publication");
        configurationService.setProperty("dspace.custom-url.consumer.entity-metadata-mapping.Publication", "dc.title");
        customUrlConsumerConfig.reload();

        // Create original item
        Item originalItem = ItemBuilder.createItem(context, publications)
                                       .withTitle("Advanced Algorithms Study")
                                       .build();

        // Create a new version using VersionBuilder (like VersionRestRepositoryIT)
        Version newVersion = VersionBuilder.createVersion(context, originalItem, "Second version").build();
        Item versionedItem = newVersion.getItem();

        // The versioned item is initially a workspace item, we need to archive it to trigger consumers
        // This mirrors what happens in VersionRestRepositoryIT.createNewVersion()
        WorkspaceItem workspaceItem = workspaceItemService.findByItem(context, versionedItem);
        if (workspaceItem != null) {
            installItemService.installItem(context, workspaceItem);
        }

        context.restoreAuthSystemState();
        context.commit();

        // The versioned item will have the same title, so it should get the same custom URL
        versionedItem = context.reloadEntity(versionedItem);

        // Verify the versioned item got the same custom URL
        assertThat(versionedItem.getMetadata(), hasItem(with("dspace.customurl", "advanced-algorithms-study")));
        // Verify the original item got the old custom URL
        assertThat(originalItem.getMetadata(), hasItem(with("dspace.customurl.old", "advanced-algorithms-study")));
        // Verify the original item does not contain the custom URL
        assertThat(itemService.getMetadataByMetadataString(originalItem, "dspace.customurl"), empty());

    }

    @Test
    public void testDuplicateCustomUrlCreationWithDifferentMetadataFieldsButSameResult() throws SQLException {

        context.turnOffAuthorisationSystem();

        // Create collections for different entity types
        Collection publications = CollectionBuilder.createCollection(context, parentCommunity)
                                                   .withName("Publications Collection")
                                                   .withEntityType("Publication")
                                                   .build();

        Collection persons = CollectionBuilder.createCollection(context, parentCommunity)
                                              .withName("Persons Collection")
                                              .withEntityType("Person")
                                              .build();

        // Configure entity types with different metadata fields that can produce the same custom URL
        configurationService.setProperty("dspace.custom-url.consumer.supported-entities", "Publication,Person");
        configurationService.setProperty("dspace.custom-url.consumer.entity-metadata-mapping.Publication", "dc.title");
        configurationService.setProperty("dspace.custom-url.consumer.entity-metadata-mapping.Person",
                                         "person.familyName,person.givenName");
        customUrlConsumerConfig.reload();

        // Create a publication with title "Smith John"
        Item publication = ItemBuilder.createItem(context, publications)
                                      .withTitle("Smith John")
                                      .build();

        // Create a person with family name "Smith" and given name "John"
        // This will concatenate to "Smith John" -> "smith-john" (same URL!)
        Item person = ItemBuilder.createItem(context, persons)
                                 .withMetadata("person", "familyName", null, "Smith")
                                 .withMetadata("person", "givenName", null, "John")
                                 .build();

        context.restoreAuthSystemState();
        context.commit();

        publication = context.reloadEntity(publication);
        person = context.reloadEntity(person);

        // Verify both items got the SAME custom URL
        assertThat(publication.getMetadata(), hasItem(with("dspace.customurl", "smith-john")));
        assertThat(person.getMetadata(), hasItem(with("dspace.customurl", "smith-john-1")));
    }

    @Test
    public void testMultipleDuplicateCustomUrlsShowProblemSeverity() throws SQLException {

        context.turnOffAuthorisationSystem();

        // Create a publication collection
        Collection publications = CollectionBuilder.createCollection(context, parentCommunity)
                                                   .withName("Publications Collection")
                                                   .withEntityType("Publication")
                                                   .build();

        // Configure Publication entity type to use dc.title
        configurationService.setProperty("dspace.custom-url.consumer.supported-entities", "Publication");
        configurationService.setProperty("dspace.custom-url.consumer.entity-metadata-mapping.Publication", "dc.title");
        customUrlConsumerConfig.reload();

        // Create multiple items with the same title to show the problem gets worse
        Item item1 = ItemBuilder.createItem(context, publications)
                                .withTitle("Data Mining Techniques")
                                .build();

        Item item2 = ItemBuilder.createItem(context, publications)
                                .withTitle("Data Mining Techniques")  // Same title
                                .build();

        Item item3 = ItemBuilder.createItem(context, publications)
                                .withTitle("Data Mining Techniques")  // Same title again
                                .build();

        context.restoreAuthSystemState();
        context.commit();

        item1 = context.reloadEntity(item1);
        item2 = context.reloadEntity(item2);
        item3 = context.reloadEntity(item3);

        // Verify all items have the SAME custom URL
        assertThat(item1.getMetadata(), hasItem(with("dspace.customurl", "data-mining-techniques")));
        assertThat(item2.getMetadata(), hasItem(with("dspace.customurl", "data-mining-techniques-1")));
        assertThat(item3.getMetadata(), hasItem(with("dspace.customurl", "data-mining-techniques-2")));
    }

    @Test
    public void testItemVersioningCausesCustomUrlSearch() throws SQLException, AuthorizeException {

        context.turnOffAuthorisationSystem();

        // Create a publication collection
        Collection publications = CollectionBuilder.createCollection(context, parentCommunity)
                                                   .withName("Publications Collection")
                                                   .withEntityType("Publication")
                                                   .build();

        // Configure Publication entity type to use dc.title
        configurationService.setProperty("dspace.custom-url.consumer.supported-entities", "Publication");
        configurationService.setProperty("dspace.custom-url.consumer.entity-metadata-mapping.Publication", "dc.title");
        customUrlConsumerConfig.reload();

        // Create original item with custom URL
        Item originalItem = ItemBuilder.createItem(context, publications)
                                       .withTitle("Versioned Publication")
                                       .build();

        originalItem = context.reloadEntity(originalItem);

        // Verify original item has the custom URL
        assertThat(originalItem.getMetadata(), hasItem(with("dspace.customurl", "versioned-publication")));
        String customUrl = "versioned-publication";

        // Verify we can find the item by custom URL before versioning
        Optional<Item> foundBeforeVersioning = customUrlService.findItemByCustomUrl(context, customUrl);
        assertTrue("Should find item by custom URL before versioning", foundBeforeVersioning.isPresent());
        assertEquals("Found item should be the original item", originalItem.getID(),
                     foundBeforeVersioning.get().getID());

        // Now create a new version
        Version newVersion = VersionBuilder.createVersion(context, originalItem, "Second version").build();
        Item versionedItem = newVersion.getItem();

        // The versioned item is initially a workspace item, we need to archive it to trigger consumers
        WorkspaceItem workspaceItem = workspaceItemService.findByItem(context, versionedItem);
        if (workspaceItem != null) {
            installItemService.installItem(context, workspaceItem);
        }

        context.restoreAuthSystemState();
        context.commit();

        // Reload both items to get latest state
        originalItem = context.reloadEntity(originalItem);
        versionedItem = context.reloadEntity(versionedItem);

        // Verify the versioned item has the same custom URL
        assertThat(versionedItem.getMetadata(), hasItem(with("dspace.customurl", "versioned-publication")));
        // Verify the original item's custom URL was moved to old
        assertThat(originalItem.getMetadata(), hasItem(with("dspace.customurl.old", "versioned-publication")));
        assertThat(itemService.getMetadataByMetadataString(originalItem, "dspace.customurl"), empty());

        assertEquals(versionedItem.getID(),
                     customUrlService.findItemByCustomUrl(context, customUrl).map(DSpaceObject::getID).orElse(null));
    }
}
