/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.validation.service.factory.impl;

import org.dspace.validation.service.ValidationService;
import org.dspace.validation.service.factory.ValidationServiceFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Concrete extension of {@link ValidationServiceFactory}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class ValidationServiceFactoryImpl extends ValidationServiceFactory {

    @Autowired
    private ValidationService validationService;

    @Override
    public ValidationService getValidationService() {
        return validationService;
    }

}
