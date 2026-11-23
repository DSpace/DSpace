/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.edit.service;

import java.util.List;
import java.util.Map;

import org.dspace.content.edit.EditItemMode;

/**
 * Validator for {@link EditItemMode} configurations.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public interface EditItemModeValidator {

    /**
     * Validate the given edit item mode configuration, throwing an exception if it
     * is not valid.
     * <p>
     * A configuration is considered <strong>valid</strong> when:
     * <ul>
     *   <li>Within each configuration key (e.g., "publication", "person"), all edit mode names are unique</li>
     *   <li>No duplicate mode names exist in the same configuration key's list</li>
     * </ul>
     * <p>
     * Duplicate mode names within the same configuration key are invalid because they would create
     * ambiguity when selecting which edit mode to apply for an item.
     *
     * @param  editItemModesMap      the configuration to be validated, where keys are entity types
     *                               or configuration identifiers, and values are lists of edit modes
     * @throws IllegalStateException if the given configuration contains duplicate edit mode names
     *                               within any configuration key
     */
    public void validate(Map<String, List<EditItemMode>> editItemModesMap) throws IllegalStateException;

}
