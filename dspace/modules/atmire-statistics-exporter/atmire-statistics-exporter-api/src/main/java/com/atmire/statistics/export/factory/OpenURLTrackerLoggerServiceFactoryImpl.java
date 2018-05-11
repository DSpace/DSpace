package com.atmire.statistics.export.factory;

import com.atmire.statistics.export.service.OpenURLTrackerLoggerService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Created by jonas - jonas@atmire.com on 09/02/17.
 */
public class OpenURLTrackerLoggerServiceFactoryImpl extends OpenURLTrackerLoggerServiceFactory{

    @Autowired(required = true)
    private OpenURLTrackerLoggerService openURLTrackerLoggerService;

    @Override
    public OpenURLTrackerLoggerService getOpenUrlTrackerLoggerService() {
        return openURLTrackerLoggerService;
    }
}
