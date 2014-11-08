/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.statistics.factory;

import org.dspace.statistics.service.ElasticSearchLoggerService;
import org.dspace.statistics.service.SolrLoggerService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Factory implementation to get services for the statistics package, use StatisticsServiceFactory.getInstance() to retrieve an implementation
 *
 * @author kevinvandevelde at atmire.com
 */
public class StatisticsServiceFactoryImpl extends StatisticsServiceFactory {

    @Autowired(required = true)
//    @Lazy
    private ElasticSearchLoggerService elasticSearchLogger;

    @Autowired(required = true)
    private SolrLoggerService solrLoggerService;

    @Override
    public SolrLoggerService getSolrLoggerService() {
        return solrLoggerService;
    }

    @Override
    public ElasticSearchLoggerService getElasticSearchLoggerService() {
        return elasticSearchLogger;
    }
}
