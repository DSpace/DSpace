/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.statistics.plugin;

import java.sql.SQLException;
import java.util.Calendar;
import java.util.Date;

import org.apache.log4j.Logger;
import org.dspace.app.cris.discovery.CrisSearchService;
import org.dspace.app.cris.metrics.common.dao.MetricsApplicationDao;
import org.dspace.app.cris.metrics.common.services.MetricsPersistenceService;
import org.dspace.app.cris.model.StatSubscription;
import org.dspace.app.cris.service.ApplicationService;
import org.dspace.app.cris.statistics.CrisSolrLogger;
import org.dspace.core.Context;
import org.dspace.discovery.SearchServiceException;
import org.dspace.kernel.ServiceManager;
import org.dspace.utils.DSpace;

public class StatsPeriodIndicatorsPlugin extends AStatsIndicatorsPlugin
{

    private static Logger log = Logger
            .getLogger(StatsPeriodIndicatorsPlugin.class);

    private String type;

    private String frequency;

    @Override
    public void buildIndicator(Context context,
            ApplicationService applicationService, CrisSolrLogger statsService,
            CrisSearchService searchService, String filter)
                    throws SearchServiceException
    {

        ServiceManager serviceManager = new DSpace().getServiceManager();
        MetricsPersistenceService pService = serviceManager.getServiceByName(
                MetricsPersistenceService.class.getName(),
                MetricsPersistenceService.class);

        long rangeLimitSx;
        long rangeLimitDx;

        if ("_last1".equals(frequency))
        {
            // get a week ago and clear time of day
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DATE, -7);

            cal.set(Calendar.HOUR_OF_DAY, 0); // ! clear would not reset the
                                              // hour of day !
            cal.clear(Calendar.MINUTE);
            cal.clear(Calendar.SECOND);
            cal.clear(Calendar.MILLISECOND);

            // get start of week in milliseconds
            cal.set(Calendar.DAY_OF_WEEK, cal.getFirstDayOfWeek());
            rangeLimitSx = cal.getTimeInMillis();

            // start of the next week
            cal.add(Calendar.WEEK_OF_YEAR, 1);
            rangeLimitDx = cal.getTimeInMillis();
        }
        else
        {
            // get a month ago and clear time of day
            Calendar cal = Calendar.getInstance();
            cal = Calendar.getInstance();
            cal.add(Calendar.MONTH, -1);
            cal.set(Calendar.HOUR_OF_DAY, 0); // ! clear would not reset the
                                              // hour of day !
            cal.clear(Calendar.MINUTE);
            cal.clear(Calendar.SECOND);
            cal.clear(Calendar.MILLISECOND);

            // get start of the month
            cal.set(Calendar.DAY_OF_MONTH, 1);
            rangeLimitSx = cal.getTimeInMillis();

            // get start of the next month
            cal.add(Calendar.MONTH, 1);
            rangeLimitDx = cal.getTimeInMillis();
        }

        pService.buildPeriodMetrics(context, frequency, type, rangeLimitSx,
                        rangeLimitDx);
        
        if(isRenewMetricsCache()) {
            searchService.renewMetricsCache();
        }   
    }

    public String getType()
    {
        return type;
    }

    public void setType(String type)
    {
        this.type = type;
    }

    public String getFrequency()
    {
        return frequency;
    }

    public void setFrequency(String frequency)
    {
        this.frequency = frequency;
    }

}
