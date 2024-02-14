/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.matcher;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;

import org.dspace.app.rest.model.FilteredCollectionRest;
import org.hamcrest.Matcher;

/**
 * Utility class to construct a Matcher for a FilteredCollectionRest.
 * @author Jean-François Morin (Université Laval)
 */
public class FilteredCollectionMatcher {

    private FilteredCollectionMatcher() { }

    public static Matcher<? super Object> matchFilteredCollectionProperties(FilteredCollectionRest collection) {
        return allOf(
                hasJsonPath("$.label", is(collection.getLabel())),
                hasJsonPath("$.community_label", is(collection.getCommunityLabel())),
                hasJsonPath("$.community_handle", is(collection.getCommunityHandle())),
                hasJsonPath("$.nb_total_items", is(collection.getTotalItems())),
                hasJsonPath("$.all_filters_value", is(collection.getAllFiltersValue()))
            );
    }

    public static Matcher<? super Object> matchFilteredCollectionSummary(int nbTotalItems, int nbFilteredItems) {
        return hasJsonPath("$.summary", allOf(
                hasJsonPath("nb_total_items", is(nbTotalItems)),
                hasJsonPath("all_filters_value", is(nbFilteredItems))));
    }

}
