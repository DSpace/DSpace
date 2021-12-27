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

        configurationService.setProperty("cris.custom-url.consumer.supported-entities", "Person");

        context.restoreAuthSystemState();
    }

    @Test
    public void testCustomUrlAddition() throws SQLException {

        context.turnOffAuthorisationSystem();

        Item item = ItemBuilder.createItem(context, collection)
                               .withTitle("Giamminonni, Lucà $£ %!/")
                               .build();

        context.restoreAuthSystemState();
        context.commit();

        item = context.reloadEntity(item);

        assertThat(item.getMetadata(), hasItem(with("cris.customurl", "giamminonni-luca")));

    }

    @Test
    public void testNoCustomUrlAdditionOccursOnWorkspaceItem() throws SQLException {

        context.turnOffAuthorisationSystem();

        WorkspaceItem workspaceItem = WorkspaceItemBuilder.createWorkspaceItem(context, collection)
                                                          .withTitle("Giamminonni, Luca")
                                                          .build();

        context.restoreAuthSystemState();
        context.commit();

        Item item = context.reloadEntity(workspaceItem.getItem());

        assertThat(itemService.getMetadataByMetadataString(item, "cris.customurl"), empty());

    }

    @Test
    public void testNoCustomUrlAdditionOccursOnItemWithAlreadyAnUrl() throws SQLException {

        context.turnOffAuthorisationSystem();

        Item item = ItemBuilder.createItem(context, collection)
                               .withTitle("Giamminonni, Luca")
                               .withCustomUrl("my-custom-url")
                               .build();

        context.restoreAuthSystemState();
        context.commit();

        item = context.reloadEntity(item);

        assertThat(itemService.getMetadataByMetadataString(item, "cris.customurl"), hasSize(1));
        assertThat(item.getMetadata(), hasItem(with("cris.customurl", "my-custom-url")));

    }

    @Test
    public void testNoCustomUrlAdditionOccursOnItemWithoutTitle() throws SQLException {

        context.turnOffAuthorisationSystem();

        Item item = ItemBuilder.createItem(context, collection)
                               .build();

        context.restoreAuthSystemState();
        context.commit();

        item = context.reloadEntity(item);

        assertThat(itemService.getMetadataByMetadataString(item, "cris.customurl"), empty());

    }

    @Test
    public void testNoCustomUrlAdditionOccursOnItemWithTitleContainingOnlyInvalidCharacters() throws SQLException {

        context.turnOffAuthorisationSystem();

        Item item = ItemBuilder.createItem(context, collection)
                               .withTitle("$$$$$")
                               .build();

        context.restoreAuthSystemState();
        context.commit();

        item = context.reloadEntity(item);

        assertThat(itemService.getMetadataByMetadataString(item, "cris.customurl"), empty());

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

        assertThat(itemService.getMetadataByMetadataString(item, "cris.customurl"), empty());

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

        assertThat(itemService.getMetadataByMetadataString(item, "cris.customurl"), empty());

    }
}
