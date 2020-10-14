/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.statistics.export.factory;

import org.dspace.statistics.export.service.FailedOpenURLTrackerService;
import org.dspace.statistics.export.service.OpenUrlService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * The service factory implementation for the OpenUrlTracker related services
 */
public class OpenURLTrackerLoggerServiceFactoryImpl extends OpenURLTrackerLoggerServiceFactory {

    @Autowired(required = true)
    private FailedOpenURLTrackerService failedOpenURLTrackerService;

    @Autowired(required = true)
    private OpenUrlService openUrlService;

    /**
     * Returns the FailedOpenURLTrackerService
     * @return FailedOpenURLTrackerService instance
     */
    @Override
    public FailedOpenURLTrackerService getOpenUrlTrackerLoggerService() {
        return failedOpenURLTrackerService;
    }

    /**
     * Returns the OpenUrlService
     * @return OpenUrlService instance
     */
    @Override
    public OpenUrlService getOpenUrlService() {
        return openUrlService;
    }
}
