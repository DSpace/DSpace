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
            .withRelationshipType("Publication")
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

        EPerson author = EPersonBuilder.createEPerson(context)
            .withEmail("author@mail.it")
            .build();

        EPerson editor = EPersonBuilder.createEPerson(context)
            .withEmail("editor@mail.it")
            .build();

        Group group = GroupBuilder.createGroup(context)
            .addMember(editor)
            .build();

        Item item = ItemBuilder.createItem(context, collection)
            .withTitle("Test item")
            .withCrisOwner("Owner", owner.getID().toString())
            .withAuthor("Walter White", author.getID().toString())
            .withEditor("Editor", group.getID().toString())
            .build();

        context.restoreAuthSystemState();

        AccessItemMode accessMode = buildAccessItemMode(CrisSecurity.CUSTOM);
        when(accessMode.getUsers()).thenReturn(List.of("dc.contributor.author"));
        when(accessMode.getGroups()).thenReturn(List.of("dc.contributor.editor"));

        boolean hasAccess = crisSecurityService.hasAccess(context, item, eperson, accessMode);
        assertThat(hasAccess, is(false));

        boolean hasAdminAccess = crisSecurityService.hasAccess(context, item, admin, accessMode);
        assertThat(hasAdminAccess, is(false));

        boolean hasOwnerAccess = crisSecurityService.hasAccess(context, item, owner, accessMode);
        assertThat(hasOwnerAccess, is(false));

        boolean hasAuthorAccess = crisSecurityService.hasAccess(context, item, author, accessMode);
        assertThat(hasAuthorAccess, is(true));

        boolean hasEditorAccess = crisSecurityService.hasAccess(context, item, editor, accessMode);
        assertThat(hasEditorAccess, is(true));
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
