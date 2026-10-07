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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;

import jakarta.ws.rs.core.MediaType;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.app.rest.model.patch.RemoveOperation;
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
import org.dspace.content.authority.service.ChoiceAuthorityService;
import org.dspace.content.authority.service.MetadataAuthorityService;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
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
