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

import java.util.List;

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
 * Integration test verifying that {@link CrisConsumer} mints authority-backed
 * relationships on archive for both the system (reference token) path and the
 * user-selected plain UUID path, and that unresolved authorities mint nothing.
 *
 * @author Adamo Fapohunda (adamo.fapohunda at 4science.com)
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 */
public class AuthorityBackedRelationshipCrisConsumerIT extends AbstractControllerIntegrationTest {

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
    public void testMintOnReferencePath() throws Exception {

        context.turnOffAuthorisationSystem();

        Item person = ItemBuilder.createItem(context, personCollection)
                                 .withTitle("Walter White Original")
                                 .withOrcidIdentifier("0000-0002-9079-593X")
                                 .build();

        Item publication = ItemBuilder.createItem(context, publicationCollection)
                                      .withTitle("Publication")
                                      .withAuthor("Walter White",
                                                  AuthorityValueService.REFERENCE + "ORCID::0000-0002-9079-593X")
                                      .build();

        context.restoreAuthSystemState();
        context.commit();

        publication = context.reloadEntity(publication);
        person = context.reloadEntity(person);

        // the authority stamp still happened (system path keeps setReferenceWithAuthority)
        MetadataValue author = getSingleMetadata(publication, "dc.contributor.author");
        assertThat(author.getAuthority(), equalTo(person.getID().toString()));
        assertThat(author.getConfidence(), equalTo(CF_ACCEPTED));

        // a configuration-backed relationship was minted, owner=left, target=right
        List<Relationship> relationships = relationshipService.findByItem(context, publication);
        assertThat(relationships, hasSize(1));
        Relationship relationship = relationships.get(0);
        assertThat(relationship.getRelationshipType(), nullValue());
        assertThat(relationship.getRelationshipConfigKey(), equalTo("authority:dc.contributor.author"));
        assertThat(relationship.getLeftItem(), equalTo(publication));
        assertThat(relationship.getRightItem(), equalTo(person));
    }

    @Test
    public void testMintOnUserSelectedUuidPathPreservesDisplayText() throws Exception {

        context.turnOffAuthorisationSystem();

        Item person = ItemBuilder.createItem(context, personCollection)
                                 .withTitle("John Smith")
                                 .build();

        // user picked the person directly: plain UUID authority, confidence 600,
        // and a deliberately-different display rendering
        Item publication = ItemBuilder.createItem(context, publicationCollection)
                                      .withTitle("Publication")
                                      .withAuthor("J. Smith", person.getID().toString(), CF_ACCEPTED)
                                      .build();

        context.restoreAuthSystemState();
        context.commit();

        publication = context.reloadEntity(publication);
        person = context.reloadEntity(person);

        // display text is preserved (user path does NOT call setReferenceWithAuthority)
        MetadataValue author = getSingleMetadata(publication, "dc.contributor.author");
        assertThat(author.getValue(), equalTo("J. Smith"));
        assertThat(author.getAuthority(), equalTo(person.getID().toString()));

        // relationship minted, configuration-backed, owner=left, target=right
        List<Relationship> relationships = relationshipService.findByItem(context, publication);
        assertThat(relationships, hasSize(1));
        Relationship relationship = relationships.get(0);
        assertThat(relationship.getRelationshipType(), nullValue());
        assertThat(relationship.getRelationshipConfigKey(), equalTo("authority:dc.contributor.author"));
        assertThat(relationship.getLeftItem(), equalTo(publication));
        assertThat(relationship.getRightItem(), equalTo(person));
    }

    @Test
    public void testUnresolvedAuthorityMintsNothing() throws Exception {

        context.turnOffAuthorisationSystem();

        Item publication = ItemBuilder.createItem(context, publicationCollection)
                                      .withTitle("Publication")
                                      .withAuthor("Walter White",
                                                  AuthorityValueService.REFERENCE + "ORCID::0000-0002-9079-593X")
                                      .build();

        context.restoreAuthSystemState();
        context.commit();

        publication = context.reloadEntity(publication);

        // no archived target: token remains, nothing minted
        MetadataValue author = getSingleMetadata(publication, "dc.contributor.author");
        assertThat(author.getAuthority(),
                   equalTo(AuthorityValueService.REFERENCE + "ORCID::0000-0002-9079-593X"));

        List<Relationship> relationships = relationshipService.findByItem(context, publication);
        assertThat(relationships, hasSize(0));
    }

    @Test
    public void testReArchiveMintsNothingExtra() throws Exception {

        context.turnOffAuthorisationSystem();

        Item person = ItemBuilder.createItem(context, personCollection)
                                 .withTitle("John Smith")
                                 .build();

        Item publication = ItemBuilder.createItem(context, publicationCollection)
                                      .withTitle("Publication")
                                      .withAuthor("J. Smith", person.getID().toString(), CF_ACCEPTED)
                                      .build();

        context.restoreAuthSystemState();
        context.commit();

        publication = context.reloadEntity(publication);
        List<Relationship> afterFirst = relationshipService.findByItem(context, publication);
        assertThat(afterFirst, hasSize(1));
        Integer relationshipId = afterFirst.get(0).getID();

        // force a second consume cycle by updating the item
        context.turnOffAuthorisationSystem();
        publication = context.reloadEntity(publication);
        itemService.addMetadata(context, publication, "dc", "description", null, null, "touch");
        itemService.update(context, publication);
        context.restoreAuthSystemState();
        context.commit();

        publication = context.reloadEntity(publication);
        List<Relationship> afterSecond = relationshipService.findByItem(context, publication);
        assertThat(afterSecond, hasSize(1));
        assertThat(afterSecond.get(0).getID(), is(relationshipId));
    }

    private MetadataValue getSingleMetadata(Item item, String field) {
        List<MetadataValue> values = itemService.getMetadataByMetadataString(item, field);
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
