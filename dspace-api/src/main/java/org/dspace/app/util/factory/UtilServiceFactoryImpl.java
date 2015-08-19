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
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Factory implementation to get services for the util package, use UtilServiceFactory.getInstance() to retrieve an implementation
 *
 * @author kevinvandevelde at atmire.com
 */
public class UtilServiceFactoryImpl extends UtilServiceFactory {

    @Autowired(required = true)
    private MetadataExposureService metadataExposureService;
    @Autowired(required = true)
    private OpenSearchService openSearchService;
    @Autowired(required = true)
    private WebAppService webAppService;

    @Override
    public WebAppService getWebAppService() {
        return webAppService;
    }

    @Override
    public OpenSearchService getOpenSearchService() {
        return openSearchService;
    }

    @Override
    public MetadataExposureService getMetadataExposureService() {
        return metadataExposureService;
    }
}
