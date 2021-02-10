/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authority.service;

import static org.dspace.app.matcher.MetadataValueMatcher.with;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;

import java.sql.SQLException;

import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.authority.service.impl.ItemReferenceResolverServiceImpl;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.utils.DSpace;
import org.junit.Before;
import org.junit.Test;

/**
 * Integration tests for {@link ItemReferenceResolverServiceImpl}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class ItemReferenceResolverServiceIT extends AbstractIntegrationTestWithDatabase {

    private ItemReferenceResolverService itemReferenceResolverService;

    private Collection collection;

    @Before
    public void setup() {
        this.itemReferenceResolverService = new DSpace().getSingletonService(ItemReferenceResolverService.class);

        context.turnOffAuthorisationSystem();

        this.parentCommunity = CommunityBuilder.createCommunity(context)
            .withName("Community")
            .build();

        this.collection = CollectionBuilder.createCollection(context, parentCommunity)
            .withName("Collection")
            .build();

        context.restoreAuthSystemState();
    }

    @Test
    public void testResolveReferencesWithOrcid() throws SQLException {

        context.turnOffAuthorisationSystem();

        Item firstItem = ItemBuilder.createItem(context, collection)
            .withTitle("First Item")
            .withRelationshipType("Publication")
            .withAuthor("Author", formatWillBeReferencedAuthority("ORCID", "0000-0002-1825-0097"))
            .build();

        Item secondItem = ItemBuilder.createItem(context, collection)
            .withTitle("Second Item")
            .withRelationshipType("Publication")
            .withAuthor("Author", formatWillBeReferencedAuthority("ORCID", "0000-0002-1825-0097"))
            .build();

        Item itemWithOrcid = ItemBuilder.createItem(context, collection)
            .withTitle("Author")
            .withRelationshipType("Person")
            .withOrcidIdentifier("0000-0002-1825-0097")
            .build();

        context.commit();
        context.restoreAuthSystemState();

        firstItem = context.reloadEntity(firstItem);
        assertThat(firstItem.getMetadata(), hasItem(with("dc.contributor.author", "Author", null,
            formatWillBeReferencedAuthority("ORCID", "0000-0002-1825-0097"), 0, 600)));

        secondItem = context.reloadEntity(secondItem);
        assertThat(secondItem.getMetadata(), hasItem(with("dc.contributor.author", "Author", null,
            formatWillBeReferencedAuthority("ORCID", "0000-0002-1825-0097"), 0, 600)));

        itemReferenceResolverService.resolveReferences(context, itemWithOrcid);

        firstItem = context.reloadEntity(firstItem);
        assertThat(firstItem.getMetadata(), hasItem(with("dc.contributor.author", "Author", null,
            itemWithOrcid.getID().toString(), 0, 600)));

        secondItem = context.reloadEntity(secondItem);
        assertThat(secondItem.getMetadata(), hasItem(with("dc.contributor.author", "Author", null,
            itemWithOrcid.getID().toString(), 0, 600)));
    }

    private String formatWillBeReferencedAuthority(String authorityPrefix, String value) {
        return AuthorityValueService.REFERENCE + authorityPrefix + "::" + value;
    }
}
