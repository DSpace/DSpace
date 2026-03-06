/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.enhancer;

import static org.dspace.app.matcher.MetadataValueMatcher.with;
import static org.dspace.app.matcher.MetadataValueMatcher.withNoPlace;
import static org.dspace.core.CrisConstants.PLACEHOLDER_PARENT_METADATA_VALUE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.sql.SQLException;
import java.util.List;

import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.app.matcher.CustomItemMatcher;
import org.dspace.authorize.AuthorizeException;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.authority.factory.ContentAuthorityServiceFactory;
import org.dspace.content.authority.service.ChoiceAuthorityService;
import org.dspace.content.authority.service.MetadataAuthorityService;
import org.dspace.content.enhancer.service.ItemEnhancerService;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.ReloadableEntity;
import org.dspace.core.factory.CoreServiceFactory;
import org.dspace.core.service.PluginService;
import org.dspace.services.ConfigurationService;
import org.dspace.utils.DSpace;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class RelatedItemEnhancerPollerIT extends AbstractIntegrationTestWithDatabase {

    private ItemService itemService;
    private ItemEnhancerService itemEnhancerService;
    private ItemEnhancerService spyItemEnhancerService;
    private RelatedItemEnhancerUpdatePoller poller = new RelatedItemEnhancerUpdatePoller();
    private Collection collection;

    @Before
    public void setup() throws Exception {
        final DSpace dspace = new DSpace();
        ConfigurationService configurationService = dspace.getConfigurationService();
        configurationService.setProperty("item.enable-virtual-metadata", false);
        itemService = ContentServiceFactory.getInstance().getItemService();
        itemEnhancerService = dspace.getSingletonService(ItemEnhancerService.class);
        spyItemEnhancerService = spy(itemEnhancerService);
        poller.setItemEnhancerService(spyItemEnhancerService);
        poller.setItemService(itemService);
        // cleanup the queue from any items left behind by other tests
        poller.pollItemToUpdateAndProcess();
        PluginService pluginService = CoreServiceFactory.getInstance().getPluginService();
        ChoiceAuthorityService choiceAuthorityService = ContentAuthorityServiceFactory
            .getInstance().getChoiceAuthorityService();
        choiceAuthorityService.getChoiceAuthoritiesNames(); // initialize the ChoiceAuthorityService
        MetadataAuthorityService metadataAuthorityService = ContentAuthorityServiceFactory
            .getInstance().getMetadataAuthorityService();
        configurationService.setProperty("choices.plugin.dc.contributor.author", "AuthorAuthority");
        configurationService.setProperty("choices.presentation.dc.contributor.author", "suggest");
        configurationService.setProperty("authority.controlled.dc.contributor.author", "true");
        configurationService.setProperty("cris.ItemAuthority.AuthorAuthority.entityType", "Person");
        pluginService.clearNamedPluginClasses();
        choiceAuthorityService.clearCache();
        metadataAuthorityService.clearCache();

        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context)
            .withName("Parent Community")
            .build();

        collection = CollectionBuilder.createCollection(context, parentCommunity)
            .withName("Collection")
            .build();
        context.restoreAuthSystemState();
    }

    @Test
    public void testUpdateRelatedItemAreProcessed() throws Exception {



        context.turnOffAuthorisationSystem();

        Item person = ItemBuilder.createItem(context, collection)
            .withTitle("Walter White")
            .withPersonMainAffiliation("4Science")
            .build();

        String personId = person.getID().toString();

        Item person2 = ItemBuilder.createItem(context, collection)
                .withTitle("John Red")
                .build();

        String person2Id = person2.getID().toString();

        Item person3 = ItemBuilder.createItem(context, collection)
                .withTitle("Marc Green")
                .withOrcidIdentifier("orcid-person3")
                .withPersonMainAffiliation("Affiliation 1")
                .withPersonMainAffiliation("Affiliation 2")
                .build();

        String person3Id = person3.getID().toString();

        Item publication = ItemBuilder.createItem(context, collection)
            .withTitle("Test publication")
            .withEntityType("Publication")
            .withAuthor("Walter White", personId)
            .build();

        Item publication2 = ItemBuilder.createItem(context, collection)
                .withTitle("Test publication 2")
                .withEntityType("Publication")
                .withSubject("test")
                .withAuthor("Walter White", personId)
                .withAuthor("John Red", person2Id)
                .build();

        Item publication3 = ItemBuilder.createItem(context, collection)
                .withTitle("Test publication 3")
                .withEntityType("Publication")
                .withAuthor("John Red", person2Id)
                .withAuthor("Marc Green", person3Id)
                .build();

        context.restoreAuthSystemState();
        publication = context.reloadEntity(publication);
        publication2 = context.reloadEntity(publication2);
        publication3 = context.reloadEntity(publication3);

        List<MetadataValue> metadataValues = publication.getMetadata();
        assertThat(metadataValues, hasSize(10));
        assertThat(metadataValues, hasItem(with("cris.virtual.department", "4Science")));
        assertThat(metadataValues, hasItem(with("cris.virtualsource.department", personId)));
        assertThat(metadataValues, hasItem(with("cris.virtual.orcid", PLACEHOLDER_PARENT_METADATA_VALUE)));
        assertThat(metadataValues, hasItem(with("cris.virtualsource.orcid", personId)));
        List<MetadataValue> metadataValues2 = publication2.getMetadata();
        assertThat(metadataValues2, hasSize(16));
        assertThat(itemService.getMetadataByMetadataString(publication2, "cris.virtual.department"),
                containsInAnyOrder(
                    withNoPlace("cris.virtual.department", "4Science"),
                    withNoPlace("cris.virtual.department", PLACEHOLDER_PARENT_METADATA_VALUE)));
        assertThat(itemService.getMetadataByMetadataString(publication2, "cris.virtualsource.department"),
                containsInAnyOrder(
                    withNoPlace("cris.virtualsource.department", personId),
                    withNoPlace("cris.virtualsource.department", person2Id)));
        assertThat(itemService.getMetadataByMetadataString(publication2, "cris.virtual.orcid"),
                containsInAnyOrder(
                    withNoPlace("cris.virtual.orcid", PLACEHOLDER_PARENT_METADATA_VALUE),
                    withNoPlace("cris.virtual.orcid", PLACEHOLDER_PARENT_METADATA_VALUE)));
        assertThat(itemService.getMetadataByMetadataString(publication2, "cris.virtualsource.orcid"),
                containsInAnyOrder(
                    withNoPlace("cris.virtualsource.orcid", personId),
                    withNoPlace("cris.virtualsource.orcid", person2Id)));
        List<MetadataValue> metadataValues3 = publication3.getMetadata();
        assertThat(metadataValues3, hasSize(17));
        assertThat(itemService.getMetadataByMetadataString(publication3, "cris.virtual.department"),
                containsInAnyOrder(
                    withNoPlace("cris.virtual.department", PLACEHOLDER_PARENT_METADATA_VALUE),
                    withNoPlace("cris.virtual.department", "Affiliation 1"),
                    withNoPlace("cris.virtual.department", "Affiliation 2")));
        assertThat(itemService.getMetadataByMetadataString(publication3, "cris.virtualsource.department"),
                containsInAnyOrder(
                    withNoPlace("cris.virtualsource.department", person2Id),
                    withNoPlace("cris.virtualsource.department", person3Id),
                    withNoPlace("cris.virtualsource.department", person3Id)));
        assertThat(itemService.getMetadataByMetadataString(publication3, "cris.virtual.orcid"),
                containsInAnyOrder(
                    withNoPlace("cris.virtual.orcid", PLACEHOLDER_PARENT_METADATA_VALUE),
                    withNoPlace("cris.virtual.orcid", "orcid-person3")));
        assertThat(itemService.getMetadataByMetadataString(publication3, "cris.virtualsource.orcid"),
                containsInAnyOrder(
                    withNoPlace("cris.virtualsource.orcid", person2Id),
                    withNoPlace("cris.virtualsource.orcid", person3Id)));

        context.turnOffAuthorisationSystem();
        itemService.addMetadata(context, person, "person", "identifier", "orcid", null, "1234-5678-9101");
        itemService.addMetadata(context, person, "person", "affiliation", "name", null, "Company");
        itemService.update(context, person);
        context.restoreAuthSystemState();
        person = commitAndReload(person);
        Mockito.reset(spyItemEnhancerService);
        poller.pollItemToUpdateAndProcess();
        verify(spyItemEnhancerService).enhance(any(), argThat(new CustomItemMatcher(publication.getID())), eq(true));
        verify(spyItemEnhancerService).enhance(any(), argThat(new CustomItemMatcher(publication2.getID())), eq(true));
        // 2 + 1 iteration as the last poll will return null
        verify(spyItemEnhancerService, times(3)).pollItemToUpdate(any());
        verify(spyItemEnhancerService).saveAffectedItemsForUpdate(any(), eq(publication.getID()));
        verify(spyItemEnhancerService).saveAffectedItemsForUpdate(any(), eq(publication2.getID()));
        verifyNoMoreInteractions(spyItemEnhancerService);
        person = context.reloadEntity(person);
        person2 = context.reloadEntity(person2);
        person3 = context.reloadEntity(person3);
        publication = context.reloadEntity(publication);
        publication2 = context.reloadEntity(publication2);
        publication3 = context.reloadEntity(publication3);

        metadataValues = publication.getMetadata();
        assertThat(metadataValues, hasSize(12));
        assertThat(itemService.getMetadataByMetadataString(publication, "cris.virtual.department"),
                containsInAnyOrder(
                    withNoPlace("cris.virtual.department", "4Science"),
                    withNoPlace("cris.virtual.department", "Company")));
        assertThat(itemService.getMetadataByMetadataString(publication, "cris.virtualsource.department"),
                containsInAnyOrder(
                    withNoPlace("cris.virtualsource.department", personId),
                    withNoPlace("cris.virtualsource.department", personId)));
        assertThat(metadataValues, hasItem(with("cris.virtual.orcid", "1234-5678-9101")));
        assertThat(metadataValues, hasItem(with("cris.virtualsource.orcid", personId)));
        metadataValues2 = publication2.getMetadata();
        assertThat(metadataValues2, hasSize(18));
        assertThat(itemService.getMetadataByMetadataString(publication2, "cris.virtual.department"),
                containsInAnyOrder(
                    withNoPlace("cris.virtual.department", "4Science"),
                    withNoPlace("cris.virtual.department", "Company"),
                    withNoPlace("cris.virtual.department", PLACEHOLDER_PARENT_METADATA_VALUE)));
        assertThat(itemService.getMetadataByMetadataString(publication2, "cris.virtualsource.department"),
                containsInAnyOrder(
                    withNoPlace("cris.virtualsource.department", personId),
                    withNoPlace("cris.virtualsource.department", personId),
                    withNoPlace("cris.virtualsource.department", person2Id)));
        assertThat(itemService.getMetadataByMetadataString(publication2, "cris.virtual.orcid"),
                containsInAnyOrder(
                    withNoPlace("cris.virtual.orcid", "1234-5678-9101"),
                    withNoPlace("cris.virtual.orcid", PLACEHOLDER_PARENT_METADATA_VALUE)));
        assertThat(itemService.getMetadataByMetadataString(publication2, "cris.virtualsource.orcid"),
                containsInAnyOrder(
                    withNoPlace("cris.virtualsource.orcid", personId),
                    withNoPlace("cris.virtualsource.orcid", person2Id)));
        metadataValues3 = publication3.getMetadata();
        assertThat(metadataValues3, hasSize(17));
        assertThat(itemService.getMetadataByMetadataString(publication3, "cris.virtual.department"),
                containsInAnyOrder(
                    withNoPlace("cris.virtual.department", PLACEHOLDER_PARENT_METADATA_VALUE),
                    withNoPlace("cris.virtual.department", "Affiliation 1"),
                    withNoPlace("cris.virtual.department", "Affiliation 2")));
        assertThat(itemService.getMetadataByMetadataString(publication3, "cris.virtualsource.department"),
                containsInAnyOrder(
                    withNoPlace("cris.virtualsource.department", person2Id),
                    withNoPlace("cris.virtualsource.department", person3Id),
                    withNoPlace("cris.virtualsource.department", person3Id)));
        assertThat(itemService.getMetadataByMetadataString(publication3, "cris.virtual.orcid"),
                containsInAnyOrder(
                    withNoPlace("cris.virtual.orcid", PLACEHOLDER_PARENT_METADATA_VALUE),
                    withNoPlace("cris.virtual.orcid", "orcid-person3")));
        assertThat(itemService.getMetadataByMetadataString(publication3, "cris.virtualsource.orcid"),
                containsInAnyOrder(
                    withNoPlace("cris.virtualsource.orcid", person2Id),
                    withNoPlace("cris.virtualsource.orcid", person3Id)));
        context.turnOffAuthorisationSystem();
        itemService.clearMetadata(context, person3, "person", "identifier", "orcid", Item.ANY);
        itemService.removeMetadataValues(context, person3,
                List.of(itemService.getMetadataByMetadataString(person3, "person.affiliation.name").get(0)));
        itemService.update(context, person3);
        context.restoreAuthSystemState();
        person3 = commitAndReload(person3);
        Mockito.reset(spyItemEnhancerService);
        poller.pollItemToUpdateAndProcess();
        verify(spyItemEnhancerService).enhance(any(), argThat(new CustomItemMatcher(publication3.getID())), eq(true));
        // 1 + 1 iteration as the last poll will return null
        verify(spyItemEnhancerService, times(2)).pollItemToUpdate(any());
        verify(spyItemEnhancerService).saveAffectedItemsForUpdate(any(), eq(publication3.getID()));
        verifyNoMoreInteractions(spyItemEnhancerService);
        person = context.reloadEntity(person);
        person2 = context.reloadEntity(person2);
        person3 = context.reloadEntity(person3);
        publication = context.reloadEntity(publication);
        publication2 = context.reloadEntity(publication2);
        publication3 = context.reloadEntity(publication3);

        metadataValues = publication.getMetadata();
        assertThat(metadataValues, hasSize(12));
        assertThat(itemService.getMetadataByMetadataString(publication, "cris.virtual.department"),
                containsInAnyOrder(
                    withNoPlace("cris.virtual.department", "4Science"),
                    withNoPlace("cris.virtual.department", "Company")));
        assertThat(itemService.getMetadataByMetadataString(publication, "cris.virtualsource.department"),
                containsInAnyOrder(
                    withNoPlace("cris.virtualsource.department", personId),
                    withNoPlace("cris.virtualsource.department", personId)));
        assertThat(metadataValues, hasItem(with("cris.virtual.orcid", "1234-5678-9101")));
        assertThat(metadataValues, hasItem(with("cris.virtualsource.orcid", personId)));
        metadataValues2 = publication2.getMetadata();
        assertThat(metadataValues2, hasSize(18));
        assertThat(itemService.getMetadataByMetadataString(publication2, "cris.virtual.department"),
                containsInAnyOrder(
                    withNoPlace("cris.virtual.department", "4Science"),
                    withNoPlace("cris.virtual.department", "Company"),
                    withNoPlace("cris.virtual.department", PLACEHOLDER_PARENT_METADATA_VALUE)));
        assertThat(itemService.getMetadataByMetadataString(publication2, "cris.virtualsource.department"),
                containsInAnyOrder(
                    withNoPlace("cris.virtualsource.department", personId),
                    withNoPlace("cris.virtualsource.department", personId),
                    withNoPlace("cris.virtualsource.department", person2Id)));
        assertThat(itemService.getMetadataByMetadataString(publication2, "cris.virtual.orcid"),
                containsInAnyOrder(
                    withNoPlace("cris.virtual.orcid", "1234-5678-9101"),
                    withNoPlace("cris.virtual.orcid", PLACEHOLDER_PARENT_METADATA_VALUE)));
        assertThat(itemService.getMetadataByMetadataString(publication2, "cris.virtualsource.orcid"),
                containsInAnyOrder(
                    withNoPlace("cris.virtualsource.orcid", personId),
                    withNoPlace("cris.virtualsource.orcid", person2Id)));
        metadataValues3 = publication3.getMetadata();
        assertThat(metadataValues3, hasSize(15));
        assertThat(itemService.getMetadataByMetadataString(publication3, "cris.virtual.department"),
                containsInAnyOrder(
                    withNoPlace("cris.virtual.department", PLACEHOLDER_PARENT_METADATA_VALUE),
                    withNoPlace("cris.virtual.department", "Affiliation 2")));
        assertThat(itemService.getMetadataByMetadataString(publication3, "cris.virtualsource.department"),
                containsInAnyOrder(
                    withNoPlace("cris.virtualsource.department", person2Id),
                    withNoPlace("cris.virtualsource.department", person3Id)));
        assertThat(itemService.getMetadataByMetadataString(publication3, "cris.virtual.orcid"),
                containsInAnyOrder(
                    withNoPlace("cris.virtual.orcid", PLACEHOLDER_PARENT_METADATA_VALUE),
                    withNoPlace("cris.virtual.orcid", PLACEHOLDER_PARENT_METADATA_VALUE)));
        assertThat(itemService.getMetadataByMetadataString(publication3, "cris.virtualsource.orcid"),
                containsInAnyOrder(
                    withNoPlace("cris.virtualsource.orcid", person2Id),
                    withNoPlace("cris.virtualsource.orcid", person3Id)));

    }

    private List<MetadataValue> getMetadataValues(Item item, String metadataField) {
        return itemService.getMetadataByMetadataString(item, metadataField);
    }

    @SuppressWarnings("rawtypes")
    private <T extends ReloadableEntity> T commitAndReload(T entity) throws SQLException, AuthorizeException {
        context.commit();
        return context.reloadEntity(entity);
    }

}
