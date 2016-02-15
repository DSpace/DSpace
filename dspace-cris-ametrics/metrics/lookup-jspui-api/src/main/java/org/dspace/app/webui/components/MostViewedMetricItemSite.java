/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.webui.components;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrServerException;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.discovery.SearchServiceException;
import org.dspace.discovery.SearchUtils;
import org.dspace.plugin.PluginException;
import org.dspace.plugin.SiteHomeProcessor;
import org.dspace.utils.DSpace;

public class MostViewedMetricItemSite implements SiteHomeProcessor
{

    Logger log = Logger.getLogger(MostViewedMetricItemSite.class);

    DSpace dspace = new DSpace();

    @Override
    public void process(Context context, HttpServletRequest request,
            HttpServletResponse response)
                    throws PluginException, AuthorizeException
    {

        TopItemManager mviManager = dspace
                .getServiceManager().getServiceByName("viewedTopItemManager", TopItemManager.class);
        List<MostViewedItem> viewedList = new ArrayList<MostViewedItem>();

        MostViewedBean mvb = new MostViewedBean();
        try
        {
            viewedList = mviManager.getMostViewed(context);
            mvb.setItems(viewedList);
            mvb.setConfiguration(SearchUtils.getMostViewedConfiguration("site")
                    .getMetadataFields());
        }
        catch (SolrServerException e)
        {
            log.error(e.getMessage(), e);
        }
        catch (SQLException e)
        {
            log.error(e.getMessage(), e);
        }
        catch (SearchServiceException e)
        {
            log.error(e.getMessage(), e);
        }

        request.setAttribute("mostViewedItem", mvb);
    }

}
