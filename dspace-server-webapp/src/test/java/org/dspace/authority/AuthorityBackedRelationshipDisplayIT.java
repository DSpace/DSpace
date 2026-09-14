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
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import java.util.List;
import java.util.stream.Collectors;

import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
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
 * Integration test verifying that an item with authority-backed authors (which
 * own type-less relationships minted by {@link CrisConsumer}) renders each
 * author exactly once — the real stored metadata value is the single display
 * representation and the type-less relationship contributes no virtual copy.
 * This documents ticket 07's phase-1 invariant: because the minted rows are
 * type-less, the {@code VirtualMetadataPopulator} map is left unchanged (the
 * typed {@code isAuthorOfPublication} subsystem stays intact) and no double
 * display can occur.
 *
 * @author DSpace
 */
public class AuthorityBackedRelationshipDisplayIT extends AbstractControllerIntegrationTest {

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
    public void testAuthorityBackedAuthorShownOnce() throws Exception {

        context.turnOffAuthorisationSystem();

        Item person = ItemBuilder.createItem(context, personCollection)
                                 .withTitle("Smith, John")
                                 .build();

        Item publication = ItemBuilder.createItem(context, publicationCollection)
                                      .withTitle("Publication")
                                      .withAuthor("Smith, John", person.getID().toString(), CF_ACCEPTED)
                                      .build();

        context.restoreAuthSystemState();
        context.commit();

        publication = context.reloadEntity(publication);
        person = context.reloadEntity(person);

        // the relationship was minted and is type-less
        List<Relationship> relationships = relationshipService.findByItem(context, publication);
        assertThat(relationships, hasSize(1));
        assertThat(relationships.get(0).getRelationshipType(), nullValue());

        // the author appears exactly once: the real stored value, no virtual duplicate
        List<MetadataValue> authors = itemService.getMetadataByMetadataString(publication, "dc.contributor.author");
        assertThat(authors, hasSize(1));
        assertThat(authors.get(0).getValue(), equalTo("Smith, John"));
        assertThat(authors.get(0).getAuthority(), equalTo(person.getID().toString()));
        assertThat(authors.get(0).getOwnedRelationshipId(), is(relationships.get(0).getID()));
    }

    @Test
    public void testMultipleAuthorityBackedAuthorsPreserveOrderAndShowOnce() throws Exception {

        context.turnOffAuthorisationSystem();

        Item personA = ItemBuilder.createItem(context, personCollection)
                                  .withTitle("Adams, Alice")
                                  .build();
        Item personB = ItemBuilder.createItem(context, personCollection)
                                  .withTitle("Brown, Bob")
                                  .build();
        Item personC = ItemBuilder.createItem(context, personCollection)
                                  .withTitle("Clark, Carol")
                                  .build();

        Item publication = ItemBuilder.createItem(context, publicationCollection)
                                      .withTitle("Publication")
                                      .withAuthor("Adams, Alice", personA.getID().toString(), CF_ACCEPTED)
                                      .withAuthor("Brown, Bob", personB.getID().toString(), CF_ACCEPTED)
                                      .withAuthor("Clark, Carol", personC.getID().toString(), CF_ACCEPTED)
                                      .build();

        context.restoreAuthSystemState();
        context.commit();

        publication = context.reloadEntity(publication);

        // three relationships, all type-less
        List<Relationship> relationships = relationshipService.findByItem(context, publication);
        assertThat(relationships, hasSize(3));
        relationships.forEach(r -> assertThat(r.getRelationshipType(), nullValue()));

        // the three authors appear once each, in submission order (owner-side ordering
        // is owned by the stored value's place, not the relationship)
        List<MetadataValue> authors = itemService.getMetadataByMetadataString(publication, "dc.contributor.author");
        assertThat(authors, hasSize(3));
        List<String> values = authors.stream().map(MetadataValue::getValue).collect(Collectors.toList());
        assertThat(values, contains("Adams, Alice", "Brown, Bob", "Clark, Carol"));

        // each stored value owns exactly one distinct relationship
        long distinctOwned = authors.stream()
                                    .map(MetadataValue::getOwnedRelationshipId)
                                    .filter(id -> id != null)
                                    .distinct()
                                    .count();
        assertThat(distinctOwned, is(3L));
    }

    private Collection createCollection(String name, String entityType) throws Exception {
        return CollectionBuilder.createCollection(context, parentCommunity)
                                .withName(name)
                                .withEntityType(entityType)
                                .withSubmitterGroup(submitter)
                                .build();
    }

}
