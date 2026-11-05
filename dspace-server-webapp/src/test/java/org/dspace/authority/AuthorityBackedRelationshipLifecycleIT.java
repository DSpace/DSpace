/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authority;

import static org.dspace.content.authority.Choices.CF_ACCEPTED;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.core.MediaType;
import org.dspace.app.rest.model.patch.AddOperation;
import org.dspace.app.rest.model.patch.MoveOperation;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.app.rest.model.patch.RemoveOperation;
import org.dspace.app.rest.model.patch.ReplaceOperation;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.authority.service.AuthorityValueService;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.EPersonBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.Relationship;
import org.dspace.content.authority.service.AuthorityBackedRelationshipService;
import org.dspace.content.authority.service.ChoiceAuthorityService;
import org.dspace.content.authority.service.MetadataAuthorityService;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.MetadataValueService;
import org.dspace.content.service.RelationshipConfigurationService;
import org.dspace.content.service.RelationshipService;
import org.dspace.core.service.PluginService;
import org.dspace.eperson.EPerson;
import org.dspace.services.ConfigurationService;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * End-to-end verification of the phase-1 authority-backed relationship lifecycle. Exercises both
 * archival orders (owner-then-target and target-then-owner), confirms equivalent relationships,
 * that editing display text does not disturb the relationship, that removing the value removes the
 * relationship, and that the item reads cleanly throughout. Idempotency is asserted across
 * re-index/re-archive.
 *
 * @author Adamo Fapohunda (adamo.fapohunda at 4science.com)
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 */
public class AuthorityBackedRelationshipLifecycleIT extends AbstractControllerIntegrationTest {

    private static final String ORCID = "0000-0002-9079-593X";

    private static final String REFERENCE_TOKEN = AuthorityValueService.REFERENCE + "ORCID::" + ORCID;

    private EPerson submitter;

    private Collection publicationCollection;

    private Collection personCollection;

    private ItemService itemService;

    private RelationshipService relationshipService;

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private ChoiceAuthorityService choiceAuthorityService;

    @Autowired
    private MetadataAuthorityService metadataAuthorityService;

    @Autowired
    private PluginService pluginService;

    @Autowired
    private RelationshipConfigurationService relationshipConfigurationService;

    @Autowired
    private AuthorityBackedRelationshipService authorityBackedRelationshipService;

    @Autowired
    private MetadataValueService metadataValueService;

    @Before
    public void setup() throws Exception {
        choiceAuthorityService.getChoiceAuthoritiesNames();
        itemService = ContentServiceFactory.getInstance().getItemService();
        relationshipService = ContentServiceFactory.getInstance().getRelationshipService();

        configurationService.setProperty("cris-consumer.skip-empty-authority", false);
        configurationService.setProperty(
            "plugin.named.org.dspace.content.authority.ChoiceAuthority",
            new String[] { "org.dspace.content.authority.ItemAuthority = AuthorAuthority" });
        configurationService.setProperty("choices.plugin.dc.contributor.author", "AuthorAuthority");
        configurationService.setProperty("choices.presentation.dc.contributor.author", "suggest");
        configurationService.setProperty("authority.controlled.dc.contributor.author", "true");
        configurationService.setProperty("cris.ItemAuthority.AuthorAuthority.entityType", "Person");
        pluginService.clearNamedPluginClasses();
        choiceAuthorityService.clearCache();
        metadataAuthorityService.clearCache();

        context.turnOffAuthorisationSystem();

        submitter = EPersonBuilder.createEPerson(context)
                                  .withEmail("submitter@example.com")
                                  .withPassword(password)
                                  .build();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();

        publicationCollection = createCollection("Collection of publications", "Publication");
        personCollection = createCollection("Collection of persons", "Person");

        context.setCurrentUser(submitter);
        context.restoreAuthSystemState();
    }

    /**
     * Archive the target (Person) first, then the owner (Publication) that references it directly
     * by UUID. Assert the relationship is minted, editing the display text leaves it intact, the
     * item reads cleanly, and removing the value removes the relationship.
     */
    @Test
    public void testOwnerAfterTargetFullLifecycle() throws Exception {

        context.turnOffAuthorisationSystem();

        Item person = ItemBuilder.createItem(context, personCollection).withTitle("Jane Doe").build();

        Item publication = ItemBuilder.createItem(context, publicationCollection)
                                      .withTitle("Publication")
                                      .withAuthor("J. Doe", person.getID().toString(), CF_ACCEPTED)
                                      .build();

        context.restoreAuthSystemState();
        context.commit();

        publication = context.reloadEntity(publication);
        person = context.reloadEntity(person);

        // relationship minted, configuration-backed, owner=left, target=right
        List<Relationship> relationships = relationshipService.findByItem(context, publication);
        assertThat(relationships, hasSize(1));
        Relationship relationship = relationships.get(0);
        assertThat(relationship.getRelationshipType(), nullValue());
        assertThat(relationship.getRelationshipConfigKey(), equalTo("authority:dc.contributor.author"));
        assertThat(relationship.getLeftItem(), equalTo(publication));
        assertThat(relationship.getRightItem(), equalTo(person));
        Integer relationshipId = relationship.getID();

        MetadataValue author = getSingleAuthor(publication);
        assertThat(author.getValue(), equalTo("J. Doe"));
        assertThat(author.getRelationship().getID(), equalTo(relationshipId));

        // the item reads cleanly (no NPE from the configuration-backed row)
        assertThat(itemService.getMetadata(publication, "dc", "contributor", "author", Item.ANY), hasSize(1));

        // edit the display text: relationship must be untouched
        context.turnOffAuthorisationSystem();
        publication = context.reloadEntity(publication);
        author = getSingleAuthor(publication);
        author.setValue("Jane Doe (edited)");
        itemService.update(context, publication);
        context.restoreAuthSystemState();
        context.commit();

        publication = context.reloadEntity(publication);
        List<Relationship> afterEdit = relationshipService.findByItem(context, publication);
        assertThat(afterEdit, hasSize(1));
        assertThat(afterEdit.get(0).getID(), is(relationshipId));
        assertThat(getSingleAuthor(publication).getValue(), equalTo("Jane Doe (edited)"));

        // Remove the configured final author anchor: the compound service removes
        // the relationship. This is NOT an ORM/FK cascade from metadata deletion.
        String token = getAuthToken(admin.getEmail(), password);
        List<Operation> ops = new ArrayList<>();
        ops.add(new RemoveOperation("/metadata/dc.contributor.author/0"));
        getClient(token).perform(patch("/api/core/items/" + publication.getID())
                            .content(getPatchContent(ops))
                            .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                        .andExpect(status().isOk());

        publication = context.reloadEntity(publication);
        assertThat(relationshipService.findByItem(context, publication), hasSize(0));
        assertThat(itemService.getMetadataByMetadataString(publication, "dc.contributor.author"), hasSize(0));
    }

    /**
     * Archive the owner (Publication) first with an unresolved reference token, then archive the
     * target (Person). The reverse resolver back-fills the relationship. Assert equivalence with
     * the owner-after-target order and idempotency across a second resolution pass.
     */
    @Test
    public void testTargetAfterOwnerFullLifecycleAndIdempotency() throws Exception {

        context.turnOffAuthorisationSystem();

        Item publication = ItemBuilder.createItem(context, publicationCollection)
                                      .withTitle("Publication")
                                      .withAuthor("Walter White", REFERENCE_TOKEN)
                                      .build();

        context.restoreAuthSystemState();
        context.commit();

        // token unresolved: nothing minted yet
        publication = context.reloadEntity(publication);
        assertThat(relationshipService.findByItem(context, publication), hasSize(0));
        assertThat(getSingleAuthor(publication).getAuthority(), equalTo(REFERENCE_TOKEN));

        // now archive the target, triggering reverse resolution
        context.turnOffAuthorisationSystem();
        Item person = ItemBuilder.createItem(context, personCollection)
                                 .withTitle("Walter White")
                                 .withOrcidIdentifier(ORCID)
                                 .build();
        context.restoreAuthSystemState();
        context.commit();

        publication = context.reloadEntity(publication);
        person = context.reloadEntity(person);

        // relationship back-filled: equivalent to the owner-after-target order
        List<Relationship> relationships = relationshipService.findByItem(context, publication);
        assertThat(relationships, hasSize(1));
        Relationship relationship = relationships.get(0);
        assertThat(relationship.getRelationshipType(), nullValue());
        assertThat(relationship.getRelationshipConfigKey(), equalTo("authority:dc.contributor.author"));
        assertThat(relationship.getLeftItem(), equalTo(publication));
        assertThat(relationship.getRightItem(), equalTo(person));
        Integer relationshipId = relationship.getID();

        MetadataValue author = getSingleAuthor(publication);
        assertThat(author.getAuthority(), equalTo(person.getID().toString()));

        // the item reads cleanly
        assertThat(itemService.getMetadata(publication, "dc", "contributor", "author", Item.ANY), hasSize(1));

        // idempotency: force another resolution pass by touching the person
        context.turnOffAuthorisationSystem();
        person = context.reloadEntity(person);
        itemService.addMetadata(context, person, "dc", "description", null, null, "touch");
        itemService.update(context, person);
        context.restoreAuthSystemState();
        context.commit();

        publication = context.reloadEntity(publication);
        List<Relationship> afterReindex = relationshipService.findByItem(context, publication);
        assertThat(afterReindex, hasSize(1));
        assertThat(afterReindex.get(0).getID(), is(relationshipId));
    }

    /**
     * Tests removing relationship-backed metadata when its authority configuration is no longer available.
     */
    @Test
    public void testRemoveRelationshipMetadataWithMissingAuthorityConfiguration() throws Exception {
        context.turnOffAuthorisationSystem();

        Item person = ItemBuilder.createItem(context, personCollection)
                                 .withTitle("Jane Doe")
                                 .build();

        Item publication = ItemBuilder.createItem(context, publicationCollection)
                                      .withTitle("Publication")
                                      .withAuthor("J. Doe", person.getID().toString(), CF_ACCEPTED)
                                      .build();

        context.restoreAuthSystemState();
        context.commit();

        publication = context.reloadEntity(publication);

        List<Relationship> relationships = relationshipService.findByItem(context, publication);
        assertThat(relationships, hasSize(1));
        assertThat(relationships.get(0).getRelationshipConfigKey(),
                   equalTo("authority:dc.contributor.author"));
        assertThat(getSingleAuthor(publication).getRelationship(), notNullValue());

        // Simulate removing the authority configuration after the Relationship was persisted.
        configurationService.setProperty("choices.plugin.dc.contributor.author", "");
        choiceAuthorityService.clearCache();
        metadataAuthorityService.clearCache();

        assertThrows(IllegalArgumentException.class,
                     () -> relationshipConfigurationService.getByKey("authority:dc.contributor.author"));

        String token = getAuthToken(admin.getEmail(), password);
        List<Operation> ops = new ArrayList<>();
        ops.add(new RemoveOperation("/metadata/dc.contributor.author/0"));

        getClient(token).perform(patch("/api/core/items/" + publication.getID())
                                     .content(getPatchContent(ops))
                                     .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                        .andExpect(status().isOk());

        publication = context.reloadEntity(publication);
        assertThat(relationshipService.findByItem(context, publication), hasSize(0));
        assertThat(itemService.getMetadataByMetadataString(publication, "dc.contributor.author"), hasSize(0));
    }

    /**
     * Tests replacing relationship-backed metadata through edit-items when its authority configuration is missing.
     */
    @Test
    public void testEditItemWithMissingAuthorityConfiguration() throws Exception {
        context.turnOffAuthorisationSystem();

        Item person = ItemBuilder.createItem(context, personCollection)
                                 .withTitle("Jane Doe")
                                 .build();

        Item publication = ItemBuilder.createItem(context, publicationCollection)
                                      .withTitle("Publication")
                                      .withAuthor("J. Doe", person.getID().toString(), CF_ACCEPTED)
                                      .build();

        context.restoreAuthSystemState();
        context.commit();

        publication = context.reloadEntity(publication);

        List<Relationship> relationships = relationshipService.findByItem(context, publication);
        assertThat(relationships, hasSize(1));
        assertThat(relationships.get(0).getRelationshipConfigKey(),
                   equalTo("authority:dc.contributor.author"));
        assertThat(getSingleAuthor(publication).getRelationship(), notNullValue());

        // Simulate removing the authority configuration after persistence
        configurationService.setProperty("choices.plugin.dc.contributor.author", "");
        choiceAuthorityService.clearCache();
        metadataAuthorityService.clearCache();

        assertThrows(IllegalArgumentException.class,
                     () -> relationshipConfigurationService.getByKey("authority:dc.contributor.author"));

        String token = getAuthToken(admin.getEmail(), password);

        // Replace the linked author with a plain-text author
        List<Operation> operations = new ArrayList<>();
        operations.add(new AddOperation(
            "/sections/traditionalpageone/dc.contributor.author",
            List.of(Map.of("value", "Updated Author"))
        ));

        getClient(token).perform(patch("/api/core/edititems/" + publication.getID() + ":MODE1")
                                     .content(getPatchContent(operations))
                                     .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                        .andExpect(status().isOk());

        publication = context.reloadEntity(publication);

        assertThat(relationshipService.findByItem(context, publication), hasSize(0));
        assertThat(itemService.getMetadataByMetadataString(publication, "dc.contributor.author"), hasSize(1));
        assertThat(getSingleAuthor(publication).getValue(), equalTo("Updated Author"));
        assertThat(getSingleAuthor(publication).getRelationship(), nullValue());
    }

    /**
     * Tests removing non-anchor metadata while preserving a relationship that still has linked metadata.
     */
    @Test
    public void testRemoveNonAnchorWithMissingConfigurationKeepsRelationship() throws Exception {
        context.turnOffAuthorisationSystem();

        Item person = ItemBuilder.createItem(context, personCollection)
                                 .withTitle("Jane Doe").build();
        Item publication = ItemBuilder.createItem(context, publicationCollection)
                                      .withTitle("Publication")
                                      .withAuthor("J. Doe", person.getID().toString(), CF_ACCEPTED)
                                      .build();

        Relationship relationship = relationshipService.findByItem(context, publication).get(0);

        itemService.addMetadata(context, publication, "person", "identifier", "orcid",
                                null, "0000-0002-1825-0097");
        MetadataValue orcid = itemService.getMetadataByMetadataString(
            publication, "person.identifier.orcid").get(0);

        authorityBackedRelationshipService.attachMetadataToRelationship(context, orcid, relationship);

        context.restoreAuthSystemState();
        context.commit();

        configurationService.setProperty("choices.plugin.dc.contributor.author", "");
        choiceAuthorityService.clearCache();
        metadataAuthorityService.clearCache();

        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(patch("/api/core/items/" + publication.getID())
                                     .content(getPatchContent(List.of(
                                         new RemoveOperation("/metadata/person.identifier.orcid/0"))))
                                     .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                        .andExpect(status().isOk());

        publication = context.reloadEntity(publication);

        assertThat(relationshipService.findByItem(context, publication), hasSize(1));
        assertThat(getSingleAuthor(publication).getRelationship(), notNullValue());
        assertThat(itemService.getMetadataByMetadataString(publication, "person.identifier.orcid"), hasSize(0));
    }

    /**
     * Tests removing the last linked metadata value from a relationship whose anchor is already missing.
     */
    @Test
    public void testRemoveLastNonAnchorWithMissingConfigurationDeletesRelationship() throws Exception {
        context.turnOffAuthorisationSystem();

        Item person = ItemBuilder.createItem(context, personCollection)
                                 .withTitle("Jane Doe").build();
        Item publication = ItemBuilder.createItem(context, publicationCollection)
                                      .withTitle("Publication")
                                      .withAuthor("J. Doe", person.getID().toString(), CF_ACCEPTED)
                                      .build();

        Relationship relationship = relationshipService.findByItem(context, publication).get(0);

        itemService.addMetadata(context, publication, "person", "identifier", "orcid",
                                null, "0000-0002-1825-0097");
        MetadataValue orcid = itemService.getMetadataByMetadataString(
            publication, "person.identifier.orcid").get(0);

        authorityBackedRelationshipService.attachMetadataToRelationship(context, orcid, relationship);

        // Simulate stale persisted state: the anchor is gone, but the relationship remains.
        MetadataValue author = getSingleAuthor(publication);
        author.setRelationship(null);
        publication.getMetadata().remove(author);
        metadataValueService.delete(context, author);
        itemService.update(context, publication);

        context.restoreAuthSystemState();
        context.commit();

        configurationService.setProperty("choices.plugin.dc.contributor.author", "");
        choiceAuthorityService.clearCache();
        metadataAuthorityService.clearCache();

        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(patch("/api/core/items/" + publication.getID())
                                     .content(getPatchContent(List.of(
                                         new RemoveOperation("/metadata/person.identifier.orcid/0"))))
                                     .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                        .andExpect(status().isOk());

        publication = context.reloadEntity(publication);

        assertThat(relationshipService.findByItem(context, publication), hasSize(0));
        assertThat(itemService.getMetadataByMetadataString(publication, "person.identifier.orcid"), hasSize(0));
    }

    /**
     * Tests updating unrelated metadata without affecting an existing relationship when its configuration is missing.
     */
    @Test
    public void testUpdateUnrelatedMetadataWithMissingAuthorityConfiguration() throws Exception {
        context.turnOffAuthorisationSystem();

        Item person = ItemBuilder.createItem(context, personCollection)
                                 .withTitle("Jane Doe")
                                 .build();

        Item publication = ItemBuilder.createItem(context, publicationCollection)
                                      .withTitle("Publication")
                                      .withAuthor("J. Doe", person.getID().toString(), CF_ACCEPTED)
                                      .build();

        context.restoreAuthSystemState();
        context.commit();

        publication = context.reloadEntity(publication);

        assertThat(relationshipService.findByItem(context, publication), hasSize(1));

        configurationService.setProperty("choices.plugin.dc.contributor.author", "");
        choiceAuthorityService.clearCache();
        metadataAuthorityService.clearCache();

        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(patch("/api/core/items/" + publication.getID())
                                     .content(getPatchContent(List.of(
                                         new ReplaceOperation("/metadata/dc.title/0",
                                                              Map.of("value", "Updated Publication")))))
                                     .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                        .andExpect(status().isOk());

        publication = context.reloadEntity(publication);

        assertThat(itemService.getMetadataByMetadataString(publication, "dc.title").get(0).getValue(),
                   equalTo("Updated Publication"));
        assertThat(relationshipService.findByItem(context, publication), hasSize(1));
        assertThat(getSingleAuthor(publication).getRelationship(), notNullValue());
    }

    /**
     * Tests reordering relationship-backed metadata without losing relationships when configuration is missing.
     */
    @Test
    public void testMoveRelationshipMetadataWithMissingAuthorityConfiguration() throws Exception {
        context.turnOffAuthorisationSystem();

        Item person1 = ItemBuilder.createItem(context, personCollection)
                                  .withTitle("Jane Doe")
                                  .build();

        Item person2 = ItemBuilder.createItem(context, personCollection)
                                  .withTitle("John Smith")
                                  .build();

        Item publication = ItemBuilder.createItem(context, publicationCollection)
                                      .withTitle("Publication")
                                      .withAuthor("Jane Doe", person1.getID().toString(), CF_ACCEPTED)
                                      .withAuthor("John Smith", person2.getID().toString(), CF_ACCEPTED)
                                      .build();

        context.restoreAuthSystemState();
        context.commit();

        publication = context.reloadEntity(publication);

        List<MetadataValue> authors = itemService.getMetadataByMetadataString(
            publication, "dc.contributor.author");

        assertThat(authors, hasSize(2));

        Integer firstRelationshipId = authors.get(0).getRelationship().getID();
        Integer secondRelationshipId = authors.get(1).getRelationship().getID();

        assertThat(relationshipService.findByItem(context, publication), hasSize(2));

        configurationService.setProperty("choices.plugin.dc.contributor.author", "");
        choiceAuthorityService.clearCache();
        metadataAuthorityService.clearCache();

        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(patch("/api/core/items/" + publication.getID())
                                     .content(getPatchContent(List.of(
                                         new MoveOperation(
                                             "/metadata/dc.contributor.author/1",
                                             "/metadata/dc.contributor.author/0"))))
                                     .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                        .andExpect(status().isOk());

        publication = context.reloadEntity(publication);

        authors = itemService.getMetadataByMetadataString(publication, "dc.contributor.author");

        assertThat(authors, hasSize(2));
        assertThat(authors.get(0).getValue(), equalTo("John Smith"));
        assertThat(authors.get(1).getValue(), equalTo("Jane Doe"));

        assertThat(authors.get(0).getRelationship().getID(), equalTo(secondRelationshipId));
        assertThat(authors.get(1).getRelationship().getID(), equalTo(firstRelationshipId));

        assertThat(relationshipService.findByItem(context, publication), hasSize(2));
    }


    /**
     * Verify that clearing relationship-backed Item metadata also deletes
     * its configuration-backed Relationship.
     */
    @Test
    public void testClearRelationshipBackedMetadata() throws Exception {
        context.turnOffAuthorisationSystem();

        Item person = ItemBuilder.createItem(context, personCollection)
                                 .withTitle("Jane Doe")
                                 .build();

        Item publication = ItemBuilder.createItem(context, publicationCollection)
                                      .withTitle("Publication")
                                      .withAuthor("J. Doe", person.getID().toString(), CF_ACCEPTED)
                                      .build();

        assertThat(relationshipService.findByItem(context, publication), hasSize(1));
        assertThat(getSingleAuthor(publication).getRelationship(), notNullValue());

        itemService.clearMetadata(context, publication, "dc", "contributor", "author", Item.ANY);

        assertThat(relationshipService.findByItem(context, publication), hasSize(0));
        assertThat(itemService.getMetadataByMetadataString(publication, "dc.contributor.author"), hasSize(0));
        assertThat(itemService.getMetadataByMetadataString(publication, "dc.title"), hasSize(1));

        context.restoreAuthSystemState();
    }

    /**
     * Verify that removing a relationship-backed MetadataValue through the
     * Item service also deletes its Relationship.
     */
    @Test
    public void testRemoveRelationshipBackedMetadataValues() throws Exception {
        context.turnOffAuthorisationSystem();

        Item person = ItemBuilder.createItem(context, personCollection)
                                 .withTitle("Jane Doe")
                                 .build();

        Item publication = ItemBuilder.createItem(context, publicationCollection)
                                      .withTitle("Publication")
                                      .withAuthor("J. Doe", person.getID().toString(), CF_ACCEPTED)
                                      .build();

        assertThat(relationshipService.findByItem(context, publication), hasSize(1));

        MetadataValue author = getSingleAuthor(publication);
        assertThat(author.getRelationship(), notNullValue());

        itemService.removeMetadataValues(context, publication, List.of(author));

        assertThat(relationshipService.findByItem(context, publication), hasSize(0));
        assertThat(itemService.getMetadataByMetadataString(publication, "dc.contributor.author"), hasSize(0));
        assertThat(itemService.getMetadataByMetadataString(publication, "dc.title"), hasSize(1));

        context.restoreAuthSystemState();
    }

    /**
     * Verify that clearing a relationship-backed metadata field also removes
     * other metadata values attached to the same relationship.
     *
     * This covers snapshot iteration when relationship removal indirectly
     * modifies the Item's managed metadata collection.
     */
    @Test
    public void testClearRelationshipBackedMetadataRemovesLinkedValues() throws Exception {
        context.turnOffAuthorisationSystem();

        Item person = ItemBuilder.createItem(context, personCollection)
                                 .withTitle("Jane Doe")
                                 .build();

        Item publication = ItemBuilder.createItem(context, publicationCollection)
                                      .withTitle("Publication")
                                      .withAuthor("J. Doe", person.getID().toString(), CF_ACCEPTED)
                                      .build();

        Relationship relationship = relationshipService.findByItem(context, publication).getFirst();

        // Attach another metadata value to the same relationship.
        MetadataValue orcid = itemService.addMetadata(
            context, publication, "person", "identifier", "orcid",
            null, "0000-0002-1825-0097");

        authorityBackedRelationshipService.attachMetadataToRelationship(context, orcid, relationship);

        assertThat(relationshipService.findByItem(context, publication), hasSize(1));
        assertThat(getSingleAuthor(publication).getRelationship(), notNullValue());
        assertThat(orcid.getRelationship(), notNullValue());

        // Clearing the author anchor removes the relationship and its linked metadata.
        itemService.clearMetadata(context, publication, "dc", "contributor", "author", Item.ANY);

        assertThat(relationshipService.findByItem(context, publication), hasSize(0));
        assertThat(itemService.getMetadataByMetadataString(publication, "dc.contributor.author"), hasSize(0));
        assertThat(itemService.getMetadataByMetadataString(publication, "person.identifier.orcid"), hasSize(0));

        // Unrelated metadata must remain untouched.
        assertThat(itemService.getMetadataByMetadataString(publication, "dc.title"), hasSize(1));

        context.restoreAuthSystemState();
    }

    private MetadataValue getSingleAuthor(Item item) {
        List<MetadataValue> values = itemService.getMetadataByMetadataString(item, "dc.contributor.author");
        assertThat(values, notNullValue());
        assertThat(values, hasSize(1));
        return values.get(0);
    }

    private Collection createCollection(String name, String entityType) throws Exception {
        return CollectionBuilder.createCollection(context, parentCommunity)
                                .withName(name)
                                .withEntityType(entityType)
                                .withSubmitterGroup(submitter)
                                .build();
    }

}
