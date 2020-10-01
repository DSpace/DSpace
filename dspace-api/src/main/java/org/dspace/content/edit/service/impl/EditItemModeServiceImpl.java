/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.edit.service.impl;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataValue;
import org.dspace.content.edit.EditItemMode;
import org.dspace.content.edit.EditItemModeSecurity;
import org.dspace.content.edit.service.EditItemModeService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.MetadataFieldService;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Service implementation for the EditItemMode object.
 * This class is responsible for all business logic calls
 * for the Item object and is autowired by spring.
 * 
 * @author Danilo Di Nuzzo (danilo.dinuzzo at 4science.it)
 */
public class EditItemModeServiceImpl implements EditItemModeService {

    @Autowired
    private ItemService itemService;
    @Autowired
    private AuthorizeService authorizeService;
    @Autowired
    private MetadataFieldService metadataService;

    private Map<String, List<EditItemMode>> editModesMap;

    /* (non-Javadoc)
     * @see org.dspace.content.edit.service.EditItemModeService#
     * findModes(org.dspace.core.Context, org.dspace.content.Item)
     */
    @Override
    public List<EditItemMode> findModes(Context context, Item item) throws SQLException {
        List<EditItemMode> editModes = new ArrayList<>();
        String entityType = "";
        if ( item != null ) {
            // retrieves the entityType, used for get edit configuration
            entityType = itemService.getMetadata(item, ETYPE_METADATA);
            if (entityType != null) {
                if (editModesMap.containsKey(entityType.toLowerCase())) {
                    List<EditItemMode> configuredModes = editModesMap.get(entityType.toLowerCase());

                    EPerson currentUser = context.getCurrentUser();
                    if (currentUser != null ) {
                        // Check if the current user is the owner of the given item
                        boolean isOwner = isOwner(item, currentUser);
                        // Check if the current user is an Administrator
                        boolean isAdmin = authorizeService.isAdmin(context);
                        // Filter for user permissions
                        for (EditItemMode editMode: configuredModes) {
                            if ( ( editMode.getSecurity().equals(EditItemModeSecurity.ADMIN) ||
                                    editMode.getSecurity().equals(EditItemModeSecurity.ADMIN_OWNER) )
                                    && isAdmin) {
                                editModes.add(editMode);
                            } else if ( ( editMode.getSecurity().equals(EditItemModeSecurity.OWNER) ||
                                    editMode.getSecurity().equals(EditItemModeSecurity.ADMIN_OWNER) )
                                    && isOwner) {
                                editModes.add(editMode);
                            } else if (editMode.getSecurity().equals(EditItemModeSecurity.CUSTOM)) {
                                boolean found = false;
                                if (editMode.getGroups() != null) {
                                    List<Group> userGroups = currentUser.getGroups();
                                    if (userGroups != null && !userGroups.isEmpty()) {
                                        for (Group group: userGroups) {
                                            for (String metadataGroup: editMode.getGroups()) {
                                                if (check(context, item, metadataGroup,
                                                        group.getID().toString())) {
                                                    editModes.add(editMode);
                                                    found = true;
                                                    break;
                                                }
                                            }
                                            if (found) {
                                                break;
                                            }
                                        }
                                    }
                                }
                                if (!found && editMode.getUsers() != null) {
                                    for (String metadataUser: editMode.getUsers()) {
                                        if (check(context, item, metadataUser,
                                                currentUser.getID().toString())) {
                                            editModes.add(editMode);
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return editModes;
    }

    /* (non-Javadoc)
     * @see org.dspace.content.edit.service.EditItemModeService#findModes(org.dspace.core.Context, java.util.UUID)
     */
    @Override
    public List<EditItemMode> findModes(Context context, UUID itemId) throws SQLException {
        return findModes(context, itemService.find(context, itemId));
    }

    /* (non-Javadoc)
     * @see org.dspace.content.edit.service.EditItemModeService#findMode(org.dspace.core.Context, java.lang.String)
     */
    @Override
    public EditItemMode findMode(Context context, UUID itemId, String name) throws SQLException {
        List<EditItemMode> modes = findModes(context, itemService.find(context, itemId));
        return modes.stream()
                .filter(mode -> mode.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

    /**
     * Returns true if the given eperson is the owner of item, false otherwise
     * @param item
     * @param eperson
     * @return
     */
    private boolean isOwner(Item item, EPerson eperson) {
        return item.getSubmitter().getID().equals(eperson.getID());
    }

    public Map<String, List<EditItemMode>> getEditModesMap() {
        return editModesMap;
    }

    public void setEditModesMap(Map<String, List<EditItemMode>> editModesMap) {
        this.editModesMap = editModesMap;
    }

    /**
     * Check if the given uuid is present in a specific metadata of item
     * @param context DSpace context
     * @param item dpsace item
     * @param metadata metadata
     * @param uuid
     * @return
     * @throws SQLException
     */
    public boolean check(Context context, Item item, String metadata, String uuid) throws SQLException {
        MetadataField field = metadataService.findByString(context, metadata, '.');
        boolean found = false;
        if (field != null ) {
            List<MetadataValue> values = itemService.getMetadata(item,
                    field.getMetadataSchema().getName(),
                    field.getElement(),
                    field.getQualifier(), Item.ANY);
            if (values != null) {
                for (MetadataValue value: values) {
                    if (value.getAuthority() != null && value.getAuthority().equalsIgnoreCase(uuid) ) {
                        found = true;
                        break;
                    }
                }
            }
        }
        return found;
    }
}
