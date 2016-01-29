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
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Factory implementation to get services for the core package, use CoreServiceFactory.getInstance() to retrieve an implementation
 *
 * @author kevinvandevelde at atmire.com
 */
public class CoreServiceFactoryImpl extends CoreServiceFactory {

    @Autowired(required = true)
    private LicenseService licenseService;

    @Autowired(required = true)
    private NewsService newsService;

    @Autowired(required = true)
    private PluginService pluginService;

    @Override
    public LicenseService getLicenseService() {
        return licenseService;
    }

    @Override
    public NewsService getNewsService() {
        return newsService;
    }

    @Override
    public PluginService getPluginService() {
        return pluginService;
    }
}
