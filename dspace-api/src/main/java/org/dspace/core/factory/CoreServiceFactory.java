/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.core.factory;

import org.dspace.core.service.LicenseService;
import org.dspace.core.service.NewsService;
import org.dspace.core.service.PluginService;
import org.dspace.services.factory.DSpaceServicesFactory;

/**
 * Abstract factory to get services for the core package, use CoreServiceFactory.getInstance() to retrieve an implementation
 *
 * @author kevinvandevelde at atmire.com
 */
public abstract class CoreServiceFactory {

    public abstract LicenseService getLicenseService();

    public abstract NewsService getNewsService();

    public abstract PluginService getPluginService();

    public static CoreServiceFactory getInstance()
    {
        return DSpaceServicesFactory.getInstance().getServiceManager().getServiceByName("coreServiceFactory", CoreServiceFactory.class);
    }
}
