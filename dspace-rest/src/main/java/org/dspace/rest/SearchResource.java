
package org.dspace.rest;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.rest.common.Collection;
import org.dspace.rest.common.Community;
import org.dspace.rest.common.Item;
import org.dspace.rest.common.search.ResultSet;
import org.dspace.rest.exceptions.ContextException;
import org.dspace.rest.exceptions.SearchProcessorException;
import org.dspace.rest.discovery.DiscoverUtility;
import org.dspace.rest.utils.UIUtil;
import org.dspace.discovery.configuration.DiscoveryConfiguration;
import org.dspace.discovery.DiscoverResult;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.SearchUtils;
import org.dspace.discovery.SearchServiceException;
import org.dspace.discovery.configuration.DiscoverySortFieldConfiguration;
import org.dspace.discovery.configuration.DiscoverySearchFilterFacet;
import org.dspace.discovery.configuration.DiscoverySearchFilter;
/**
 * This class perform search.
 * 
 * @author Hamed Yousefi Nasab
 */
 
 
 @Path("/discover")
 public class SearchResource extends Resource
 {
	 private static Logger log = Logger.getLogger(SearchResource.class);
	 private org.dspace.content.DSpaceObject scope;

	 @GET
	 @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
	 public ResultSet[] search(@QueryParam("scope") String scope_id, @QueryParam("query") String queryStr,
			@QueryParam("page") int page, @QueryParam("rpp") int rpp , @QueryParam("expand") String expand, 
			@QueryParam("order") String Order, @QueryParam("sort_by") String sort,@QueryParam("start") String start,
			@QueryParam("userIP") String user_ip, @QueryParam("userAgent") String user_agent,
            @QueryParam("xforwardedfor") String xforwardedfor, @Context HttpHeaders headers, @Context HttpServletRequest request)
			throws SearchProcessorException,
            IOException
	{
				
		log.info("Start shearching....");
		long totalResults = 0;
		org.dspace.core.Context context = null;
		ArrayList<Community> communities = null;
		ArrayList<Collection> collections = null;
		ArrayList<Item> items = null;
		DiscoverResult qResults = null;
		try
		{			
			context = createContext(getUser(headers));
			DiscoverQuery queryArgs = preparDiscoverConfig(context, request);
			
			int p = UIUtil.getIntParameter(request,"page");
			queryArgs.setStart(queryArgs.getMaxResults()*((p<1 ? 1 : p)-1));
			try
			{
				qResults = SearchUtils.getSearchService().search(context, scope,
						queryArgs);
				
				totalResults = qResults.getTotalSearchResults();
				communities  = new ArrayList<Community>();
				collections  = new ArrayList<Collection>();
				items  = new ArrayList<Item>();
				for (org.dspace.content.DSpaceObject dso : qResults.getDspaceObjects())
				{
					if (dso instanceof org.dspace.content.Item)
					{
						if (AuthorizeManager.authorizeActionBoolean(context, (org.dspace.content.Item) dso, org.dspace.core.Constants.READ))
						{
							Item item = new Item((org.dspace.content.Item) dso, "metadata", context);
							items.add(item);
						}
					}
					else if (dso instanceof org.dspace.content.Collection)
					{
						if (AuthorizeManager.authorizeActionBoolean(context, (org.dspace.content.Collection) dso, org.dspace.core.Constants.READ))
						{
							Collection collection = new Collection((org.dspace.content.Collection) dso, expand, null, 100, 0);
							collections.add(collection);
						}
					}
					else if (dso instanceof org.dspace.content.Community)
					{
						if (AuthorizeManager.authorizeActionBoolean(context, (org.dspace.content.Community) dso, org.dspace.core.Constants.READ))
						{
							Community community = new Community((org.dspace.content.Community) dso, expand, context);
							communities.add(community);
						}
					}
				}
			}
			catch (SearchServiceException e)
			{
				
			}

			context.complete();
		}
		catch (SQLException e)
        {
            processException("Could not read communities, SQLException. Message:" + e, context);
        }
        catch (ContextException e)
        {
            processException("Could not read communities, ContextException. Message:" + e.getMessage(), context);
        }
        finally
        {
            processFinally(context);
        }
		
		ArrayList<ResultSet> result = new ArrayList<ResultSet>();
		ResultSet rs = new ResultSet();
		rs.setTotalResults(totalResults);
		rs.setCommunities(communities.toArray(new Community[0]));
		rs.setCollections(collections.toArray(new Collection[0]));
		rs.setItems(items.toArray(new Item[0]));
		rs.setSearchTime(qResults.getSearchTime());
		result.add(rs);
		
		return result.toArray(new ResultSet[0]);
	 }

	 private DiscoverQuery preparDiscoverConfig(org.dspace.core.Context context, 
											@Context HttpServletRequest request)
				throws SearchProcessorException,
				IOException
			{
		 
			try
			{
				scope = DiscoverUtility.getSearchScope(context, request);
			}
			catch (IllegalStateException e)
			{
				throw new SearchProcessorException(e.getMessage(), e);
			}
			catch (SQLException e)
			{
				throw new SearchProcessorException(e.getMessage(), e);
			}
			
			DiscoveryConfiguration discoveryConfiguration = SearchUtils
                .getDiscoveryConfiguration(scope);
			List<DiscoverySortFieldConfiguration> sortFields = discoveryConfiguration
					.getSearchSortConfiguration().getSortFields();
			List<String> sortOptions = new ArrayList<String>();
			for (DiscoverySortFieldConfiguration sortFieldConfiguration : sortFields)
			{
				String sortField = SearchUtils.getSearchService().toSortFieldIndex(
						sortFieldConfiguration.getMetadataField(),
						sortFieldConfiguration.getType());
				sortOptions.add(sortField);
			}
			request.setAttribute("sortOptions", sortOptions);
			
			DiscoverQuery queryArgs = DiscoverUtility.getDiscoverQuery(context,
                request, scope, true);
			
			queryArgs.setSpellCheck(discoveryConfiguration.isSpellCheckEnabled()); 
			
			List<DiscoverySearchFilterFacet> availableFacet = discoveryConfiguration
                .getSidebarFacets();
			
			request.setAttribute("facetsConfig",
                availableFacet != null ? availableFacet
                        : new ArrayList<DiscoverySearchFilterFacet>());
			int etal = UIUtil.getIntParameter(request, "etal");
			if (etal == -1)
			{
				etal = ConfigurationManager
						.getIntProperty("webui.itemlist.author-limit");
			}
			
			request.setAttribute("etal", etal);

			String query = queryArgs.getQuery();
			request.setAttribute("query", query);
			request.setAttribute("queryArgs", queryArgs);
			List<DiscoverySearchFilter> availableFilters = discoveryConfiguration
					.getSearchFilters();
			request.setAttribute("availableFilters", availableFilters);

			List<String[]> appliedFilters = DiscoverUtility.getFilters(request);
			request.setAttribute("appliedFilters", appliedFilters);
			List<String> appliedFilterQueries = new ArrayList<String>();
			for (String[] filter : appliedFilters)
			{
				appliedFilterQueries.add(filter[0] + "::" + filter[1] + "::"
						+ filter[2]);
			}
			request.setAttribute("appliedFilterQueries", appliedFilterQueries);
			List<org.dspace.content.DSpaceObject> scopes = new ArrayList<org.dspace.content.DSpaceObject>();
			if (scope == null)
			{
				org.dspace.content.Community[] topCommunities;
				try
				{
					topCommunities = org.dspace.content.Community.findAllTop(context);
				}
				catch (SQLException e)
				{
					throw new SearchProcessorException(e.getMessage(), e);
				}
				for (org.dspace.content.Community com : topCommunities)
				{
					scopes.add(com);
				}
			}
			else
			{
				try
				{
					org.dspace.content.DSpaceObject pDso = scope.getParentObject();
					while (pDso != null)
					{
						scopes.add(0, pDso);
						pDso = pDso.getParentObject();
					}
					scopes.add(scope);
					if (scope instanceof org.dspace.content.Community)
					{
						org.dspace.content.Community[] comms = ((org.dspace.content.Community) scope).getSubcommunities();
						for (org.dspace.content.Community com : comms)
						{
							scopes.add(com);
						}
						org.dspace.content.Collection[] colls = ((org.dspace.content.Community) scope).getCollections();
						for (org.dspace.content.Collection col : colls)
						{
							scopes.add(col);
						}
					}
				}
				catch (SQLException e)
				{
					throw new SearchProcessorException(e.getMessage(), e);
				}
			}
			request.setAttribute("scope", scope);
			request.setAttribute("scopes", scopes);
			
			return queryArgs;
	 }
 }