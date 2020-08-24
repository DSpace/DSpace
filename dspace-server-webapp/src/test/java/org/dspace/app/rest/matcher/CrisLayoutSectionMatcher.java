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
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;

import org.hamcrest.Matcher;

/**
 * Utility class to construct a Matcher for an CRIS layout section.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 */
public final class CrisLayoutSectionMatcher {

    private CrisLayoutSectionMatcher() {
    }

    /**
     * Matcher to verify that the section with the given id has a browse component
     * at the position pos of the row row with the given names.
     * 
     * @param id          the section id to match
     * @param row         the row index of the browse component to match
     * @param pos         the index of the browse component to match in the given
     *                    row
     * @param style       the component style
     * @param browseNames the browse names to match
     * @return the Matcher instance
     */
    public static Matcher<? super Object> withIdAndBrowseComponent(String id, int row, int pos, String style,
        String... browseNames) {

        return allOf(
            hasJsonPath("$.id", is(id)),
            withBrowseComponent(row, pos, style, browseNames)
        );
    }

    /**
     * Matcher to verify that the section with the given id has a top component at
     * the position pos of the row row with the given attributes.
     * 
     * @param id              the section id to match
     * @param row             the row index of the top component to match
     * @param pos             the index of the top component in the given row
     * @param style           the component style
     * @param discoveryConfig the discovery configuration name of the top component
     * @param sortField       the sort field of the top component to match
     * @param order           the order of the top component to match
     * @return the Matcher instance
     */
    public static Matcher<? super Object> withIdAndTopComponent(String id, int row, int pos, String style,
        String discoveryConfig, String sortField, String order) {

        return allOf(
            hasJsonPath("$.id", is(id)),
            withTopComponent(row, pos, style, discoveryConfig, sortField, order)
        );
    }

    /**
     * Matcher to verify that the section with the given id has a search component
     * at the position pos of the row row with the given attributes.
     * 
     * @param id              the section id to match
     * @param row             the row index of the top component to match
     * @param pos             the index of the top component in the given row
     * @param style           the component style
     * @param discoveryConfig the discovery configuration name of the top component
     * @return the Matcher instance
     */
    public static Matcher<? super Object> withIdAndSearchComponent(String id, int row, int pos, String style,
        String discoveryConfig) {

        return allOf(
            hasJsonPath("$.id", is(id)),
            withSearchComponent(row, pos, style, discoveryConfig));
    }

    /**
     * Matcher to verify that the section with the given id has a facet component at
     * the position pos of the row row with the given attributes.
     * 
     * @param id              the section id to match
     * @param row             the row index of the top component to match
     * @param pos             the index of the top component in the given row
     * @param style           the component style
     * @param discoveryConfig the discovery configuration name of the top component
     * @return the Matcher instance
     */
    public static Matcher<? super Object> withIdAndFacetComponent(String id, int row, int pos, String style,
        String discoveryConfig) {

        return allOf(
            hasJsonPath("$.id", is(id)),
            withFacetComponent(row, pos, style, discoveryConfig));
    }

    /**
     * Matcher to verify that the current section has a browse component at the
     * position pos of the row row with the given names.
     * 
     * @param row         the row index of the browse component to match
     * @param pos         the index of the browse component to match in the given
     *                    row
     * @param browseNames the browse names to match
     * @return the Matcher instance
     */
    public static Matcher<? super Object> withBrowseComponent(int row, int pos, String style, String... browseNames) {

        return allOf(
            hasJsonPath("$.componentRows[" + row + "][" + pos + "].componentType", is("browse")),
            hasJsonPath("$.componentRows[" + row + "][" + pos + "].style", is(style)),
            hasJsonPath("$.componentRows[" + row + "][" + pos + "].browseNames", containsInAnyOrder(browseNames)));
    }

    /**
     * Matcher to verify that the current section has a top component at the
     * position pos of the row row with the given attributes.
     * 
     * @param id              the section id to match
     * @param row             the row index of the top component to match
     * @param pos             the index of the top component in the given row
     * @param style           the component style
     * @param discoveryConfig the discovery configuration name of the top component
     * @param sortField       the sort field of the top component to match
     * @param order           the order of the top component to match
     * @return the Matcher instance
     */
    public static Matcher<? super Object> withTopComponent(int row, int pos, String style,
        String discoveryConfig, String sortField, String order) {

        return allOf(
            hasJsonPath("$.componentRows[" + row + "][" + pos + "].componentType", is("top")),
            hasJsonPath("$.componentRows[" + row + "][" + pos + "].style", is(style)),
            hasJsonPath("$.componentRows[" + row + "][" + pos + "].discoveryConfigurationName", is(discoveryConfig)),
            hasJsonPath("$.componentRows[" + row + "][" + pos + "].sortField", is(sortField)),
            hasJsonPath("$.componentRows[" + row + "][" + pos + "].order", is(order)));
    }

    /**
     * Matcher to verify that the current section has a search component at the
     * position pos of the row row with the given attributes.
     * 
     * @param id              the section id to match
     * @param row             the row index of the top component to match
     * @param pos             the index of the top component in the given row
     * @param style           the component style
     * @param discoveryConfig the discovery configuration name of the top component
     * @return the Matcher instance
     */
    public static Matcher<? super Object> withSearchComponent(int row, int pos, String style, String discoveryConfig) {

        return allOf(
            hasJsonPath("$.componentRows[" + row + "][" + pos + "].componentType", is("search")),
            hasJsonPath("$.componentRows[" + row + "][" + pos + "].style", is(style)),
            hasJsonPath("$.componentRows[" + row + "][" + pos + "].discoveryConfigurationName", is(discoveryConfig)));
    }

    /**
     * Matcher to verify that the current section has a facet component at the
     * position pos of the row row with the given attributes.
     * 
     * @param id              the section id to match
     * @param row             the row index of the top component to match
     * @param pos             the index of the top component in the given row
     * @param style           the component style
     * @param discoveryConfig the discovery configuration name of the top component
     * @return the Matcher instance
     */
    public static Matcher<? super Object> withFacetComponent(int row, int pos, String style, String discoveryConfig) {

        return allOf(
            hasJsonPath("$.componentRows[" + row + "][" + pos + "].componentType", is("facet")),
            hasJsonPath("$.componentRows[" + row + "][" + pos + "].style", is(style)),
            hasJsonPath("$.componentRows[" + row + "][" + pos + "].discoveryConfigurationName", is(discoveryConfig)));
    }
}
