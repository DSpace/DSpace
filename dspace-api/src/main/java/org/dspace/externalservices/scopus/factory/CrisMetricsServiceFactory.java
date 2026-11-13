/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.externalservices.scopus.factory;
import org.dspace.app.metrics.service.CrisMetricsService;
import org.dspace.services.factory.DSpaceServicesFactory;

/**
 * Abstract factory to get services for the CrisMetrics package, use
 * CrisMetricsServiceFactory.getInstance() to retrieve an implementation.
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 *
 */
public abstract class CrisMetricsServiceFactory {

    public abstract CrisMetricsService getCrisMetricsService();

    public static CrisMetricsServiceFactory getInstance() {
        return DSpaceServicesFactory.getInstance().getServiceManager().getServiceByName(
                           "crisMetricsServiceFactory", CrisMetricsServiceFactory.class);
    }
}