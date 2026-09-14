/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authority;

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
 * Integration test verifying that the reverse reference-resolution path
 * ({@link ItemReferenceResolverConsumer}) mints authority-backed relationships
 * onto owner items when the referenced target is archived later.
 *
 * @author DSpace
 */
public class AuthorityBackedRelationshipReverseResolveIT extends AbstractControllerIntegrationTest {

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
        choiceAuthorityService.getChoiceAuthoritiesNames(); // initialize the ChoiceAuthorityService
        itemService = ContentServiceFactory.getInstance().getItemService();
        relationshipService = ContentServiceFactory.getInstance().getRelationshipService();

        configurationService.setProperty("plugin.named.org.dspace.content.authority.ChoiceAuthority",
                                         new String[] {
                                             "org.dspace.content.authority.ItemAuthority = AuthorAuthority"
                                         });
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
    public void testReverseResolutionMintsRelationshipsOnArchiveOfTarget() throws Exception {

        context.turnOffAuthorisationSystem();

        String orcidAuthority = formatWillBeReferencedAuthority("ORCID", "0000-0002-1825-0097");

        // owners reference a not-yet-archived target by token
        Item firstPublication = ItemBuilder.createItem(context, publicationCollection)
                                           .withTitle("First Item")
                                           .withAuthor("Author", orcidAuthority)
                                           .build();

        Item secondPublication = ItemBuilder.createItem(context, publicationCollection)
                                            .withTitle("Second Item")
                                            .withAuthor("Author", orcidAuthority)
                                            .build();

        context.commit();
        context.restoreAuthSystemState();

        // before the target exists, nothing is minted
        firstPublication = context.reloadEntity(firstPublication);
        secondPublication = context.reloadEntity(secondPublication);
        assertThat(relationshipService.findByItem(context, firstPublication), hasSize(0));
        assertThat(relationshipService.findByItem(context, secondPublication), hasSize(0));

        // now archive the target
        context.turnOffAuthorisationSystem();
        Item person = ItemBuilder.createItem(context, personCollection)
                                 .withTitle("Author")
                                 .withOrcidIdentifier("0000-0002-1825-0097")
                                 .build();
        context.commit();
        context.restoreAuthSystemState();

        person = context.reloadEntity(person);
        firstPublication = context.reloadEntity(firstPublication);
        secondPublication = context.reloadEntity(secondPublication);

        // each owner now has a type-less relationship (owner=left, target=right)
        assertOwnerHasRelationshipTo(firstPublication, person);
        assertOwnerHasRelationshipTo(secondPublication, person);

        // a second resolution pass mints nothing extra
        context.turnOffAuthorisationSystem();
        person = context.reloadEntity(person);
        itemService.addMetadata(context, person, "dc", "description", null, null, "touch");
        itemService.update(context, person);
        context.commit();
        context.restoreAuthSystemState();

        firstPublication = context.reloadEntity(firstPublication);
        secondPublication = context.reloadEntity(secondPublication);
        assertThat(relationshipService.findByItem(context, firstPublication), hasSize(1));
        assertThat(relationshipService.findByItem(context, secondPublication), hasSize(1));
    }

    private void assertOwnerHasRelationshipTo(Item owner, Item target) throws Exception {
        List<Relationship> relationships = relationshipService.findByItem(context, owner);
        assertThat(relationships, hasSize(1));
        Relationship relationship = relationships.get(0);
        assertThat(relationship.getRelationshipType(), nullValue());
        assertThat(relationship.getLeftItem(), equalTo(owner));
        assertThat(relationship.getRightItem(), equalTo(target));

        MetadataValue author = itemService.getMetadataByMetadataString(owner, "dc.contributor.author").get(0);
        assertThat(author.getOwnedRelationshipId(), notNullValue());
        assertThat(author.getOwnedRelationshipId(), is(relationship.getID()));
    }

    private Collection createCollection(String name, String entityType) throws Exception {
        return CollectionBuilder.createCollection(context, parentCommunity)
                                .withName(name)
                                .withEntityType(entityType)
                                .withSubmitterGroup(submitter)
                                .build();
    }

    private String formatWillBeReferencedAuthority(String authorityPrefix, String value) {
        return AuthorityValueService.REFERENCE + authorityPrefix + "::" + value;
    }

}
