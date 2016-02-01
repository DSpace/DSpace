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
 * Abstract factory to get services for the statistics package, use StatisticsServiceFactory.getInstance() to retrieve an implementation
 *
 * @author kevinvandevelde at atmire.com
 */
public abstract class StatisticsServiceFactory {

    public abstract SolrLoggerService getSolrLoggerService();

    public abstract ElasticSearchLoggerService getElasticSearchLoggerService();

    public static StatisticsServiceFactory getInstance()
    {
        return DSpaceServicesFactory.getInstance().getServiceManager().getServiceByName("statisticsServiceFactory", StatisticsServiceFactory.class);
    }
}
