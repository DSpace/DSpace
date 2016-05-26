/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.license.factory;

import org.dspace.license.service.CreativeCommonsService;
import org.dspace.services.factory.DSpaceServicesFactory;

/**
 * Abstract factory to get services for the license package, use LicenseServiceFactory.getInstance() to retrieve an implementation
 *
 * @author kevinvandevelde at atmire.com
 */
public abstract class LicenseServiceFactory {

    public abstract CreativeCommonsService getCreativeCommonsService();

    public static LicenseServiceFactory getInstance()
    {
        return DSpaceServicesFactory.getInstance().getServiceManager().getServiceByName("licenseServiceFactory", LicenseServiceFactory.class);
    }
}
