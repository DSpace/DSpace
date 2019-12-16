/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.utils;

public class RegexUtils {

    private RegexUtils(){}

    /**
     * Regular expression in the request mapping to accept UUID as identifier
     */
    public static final String REGEX_REQUESTMAPPING_IDENTIFIER_AS_UUID =
        "/{uuid:[0-9a-fxA-FX]{8}-[0-9a-fxA-FX]{4}-[0-9a-fxA-FX]{4}-[0-9a-fxA-FX]{4}-[0-9a-fxA-FX]{12}}";

    /**
     * Regular expression in the request mapping to accept a string as identifier but not the other kind of
     * identifier (digits or uuid)
     */
    public static final String REGEX_REQUESTMAPPING_IDENTIFIER_AS_STRING_VERSION_STRONG = "/{id:^(?!^\\d+$)" +
        "(?!^[0-9a-fxA-FX]{8}-[0-9a-fxA-FX]{4}-[0-9a-fxA-FX]{4}-[0-9a-fxA-FX]{4}-[0-9a-fxA-FX]{12}$)[\\w+\\-]+$+}";

    /**
     * Regular expression in the request mapping to accept number as identifier
     */
    public static final String REGEX_REQUESTMAPPING_IDENTIFIER_AS_DIGIT = "/{id:\\d+}";

}
