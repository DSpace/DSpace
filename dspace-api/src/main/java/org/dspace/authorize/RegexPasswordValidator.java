/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authorize;

import static org.apache.commons.lang.StringUtils.isNotBlank;

import java.util.regex.Pattern;

import org.dspace.authorize.service.PasswordValidator;
import org.dspace.core.Context;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of {@link PasswordValidator} that verifies if the given
 * passowrd matches the configured pattern.
 * 
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 */
public class RegexPasswordValidator implements PasswordValidator {

    @Autowired
    private ConfigurationService configurationService;

    @Override
    public boolean isPasswordValidationEnabled() {
        return isNotBlank(getPasswordValidationPattern());
    }

    @Override
    public boolean isPasswordValid(Context context, String password) {
        if (!isPasswordValidationEnabled()) {
            return true;
        }

        Pattern pattern = Pattern.compile(getPasswordValidationPattern());
        return pattern.matcher(password).find();
    }

    private String getPasswordValidationPattern() {
        return configurationService.getProperty("eperson.password.regex-validation.pattern");
    }

}
