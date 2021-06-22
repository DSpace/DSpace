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

import java.util.LinkedList;
import java.util.List;

import org.hamcrest.Matcher;

public class SubmissionCCLicenseMatcher {

    private SubmissionCCLicenseMatcher() {
    }

    public static Matcher<? super Object> matchLicenseEntry(int count, int[] amountOfFieldsAndEnums) {
        return allOf(
                matchLicenseProperties(count),
                matchFields(count, amountOfFieldsAndEnums)
        );
    }

    private static Matcher<? super Object> matchFields(int count, int[] amountOfFieldsAndEnums) {
        List<Matcher<? super Object>> matchers = new LinkedList<>();
        for (int index = 0; index < amountOfFieldsAndEnums.length; index++) {
            matchers.add(matchField(count, index, amountOfFieldsAndEnums[index]));
        }
        return hasJsonPath("$.fields", containsInAnyOrder(matchers));
    }

    private static Matcher<? super Object> matchField(int count, int fieldIndex, int amountOfEnums) {
        return allOf(
                matchLicenseFieldProperties(count, fieldIndex),
                matchEnums(count, fieldIndex, amountOfEnums)
        );

    }

    private static Matcher<? super Object> matchEnums(int count, int fieldIndex, int amountOfEnums) {
        List<Matcher<? super Object>> matchers = new LinkedList<>();
        for (int index = 0; index < amountOfEnums; index++) {
            matchers.add(matchLicenseFieldEnumProperties(count, fieldIndex, index));
        }
//        return hasJsonPath("$.enums");
        return hasJsonPath("$.enums", containsInAnyOrder(matchers));
    }


    public static Matcher<? super Object> matchLicenseProperties(int count) {
        return allOf(
                hasJsonPath("$.id", is("license" + count)),
                hasJsonPath("$.name", is("License " + count + " - Name"))
        );
    }

    public static Matcher<? super Object> matchLicenseFieldProperties(int count, int fieldIndex) {
        return allOf(
                hasJsonPath("$.id", is("license" + count + "-field" + fieldIndex)),
                hasJsonPath("$.label", is("License " + count + " - Field " + fieldIndex + " - Label")),
                hasJsonPath("$.description", is("License " + count + " - Field " + fieldIndex + " - Description"))
        );
    }

    public static Matcher<? super Object> matchLicenseFieldEnumProperties(int count, int fieldIndex, int enumIndex) {
        return allOf(
                hasJsonPath("$.id", is("license" + count + "-field" + fieldIndex + "-enum" + enumIndex)),
                hasJsonPath("$.label",
                    is("License " + count + " - Field " + fieldIndex + " - Enum " + enumIndex + " - Label")),
                hasJsonPath("$.description",
                    is("License " + count + " - Field " + fieldIndex + " - Enum " + enumIndex + " - " + "Description"))
        );
    }
}
