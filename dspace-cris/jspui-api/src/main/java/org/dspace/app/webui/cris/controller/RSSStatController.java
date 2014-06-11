/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.webui.cris.controller;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.SolrServerException;
import org.dspace.app.cris.model.StatSubscription;
import org.dspace.app.cris.service.ApplicationService;
import org.dspace.app.cris.statistics.StatSyndicationFeed;
import org.dspace.app.cris.statistics.SummaryStatBean;
import org.dspace.app.cris.statistics.service.StatSubscribeService;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.multiaction.MultiActionController;

/**
 * This SpringMVC controller distributes RSS feeds with statistics update on
 * several types of object: Item, Collection, Community and Researcher Page
 * 
 * @author cilea
 * 
 */
public class RSSStatController extends MultiActionController
{

    private StatSubscribeService statSubscribeService;

    private List<String> formats;

    public RSSStatController(StatSubscribeService statSubscribeService)
    {
        String fmtsStr = ConfigurationManager.getProperty("webui.feed.formats");
        if (fmtsStr != null)
        {
            formats = new ArrayList<String>();
            String[] fmts = fmtsStr.split(",");
            for (int i = 0; i < fmts.length; i++)
            {
                formats.add(fmts[i].trim());
            }
        }
        this.statSubscribeService = statSubscribeService;
    }

    public ModelAndView daily(HttpServletRequest arg0, HttpServletResponse arg1)
            throws Exception
    {
        arg1.setContentType("application/rss+xml");
        processRSSStat(arg0, StatSubscription.FREQUENCY_DAILY).output(
                arg1.getWriter());
        arg1.getWriter().flush();
        return null;
    }

    public ModelAndView weekly(HttpServletRequest arg0, HttpServletResponse arg1)
            throws Exception
    {
        arg1.setContentType("application/rss+xml");
        processRSSStat(arg0, StatSubscription.FREQUENCY_WEEKLY).output(
                arg1.getWriter());
        arg1.getWriter().flush();
        return null;
    }

    public ModelAndView monthly(HttpServletRequest arg0,
            HttpServletResponse arg1) throws Exception
    {
        arg1.setContentType("application/rss+xml");
        processRSSStat(arg0, StatSubscription.FREQUENCY_MONTHLY).output(
                arg1.getWriter());
        arg1.getWriter().flush();
        return null;
    }

    private StatSyndicationFeed processRSSStat(HttpServletRequest arg0, int freq)
            throws SQLException, SolrServerException
    {
        String uid = arg0.getParameter("uid");
        int type = UIUtil.getIntParameter(arg0, "type");
        String feedType = arg0.getParameter("feedtype");
        Context context = UIUtil.obtainContext(arg0);
        int numsFeed = ConfigurationManager.getIntProperty("webui.feed.stats",
                4);
        SummaryStatBean summary = statSubscribeService.getStatBean(context,
                uid, type, freq, numsFeed);
        StatSyndicationFeed feed = new StatSyndicationFeed();

        if (StringUtils.isEmpty(feedType) || !formats.contains(feedType))
        {
            feedType = formats.get(0);
        }
        feed.setType(feedType);
        feed.populate(arg0, summary);
        return feed;
    }
}
