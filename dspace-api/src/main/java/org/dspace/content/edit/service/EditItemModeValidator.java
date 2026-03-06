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
     *
     * @param  editItemModesMap      the configuration to be validated
     * @throws IllegalStateException if the given configuration is not valid
     */
    public void validate(Map<String, List<EditItemMode>> editItemModesMap) throws IllegalStateException;

}
