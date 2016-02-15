/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
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
import org.dspace.discovery.SearchUtils;
import org.dspace.plugin.PluginException;
import org.dspace.plugin.SiteHomeProcessor;

public class MostViewedBitstreamSite implements SiteHomeProcessor {

	Logger log = Logger.getLogger(MostViewedBitstreamSite.class);
	@Override
	public void process(Context context, HttpServletRequest request,
			HttpServletResponse response) throws PluginException,
			AuthorizeException {
		
		MostViewedBitstreamManager mviManager = new MostViewedBitstreamManager();
		List<MostViewedItem> viewedList = new ArrayList<MostViewedItem>();
		
		MostViewedBean mvb = new MostViewedBean();		
		try {
			 viewedList = mviManager.getMostViewed(context);
			 
			 mvb.setItems(viewedList);
			 mvb.setConfiguration(SearchUtils.getMostViewedConfiguration("site").getMetadataFields());
		} catch (SolrServerException e) {
			log.error(e.getMessage(), e);
		} catch (SQLException e) {
			log.error(e.getMessage(), e);	
		}
		
		request.setAttribute("mostViewedBitstream", mvb);
	}

}
