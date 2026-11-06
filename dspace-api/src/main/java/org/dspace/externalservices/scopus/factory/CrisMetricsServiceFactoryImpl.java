/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.externalservices.scopus.factory;
import org.dspace.app.metrics.service.CrisMetricsService;
import org.springframework.beans.factory.annotation.Autowired;

public class CrisMetricsServiceFactoryImpl extends CrisMetricsServiceFactory {

    @Autowired
    private CrisMetricsService crisMetricsService;

    @Override
    public CrisMetricsService getCrisMetricsService() {
        return crisMetricsService;
    }

}