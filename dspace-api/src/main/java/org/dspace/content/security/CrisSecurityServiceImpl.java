/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.security;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.security.service.CrisSecurityService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.util.UUIDUtils;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of {@link CrisSecurityService}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class CrisSecurityServiceImpl implements CrisSecurityService {

    @Autowired
    private ItemService itemService;

    @Autowired
    private AuthorizeService authorizeService;

    @Override
    public boolean hasAccess(Context context, Item item, EPerson user, AccessItemMode accessMode)
        throws SQLException {
        boolean isOwner = isOwner(user, item);
        boolean isAdmin = authorizeService.isAdmin(context, user);
        return hasAccess(context, item, user, isOwner, isAdmin, accessMode);
    }

    @Override
    public boolean isOwner(EPerson eperson, Item item) {
        if (eperson == null) {
            return false;
        }
        List<MetadataValue> owners = itemService.getMetadataByMetadataString(item, "cris.owner");
        Predicate<MetadataValue> checkOwner = v -> StringUtils.equals(v.getAuthority(), eperson.getID().toString());
        return owners.stream().anyMatch(checkOwner);
    }

    private boolean hasAccess(Context context, Item item, EPerson user, boolean isOwner,
        boolean isAdmin, AccessItemMode accessMode) throws SQLException {

        CrisSecurity security = accessMode.getSecurity();

        if ((security == CrisSecurity.ADMIN || security == CrisSecurity.ADMIN_OWNER) && isAdmin) {
            return true;
        }

        if ((security == CrisSecurity.OWNER || security == CrisSecurity.ADMIN_OWNER) && isOwner) {
            return true;
        }

        if (security == CrisSecurity.CUSTOM) {

            boolean hasAccessByGroup = hasAccessByGroup(context, item, user, accessMode.getGroupMetadataFields());
            if (hasAccessByGroup) {
                return true;
            }

            boolean hasAccessByUser = hasAccessByUser(context, item, user, accessMode.getUserMetadataFields());
            if (hasAccessByUser) {
                return true;
            }

            boolean hasAccessByItem = hasAccessByItem(context, item, user, accessMode.getItemMetadataFields());
            if (hasAccessByItem) {
                return true;
            }

        }

        return false;
    }

    private boolean hasAccessByGroup(Context context, Item item, EPerson user, List<String> groupMetadataFields)
        throws SQLException {

        if (user == null || CollectionUtils.isEmpty(groupMetadataFields)) {
            return false;
        }

        List<Group> userGroups = user.getGroups();
        if (CollectionUtils.isEmpty(userGroups)) {
            return false;
        }

        for (Group group : userGroups) {
            for (String groupMetadataField : groupMetadataFields) {
                if (anyMetadataHasAuthorityEqualsTo(item, groupMetadataField, group.getID())) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean hasAccessByUser(Context context, Item item, EPerson user, List<String> userMetadataFields)
        throws SQLException {

        if (user == null || CollectionUtils.isEmpty(userMetadataFields)) {
            return false;
        }

        for (String userMetadataField : userMetadataFields) {
            if (anyMetadataHasAuthorityEqualsTo(item, userMetadataField, user.getID())) {
                return true;
            }
        }

        return false;
    }

    private boolean hasAccessByItem(Context context, Item item, EPerson user, List<String> itemMetadataFields)
        throws SQLException {

        if (user == null || CollectionUtils.isEmpty(itemMetadataFields)) {
            return false;
        }

        return findRelatedItems(context, item, itemMetadataFields).stream()
            .anyMatch(relatedItem -> isOwner(user, relatedItem));
    }

    private List<Item> findRelatedItems(Context context, Item item, List<String> itemMetadataFields)
        throws SQLException {

        List<Item> relatedItems = new ArrayList<>();

        if (CollectionUtils.isEmpty(itemMetadataFields)) {
            return relatedItems;
        }

        for (String itemMetadataField : itemMetadataFields) {
            List<MetadataValue> metadataValues = itemService.getMetadataByMetadataString(item, itemMetadataField);
            for (MetadataValue metadataValue : metadataValues) {
                UUID relatedItemId = UUIDUtils.fromString(metadataValue.getAuthority());
                Item relatedItem = itemService.find(context, relatedItemId);
                if (relatedItem != null) {
                    relatedItems.add(relatedItem);
                }
            }
        }

        return relatedItems;

    }

    private boolean anyMetadataHasAuthorityEqualsTo(Item item, String metadata, UUID uuid) {
        return itemService.getMetadataByMetadataString(item, metadata).stream()
            .anyMatch(value -> uuid.toString().equals(value.getAuthority()));
    }

}
