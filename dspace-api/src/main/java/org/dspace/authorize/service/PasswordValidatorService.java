/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authorize.service;

/**
 * Interface for classes that validate a given password with a specific
 * strategy.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 */
public interface PasswordValidatorService {

    /**
     * Check if the password validator is active.
     */
    public boolean isPasswordValidationEnabled();

    /**
     * This method checks whether the password is valid
     * 
     * @param password password to validate
     */
    public boolean isPasswordValid(String password);
}
