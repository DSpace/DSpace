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
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.SQLException;
import java.util.List;
import java.util.Set;

import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.EPersonBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataValue;
import org.dspace.content.Relationship;
import org.dspace.content.RelationshipConfigurationServiceImpl;
import org.dspace.content.RelationshipTypeConfiguration;
import org.dspace.content.authority.factory.ContentAuthorityServiceFactory;
import org.dspace.content.authority.service.AuthorityBackedRelationshipService;
import org.dspace.content.authority.service.ChoiceAuthorityService;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.MetadataValueService;
import org.dspace.content.service.RelationshipConfigurationService;
import org.dspace.content.service.RelationshipService;
import org.dspace.core.Constants;
import org.dspace.core.factory.CoreServiceFactory;
import org.dspace.eperson.EPerson;
import org.dspace.event.Event;
import org.dspace.services.ConfigurationService;
import org.dspace.utils.DSpace;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

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
    public void testPromotionWritesAtMostOnePrimaryPerEndpoint() throws Exception {
        context.turnOffAuthorisationSystem();
        MetadataValue value = author();
        Relationship relationship = service.promoteResolvedAuthority(context, owner, value, target);

        List<MetadataValue> values = metadataValueService.findByRelationship(context, relationship);
        assertThat(values, hasSize(1));
        MetadataValue primary = values.get(0);
        assertThat(primary.getDSpaceObject().getID(), equalTo(owner.getID()));
        assertThat(primary.getMetadataField().toString('.'), equalTo("dc.contributor.author"));

        // The Person side has no configured primary field, so no metadata is written on that endpoint.
        assertThat(values.stream().noneMatch(candidate -> candidate.getDSpaceObject().getID()
            .equals(target.getID())), is(true));

        // The relationship still exists and stays navigable from both endpoints.
        assertThat(relationshipService.findByItem(context, owner), hasItem(relationship));
        assertThat(relationshipService.findByItem(context, target), hasItem(relationship));
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
    public void testRemovingFinalPrimaryRelationshipMetadataRemovesAllRelationshipMetadata() throws Exception {
        context.turnOffAuthorisationSystem();
        Relationship relationship = service.promoteResolvedAuthority(context, owner, author(), target);
        assertThat(service.removeMetadataValue(context, author(), null), is(true));
        context.commit();
        assertThat(relationshipService.find(context, relationship.getID()), nullValue());
        assertThat(itemService.getMetadata(owner, "dc", "contributor", "author", Item.ANY), hasSize(0));
    }

    @Test
    public void testAuthorityFallbackWithExplicitKeyOnDifferentEntityType() throws Exception {
        context.turnOffAuthorisationSystem();
        Item dataset = ItemBuilder.createItem(context, collection).withTitle("dataset")
            .withEntityType("DataSet").withAuthor("Someone").build();
        MetadataValue author = itemService.getMetadata(dataset, "dc", "contributor", "author", Item.ANY).get(0);

        // The explicit bean only covers Publication -> Person. A DataSet can still use the linked-entity
        // authority fallback with the same key, provided persisted resolution uses its endpoint types.
        Relationship relationship = service.promoteResolvedAuthority(context, dataset, author, target);
        assertThat(relationship, notNullValue());
        assertThat(relationship.getRelationshipConfigKey(), equalTo("authority:dc.contributor.author"));
        assertThat(author.getRelationship().getID(), equalTo(relationship.getID()));
        RelationshipConfigurationServiceImpl configurations = new DSpace().getServiceManager().getServiceByName(
            RelationshipConfigurationServiceImpl.class.getCanonicalName(), RelationshipConfigurationServiceImpl.class);
        RelationshipTypeConfiguration resolved = configurations.getRelationshipConfig(context, relationship);
        assertThat(resolved.getLeftEntityTypes().isEmpty(), is(true));
        assertThat(resolved.getRightEntityTypes(), equalTo(Set.of("Person")));
    }

    @Test
    public void testAuthorityOnlyEditorRelationshipWithoutSpringConfiguration() throws Exception {
        context.turnOffAuthorisationSystem();
        itemService.addMetadata(context, owner, "dc", "contributor", "editor", null, "Smith, John");
        MetadataValue editor = itemService.getMetadata(owner, "dc", "contributor", "editor", Item.ANY).get(0);

        Relationship relationship = service.promoteResolvedAuthority(context, owner, editor, target);
        assertThat(relationship, notNullValue());
        assertThat(relationship.getRelationshipConfigKey(), equalTo("authority:dc.contributor.editor"));
        RelationshipConfigurationServiceImpl configurations = new DSpace().getServiceManager().getServiceByName(
            RelationshipConfigurationServiceImpl.class.getCanonicalName(), RelationshipConfigurationServiceImpl.class);
        assertThat(configurations.getRelationshipConfig(context, relationship).getLeftPrimaryMetadataField()
            .orElse(null), equalTo("dc.contributor.editor"));
    }

    @Test
    public void testCreationRejectsUnresolvableConfigurationKey() throws Exception {
        context.turnOffAuthorisationSystem();
        RelationshipTypeConfiguration unknown = new RelationshipTypeConfiguration();
        unknown.setId("nonexistent-config");
        unknown.setLeftEntityTypes(Set.of("Publication"));
        unknown.setRightEntityTypes(Set.of("Person"));
        unknown.setLeftPrimaryMetadataField("dc.contributor.author");

        assertThrows(IllegalArgumentException.class,
            () -> relationshipService.createConfigBackedRelationship(context, owner, target, unknown));
        assertThat(relationshipService.findByItem(context, owner), hasSize(0));
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
    public void testChangingPrimaryTargetIsDeleteThenCreate() throws Exception {
        context.turnOffAuthorisationSystem();
        author().setAuthority(target.getID().toString());
        Relationship first = service.promoteResolvedAuthority(context, owner, author(), target);
        Integer firstId = first.getID();
        Item replacement = ItemBuilder.createItem(context, collection).withTitle("replacement")
            .withEntityType("Person").build();

        service.removeRelationshipAndMetadata(context, first);
        assertThat(itemService.getMetadata(owner, "dc", "contributor", "author", Item.ANY), hasSize(0));
        itemService.addMetadata(context, owner, "dc", "contributor", "author", null, "Smith, John",
            replacement.getID().toString(), Choices.CF_ACCEPTED);
        Relationship second = service.promoteResolvedAuthority(context, owner, author(), replacement);
        context.commit();

        assertThat(relationshipService.find(context, firstId), nullValue());
        assertThat(second.getID(), not(equalTo(firstId)));
        assertThat(second.getRightItem(), equalTo(replacement));
        assertThat(author().getValue(), equalTo("Smith, John"));
        assertThat(author().getAuthority(), equalTo(replacement.getID().toString()));
    }

    @Test
    public void testPromotesRightEndpointWithMatchingPrimaryFieldOnBothSides() throws Exception {
        context.turnOffAuthorisationSystem();
        RelationshipConfigurationServiceImpl configurations = new DSpace().getServiceManager().getServiceByName(
            RelationshipConfigurationServiceImpl.class.getCanonicalName(), RelationshipConfigurationServiceImpl.class);
        Object original = ReflectionTestUtils.getField(configurations, "configurations");

        RelationshipTypeConfiguration twoSided = new RelationshipTypeConfiguration();
        twoSided.setId("test:two-sided-title");
        twoSided.setLeftEntityTypes(Set.of("Publication"));
        twoSided.setRightEntityTypes(Set.of("Person"));
        twoSided.setLeftPrimaryMetadataField("dc.title");
        twoSided.setRightPrimaryMetadataField("dc.title");
        Relationship created = null;
        ReflectionTestUtils.setField(configurations, "configurations", List.of(twoSided));
        try {
            MetadataValue personTitle = itemService.getMetadata(target, "dc", "title", null, Item.ANY).get(0);
            created = service.promoteResolvedAuthority(context, target, personTitle, owner);

            assertThat(created, notNullValue());
            assertThat(created.getLeftItem(), equalTo(owner));
            assertThat(created.getRightItem(), equalTo(target));
            assertThat(created.getRelationshipConfigKey(), equalTo(twoSided.getId()));
            assertThat(personTitle.getRelationship().getID(), equalTo(created.getID()));
            assertThat(configurations.getRelationshipConfig(context, created).getId(),
                equalTo(twoSided.getId()));
        } finally {
            try {
                if (created != null) {
                    // Clean up the temporary relationship before restoring the singleton's configuration.
                    service.detachRelationshipKeepMetadata(context, created);
                }
            } finally {
                ReflectionTestUtils.setField(configurations, "configurations", original);
            }
        }
    }

    @Test
    public void testForeignKeyDirectionDoesNotRequireMetadataToExist() throws Exception {
        context.turnOffAuthorisationSystem();
        Relationship relationship = relationshipService
            .createConfigBackedRelationship(context, owner, target, authorRelationshipConfiguration());
        context.commit();
        relationship = context.reloadEntity(relationship);
        assertThat(relationship, notNullValue());
        assertThat(metadataValueService.findByRelationship(context, relationship), hasSize(0));
        // Includes configured rows even in the path which excludes legacy tilted types.
        assertThat(relationshipService.findByItem(context, target, -1, -1, true), hasSize(1));
    }

    @Test
    public void testConfigBackedCreationWithWriteLeftAndReadBoth() throws Exception {
        context.turnOffAuthorisationSystem();

        EPerson user = EPersonBuilder.createEPerson(context)
                                     .withEmail("relationship-create-left@example.com")
                                     .withPassword("password")
                                     .build();

        AuthorizeService authorizeService = AuthorizeServiceFactory.getInstance().getAuthorizeService();
        authorizeService.removeAllPolicies(context, owner);
        authorizeService.removeAllPolicies(context, target);
        authorizeService.addPolicy(context, owner, Constants.WRITE, user);
        authorizeService.addPolicy(context, owner, Constants.READ, user);
        authorizeService.addPolicy(context, target, Constants.READ, user);

        context.setCurrentUser(user);
        context.restoreAuthSystemState();

        Relationship relationship = relationshipService
            .createConfigBackedRelationship(context, owner, target, authorRelationshipConfiguration());

        assertThat(relationship, notNullValue());
        assertThat(relationship.getLeftItem(), equalTo(owner));
        assertThat(relationship.getRightItem(), equalTo(target));
    }

    @Test
    public void testConfigBackedCreationWithWriteRightAndReadBoth() throws Exception {
        context.turnOffAuthorisationSystem();

        EPerson user = EPersonBuilder.createEPerson(context)
                                     .withEmail("relationship-create-right@example.com")
                                     .withPassword("password")
                                     .build();

        AuthorizeService authorizeService = AuthorizeServiceFactory.getInstance().getAuthorizeService();
        authorizeService.removeAllPolicies(context, owner);
        authorizeService.removeAllPolicies(context, target);
        authorizeService.addPolicy(context, owner, Constants.READ, user);
        authorizeService.addPolicy(context, target, Constants.WRITE, user);
        authorizeService.addPolicy(context, target, Constants.READ, user);

        context.setCurrentUser(user);
        context.restoreAuthSystemState();

        Relationship relationship = relationshipService
            .createConfigBackedRelationship(context, owner, target, authorRelationshipConfiguration());

        assertThat(relationship, notNullValue());
        assertThat(relationship.getLeftItem(), equalTo(owner));
        assertThat(relationship.getRightItem(), equalTo(target));
    }

    @Test
    public void testConfigBackedCreationWithReadOnBothItemsIsDenied() throws Exception {
        context.turnOffAuthorisationSystem();

        EPerson user = EPersonBuilder.createEPerson(context)
                                     .withEmail("relationship-create-read-only@example.com")
                                     .withPassword("password")
                                     .build();

        AuthorizeService authorizeService = AuthorizeServiceFactory.getInstance().getAuthorizeService();
        authorizeService.removeAllPolicies(context, owner);
        authorizeService.removeAllPolicies(context, target);
        authorizeService.addPolicy(context, owner, Constants.READ, user);
        authorizeService.addPolicy(context, target, Constants.READ, user);

        context.setCurrentUser(user);
        context.restoreAuthSystemState();

        assertThrows(AuthorizeException.class,
                     () -> relationshipService.createConfigBackedRelationship(context, owner, target,
                                                                              authorRelationshipConfiguration()));
    }

    @Test
    public void testConfigBackedCreationWithWriteLeftAndNoReadRightIsDenied() throws Exception {
        context.turnOffAuthorisationSystem();

        EPerson user = EPersonBuilder.createEPerson(context)
                                     .withEmail("relationship-create-no-read@example.com")
                                     .withPassword("password")
                                     .build();

        AuthorizeService authorizeService = AuthorizeServiceFactory.getInstance().getAuthorizeService();
        authorizeService.removeAllPolicies(context, owner);
        authorizeService.removeAllPolicies(context, target);
        authorizeService.addPolicy(context, owner, Constants.WRITE, user);
        authorizeService.addPolicy(context, owner, Constants.READ, user);

        context.setCurrentUser(user);
        context.restoreAuthSystemState();

        assertThrows(AuthorizeException.class,
                     () -> relationshipService.createConfigBackedRelationship(context, owner, target,
                                                                              authorRelationshipConfiguration()));
    }

    /**
     * Verify that the MetadataValue/Relationship owner invariant is enforced when a managed MetadataValue
     * is modified directly and persisted through Hibernate, without calling MetadataValueService.update().
     *
     * A MetadataValue linked to a Relationship must belong to one of that Relationship's endpoint Items.
     */
    @Test
    public void testUnrelatedMetadataOwnerIsRejectedOnHibernateFlush() throws Exception {
        context.turnOffAuthorisationSystem();

        // Create a valid relationship-backed MetadataValue owned by one of the relationship endpoints.
        MetadataValue value = author();
        service.promoteResolvedAuthority(context, owner, value, target);

        Item stranger = ItemBuilder.createItem(context, collection).withTitle("stranger").build();

        // Make the managed MetadataValue invalid without going through MetadataValueService.update().
        value.setDSpaceObject(stranger);

        // Hibernate dirty checking must not allow the invalid owner/relationship combination to be persisted.
        assertThrows(IllegalArgumentException.class, () -> context.commit());

        // The failed flush marks the transaction for rollback; recover the Context so test cleanup can proceed.
        context.rollback();
    }

    /**
     * Verify that the MetadataValue/Relationship owner invariant is enforced when a new MetadataValue
     * is persisted with a Relationship whose endpoints do not include the MetadataValue owner.
     *
     * This verifies the invariant during initial persistence, before the MetadataValue has been stored.
     */
    @Test
    public void testUnrelatedMetadataOwnerIsRejectedOnPersist() throws Exception {
        context.turnOffAuthorisationSystem();

        Relationship relationship = service.promoteResolvedAuthority(context, owner, author(), target);

        Item stranger = ItemBuilder.createItem(context, collection).withTitle("stranger").build();

        MetadataField field = ContentServiceFactory.getInstance().getMetadataFieldService()
                                                   .findByElement(context, "dc", "contributor", "author");

        // Create a new MetadataValue owned by an Item which is not an endpoint of the Relationship.
        MetadataValue value = metadataValueService.create(context, stranger, field);
        value.setValue("Other");
        value.setRelationship(relationship);

        // The invalid owner/relationship combination must be rejected when the new entity is persisted.
        assertThrows(IllegalArgumentException.class, () -> context.commit());

        // The failed persistence marks the transaction for rollback; recover the Context for test cleanup.
        context.rollback();
    }

    private MetadataValue author() {
        return itemService.getMetadata(owner, "dc", "contributor", "author", Item.ANY).get(0);
    }


    @Test
    public void testConfigBackedDeletionGeneratesMetadataEvents() throws Exception {
        context.turnOffAuthorisationSystem();

        Relationship relationship = service.promoteResolvedAuthority(context, owner, author(), target);
        context.getEvents().clear();

        relationshipService.delete(context, relationship);

        assertMetadataEvents(owner, 1);
        assertMetadataEvents(target, 1);
    }

    @Test
    public void testConfigBackedDeletionWithCopyFlagsGeneratesMetadataEvents() throws Exception {
        context.turnOffAuthorisationSystem();

        Relationship relationship = service.promoteResolvedAuthority(context, owner, author(), target);
        context.getEvents().clear();

        relationshipService.delete(context, relationship, false, false);

        assertMetadataEvents(owner, 1);
        assertMetadataEvents(target, 1);
    }

    @Test
    public void testRemovingRelationshipAndMetadataGeneratesMetadataEvents() throws Exception {
        context.turnOffAuthorisationSystem();

        Relationship relationship = service.promoteResolvedAuthority(context, owner, author(), target);
        context.getEvents().clear();

        service.removeRelationshipAndMetadata(context, relationship);

        assertMetadataEvents(owner, 1);
        assertMetadataEvents(target, 1);
    }

    @Test
    public void testItemDeletionGeneratesSurvivingEndpointEvent() throws Exception {
        context.turnOffAuthorisationSystem();

        service.promoteResolvedAuthority(context, owner, author(), target);
        context.getEvents().clear();

        itemService.delete(context, target);

        assertMetadataEvents(owner, 1);
    }

    @Test
    public void testCopyAllVirtualMetadataDeletionGeneratesSurvivingEndpointEvent() throws Exception {
        context.turnOffAuthorisationSystem();

        service.promoteResolvedAuthority(context, owner, author(), target);
        context.getEvents().clear();

        relationshipService.deleteMultipleRelationshipsCopyVirtualMetadata(
            context, new String[] { "all" }, target);

        assertMetadataEvents(owner, 1);
    }

    @Test
    public void testCopyConfiguredVirtualMetadataDeletionGeneratesSurvivingEndpointEvent() throws Exception {
        context.turnOffAuthorisationSystem();

        service.promoteResolvedAuthority(context, owner, author(), target);
        context.getEvents().clear();

        relationshipService.deleteMultipleRelationshipsCopyVirtualMetadata(
            context, new String[] { "configured" }, target);

        assertMetadataEvents(owner, 1);
    }

    @Test
    public void testDeletingMultipleRelationshipsUpdatesSurvivorOnce() throws Exception {
        context.turnOffAuthorisationSystem();

        Item secondTarget = ItemBuilder.createItem(context, collection)
                                       .withTitle("second person")
                                       .withEntityType("Person")
                                       .build();

        service.promoteResolvedAuthority(context, owner, author(), target);

        itemService.addMetadata(context, owner, "dc", "contributor", "author",
                                null, "Second Author");
        List<MetadataValue> authors =
            itemService.getMetadata(owner, "dc", "contributor", "author", Item.ANY);

        service.promoteResolvedAuthority(context, owner, authors.get(authors.size() - 1), secondTarget);
        context.getEvents().clear();

        itemService.delete(context, owner);

        assertMetadataEvents(target, 1);
        assertMetadataEvents(secondTarget, 1);
    }

    @Test
    public void testConfigBackedDeletionWithWriteOnOneEndpoint() throws Exception {
        context.turnOffAuthorisationSystem();

        Relationship relationship = service.promoteResolvedAuthority(context, owner, author(), target);

        EPerson user = EPersonBuilder.createEPerson(context)
                                     .withEmail("relationship-test@example.com")
                                     .withPassword("password")
                                     .build();

        AuthorizeService authorizeService = AuthorizeServiceFactory.getInstance().getAuthorizeService();
        authorizeService.addPolicy(context, owner, Constants.WRITE, user);

        context.setCurrentUser(user);
        context.restoreAuthSystemState();

        context.getEvents().clear();

        relationshipService.delete(context, relationship);

        assertMetadataEvents(owner, 1);
        assertMetadataEvents(target, 1);
    }

    @Test
    public void testItemDeletionWithoutWriteOnSurvivor() throws Exception {
        context.turnOffAuthorisationSystem();

        service.promoteResolvedAuthority(context, owner, author(), target);

        EPerson user = EPersonBuilder.createEPerson(context)
                                     .withEmail("item-deletion-test@example.com")
                                     .withPassword("password")
                                     .build();

        AuthorizeService authorizeService = AuthorizeServiceFactory.getInstance().getAuthorizeService();
        authorizeService.addPolicy(context, target, Constants.DELETE, user);
        authorizeService.addPolicy(context, target, Constants.REMOVE, user);
        authorizeService.addPolicy(context, target, Constants.WRITE, user);

        context.setCurrentUser(user);
        context.restoreAuthSystemState();

        context.getEvents().clear();

        itemService.delete(context, target);

        assertMetadataEvents(owner, 1);
    }

    @Test
    public void testCopyVirtualDeletionWithoutWriteOnSurvivor() throws Exception {
        context.turnOffAuthorisationSystem();

        Relationship relationship = service.promoteResolvedAuthority(context, owner, author(), target);

        EPerson user = EPersonBuilder.createEPerson(context)
                                     .withEmail("copy-virtual-test@example.com")
                                     .withPassword("password")
                                     .build();

        AuthorizeService authorizeService = AuthorizeServiceFactory.getInstance().getAuthorizeService();
        authorizeService.addPolicy(context, target, Constants.WRITE, user);

        context.setCurrentUser(user);
        context.restoreAuthSystemState();

        context.getEvents().clear();

        relationshipService.deleteMultipleRelationshipsCopyVirtualMetadata(
            context, new String[] { "configured" }, target);

        assertMetadataEvents(owner, 1);
    }

    private void assertMetadataEvents(Item item, long expectedCount) {
        long count = context.getEvents().stream()
                            .filter(event -> event.getEventType() == Event.MODIFY_METADATA)
                            .filter(event -> item.getID().equals(event.getSubjectID()))
                            .count();

        assertThat(count, is(expectedCount));
    }

    /**
     * Creates the relationship configuration used by the configuration-backed creation tests.
     */
    private RelationshipTypeConfiguration authorRelationshipConfiguration() throws SQLException {
        Relationship lookup = mock(Relationship.class);
        when(lookup.getRelationshipConfigKey()).thenReturn("authority:dc.contributor.author");
        RelationshipConfigurationService configurations = new DSpace().getServiceManager().getServiceByName(
            RelationshipConfigurationServiceImpl.class.getCanonicalName(), RelationshipConfigurationService.class);
        return configurations.getRelationshipConfig(context, lookup);
    }

}
