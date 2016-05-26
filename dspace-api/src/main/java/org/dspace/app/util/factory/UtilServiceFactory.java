/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.util.factory;

import org.dspace.app.util.service.MetadataExposureService;
import org.dspace.app.util.service.OpenSearchService;
import org.dspace.app.util.service.WebAppService;
import org.dspace.services.factory.DSpaceServicesFactory;

/**
 * Abstract factory to get services for the util package, use UtilServiceFactory.getInstance() to retrieve an implementation
 *
 * @author kevinvandevelde at atmire.com
 */
public abstract class UtilServiceFactory
{
    public abstract WebAppService getWebAppService();

    public abstract OpenSearchService getOpenSearchService();

    public abstract MetadataExposureService getMetadataExposureService();

    public static UtilServiceFactory getInstance()
    {
        return DSpaceServicesFactory.getInstance().getServiceManager().getServiceByName("appUtilServiceFactory", UtilServiceFactory.class);
    }

}
