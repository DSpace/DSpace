/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authorize;

import java.util.List;

import org.dspace.authorize.service.PasswordValidator;
import org.dspace.authorize.service.ValidatePasswordService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Basic implementation for validation password robustness.
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk@4science.com)
 */
public class ValidatePasswordServiceImpl implements ValidatePasswordService {

    @Autowired
    private List<PasswordValidator> validators;

    @Override
    public boolean isPasswordValid(Context context, String password) {
        return validators.stream()
            .filter(passwordValidator -> passwordValidator.isPasswordValidationEnabled())
            .allMatch(passwordValidator -> passwordValidator.isPasswordValid(context, password));
    }

}