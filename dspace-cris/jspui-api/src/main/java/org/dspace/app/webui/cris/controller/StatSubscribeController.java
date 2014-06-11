/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.webui.cris.controller;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.dspace.app.cris.statistics.StatSubscriptionViewBean;
import org.dspace.app.cris.statistics.service.StatSubscribeService;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.content.DSpaceObject;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.handle.HandleManager;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.multiaction.MultiActionController;

/**
 * This SpringMVC controller allows a dspace user to subscribe, unsubscribe and
 * list alert notification of statistics update
 * 
 * @author cilea
 * 
 */
public class StatSubscribeController extends MultiActionController
{

    /**
     * the service to manage stat subscriptions, injected by Spring IoC
     */
    private StatSubscribeService statSubService;

    private String listView;

    public StatSubscribeController(StatSubscribeService statSubService,
            String listView)
    {
        super();
        this.statSubService = statSubService;
        this.listView = listView;
    }

    public ModelAndView subscribe(HttpServletRequest arg0,
            HttpServletResponse arg1) throws Exception
    {
        String uuid = arg0.getParameter("uid");
        int type = UIUtil.getIntParameter(arg0, "type");
        boolean showList = UIUtil.getBoolParameter(arg0, "list");
        int[] freqs = UIUtil.getIntParameters(arg0, "freq");
        Context context = UIUtil.obtainContext(arg0);
        EPerson e = context.getCurrentUser();

        statSubService.subscribeUUID(e, uuid, freqs, type);

        if (showList)
        {
            return list(arg0, arg1);
        }
        else
        {
            String url = ConfigurationManager.getProperty("dspace.url")
                    + "/cris/stats/";

            switch (type)
            {
            case Constants.ITEM:
                url += "item.html";
                url += "?handle=" + uuid;
                break;
            case Constants.COLLECTION:
                url += "collection.html";
                url += "?handle=" + uuid;
                break;
            case Constants.COMMUNITY:
                url += "community.html";
                url += "?handle=" + uuid;
                break;
            default:
                url += statSubService.getAs().getEntityByUUID(uuid)
                        .getPublicPath()
                        + ".html";
                url += "?id=" + uuid;
            }            

            return new ModelAndView("redirect:" + url);
        }
    }

    public ModelAndView unsubscribe(HttpServletRequest arg0,
            HttpServletResponse arg1) throws Exception
    {
        String clearAll = arg0.getParameter("clear");
        String uuid = arg0.getParameter("uid");
        int type = UIUtil.getIntParameter(arg0, "type");
        boolean showList = UIUtil.getBoolParameter(arg0, "list");
        Context context = UIUtil.obtainContext(arg0);
        EPerson e = context.getCurrentUser();
        if (StringUtils.isNotEmpty(clearAll)
                && clearAll.equalsIgnoreCase("all"))
        {
            statSubService.clearAll(e);
            showList = true;
        }
        else if (StringUtils.isNotEmpty(uuid))
        {
            statSubService.unsubscribeUUID(e, uuid);
        }

        if (showList)
        {
            return list(arg0, arg1);
        }
        else
        {
            String url = ConfigurationManager.getProperty("dspace.url");
            if (type >= 9)
            {
                url += "/cris/uuid/" + uuid;
            }
            else
            {
                url += "/handle/" + uuid;
            }
            return new ModelAndView("redirect:" + url);
        }
    }

    public ModelAndView list(HttpServletRequest arg0, HttpServletResponse arg1)
            throws Exception
    {
        Context context = UIUtil.obtainContext(arg0);
        EPerson e = context.getCurrentUser();

        List<StatSubscriptionViewBean> subscriptions = statSubService
                .getSubscriptions(context, e);

        ModelAndView mv = new ModelAndView(listView);
        mv.addObject("subscriptions", subscriptions);
        return mv;
    }
}
