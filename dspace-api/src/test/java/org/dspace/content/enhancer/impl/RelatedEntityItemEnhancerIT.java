/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.enhancer.impl;

import static org.dspace.core.CrisConstants.PLACEHOLDER_PARENT_METADATA_VALUE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

import java.util.List;

import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.builder.MetadataFieldBuilder;
import org.dspace.builder.MetadataSchemaBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataSchema;
import org.dspace.content.MetadataValue;
import org.dspace.content.Relationship;
import org.dspace.content.authority.AuthorityBackedRelationshipServiceImpl;
import org.dspace.content.authority.factory.ContentAuthorityServiceFactory;
import org.dspace.content.authority.service.AuthorityBackedRelationshipService;
import org.dspace.content.authority.service.MetadataAuthorityService;
import org.dspace.content.enhancer.service.ItemEnhancerService;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.MetadataFieldService;
import org.dspace.content.service.MetadataSchemaService;
import org.dspace.services.ConfigurationService;
import org.dspace.utils.DSpace;
import org.junit.Before;
import org.junit.Test;

/**
 * End-to-end tests for the relationship additional metadata produced by the related entity enhancer.
 *
 * <p>The enhancer resolves the configured generator through the relationship configuration, reads the
 * related entity through the {@code ItemMetadataSource} adapter, persists the extracted values on the
 * owning item and stamps them with the relationship id. The tests cover the derived value, the
 * inherited authority, the placeholder for an empty source and idempotency on re-enhancement.</p>
 *
 * @author Adamo Fapohunda (adamo.fapohunda at 4science.com)
 */
public class RelatedEntityItemEnhancerIT extends AbstractIntegrationTestWithDatabase {

    private static final String ADDITIONAL_FIELD = "additional.identifier.orcid";
    private static final String ORCID = "0000-0002-1825-0097";

    private ItemService itemService;
    private ItemEnhancerService itemEnhancerService;
    private AuthorityBackedRelationshipService authorityBackedRelationshipService;

    private Collection collection;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();

        DSpace dspace = new DSpace();
        itemService = ContentServiceFactory.getInstance().getItemService();
        itemEnhancerService = dspace.getServiceManager().getServiceByName(
            "org.dspace.content.enhancer.service.ItemEnhancerService", ItemEnhancerService.class);
        authorityBackedRelationshipService = dspace.getServiceManager().getServiceByName(
            AuthorityBackedRelationshipServiceImpl.class.getCanonicalName(),
            AuthorityBackedRelationshipService.class);

        ConfigurationService configurationService = dspace.getSingletonService(ConfigurationService.class);
        configurationService.setProperty("authority.controlled.dc.contributor.author", "true");
        MetadataAuthorityService metadataAuthorityService =
            ContentAuthorityServiceFactory.getInstance().getMetadataAuthorityService();
        metadataAuthorityService.clearCache();

        context.turnOffAuthorisationSystem();
        registerAdditionalOrcidField();
        Community community = CommunityBuilder.createCommunity(context).withName("community").build();
        collection = CollectionBuilder.createCollection(context, community).withName("collection").build();
        context.restoreAuthSystemState();
    }

    @Test
    public void testDerivedAdditionalMetadataPersistedAndStampedAfterArchive() throws Exception {
        context.turnOffAuthorisationSystem();
        Item person = createPerson(ORCID, "en");
        Item publication = createPublication(person);
        Relationship relationship = promote(publication, person);
        context.restoreAuthSystemState();
        context.commit();

        publication = context.reloadEntity(publication);
        relationship = context.reloadEntity(relationship);

        context.turnOffAuthorisationSystem();
        itemEnhancerService.enhance(context, publication, false);
        context.restoreAuthSystemState();
        context.commit();

        publication = context.reloadEntity(publication);
        MetadataValue additional = singleAdditional(publication);

        assertThat(additional.getValue(), equalTo(ORCID));
        assertThat(additional.getLanguage(), equalTo("en"));
        assertThat(additional.getRelationship(), notNullValue());
        assertThat(additional.getRelationship().getID(), equalTo(relationship.getID()));
        assertThat(additional.getAuthority(), equalTo(primaryAuthority(publication)));
    }

    @Test
    public void testAuthorityInheritedFromPrimaryWhenDerivedAuthorityIsBlank() throws Exception {
        context.turnOffAuthorisationSystem();
        Item person = createPerson(ORCID, null);
        Item publication = createPublication(person);
        Relationship relationship = promote(publication, person);
        context.restoreAuthSystemState();
        context.commit();

        publication = context.reloadEntity(publication);
        relationship = context.reloadEntity(relationship);

        context.turnOffAuthorisationSystem();
        itemEnhancerService.enhance(context, publication, false);
        context.restoreAuthSystemState();
        context.commit();

        publication = context.reloadEntity(publication);
        MetadataValue additional = singleAdditional(publication);

        assertThat(additional.getAuthority(), equalTo(person.getID().toString()));
        assertThat(additional.getRelationship().getID(), equalTo(relationship.getID()));
    }

    @Test
    public void testPlaceholderWrittenWhenSourceHasNoValues() throws Exception {
        context.turnOffAuthorisationSystem();
        Item person = createPerson(null, null);
        Item publication = createPublication(person);
        Relationship relationship = promote(publication, person);
        context.restoreAuthSystemState();
        context.commit();

        publication = context.reloadEntity(publication);
        relationship = context.reloadEntity(relationship);

        context.turnOffAuthorisationSystem();
        itemEnhancerService.enhance(context, publication, false);
        context.restoreAuthSystemState();
        context.commit();

        publication = context.reloadEntity(publication);
        MetadataValue additional = singleAdditional(publication);

        assertThat(additional.getValue(), equalTo(PLACEHOLDER_PARENT_METADATA_VALUE));
        assertThat(additional.getRelationship().getID(), equalTo(relationship.getID()));
    }

    @Test
    public void testReEnhancementIsIdempotentPerRelationshipAndTargetField() throws Exception {
        context.turnOffAuthorisationSystem();
        Item person = createPerson(ORCID, "en");
        Item publication = createPublication(person);
        Relationship relationship = promote(publication, person);
        context.restoreAuthSystemState();
        context.commit();

        publication = context.reloadEntity(publication);
        relationship = context.reloadEntity(relationship);

        context.turnOffAuthorisationSystem();
        itemEnhancerService.enhance(context, publication, false);
        context.restoreAuthSystemState();
        context.commit();

        publication = context.reloadEntity(publication);
        Integer firstId = singleAdditional(publication).getID();

        context.turnOffAuthorisationSystem();
        itemEnhancerService.enhance(context, publication, false);
        context.restoreAuthSystemState();
        context.commit();

        publication = context.reloadEntity(publication);
        List<MetadataValue> additionals =
            itemService.getMetadataByMetadataString(publication, ADDITIONAL_FIELD);

        assertThat(additionals, hasSize(1));
        assertThat(additionals.get(0).getID(), equalTo(firstId));
        assertThat(additionals.get(0).getRelationship().getID(), equalTo(relationship.getID()));
    }

    /**
     * Register the target metadata field used by the production relationship configuration.
     *
     * <p>The field is created in the test database so that the boot-time registry validation of the
     * production configuration accepts the configured target field while this integration test runs.</p>
     */
    private void registerAdditionalOrcidField() throws Exception {
        MetadataFieldService metadataFieldService = ContentServiceFactory.getInstance().getMetadataFieldService();
        MetadataField existing = metadataFieldService.findByElement(context, "additional", "identifier", "orcid");
        if (existing != null) {
            return;
        }

        MetadataSchemaService metadataSchemaService = ContentServiceFactory.getInstance().getMetadataSchemaService();
        MetadataSchema schema = metadataSchemaService.find(context, "additional");
        if (schema == null) {
            schema = MetadataSchemaBuilder
                .createMetadataSchema(context, "additional", "http://dspace.org/additional")
                .build();
        }
        MetadataFieldBuilder.createMetadataField(context, schema, "identifier", "orcid", null).build();
    }

    private Item createPerson(String orcid, String language) throws Exception {
        Item person = ItemBuilder.createItem(context, collection)
            .withTitle("person")
            .withEntityType("Person")
            .inArchive()
            .build();
        if (orcid != null) {
            itemService.addMetadata(context, person, "person", "identifier", "orcid", language, orcid);
            itemService.update(context, person);
        }
        return person;
    }

    private Item createPublication(Item person) throws Exception {
        return ItemBuilder.createItem(context, collection)
            .withTitle("publication")
            .withEntityType("Publication")
            .withAuthor("Smith, John", person.getID().toString())
            .inArchive()
            .build();
    }

    private Relationship promote(Item publication, Item person) throws Exception {
        MetadataValue author = itemService
            .getMetadata(publication, "dc", "contributor", "author", Item.ANY).get(0);
        return authorityBackedRelationshipService.promoteResolvedAuthority(context, publication, author, person);
    }

    private MetadataValue singleAdditional(Item publication) {
        List<MetadataValue> additionals =
            itemService.getMetadataByMetadataString(publication, ADDITIONAL_FIELD);
        assertThat(additionals, hasSize(1));
        return additionals.get(0);
    }

    private String primaryAuthority(Item publication) {
        return itemService.getMetadata(publication, "dc", "contributor", "author", Item.ANY)
            .get(0).getAuthority();
    }

}
