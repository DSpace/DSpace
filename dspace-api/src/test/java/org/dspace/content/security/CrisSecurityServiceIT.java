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
import org.dspace.content.authority.factory.ContentAuthorityServiceFactory;
import org.dspace.content.authority.service.ChoiceAuthorityService;
import org.dspace.content.authority.service.MetadataAuthorityService;
import org.dspace.content.logic.Filter;
import org.dspace.content.logic.LogicalStatementException;
import org.dspace.content.security.service.CrisSecurityService;
import org.dspace.core.Context;
import org.dspace.core.factory.CoreServiceFactory;
import org.dspace.core.service.PluginService;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
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

    private ConfigurationService configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();


    private PluginService pluginService = CoreServiceFactory.getInstance().getPluginService();

    private ChoiceAuthorityService choiceAuthorityService = ContentAuthorityServiceFactory
        .getInstance().getChoiceAuthorityService();

    private MetadataAuthorityService metadataAuthorityService = ContentAuthorityServiceFactory
        .getInstance().getMetadataAuthorityService();

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
    public void testHasAccessWithAdminOrOwnerConfig() throws SQLException {

        context.turnOffAuthorisationSystem();

        Item item = ItemBuilder.createItem(context, collection)
            .withTitle("Test item")
            .withDspaceObjectOwner("Owner", owner.getID().toString())
            .build();

        context.restoreAuthSystemState();

        AccessItemMode accessMode = buildAccessItemMode(CrisSecurity.ADMIN, CrisSecurity.OWNER);

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
            .withDspaceObjectOwner("Owner", owner.getID().toString())
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
    public void testHasAccessWithCustomConfig() throws Exception {

        choiceAuthorityService.getChoiceAuthoritiesNames();
        pluginService.clearNamedPluginClasses();


        configurationService.setProperty("plugin.named.org.dspace.content.authority.ChoiceAuthority",
                                         new String[] {
                                             "org.dspace.content.authority.ItemAuthority = AuthorAuthority",
                                             "org.dspace.content.authority.OrcidAuthority = EditorAuthority",
                                             "org.dspace.content.authority.EPersonAuthority = EPersonAuthority",
                                             "org.dspace.content.authority.GroupAuthority = GroupAuthority"
                                         });
        configurationService.setProperty("cris.ItemAuthority.OrgUnitAuthority.entityType", "OrgUnit");
        configurationService.setProperty("cris.ItemAuthority.AuthorAuthority.entityType", "Person");
        configurationService.setProperty("choices.plugin.dc.contributor.author", "AuthorAuthority");
        configurationService.setProperty("choices.presentation.dc.contributor.author", "suggest");
        configurationService.setProperty("authority.controlled.dc.contributor.author", "true");

        configurationService.setProperty("choices.plugin.dc.contributor.editor", "EditorAuthority");
        configurationService.setProperty("choices.presentation.dc.contributor.editor", "suggest");
        configurationService.setProperty("authority.controlled.dc.contributor.editor", "true");
        configurationService.setProperty("cris.ItemAuthority.EditorAuthority.entityType", "Person");

        configurationService.setProperty("choices.plugin.dspace.policy.eperson", "EPersonAuthority");
        configurationService.setProperty("cchoices.presentation.dspace.policy.eperson", "suggest");
        configurationService.setProperty("authority.controlled.dspace.policy.eperson", "true");

        configurationService.setProperty("choices.plugin.dspace.policy.group", "GroupAuthority");
        configurationService.setProperty("cchoices.presentation.dspace.policy.group", "suggest");
        configurationService.setProperty("authority.controlled.dspace.policy.group", "true");

        choiceAuthorityService.clearCache();
        metadataAuthorityService.clearCache();

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
            .withDspaceObjectOwner(thirdUser)
            .build();

        Item editor = ItemBuilder.createItem(context, collection)
            .withTitle("Editor")
            .withDspaceObjectOwner(fourthUser)
            .build();

        Group group = GroupBuilder.createGroup(context)
            .withName("Group")
            .addMember(secondUser)
            .build();

        Item item = ItemBuilder.createItem(context, collection)
            .withTitle("Test item")
            .withDspaceObjectOwner("Owner", owner.getID().toString())
            .withAuthor("Author", author.getID().toString())
            .withEditor("Editor", editor.getID().toString())
            .withEditor("Another editor", "5260f7f1-f583-4a7a-86e5-25db93a29240")
            .withPolicyEPerson("First User", firstUser.getID().toString())
            .withPolicyGroup("Second User", group.getID().toString())
            .build();

        context.restoreAuthSystemState();

        AccessItemMode accessMode = buildAccessItemMode(CrisSecurity.CUSTOM);
        when(accessMode.getUserMetadataFields()).thenReturn(List.of("dspace.policy.eperson"));
        when(accessMode.getGroupMetadataFields()).thenReturn(List.of("dspace.policy.group"));
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
    public void testUserHasAccessItemWithoutAssignedGroup() throws SQLException {

        context.turnOffAuthorisationSystem();

        EPerson ePerson = EPersonBuilder.createEPerson(context)
            .withEmail("user@mail.it")
            .build();

        Item item = ItemBuilder.createItem(context, collection)
            .withTitle("Test item")
            .withAuthor("Author")
            .build();

        context.restoreAuthSystemState();

        AccessItemMode accessMode = buildAccessItemMode(CrisSecurity.GROUP);
        when(accessMode.getGroups()).thenReturn(List.of(Group.ANONYMOUS));

        assertThat(crisSecurityService.hasAccess(context, item, eperson, accessMode), is(true));
    }

    @Test
    public void testHasAccessWithItemAdminConfig() throws SQLException, AuthorizeException {

        context.turnOffAuthorisationSystem();

        Item item = ItemBuilder.createItem(context, collection)
            .withTitle("Test item")
            .withDspaceObjectOwner("Owner", owner.getID().toString())
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
            .withDspaceObjectOwner("Owner", owner.getID().toString())
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
            .withDspaceObjectOwner("Owner", owner.getID().toString())
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
    public void testHasAccessWithGroupChildOfResearchersConfig() throws SQLException {
        context.turnOffAuthorisationSystem();
        Group researchersMainGroup = GroupBuilder.createGroup(context)
                .withName("Researchers")
                .build();
        Group researcherSubGroup = GroupBuilder.createGroup(context)
                .withName("Researcher")
                .withParent(researchersMainGroup)
                .build();
        EPerson firstUser = EPersonBuilder.createEPerson(context)
                .withEmail("user@mail.it")
                .withGroupMembership(researcherSubGroup)
                .build();
        Item item = ItemBuilder.createItem(context, collection)
                .withTitle("Test item")
                .withDspaceObjectOwner("Owner", owner.getID().toString())
                //.withCrisOwner("Owner", owner.getID().toString())
                .build();
        context.restoreAuthSystemState();
        AccessItemMode accessMode = buildAccessItemMode(CrisSecurity.GROUP);
        when(accessMode.getGroups()).thenReturn(List.of("Researcher"));
        assertThat(crisSecurityService.hasAccess(context, item, firstUser, accessMode), is(true));
        assertThat(crisSecurityService.hasAccess(context, item, eperson, accessMode), is(false));
        assertThat(crisSecurityService.hasAccess(context, item, owner, accessMode), is(false));
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
            .withDspaceObjectOwner("Owner", owner.getID().toString())
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

    @Test
    public void testHasAccessWithGroupConfigAndAdditionalFilter() throws SQLException, AuthorizeException {

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
                               .withDspaceObjectOwner("Owner", owner.getID().toString())
                               .build();

        Item itemNotAccessible = ItemBuilder.createItem(context, collection)
                               .withTitle("Test item not accessible")
                               .withDspaceObjectOwner("Owner", owner.getID().toString())
                               .build();

        context.restoreAuthSystemState();

        AccessItemMode accessMode = buildAccessItemMode(CrisSecurity.GROUP);
        when(accessMode.getGroups()).thenReturn(List.of("Group 1", thirdGroup.getID().toString()));
        // filter valid only on first item
        when(accessMode.getAdditionalFilter()).thenReturn(new Filter() {
            @Override
            public boolean getResult(Context context, Item item) throws LogicalStatementException {
                return item.getName().equals("Test item");
            }

            @Override
            public String getName() {
                return null;
            }

            @Override
            public void setBeanName(String s) {}
        });

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

        assertThat(crisSecurityService.hasAccess(context, itemNotAccessible, firstUser, accessMode), is(false));
        assertThat(crisSecurityService.hasAccess(context, itemNotAccessible, secondUser, accessMode), is(false));
        assertThat(crisSecurityService.hasAccess(context, itemNotAccessible, thirdUser, accessMode), is(false));
        assertThat(crisSecurityService.hasAccess(context, itemNotAccessible, fourthUser, accessMode), is(false));
    }

    private AccessItemMode buildAccessItemMode(CrisSecurity... securities) {
        AccessItemMode mode = mock(AccessItemMode.class);
        when(mode.getSecurities()).thenReturn(List.of(securities));
        return mode;
    }

    @Test
    public void testHasAccessWithAllConfig() throws SQLException {

        context.turnOffAuthorisationSystem();

        Item item = ItemBuilder.createItem(context, collection)
            .withTitle("Test item")
            .build();

        context.restoreAuthSystemState();

        AccessItemMode accessMode = buildAccessItemMode(CrisSecurity.ALL);

        assertThat(crisSecurityService.hasAccess(context, item, eperson, accessMode), is(true));
        assertThat(crisSecurityService.hasAccess(context, item, admin, accessMode), is(true));
        assertThat(crisSecurityService.hasAccess(context, item, owner, accessMode), is(true));
        assertThat(crisSecurityService.hasAccess(context, item, collectionAdmin, accessMode), is(true));
        assertThat(crisSecurityService.hasAccess(context, item, communityAdmin, accessMode), is(true));
        assertThat(crisSecurityService.hasAccess(context, item, submitter, accessMode), is(true));
        assertThat(crisSecurityService.hasAccess(context, item, anotherSubmitter, accessMode), is(true));
        assertThat(crisSecurityService.hasAccess(context, item, null, accessMode), is(true));
    }

    @Test
    public void testHasAccessWithNoneConfig() throws SQLException {

        context.turnOffAuthorisationSystem();

        Item item = ItemBuilder.createItem(context, collection)
            .withTitle("Test item")
            .build();

        context.restoreAuthSystemState();

        AccessItemMode accessMode = buildAccessItemMode(CrisSecurity.NONE);

        assertThat(crisSecurityService.hasAccess(context, item, eperson, accessMode), is(false));
        assertThat(crisSecurityService.hasAccess(context, item, admin, accessMode), is(false));
        assertThat(crisSecurityService.hasAccess(context, item, owner, accessMode), is(false));
        assertThat(crisSecurityService.hasAccess(context, item, collectionAdmin, accessMode), is(false));
        assertThat(crisSecurityService.hasAccess(context, item, communityAdmin, accessMode), is(false));
        assertThat(crisSecurityService.hasAccess(context, item, submitter, accessMode), is(false));
        assertThat(crisSecurityService.hasAccess(context, item, anotherSubmitter, accessMode), is(false));
        assertThat(crisSecurityService.hasAccess(context, item, null, accessMode), is(false));
    }

    @Test
    public void testHasAccessWithAnonymousUser() throws SQLException {

        context.turnOffAuthorisationSystem();

        Item item = ItemBuilder.createItem(context, collection)
            .withTitle("Test item")
            .withDspaceObjectOwner("Owner", owner.getID().toString())
            .build();

        context.restoreAuthSystemState();

        assertThat(crisSecurityService.hasAccess(context, item, null,
            buildAccessItemMode(CrisSecurity.ADMIN)), is(false));
        assertThat(crisSecurityService.hasAccess(context, item, null,
            buildAccessItemMode(CrisSecurity.ITEM_ADMIN)), is(false));
        assertThat(crisSecurityService.hasAccess(context, item, null,
            buildAccessItemMode(CrisSecurity.OWNER)), is(false));
        assertThat(crisSecurityService.hasAccess(context, item, null,
            buildAccessItemMode(CrisSecurity.SUBMITTER)), is(false));
        assertThat(crisSecurityService.hasAccess(context, item, null,
            buildAccessItemMode(CrisSecurity.SUBMITTER_GROUP)), is(false));

        AccessItemMode groupMode = buildAccessItemMode(CrisSecurity.GROUP);
        when(groupMode.getGroups()).thenReturn(List.of("Group 1"));
        assertThat(crisSecurityService.hasAccess(context, item, null, groupMode), is(false));

        AccessItemMode customMode = buildAccessItemMode(CrisSecurity.CUSTOM);
        when(customMode.getUserMetadataFields()).thenReturn(List.of("dc.contributor.author"));
        when(customMode.getGroupMetadataFields()).thenReturn(List.of("dspace.policy.group"));
        when(customMode.getItemMetadataFields()).thenReturn(List.of("dc.contributor.author"));
        assertThat(crisSecurityService.hasAccess(context, item, null, customMode), is(false));
    }

    @Test
    public void testHasAccessWithCustomConfigAndAdditionalFilter() throws Exception {

        choiceAuthorityService.getChoiceAuthoritiesNames();
        pluginService.clearNamedPluginClasses();

        configurationService.setProperty("plugin.named.org.dspace.content.authority.ChoiceAuthority",
                                         new String[] {
                                             "org.dspace.content.authority.EPersonAuthority = EPersonAuthority"
                                         });
        configurationService.setProperty("choices.plugin.dspace.policy.eperson", "EPersonAuthority");
        configurationService.setProperty("choices.presentation.dspace.policy.eperson", "suggest");
        configurationService.setProperty("authority.controlled.dspace.policy.eperson", "true");

        choiceAuthorityService.clearCache();
        metadataAuthorityService.clearCache();

        context.turnOffAuthorisationSystem();

        EPerson authorizedUser = EPersonBuilder.createEPerson(context)
            .withEmail("authorized@mail.it")
            .build();

        Item accessibleItem = ItemBuilder.createItem(context, collection)
            .withTitle("Accessible item")
            .withPolicyEPerson("Authorized", authorizedUser.getID().toString())
            .build();

        Item blockedItem = ItemBuilder.createItem(context, collection)
            .withTitle("Blocked item")
            .withPolicyEPerson("Authorized", authorizedUser.getID().toString())
            .build();

        context.restoreAuthSystemState();

        AccessItemMode accessMode = buildAccessItemMode(CrisSecurity.CUSTOM);
        when(accessMode.getUserMetadataFields()).thenReturn(List.of("dspace.policy.eperson"));
        when(accessMode.getAdditionalFilter()).thenReturn(new Filter() {
            @Override
            public boolean getResult(Context context, Item item) throws LogicalStatementException {
                return item.getName().equals("Accessible item");
            }

            @Override
            public String getName() {
                return null;
            }

            @Override
            public void setBeanName(String s) {}
        });

        assertThat(crisSecurityService.hasAccess(context, accessibleItem, authorizedUser, accessMode),
                   is(true));
        assertThat(crisSecurityService.hasAccess(context, blockedItem, authorizedUser, accessMode),
                   is(false));
        assertThat(crisSecurityService.hasAccess(context, accessibleItem, eperson, accessMode),
                   is(false));
    }

    @Test
    public void testHasAccessWithGroupConfigAndNonExistentGroup() throws SQLException {

        context.turnOffAuthorisationSystem();

        Group existingGroup = GroupBuilder.createGroup(context)
            .withName("Existing Group")
            .build();

        EPerson groupMember = EPersonBuilder.createEPerson(context)
            .withEmail("member@mail.it")
            .withGroupMembership(existingGroup)
            .build();

        Item item = ItemBuilder.createItem(context, collection)
            .withTitle("Test item")
            .build();

        context.restoreAuthSystemState();

        AccessItemMode accessMode = buildAccessItemMode(CrisSecurity.GROUP);
        when(accessMode.getGroups()).thenReturn(
            List.of("Non Existent Group", "00000000-0000-0000-0000-000000000000"));
        assertThat(crisSecurityService.hasAccess(context, item, groupMember, accessMode), is(false));

        when(accessMode.getGroups()).thenReturn(
            List.of("Non Existent Group", "Existing Group"));
        assertThat(crisSecurityService.hasAccess(context, item, groupMember, accessMode), is(true));
    }

    private CrisSecurityService getCrisSecurityService() {
        return new DSpace().getServiceManager().getApplicationContext().getBean(CrisSecurityService.class);
    }
}
