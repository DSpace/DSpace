/*
 */
package org.datadryad.api;

import java.util.Arrays;
import java.util.List;

/**
 *
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
public class DryadEmbargoTypes {
    public static final String UNTIL_ARTICLE_APPEARS = "untilArticleAppears";
    public static final String ONEYEAR = "oneyear";
    public static final String CUSTOM = "custom";

    private static final List<String> VALID_TYPES = Arrays.asList(
            UNTIL_ARTICLE_APPEARS,
            ONEYEAR,
            CUSTOM
        );

    public static Boolean validate(String type) {
        return VALID_TYPES.contains(type);
    }
}
