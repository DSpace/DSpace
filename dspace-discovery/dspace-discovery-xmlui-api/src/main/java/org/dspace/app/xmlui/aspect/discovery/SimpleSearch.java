/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.discovery;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.cocoon.caching.CacheableProcessingComponent;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.HandleUtil;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.*;
import org.dspace.app.xmlui.wing.element.Item;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.*;
import org.dspace.core.ConfigurationManager;
import org.dspace.discovery.*;
import org.dspace.discovery.configuration.DiscoveryConfiguration;
import org.dspace.discovery.configuration.DiscoverySearchFilter;
import org.dspace.utils.DSpace;
import org.xml.sax.SAXException;

/**
 * Preform a simple search of the repository. The user provides a simple one
 * field query (the url parameter is named query) and the results are processed.
 *
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 */
public class SimpleSearch extends AbstractSearch implements CacheableProcessingComponent {
    /**
     * Language Strings
     */
    private static final Message T_title =
            message("xmlui.ArtifactBrowser.SimpleSearch.title");

    private static final Message T_dspace_home =
            message("xmlui.general.dspace_home");

    private static final Message T_trail =
            message("xmlui.ArtifactBrowser.SimpleSearch.trail");

    private static final Message T_search_scope =
        message("xmlui.ArtifactBrowser.SimpleSearch.search_scope");

    private static final Message T_head =
            message("xmlui.ArtifactBrowser.SimpleSearch.head");

    private static final Message T_search_label =
            message("xmlui.discovery.SimpleSearch.search_label");

    private static final Message T_go =
            message("xmlui.general.go");
    private static final Message T_filter_label = message("xmlui.Discovery.SimpleSearch.filter_head");
    private static final Message T_filter_help = message("xmlui.Discovery.SimpleSearch.filter_help");
    private static final Message T_add_filter = message("xmlui.Discovery.SimpleSearch.filter_add");
    private static final Message T_filter_apply = message("xmlui.Discovery.SimpleSearch.filter_apply");
    private static final Message T_FILTERS_SELECTED = message("xmlui.ArtifactBrowser.SimpleSearch.filter.selected");

    private SearchService searchService = null;

    public SimpleSearch() {
        DSpace dspace = new DSpace();
        searchService = dspace.getServiceManager().getServiceByName(SearchService.class.getName(),SearchService.class);
    }


    /**
     * Add Page metadata.
     */
    public void addPageMeta(PageMeta pageMeta) throws WingException, SQLException {
        pageMeta.addMetadata("title").addContent(T_title);
        pageMeta.addTrailLink(contextPath + "/", T_dspace_home);

        DSpaceObject dso = HandleUtil.obtainHandle(objectModel);
        if ((dso instanceof org.dspace.content.Collection) || (dso instanceof Community)) {
            HandleUtil.buildHandleTrail(dso, pageMeta, contextPath);
        }

        pageMeta.addTrail().addContent(T_trail);
    }

    /**
     * build the DRI page representing the body of the search query. This
     * provides a widget to generate a new query and list of search results if
     * present.
     */
    public void addBody(Body body) throws SAXException, WingException,
            SQLException, IOException, AuthorizeException {

        Request request = ObjectModelHelper.getRequest(objectModel);
        String queryString = getQuery();

        // Build the DRI Body
        Division search = body.addDivision("search", "primary");
        search.setHead(T_head);
        String searchUrl = ConfigurationManager.getProperty("dspace.url") + "/JSON/discovery/search";

        search.addHidden("discovery-json-search-url").setValue(searchUrl);
        DSpaceObject currentScope = getScope();
        if(currentScope != null){
            search.addHidden("discovery-json-scope").setValue(currentScope.getHandle());
        }
        search.addHidden("contextpath").setValue(contextPath);

        String[] fqs = getFilterQueries();

        Division mainSearchDiv = search.addInteractiveDivision("general-query",
                "discover", Division.METHOD_GET, "discover-search-box search");

        List searchList = mainSearchDiv.addList("primary-search", List.TYPE_FORM);

        searchList.setHead(T_search_label);
        if (variableScope()) {
            Select scope = searchList.addItem().addSelect("scope");
            scope.setLabel(T_search_scope);
            buildScopeList(scope);
        }

        Item searchBoxItem = searchList.addItem();
        Text text = searchBoxItem.addText("query");
        text.setValue(queryString);
        text.setSize(75);
        searchBoxItem.addButton("submit").setValue(T_go);
        addHiddenFormFields("search", request, fqs, mainSearchDiv);


        DSpaceObject dso = HandleUtil.obtainHandle(objectModel);
        DiscoveryConfiguration discoveryConfiguration = SearchUtils.getDiscoveryConfiguration(dso);
        java.util.List<DiscoverySearchFilter> filterFields = discoveryConfiguration.getSearchFilters();
        if(0 < fqs.length || 0 < filterFields.size()){
            Division searchFiltersDiv = search.addInteractiveDivision("search-filters",
                    "discover", Division.METHOD_GET, "discover-search-box search");

            List secondarySearchList = searchFiltersDiv.addList("secondary-search", List.TYPE_FORM);
            secondarySearchList.setHead(T_filter_label);


    //        queryList.addItem().addContent("Filters");
            //If we have any filters, show them
            if(fqs.length > 0){
                //if(filters != null && filters.size() > 0){
                Item item = secondarySearchList.addItem("used-filters", "used-filters-list");


//                Composite composite = item.addComposite("facet-controls");

//                composite.setLabel(T_FILTERS_SELECTED);


                for (int i = 0; i <  fqs.length; i++) {
                    String filterQuery = fqs[i];
                    DiscoverFilterQuery fq = searchService.toFilterQuery(context, filterQuery);

//                    CheckBox box = item.addCheckBox("fq");
                    CheckBox box = item.addCheckBox("fq");
                    if(i == 0){
                        box.setLabel(T_FILTERS_SELECTED);
                    }
                    Option option = box.addOption(true, fq.getFilterQuery());
                    String field = fq.getField();
                    option.addContent(message("xmlui.ArtifactBrowser.SimpleSearch.filter." + field));

                    //We have a filter query get the display value
                    //Check for a range query
                    Pattern pattern = Pattern.compile("\\[(.*? TO .*?)\\]");
                    Matcher matcher = pattern.matcher(fq.getDisplayedValue());
                    boolean hasPattern = matcher.find();
                    if (hasPattern) {
                        String[] years = matcher.group(0).replace("[", "").replace("]", "").split(" TO ");
                        option.addContent(": " + years[0] + " - " + years[1]);
                        continue;
                    }

                    option.addContent(": " + fq.getDisplayedValue());
                }
                secondarySearchList.addItem().addButton("submit_update_filters", "update-filters").setValue(T_filter_apply);
            }



            if(0 < filterFields.size()){
                //We have at least one filter so add our filter box
                Item item = secondarySearchList.addItem("search-filter-list", "search-filter-list");
                Composite filterComp = item.addComposite("search-filter-controls");
                filterComp.setLabel(T_add_filter);
                filterComp.setHelp(T_filter_help);

    //            filterComp.setLabel("");

                Select select = filterComp.addSelect("filtertype");

                //For each field found (at least one) add options
                for (DiscoverySearchFilter searchFilter : filterFields) {
                    select.addOption(searchFilter.getIndexFieldName(), message("xmlui.ArtifactBrowser.SimpleSearch.filter." + searchFilter.getIndexFieldName()));
                }

                //Add a box so we can search for our value
                filterComp.addText("filter").setSize(30);

                //And last add an add button
                filterComp.enableAddOperation();
            }

            addHiddenFormFields("filter", request, fqs, searchFiltersDiv);

        }



        Division searchControlsDiv = search.addInteractiveDivision("search-controls",
                "discover", Division.METHOD_GET, "discover-sort-box search");

        buildSearchControls(searchControlsDiv);
        addHiddenFormFields("sort", request, fqs, searchControlsDiv);


//        query.addPara(null, "button-list").addButton("submit").setValue(T_go);

        // Build the DRI Body
        //Division results = body.addDivision("results", "primary");
        //results.setHead(T_head);

        // Add the result division
        try {
            buildSearchResultsDivision(search);
        } catch (SearchServiceException e) {
            throw new UIException(e.getMessage(), e);
        }

    }

    /**
     * Returns a list of the filter queries for use in rendering pages, creating page more urls, ....
     * @return an array containing the filter queries
     */
    protected String[] getParameterFilterQueries() {
        Request request = ObjectModelHelper.getRequest(objectModel);
        java.util.List<String> fqs = new ArrayList<String>();
        if(request.getParameterValues("fq") != null)
        {
            fqs.addAll(Arrays.asList(request.getParameterValues("fq")));
        }

        //Have we added a filter using the UI
        if(request.getParameter("filter") != null && !"".equals(request.getParameter("filter")))
        {
            fqs.add((request.getParameter("filtertype")) + ":" + request.getParameter("filter"));
        }
        return fqs.toArray(new String[fqs.size()]);
    }

    /**
     * Returns all the filter queries for use by discovery
     *  This method returns more expanded filter queries then the getParameterFilterQueries
     * @return an array containing the filter queries
     */
    protected String[] getFilterQueries() {
        try {
            java.util.List<String> allFilterQueries = new ArrayList<String>();
            Request request = ObjectModelHelper.getRequest(objectModel);
            java.util.List<String> fqs = new ArrayList<String>();

            if(request.getParameterValues("fq") != null)
            {
                fqs.addAll(Arrays.asList(request.getParameterValues("fq")));
            }

            String type = request.getParameter("filtertype");
            String value = request.getParameter("filter");

            if(value != null && !value.equals("")){
                allFilterQueries.add(searchService.toFilterQuery(context, (type.equals("*") ? "" : type), value).getFilterQuery());
            }

            //Add all the previous filters also
            for (String fq : fqs) {
                allFilterQueries.add(searchService.toFilterQuery(context, fq).getFilterQuery());
            }

            return allFilterQueries.toArray(new String[allFilterQueries.size()]);
        }
        catch (RuntimeException re) {
            throw re;
        } catch (Exception e) {
            return null;
        }
    }


    /**
     * Get the search query from the URL parameter, if none is found the empty
     * string is returned.
     */
    protected String getQuery() throws UIException {
        Request request = ObjectModelHelper.getRequest(objectModel);
        String query = decodeFromURL(request.getParameter("query"));
        if (query == null)
        {
            return "";
        }
        return query.trim();
    }

    /**
     * Generate a url to the simple search url.
     */
    protected String generateURL(Map<String, String> parameters)
            throws UIException {
        String query = getQuery();
        if (!"".equals(query))
        {
            parameters.put("query", encodeForURL(query));
        }

        if (parameters.get("page") == null)
        {
            parameters.put("page", String.valueOf(getParameterPage()));
        }

        if (parameters.get("rpp") == null)
        {
            parameters.put("rpp", String.valueOf(getParameterRpp()));
        }


        if (parameters.get("group_by") == null)
        {
            parameters.put("group_by", String.valueOf(this.getParameterGroup()));
        }

        if (parameters.get("sort_by") == null && getParameterSortBy() != null)
        {
            parameters.put("sort_by", String.valueOf(getParameterSortBy()));
        }

        if (parameters.get("order") == null && getParameterOrder() != null)
        {
            parameters.put("order", getParameterOrder());
        }

        if (parameters.get("etal") == null)
        {
            parameters.put("etal", String.valueOf(getParameterEtAl()));
        }
        if(parameters.get("scope") == null && getParameterScope() != null)
        {
            parameters.put("scope", getParameterScope());
        }

        return AbstractDSpaceTransformer.generateURL("discover", parameters);
    }

    /**
     * Since the layout is creating separate forms for each search part
     * this method will add hidden fields containing the values from other form parts
     *
     * @param type the type of our form
     * @param request the request
     * @param fqs the filter queries
     * @param division the division that requires the hidden fields
     * @throws WingException will never occur
     */
    private void addHiddenFormFields(String type, Request request, String[] fqs, Division division) throws WingException {
        if(type.equals("filter") || type.equals("sort")){
            if(request.getParameter("query") != null){
                division.addHidden("query").setValue(request.getParameter("query"));
            }
            if(request.getParameter("scope") != null){
                division.addHidden("scope").setValue(request.getParameter("scope"));
            }
        }

        //Add the filter queries, current search settings so these remain saved when performing a new search !
        if(type.equals("search") || type.equals("sort")){
            for (String fq : fqs) {
                division.addHidden("fq").setValue(fq);
            }
        }

        if(type.equals("search") || type.equals("filter")){
            if(request.getParameter("rpp") != null){
                division.addHidden("rpp").setValue(request.getParameter("rpp"));
            }
            if(request.getParameter("sort_by") != null){
                division.addHidden("sort_by").setValue(request.getParameter("sort_by"));
            }
            if(request.getParameter("order") != null){
                division.addHidden("order").setValue(request.getParameter("order"));
            }
        }
    }
}
