/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.security;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.SQLException;
import java.util.List;

import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.EPersonBuilder;
import org.dspace.builder.GroupBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.security.service.CrisSecurityService;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.utils.DSpace;
import org.junit.Before;
import org.junit.Test;

/**
 * Integration tests for {@link CrisSecurityService}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class CrisSecurityServiceIT extends AbstractIntegrationTestWithDatabase {

    private CrisSecurityService crisSecurityService = getCrisSecurityService();

    private Collection collection;

    private EPerson owner;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();

        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
            .withName("Test community")
            .build();

        collection = CollectionBuilder.createCollection(context, parentCommunity)
            .withName("Test collection")
            .withEntityType("Publication")
            .build();

        owner = EPersonBuilder.createEPerson(context)
            .withEmail("owner@mail.it")
            .build();

        context.restoreAuthSystemState();
    }

    @Test
    public void testHasAccessWithAdminConfig() throws SQLException {

        context.turnOffAuthorisationSystem();

        Item item = ItemBuilder.createItem(context, collection)
            .withTitle("Test item")
            .build();

        context.restoreAuthSystemState();

        AccessItemMode accessMode = buildAccessItemMode(CrisSecurity.ADMIN);

        boolean hasAccess = crisSecurityService.hasAccess(context, item, eperson, accessMode);
        assertThat(hasAccess, is(false));

        boolean hasAdminAccess = crisSecurityService.hasAccess(context, item, admin, accessMode);
        assertThat(hasAdminAccess, is(true));

        boolean hasOwnerAccess = crisSecurityService.hasAccess(context, item, owner, accessMode);
        assertThat(hasOwnerAccess, is(false));
    }

    @Test
    public void testHasAccessWithAdminOwnerConfig() throws SQLException {

        context.turnOffAuthorisationSystem();

        Item item = ItemBuilder.createItem(context, collection)
            .withTitle("Test item")
            .withCrisOwner("Owner", owner.getID().toString())
            .build();

        context.restoreAuthSystemState();

        AccessItemMode accessMode = buildAccessItemMode(CrisSecurity.ADMIN_OWNER);

        boolean hasAccess = crisSecurityService.hasAccess(context, item, eperson, accessMode);
        assertThat(hasAccess, is(false));

        boolean hasAdminAccess = crisSecurityService.hasAccess(context, item, admin, accessMode);
        assertThat(hasAdminAccess, is(true));

        boolean hasOwnerAccess = crisSecurityService.hasAccess(context, item, owner, accessMode);
        assertThat(hasOwnerAccess, is(true));
    }

    @Test
    public void testHasAccessWithOwnerConfig() throws SQLException {

        context.turnOffAuthorisationSystem();

        Item item = ItemBuilder.createItem(context, collection)
            .withTitle("Test item")
            .withCrisOwner("Owner", owner.getID().toString())
            .build();

        context.restoreAuthSystemState();

        AccessItemMode accessMode = buildAccessItemMode(CrisSecurity.OWNER);

        boolean hasAccess = crisSecurityService.hasAccess(context, item, eperson, accessMode);
        assertThat(hasAccess, is(false));

        boolean hasAdminAccess = crisSecurityService.hasAccess(context, item, admin, accessMode);
        assertThat(hasAdminAccess, is(false));

        boolean hasOwnerAccess = crisSecurityService.hasAccess(context, item, owner, accessMode);
        assertThat(hasOwnerAccess, is(true));
    }

    @Test
    public void testHasAccessWithCustomConfig() throws SQLException {

        context.turnOffAuthorisationSystem();

        EPerson firstUser = EPersonBuilder.createEPerson(context)
            .withEmail("user@mail.it")
            .build();

        EPerson secondUser = EPersonBuilder.createEPerson(context)
            .withEmail("user2@mail.it")
            .build();

        EPerson thirdUser = EPersonBuilder.createEPerson(context)
            .withEmail("user3@mail.it")
            .build();

        EPerson fourthUser = EPersonBuilder.createEPerson(context)
            .withEmail("user4@mail.it")
            .build();

        Item author = ItemBuilder.createItem(context, collection)
            .withTitle("Author")
            .withCrisOwner(thirdUser)
            .build();

        Item editor = ItemBuilder.createItem(context, collection)
            .withTitle("Editor")
            .withCrisOwner(fourthUser)
            .build();

        Group group = GroupBuilder.createGroup(context)
            .withName("Group")
            .addMember(secondUser)
            .build();

        Item item = ItemBuilder.createItem(context, collection)
            .withTitle("Test item")
            .withCrisOwner("Owner", owner.getID().toString())
            .withAuthor("Author", author.getID().toString())
            .withEditor("Editor", editor.getID().toString())
            .withEditor("Another editor", "5260f7f1-f583-4a7a-86e5-25db93a29240")
            .withCrisPolicyEPerson("First User", firstUser.getID().toString())
            .withCrisPolicyGroup("Second User", group.getID().toString())
            .build();

        context.restoreAuthSystemState();

        AccessItemMode accessMode = buildAccessItemMode(CrisSecurity.CUSTOM);
        when(accessMode.getUserMetadataFields()).thenReturn(List.of("cris.policy.eperson"));
        when(accessMode.getGroupMetadataFields()).thenReturn(List.of("cris.policy.group"));
        when(accessMode.getItemMetadataFields()).thenReturn(List.of("dc.contributor.author"));

        assertThat(crisSecurityService.hasAccess(context, item, eperson, accessMode), is(false));
        assertThat(crisSecurityService.hasAccess(context, item, admin, accessMode), is(false));
        assertThat(crisSecurityService.hasAccess(context, item, owner, accessMode), is(false));
        assertThat(crisSecurityService.hasAccess(context, item, firstUser, accessMode), is(true));
        assertThat(crisSecurityService.hasAccess(context, item, secondUser, accessMode), is(true));
        assertThat(crisSecurityService.hasAccess(context, item, thirdUser, accessMode), is(true));
        assertThat(crisSecurityService.hasAccess(context, item, fourthUser, accessMode), is(false));

        when(accessMode.getItemMetadataFields()).thenReturn(List.of("dc.contributor.author", "dc.contributor.editor"));
        assertThat(crisSecurityService.hasAccess(context, item, thirdUser, accessMode), is(true));
        assertThat(crisSecurityService.hasAccess(context, item, fourthUser, accessMode), is(true));

    }

    private AccessItemMode buildAccessItemMode(CrisSecurity security) {
        AccessItemMode mode = mock(AccessItemMode.class);
        when(mode.getSecurity()).thenReturn(security);
        return mode;
    }

    private CrisSecurityService getCrisSecurityService() {
        return new DSpace().getServiceManager().getApplicationContext().getBean(CrisSecurityService.class);
    }
}
