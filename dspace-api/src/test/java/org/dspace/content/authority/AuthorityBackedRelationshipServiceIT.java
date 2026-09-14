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
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

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
import org.dspace.content.authority.service.AuthorityBackedRelationshipService;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.RelationshipService;
import org.dspace.utils.DSpace;
import org.junit.Before;
import org.junit.Test;

/**
 * Integration tests for the shared authority-backed relationship minting helper
 * ({@link AuthorityBackedRelationshipService#createRelationshipForResolvedAuthority}).
 *
 * Covers ticket 04 (shared minting helper).
 */
public class AuthorityBackedRelationshipServiceIT extends AbstractIntegrationTestWithDatabase {

    private AuthorityBackedRelationshipService authorityBackedRelationshipService;
    private RelationshipService relationshipService;
    private ItemService itemService;

    private Collection collection;
    private Item owner;
    private Item target;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();

        authorityBackedRelationshipService = new DSpace().getServiceManager()
            .getServiceByName(AuthorityBackedRelationshipServiceImpl.class.getCanonicalName(),
                AuthorityBackedRelationshipService.class);
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
            .withAuthor("Smith, John")
            .build();

        target = ItemBuilder.createItem(context, collection)
            .withTitle("target person")
            .build();

        context.restoreAuthSystemState();
    }

    @Test
    public void testMintsRelationshipAndStampsOwningValue() throws Exception {
        context.turnOffAuthorisationSystem();

        MetadataValue authorValue = getFirstAuthorValue(owner);
        assertThat(authorValue.getOwnedRelationshipId(), nullValue());
        String originalValue = authorValue.getValue();
        String originalAuthority = authorValue.getAuthority();

        Relationship relationship = authorityBackedRelationshipService
            .createRelationshipForResolvedAuthority(context, owner, authorValue, target);

        context.restoreAuthSystemState();

        assertThat(relationship, notNullValue());
        assertThat(relationship.getID(), notNullValue());
        // Side convention (D21): owner -> left, target -> right; type-less
        assertThat(relationship.getLeftItem(), equalTo(owner));
        assertThat(relationship.getRightItem(), equalTo(target));
        assertThat(relationship.getRelationshipType(), nullValue());

        // The owning metadata value now carries the relationship id
        assertThat(authorValue.getOwnedRelationshipId(), equalTo(relationship.getID()));
        // The helper never touches value or authority
        assertThat(authorValue.getValue(), equalTo(originalValue));
        assertThat(authorValue.getAuthority(), equalTo(originalAuthority));
    }

    @Test
    public void testSecondCallForSameValueIsNoOp() throws Exception {
        context.turnOffAuthorisationSystem();

        MetadataValue authorValue = getFirstAuthorValue(owner);

        Relationship first = authorityBackedRelationshipService
            .createRelationshipForResolvedAuthority(context, owner, authorValue, target);
        Relationship second = authorityBackedRelationshipService
            .createRelationshipForResolvedAuthority(context, owner, authorValue, target);

        context.restoreAuthSystemState();

        // Idempotent: the second call mints nothing, returns the existing link
        assertThat(second.getID(), equalTo(first.getID()));

        List<Relationship> ownerRels = relationshipService.findByItem(context, owner);
        long typeLessCount = ownerRels.stream().filter(r -> r.getRelationshipType() == null).count();
        assertThat(typeLessCount, equalTo(1L));
    }

    @Test
    public void testTwoDifferentValuesMintTwoRows() throws Exception {
        // Option B: two different mdvs on the same owner->target pair mint two rows.
        context.turnOffAuthorisationSystem();

        itemService.addMetadata(context, owner, "dc", "contributor", "editor", null, "Smith, John");
        itemService.update(context, owner);

        MetadataValue authorValue = getFirstAuthorValue(owner);
        MetadataValue editorValue =
            itemService.getMetadata(owner, "dc", "contributor", "editor", Item.ANY).get(0);

        Relationship authorRel = authorityBackedRelationshipService
            .createRelationshipForResolvedAuthority(context, owner, authorValue, target);
        Relationship editorRel = authorityBackedRelationshipService
            .createRelationshipForResolvedAuthority(context, owner, editorValue, target);

        context.restoreAuthSystemState();

        assertThat(authorRel.getID().equals(editorRel.getID()), equalTo(false));

        List<Relationship> ownerRels = relationshipService.findByItem(context, owner);
        long typeLessCount = ownerRels.stream().filter(r -> r.getRelationshipType() == null).count();
        assertThat(typeLessCount, equalTo(2L));
    }

    private MetadataValue getFirstAuthorValue(Item item) {
        return itemService.getMetadata(item, "dc", "contributor", "author", Item.ANY).get(0);
    }
}
