/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.utils;

import org.apache.commons.lang3.StringUtils;
import org.dspace.app.rest.model.CommunityRest;
import org.springframework.stereotype.Component;

/**
 * This class will contain methods that can define in what way CommunityRest objects are equal
 */
@Component
public class CommunityRestEqualityUtils extends DSpaceObjectRestEqualityUtils {

    /**
     * This method will return a boolean indicating whether the given CommunityRest objects are equal
     * through comparing their attributes
     * @param original  The original CommunityRest object
     * @param updated   The CommunityRest object that has to be checked for equality
     * @return          A boolean indicating whether they're equal or not
     */
    public boolean isCommunityRestEqualWithoutMetadata(CommunityRest original, CommunityRest updated) {
        return super.isDSpaceObjectEqualsWithoutMetadata(original, updated) &&
            StringUtils.equals(original.getCategory(), updated.getCategory()) &&
            StringUtils.equals(original.getType(), updated.getType());

    }
}
