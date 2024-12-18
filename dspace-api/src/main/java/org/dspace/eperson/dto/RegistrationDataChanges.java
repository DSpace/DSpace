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
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 **/
public class RegistrationDataChanges {

    private static final String EMAIL_PATTERN =
        "[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*@(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)" +
            "+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?";

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

    public boolean isValidEmail() {
        return email.matches(EMAIL_PATTERN);
    }

    public String getEmail() {
        return email;
    }

    public RegistrationTypeEnum getRegistrationType() {
        return registrationType;
    }
}
