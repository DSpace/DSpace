/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.matcher;

import java.util.Objects;

import org.dspace.content.MetadataValue;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

public class MetadataValueMatcher extends TypeSafeMatcher<MetadataValue> {

    private String field;

    private String value;

    private String language;

    private String authority;

    private Integer place;

    private Integer confidence;

    private MetadataValueMatcher(String field, String value, String language, String authority, Integer place,
        Integer confidence) {

        this.field = field;
        this.value = value;
        this.language = language;
        this.authority = authority;
        this.place = place;
        this.confidence = confidence;

    }

    @Override
    public void describeTo(Description description) {
        description.appendText("MetadataValue with the following attributes [value=" + value + ", language="
            + language + ", authority=" + authority + ", place=" + place + ", confidence=" + confidence + "]");
    }

    @Override
    protected boolean matchesSafely(MetadataValue metadataValue) {
        return Objects.equals(metadataValue.getValue(), value) &&
            Objects.equals(metadataValue.getMetadataField().toString('.'), field) &&
            Objects.equals(metadataValue.getLanguage(), language) &&
            Objects.equals(metadataValue.getAuthority(), authority) &&
            Objects.equals(metadataValue.getPlace(), place) &&
            Objects.equals(metadataValue.getConfidence(), confidence);
    }

    public static MetadataValueMatcher with(String field, String value, String language,
        String authority, Integer place, Integer confidence) {
        return new MetadataValueMatcher(field, value, language, authority, place, confidence);
    }

}
