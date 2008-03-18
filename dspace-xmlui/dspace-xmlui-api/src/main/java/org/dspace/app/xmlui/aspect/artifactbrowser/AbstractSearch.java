/*
 * AbstractSearch.java
 *
 * Version: $Revision: 1.18 $
 *
 * Date: $Date: 2006/07/27 18:24:34 $
 *
 * Copyright (c) 2002, Hewlett-Packard Company and Massachusetts
 * Institute of Technology.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * - Neither the name of the Hewlett-Packard Company nor the name of the
 * Massachusetts Institute of Technology nor the names of their
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
 */
package org.dspace.app.xmlui.aspect.artifactbrowser;

import java.io.IOException;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.util.HashUtil;
import org.apache.excalibur.source.SourceValidity;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.ContextUtil;
import org.dspace.app.xmlui.utils.DSpaceValidity;
import org.dspace.app.xmlui.utils.HandleUtil;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.ReferenceSet;
import org.dspace.app.xmlui.wing.element.Para;
import org.dspace.app.xmlui.wing.element.Select;
import org.dspace.app.xmlui.wing.element.Table;
import org.dspace.app.xmlui.wing.element.Row;
import org.dspace.app.xmlui.wing.element.Cell;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.handle.HandleManager;
import org.dspace.search.DSQuery;
import org.dspace.search.QueryArgs;
import org.dspace.search.QueryResults;
import org.dspace.sort.SortOption;
import org.dspace.sort.SortException;
import org.xml.sax.SAXException;

/**
 * This is an abstract search page. It is a collection of search methods that
 * are common between diffrent search implementation. An implementer must
 * implement at least three methods: addBody(), getQuery(), and generateURL().
 * 
 * See the two implementors: SimpleSearch and AdvancedSearch.
 * 
 * @author Scott Phillips
 */
public abstract class AbstractSearch extends AbstractDSpaceTransformer
{
	/** Language strings */
    private static final Message T_result_query = 
        message("xmlui.ArtifactBrowser.AbstractSearch.result_query");
    
    private static final Message T_head1_community =
        message("xmlui.ArtifactBrowser.AbstractSearch.head1_community");
    
    private static final Message T_head1_collection =  
        message("xmlui.ArtifactBrowser.AbstractSearch.head1_collection");
    
    private static final Message T_head1_none =  
        message("xmlui.ArtifactBrowser.AbstractSearch.head1_none");
    
    private static final Message T_head2 =
        message("xmlui.ArtifactBrowser.AbstractSearch.head2");
    
    private static final Message T_head3 =
        message("xmlui.ArtifactBrowser.AbstractSearch.head3");
    
    private static final Message T_no_results =
        message("xmlui.ArtifactBrowser.AbstractSearch.no_results");
    
    private static final Message T_all_of_dspace =
        message("xmlui.ArtifactBrowser.AbstractSearch.all_of_dspace");

    private static final Message T_sort_by_relevance =
        message("xmlui.ArtifactBrowser.AbstractSearch.sort_by.relevance");

    private final static Message T_sort_by = message("xmlui.ArtifactBrowser.AbstractSearch.sort_by");

    private final static Message T_order      = message("xmlui.ArtifactBrowser.AbstractSearch.order");
    private final static Message T_order_asc  = message("xmlui.ArtifactBrowser.AbstractSearch.order.asc");
    private final static Message T_order_desc = message("xmlui.ArtifactBrowser.AbstractSearch.order.desc");

    private final static Message T_rpp = message("xmlui.ArtifactBrowser.AbstractSearch.rpp");
    
    /** The options for results per page */
    private static final int[] RESULTS_PER_PAGE_PROGRESSION = {5,10,20,40,60,80,100};
    
    /** Cached query results */
    private QueryResults queryResults;
    
    /** Cached validity object */
    private SourceValidity validity;
    
    /**
     * Generate the unique caching key.
     * This key must be unique inside the space of this component.
     */
    public Serializable getKey()
    {
        try 
        {
            String key = "";
            
            // Page Parameter
            Request request = ObjectModelHelper.getRequest(objectModel);
            key += "-" + getParameterPage();
            key += "-" + getParameterRpp();
            key += "-" + getParameterSortBy();
            key += "-" + getParameterOrder();
            key += "-" + getParameterEtAl();

            // What scope the search is at
            DSpaceObject scope = getScope();
            if (scope != null)
                key += "-" + scope.getHandle();
            
            // The actualy search query.
            key += "-" + getQuery();

            return HashUtil.hash(key);
        }
        catch (Exception e)
        {
            // Ignore all errors and just don't cache.
            return "0";
        }
    }

    /**
     * Generate the cache validity object.
     * 
     * This validity object should never "over cache" because it will
     * perform the search, and serialize the results using the
     * DSpaceValidity object.
     */
    public SourceValidity getValidity()
    {
    	if (this.validity == null)
    	{
	        try
	        {
	            DSpaceValidity validity = new DSpaceValidity();
	            
	            DSpaceObject scope = getScope();
	            validity.add(scope);
	            
	            performSearch();
	            
	            @SuppressWarnings("unchecked") // This cast is correct
	            java.util.List<String> handles = queryResults.getHitHandles();
	            for (String handle : handles)
	            {
	                DSpaceObject resultDSO = HandleManager.resolveToObject(context, handle);
	                validity.add(resultDSO);
	            }
	            
	            this.validity = validity.complete();
	        }
	        catch (Exception e)
	        {
	            // Just ignore all errors and return an invalid cache.
	        }
    	}
    	return this.validity;
    }
    
    
    /**
     * Build the resulting search DRI document.
     */
    public abstract void addBody(Body body) throws SAXException, WingException,
            UIException, SQLException, IOException, AuthorizeException;

    /**
     * 
     * Attach a division to the given search division named "search-results"
     * which contains results for this search query.
     * 
     * @param search
     *            The search division to contain the search-results division.
     */
    protected void buildSearchResultsDivision(Division search)
            throws IOException, SQLException, WingException
    {
        if (getQuery().length() > 0)
        {

            // Preform the actual search
            performSearch();
            DSpaceObject searchScope = getScope();
            
            Para para = search.addPara("result-query","result-query");

            String query = getQuery();
            int hitCount = queryResults.getHitCount();
            para.addContent(T_result_query.parameterize(query,hitCount));
            
            Division results = search.addDivision("search-results","primary");
            
            if (searchScope instanceof Community)
            {
                Community community = (Community) searchScope;
                String communityName = community.getMetadata("name");
                results.setHead(T_head1_community.parameterize(communityName));
            }
            else if (searchScope instanceof Collection)
            {
                Collection collection = (Collection) searchScope;
                String collectionName = collection.getMetadata("name");
                results.setHead(T_head1_collection.parameterize(collectionName));
            }
            else
            {
                results.setHead(T_head1_none);
            }

            if (queryResults.getHitCount() > 0)
            {
                // Pagination variables.
                int itemsTotal = queryResults.getHitCount();
                int firstItemIndex = queryResults.getStart() + 1;
                int lastItemIndex = queryResults.getStart()
                        + queryResults.getPageSize();
                if (itemsTotal < lastItemIndex)
                    lastItemIndex = itemsTotal;
                int currentPage = (queryResults.getStart() / queryResults
                        .getPageSize()) + 1;
                int pagesTotal = ((queryResults.getHitCount() - 1) / queryResults
                        .getPageSize()) + 1;
                Map<String, String> parameters = new HashMap<String, String>();
                parameters.put("page", "{pageNum}");
                String pageURLMask = generateURL(parameters);

                results.setMaskedPagination(itemsTotal, firstItemIndex,
                        lastItemIndex, currentPage, pagesTotal, pageURLMask);

                // Look for any communities or collections in the mix
                ReferenceSet referenceSet = null;
                boolean resultsContainsBothContainersAndItems = false;
                
                @SuppressWarnings("unchecked") // This cast is correct
                java.util.List<String> containerHandles = queryResults.getHitHandles();
                for (String handle : containerHandles)
                {
                    DSpaceObject resultDSO = HandleManager.resolveToObject(
                            context, handle);

                    if (resultDSO instanceof Community
                            || resultDSO instanceof Collection)
                    {
                        if (referenceSet == null) {
                            referenceSet = results.addReferenceSet("search-results-repository",
                                    ReferenceSet.TYPE_SUMMARY_LIST,null,"repository-search-results");
                            // Set a heading showing that we will be listing containers that matched:
                            referenceSet.setHead(T_head2);
                            resultsContainsBothContainersAndItems = true;
                        }
                        referenceSet.addReference(resultDSO);
                    }
                }
                
                
                // Look for any items in the result set.
                referenceSet = null;
                
                @SuppressWarnings("unchecked") // This cast is correct
                java.util.List<String> itemHandles = queryResults.getHitHandles();
                for (String handle : itemHandles)
                {
                    DSpaceObject resultDSO = HandleManager.resolveToObject(
                            context, handle);

                    if (resultDSO instanceof Item)
                    {
                        if (referenceSet == null) {
                            referenceSet = results.addReferenceSet("search-results-repository",
                                    ReferenceSet.TYPE_SUMMARY_LIST,null,"repository-search-results");
                            // Only set a heading if there are both containers and items.
                            if (resultsContainsBothContainersAndItems)
                            	referenceSet.setHead(T_head3);  
                        }
                        referenceSet.addReference(resultDSO);
                    }
                }
                
            }
            else
            {
                results.addPara(T_no_results);
            }
        }// Empty query
    }
    
    /**
     * Add options to the search scope field. This field determines in what
     * communities or collections to search for the query.
     * 
     * The scope list will depend upon the current search scope. There are three
     * cases:
     * 
     * No current scope: All top level communities are listed.
     * 
     * The current scope is a community: All collections contained within the
     * community are listed.
     * 
     * The current scope is a collection: All parent communities are listed.
     * 
     * @param scope
     *            The current scope field.
     */
    protected void buildScopeList(Select scope) throws SQLException,
            WingException
    {

        DSpaceObject scopeDSO = getScope();
        if (scopeDSO == null)
        {
            // No scope, display all root level communities
            scope.addOption("/",T_all_of_dspace);
            scope.setOptionSelected("/");
            for (Community community : Community.findAll(context))
            {
                scope.addOption(community.getHandle(),community.getMetadata("name"));
            }
        }
        else if (scopeDSO instanceof Community)
        {
            // The scope is a community, display all collections contained
            // within
            Community community = (Community) scopeDSO;
            scope.addOption("/",T_all_of_dspace);
            scope.addOption(community.getHandle(),community.getMetadata("name"));
            scope.setOptionSelected(community.getHandle());

            for (Collection collection : community.getCollections())
            {
                scope.addOption(collection.getHandle(),collection.getMetadata("name"));
            }
        }
        else if (scopeDSO instanceof Collection)
        {
            // The scope is a collection, display all parent collections.
            Collection collection = (Collection) scopeDSO;
            scope.addOption("/",T_all_of_dspace);
            scope.addOption(collection.getHandle(),collection.getMetadata("name"));
            scope.setOptionSelected(collection.getHandle());
            
            Community[] communities = collection.getCommunities()[0]
                    .getAllParents();
            for (Community community : communities)
            {
                scope.addOption(community.getHandle(),community.getMetadata("name"));
            }
        }
    }

    /**
     * Query DSpace for a list of all items / collections / or communities that
     * match the given search query.
     * 
     * @return The associated query results.
     */
    protected void performSearch() throws SQLException, IOException, UIException
    {
        if (queryResults != null)
            return;
        
        Context context = ContextUtil.obtainContext(objectModel);
        String query = getQuery();
        DSpaceObject scope = getScope();
        int page = getParameterPage();

        QueryArgs qArgs = new QueryArgs();
        qArgs.setPageSize(getParameterRpp());
        try
        {
            qArgs.setSortOption(SortOption.getSortOption(getParameterSortBy()));
        }
        catch (SortException se)
        {
        }
        
        qArgs.setSortOrder(getParameterOrder());

        qArgs.setQuery(query);
        if (page > 1)
            qArgs.setStart((Integer.valueOf(page) - 1) * qArgs.getPageSize());
        else
            qArgs.setStart(0);

        QueryResults qResults = null;
        if (scope instanceof Community)
        {
            qResults = DSQuery.doQuery(context, qArgs, (Community) scope);
        }
        else if (scope instanceof Collection)
        {
            qResults = DSQuery.doQuery(context, qArgs, (Collection) scope);
        }
        else
        {
            qResults = DSQuery.doQuery(context, qArgs);
        }

        this.queryResults = qResults;
    }

    /**
     * Determine the current scope. This may be derived from the current url
     * handle if present or the scope parameter is given. If no scope is
     * specified then null is returned.
     * 
     * @return The current scope.
     */
    protected DSpaceObject getScope() throws SQLException
    {
        Request request = ObjectModelHelper.getRequest(objectModel);
        String scopeString = request.getParameter("scope");

        // Are we in a community or collection?
        DSpaceObject dso;
        if (scopeString == null || "".equals(scopeString))
            // get the search scope from the url handle
            dso = HandleUtil.obtainHandle(objectModel);
        else
            // Get the search scope from the location parameter
            dso = HandleManager.resolveToObject(context, scopeString);

        return dso;
    }

    protected int getParameterPage()
    {
        try
        {
            return Integer.parseInt(ObjectModelHelper.getRequest(objectModel).getParameter("page"));
        }
        catch (Exception e)
        {
            return 1;
        }
    }

    protected int getParameterRpp()
    {
        try
        {
            return Integer.parseInt(ObjectModelHelper.getRequest(objectModel).getParameter("rpp"));
        }
        catch (Exception e)
        {
            return 10;
        }
    }

    protected int getParameterSortBy()
    {
        try
        {
            return Integer.parseInt(ObjectModelHelper.getRequest(objectModel).getParameter("sort_by"));
        }
        catch (Exception e)
        {
            return 0;
        }
    }

    protected String getParameterOrder()
    {
        String s = ObjectModelHelper.getRequest(objectModel).getParameter("order");
        return s != null ? s : "DESC";
    }

    protected int getParameterEtAl()
    {
        try
        {
            return Integer.parseInt(ObjectModelHelper.getRequest(objectModel).getParameter("etal"));
        }
        catch (Exception e)
        {
            return 0;
        }
    }

    /**
     * Determine if the scope of the search should fixed or is changeable by the
     * user. 
     * 
     * The search scope when preformed by url, i.e. they are at the url handle/xxxx/xx/search 
     * then it is fixed. However at the global level the search is variable.
     * 
     * @return true if the scope is variable, false otherwise.
     */
    protected boolean variableScope() throws SQLException
    {
        if (HandleUtil.obtainHandle(objectModel) == null)
            return true;
        else 
            return false;
    }
    
    /**
     * Extract the query string. Under most implementations this will be derived
     * from the url parameters.
     * 
     * @return The query string.
     */
    abstract protected String getQuery() throws UIException;

    /**
     * Generate a url to the given search implementation with the associated
     * parameters included.
     * 
     * @param parameters
     * @return The post URL
     */
    abstract protected String generateURL(Map<String, String> parameters)
            throws UIException;

    
    /**
     * Recycle
     */
    public void recycle() 
    {
        this.queryResults = null;
        this.validity = null;
        super.recycle();
    }

    protected void buildSearchControls(Division div)
            throws WingException
    {
        Table controlsTable = div.addTable("search-controls", 1, 3);
        Row controlsRow = controlsTable.addRow(Row.ROLE_DATA);

        // Create a control for the number of records to display
        Cell rppCell = controlsRow.addCell();
        rppCell.addContent(T_rpp);
        Select rppSelect = rppCell.addSelect("rpp");
        for (int i : RESULTS_PER_PAGE_PROGRESSION)
        {
            rppSelect.addOption((i == getParameterRpp()), i, Integer.toString(i));
        }

        Cell sortCell = controlsRow.addCell();
        try
        {
            // Create a drop down of the different sort columns available
            sortCell.addContent(T_sort_by);
            Select sortSelect = sortCell.addSelect("sort_by");
            sortSelect.addOption(false, 0, T_sort_by_relevance);
            for (SortOption so : SortOption.getSortOptions())
            {
                if (so.isVisible())
                {
                    sortSelect.addOption((so.getNumber() == getParameterSortBy()), so.getNumber(),
                            message("xmlui.ArtifactBrowser.AbstractSearch.sort_by." + so.getName()));
                }
            }
        }
        catch (SortException se)
        {
            throw new WingException("Unable to get sort options", se);
        }

        // Create a control to changing ascending / descending order
        Cell orderCell = controlsRow.addCell();
        orderCell.addContent(T_order);
        Select orderSelect = orderCell.addSelect("order");
        orderSelect.addOption(SortOption.ASCENDING.equals(getParameterOrder()), SortOption.ASCENDING, T_order_asc);
        orderSelect.addOption(SortOption.DESCENDING.equals(getParameterOrder()), SortOption.DESCENDING, T_order_desc);

        // Create a control for the number of authors per item to display
        // FIXME This is currently disabled, as the supporting functionality
        // is not currently present in xmlui
        //if (isItemBrowse(info))
        //{
        //    controlsForm.addContent(T_etal);
        //    Select etalSelect = controlsForm.addSelect(BrowseParams.ETAL);
        //
        //    etalSelect.addOption((info.getEtAl() < 0), 0, T_etal_all);
        //    etalSelect.addOption(1 == info.getEtAl(), 1, Integer.toString(1));
        //
        //    for (int i = 5; i <= 50; i += 5)
        //    {
        //        etalSelect.addOption(i == info.getEtAl(), i, Integer.toString(i));
        //    }
        //}
    }
}
