/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.statistics.export.factory;

import org.dspace.statistics.export.service.OpenURLTrackerLoggerService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Created by jonas - jonas@atmire.com on 09/02/17.
 */
public class OpenURLTrackerLoggerServiceFactoryImpl extends OpenURLTrackerLoggerServiceFactory {

    @Autowired(required = true)
    private OpenURLTrackerLoggerService openURLTrackerLoggerService;

    @Override
    public OpenURLTrackerLoggerService getOpenUrlTrackerLoggerService() {
        return openURLTrackerLoggerService;
    }
}
