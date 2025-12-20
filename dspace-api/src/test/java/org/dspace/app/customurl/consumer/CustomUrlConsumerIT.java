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

import java.sql.SQLException;

import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.builder.WorkspaceItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
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

    private Collection collection;

    @Before
    public void setup() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();

        collection = CollectionBuilder.createCollection(context, parentCommunity)
                                      .withName("Collection 1")
                                      .withEntityType("Person")
                                      .build();

        configurationService.setProperty("dspace.custom-url.consumer.supported-entities", "Person");
        configurationService.setProperty("dspace.custom-url.consumer.entity-metadata-mapping.Person",
                                         "person.familyName;person.givenName");

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
                                         "person.familyName;person.givenName");
        configurationService.setProperty("dspace.custom-url.consumer.entity-metadata-mapping.Publication",
                                         "dc.title");

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
                                         "person.familyName;person.givenName");
        configurationService.setProperty("dspace.custom-url.consumer.entity-metadata-mapping.Publication",
                                         "dc.contributor.author");

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
                                         "person.familyName;person.givenName");
        configurationService.setProperty("dspace.custom-url.consumer.entity-metadata-mapping.OrgUnit",
                                         "dc.title");

        Item item = ItemBuilder.createItem(context, orgUnits)
                               .withTitle("Test Org Unit")
                               .build();

        context.restoreAuthSystemState();
        context.commit();

        item = context.reloadEntity(item);

        assertThat(item.getMetadata(), hasItem(with("dspace.customurl", "test-org-unit")));

    }

    @Test
    public void testCustomUrlAdditionWithMixedMetadataFields() throws SQLException {

        context.turnOffAuthorisationSystem();

        // Create a collection for an entity type that uses both qualified and unqualified metadata fields
        Collection publications = CollectionBuilder.createCollection(context, parentCommunity)
                                                   .withName("Collection 5")
                                                   .withEntityType("Publication")
                                                   .build();

        // Update configuration to use mixed metadata fields
        configurationService.setProperty("dspace.custom-url.consumer.supported-entities", "Person,Publication");
        configurationService.setProperty("dspace.custom-url.consumer.entity-metadata-mapping.Person",
                                         "person.familyName;person.givenName");
        configurationService.setProperty("dspace.custom-url.consumer.entity-metadata-mapping.Publication",
                                         "dc.title;dc.contributor.author");

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
}
