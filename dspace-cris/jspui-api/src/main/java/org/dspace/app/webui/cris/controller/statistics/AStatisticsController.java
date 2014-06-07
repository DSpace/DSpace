/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.webui.cris.controller.statistics;


import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.dspace.app.cris.integration.statistics.IStatComponentService;
import org.dspace.app.cris.integration.statistics.IStatsComponent;
import org.dspace.app.cris.integration.statistics.IStatsGenericComponent;
import org.dspace.app.cris.integration.statistics.StatComponentsService;
import org.dspace.app.cris.model.StatSubscription;
import org.dspace.app.cris.service.ApplicationService;
import org.dspace.app.cris.statistics.util.StatsConfig;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;


public abstract class AStatisticsController<T extends IStatsGenericComponent> implements Controller
{
    
    /** log4j logger */
    private static Logger log = Logger
            .getLogger(AStatisticsController.class);
    
    protected ApplicationService applicationService;
    
    protected StatsConfig solrConfig;   
    
    protected static final String _RESULT_BEAN = "resultBean";

    protected static final String _JSP_KEY = "jspKey";

    protected static final String _ID_LABEL = "id";

    protected IStatComponentService<T> statsComponentsService;

    String jspKey;

    String success;

    String error;

    public String getJspKey()
    {
        return jspKey;
    }

    public void setJspKey(String jspKey)
    {
        this.jspKey = jspKey;
    }

    public String getSuccess()
    {
        return success;
    }

    public void setSuccess(String success)
    {
        this.success = success;
    }

    public String getError()
    {
        return error;
    }

    public void setError(String error)
    {
        this.error = error;
    }

   
    public abstract String getId(HttpServletRequest request);
    
    public abstract DSpaceObject getObject(HttpServletRequest request);
    
    public abstract String getTitle(HttpServletRequest request);
    
       
    public void addSubscriptionStatus(ModelAndView modelAndView,
            HttpServletRequest request)
    {
        try
        {
            Context context = UIUtil.obtainContext(request);
            EPerson currUser = context.getCurrentUser();
            if (currUser != null)
            {
                List<StatSubscription> statSubs = applicationService
                        .getStatSubscriptionByEPersonIDAndUID(currUser.getID(), getObject(request).getHandle());                                
                for (StatSubscription sub : statSubs)
                {
                    switch (sub.getFreq())
                    {
                    case StatSubscription.FREQUENCY_DAILY:
                        modelAndView.addObject("dailysubscribed", true);
                        break;
                    case StatSubscription.FREQUENCY_WEEKLY:
                        modelAndView.addObject("weeklysubscribed", true);
                        break;
                    case StatSubscription.FREQUENCY_MONTHLY:
                        modelAndView.addObject("monthlysubscribed", true);
                        break;
                    }
                }
            }
        }
        catch (Exception e)
        {   
            log.error(e.getMessage(), e);
        }
    }

    
    
    public StatsConfig getSolrConfig()
    {
        return solrConfig;
    }

    public void setSolrConfig(StatsConfig solrConfig)
    {
        this.solrConfig = solrConfig;
    }


    public ApplicationService getApplicationService()
    {
        return applicationService;
    }

    public void setApplicationService(ApplicationService applicationService)
    {
        this.applicationService = applicationService;
    }

    public IStatComponentService<T> getStatsComponentsService()
    {
        return statsComponentsService;
    }

    public void setStatsComponentsService(
            IStatComponentService<T> statsComponentsService)
    {
        this.statsComponentsService = statsComponentsService;
    }
   
}
