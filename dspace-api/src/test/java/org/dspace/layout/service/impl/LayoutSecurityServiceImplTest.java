/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.layout.service.impl;

import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataSchema;
import org.dspace.content.MetadataValue;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.service.GroupService;
import org.dspace.layout.CrisLayoutBox;
import org.dspace.layout.LayoutSecurity;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author Corrado Lombardi (corrado.lombardi at 4science.it)
 */
@RunWith(MockitoJUnitRunner.class)
public class LayoutSecurityServiceImplTest {

    @Mock
    private AuthorizeService authorizeService;
    @Mock
    private ItemService itemService;
    @Mock
    private GroupService groupService;

    private LayoutSecurityServiceImpl securityService;

    @Before
    public void setUp() throws Exception {
        securityService = new LayoutSecurityServiceImpl(authorizeService, itemService, groupService);
    }

    /**
     * PUBLIC {@link LayoutSecurity} set, access is granted
     *
     * @throws SQLException
     */
    @Test
    public void publicAccessReturnsTrue() throws SQLException {

        boolean granted =
            securityService.hasAccess(LayoutSecurity.PUBLIC,
                                      mock(Context.class),
                                      ePerson(UUID.randomUUID()),
                                      emptySet(),
                                      mock(Item.class));

        assertThat(granted, is(true));
    }

    /**
     * OWNER_ONLY {@link LayoutSecurity} set, accessed by item's owner, access is granted.
     *
     * @throws SQLException
     */
    @Test
    public void ownerOnlyAccessedByItemOwner() throws SQLException {
        UUID userUuid = UUID.randomUUID();

        Item item = mock(Item.class);

        when(itemService.getMetadataFirstValue(item, "cris", "owner", null, Item.ANY))
            .thenReturn(userUuid.toString());

        boolean granted =
            securityService
                .hasAccess(LayoutSecurity.OWNER_ONLY,
                           mock(Context.class),
                           ePerson(userUuid),
                           emptySet(),
                           item);

        assertThat(granted, is(true));
    }

    /**
     * OWNER_ONLY {@link LayoutSecurity} set, accessed different owner, access forbidden.
     *
     * @throws SQLException
     */
    @Test
    public void ownerOnlyAccessedByOtherUser() throws SQLException {
        UUID userUuid = UUID.randomUUID();
        UUID ownerUuid = UUID.randomUUID();

        Item item = mock(Item.class);


        when(itemService.getMetadataFirstValue(item, "cris", "owner", null, Item.ANY))
            .thenReturn(ownerUuid.toString());

        boolean granted =
            securityService
                .hasAccess(LayoutSecurity.OWNER_ONLY,
                           mock(Context.class), ePerson(userUuid),
                           emptySet(),
                           item);

        assertThat(granted, is(false));
    }

    /**
     * OWNER_AND_ADMINISTRATOR {@link LayoutSecurity} set, accessed by administrator user, grant given
     *
     * @throws SQLException
     */
    @Test
    public void ownerAndAdminAccessedByAdminUser() throws SQLException {

        Context context = mock(Context.class);
        Item item = mock(Item.class);

        when(authorizeService.isAdmin(context)).thenReturn(true);

        boolean granted =
            securityService
                .hasAccess(LayoutSecurity.OWNER_AND_ADMINISTRATOR,
                           context, mock(EPerson.class), emptySet(),
                           item);

        assertThat(granted, is(true));
    }

    /**
     * OWNER_AND_ADMINISTRATOR {@link LayoutSecurity} set, accessed by item's owner user, access is granted
     *
     * @throws SQLException
     */
    @Test
    public void ownerAndAdminAccessedByOwnerUser() throws SQLException {

        UUID userUuid = UUID.randomUUID();

        Context context = mock(Context.class);
        Item item = mock(Item.class);

        when(authorizeService.isAdmin(context)).thenReturn(false);

        when(itemService.getMetadataFirstValue(item, "cris", "owner", null, Item.ANY))
            .thenReturn(userUuid.toString());

        boolean granted =
            securityService
                .hasAccess(LayoutSecurity.OWNER_AND_ADMINISTRATOR,
                           context, ePerson(userUuid), emptySet(), item);

        assertThat(granted, is(true));
    }

    /**
     * OWNER_AND_ADMINISTRATOR {@link LayoutSecurity} set, accessed by different user, access NOT granted
     *
     * @throws SQLException
     */
    @Test
    public void ownerAndAdminAccessedByDifferentNotAdminUser() throws SQLException {

        UUID userUuid = UUID.randomUUID();
        UUID ownerUuid = UUID.randomUUID();

        Context context = mock(Context.class);
        Item item = mock(Item.class);

        when(authorizeService.isAdmin(context)).thenReturn(false);

        when(itemService.getMetadataFirstValue(item, "cris", "owner", null, Item.ANY))
            .thenReturn(ownerUuid.toString());

        boolean granted =
            securityService
                .hasAccess(LayoutSecurity.OWNER_AND_ADMINISTRATOR,
                           context, ePerson(userUuid), emptySet(), item);

        assertThat(granted, is(false));
    }

    /**
     * ADMINISTRATOR {@link LayoutSecurity} set, accessed by administrator eperson, access is granted
     *
     * @throws SQLException
     */
    @Test
    public void adminAccessedByAdmin() throws SQLException {

        Context context = mock(Context.class);

        when(authorizeService.isAdmin(context)).thenReturn(true);

        boolean granted =
            securityService
                .hasAccess(LayoutSecurity.ADMINISTRATOR,
                           context, mock(EPerson.class), emptySet(),
                           mock(Item.class));

        assertThat(granted, is(true));
    }

    /**
     * ADMINISTRATOR {@link LayoutSecurity} set, accessed by not administrator eperson, access is NOT  granted
     *
     * @throws SQLException
     */
    @Test
    public void adminAccessedByNotAdmin() throws SQLException {

        Context context = mock(Context.class);

        when(authorizeService.isAdmin(context)).thenReturn(false);

        boolean granted = securityService.hasAccess(LayoutSecurity.ADMINISTRATOR,
                                                    context, mock(EPerson.class), emptySet(), mock(Item.class));

        assertThat(granted, is(false));
    }

    /**
     * CUSTOM_DATA {@link LayoutSecurity} set, accessed by user with id having authority on metadata
     * contained in the box, access is granted
     *
     * @throws SQLException
     */
    @Test
    public void customSecurityUserAllowed() throws SQLException {

        UUID userUuid = UUID.randomUUID();

        Item item = mock(Item.class);

        List<MetadataValue> metadataValueList = Arrays.asList(metadataValueWithAuthority(userUuid.toString()),
                                                              metadataValueWithAuthority(UUID.randomUUID().toString()));

        HashSet<MetadataField> securityMetadataFieldSet = new HashSet<>(singletonList(
            securityMetadataField()));

        when(itemService.getMetadata(item, securityMetadataField().getMetadataSchema().getName(),
                                     securityMetadataField().getElement(), null, Item.ANY, true))
            .thenReturn(metadataValueList);

        boolean granted =
            securityService.hasAccess(LayoutSecurity.CUSTOM_DATA, mock(Context.class), ePerson(userUuid),
                                      securityMetadataFieldSet, item);

        assertThat(granted, is(true));
    }


    /**
     * CUSTOM_DATA {@link LayoutSecurity} set, accessed by user belonging to a group with id having
     * authority on metadata contained in the box, access is granted
     *
     * @throws SQLException
     */
    @Test
    public void customSecurityUserGroupAllowed() throws SQLException {

        UUID userUuid = UUID.randomUUID();
        UUID groupUuid = UUID.randomUUID();
        UUID securityAuthorityUuid = UUID.randomUUID();

        Item item = mock(Item.class);
        Context context = mock(Context.class);

        EPerson user = ePerson(userUuid, UUID.randomUUID(), groupUuid);
        Group userGroup = group(groupUuid);

        when(groupService.allMemberGroupsSet(any(Context.class), eq(user)))
            .thenReturn(new HashSet<>(Collections.singletonList(userGroup)));

        MetadataField securityMetadataField = securityMetadataField();

        HashSet<MetadataField> securityMetadataFieldSet = new HashSet<>(singletonList(securityMetadataField));

        List<MetadataValue> metadataValueList =
            Arrays.asList(metadataValueWithAuthority(securityAuthorityUuid.toString()),
                          metadataValueWithAuthority(groupUuid.toString()));


        when(itemService.getMetadata(item, securityMetadataField.getMetadataSchema().getName(),
                                     securityMetadataField.getElement(), null, Item.ANY, true))
            .thenReturn(metadataValueList);

        boolean granted = securityService.hasAccess(LayoutSecurity.CUSTOM_DATA,
                                                    context, user,
                                                    securityMetadataFieldSet,
                                                    item);

        assertThat(granted, is(true));
    }

    /**
     * CUSTOM_DATA {@link LayoutSecurity} set, accessed by user with id that does not have any authority on
     * metadata contained in the box, access is NOT  granted
     *
     * @throws SQLException
     */
    @Test
    public void customSecurityUserNotAllowed() throws SQLException {

        UUID userUuid = UUID.randomUUID();
        UUID groupUuid = UUID.randomUUID();
        UUID securityAuthorityUuid = UUID.randomUUID();

        Item item = mock(Item.class);

        Context context = mock(Context.class);
        EPerson user = ePerson(userUuid);
        Group userGroup = group(groupUuid);

        when(groupService.allMemberGroupsSet(any(Context.class), eq(user)))
            .thenReturn(new HashSet<>(Collections.singletonList(userGroup)));

        MetadataField securityMetadataField = securityMetadataField();

        HashSet<MetadataField> securityMetadataFieldSet = new HashSet<>(
            singletonList(securityMetadataField));

        List<MetadataValue> metadataValueList =
            singletonList(metadataValueWithAuthority(securityAuthorityUuid.toString()));


        when(itemService.getMetadata(item, securityMetadataField.getMetadataSchema().getName(),
                                     securityMetadataField.getElement(), null, Item.ANY, true))
            .thenReturn(metadataValueList);

        boolean granted = securityService.hasAccess(
            LayoutSecurity.CUSTOM_DATA,
            context, user,
            securityMetadataFieldSet, item);

        assertThat(granted, is(false));
    }

    /**
     * Tests layout security layers with null user object
     *
     * @throws SQLException
     */
    @Test
    public void nullUserHasOnlyPublicAccess() throws SQLException {

        final Context context = mock(Context.class);
        final Item item = mock(Item.class);
        final EPerson user = null;

        final MetadataField metadataField = securityMetadataField();

        final HashSet<MetadataField> securityMetadataFieldSet = new HashSet<>(singletonList(metadataField));

        List<MetadataValue> metadataValueList =
            singletonList(metadataValueWithAuthority(UUID.randomUUID().toString()));


        when(itemService.getMetadata(item, metadataField.getMetadataSchema().getName(),
                                     metadataField.getElement(), null, Item.ANY, true))
            .thenReturn(metadataValueList);

        final boolean publicAccess = securityService.hasAccess(LayoutSecurity.PUBLIC,
                                                                   context, user, securityMetadataFieldSet, item);

        final boolean customDataAccess = securityService.hasAccess(LayoutSecurity.CUSTOM_DATA,
                                                    context, user, securityMetadataFieldSet, item);

        final boolean adminAccess = securityService.hasAccess(LayoutSecurity.ADMINISTRATOR,
                                                              context, user, securityMetadataFieldSet, item);

        final boolean adminOwnerAccess = securityService.hasAccess(LayoutSecurity.OWNER_AND_ADMINISTRATOR,
                                                                   context, user, securityMetadataFieldSet, item);

        assertThat(publicAccess, is(true));
        assertThat(customDataAccess, is(false));
        assertThat(adminAccess, is(false));
        assertThat(adminOwnerAccess, is(false));
    }

    private EPerson ePerson(UUID userUuid, UUID... groupsUuid) throws SQLException {
        EPerson currentUser = mock(EPerson.class);

        when(currentUser.getID()).thenReturn(userUuid);

        return currentUser;
    }

    private Group group(UUID groupUuid) {
        Group group = mock(Group.class);
        when(group.getID()).thenReturn(groupUuid);
        return group;
    }

    private MetadataField securityMetadataField() {
        MetadataSchema ms = mock(MetadataSchema.class);
        when(ms.getName()).thenReturn("schemaname");
        MetadataField msf = mock(MetadataField.class);
        when(msf.getMetadataSchema()).thenReturn(ms);
        when(msf.getElement()).thenReturn("element");
        return msf;
    }

    private MetadataValue metadataValueWithAuthority(String authority) {
        MetadataValue metadataValue = mock(MetadataValue.class);

        when(metadataValue.getAuthority()).thenReturn(authority);

        return metadataValue;

    }


    private CrisLayoutBox box(LayoutSecurity security, MetadataField... securityFields) {
        CrisLayoutBox box = new CrisLayoutBox();
        box.setSecurity(security);
        box.addMetadataSecurityFields(new HashSet<>(Arrays.asList(securityFields)));
        return box;
    }
}
