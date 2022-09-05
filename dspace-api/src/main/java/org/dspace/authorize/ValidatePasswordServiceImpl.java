/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authorize;

import java.util.List;

import org.dspace.authorize.service.PasswordValidatorService;
import org.dspace.authorize.service.ValidatePasswordService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Basic implementation for validation password robustness.
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk@4science.com)
 */
public class ValidatePasswordServiceImpl implements ValidatePasswordService {

    @Autowired
    private List<PasswordValidatorService> validators;

    @Override
    public boolean isPasswordValid(String password) {
        return validators.stream()
            .filter(passwordValidator -> passwordValidator.isPasswordValidationEnabled())
            .allMatch(passwordValidator -> passwordValidator.isPasswordValid(password));
    }

}