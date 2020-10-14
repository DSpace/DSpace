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
import java.util.function.Predicate;

import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataValue;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.layout.CrisLayoutBox;
import org.dspace.layout.service.CrisLayoutBoxAccessService;

/**
 * @author Corrado Lombardi (corrado.lombardi at 4science.it)
 */
public class CrisLayoutBoxAccessServiceImpl implements CrisLayoutBoxAccessService {

    private final AuthorizeService authorizeService;
    private final ItemService itemService;

    public CrisLayoutBoxAccessServiceImpl(AuthorizeService authorizeService,
                                          ItemService itemService) {
        this.authorizeService = authorizeService;
        this.itemService = itemService;
    }

    @Override
    public boolean grantAccess(Context context, EPerson currentUser, CrisLayoutBox box, Item item)
        throws SQLException {
        int layoutSecurity = box.getSecurity();

        switch (layoutSecurity) {
            case 1:
                return authorizeService.isAdmin(context);
            case 2:
                return isOwner(currentUser, item);
            case 3:
                return (isOwner(currentUser, item) || authorizeService.isAdmin(context));
            case 4:
                return customDataGrantAccess(currentUser, box, item);
            default:
                return false;
        }
    }

    private boolean customDataGrantAccess(EPerson currentUser, CrisLayoutBox box, Item item) {
        return box.getMetadataSecurityFields().stream()
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
