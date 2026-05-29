/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.matomo.model;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;

import java.util.List;
import java.util.Map;

import org.dspace.AbstractUnitTest;
import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.Test;

public class MatomoRequestDetailsSplitterTest extends AbstractUnitTest {

    @Test
    public void testMatomoRequestDetailsSingleRequest() {
        Map<String, List<MatomoRequestDetails>> split = MatomoRequestDetailsSplitter.split(
            List.of(
                new MatomoRequestDetails()
                    .addParameter("_id", "first")
                    .addParameter("param1", "value1")
            )
        );
        MatcherAssert.assertThat(
            split.keySet(),
            CoreMatchers.hasItem("first")
        );
        MatcherAssert.assertThat(
            split.get("first").get(0).parameters,
            CoreMatchers.allOf(
                hasEntry("_id", "first"), hasEntry("param1", "value1")
            )
        );
    }

    @Test
    public void testMatomoRequestDetailsNoIdRequest() {
        Map<String, List<MatomoRequestDetails>> split = MatomoRequestDetailsSplitter.split(
            List.of(
                new MatomoRequestDetails()
                    .addParameter("param1", "value1")
                    .addParameter("param2", "value2")
            )
        );
        MatcherAssert.assertThat(
            split.keySet(),
            CoreMatchers.hasItem("default")
        );
        MatcherAssert.assertThat(
            split.get("default").get(0).parameters,
            CoreMatchers.allOf(
                hasEntry("param2", "value2"), hasEntry("param1", "value1")
            )
        );
    }


    @Test
    public void testMatomoMultipleRequests() {
        Map<String, List<MatomoRequestDetails>> split = MatomoRequestDetailsSplitter.split(
            List.of(
                new MatomoRequestDetails()
                    .addParameter("_id", "first")
                    .addParameter("param2", "value2"),
                new MatomoRequestDetails()
                    .addParameter("_id", "first")
                    .addParameter("param1", "value1")
            )
        );
        MatcherAssert.assertThat(
            split.keySet(),
            hasItem("first")
        );
        MatcherAssert.assertThat(
            split.keySet(),
            not(hasItem("default"))
        );
        MatcherAssert.assertThat(
            split.get("first").get(0).parameters,
            allOf(
                hasEntry("param2", "value2"),
                hasEntry("_id", "first")
            )
        );
        MatcherAssert.assertThat(
            split.get("first").get(1).parameters,
            allOf(
                hasEntry("param1", "value1"),
                hasEntry("_id", "first")
            )
        );
    }

    @Test
    public void testMatomoMultipleRequestsNoId() {
        Map<String, List<MatomoRequestDetails>> split = MatomoRequestDetailsSplitter.split(
            List.of(
                new MatomoRequestDetails()
                    .addParameter("_id", "first")
                    .addParameter("param2", "value2"),
                new MatomoRequestDetails()
                    .addParameter("_id", "first")
                    .addParameter("param1", "value1"),
                new MatomoRequestDetails()
                    .addParameter("param3", "value3")
                    .addParameter("param4", "value4")
            )
        );
        MatcherAssert.assertThat(
            split.keySet(),
            hasItem("first")
        );
        MatcherAssert.assertThat(
            split.keySet(),
            hasItem("default")
        );
        MatcherAssert.assertThat(
            split.get("first").get(0).parameters,
            allOf(
                hasEntry("param2", "value2"),
                hasEntry("_id", "first")
            )
        );
        MatcherAssert.assertThat(
            split.get("first").get(1).parameters,
            allOf(
                hasEntry("param1", "value1"),
                hasEntry("_id", "first")
            )
        );
        MatcherAssert.assertThat(
            split.get("default").get(0).parameters,
            allOf(
                hasEntry("param3", "value3"),
                hasEntry("param4", "value4")
            )
        );
    }

}
