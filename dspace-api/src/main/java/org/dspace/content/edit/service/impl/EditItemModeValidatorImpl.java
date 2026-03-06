/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.edit.service.impl;

import static java.util.stream.Collectors.groupingBy;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.dspace.content.edit.EditItemMode;
import org.dspace.content.edit.service.EditItemModeValidator;

/**
 * Implementation of {@link EditItemModeValidator}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class EditItemModeValidatorImpl implements EditItemModeValidator {

    @Override
    public void validate(Map<String, List<EditItemMode>> editItemModesConfiguration) throws IllegalStateException {

        List<String> validationErrorMessages = getValidationErrorMessages(editItemModesConfiguration);

        if (isNotEmpty(validationErrorMessages)) {
            throw new IllegalStateException("Invalid Edit item mode configuration: " + validationErrorMessages);
        }

    }

    private List<String> getValidationErrorMessages(Map<String, List<EditItemMode>> editItemModesConfiguration) {
        List<String> errorMessages = new ArrayList<String>();
        errorMessages.addAll(getDuplicatedEditModeMessages(editItemModesConfiguration));
        return errorMessages;
    }

    private List<String> getDuplicatedEditModeMessages(Map<String, List<EditItemMode>> editItemModesConfiguration) {
        List<String> messages = new ArrayList<String>();

        for (String configurationKey : editItemModesConfiguration.keySet()) {
            List<EditItemMode> editModes = editItemModesConfiguration.get(configurationKey);
            List<String> duplicatedModes = getDuplicatedEditModes(editModes);
            if (isNotEmpty(duplicatedModes)) {
                messages.add("Configuration with key '" + configurationKey +
                    "' has the following duplicated edit modes: " + duplicatedModes);
            }
        }

        return messages;
    }

    private List<String> getDuplicatedEditModes(List<EditItemMode> editModes) {
        return groupEditItemModesByName(editModes).entrySet().stream()
            .filter(entry -> entry.getValue().size() > 1)
            .map(entry -> entry.getKey())
            .collect(Collectors.toList());
    }

    private Map<String, List<EditItemMode>> groupEditItemModesByName(List<EditItemMode> editModes) {
        return editModes.stream().collect(groupingBy(EditItemMode::getName));
    }

}
