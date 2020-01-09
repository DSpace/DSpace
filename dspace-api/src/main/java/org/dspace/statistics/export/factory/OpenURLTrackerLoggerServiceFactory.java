/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.statistics.export.factory;

import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.statistics.export.service.OpenURLTrackerLoggerService;

/**
 * Created by jonas - jonas@atmire.com on 09/02/17.
 */
public abstract class OpenURLTrackerLoggerServiceFactory {

    public abstract OpenURLTrackerLoggerService getOpenUrlTrackerLoggerService();

    public static OpenURLTrackerLoggerServiceFactory getInstance() {
        return DSpaceServicesFactory.getInstance().getServiceManager()
                                    .getServiceByName("openURLTrackerLoggerServiceFactory",
                                                      OpenURLTrackerLoggerServiceFactory.class);

    }

}
