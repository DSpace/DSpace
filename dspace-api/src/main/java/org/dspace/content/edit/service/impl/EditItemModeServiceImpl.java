/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.edit.service.impl;

import static org.apache.commons.lang3.StringUtils.isBlank;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.annotation.PostConstruct;
import org.dspace.app.util.SubmissionConfigReader;
import org.dspace.app.util.SubmissionConfigReaderException;
import org.dspace.content.Item;
import org.dspace.content.edit.EditItemMode;
import org.dspace.content.edit.service.EditItemModeService;
import org.dspace.content.edit.service.EditItemModeValidator;
import org.dspace.content.security.service.CrisSecurityService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.core.exception.SQLRuntimeException;
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

    @Autowired
    private EditItemModeValidator editItemModeValidator;

    private SubmissionConfigReader submissionConfigReader;

    private Map<String, List<EditItemMode>> editModesMap;

    @PostConstruct
    private void setup() throws SubmissionConfigReaderException {
        editItemModeValidator.validate(editModesMap);
        submissionConfigReader = new SubmissionConfigReader();
    }

    @Override
    public List<EditItemMode> findModes(Context context, Item item) throws SQLException {
        return findModes(context, item, true);
    }

    public List<EditItemMode> findModes(Context context, Item item, boolean checkSecurity) throws SQLException {

        if (context.getCurrentUser() == null) {
            return List.of();
        }

        List<EditItemMode> configuredModes = findEditItemModesByItem(item);

        if (!checkSecurity) {
            return configuredModes;
        }

        return configuredModes.stream()
            .filter(editMode -> hasAccess(context, item, editMode))
            .collect(Collectors.toList());
    }

    @Override
    public List<EditItemMode> findModes(Context context, UUID itemId) throws SQLException {
        return findModes(context, itemService.find(context, itemId));
    }

    @Override
    public EditItemMode findMode(Context context, Item item, String name) throws SQLException {
        List<EditItemMode> modes = findModes(context, item, false);
        return modes.stream()
                .filter(mode -> mode.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

    @Override
    public boolean canEdit(Context context, Item item) {

        if (context.getCurrentUser() == null) {
            return false;
        }

        return findEditItemModesByItem(item).stream()
            .anyMatch(editMode -> hasAccess(context, item, editMode));

    }

    private boolean hasAccess(Context context, Item item, EditItemMode editMode) {
        try {
            return crisSecurityService.hasAccess(context, item, context.getCurrentUser(), editMode);
        } catch (SQLException e) {
            throw new SQLRuntimeException(e);
        }
    }

    private List<EditItemMode> findEditItemModesByItem(Item item) {

        List<EditItemMode> defaultModes = List.of();

        if (item == null || !item.isArchived()) {
            return defaultModes;
        }

        String entityType = itemService.getEntityTypeLabel(item);
        if (isBlank(entityType)) {
            return defaultModes;
        }

        String entityTypeLowerCase = entityType.toLowerCase();
        String submissionName = getSubmissionDefinitionName(item);

        String typeAndSubmissionKey = String.join(".", entityTypeLowerCase, submissionName);
        if (editModesMap.containsKey(typeAndSubmissionKey)) {
            return editModesMap.get(typeAndSubmissionKey);
        }

        return editModesMap.getOrDefault(entityTypeLowerCase, defaultModes);
    }

    private String getSubmissionDefinitionName(Item item) {
        return submissionConfigReader.getSubmissionConfigByCollection(item.getOwningCollection()).getSubmissionName();
    }

    public Map<String, List<EditItemMode>> getEditModesMap() {
        return editModesMap;
    }

    public void setEditModesMap(Map<String, List<EditItemMode>> editModesMap) {
        this.editModesMap = editModesMap;
    }
}
