/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.layout.service.impl;

import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataValue;
import org.dspace.content.authority.ChoiceAuthority;
import org.dspace.content.authority.EPersonAuthority;
import org.dspace.content.authority.GroupAuthority;
import org.dspace.content.authority.ItemAuthority;
import org.dspace.content.authority.service.ChoiceAuthorityService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.exception.SQLRuntimeException;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.service.EPersonService;
import org.dspace.eperson.service.GroupService;
import org.dspace.layout.LayoutSecurity;
import org.dspace.layout.service.LayoutSecurityService;
import org.dspace.util.UUIDUtils;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Corrado Lombardi (corrado.lombardi at 4science.it)
 */
public class LayoutSecurityServiceImpl implements LayoutSecurityService {

    private final AuthorizeService authorizeService;
    private final ItemService itemService;
    private final GroupService groupService;
    private final EPersonService ePersonService;
    private final ChoiceAuthorityService choiceAuthorityService;

    @Autowired
    public LayoutSecurityServiceImpl(AuthorizeService authorizeService,
                                     ItemService itemService,
                                     final GroupService groupService,
                                     EPersonService ePersonService,
                                     ChoiceAuthorityService choiceAuthorityService) {
        this.authorizeService = authorizeService;
        this.itemService = itemService;
        this.groupService = groupService;
        this.ePersonService = ePersonService;
        this.choiceAuthorityService = choiceAuthorityService;
    }


    @Override
    public boolean hasAccess(LayoutSecurity layoutSecurity, Context context, EPerson user,
                             Set<MetadataField> metadataSecurityFields,
                             Set<Group> groupSecurityFields, Item item) throws SQLException {

        switch (layoutSecurity) {
            case PUBLIC:
                return true;
            case OWNER_ONLY:
                return isOwner(user, item);
            case CUSTOM_DATA:
                return customDataGrantAccess(context, user, metadataSecurityFields, item)
                    || customDataGrantAccessGroup(context, groupSecurityFields);
            case ADMINISTRATOR:
                return authorizeService.isAdmin(context);
            case CUSTOM_DATA_AND_ADMINISTRATOR:
                return authorizeService.isAdmin(context)
                    || customDataGrantAccess(context, user, metadataSecurityFields, item)
                    || customDataGrantAccessGroup(context, groupSecurityFields);
            case OWNER_AND_ADMINISTRATOR:
                return authorizeService.isAdmin(context) || isOwner(user, item);
            default:
                return false;
        }
    }

    private boolean customDataGrantAccess(final Context context, EPerson user,
                                          Set<MetadataField> metadataSecurityFields, Item item) {
        return metadataSecurityFields.stream()
                                     .map(mf -> getMetadata(item, mf))
                                     .filter(Objects::nonNull)
                                     .filter(metadataValues -> !metadataValues.isEmpty())
                                     .anyMatch(values -> checkUser(context, user, item, values));
    }

    private boolean customDataGrantAccessGroup(Context context, Set<Group> groupSecurityFields) {
        return groupSecurityFields.stream().anyMatch(group -> isMemberOfGroup(context, group));
    }

    private List<MetadataValue> getMetadata(Item item, MetadataField mf) {
        return itemService
                   .getMetadata(item, mf.getMetadataSchema().getName(), mf.getElement(), mf.getQualifier(), Item.ANY,
                                true);
    }

    private boolean checkUser(final Context context, EPerson user, Item item, List<MetadataValue> values) {

        for (MetadataValue metadataValue : values) {

            ChoiceAuthority choiceAuthority = getChoiceAuthority(item, metadataValue);
            if (choiceAuthority == null) {
                continue;
            }

            if (choiceAuthority instanceof EPersonAuthority && isAuthorityEqualsTo(metadataValue, user)) {
                return true;
            }

            if (choiceAuthority instanceof GroupAuthority && checkGroup(metadataValue, groups(context, user))) {
                return true;
            }

            if (choiceAuthority instanceof ItemAuthority && isOwnerOfRelatedItem(context, metadataValue, user)) {
                return true;
            }

        }

        return false;
    }


    private ChoiceAuthority getChoiceAuthority(Item item, MetadataValue metadataValue) {
        String schema = metadataValue.getMetadataField().getMetadataSchema().getName();
        String element = metadataValue.getMetadataField().getElement();
        String qualifier = metadataValue.getMetadataField().getQualifier();
        Collection collection = item.getOwningCollection();
        String authorityName = choiceAuthorityService.getChoiceAuthorityName(schema, element, qualifier, Constants.ITEM,
                collection);
        return authorityName != null ? choiceAuthorityService.getChoiceAuthorityByAuthorityName(authorityName) : null;
    }

    private boolean isAuthorityEqualsTo(MetadataValue metadataValue, EPerson user) {
        if (Objects.isNull(metadataValue) || Objects.isNull(user)) {
            return false;
        }
        return StringUtils.equals(metadataValue.getAuthority(), user.getID().toString());
    }

    private boolean checkGroup(MetadataValue value, Set<Group> groups) {
        return groups.stream()
                     .anyMatch(g -> g.getID().toString().equals(value.getAuthority()));
    }

    // in private method so that checked exception can be handled and metod can be called from a lambda
    private Set<Group> groups(final Context context, final EPerson user) {
        try {
            return groupService.allMemberGroupsSet(context, user);
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private boolean isOwnerOfRelatedItem(Context context, MetadataValue metadataValue, EPerson user) {
        try {
            Item relatedItem = itemService.find(context, UUIDUtils.fromString(metadataValue.getAuthority()));
            return relatedItem != null ? isOwner(user, relatedItem) : false;
        } catch (SQLException e) {
            throw new SQLRuntimeException(e);
        }

    }

    private boolean isOwner(EPerson eperson, Item item) {
        return ePersonService.isOwnerOfItem(eperson, item);
    }

    private boolean isMemberOfGroup(Context context, Group group) {
        try {
            return groupService.isMember(context, group);
        } catch (SQLException e) {
            throw new SQLRuntimeException(e);
        }
    }

}
