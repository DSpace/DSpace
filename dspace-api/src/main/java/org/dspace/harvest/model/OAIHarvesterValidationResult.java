/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.harvest.model;

/**
 * Model the result of the validation performed by
 * {@link OAIHarvesterValidator}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public final class OAIHarvesterValidationResult {

    private final String message;

    private final boolean valid;

    public static OAIHarvesterValidationResult valid() {
        return new OAIHarvesterValidationResult("", true);
    }

    public static OAIHarvesterValidationResult invalid(String message) {
        return new OAIHarvesterValidationResult(message, false);
    }

    public OAIHarvesterValidationResult(String message, boolean valid) {
        this.message = message;
        this.valid = valid;
    }

    public String getMessage() {
        return message;
    }

    public boolean isValid() {
        return valid;
    }

    public boolean isNotValid() {
        return !isValid();
    }

}
