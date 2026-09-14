/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import java.util.List;
import java.util.stream.Collectors;

import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.RelationshipService;
import org.junit.Before;
import org.junit.Test;

/**
 * Integration tests for the type-less (authority-backed) relationship create path
 * ({@link RelationshipService#createTypeLessRelationship}) and the null-type guard on the
 * virtual-metadata read path.
 *
 * Covers tickets 02 (type-less create path) and 03 (null-type read guard).
 */
public class TypeLessRelationshipIT extends AbstractIntegrationTestWithDatabase {

    private RelationshipService relationshipService;
    private ItemService itemService;

    private Collection collection;
    private Item owner;
    private Item target;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();

        relationshipService = ContentServiceFactory.getInstance().getRelationshipService();
        itemService = ContentServiceFactory.getInstance().getItemService();

        context.turnOffAuthorisationSystem();

        Community community = CommunityBuilder.createCommunity(context)
            .withName("community")
            .build();

        collection = CollectionBuilder.createCollection(context, community)
            .withName("collection")
            .build();

        owner = ItemBuilder.createItem(context, collection)
            .withTitle("owner publication")
            .build();

        target = ItemBuilder.createItem(context, collection)
            .withTitle("target person")
            .build();

        context.restoreAuthSystemState();
    }

    @Test
    public void testCreateTypeLessRelationshipPersistsAndRoundTrips() throws Exception {
        context.turnOffAuthorisationSystem();
        Relationship relationship = relationshipService.createTypeLessRelationship(context, owner, target);
        context.restoreAuthSystemState();

        assertThat(relationship, notNullValue());
        assertThat(relationship.getID(), notNullValue());
        // Side convention (D21): owner -> left, target -> right
        assertThat(relationship.getLeftItem(), equalTo(owner));
        assertThat(relationship.getRightItem(), equalTo(target));
        // getRelationshipType() must return null without throwing
        assertThat(relationship.getRelationshipType(), nullValue());

        // Fetch by id
        Relationship byId = relationshipService.find(context, relationship.getID());
        assertThat(byId, notNullValue());
        assertThat(byId.getRelationshipType(), nullValue());

        // Fetch by item (both sides)
        List<Relationship> ownerRels = relationshipService.findByItem(context, owner);
        assertThat(
            ownerRels.stream().map(Relationship::getID).collect(Collectors.toList()),
            hasItem(relationship.getID())
        );
        List<Relationship> targetRels = relationshipService.findByItem(context, target);
        assertThat(
            targetRels.stream().map(Relationship::getID).collect(Collectors.toList()),
            hasItem(relationship.getID())
        );
    }

    @Test
    public void testTwoTypeLessRowsBetweenSameItemsAreDistinct() throws Exception {
        // Simulates author + editor of the same person: two type-less rows between the same owner/target.
        // The UNIQUE(left, type, right) constraint does not collapse them when type is NULL.
        context.turnOffAuthorisationSystem();
        Relationship first = relationshipService.createTypeLessRelationship(context, owner, target);
        Relationship second = relationshipService.createTypeLessRelationship(context, owner, target);
        context.restoreAuthSystemState();

        assertThat(first.getID(), notNullValue());
        assertThat(second.getID(), notNullValue());
        assertThat(first.getID(), equalTo(first.getID()));
        // Two distinct rows persisted
        assertThat(first.getID().equals(second.getID()), equalTo(false));

        List<Relationship> ownerRels = relationshipService.findByItem(context, owner);
        List<Integer> ids = ownerRels.stream()
            .filter(r -> r.getRelationshipType() == null)
            .map(Relationship::getID)
            .collect(Collectors.toList());
        assertThat(ids, hasItem(first.getID()));
        assertThat(ids, hasItem(second.getID()));
    }

    @Test
    public void testReadItemOwningTypeLessRelationshipDoesNotThrow() throws Exception {
        // Ticket 03: reading an item that owns a type-less relationship must not NPE on the
        // virtual-metadata read path, and its real stored metadata must be intact.
        context.turnOffAuthorisationSystem();

        Item publication = ItemBuilder.createItem(context, collection)
            .withTitle("A publication with a real author value")
            .withAuthor("Smith, John")
            .build();

        relationshipService.createTypeLessRelationship(context, publication, target);
        context.restoreAuthSystemState();

        // Reading the full metadata must not throw and must return the real stored metadata unchanged.
        List<MetadataValue> allMetadata = itemService.getMetadata(publication, Item.ANY, Item.ANY, Item.ANY, Item.ANY);
        assertThat(allMetadata, notNullValue());

        List<MetadataValue> authors =
            itemService.getMetadata(publication, "dc", "contributor", "author", Item.ANY);
        // Exactly the one real stored author, no virtual copy from the type-less row.
        assertThat(authors, hasSize(1));
        assertThat(authors.get(0).getValue(), equalTo("Smith, John"));
    }
}
