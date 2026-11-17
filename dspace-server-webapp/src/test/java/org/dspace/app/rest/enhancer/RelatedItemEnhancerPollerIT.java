/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.enhancer;

import static org.dspace.core.CrisConstants.PLACEHOLDER_PARENT_METADATA_VALUE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
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
import java.util.stream.Collectors;

import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.app.matcher.CustomItemMatcher;
import org.dspace.authorize.AuthorizeException;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.builder.MetadataFieldBuilder;
import org.dspace.builder.MetadataSchemaBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataSchema;
import org.dspace.content.MetadataValue;
import org.dspace.content.authority.AuthorityBackedRelationshipServiceImpl;
import org.dspace.content.authority.factory.ContentAuthorityServiceFactory;
import org.dspace.content.authority.service.AuthorityBackedRelationshipService;
import org.dspace.content.authority.service.ChoiceAuthorityService;
import org.dspace.content.authority.service.MetadataAuthorityService;
import org.dspace.content.enhancer.service.ItemEnhancerService;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.MetadataFieldService;
import org.dspace.content.service.MetadataSchemaService;
import org.dspace.core.Context;
import org.dspace.core.ReloadableEntity;
import org.dspace.core.factory.CoreServiceFactory;
import org.dspace.core.service.PluginService;
import org.dspace.services.ConfigurationService;
import org.dspace.utils.DSpace;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class RelatedItemEnhancerPollerIT extends AbstractIntegrationTestWithDatabase {

    private static final String ADDITIONAL_ORCID_FIELD = "additional.identifier.orcid";

    private static final String ADDITIONAL_AFFILIATION_FIELD = "additional.affiliation.name";

    private ItemService itemService;
    private ItemEnhancerService itemEnhancerService;
    private ItemEnhancerService spyItemEnhancerService;
    private AuthorityBackedRelationshipService authorityBackedRelationshipService;
    private RelatedItemEnhancerUpdatePoller poller = new RelatedItemEnhancerUpdatePoller();
    private Collection collection;

    @Before
    public void setup() throws Exception {
        final DSpace dspace = new DSpace();
        ConfigurationService configurationService = dspace.getConfigurationService();
        itemService = ContentServiceFactory.getInstance().getItemService();
        itemEnhancerService = dspace.getSingletonService(ItemEnhancerService.class);
        authorityBackedRelationshipService = dspace.getServiceManager().getServiceByName(
            AuthorityBackedRelationshipServiceImpl.class.getCanonicalName(),
            AuthorityBackedRelationshipService.class);
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
        registerAdditionalFields();
        context.restoreAuthSystemState();
    }

    @Test
    public void testUpdateRelatedItemAreProcessed() throws Exception {

        context.turnOffAuthorisationSystem();

        Item person = ItemBuilder.createItem(context, collection)
            .withTitle("Walter White")
            .withEntityType("Person")
            .withPersonMainAffiliation("4Science")
            .build();

        String personId = person.getID().toString();

        Item person2 = ItemBuilder.createItem(context, collection)
                .withTitle("John Red")
                .withEntityType("Person")
                .build();

        String person2Id = person2.getID().toString();

        Item person3 = ItemBuilder.createItem(context, collection)
                .withTitle("Marc Green")
                .withEntityType("Person")
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

        promote(publication, person);
        promote(publication2, person);
        promote(publication2, person2);
        promote(publication3, person2);
        promote(publication3, person3);

        context.restoreAuthSystemState();
        context.commit();
        publication = context.reloadEntity(publication);
        publication2 = context.reloadEntity(publication2);
        publication3 = context.reloadEntity(publication3);

        List<MetadataValue> metadataValues = publication.getMetadata();
        assertThat(metadataValues, hasSize(8));
        assertThat(getValues(publication, ADDITIONAL_ORCID_FIELD),
                containsInAnyOrder(PLACEHOLDER_PARENT_METADATA_VALUE));
        assertThat(getValues(publication, ADDITIONAL_AFFILIATION_FIELD),
                containsInAnyOrder("4Science"));
        List<MetadataValue> metadataValues2 = publication2.getMetadata();
        assertThat(metadataValues2, hasSize(12));
        assertThat(getValues(publication2, ADDITIONAL_ORCID_FIELD),
                containsInAnyOrder(
                    PLACEHOLDER_PARENT_METADATA_VALUE,
                    PLACEHOLDER_PARENT_METADATA_VALUE));
        assertThat(getValues(publication2, ADDITIONAL_AFFILIATION_FIELD),
                containsInAnyOrder(
                    "4Science",
                    PLACEHOLDER_PARENT_METADATA_VALUE));
        List<MetadataValue> metadataValues3 = publication3.getMetadata();
        assertThat(metadataValues3, hasSize(12));
        assertThat(getValues(publication3, ADDITIONAL_ORCID_FIELD),
                containsInAnyOrder(
                    PLACEHOLDER_PARENT_METADATA_VALUE,
                    "orcid-person3"));
        assertThat(getValues(publication3, ADDITIONAL_AFFILIATION_FIELD),
                containsInAnyOrder(
                    PLACEHOLDER_PARENT_METADATA_VALUE,
                    "Affiliation 1",
                    "Affiliation 2"));

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
        assertThat(metadataValues, hasSize(9));
        assertThat(getValues(publication, ADDITIONAL_ORCID_FIELD),
                containsInAnyOrder("1234-5678-9101"));
        assertThat(getValues(publication, ADDITIONAL_AFFILIATION_FIELD),
                containsInAnyOrder("4Science", "Company"));
        metadataValues2 = publication2.getMetadata();
        assertThat(metadataValues2, hasSize(13));
        assertThat(getValues(publication2, ADDITIONAL_ORCID_FIELD),
                containsInAnyOrder(
                    "1234-5678-9101",
                    PLACEHOLDER_PARENT_METADATA_VALUE));
        assertThat(getValues(publication2, ADDITIONAL_AFFILIATION_FIELD),
                containsInAnyOrder(
                    "4Science",
                    "Company",
                    PLACEHOLDER_PARENT_METADATA_VALUE));
        metadataValues3 = publication3.getMetadata();
        assertThat(metadataValues3, hasSize(12));
        assertThat(getValues(publication3, ADDITIONAL_ORCID_FIELD),
                containsInAnyOrder(
                    PLACEHOLDER_PARENT_METADATA_VALUE,
                    "orcid-person3"));

        context.turnOffAuthorisationSystem();
        itemService.clearMetadata(context, person3, "person", "identifier", "orcid", Item.ANY);
        removeAffiliation(context, person3, "Affiliation 1");
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
        assertThat(metadataValues, hasSize(9));
        assertThat(getValues(publication, ADDITIONAL_ORCID_FIELD),
                containsInAnyOrder("1234-5678-9101"));
        assertThat(getValues(publication, ADDITIONAL_AFFILIATION_FIELD),
                containsInAnyOrder("4Science", "Company"));
        metadataValues2 = publication2.getMetadata();
        assertThat(metadataValues2, hasSize(13));
        assertThat(getValues(publication2, ADDITIONAL_ORCID_FIELD),
                containsInAnyOrder(
                    "1234-5678-9101",
                    PLACEHOLDER_PARENT_METADATA_VALUE));
        assertThat(getValues(publication2, ADDITIONAL_AFFILIATION_FIELD),
                containsInAnyOrder(
                    "4Science",
                    "Company",
                    PLACEHOLDER_PARENT_METADATA_VALUE));
        metadataValues3 = publication3.getMetadata();
        assertThat(metadataValues3, hasSize(11));
        assertThat(getValues(publication3, ADDITIONAL_ORCID_FIELD),
                containsInAnyOrder(
                    PLACEHOLDER_PARENT_METADATA_VALUE,
                    PLACEHOLDER_PARENT_METADATA_VALUE));
        assertThat(getValues(publication3, ADDITIONAL_AFFILIATION_FIELD),
                containsInAnyOrder(
                    PLACEHOLDER_PARENT_METADATA_VALUE,
                    "Affiliation 2"));

    }

    /**
     * Remove the affiliation with the given value from the given person.
     *
     * @param context the DSpace context
     * @param person  the person item
     * @param value   the affiliation value to remove
     * @throws SQLException if the metadata values cannot be removed
     */
    private void removeAffiliation(Context context, Item person, String value) throws SQLException {
        List<MetadataValue> affiliations = itemService
            .getMetadata(person, "person", "affiliation", "name", Item.ANY)
            .stream()
            .filter(metadata -> value.equals(metadata.getValue()))
            .collect(Collectors.toList());
        itemService.removeMetadataValues(context, person, affiliations);
    }

    /**
     * Register the target metadata fields used by the production relationship configuration.
     *
     * @throws SQLException       if the metadata fields cannot be looked up
     * @throws AuthorizeException if the metadata schema or fields cannot be created
     */
    private void registerAdditionalFields() throws SQLException, AuthorizeException {
        registerAdditionalField("identifier", "orcid");
        registerAdditionalField("affiliation", "name");
    }

    /**
     * Register a single field of the {@code additional} metadata schema.
     *
     * @param element   the metadata field element
     * @param qualifier the metadata field qualifier
     * @throws SQLException       if the metadata field cannot be looked up
     * @throws AuthorizeException if the metadata schema or field cannot be created
     */
    private void registerAdditionalField(String element, String qualifier) throws SQLException, AuthorizeException {
        MetadataFieldService metadataFieldService = ContentServiceFactory.getInstance().getMetadataFieldService();
        MetadataField existing = metadataFieldService.findByElement(context, "additional", element, qualifier);
        if (existing != null) {
            return;
        }
        MetadataSchemaService metadataSchemaService = ContentServiceFactory.getInstance().getMetadataSchemaService();
        MetadataSchema schema = metadataSchemaService.find(context, "additional");
        if (schema == null) {
            schema = MetadataSchemaBuilder.createMetadataSchema(context, "additional", "http://dspace.org/additional")
                .build();
        }
        MetadataFieldBuilder.createMetadataField(context, schema, element, qualifier, null).build();
    }

    private void promote(Item publication, Item person) throws SQLException, AuthorizeException {
        MetadataValue author = itemService
            .getMetadata(publication, "dc", "contributor", "author", Item.ANY)
            .stream()
            .filter(value -> person.getID().toString().equals(value.getAuthority()))
            .findFirst()
            .orElseThrow();
        authorityBackedRelationshipService.promoteResolvedAuthority(context, publication, author, person);
    }

    private List<String> getValues(Item item, String metadataField) {
        return itemService.getMetadataByMetadataString(item, metadataField).stream()
            .map(MetadataValue::getValue)
            .collect(Collectors.toList());
    }

    @SuppressWarnings("rawtypes")
    private <T extends ReloadableEntity> T commitAndReload(T entity) throws SQLException, AuthorizeException {
        context.commit();
        return context.reloadEntity(entity);
    }

}
