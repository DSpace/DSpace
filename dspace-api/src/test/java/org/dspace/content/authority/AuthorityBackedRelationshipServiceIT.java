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

import java.util.UUID;

import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.authority.service.AuthorityValueService;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.authority.service.AuthorityBackedRelationshipService;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.RelationshipService;
import org.dspace.core.Constants;
import org.dspace.utils.DSpace;
import org.junit.Before;
import org.junit.Test;

/**
 * Integration tests for the shared authority-backed relationship stamping helper
 * ({@link AuthorityBackedRelationshipService#markRelationshipForResolvedAuthority}).
 *
 * Covers ticket 04 (shared minting helper).
 *
 * @author Adamo Fapohunda (adamo.fapohunda at 4science.com)
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 */
public class AuthorityBackedRelationshipServiceIT extends AbstractIntegrationTestWithDatabase {

    private AuthorityBackedRelationshipService authorityBackedRelationshipService;
    private ItemService itemService;
    private RelationshipService relationshipService;

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
        itemService = ContentServiceFactory.getInstance().getItemService();
        relationshipService = ContentServiceFactory.getInstance().getRelationshipService();

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
    public void testStampsOwningValueWithRelationshipSides() throws Exception {
        context.turnOffAuthorisationSystem();

        MetadataValue authorValue = getFirstAuthorValue(owner);
        assertThat(authorValue.isRelationshipBacked(), equalTo(false));
        String originalValue = authorValue.getValue();

        boolean stamped = authorityBackedRelationshipService
            .markRelationshipForResolvedAuthority(context, owner, authorValue, target);

        context.restoreAuthSystemState();

        assertThat(stamped, equalTo(true));
        // Side convention (D21): owner -> left, target -> right
        assertThat(authorValue.isRelationshipBacked(), equalTo(true));
        assertThat(authorValue.getLeftItem(), equalTo(owner.getID()));
        assertThat(authorValue.getRightItem(), equalTo(target.getID()));
        // The helper never touches the display value
        assertThat(authorValue.getValue(), equalTo(originalValue));
        // After a successful mint the authority column is elided (single-store),
        // so the RAW column is null while the derived getter reconstitutes the
        // target UUID from right_id.
        assertThat(authorValue.getRawAuthority(), equalTo(null));
        assertThat(authorValue.getAuthority(), equalTo(target.getID().toString()));
    }

    @Test
    public void testSecondCallForSameValueIsNoOp() throws Exception {
        context.turnOffAuthorisationSystem();

        MetadataValue authorValue = getFirstAuthorValue(owner);

        boolean first = authorityBackedRelationshipService
            .markRelationshipForResolvedAuthority(context, owner, authorValue, target);
        boolean second = authorityBackedRelationshipService
            .markRelationshipForResolvedAuthority(context, owner, authorValue, target);

        context.restoreAuthSystemState();

        // Idempotent: the second call changes nothing
        assertThat(first, equalTo(true));
        assertThat(second, equalTo(false));
    }

    @Test
    public void testTwoDifferentValuesStampIndependently() throws Exception {
        // Option B: two different mdvs on the same owner->target pair are stamped independently.
        context.turnOffAuthorisationSystem();

        itemService.addMetadata(context, owner, "dc", "contributor", "editor", null, "Smith, John");
        itemService.update(context, owner);

        MetadataValue authorValue = getFirstAuthorValue(owner);
        MetadataValue editorValue =
            itemService.getMetadata(owner, "dc", "contributor", "editor", Item.ANY).get(0);

        boolean authorStamped = authorityBackedRelationshipService
            .markRelationshipForResolvedAuthority(context, owner, authorValue, target);
        boolean editorStamped = authorityBackedRelationshipService
            .markRelationshipForResolvedAuthority(context, owner, editorValue, target);

        context.restoreAuthSystemState();

        assertThat(authorStamped, equalTo(true));
        assertThat(editorStamped, equalTo(true));
        assertThat(authorValue.isRelationshipBacked(), equalTo(true));
        assertThat(editorValue.isRelationshipBacked(), equalTo(true));
    }

    @Test
    public void testIsValidTargetRejectsNonItemAuthorities() throws Exception {
        assertThat(authorityBackedRelationshipService.isValidTarget(context, null), equalTo(false));
        assertThat(authorityBackedRelationshipService.isValidTarget(context, ""), equalTo(false));
        assertThat(authorityBackedRelationshipService.isValidTarget(context, "   "), equalTo(false));
        assertThat(authorityBackedRelationshipService.isValidTarget(context, "ORCID::0000-0002-9079-593X"),
            equalTo(false));
        assertThat(authorityBackedRelationshipService
            .isValidTarget(context, AuthorityValueService.GENERATE + "Person"), equalTo(false));
        assertThat(authorityBackedRelationshipService
            .isValidTarget(context, AuthorityValueService.REFERENCE + "1234"), equalTo(false));
        assertThat(authorityBackedRelationshipService
            .isValidTarget(context, Constants.VIRTUAL_AUTHORITY_PREFIX + "12"), equalTo(false));
        // correct length, but not made of hex characters and dashes
        assertThat(authorityBackedRelationshipService
            .isValidTarget(context, "zzzzzzzz-zzzz-zzzz-zzzz-zzzzzzzzzzzz"), equalTo(false));
        // hex characters, wrong length
        assertThat(authorityBackedRelationshipService
            .isValidTarget(context, "0ef21bb6-3a2e-4f30-9a4e-4a3c4b0d1e"), equalTo(false));
        // well-formed UUID that names no item
        assertThat(authorityBackedRelationshipService
            .isValidTarget(context, UUID.randomUUID().toString()), equalTo(false));
    }

    @Test
    public void testIsValidTargetAcceptsAnExistingItemCaseInsensitively() throws Exception {
        assertThat(authorityBackedRelationshipService.isValidTarget(context, target.getID().toString()),
            equalTo(true));
        assertThat(authorityBackedRelationshipService
            .isValidTarget(context, target.getID().toString().toUpperCase()), equalTo(true));
    }

    @Test
    public void testReconcileCreatesTheRowForAnAuthorityNamingAnItem() throws Exception {
        context.turnOffAuthorisationSystem();

        MetadataValue authorValue = getFirstAuthorValue(owner);
        authorValue.setAuthority(target.getID().toString());
        authorValue.setConfidence(Choices.CF_ACCEPTED);

        boolean reconciled = authorityBackedRelationshipService
            .reconcileRelationshipForAuthority(context, owner, authorValue);
        context.commit();

        context.restoreAuthSystemState();

        assertThat(reconciled, equalTo(true));
        assertThat(authorValue.isRelationshipBacked(), equalTo(true));
        assertThat(authorValue.getLeftItem(), equalTo(owner.getID()));
        assertThat(authorValue.getRightItem(), equalTo(target.getID()));
        assertThat(relationshipService.findByItem(context, owner), hasSize(1));
        // the row is minted and the authority column elided, so the raw column is
        // null while the derived getter still returns the target UUID from right_id.
        assertThat(authorValue.getRawAuthority(), equalTo(null));
        assertThat(authorValue.getAuthority(), equalTo(target.getID().toString()));
    }

    @Test
    public void testReconcileRepointsTheRowWhenTheAuthorityChanges() throws Exception {
        context.turnOffAuthorisationSystem();

        Item otherTarget = ItemBuilder.createItem(context, collection)
            .withTitle("other person")
            .build();

        MetadataValue authorValue = getFirstAuthorValue(owner);
        authorValue.setAuthority(target.getID().toString());
        authorValue.setConfidence(Choices.CF_ACCEPTED);
        itemService.update(context, owner);
        assertThat(authorValue.getRightItem(), equalTo(target.getID()));

        authorValue.setAuthority(otherTarget.getID().toString());
        itemService.update(context, owner);
        context.commit();

        context.restoreAuthSystemState();

        assertThat(authorValue.isRelationshipBacked(), equalTo(true));
        assertThat(authorValue.getLeftItem(), equalTo(owner.getID()));
        assertThat(authorValue.getRightItem(), equalTo(otherTarget.getID()));
        assertThat(relationshipService.findByItem(context, owner), hasSize(1));
        assertThat(authorValue.getRawAuthority(), equalTo(null));
        assertThat(authorValue.getAuthority(), equalTo(otherTarget.getID().toString()));
    }

    @Test
    public void testReconcileIsNoOpWhenTheAuthorityStillNamesTheSameItem() throws Exception {
        context.turnOffAuthorisationSystem();

        MetadataValue authorValue = getFirstAuthorValue(owner);
        authorValue.setAuthority(target.getID().toString());
        authorValue.setConfidence(Choices.CF_ACCEPTED);
        itemService.update(context, owner);
        // After the first update the row is minted and the column elided (raw null, right_id set).
        assertThat(authorValue.getRawAuthority(), equalTo(null));
        assertThat(authorValue.getRightItem(), equalTo(target.getID()));

        // reconciling an already-elided, in-sync value is a no-op —
        // the raw-null column must not be misread as "authority names nothing".
        boolean reconciled = authorityBackedRelationshipService
            .reconcileRelationshipForAuthority(context, owner, authorValue);
        context.commit();

        context.restoreAuthSystemState();

        assertThat(reconciled, equalTo(false));
        assertThat(authorValue.getRightItem(), equalTo(target.getID()));
        assertThat(authorValue.getAuthority(), equalTo(target.getID().toString()));
        assertThat(relationshipService.findByItem(context, owner), hasSize(1));
    }

    @Test
    public void testReconcileDeletesTheRowWhenTheAuthorityIsCleared() throws Exception {
        context.turnOffAuthorisationSystem();

        MetadataValue authorValue = getFirstAuthorValue(owner);
        authorValue.setAuthority(target.getID().toString());
        authorValue.setConfidence(Choices.CF_ACCEPTED);
        itemService.update(context, owner);
        assertThat(authorValue.isRelationshipBacked(), equalTo(true));

        authorValue.setAuthority(null);
        authorValue.setConfidence(Choices.CF_UNSET);
        itemService.update(context, owner);
        context.commit();

        context.restoreAuthSystemState();

        assertThat(authorValue.isRelationshipBacked(), equalTo(false));
        assertThat(relationshipService.findByItem(context, owner), hasSize(0));
    }

    @Test
    public void testReconcileDeletesTheRowWhenTheAuthorityBecomesAReferenceToken() throws Exception {
        context.turnOffAuthorisationSystem();

        MetadataValue authorValue = getFirstAuthorValue(owner);
        authorValue.setAuthority(target.getID().toString());
        authorValue.setConfidence(Choices.CF_ACCEPTED);
        itemService.update(context, owner);
        assertThat(authorValue.isRelationshipBacked(), equalTo(true));

        authorValue.setAuthority(AuthorityValueService.REFERENCE + "ORCID::0000-0002-9079-593X");
        authorValue.setConfidence(Choices.CF_UNSET);
        itemService.update(context, owner);
        context.commit();

        context.restoreAuthSystemState();

        assertThat(authorValue.isRelationshipBacked(), equalTo(false));
        assertThat(relationshipService.findByItem(context, owner), hasSize(0));
    }

    @Test
    public void testReconcileIgnoresNonItemOwners() throws Exception {
        context.turnOffAuthorisationSystem();

        MetadataValue authorValue = getFirstAuthorValue(owner);
        authorValue.setAuthority(target.getID().toString());
        authorValue.setConfidence(Choices.CF_ACCEPTED);

        boolean reconciled = authorityBackedRelationshipService
            .reconcileRelationshipForAuthority(context, collection, authorValue);

        context.restoreAuthSystemState();

        assertThat(reconciled, equalTo(false));
        assertThat(authorValue.isRelationshipBacked(), equalTo(false));
    }

    private MetadataValue getFirstAuthorValue(Item item) {
        return itemService.getMetadata(item, "dc", "contributor", "author", Item.ANY).get(0);
    }
}
