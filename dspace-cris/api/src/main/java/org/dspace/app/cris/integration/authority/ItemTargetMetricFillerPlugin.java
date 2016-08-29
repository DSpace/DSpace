package org.dspace.app.cris.integration.authority;

import java.util.Calendar;
import java.util.Date;

import org.apache.commons.lang.StringUtils;
import org.dspace.app.cris.metrics.common.model.CrisMetrics;
import org.dspace.app.cris.metrics.common.services.MetricsPersistenceService;
import org.dspace.app.cris.service.ApplicationService;
import org.dspace.content.Item;
import org.dspace.content.Metadatum;
import org.dspace.core.Context;

public class ItemTargetMetricFillerPlugin extends TargetMetricFillerPlugin
{

    @Override
    public void buildMetric(Context context, Item item, Metadatum m,
            ApplicationService applicationService,
            MetricsPersistenceService metricService)
    {
        CrisMetrics metric = new CrisMetrics();
        metric.setMetricCount(Double.parseDouble(m.value));
        metric.setMetricType(m.qualifier);
        metric.setResourceId(item.getID());
        metric.setResourceTypeId(item.getType());
        metric.setUuid(item.getHandle());

        String acquisitionYear = item
                .getMetadata("metrics.common.acquisitionyear");

        int year = -1;
        if (StringUtils.isNotBlank(acquisitionYear))
        {
            year = Integer.parseInt(acquisitionYear);
        }
        else
        {
            // get a calendar using the default time zone and locale.
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(new Date());
            year = calendar.get(Calendar.YEAR);
        }

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, year);
        cal.set(Calendar.DAY_OF_YEAR, 1);
        Date start = cal.getTime();

        // set date to last day of year
        cal.set(Calendar.YEAR, year);
        cal.set(Calendar.MONTH, 11); // 11 = december
        cal.set(Calendar.DAY_OF_MONTH, 31); // new years eve
        Date end = cal.getTime();

        metric.setStartDate(start);
        metric.setEndDate(end);

        metricService.saveOrUpdate(CrisMetrics.class, metric);
    }

}
