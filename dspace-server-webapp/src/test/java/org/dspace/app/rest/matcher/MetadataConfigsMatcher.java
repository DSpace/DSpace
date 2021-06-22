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
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.is;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.hamcrest.Matcher;

/**
 * Class used for matching metadata configurations in JSON
 */
public class MetadataConfigsMatcher {

    private MetadataConfigsMatcher() { }

    public static Matcher<? super Object> matchMetadataConfigs(List<Map<String,String>> configs) {

        /**
         * This function returns a list of values, containing the values matching the key for each
         * configuration (Map<String,String>) in the list. For example, getAllValues("id") returns
         * a list with the ids of every configuration in the list.
         */
        Function<String,List<String>> getAllValues = key -> configs.stream()
                                                                    .map(x -> x.get(key))
                                                                    .collect(Collectors.toList());

        return allOf(
            hasJsonPath("$.configs[*].id", is(getAllValues.apply("id"))),
            hasJsonPath("$.configs[*].label", is(getAllValues.apply("label"))),
            hasJsonPath("$.configs[*].namespace", is(getAllValues.apply("namespace"))),
            hasJsonPath("$._links.self.href", endsWith("/api/config/harvestermetadata"))
        );
    }

}
