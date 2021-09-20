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
import org.dspace.authorize.AuthorizeException;
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

    private EPerson submitter;

    private EPerson anotherSubmitter;

    private EPerson collectionAdmin;

    private EPerson communityAdmin;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();

        context.turnOffAuthorisationSystem();

        communityAdmin = EPersonBuilder.createEPerson(context)
            .withEmail("communityAdmin@test.it")
            .build();

        submitter = EPersonBuilder.createEPerson(context)
            .withEmail("submitter@mail.it")
            .build();

        context.setCurrentUser(submitter);

        anotherSubmitter = EPersonBuilder.createEPerson(context)
            .withEmail("anotherSubmitter@mail.it")
            .build();

        parentCommunity = CommunityBuilder.createCommunity(context)
            .withName("Test community")
            .withAdminGroup(communityAdmin)
            .build();

        collectionAdmin = EPersonBuilder.createEPerson(context)
            .withEmail("collectionAdmin@test.it")
            .build();

        collection = CollectionBuilder.createCollection(context, parentCommunity)
            .withName("Test collection")
            .withEntityType("Publication")
            .withAdminGroup(collectionAdmin)
            .withSubmitterGroup(submitter, anotherSubmitter)
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

        assertThat(crisSecurityService.hasAccess(context, item, eperson, accessMode), is(false));
        assertThat(crisSecurityService.hasAccess(context, item, admin, accessMode), is(true));
        assertThat(crisSecurityService.hasAccess(context, item, owner, accessMode), is(false));
        assertThat(crisSecurityService.hasAccess(context, item, collectionAdmin, accessMode), is(false));
        assertThat(crisSecurityService.hasAccess(context, item, communityAdmin, accessMode), is(false));
        assertThat(crisSecurityService.hasAccess(context, item, submitter, accessMode), is(false));
        assertThat(crisSecurityService.hasAccess(context, item, anotherSubmitter, accessMode), is(false));

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

        assertThat(crisSecurityService.hasAccess(context, item, eperson, accessMode), is(false));
        assertThat(crisSecurityService.hasAccess(context, item, admin, accessMode), is(true));
        assertThat(crisSecurityService.hasAccess(context, item, owner, accessMode), is(true));
        assertThat(crisSecurityService.hasAccess(context, item, collectionAdmin, accessMode), is(false));
        assertThat(crisSecurityService.hasAccess(context, item, communityAdmin, accessMode), is(false));
        assertThat(crisSecurityService.hasAccess(context, item, submitter, accessMode), is(false));
        assertThat(crisSecurityService.hasAccess(context, item, anotherSubmitter, accessMode), is(false));
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

        assertThat(crisSecurityService.hasAccess(context, item, eperson, accessMode), is(false));
        assertThat(crisSecurityService.hasAccess(context, item, admin, accessMode), is(false));
        assertThat(crisSecurityService.hasAccess(context, item, owner, accessMode), is(true));
        assertThat(crisSecurityService.hasAccess(context, item, collectionAdmin, accessMode), is(false));
        assertThat(crisSecurityService.hasAccess(context, item, communityAdmin, accessMode), is(false));
        assertThat(crisSecurityService.hasAccess(context, item, submitter, accessMode), is(false));
        assertThat(crisSecurityService.hasAccess(context, item, anotherSubmitter, accessMode), is(false));
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
        assertThat(crisSecurityService.hasAccess(context, item, collectionAdmin, accessMode), is(false));
        assertThat(crisSecurityService.hasAccess(context, item, communityAdmin, accessMode), is(false));
        assertThat(crisSecurityService.hasAccess(context, item, submitter, accessMode), is(false));
        assertThat(crisSecurityService.hasAccess(context, item, anotherSubmitter, accessMode), is(false));

        assertThat(crisSecurityService.hasAccess(context, item, firstUser, accessMode), is(true));
        assertThat(crisSecurityService.hasAccess(context, item, secondUser, accessMode), is(true));
        assertThat(crisSecurityService.hasAccess(context, item, thirdUser, accessMode), is(true));
        assertThat(crisSecurityService.hasAccess(context, item, fourthUser, accessMode), is(false));

        when(accessMode.getItemMetadataFields()).thenReturn(List.of("dc.contributor.author", "dc.contributor.editor"));
        assertThat(crisSecurityService.hasAccess(context, item, thirdUser, accessMode), is(true));
        assertThat(crisSecurityService.hasAccess(context, item, fourthUser, accessMode), is(true));

    }

    @Test
    public void testHasAccessWithItemAdminConfig() throws SQLException, AuthorizeException {

        context.turnOffAuthorisationSystem();

        Item item = ItemBuilder.createItem(context, collection)
            .withTitle("Test item")
            .withCrisOwner("Owner", owner.getID().toString())
            .build();

        context.restoreAuthSystemState();

        AccessItemMode accessMode = buildAccessItemMode(CrisSecurity.ITEM_ADMIN);

        assertThat(crisSecurityService.hasAccess(context, item, eperson, accessMode), is(false));
        assertThat(crisSecurityService.hasAccess(context, item, admin, accessMode), is(true));
        assertThat(crisSecurityService.hasAccess(context, item, owner, accessMode), is(false));
        assertThat(crisSecurityService.hasAccess(context, item, collectionAdmin, accessMode), is(true));
        assertThat(crisSecurityService.hasAccess(context, item, communityAdmin, accessMode), is(true));
        assertThat(crisSecurityService.hasAccess(context, item, submitter, accessMode), is(false));
        assertThat(crisSecurityService.hasAccess(context, item, anotherSubmitter, accessMode), is(false));
    }

    @Test
    public void testHasAccessWithSubmitterConfig() throws SQLException, AuthorizeException {

        context.turnOffAuthorisationSystem();

        Item item = ItemBuilder.createItem(context, collection)
            .withTitle("Test item")
            .withCrisOwner("Owner", owner.getID().toString())
            .build();

        context.restoreAuthSystemState();

        AccessItemMode accessMode = buildAccessItemMode(CrisSecurity.SUBMITTER);

        assertThat(crisSecurityService.hasAccess(context, item, eperson, accessMode), is(false));
        assertThat(crisSecurityService.hasAccess(context, item, admin, accessMode), is(false));
        assertThat(crisSecurityService.hasAccess(context, item, owner, accessMode), is(false));
        assertThat(crisSecurityService.hasAccess(context, item, collectionAdmin, accessMode), is(false));
        assertThat(crisSecurityService.hasAccess(context, item, communityAdmin, accessMode), is(false));
        assertThat(crisSecurityService.hasAccess(context, item, submitter, accessMode), is(true));
        assertThat(crisSecurityService.hasAccess(context, item, anotherSubmitter, accessMode), is(false));
    }

    @Test
    public void testHasAccessWithSubmitterGroupConfig() throws SQLException, AuthorizeException {

        context.turnOffAuthorisationSystem();

        Item item = ItemBuilder.createItem(context, collection)
            .withTitle("Test item")
            .withCrisOwner("Owner", owner.getID().toString())
            .build();

        context.restoreAuthSystemState();

        AccessItemMode accessMode = buildAccessItemMode(CrisSecurity.SUBMITTER_GROUP);

        assertThat(crisSecurityService.hasAccess(context, item, eperson, accessMode), is(false));
        assertThat(crisSecurityService.hasAccess(context, item, admin, accessMode), is(false));
        assertThat(crisSecurityService.hasAccess(context, item, owner, accessMode), is(false));
        assertThat(crisSecurityService.hasAccess(context, item, collectionAdmin, accessMode), is(false));
        assertThat(crisSecurityService.hasAccess(context, item, communityAdmin, accessMode), is(false));
        assertThat(crisSecurityService.hasAccess(context, item, submitter, accessMode), is(true));
        assertThat(crisSecurityService.hasAccess(context, item, anotherSubmitter, accessMode), is(true));
    }

    @Test
    public void testHasAccessWithGroupConfig() throws SQLException, AuthorizeException {

        context.turnOffAuthorisationSystem();

        Group firstGroup = GroupBuilder.createGroup(context)
            .withName("Group 1")
            .build();

        Group secondGroup = GroupBuilder.createGroup(context)
            .withName("Group 2")
            .build();

        Group thirdGroup = GroupBuilder.createGroup(context)
            .withName("Group 3")
            .build();

        EPerson firstUser = EPersonBuilder.createEPerson(context)
            .withEmail("user@mail.it")
            .withGroupMembership(firstGroup)
            .build();

        EPerson secondUser = EPersonBuilder.createEPerson(context)
            .withEmail("user2@mail.it")
            .withGroupMembership(secondGroup)
            .build();

        EPerson thirdUser = EPersonBuilder.createEPerson(context)
            .withEmail("user3@mail.it")
            .withGroupMembership(thirdGroup)
            .build();

        EPerson fourthUser = EPersonBuilder.createEPerson(context)
            .withEmail("user4@mail.it")
            .withGroupMembership(thirdGroup)
            .build();

        Item item = ItemBuilder.createItem(context, collection)
            .withTitle("Test item")
            .withCrisOwner("Owner", owner.getID().toString())
            .build();

        context.restoreAuthSystemState();

        AccessItemMode accessMode = buildAccessItemMode(CrisSecurity.GROUP);
        when(accessMode.getGroups()).thenReturn(List.of("Group 1", thirdGroup.getID().toString()));

        assertThat(crisSecurityService.hasAccess(context, item, eperson, accessMode), is(false));
        assertThat(crisSecurityService.hasAccess(context, item, admin, accessMode), is(false));
        assertThat(crisSecurityService.hasAccess(context, item, owner, accessMode), is(false));
        assertThat(crisSecurityService.hasAccess(context, item, collectionAdmin, accessMode), is(false));
        assertThat(crisSecurityService.hasAccess(context, item, communityAdmin, accessMode), is(false));
        assertThat(crisSecurityService.hasAccess(context, item, submitter, accessMode), is(false));
        assertThat(crisSecurityService.hasAccess(context, item, anotherSubmitter, accessMode), is(false));

        assertThat(crisSecurityService.hasAccess(context, item, firstUser, accessMode), is(true));
        assertThat(crisSecurityService.hasAccess(context, item, secondUser, accessMode), is(false));
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
