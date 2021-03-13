/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * This is the exception to capture details about a not existing linked resource
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
@ResponseStatus(value = HttpStatus.NOT_FOUND, reason = "This link is not found in the system")
public class LinkNotFoundException extends RuntimeException {
    String apiCategory;
    String model;
    String id;

    public LinkNotFoundException(String apiCategory, String model, String id) {
        this.apiCategory = apiCategory;
        this.model = model;
        this.id = id;
    }

}
