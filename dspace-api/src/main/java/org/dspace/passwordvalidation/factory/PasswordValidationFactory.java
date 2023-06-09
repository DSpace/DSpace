/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.passwordvalidation.factory;

import org.dspace.authorize.service.PasswordValidatorService;
import org.dspace.services.factory.DSpaceServicesFactory;

/**
 * Abstract factory to get services for the passwordvalidation package,
 * use PasswordValidationFactory.getInstance() to retrieve an implementation.
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk@4science.com)
 */
public abstract class PasswordValidationFactory {

    public abstract PasswordValidatorService getPasswordValidationService();

    public static PasswordValidationFactory getInstance() {
        return DSpaceServicesFactory.getInstance()
                                    .getServiceManager()
                                    .getServiceByName("validationPasswordFactory", PasswordValidationFactory.class);
    }

}