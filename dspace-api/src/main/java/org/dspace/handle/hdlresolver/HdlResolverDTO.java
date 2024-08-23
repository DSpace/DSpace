/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.handle.hdlresolver;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Objects;

import org.apache.commons.lang3.Validate;
import org.dspace.core.Constants;

/**
 * Maps the URL of the request to an handle identifier
 * 
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.it)
 *
 */
public class HdlResolverDTO {

    private final String[] splittedString;
    private final String handle;

    /**
     * Decode a given URL
     * @param url URL
     * @return decoded URL
     */
    private static String decode(String url) {
        try {
            return URLDecoder.decode(url, Constants.DEFAULT_ENCODING);
        } catch (UnsupportedEncodingException e) {
            return url;
        }
    }

    /**
     * Default Constructor
     * 
     * @param requestURL is the complete Request URL
     * @param resolverSubPath is the rest service Sub-path
     */
    public HdlResolverDTO(final String requestURL, final String resolverSubPath) {
        Validate.notBlank(requestURL, "RequestURI not specified");
        Validate.notBlank(resolverSubPath, "fullPath not specified");
        this.splittedString = requestURL.split(resolverSubPath);
        if (Objects.nonNull(splittedString) && splittedString.length > 1) {
            // Decodes the URL-encoded characters of the String
            this.handle = decode(splittedString[1]);
        } else {
            this.handle = null;
        }
    }

    /**
     * Returns the split String of the resource-path
     * 
     * @return
     */
    public final String[] getSplittedString() {
        return this.splittedString;
    }

    /**
     * Returns the handle identifier
     * 
     * @return
     */
    public final String getHandle() {
        return this.handle;
    }

    /**
     * Checks if the handle identifier is valid.
     * 
     * @return
     */
    public boolean isValid() {
        return Objects.nonNull(this.handle) &&
                !"null".equalsIgnoreCase(this.handle) &&
                !this.handle.trim().isEmpty();
    }
}
