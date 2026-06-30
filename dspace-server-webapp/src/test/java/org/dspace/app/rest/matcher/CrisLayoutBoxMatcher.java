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

import java.util.Collection;

import org.dspace.content.MetadataField;
import org.dspace.layout.CrisLayoutBox;
import org.hamcrest.Matcher;

public class CrisLayoutBoxMatcher {

    private CrisLayoutBoxMatcher() {}

    public static Matcher<? super Object> matchBox(CrisLayoutBox box) {
        return allOf(
                hasJsonPath("$.id", is(box.getID())),
                hasJsonPath("$.shortname", is(box.getShortname())),
                hasJsonPath("$.header", is(box.getHeader())),
                hasJsonPath("$.entityType", is(box.getEntitytype().getLabel())),
                hasJsonPath("$.container", is(box.isContainer())),
                hasJsonPath("$.collapsed", is(box.getCollapsed())),
                hasJsonPath("$.minor", is(box.getMinor())),
                hasJsonPath("$.style", is(box.getStyle())),
                hasJsonPath("$.security", is(box.getSecurity())),
                hasJsonPath("$.metadataSecurityFields",
                    containsInAnyOrder(metadataFields(box.getMetadataSecurityFields())))
        );
    }

    private static String[] metadataFields(Collection<MetadataField> metadataFields) {
        return metadataFields.stream()
            .map(metadataField -> metadataField.toString('.'))
            .toArray(String[]::new);
    }
}
