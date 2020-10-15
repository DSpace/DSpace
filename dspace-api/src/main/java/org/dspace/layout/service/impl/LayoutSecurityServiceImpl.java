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
import org.dspace.layout.LayoutSecurity;
import org.dspace.layout.service.LayoutSecurityService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Corrado Lombardi (corrado.lombardi at 4science.it)
 */
public class LayoutSecurityServiceImpl implements LayoutSecurityService {

    private final AuthorizeService authorizeService;
    private final ItemService itemService;

    @Autowired
    public LayoutSecurityServiceImpl(AuthorizeService authorizeService,
                                     ItemService itemService) {
        this.authorizeService = authorizeService;
        this.itemService = itemService;
    }


    @Override
    public boolean grantAccess(LayoutSecurity layoutSecurity, Context context, EPerson currentUser,
                               Set<MetadataField> metadataSecurityFields, Item item) throws SQLException {

        switch (layoutSecurity) {
            case PUBLIC:
                return true;
            case OWNER_ONLY:
                return isOwner(currentUser, item);
            case CUSTOM_DATA:
                return customDataGrantAccess(currentUser, metadataSecurityFields, item);
            case ADMINISTRATOR:
                return authorizeService.isAdmin(context);
            case OWNER_AND_ADMINISTRATOR:
                return authorizeService.isAdmin(context) || isOwner(currentUser, item);
            default:
                return false;
        }
    }

    private boolean customDataGrantAccess(EPerson currentUser, Set<MetadataField> metadataSecurityFields, Item item) {
        return metadataSecurityFields.stream()
                                     .map(mf -> getMetadata(item, mf))
                                     .anyMatch(values -> checkUser(currentUser, values));


    }

    private List<MetadataValue> getMetadata(Item item, MetadataField mf) {
        return itemService
                   .getMetadata(item, mf.getMetadataSchema().getName(), mf.getElement(), mf.getQualifier(), Item.ANY,
                                true);
    }

    private boolean checkUser(EPerson currentUser, List<MetadataValue> values) {
        Predicate<MetadataValue> currentUserPredicate = v -> v.getAuthority().equals(currentUser.getID().toString());
        Predicate<MetadataValue> checkGroupsPredicate = v -> checkGroup(v, currentUser.getGroups());

        return values.stream()
                     .anyMatch(currentUserPredicate.or(checkGroupsPredicate));
    }

    private boolean checkGroup(MetadataValue value, List<Group> groups) {
        return groups.stream()
                     .anyMatch(g -> g.getID().toString().equals(value.getAuthority()));
    }

    private boolean isOwner(EPerson currentUser, Item item) {
        String uuidOwner = itemService.getMetadataFirstValue(item, "cris", "owner", null, Item.ANY);

        return Optional.ofNullable(uuidOwner).map(ownerId -> ownerId.equals(currentUser.getID().toString()))
                       .orElse(false);
    }
}
