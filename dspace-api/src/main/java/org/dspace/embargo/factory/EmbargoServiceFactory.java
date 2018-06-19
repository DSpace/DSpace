/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.embargo.factory;

import org.dspace.embargo.service.EmbargoService;
import org.dspace.services.factory.DSpaceServicesFactory;

/**
 * Abstract factory to get services for the embargo package, use EmbargoServiceFactory.getInstance() to retrieve an implementation
 *
 * @author kevinvandevelde at atmire.com
 */
public abstract class EmbargoServiceFactory {

    public abstract EmbargoService getEmbargoService();

    public static EmbargoServiceFactory getInstance()
    {
        return DSpaceServicesFactory.getInstance().getServiceManager().getServiceByName("embargoServiceFactory", EmbargoServiceFactory.class);
    }
}
