/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.utils;

import org.apache.commons.lang3.StringUtils;
import org.dspace.app.rest.model.DSpaceObjectRest;
import org.springframework.stereotype.Component;

/**
 * This class will contain methods that can define in what way DSpaceObjectRest objects are equal
 */
@Component
public class DSpaceObjectRestEqualityUtils {

    /**
     * This method will return a boolean indicating whether the given DSpaceObjectRest objects are equal
     * through comparing their attributes
     * @param original  The original DSpaceObjectRest object
     * @param updated   The DSpaceObjectRest object that has to be checked for equality
     * @return          A boolean indicating whether they're equal or not
     */
    public boolean isDSpaceObjectEqualsWithoutMetadata(DSpaceObjectRest original, DSpaceObjectRest updated) {
        return StringUtils.equals(original.getId(), updated.getId()) &&
            StringUtils.equals(original.getCategory(), updated.getCategory()) &&
            StringUtils.equals(original.getHandle(), updated.getHandle());

    }
}
