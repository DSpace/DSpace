/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.matcher;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static org.dspace.app.rest.matcher.GroupMatcher.matchGroupEntry;
import static org.dspace.app.rest.matcher.ItemMatcher.matchItemProperties;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;

import org.dspace.eperson.Group;
import org.dspace.supervision.SupervisionOrder;
import org.hamcrest.Matcher;

/**
 * Utility class to construct a Matcher for an SupervisionOrder object
 *
 * @author Mohamed Eskander (mohamed.eskander at 4science dot it)
 */
public class SupervisionOrderMatcher {

    private SupervisionOrderMatcher() {
    }

    public static Matcher<? super Object> matchSuperVisionOrder(SupervisionOrder supervisionOrder) {
        Group group = supervisionOrder.getGroup();
        return allOf(
            hasJsonPath("$.id", is(supervisionOrder.getID())),
            hasJsonPath("$._embedded.item", matchItemProperties(supervisionOrder.getItem())),
            hasJsonPath("$._embedded.group", matchGroupEntry(group.getID(), group.getName()))
        );
    }

}
