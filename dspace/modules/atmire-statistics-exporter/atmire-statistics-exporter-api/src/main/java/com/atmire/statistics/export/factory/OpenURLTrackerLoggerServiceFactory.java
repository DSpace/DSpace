package com.atmire.statistics.export.factory;

import com.atmire.statistics.export.service.OpenURLTrackerLoggerService;
import org.dspace.services.factory.DSpaceServicesFactory;

/**
 * Created by jonas - jonas@atmire.com on 09/02/17.
 */
public abstract class OpenURLTrackerLoggerServiceFactory {

    public abstract OpenURLTrackerLoggerService getOpenUrlTrackerLoggerService();

    public static OpenURLTrackerLoggerServiceFactory getInstance(){
        return DSpaceServicesFactory.getInstance().getServiceManager().getServiceByName("openURLTrackerLoggerServiceFactory", OpenURLTrackerLoggerServiceFactory.class);

    }

}
