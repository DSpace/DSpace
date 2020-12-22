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

import org.dspace.content.Item;
import org.dspace.content.edit.EditItemMode;
import org.dspace.content.edit.service.EditItemModeService;
import org.dspace.content.security.service.CrisSecurityService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
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
    private CrisSecurityService crisSecurityService;

    private Map<String, List<EditItemMode>> editModesMap;

    /* (non-Javadoc)
     * @see org.dspace.content.edit.service.EditItemModeService#
     * findModes(org.dspace.core.Context, org.dspace.content.Item)
     */
    @Override
    public List<EditItemMode> findModes(Context context, Item item) throws SQLException {
        return findModes(context, item, true);
    }

    public List<EditItemMode> findModes(Context context, Item item, boolean checkSecurity) throws SQLException {
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
                        // Filter for user permissions
                        for (EditItemMode editMode : configuredModes) {
                            if (!checkSecurity || crisSecurityService.hasAccess(context, item, currentUser, editMode)) {
                                editModes.add(editMode);
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
    public EditItemMode findMode(Context context, Item item, String name) throws SQLException {
        List<EditItemMode> modes = findModes(context, item, false);
        return modes.stream()
                .filter(mode -> mode.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

    public Map<String, List<EditItemMode>> getEditModesMap() {
        return editModesMap;
    }

    public void setEditModesMap(Map<String, List<EditItemMode>> editModesMap) {
        this.editModesMap = editModesMap;
    }
}
