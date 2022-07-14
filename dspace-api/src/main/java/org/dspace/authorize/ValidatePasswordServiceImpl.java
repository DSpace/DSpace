/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authorize;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.dspace.app.exception.PasswordNotValidException;
import org.dspace.authorize.service.ValidatePasswordService;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Basic implementation for validation password robustness
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk@4science.com)
 */
public class ValidatePasswordServiceImpl implements ValidatePasswordService {

    @Autowired
    private ConfigurationService configurationService;

    @Override
    public boolean isEnabledValidatePassword() {
        return StringUtils.isNotBlank(configurationService.getProperty("validate-password-reg-expression"));
    }

    @Override
    public boolean validatePasswordRobustness(String password) {
        if (isEnabledValidatePassword()) {
            String expression = configurationService.getProperty("validate-password-reg-expression");
            Pattern pattern = Pattern.compile(expression);
            Matcher matcher = pattern.matcher(password);
            if (!matcher.find()) {
                throw new PasswordNotValidException("The provide password isn't valid!");
            }
        }
        return true;
    }

}