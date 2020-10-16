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
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataValue;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.service.GroupService;
import org.dspace.layout.LayoutSecurity;
import org.dspace.layout.service.LayoutSecurityService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Corrado Lombardi (corrado.lombardi at 4science.it)
 */
public class LayoutSecurityServiceImpl implements LayoutSecurityService {

    private final AuthorizeService authorizeService;
    private final ItemService itemService;
    private final GroupService groupService;

    @Autowired
    public LayoutSecurityServiceImpl(AuthorizeService authorizeService,
                                     ItemService itemService,
                                     final GroupService groupService) {
        this.authorizeService = authorizeService;
        this.itemService = itemService;
        this.groupService = groupService;
    }


    @Override
    public boolean hasAccess(LayoutSecurity layoutSecurity, Context context, EPerson user,
                             Set<MetadataField> metadataSecurityFields, Item item) throws SQLException {

        switch (layoutSecurity) {
            case PUBLIC:
                return true;
            case OWNER_ONLY:
                return isOwner(user, item);
            case CUSTOM_DATA:
                return customDataGrantAccess(context, user, metadataSecurityFields, item);
            case ADMINISTRATOR:
                return authorizeService.isAdmin(context);
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
                                     .anyMatch(values -> checkUser(context, user, values));


    }

    private List<MetadataValue> getMetadata(Item item, MetadataField mf) {
        return itemService
                   .getMetadata(item, mf.getMetadataSchema().getName(), mf.getElement(), mf.getQualifier(), Item.ANY,
                                true);
    }

    private boolean checkUser(final Context context, EPerson user, List<MetadataValue> values) {
        Predicate<MetadataValue> currentUserPredicate = v -> v.getAuthority().equals(user.getID().toString());
        Predicate<MetadataValue> checkGroupsPredicate = v -> checkGroup(v, groups(context, user));

        return values.stream()
                     .anyMatch(currentUserPredicate.or(checkGroupsPredicate));
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

    private boolean isOwner(EPerson currentUser, Item item) {
        String uuidOwner = itemService.getMetadataFirstValue(item, "cris", "owner", null, Item.ANY);

        return Optional.ofNullable(uuidOwner).map(ownerId -> ownerId.equals(currentUser.getID().toString()))
                       .orElse(false);
    }
}
