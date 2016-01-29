/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.statistics.factory;

import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.statistics.service.ElasticSearchLoggerService;
import org.dspace.statistics.service.SolrLoggerService;

/**
 * Factory implementation to get services for the statistics package, use StatisticsServiceFactory.getInstance() to retrieve an implementation
 *
 * @author kevinvandevelde at atmire.com
 */
public class StatisticsServiceFactoryImpl extends StatisticsServiceFactory {

    @Override
    public SolrLoggerService getSolrLoggerService() {
        // In order to lazy load, we cannot autowire it and instead load it by name
        return DSpaceServicesFactory.getInstance().getServiceManager().getServiceByName("solrLoggerService", SolrLoggerService.class);
    }

    @Override
    public ElasticSearchLoggerService getElasticSearchLoggerService() {
        // In order to lazy load, we cannot autowire it and instead load it by name
        return DSpaceServicesFactory.getInstance().getServiceManager().getServiceByName("elasticSearchLoggerService", ElasticSearchLoggerService.class);
    }
}
