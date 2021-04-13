/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.validation.service.factory;

import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.validation.service.ValidationService;

/**
 * Abstract factory to get services related to validation.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public abstract class ValidationServiceFactory {

    public abstract ValidationService getValidationService();

    public static ValidationServiceFactory getInstance() {
        return DSpaceServicesFactory.getInstance().getServiceManager()
            .getServiceByName("validationServiceFactory", ValidationServiceFactory.class);
    }
}
