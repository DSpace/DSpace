/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.authority;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThrows;

import java.util.List;

import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.Relationship;
import org.dspace.content.authority.factory.ContentAuthorityServiceFactory;
import org.dspace.content.authority.service.AuthorityBackedRelationshipService;
import org.dspace.content.authority.service.ChoiceAuthorityService;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.MetadataValueService;
import org.dspace.content.service.RelationshipService;
import org.dspace.core.factory.CoreServiceFactory;
import org.dspace.services.ConfigurationService;
import org.dspace.utils.DSpace;
import org.junit.Before;
import org.junit.Test;

/**
 * Persistence and lifecycle regression tests for the metadata-to-relationship FK.
 *
 * @author Adamo Fapohunda (adamo.fapohunda at 4science.com)
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 * @author Ben Bosman (ben . bosman at atmire.com)
 */
public class AuthorityBackedRelationshipServiceIT extends AbstractIntegrationTestWithDatabase {
    private AuthorityBackedRelationshipService service;
    private ItemService itemService;
    private MetadataValueService metadataValueService;
    private RelationshipService relationshipService;
    private Collection collection;
    private Item owner;
    private Item target;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        DSpace dspace = new DSpace();
        service = dspace.getServiceManager().getServiceByName(
            AuthorityBackedRelationshipServiceImpl.class.getCanonicalName(), AuthorityBackedRelationshipService.class);
        itemService = ContentServiceFactory.getInstance().getItemService();
        metadataValueService = ContentServiceFactory.getInstance().getMetadataValueService();
        relationshipService = ContentServiceFactory.getInstance().getRelationshipService();
        ConfigurationService config = dspace.getSingletonService(ConfigurationService.class);
        config.setProperty("cris-consumer.skip-empty-authority", true);
        ChoiceAuthorityService choices =
                ContentAuthorityServiceFactory.getInstance().getChoiceAuthorityService();
        choices.getChoiceAuthoritiesNames();
        config.setProperty("plugin.named.org.dspace.content.authority.ChoiceAuthority",
            new String[] { "org.dspace.content.authority.ItemAuthority = AuthorAuthority" });
        config.setProperty("choices.plugin.dc.contributor.author", "AuthorAuthority");
        config.setProperty("choices.plugin.dc.contributor.editor", "AuthorAuthority");
        config.setProperty("authority.controlled.dc.contributor.author", "true");
        config.setProperty("authority.controlled.dc.contributor.editor", "true");
        config.setProperty("cris.ItemAuthority.AuthorAuthority.entityType", "Person");
        CoreServiceFactory.getInstance()
                .getPluginService()
                .clearNamedPluginClasses();
        choices.clearCache();
        ContentAuthorityServiceFactory.getInstance()
                .getMetadataAuthorityService()
                .clearCache();

        context.turnOffAuthorisationSystem();
        Community community = CommunityBuilder.createCommunity(context).withName("community").build();
        collection = CollectionBuilder.createCollection(context, community).withName("collection").build();
        // No authority is set initially: the test explicitly exercises promotion.
        owner = ItemBuilder.createItem(context, collection).withTitle("publication")
            .withEntityType("Publication").withAuthor("Smith, John").build();
        target = ItemBuilder.createItem(context, collection).withTitle("person").withEntityType("Person").build();
        context.restoreAuthSystemState();
    }

    @Test
    public void testPromotesMetadataToDurableRelationship() throws Exception {
        context.turnOffAuthorisationSystem();
        MetadataValue value = author();
        String text = value.getValue();
        String authority = value.getAuthority();
        Relationship relationship = service.promoteResolvedAuthority(context, owner, value, target);
        assertThat(relationship.getID(), notNullValue());
        assertThat(relationship.getLeftItem(), equalTo(owner));
        assertThat(relationship.getRightItem(), equalTo(target));
        assertThat(relationship.getRelationshipConfigKey(), equalTo("authority:dc.contributor.author"));
        assertThat(value.getRelationship().getID(), equalTo(relationship.getID()));
        assertThat(value.getValue(), equalTo(text));
        assertThat(value.getAuthority(), equalTo(authority));
        context.commit();
        value = context.reloadEntity(value);
        assertThat(value.getRelationship().getID(), equalTo(relationship.getID()));
    }

    @Test
    public void testPromotionIsIdempotentButDoesNotSilentlyRelink() throws Exception {
        context.turnOffAuthorisationSystem();
        Relationship first = service.promoteResolvedAuthority(context, owner, author(), target);
        Relationship second = service.promoteResolvedAuthority(context, owner, author(), target);
        assertThat(second.getID(), equalTo(first.getID()));
        Item other = ItemBuilder.createItem(context, collection).withTitle("other").withEntityType("Person").build();
        assertThrows(IllegalArgumentException.class,
            () -> service.promoteResolvedAuthority(context, owner, author(), other));
    }

    @Test
    public void testDifferentSemanticsToSameTargetRemainDifferentRelationships() throws Exception {
        context.turnOffAuthorisationSystem();
        itemService.addMetadata(context, owner, "dc", "contributor", "editor", null, "Smith, John");
        MetadataValue editor = itemService.getMetadata(owner, "dc", "contributor", "editor", Item.ANY).get(0);
        Relationship authorship = service.promoteResolvedAuthority(context, owner, author(), target);
        Relationship editorship = service.promoteResolvedAuthority(context, owner, editor, target);
        assertThat(authorship.getID(), not(equalTo(editorship.getID())));
        assertThat(editorship.getRelationshipConfigKey(), equalTo("authority:dc.contributor.editor"));
    }

    @Test
    public void testMultipleProjectionsShareOneRelationship() throws Exception {
        context.turnOffAuthorisationSystem();
        Relationship relationship = service.promoteResolvedAuthority(context, owner, author(), target);
        MetadataValue orcid = addOrcid();
        service.attachMetadataToRelationship(context, orcid, relationship);
        context.commit();
        relationship = context.reloadEntity(relationship);
        assertThat(metadataValueService.findByRelationship(context, relationship), hasSize(2));
        assertThat(metadataValueService.countByRelationship(context, relationship), equalTo(2));
        assertThat(relationshipService.findByItem(context, owner), hasSize(1));
    }

    @Test
    public void testRemoveDependentProjectionRetainsRelationshipAndAuthor() throws Exception {
        context.turnOffAuthorisationSystem();
        Relationship relationship = service.promoteResolvedAuthority(context, owner, author(), target);
        MetadataValue orcid = addOrcid();
        service.attachMetadataToRelationship(context, orcid, relationship);
        service.removeMetadataProjection(context, orcid);
        context.commit();
        assertThat(relationshipService.find(context, relationship.getID()), notNullValue());
        assertThat(metadataValueService.findByRelationship(context, relationship), hasSize(1));
    }

    @Test
    public void testRemovingFinalAnchorRemovesAllProjections() throws Exception {
        context.turnOffAuthorisationSystem();
        Relationship relationship = service.promoteResolvedAuthority(context, owner, author(), target);
        service.attachMetadataToRelationship(context, addOrcid(), relationship);
        service.removeMetadataValue(context, author());
        context.commit();
        assertThat(relationshipService.find(context, relationship.getID()), nullValue());
        assertThat(itemService.getMetadata(owner, "dc", "contributor", "author", Item.ANY), hasSize(0));
        assertThat(itemService.getMetadata(owner, "person", "identifier", "orcid", Item.ANY), hasSize(0));
    }

    @Test
    public void testDetachRetainsTextAndDoesNotRemint() throws Exception {
        context.turnOffAuthorisationSystem();
        MetadataValue value = author();
        value.setAuthority(target.getID().toString());
        value.setConfidence(Choices.CF_ACCEPTED);
        Relationship relationship = service.promoteResolvedAuthority(context, owner, value, target);
        service.detachRelationshipKeepMetadata(context, relationship);
        itemService.update(context, owner);
        context.commit();
        value = context.reloadEntity(value);
        assertThat(value.getRelationship(), nullValue());
        assertThat(value.getAuthority(), nullValue());
        assertThat(value.getValue(), equalTo("Smith, John"));
        assertThat(value.getConfidence(), equalTo(Choices.CF_REJECTED));
        assertThat(relationshipService.find(context, relationship.getID()), nullValue());
    }

    @Test
    public void testReplacementPreservesRelationshipIdentityAndDisplayText() throws Exception {
        context.turnOffAuthorisationSystem();
        author().setAuthority(target.getID().toString());
        Relationship relationship = service.promoteResolvedAuthority(context, owner, author(), target);
        Integer id = relationship.getID();
        Item replacement = ItemBuilder.createItem(context, collection).withTitle("replacement")
            .withEntityType("Person").build();
        service.replaceRelatedObject(context, owner, relationship, replacement);
        context.commit();
        relationship = context.reloadEntity(relationship);
        assertThat(relationship.getID(), equalTo(id));
        assertThat(relationship.getRightItem(), equalTo(replacement));
        assertThat(author().getValue(), equalTo("Smith, John"));
        assertThat(author().getAuthority(), equalTo(replacement.getID().toString()));
    }

    @Test
    public void testUnrelatedMetadataOwnerIsRejected() throws Exception {
        context.turnOffAuthorisationSystem();
        Relationship relationship = service.promoteResolvedAuthority(context, owner, author(), target);
        Item stranger = ItemBuilder.createItem(context, collection).withTitle("stranger").withAuthor("other").build();
        MetadataValue otherValue = itemService.getMetadata(stranger, "dc", "contributor", "author", Item.ANY).get(0);
        assertThrows(IllegalArgumentException.class,
            () -> service.attachMetadataToRelationship(context, otherValue, relationship));
        assertThat(otherValue.getRelationship(), nullValue());
    }

    @Test
    public void testForeignKeyDirectionDoesNotRequireMetadataToExist() throws Exception {
        context.turnOffAuthorisationSystem();
        Relationship relationship = relationshipService.createConfigBackedRelationship(context, owner, target,
            "authority:dc.contributor.author");
        context.commit();
        relationship = context.reloadEntity(relationship);
        assertThat(relationship, notNullValue());
        assertThat(metadataValueService.findByRelationship(context, relationship), hasSize(0));
        // Includes configured rows even in the path which excludes legacy tilted types.
        assertThat(relationshipService.findByItem(context, target, -1, -1, true), hasSize(1));
    }

    private MetadataValue addOrcid() throws Exception {
        itemService.addMetadata(context, owner, "person", "identifier", "orcid", null, "0000-0002-1825-0097");
        List<MetadataValue> values = itemService.getMetadata(owner, "person", "identifier", "orcid", Item.ANY);
        return values.get(values.size() - 1);
    }

    private MetadataValue author() {
        return itemService.getMetadata(owner, "dc", "contributor", "author", Item.ANY).get(0);
    }
}
