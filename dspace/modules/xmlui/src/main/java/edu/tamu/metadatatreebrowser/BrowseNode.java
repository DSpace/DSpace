/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package edu.tamu.metadatatreebrowser;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.cocoon.caching.CacheableProcessingComponent;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.commons.collections.CollectionUtils;
import org.apache.log4j.Logger;
import org.dspace.app.xmlui.aspect.discovery.AbstractSearch;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.HandleUtil;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.BitstreamService;
import org.dspace.core.Constants;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.DiscoverResult;
import org.dspace.discovery.SearchServiceException;
import org.dspace.discovery.SearchUtils;
import org.dspace.discovery.configuration.DiscoveryConfiguration;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.xml.sax.SAXException;

/**
 *  Custom search for the Metadata Tree Browser (Adapted from org.dspace.app.xmlui.aspect.discovery.SimpleSearch) 
 *
 * @author Scott Phillips, http://www.scottphillips.com/
 * @author Alexey Maslov
 * @author Jason Savell <jsavell@library.tamu.edu>
 */
public class BrowseNode extends AbstractSearch implements CacheableProcessingComponent {
	private static final Logger log = Logger.getLogger(BrowseNode.class);

	private MetadataTreeNode node;
	
    private static final Message T_head1_none =
            message("xmlui.Discovery.AbstractSearch.head1_none");

    private static final Message T_dspace_home =
            message("xmlui.general.dspace_home");

    private static final Message T_no_results =
            message("xmlui.ArtifactBrowser.AbstractSearch.no_results");
    
    private static final Message T_result_head_3 = message("xmlui.Discovery.AbstractSearch.head3");
    private static final Message T_result_head_2 = message("xmlui.Discovery.AbstractSearch.head2");

    
    protected BitstreamService bitstreamService = ContentServiceFactory.getInstance().getBitstreamService();
    
    /**
     * Add Page metadata.
     */
    public void addPageMeta(PageMeta pageMeta) throws WingException, SQLException {
       	pageMeta.addMetadata("title").addContent(node.getName());
       	
        // Add the trail back to the repository root.
        pageMeta.addTrailLink(contextPath + "/",T_dspace_home);

        DSpaceObject dso = HandleUtil.obtainHandle(objectModel);
        if ((dso instanceof org.dspace.content.Collection) || (dso instanceof Community)) {
            HandleUtil.buildHandleTrail(context, dso, pageMeta, contextPath, true);
        }
    }

    /**
     * build the DRI page representing the body of the search query. This
     * provides a widget to generate a new query and list of search results if
     * present.
     */
    public void addBody(Body body) throws SAXException, WingException,
            SQLException, IOException, AuthorizeException {
        DSpaceObject currentScope = getScope();

    	if (node == null) {
			return;
    	}
    	
		String baseURL = contextPath + "/handle/" + currentScope.getHandle()+ "/mdbrowse";
		
		// Display the Parent bread crumb
		Division div = body.addDivision("metadata-tree-browser-node","primary");
		
		// Nested parent list
		Division parentDiv = div.addDivision("parent-div");
		org.dspace.app.xmlui.wing.element.List parentList = parentDiv.addList("parent-list");
		parentList.addItemXref(contextPath + "/handle/" + currentScope.getHandle(), currentScope instanceof org.dspace.content.Collection ? "Collection Home" : "Community Home");

		if (!(node.getParent() == null || node.getParent().isRoot())) {
			parentList = parentList.addList("parent-sub-list");
			for(MetadataTreeNode parent : node.getParents()) {
				if (!parent.isRoot()) {
					String nodeURL = baseURL + "?node=" + parent.getId();
					parentList.addItemXref(nodeURL, parent.getName());
					parentList = parentList.addList("parent-sub-list");
				}
			}
		}
		
		Division contentDiv = div.addDivision("node-content-div","primary");
		contentDiv.setHead(node.getName());
		
		// Display any children
		if (node.hasChildren()) {
			Division childDiv = contentDiv.addDivision("child-div");
			org.dspace.app.xmlui.wing.element.List childList = childDiv.addList("child-list");
			for(MetadataTreeNode child : node.getChildren()) {
				Bitstream thumbnail = bitstreamService.find(context, child.getThumbnailId());
				String thumbnailURL = contextPath + "/bitstream/id/"+thumbnail.getID()+"/?sequence="+thumbnail.getSequenceID();
				String nodeURL = baseURL + "?node=" + child.getId();

				org.dspace.app.xmlui.wing.element.Item item = childList.addItem();
				item.addFigure(thumbnailURL, nodeURL, "node-thumbnail");
				item.addXref(nodeURL, child.getName(),"node-label");
			}
		}
		
        // Add the result division
		// Display any items
		if (node.hasContent()) {
			try {
	            buildSearchResultsDivision(contentDiv);
	        } catch (SearchServiceException e) {
	            throw new UIException(e.getMessage(), e);
	        }
		}
    }

    /**
    * 
    * Attach a division to the given search division named "search-results"
    * which contains results for this search query.
    * 
    * @param search
    *            The search division to contain the search-results division.
    */
   @Override 
   protected void buildSearchResultsDivision(Division search)
		   throws IOException, SQLException, WingException, SearchServiceException
   {
       try {
           if (queryResults == null) {
               DSpaceObject scope = getScope();
               this.performSearch(scope);
           }
       } catch (RuntimeException e) {
           log.error(e.getMessage(), e);
           queryResults = null;
       } catch (Exception e) {
           log.error(e.getMessage(), e);
           queryResults = null;
       }	   
	   
       Division results = search.addDivision("search-results","primary");
       
       DSpaceObject searchScope = getScope();

       int displayedResults;
       long totalResults;
       float searchTime;
       
       if(queryResults != null && 0 < queryResults.getTotalSearchResults()) {
           displayedResults = queryResults.getDspaceObjects().size();
           totalResults = queryResults.getTotalSearchResults();
           searchTime = ((float) queryResults.getSearchTime() / 1000) % 60;

           if (!(searchScope instanceof org.dspace.content.Community) && !(searchScope instanceof org.dspace.content.Collection)) {
               results.setHead(T_head1_none.parameterize(displayedResults, totalResults, searchTime));
           }
       }

       if (queryResults != null && 0 < queryResults.getDspaceObjects().size()) {
           // Pagination variables.
           int itemsTotal = (int) queryResults.getTotalSearchResults();
           int firstItemIndex = (int) this.queryResults.getStart() + 1;
           int lastItemIndex = (int) this.queryResults.getStart() + queryResults.getDspaceObjects().size();

           int currentPage = this.queryResults.getStart() / this.queryResults.getMaxResults() + 1;
           int pagesTotal = (int) ((this.queryResults.getTotalSearchResults() - 1) / this.queryResults.getMaxResults()) + 1;
           Map<String, String> parameters = new HashMap<String, String>();
           parameters.put("page", "{pageNum}");
           String pageURLMask = generateURL(parameters);
           pageURLMask = addFilterQueriesToUrl(pageURLMask);

           results.setMaskedPagination(itemsTotal, firstItemIndex,
                   lastItemIndex, currentPage, pagesTotal, pageURLMask);

           // Look for any communities or collections in the mix
           org.dspace.app.xmlui.wing.element.List dspaceObjectsList = null;

           // Put it on the top of level search result list
           dspaceObjectsList = results.addList("search-results-repository",
                   org.dspace.app.xmlui.wing.element.List.TYPE_DSO_LIST, "repository-search-results");

           java.util.List<DSpaceObject> commCollList = new ArrayList<DSpaceObject>();
           java.util.List<org.dspace.content.Item> itemList = new ArrayList<org.dspace.content.Item>();
           for (DSpaceObject resultDso : queryResults.getDspaceObjects()) {
               if(resultDso.getType() == Constants.COMMUNITY || resultDso.getType() == Constants.COLLECTION) {
                   commCollList.add(resultDso);
               } else if (resultDso.getType() == Constants.ITEM) {
                   itemList.add((org.dspace.content.Item) resultDso);
               }
           }

           if(CollectionUtils.isNotEmpty(commCollList)) {
               org.dspace.app.xmlui.wing.element.List commCollWingList = dspaceObjectsList.addList("comm-coll-result-list");
               commCollWingList.setHead(T_result_head_2);
               for (DSpaceObject dso : commCollList) {
                   DiscoverResult.DSpaceObjectHighlightResult highlightedResults = queryResults.getHighlightedResults(dso);
                   if(dso.getType() == Constants.COMMUNITY) {
                       //Render our community !
                       org.dspace.app.xmlui.wing.element.List communityMetadata = commCollWingList.addList(dso.getHandle() + ":community");
                       renderCommunity((Community) dso, highlightedResults, communityMetadata);
                   } else if (dso.getType() == Constants.COLLECTION) {
                       //Render our collection !
                       org.dspace.app.xmlui.wing.element.List collectionMetadata = commCollWingList.addList(dso.getHandle() + ":collection");
                       renderCollection((org.dspace.content.Collection) dso, highlightedResults, collectionMetadata);
                   }
               }
           }

           if(CollectionUtils.isNotEmpty(itemList)) {
               org.dspace.app.xmlui.wing.element.List itemWingList = dspaceObjectsList.addList("item-result-list");
               if(CollectionUtils.isNotEmpty(commCollList)) {
                   itemWingList.setHead(T_result_head_3);
               }

               for (org.dspace.content.Item resultDso : itemList) {
                   DiscoverResult.DSpaceObjectHighlightResult highlightedResults = queryResults.getHighlightedResults(resultDso);
                   renderItem(itemWingList, resultDso, highlightedResults);
               }
           }
       } else {
           results.addPara(T_no_results);
       }
   }

   @Override
   protected String getBasicUrl() throws SQLException {
       Request request = ObjectModelHelper.getRequest(objectModel);
       DSpaceObject dso = HandleUtil.obtainHandle(objectModel);
       return request.getContextPath() + (dso == null ? "" : "/handle/" + dso.getHandle()) + "/discover";
   }

   /**
    * Get the browse node from the URL parameter, if none is found the empty
    * string is returned.
    */
   protected String getQuery() throws UIException {
        Request request = ObjectModelHelper.getRequest(objectModel);
        String node = decodeFromURL(request.getParameter("node"));
        if (node == null) {
            return "";
        }
        return node.trim();
    }
    
   /**
    * Generate a url to the simple search url.
    */
   protected String generateURL(Map<String, String> parameters) throws UIException {
	   Request request = ObjectModelHelper.getRequest(objectModel);
	   parameters.put("node",request.getParameter("node"));

	   if (parameters.get("page") == null) {
		   parameters.put("page", request.getParameter("page"));
	   }
	   return AbstractDSpaceTransformer.generateURL("mdbrowse", parameters);
   }
   
   /**
    * Query DSpace for a list of all items / collections / or communities that
    * match the given search query.
    *
    *
    * @param scope the dspace object parent
    */
   public void performSearch(DSpaceObject scope) throws UIException, SearchServiceException {
       if (queryResults != null) {
           return;
       }
       
       String handle = null;
       try {
		    String nodeString = getQuery();
		    
			DSpaceObject dso = HandleUtil.obtainHandle(objectModel);
			handle = dso.getHandle();
			
		    MetadataTreeNode root = MetadataTreeService.getInstance().getFullTree(context, dso); 
			
			node = root.findById(Integer.valueOf(nodeString));

       } catch (UIException e) {
           log.error(e.getMessage(), e);
       } catch (SQLException e) {
           log.error(e.getMessage(), e);
       }

       int page = getParameterPage();

       java.util.List<String> filterQueries = new ArrayList<String>();
       String[] fqs = getFilterQueries();

       if (fqs != null) {
           filterQueries.addAll(Arrays.asList(fqs));
       }

       this.queryArgs = new DiscoverQuery();

       //Add the configured default filter queries
       DiscoveryConfiguration discoveryConfiguration = SearchUtils.getDiscoveryConfiguration(scope);
       java.util.List<String> defaultFilterQueries = discoveryConfiguration.getDefaultFilterQueries();
       queryArgs.addFilterQueries(defaultFilterQueries.toArray(new String[defaultFilterQueries.size()]));

       if (filterQueries.size() > 0) {
           queryArgs.addFilterQueries(filterQueries.toArray(new String[filterQueries.size()]));
       }

       queryArgs.setMaxResults(getParameterRpp());

       String sortBy = ObjectModelHelper.getRequest(objectModel).getParameter("sort_by");
       sortBy = "dc.title_sort";

       queryArgs.setSortField(sortBy, DiscoverQuery.SORT_ORDER.asc);

       String fieldLabel = DSpaceServicesFactory.getInstance().getConfigurationService().getProperty("xmlui.mdbrowser."+handle+".field");

       if (fieldLabel == null || fieldLabel.length() == 0) {
			fieldLabel = "dc.relation.ispartof";
       }
       queryArgs.setQuery(fieldLabel+": \""+node.getFieldValue()+"\"");

       if (page > 1) {
           queryArgs.setStart((page - 1) * queryArgs.getMaxResults());
       } else {
           queryArgs.setStart(0);
       }
       this.queryResults = SearchUtils.getSearchService().search(context, scope, queryArgs);
   }
}
