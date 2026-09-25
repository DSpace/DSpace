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
import org.dspace.app.rest.model.MetadataValueRest;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.app.rest.model.patch.RemoveOperation;
import org.dspace.app.rest.model.patch.ReplaceOperation;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.EPersonBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.Relationship;
import org.dspace.content.authority.Choices;
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
 * Integration test verifying relationship teardown when authority-backed metadata values are
 * removed via REST PATCH: removing one value removes exactly its relationship (siblings intact),
 * clearing a field removes every owned relationship, and deleting the whole item leaves nothing
 * dangling.
 *
 * @author Adamo Fapohunda (adamo.fapohunda at 4science.com)
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 */
public class AuthorityBackedRelationshipTeardownIT extends AbstractControllerIntegrationTest {

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

    @Test
    public void testRemoveOneValueRemovesOnlyItsRelationship() throws Exception {

        context.turnOffAuthorisationSystem();

        Item personA = ItemBuilder.createItem(context, personCollection).withTitle("Person A").build();
        Item personB = ItemBuilder.createItem(context, personCollection).withTitle("Person B").build();

        Item publication = ItemBuilder.createItem(context, publicationCollection)
                                      .withTitle("Publication")
                                      .withAuthor("Author A", personA.getID().toString(), CF_ACCEPTED)
                                      .withAuthor("Author B", personB.getID().toString(), CF_ACCEPTED)
                                      .build();

        context.restoreAuthSystemState();
        context.commit();

        publication = context.reloadEntity(publication);
        List<Relationship> minted = relationshipService.findByItem(context, publication);
        assertThat(minted, hasSize(2));

        // capture the relationship owned by the SECOND author before removal
        Integer authorBRelationshipId = minted.stream()
                                              .filter(rel -> personB.equals(rel.getRightItem()))
                                              .map(Relationship::getID)
                                              .findFirst()
                                              .orElse(null);
        assertThat(authorBRelationshipId, is(notNullValue()));

        // remove the first author (index 0) via REST PATCH
        String token = getAuthToken(admin.getEmail(), password);
        List<Operation> ops = new ArrayList<>();
        ops.add(new RemoveOperation("/metadata/dc.contributor.author/0"));
        getClient(token).perform(patch("/api/core/items/" + publication.getID())
                            .content(getPatchContent(ops))
                            .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                        .andExpect(status().isOk());

        publication = context.reloadEntity(publication);

        // exactly one relationship remains, and it is the one owned by author B (the surviving value)
        List<Relationship> remaining = relationshipService.findByItem(context, publication);
        assertThat(remaining, hasSize(1));
        assertThat(remaining.get(0).getID(), is(authorBRelationshipId));

        List<MetadataValue> authorsAfter = itemService.getMetadataByMetadataString(publication,
                                                                                   "dc.contributor.author");
        assertThat(authorsAfter, hasSize(1));
        assertThat(authorsAfter.get(0).getValue(), is("Author B"));
    }

    @Test
    public void testClearFieldRemovesAllOwnedRelationships() throws Exception {

        context.turnOffAuthorisationSystem();

        Item personA = ItemBuilder.createItem(context, personCollection).withTitle("Person A").build();
        Item personB = ItemBuilder.createItem(context, personCollection).withTitle("Person B").build();

        Item publication = ItemBuilder.createItem(context, publicationCollection)
                                      .withTitle("Publication")
                                      .withAuthor("Author A", personA.getID().toString(), CF_ACCEPTED)
                                      .withAuthor("Author B", personB.getID().toString(), CF_ACCEPTED)
                                      .build();

        context.restoreAuthSystemState();
        context.commit();

        publication = context.reloadEntity(publication);
        assertThat(relationshipService.findByItem(context, publication), hasSize(2));

        // remove the whole field (no index)
        String token = getAuthToken(admin.getEmail(), password);
        List<Operation> ops = new ArrayList<>();
        ops.add(new RemoveOperation("/metadata/dc.contributor.author"));
        getClient(token).perform(patch("/api/core/items/" + publication.getID())
                            .content(getPatchContent(ops))
                            .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                        .andExpect(status().isOk());

        publication = context.reloadEntity(publication);
        assertThat(relationshipService.findByItem(context, publication), hasSize(0));
        assertThat(itemService.getMetadataByMetadataString(publication, "dc.contributor.author"), hasSize(0));
    }

    @Test
    public void testReplaceWholeValueWithNullAuthorityRemovesItsRelationship() throws Exception {

        context.turnOffAuthorisationSystem();

        // A cleared authority must not be re-linked by the cris-consumer from the bare display text:
        // this test isolates the Hibernate secondary-row delete, so skip empty-authority values.
        configurationService.setProperty("cris-consumer.skip-empty-authority", true);

        Item personA = ItemBuilder.createItem(context, personCollection).withTitle("Person A").build();

        Item publication = ItemBuilder.createItem(context, publicationCollection)
                                      .withTitle("Publication")
                                      .withAuthor("Author A", personA.getID().toString(), CF_ACCEPTED)
                                      .build();

        context.restoreAuthSystemState();
        context.commit();

        publication = context.reloadEntity(publication);
        assertThat(relationshipService.findByItem(context, publication), hasSize(1));

        // whole-value replace with an empty authority (the value survives, its authority is cleared)
        String token = getAuthToken(admin.getEmail(), password);
        MetadataValueRest replacement = new MetadataValueRest();
        replacement.setValue("Author A");
        replacement.setAuthority("");
        replacement.setConfidence(Choices.CF_UNSET);

        List<Operation> ops = new ArrayList<>();
        ops.add(new ReplaceOperation("/metadata/dc.contributor.author/0", replacement));
        getClient(token).perform(patch("/api/core/items/" + publication.getID())
                            .content(getPatchContent(ops))
                            .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                        .andExpect(status().isOk());

        publication = context.reloadEntity(publication);

        // the relationship row is gone (both endpoints nulled → Hibernate deletes the secondary row)
        assertThat(relationshipService.findByItem(context, publication), hasSize(0));

        // the metadata value itself survives with its display text
        List<MetadataValue> authorsAfter = itemService.getMetadataByMetadataString(publication,
                                                                                   "dc.contributor.author");
        assertThat(authorsAfter, hasSize(1));
        assertThat(authorsAfter.get(0).getValue(), is("Author A"));
    }

    @Test
    public void testReplaceAuthorityPropertyToNullRemovesRowAndKeepsSibling() throws Exception {

        context.turnOffAuthorisationSystem();

        // A cleared authority must not be re-linked by the cris-consumer from the bare display text:
        // this test isolates the Hibernate secondary-row delete, so skip empty-authority values.
        configurationService.setProperty("cris-consumer.skip-empty-authority", true);

        Item personA = ItemBuilder.createItem(context, personCollection).withTitle("Person A").build();
        Item personB = ItemBuilder.createItem(context, personCollection).withTitle("Person B").build();

        Item publication = ItemBuilder.createItem(context, publicationCollection)
                                      .withTitle("Publication")
                                      .withAuthor("Author A", personA.getID().toString(), CF_ACCEPTED)
                                      .withAuthor("Author B", personB.getID().toString(), CF_ACCEPTED)
                                      .build();

        context.restoreAuthSystemState();
        context.commit();

        publication = context.reloadEntity(publication);
        List<Relationship> minted = relationshipService.findByItem(context, publication);
        assertThat(minted, hasSize(2));

        // capture the relationship owned by the SECOND author before the clear
        Integer authorBRelationshipId = minted.stream()
                                              .filter(rel -> personB.equals(rel.getRightItem()))
                                              .map(Relationship::getID)
                                              .findFirst()
                                              .orElse(null);
        assertThat(authorBRelationshipId, is(notNullValue()));

        // single-property replace clearing ONLY the authority of the first author
        String token = getAuthToken(admin.getEmail(), password);
        List<Operation> ops = new ArrayList<>();
        ops.add(new ReplaceOperation("/metadata/dc.contributor.author/0/authority", ""));
        getClient(token).perform(patch("/api/core/items/" + publication.getID())
                            .content(getPatchContent(ops))
                            .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                        .andExpect(status().isOk());

        publication = context.reloadEntity(publication);

        // exactly one relationship remains, and it is the sibling (author B) — detaching one author
        // must not affect the other
        List<Relationship> remaining = relationshipService.findByItem(context, publication);
        assertThat(remaining, hasSize(1));
        assertThat(remaining.get(0).getID(), is(authorBRelationshipId));

        // both metadata values survive: only the authority link of the first was cleared
        List<MetadataValue> authorsAfter = itemService.getMetadataByMetadataString(publication,
                                                                                   "dc.contributor.author");
        assertThat(authorsAfter, hasSize(2));
    }

    @Test
    public void testDeleteItemLeavesNothingDangling() throws Exception {

        context.turnOffAuthorisationSystem();

        Item personA = ItemBuilder.createItem(context, personCollection).withTitle("Person A").build();

        Item publication = ItemBuilder.createItem(context, publicationCollection)
                                      .withTitle("Publication")
                                      .withAuthor("Author A", personA.getID().toString(), CF_ACCEPTED)
                                      .build();

        context.restoreAuthSystemState();
        context.commit();

        publication = context.reloadEntity(publication);
        List<Relationship> before = relationshipService.findByItem(context, publication);
        assertThat(before, hasSize(1));

        // delete the whole item
        context.turnOffAuthorisationSystem();
        publication = context.reloadEntity(publication);
        itemService.delete(context, publication);
        context.restoreAuthSystemState();
        context.commit();

        // the person still exists and has no dangling relationships
        personA = context.reloadEntity(personA);
        assertThat(relationshipService.findByItem(context, personA), hasSize(0));
    }

    @Test
    public void testResolvedInternalReferenceElidesTheAuthorityColumnButGetterReturnsUuid() throws Exception {

        // After an internal reference is minted, the authority column is physically null in the DB,
        // but the public getter reconstitutes the UUID from the backing relationship's right_id.
        context.turnOffAuthorisationSystem();

        Item personA = ItemBuilder.createItem(context, personCollection).withTitle("Person A").build();

        Item publication = ItemBuilder.createItem(context, publicationCollection)
                                      .withTitle("Publication")
                                      .withAuthor("Author A", personA.getID().toString(), CF_ACCEPTED)
                                      .build();

        context.restoreAuthSystemState();
        context.commit();

        // evict everything and reload the metadata value straight from the DB
        context.uncacheEntity(publication);
        publication = context.reloadEntity(publication);

        List<MetadataValue> authors = itemService.getMetadataByMetadataString(publication, "dc.contributor.author");
        assertThat(authors, hasSize(1));
        MetadataValue author = authors.get(0);

        // the raw column persisted as null...
        assertThat(author.getRawAuthority(), nullValue());
        // ...yet the public getter still yields the related person's UUID
        assertThat(author.getAuthority(), equalTo(personA.getID().toString()));
        // and the backing relationship still exists
        assertThat(relationshipService.findByItem(context, publication), hasSize(1));
    }

    @Test
    public void testRetargetRepointsTheRowAndReElidesTheColumn() throws Exception {

        // Setting a new UUID authority on an internal-backed value repoints the relationship row to the new target
        // and re-elides the column.
        context.turnOffAuthorisationSystem();

        Item personA = ItemBuilder.createItem(context, personCollection).withTitle("Person A").build();
        Item personB = ItemBuilder.createItem(context, personCollection).withTitle("Person B").build();

        Item publication = ItemBuilder.createItem(context, publicationCollection)
                                      .withTitle("Publication")
                                      .withAuthor("Author A", personA.getID().toString(), CF_ACCEPTED)
                                      .build();

        context.restoreAuthSystemState();
        context.commit();

        publication = context.reloadEntity(publication);
        List<Relationship> minted = relationshipService.findByItem(context, publication);
        assertThat(minted, hasSize(1));
        assertThat(minted.get(0).getRightItem(), equalTo(personA));

        // retarget: single-property replace of the authority with personB's UUID
        String token = getAuthToken(admin.getEmail(), password);
        List<Operation> ops = new ArrayList<>();
        ops.add(new ReplaceOperation("/metadata/dc.contributor.author/0/authority", personB.getID().toString()));
        getClient(token).perform(patch("/api/core/items/" + publication.getID())
                            .content(getPatchContent(ops))
                            .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                        .andExpect(status().isOk());

        context.uncacheEntity(publication);
        publication = context.reloadEntity(publication);

        // exactly one relationship, now pointing at personB
        List<Relationship> after = relationshipService.findByItem(context, publication);
        assertThat(after, hasSize(1));
        assertThat(after.get(0).getRightItem(), equalTo(personB));

        // column re-elided; getter reconstitutes the NEW target UUID
        MetadataValue author = itemService.getMetadataByMetadataString(publication, "dc.contributor.author").get(0);
        assertThat(author.getRawAuthority(), nullValue());
        assertThat(author.getAuthority(), equalTo(personB.getID().toString()));
    }

    @Test
    public void testExternalAuthorityKeyIsNeverElided() throws Exception {

        // An external authority key has no right_id, so it is never minted and never elided — it keeps living in the
        // authority column.
        context.turnOffAuthorisationSystem();

        String externalKey = "0000-0002-1825-0097";

        Item publication = ItemBuilder.createItem(context, publicationCollection)
                                      .withTitle("Publication")
                                      .withAuthor("Walter White", externalKey, CF_ACCEPTED)
                                      .build();

        context.restoreAuthSystemState();
        context.commit();

        context.uncacheEntity(publication);
        publication = context.reloadEntity(publication);

        // no relationship minted (the key names no internal item)
        assertThat(relationshipService.findByItem(context, publication), hasSize(0));

        // the external key is untouched in the raw column
        MetadataValue author = itemService.getMetadataByMetadataString(publication, "dc.contributor.author").get(0);
        assertThat(author.getRawAuthority(), equalTo(externalKey));
        assertThat(author.getAuthority(), equalTo(externalKey));
    }

    @Test
    public void testUserClearUnlinksButKeepsDisplayText() throws Exception {

        // A user clearing the authority via REST PATCH tears the link down
        // (both endpoints nulled → row deleted) while keeping the display text.
        context.turnOffAuthorisationSystem();

        // isolate the Hibernate delete from cris-consumer re-linking of the bare display text
        configurationService.setProperty("cris-consumer.skip-empty-authority", true);

        Item personA = ItemBuilder.createItem(context, personCollection).withTitle("Person A").build();

        Item publication = ItemBuilder.createItem(context, publicationCollection)
                                      .withTitle("Publication")
                                      .withAuthor("Author A", personA.getID().toString(), CF_ACCEPTED)
                                      .build();

        context.restoreAuthSystemState();
        context.commit();

        publication = context.reloadEntity(publication);
        assertThat(relationshipService.findByItem(context, publication), hasSize(1));

        // User clears ONLY the authority property
        String token = getAuthToken(admin.getEmail(), password);
        List<Operation> ops = new ArrayList<>();
        ops.add(new ReplaceOperation("/metadata/dc.contributor.author/0/authority", ""));
        getClient(token).perform(patch("/api/core/items/" + publication.getID())
                            .content(getPatchContent(ops))
                            .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                        .andExpect(status().isOk());

        context.uncacheEntity(publication);
        publication = context.reloadEntity(publication);

        // link torn down: the relationship row is gone
        assertThat(relationshipService.findByItem(context, publication), hasSize(0));

        // display text preserved; authority now resolves to null (raw null + no endpoints)
        MetadataValue author = itemService.getMetadataByMetadataString(publication, "dc.contributor.author").get(0);
        assertThat(author.getValue(), is("Author A"));
        assertThat(author.getRawAuthority(), nullValue());
        assertThat(author.getAuthority(), nullValue());
    }

    private Collection createCollection(String name, String entityType) throws Exception {
        return CollectionBuilder.createCollection(context, parentCommunity)
                                .withName(name)
                                .withEntityType(entityType)
                                .withSubmitterGroup(submitter)
                                .build();
    }

}
