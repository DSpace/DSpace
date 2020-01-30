/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter.factory;

import org.dspace.app.rest.converter.ConverterService;
import org.dspace.utils.DSpace;

/**
 * Abstract factory to get services for the services package, use
 * DSpaceServicesFactory.getInstance() to retrieve an implementation
 */
public abstract class ConverterServiceFactory {

    public abstract ConverterService getConverterService();

    public static ConverterServiceFactory getInstance() {
        return new DSpace().getServiceManager().getServiceByName("ConverterServiceFactory",
                ConverterServiceFactory.class);
    }
}
