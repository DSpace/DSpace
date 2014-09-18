package org.dspace.app.webui.discovery;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.dspace.app.webui.discovery.dto.CloudTagResult;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.discovery.DiscoverFacetField;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.DiscoverResult;
import org.dspace.discovery.DiscoverResult.FacetResult;
import org.dspace.discovery.SearchServiceException;
import org.dspace.discovery.SearchUtils;
import org.dspace.discovery.configuration.DiscoveryConfigurationParameters;
import org.dspace.plugin.PluginException;
import org.dspace.plugin.SiteHomeProcessor;

import edu.emory.mathcs.backport.java.util.Collections;


/**
 * Handles cloud tag construction
 * @author Márcio Ribeiro Gurgel do Amaral (marcio.rga@gmail.com)
 *
 */
public class CloudTagProcessor implements SiteHomeProcessor 
{
	
	private static final int MAX_TAGS = 30;
	private static Logger logger = Logger.getLogger(CloudTagProcessor.class);
	
	@Override
	public void process(Context context, HttpServletRequest request, HttpServletResponse response) 
			throws PluginException, AuthorizeException 
	{
		
		try 
		{
			DiscoverQuery query = new DiscoverQuery();
			query.addFacetField(new DiscoverFacetField("subject", "text", ConfigurationManager.getIntProperty("dspace.home.cloudtag.amount", MAX_TAGS), DiscoveryConfigurationParameters.SORT.COUNT));
			
			DiscoverResult result = SearchUtils.getSearchService().search(context, query);
			List<CloudTagResult> cloudResult = new ArrayList<CloudTagResult>();
			
			for(Map.Entry<String, List<FacetResult>> foundResults : result.getFacetResults().entrySet())
			{
				for(FacetResult facetResult : foundResults.getValue())
				{
					cloudResult.add(new CloudTagResult(facetResult.getDisplayedValue().toLowerCase().replaceAll("\n", ""), foundResults.getKey(), facetResult.getCount()));
				}
			}
			
			Collections.shuffle(cloudResult);
			
			request.setAttribute("cloudResult", cloudResult);
			
		} 
		catch (SearchServiceException e) 
		{
			logger.error(e.getMessage(), e);
		}
		
	}
	
}
