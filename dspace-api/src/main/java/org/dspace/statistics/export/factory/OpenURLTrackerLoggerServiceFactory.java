/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.statistics.export.factory;

import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.statistics.export.service.FailedOpenURLTrackerService;
import org.dspace.statistics.export.service.OpenUrlService;

/**
 * The service factory for the OpenUrlTracker related services
 */
public abstract class OpenURLTrackerLoggerServiceFactory {

    /**
     * Returns the FailedOpenURLTrackerService
     * @return FailedOpenURLTrackerService instance
     */
    public abstract FailedOpenURLTrackerService getOpenUrlTrackerLoggerService();

    /**
     * Retrieve the OpenURLTrackerLoggerServiceFactory
     * @return OpenURLTrackerLoggerServiceFactory instance
     */
    public static OpenURLTrackerLoggerServiceFactory getInstance() {
        return DSpaceServicesFactory.getInstance().getServiceManager()
                                    .getServiceByName("openURLTrackerLoggerServiceFactory",
                                                      OpenURLTrackerLoggerServiceFactory.class);

    }

    /**
     * Returns the OpenUrlService
     * @return OpenUrlService instance
     */
    public abstract OpenUrlService getOpenUrlService();
}
