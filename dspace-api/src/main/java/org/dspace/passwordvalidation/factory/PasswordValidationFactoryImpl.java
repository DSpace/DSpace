/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.passwordvalidation.factory;

import org.dspace.authorize.service.PasswordValidatorService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Factory implementation to get services for the PasswordValidation package,
 * use PasswordValidationFactory.getInstance() to retrieve an implementation.
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk@4science.com)
 */
public class PasswordValidationFactoryImpl extends PasswordValidationFactory {

    @Autowired(required = true)
    private PasswordValidatorService PasswordValidatorService;

    @Override
    public PasswordValidatorService getPasswordValidationService() {
        return PasswordValidatorService;
    }

}