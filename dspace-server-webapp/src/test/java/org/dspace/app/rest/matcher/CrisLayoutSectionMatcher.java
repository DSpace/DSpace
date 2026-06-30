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

import java.util.ArrayList;
import java.util.List;

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
     * @param numberOfItems
     * @return the Matcher instance
     */
    public static Matcher<? super Object> withIdAndTopComponent(String id, int row, int pos, String style,
                                                                String discoveryConfig, String sortField, String order,
                                                                Integer numberOfItems) {

        return allOf(
            hasJsonPath("$.id", is(id)),
            withTopComponent(row, pos, style, discoveryConfig, sortField, order, numberOfItems)
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
     * @param row             the row index of the top component to match
     * @param pos             the index of the top component in the given row
     * @param style           the component style
     * @param discoveryConfig the discovery configuration name of the top component
     * @param sortField       the sort field of the top component to match
     * @param order           the order of the top component to match
     * @param numberOfItems
     * @return the Matcher instance
     */
    public static Matcher<? super Object> withTopComponent(int row, int pos, String style,
                                                           String discoveryConfig, String sortField, String order,
                                                           Integer numberOfItems) {

        return allOf(
            hasJsonPath("$.componentRows[" + row + "][" + pos + "].componentType", is("top")),
            hasJsonPath("$.componentRows[" + row + "][" + pos + "].style", is(style)),
            hasJsonPath("$.componentRows[" + row + "][" + pos + "].discoveryConfigurationName", is(discoveryConfig)),
            hasJsonPath("$.componentRows[" + row + "][" + pos + "].sortField", is(sortField)),
            hasJsonPath("$.componentRows[" + row + "][" + pos + "].order", is(order)),
            hasJsonPath("$.componentRows[" + row + "][" + pos + "].numberOfItems", is(numberOfItems)));
    }

    /**
     * Matcher to verify that the current section has a search component at the
     * position pos of the row row with the given attributes.
     * 
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

    /**
     * Matcher to verify that the section with the given id has a textRow component
     * at the position pos of the row row with the given attributes.
     *
     * @param id              the section id to match
     * @param row             the row index of the top component to match
     * @param pos             the index of the top component in the given row
     * @param style           the component style
     * @return the Matcher instance
     */
    public static Matcher<? super Object> withIdAndTextRowComponent(String id, int row, int pos, String style,
                                                                   String contentType) {

        return allOf(
            hasJsonPath("$.id", is(id)),
            withTextRowComponent(row, pos, style, contentType));
    }

    /**
     * Matcher to verify that the section has a textRow component
     * at the position pos of the row row with the given attributes.
     *
     * @param row             the row index of the top component to match
     * @param pos             the index of the top component in the given row
     * @param style           the component style
     * @param contentType     the content type of the text row component
     * @return the Matcher instance
     */
    public static Matcher<? super Object> withTextRowComponent(int row, int pos, String style,
                                                                    String contentType) {

        return allOf(
            hasJsonPath("$.componentRows[" + row + "][" + pos + "].componentType", is("text-row")),
            hasJsonPath("$.componentRows[" + row + "][" + pos + "].style", is(style)),
            hasJsonPath("$.componentRows[" + row + "][" + pos + "].contentType", is(contentType)));
    }

    /**
     * Matcher to verify that the section with the given id has a counters component
     * at the position pos of the row row with the given discovery configurations.
     *
     * @param id                        section id
     * @param row                       row
     * @param pos                       position
     * @param style                     style to check
     * @param discoveryConfigurations   discovery configuration to check
     * @return
     */
    public static Matcher<? super Object> withIdAndCountersComponent(String id, int row, int pos, String style,
                                                     List<String> discoveryConfigurations) {
        return allOf(
            hasJsonPath("$.id", is(id)),
            withCountersComponent(row, pos, style, discoveryConfigurations));
    }

    /**
     * Matcher to verify that the section with the given id has a counters component
     * at the position pos of the row row with the given discovery configurations.
     *
     * @param row row
     * @param pos position
     * @param style style to check
     * @param discoveryConfigurations discovery configuration to check
     * @return
     */
    public static Matcher<? super Object> withCountersComponent(int row, int pos, String style,
                                                                     List<String> discoveryConfigurations) {
        List<Matcher<? super Object>> matchers = new ArrayList<>();
        matchers.add(hasJsonPath("$.componentRows[" + row + "][" + pos + "].componentType", is("counters")));
        matchers.add(hasJsonPath("$.componentRows[" + row + "][" + pos + "].style", is(style)));
        for (int i = 0; i < discoveryConfigurations.size(); i++) {
            matchers.add(withCounterComponent(row, pos, i, discoveryConfigurations.get(i)));
        }
        return allOf(matchers);
    }

    /**
     * Matcher to verify that the counter on pos pos of row row
     * at the position counterPos within counters list
     * has the given discovery configuration.
     *
     * @param row   row
     * @param pos   pos
     * @param counterPos position of the counter inside counter list
     * @param discoveryConfiguration discovery configuration to check
     * @return
     */
    public static Matcher<? super Object> withCounterComponent(int row, int pos, int counterPos,
                                                               String discoveryConfiguration) {
        return hasJsonPath("$.componentRows[" + row + "][" + pos + "].counterSettingsList[" + counterPos + "]" +
            ".discoveryConfigurationName", is(discoveryConfiguration));
    }
}
