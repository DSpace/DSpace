/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.eperson.dto;

import org.dspace.eperson.RegistrationTypeEnum;

/**
 * Class that embeds a change done for the {@link org.dspace.eperson.RegistrationData}
 *
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 **/
public class RegistrationDataChanges {

    @SuppressWarnings("checkstyle:LineLength")
    private static final String EMAIL_PATTERN = "^[a-zA-Z0-9.!#$%&'*+\\\\/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$";

    private final String email;
    private final RegistrationTypeEnum registrationType;

    public RegistrationDataChanges(String email, RegistrationTypeEnum type) {
        if (email == null || email.trim().isBlank()) {
            throw new IllegalArgumentException("Cannot update with an empty email address");
        }
        if (type == null) {
            throw new IllegalArgumentException("Cannot update with a null registration type");
        }
        this.email = email;
        if (!isValidEmail()) {
            throw new IllegalArgumentException("Invalid email address provided!");
        }
        this.registrationType = type;
    }

    /**
     * Checks if the email is valid using the EMAIL_PATTERN.
     * @return true if valid, false otherwise
     */
    public boolean isValidEmail() {
        return email.matches(EMAIL_PATTERN);
    }

    /**
     * Returns the email of change.
     *
     * @return the email of the change
     */
    public String getEmail() {
        return email;
    }

    /**
     * Returns the {@link RegistrationTypeEnum} of the registration.
     *
     * @return the type of the change
     */
    public RegistrationTypeEnum getRegistrationType() {
        return registrationType;
    }
}
