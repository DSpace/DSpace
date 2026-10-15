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

import org.dspace.app.rest.model.DynamicLayoutTabRest;
import org.dspace.layout.DynamicLayoutTab;
import org.hamcrest.Matcher;

public class DynamicLayoutTabMatcher {

    private DynamicLayoutTabMatcher() {}

    public static Matcher<? super Object> matchTab(DynamicLayoutTab tab) {
        return allOf(
                hasJsonPath("$.id", is(tab.getID())),
                hasJsonPath("$.shortname", is(tab.getShortName())),
                hasJsonPath("$.header", is(tab.getHeader())),
                hasJsonPath("$.priority", is(tab.getPriority())),
                hasJsonPath("$.security", is(tab.getSecurity()))
        );
    }

    public static Matcher<? super Object> matchRest(DynamicLayoutTabRest rest) {
        return allOf(
                hasJsonPath("$.shortname", is(rest.getShortname())),
                hasJsonPath("$.header", is(rest.getHeader())),
                hasJsonPath("$.priority", is(rest.getPriority())),
                hasJsonPath("$.security", is(rest.getSecurity())),
                hasJsonPath("$.entityType", is(rest.getEntityType()))
        );
    }

}
