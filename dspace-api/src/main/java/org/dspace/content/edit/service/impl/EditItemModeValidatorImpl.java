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
 * <p>
 * This validator ensures that edit item mode configurations do not contain duplicate mode names
 * within the same configuration key. Duplicate names would create ambiguity when determining
 * which edit mode applies to an item.
 * <p>
 * <strong>Validation performed:</strong>
 * <ul>
 *   <li><strong>Duplicate mode name detection</strong>: For each configuration key (e.g., "publication", "person"),
 *       verifies that no mode name appears more than once in that key's list of edit modes</li>
 * </ul>
 * <p>
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

    /**
     * Collects all validation error messages for the given configuration.
     * <p>
     * Currently only checks for duplicate edit mode names. This method is designed to be extensible
     * for future validation checks.
     *
     * @param  editItemModesConfiguration the configuration to validate
     * @return list of error messages (empty if configuration is valid)
     */
    private List<String> getValidationErrorMessages(Map<String, List<EditItemMode>> editItemModesConfiguration) {
        List<String> errorMessages = new ArrayList<String>();
        errorMessages.addAll(getDuplicatedEditModeMessages(editItemModesConfiguration));
        return errorMessages;
    }

    /**
     * Generates error messages for any configuration keys that contain duplicate edit mode names.
     * <p>
     * Iterates through each configuration key and checks for duplicate mode names within that key's
     * list of edit modes.
     *
     * @param  editItemModesConfiguration the configuration to check for duplicates
     * @return list of error messages describing duplicate modes (empty if no duplicates found)
     */
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

    /**
     * Identifies duplicate edit mode names within a list of edit modes.
     * <p>
     * Groups edit modes by their name and returns the names that appear more than once.
     *
     * @param  editModes the list of edit modes to check
     * @return list of mode names that appear more than once (empty if no duplicates)
     */
    private List<String> getDuplicatedEditModes(List<EditItemMode> editModes) {
        return groupEditItemModesByName(editModes).entrySet().stream()
            .filter(entry -> entry.getValue().size() > 1)
            .map(entry -> entry.getKey())
            .collect(Collectors.toList());
    }

    /**
     * Groups edit modes by their name for duplicate detection.
     *
     * @param  editModes the list of edit modes to group
     * @return map where keys are mode names and values are lists of modes with that name
     */
    private Map<String, List<EditItemMode>> groupEditItemModesByName(List<EditItemMode> editModes) {
        return editModes.stream().collect(groupingBy(EditItemMode::getName));
    }

}
