/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.enhancer.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

import java.util.List;
import java.util.stream.Collectors;

import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.Relationship;
import org.dspace.content.authority.AuthorityBackedRelationshipServiceImpl;
import org.dspace.content.authority.factory.ContentAuthorityServiceFactory;
import org.dspace.content.authority.service.AuthorityBackedRelationshipService;
import org.dspace.content.authority.service.MetadataAuthorityService;
import org.dspace.content.enhancer.service.ItemEnhancerService;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.services.ConfigurationService;
import org.dspace.utils.DSpace;
import org.junit.Before;
import org.junit.Test;

/**
 * End-to-end tests for the relationship additional metadata produced by the related entity enhancer.
 *
 * <p>The enhancer resolves the configured generator through the relationship configuration, reads the
 * related entity through {@code ItemMetadataSource}, persists the extracted values on the owning item and
 * stamps them with the relationship id. The tests cover the derived values for both the ORCID and the
 * affiliation target fields, the inherited authority, that no additional value is written when the
 * source field has no value, and idempotency on re-enhancement.</p>
 *
 * @author Adamo Fapohunda (adamo.fapohunda at 4science.com)
 */
public class RelatedEntityItemEnhancerIT extends AbstractIntegrationTestWithDatabase {

    private static final String ADDITIONAL_ORCID_FIELD = "additional.identifier.orcid";
    private static final String ADDITIONAL_AFFILIATION_FIELD = "additional.affiliation.name";
    private static final String ORCID = "0000-0002-1825-0097";
    private static final String AFFILIATION = "University of Milan";

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
        configurationService.setProperty("authority.controlled.dc.contributor.editor", "true");
        // The derived authority is only stamped when the target field is authority controlled, otherwise
        // it is dropped by the enhancer to keep the value copyable (e.g. on versioning).
        configurationService.setProperty("authority.controlled.additional.identifier.orcid", "true");
        MetadataAuthorityService metadataAuthorityService =
            ContentAuthorityServiceFactory.getInstance().getMetadataAuthorityService();
        metadataAuthorityService.clearCache();

        context.turnOffAuthorisationSystem();
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
        MetadataValue additional = singleAdditional(publication, ADDITIONAL_ORCID_FIELD);

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
        MetadataValue additional = singleAdditional(publication, ADDITIONAL_ORCID_FIELD);

        assertThat(additional.getAuthority(), equalTo(person.getID().toString()));
        assertThat(additional.getRelationship().getID(), equalTo(relationship.getID()));
    }

    @Test
    public void testNoAdditionalWrittenWhenSourceHasNoValues() throws Exception {
        context.turnOffAuthorisationSystem();
        Item person = createPerson(null, null);
        Item publication = createPublication(person);
        promote(publication, person);
        context.restoreAuthSystemState();
        context.commit();

        publication = context.reloadEntity(publication);

        context.turnOffAuthorisationSystem();
        itemEnhancerService.enhance(context, publication, false);
        context.restoreAuthSystemState();
        context.commit();

        publication = context.reloadEntity(publication);

        // the related person has no ORCID, so no additional value is written at all
        assertThat(itemService.getMetadataByMetadataString(publication, ADDITIONAL_ORCID_FIELD), empty());
    }

    @Test
    public void testDerivedAffiliationMetadataPersistedAndStampedAfterArchive() throws Exception {
        context.turnOffAuthorisationSystem();
        Item person = createPerson(ORCID, "en");
        addAffiliation(person, AFFILIATION);
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
        MetadataValue additional = singleAdditional(publication, ADDITIONAL_AFFILIATION_FIELD);

        assertThat(additional.getValue(), equalTo(AFFILIATION));
        assertThat(additional.getRelationship(), notNullValue());
        assertThat(additional.getRelationship().getID(), equalTo(relationship.getID()));
    }

    @Test
    public void testNoAffiliationAdditionalWrittenWhenPersonHasNoAffiliation() throws Exception {
        context.turnOffAuthorisationSystem();
        Item person = createPerson(ORCID, "en");
        Item publication = createPublication(person);
        promote(publication, person);
        context.restoreAuthSystemState();
        context.commit();

        publication = context.reloadEntity(publication);

        context.turnOffAuthorisationSystem();
        itemEnhancerService.enhance(context, publication, false);
        context.restoreAuthSystemState();
        context.commit();

        publication = context.reloadEntity(publication);

        // the related person has no affiliation, so no additional value is written at all
        assertThat(itemService.getMetadataByMetadataString(publication, ADDITIONAL_AFFILIATION_FIELD), empty());
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
        Integer firstId = singleAdditional(publication, ADDITIONAL_ORCID_FIELD).getID();

        context.turnOffAuthorisationSystem();
        itemEnhancerService.enhance(context, publication, false);
        context.restoreAuthSystemState();
        context.commit();

        publication = context.reloadEntity(publication);
        List<MetadataValue> additionals =
            itemService.getMetadataByMetadataString(publication, ADDITIONAL_ORCID_FIELD);

        assertThat(additionals, hasSize(1));
        assertThat(additionals.get(0).getID(), equalTo(firstId));
        assertThat(additionals.get(0).getRelationship().getID(), equalTo(relationship.getID()));
    }

    @Test
    public void testChangeOnOneRelationshipDoesNotTouchValuesOfTheOtherOnTheSameTargetField() throws Exception {
        String editorOrcid = "0000-0001-5109-3700";
        context.turnOffAuthorisationSystem();
        Item author = createPerson(ORCID, "en");
        Item editor = createPerson(editorOrcid, "en");
        Item publication = ItemBuilder.createItem(context, collection)
            .withTitle("publication")
            .withEntityType("Publication")
            .withAuthor("Smith, John", author.getID().toString())
            .withEditor("Doe, Jane", editor.getID().toString())
            .inArchive()
            .build();
        promote(publication, author);
        MetadataValue editorValue = itemService
            .getMetadata(publication, "dc", "contributor", "editor", Item.ANY).get(0);
        authorityBackedRelationshipService.promoteResolvedAuthority(context, publication, editorValue, editor);
        context.restoreAuthSystemState();
        context.commit();

        publication = context.reloadEntity(publication);
        context.turnOffAuthorisationSystem();
        itemEnhancerService.enhance(context, publication, false);
        context.restoreAuthSystemState();
        context.commit();

        publication = context.reloadEntity(publication);
        Integer authorValueId = itemService.getMetadataByMetadataString(publication, ADDITIONAL_ORCID_FIELD)
            .stream().filter(v -> ORCID.equals(v.getValue())).findFirst().get().getID();

        // the editor's ORCID changes: only the editor-derived value is rebuilt
        context.turnOffAuthorisationSystem();
        editor = context.reloadEntity(editor);
        itemService.clearMetadata(context, editor, "person", "identifier", "orcid", Item.ANY);
        itemService.addMetadata(context, editor, "person", "identifier", "orcid", "en", "0000-0003-1415-9269");
        itemService.update(context, editor);
        itemEnhancerService.enhance(context, context.reloadEntity(publication), false);
        context.restoreAuthSystemState();
        context.commit();

        publication = context.reloadEntity(publication);
        List<MetadataValue> additionals =
            itemService.getMetadataByMetadataString(publication, ADDITIONAL_ORCID_FIELD);
        assertThat(additionals, hasSize(2));
        assertThat(additionals.stream().map(MetadataValue::getValue).collect(Collectors.toList()),
            containsInAnyOrder(ORCID, "0000-0003-1415-9269"));
        assertThat(additionals.stream().filter(v -> ORCID.equals(v.getValue())).findFirst().get().getID(),
            equalTo(authorValueId));
    }

    @Test
    public void testRemovingEditorKeepsOnlyAuthorAdditional() throws Exception {
        assertOnlyOtherAdditionalSurvivesRemoval("editor");
    }

    @Test
    public void testRemovingAuthorKeepsOnlyEditorAdditional() throws Exception {
        assertOnlyOtherAdditionalSurvivesRemoval("author");
    }

    private void assertOnlyOtherAdditionalSurvivesRemoval(String qualifierToRemove) throws Exception {
        String editorOrcid = "0000-0001-5109-3700";
        context.turnOffAuthorisationSystem();
        Item author = createPerson(ORCID, "en");
        Item editor = createPerson(editorOrcid, "en");
        Item publication = ItemBuilder.createItem(context, collection)
            .withTitle("publication")
            .withEntityType("Publication")
            .withAuthor("Smith, John", author.getID().toString())
            .withEditor("Doe, Jane", editor.getID().toString())
            .inArchive()
            .build();
        promote(publication, author);
        MetadataValue editorValue = itemService
            .getMetadata(publication, "dc", "contributor", "editor", Item.ANY).get(0);
        authorityBackedRelationshipService.promoteResolvedAuthority(context, publication, editorValue, editor);
        context.restoreAuthSystemState();
        context.commit();

        publication = context.reloadEntity(publication);
        context.turnOffAuthorisationSystem();
        itemEnhancerService.enhance(context, publication, false);
        context.restoreAuthSystemState();
        context.commit();

        publication = context.reloadEntity(publication);
        assertThat(itemService.getMetadataByMetadataString(publication, ADDITIONAL_ORCID_FIELD), hasSize(2));

        MetadataValue toRemove = itemService
            .getMetadata(publication, "dc", "contributor", qualifierToRemove, Item.ANY).get(0);
        context.turnOffAuthorisationSystem();
        itemService.removeMetadataValues(context, publication, List.of(toRemove));
        itemService.update(context, publication);
        context.restoreAuthSystemState();
        context.commit();

        publication = context.reloadEntity(publication);
        List<MetadataValue> additionals =
            itemService.getMetadataByMetadataString(publication, ADDITIONAL_ORCID_FIELD);
        assertThat(additionals, hasSize(1));
        assertThat(additionals.get(0).getValue(), equalTo("editor".equals(qualifierToRemove) ? ORCID : editorOrcid));
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

    private void addAffiliation(Item person, String affiliation) throws Exception {
        itemService.addMetadata(context, person, "person", "affiliation", "name", null, affiliation);
        itemService.update(context, person);
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

    private MetadataValue singleAdditional(Item publication, String field) {
        List<MetadataValue> additionals =
            itemService.getMetadataByMetadataString(publication, field);
        assertThat(additionals, hasSize(1));
        return additionals.get(0);
    }

    private String primaryAuthority(Item publication) {
        return itemService.getMetadata(publication, "dc", "contributor", "author", Item.ANY)
            .get(0).getAuthority();
    }

}
