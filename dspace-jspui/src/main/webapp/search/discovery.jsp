<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>

<%--
  - Display the form to refine the simple-search and dispaly the results of the search
  -
  - Attributes to pass in:
  -
  -   scope            - pass in if the scope of the search was a community
  -                      or a collection
  -   scopes 		   - the list of available scopes where limit the search
  -   sortOptions	   - the list of available sort options
  -   availableFilters - the list of filters available to the user
  -
  -   query            - The original query
  -   queryArgs		   - The query configuration parameters (rpp, sort, etc.)
  -   appliedFilters   - The list of applied filters (user input or facet)
  -
  -   search.error     - a flag to say that an error has occurred
  -   spellcheck	   - the suggested spell check query (if any)
  -   qResults		   - the discovery results
  -   items            - the results.  An array of Items, most relevant first
  -   communities      - results, Community[]
  -   collections      - results, Collection[]
  -
  -   admin_button     - If the user is an admin
  --%>

<%@page import="org.apache.commons.lang.StringEscapeUtils" %>
<%@page import="org.apache.commons.lang.StringUtils" %>
<%@page import="org.dspace.app.webui.util.UIUtil" %>
<%@page import="org.dspace.content.Collection" %>
<%@page import="org.dspace.content.Community" %>
<%@page import="org.dspace.content.DSpaceObject" %>
<%@page import="org.dspace.content.Item" %>
<%@page import="org.dspace.core.ConfigurationManager" %>
<%@page import="org.dspace.core.Context" %>
<%@page import="org.dspace.discovery.DiscoverQuery" %>
<%@page import="org.dspace.discovery.DiscoverResult" %>
<%@page import="org.dspace.discovery.DiscoverResult.FacetResult" %>
<%@page import="org.dspace.discovery.configuration.DiscoverySearchFilter" %>
<%@page import="org.dspace.discovery.configuration.DiscoverySearchFilterFacet" %>
<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="org.dspace.sort.SortOption" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"
           prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core"
           prefix="c" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>
<%@ page import="org.dspace.storage.rdbms.DatabaseManager" %>
<%@ page import="org.dspace.storage.rdbms.TableRow" %>
<%@ page import="org.dspace.storage.rdbms.TableRowIterator" %>
<%@ page import="ua.edu.sumdu.essuir.utils.DCInputReader" %>
<%@ page import="ua.edu.sumdu.essuir.utils.EssuirUtils" %>
<%@ page import="java.net.URLEncoder" %>
<%@ page import="java.sql.SQLException" %>
<%@ page import="java.util.*" %>
<%!
    private static final String FIRST_PAPER_YEAR = "1964";
%><%
    // Get the attributes
    DSpaceObject scope = (DSpaceObject) request.getAttribute("scope");
    String searchScope = scope != null ? scope.getHandle() : "";
    List<DSpaceObject> scopes = (List<DSpaceObject>) request.getAttribute("scopes");
    List<String> sortOptions = (List<String>) request.getAttribute("sortOptions");

    String query = (String) request.getAttribute("query");
    if (query == null) {
        query = "";
    }
    Boolean error_b = (Boolean) request.getAttribute("search.error");
    boolean error = (error_b == null ? false : error_b.booleanValue());

    DiscoverQuery qArgs = (DiscoverQuery) request.getAttribute("queryArgs");
    String sortedBy = qArgs.getSortField();
    String order = qArgs.getSortOrder().toString();
    String ascSelected = (SortOption.ASCENDING.equalsIgnoreCase(order) ? "selected=\"selected\"" : "");
    String descSelected = (SortOption.DESCENDING.equalsIgnoreCase(order) ? "selected=\"selected\"" : "");
    String httpFilters = "";
    String spellCheckQuery = (String) request.getAttribute("spellcheck");
    List<DiscoverySearchFilter> availableFilters = (List<DiscoverySearchFilter>) request.getAttribute("availableFilters");
    List<String[]> appliedFilters = (List<String[]>) request.getAttribute("appliedFilters");
    List<String> appliedFilterQueries = (List<String>) request.getAttribute("appliedFilterQueries");
    if (appliedFilters != null && appliedFilters.size() > 0) {
        int idx = 1;
        for (String[] filter : appliedFilters) {
            httpFilters += "&amp;filter_field_" + idx + "=" + URLEncoder.encode(filter[0], "UTF-8");
            httpFilters += "&amp;filter_type_" + idx + "=" + URLEncoder.encode(filter[1], "UTF-8");
            httpFilters += "&amp;filter_value_" + idx + "=" + URLEncoder.encode(filter[2], "UTF-8");
            idx++;
        }
    }
    int rpp = qArgs.getMaxResults();
    int etAl = ((Integer) request.getAttribute("etal")).intValue();

    String[] options = new String[]{"equals", "contains", "notequals", "notcontains"};

    // Admin user or not
    Boolean admin_b = (Boolean) request.getAttribute("admin_button");
    boolean admin_button = (admin_b == null ? false : admin_b.booleanValue());

    java.util.Locale sessionLocale = org.dspace.app.webui.util.UIUtil.getSessionLocale(request);
    String locale = sessionLocale.toString();
    java.util.List codesList = DCInputReader.getInputsReader(locale).getPairs("common_iso_languages");

    StringBuilder langList = new StringBuilder();

    for (int i = 0; i < codesList.size(); i += 2) {
        langList.append("<option value=\\\"")
                .append(codesList.get(i + 1))
                .append("\\\">")
                .append(codesList.get(i))
                .append("</option>");
    }

    codesList = DCInputReader.getInputsReader(locale).getPairs("common_types");

    StringBuilder typeList = new StringBuilder();

    for (int i = 0; i < codesList.size(); i += 2) {
        typeList.append("<option value=\\\"")
                .append(codesList.get(i + 1))
                .append("\\\">")
                .append(codesList.get(i))
                .append("</option>");
    }
    Boolean needToDisplaySlider = true;
    int dateIssuedItemIndex = -1;
%>

<c:set var="dspace.layout.head.last" scope="request">
    <script type="text/javascript">
        var jQ = jQuery.noConflict();
        function go() {
            jQ("#spellCheckQuery").click(function () {
                jQ("#query").val(jQ(this).attr('data-spell'));
                jQ("#main-query-submit").click();
            });
            jQ("#filterquery")
                    .autocomplete({
                        source: function (request, response) {
                            jQ.ajax({
                                url: "<%= request.getContextPath() %>/json/discovery/autocomplete?query=<%= URLEncoder.encode(query,"UTF-8")%><%= httpFilters.replaceAll("&amp;","&") %>",
                                dataType: "json",
                                cache: false,
                                data: {
                                    auto_idx: jQ("#filtername").val(),
                                    auto_query: request.term,
                                    auto_sort: 'count',
                                    auto_type: jQ("#filtertype").val(),
                                    location: '<%= searchScope %>'
                                },
                                success: function (data) {
                                    response(jQ.map(data.autocomplete, function (item) {
                                        var tmp_val = item.authorityKey;
                                        if (tmp_val == null || tmp_val == '') {
                                            tmp_val = item.displayedValue;
                                        }
                                        return {
                                            label: item.displayedValue + " (" + item.count + ")",
                                            value: tmp_val
                                        };
                                    }))
                                }
                            })
                        }
                    });
            jQ("#filtername").change(function () {
                var activeType = jQ("#filtername").val();
                if (activeType == 'language') {
                    document.getElementById("filterqueryfield").innerHTML = "<select name=\"filterquery\" style=\"width:349px;\"  class=\"form-control\"><%= langList.toString() %></select>";
                } else if (activeType == 'type') {
                    document.getElementById("filterqueryfield").innerHTML = "<select name=\"filterquery\" style=\"width:349px;\"class=\"form-control\"><%= typeList.toString() %></select>";
                } else {
                    document.getElementById("filterqueryfield").innerHTML = "<input type=\"text\" id=\"filterquery\" name=\"filterquery\" size=\"45\" required=\"required\" class=\"form-control\"/>";
                    go();
                }
            });
        }
        jQ(document).ready(function () {
            go();
        });

        function validateFilters() {
            return document.getElementById("filterquery").value.length > 0;
        }

    </script>

</c:set>

<dspace:layout titlekey="jsp.search.title">

    <%-- <h1>Search Results</h1> --%>

    <h2><fmt:message key="jsp.search.title"/></h2>

    <div class="discovery-search-form panel panel-default">
            <%-- Controls for a repeat search --%>
        <div class="discovery-query panel-heading">
            <form action="simple-search" method="get">
                <div class="form-inline">
                    <div class="form-group">
                            <%--<label for="tlocation">--%>
                            <%--<fmt:message key="jsp.search.results.searchin"/>--%>
                            <%--</label>--%>
                        <select name="location" id="tlocation" class="form-control">
                            <%
                                if (scope == null) {
                                    // Scope of the search was all of DSpace.  The scope control will list
                                    // "all of DSpace" and the communities.
                            %>
                                <%-- <option selected value="/">All of DSpace</option> --%>
                            <option selected="selected" value="/"><fmt:message key="jsp.general.genericScope"/></option>
                            <% } else {
                            %>
                            <option value="/"><fmt:message key="jsp.general.genericScope"/></option>
                            <% }
                                for (DSpaceObject dso : scopes) {
                            %>
                            <option value="<%= dso.getHandle() %>" <%=dso.getHandle().equals(searchScope) ? "selected=\"selected\"" : "" %>>
                                <%= dso.getName() %>
                            </option>
                            <%
                                }
                            %></select>
                    </div>

                    <input type="submit" id="main-query-submit" class="btn btn-default"
                           value="<fmt:message key="jsp.general.go"/>"/>
                </div>
                <% if (StringUtils.isNotBlank(spellCheckQuery)) {%>
                <p class="lead"><fmt:message key="jsp.search.didyoumean"><fmt:param><a id="spellCheckQuery"
                                                                                       data-spell="<%= StringEscapeUtils.escapeHtml(spellCheckQuery) %>"
                                                                                       href="#"><%= spellCheckQuery %>
                </a></fmt:param></fmt:message></p>
                <% } %>
                <input type="hidden" value="<%= rpp %>" name="rpp"/>
                <input type="hidden" value="<%= sortedBy %>" name="sort_by"/>
                <input type="hidden" value="<%= order %>" name="order"/>
                <% if (appliedFilters.size() > 0) { %>
                <div class="discovery-search-appliedFilters" style="margin-top:20px;">
                        <%--<span><fmt:message key="jsp.search.filter.applied"/></span><br/>--%>
                    <%
                        int idx = 1;
                        for (String[] filter : appliedFilters) {
                            boolean found = false;
                    %>
                    <div class="form-inline">
                        <div class="form-group">
                            <select id="filter_field_<%=idx %>" name="filter_field_<%=idx %>_select" disabled
                                    class="form-control">
                                <%
                                    for (DiscoverySearchFilter searchFilter : availableFilters) {
                                        String fkey = "jsp.search.filter." + searchFilter.getIndexFieldName();
                                %>
                                <option value="<%= searchFilter.getIndexFieldName() %>"<%
                                    if (filter[0].equals(searchFilter.getIndexFieldName())) {
                                %> selected="selected"<%
                                        found = true;
                                    }
                                %>><fmt:message key="<%= fkey %>"/></option>
                                <%
                                    }
                                    if (!found) {
                                        String fkey = "jsp.search.filter." + filter[0];
                                %>
                                <option value="<%= filter[0] %>" selected="selected"><fmt:message
                                        key="<%= fkey %>"/></option>
                                <%
                                    }
                                %>
                            </select>
                        </div>
                        <input type="hidden" value="<%= filter[0] %>" name="filter_field_<%=idx %>"/>
                        <input type="hidden" value="<%= filter[1] %>" name="filter_type_<%=idx %>"/>

                        <div class="form-group">
                            <select id="filter_type_<%=idx %>" name="filter_type_<%=idx %>_select" disabled
                                    class="form-control">
                                <%
                                    for (String opt : options) {
                                        String fkey = "jsp.search.filter.op." + opt;
                                %>
                                <option value="<%= opt %>"<%= opt.equals(filter[1]) ? " selected=\"selected\"" : "" %>>
                                    <fmt:message key="<%= fkey %>"/></option>
                                <%
                                    }
                                    String filterValue = StringEscapeUtils.escapeHtml(filter[2]);
                                    String filterValueLocalized = filterValue;
                                    if (filter[0].equals("type")) {
                                        filterValueLocalized = EssuirUtils.getTypeLocalized(StringEscapeUtils.escapeHtml(filterValue), locale);
                                    } else if (filter[0].equals("language")) {
                                        filterValueLocalized = EssuirUtils.getLanguageLocalized(StringEscapeUtils.escapeHtml(filterValue), locale);
                                    }
                                %>
                            </select>
                        </div>
                        <div class="form-group">
                            <input type="text" id="filter_value_<%=idx %>_display" name="filter_value_<%=idx %>_display"
                                   value="<%= filterValueLocalized %>" size="45" readonly class="form-control"/>

                            <input type="hidden" value="<%= filterValue %>" name="filter_value_<%=idx %>"
                                   id="filter_value_<%=idx %>"/>
                        </div>
                        <input class="btn btn-default" type="submit" id="submit_filter_remove_<%=idx %>"
                               name="submit_filter_remove_<%=idx %>" value="X"/>
                    </div>
                    <br/>
                    <%
                            idx++;
                        }
                    %>
                </div>
                <% } %>
                <a class="btn btn-default"
                   href="<%= request.getContextPath()+"/simple-search?rpp="+ConfigurationManager.getIntProperty("webui.collectionhome.perpage", 20) %>"><fmt:message
                        key="jsp.search.general.new-search"/></a>
            </form>
        </div>
        <% if (availableFilters.size() > 0) { %>
        <div class="discovery-search-filters panel-body">
            <h5><fmt:message key="jsp.search.filter.heading"/></h5>

            <p class="discovery-search-filters-hint"><fmt:message key="jsp.search.filter.hint"/></p>

            <form action="simple-search" method="get" class="form-inline">
                <input type="hidden" value="<%= StringEscapeUtils.escapeHtml(searchScope) %>" name="location"/>
                <input type="hidden" value="<%= StringEscapeUtils.escapeHtml(query) %>" name="query"/>
                <% if (appliedFilterQueries.size() > 0) {
                    int idx = 1;
                    for (String[] filter : appliedFilters) {
                        boolean found = false;
                %>
                <input type="hidden" id="filter_field_<%=idx %>" name="filter_field_<%=idx %>"
                       value="<%= filter[0] %>"/>
                <input type="hidden" id="filter_type_<%=idx %>" name="filter_type_<%=idx %>" value="<%= filter[1] %>"/>
                <input type="hidden" id="filter_value_<%=idx %>" name="filter_value_<%=idx %>"
                       value="<%= StringEscapeUtils.escapeHtml(filter[2]) %>"/>
                <%
                            idx++;
                        }
                    } %>
                <div class="form-group">
                    <select id="filtername" name="filtername" class="form-control">
                        <%
                            for (DiscoverySearchFilter searchFilter : availableFilters) {
                                String fkey = "jsp.search.filter." + searchFilter.getIndexFieldName();
                        %>
                        <option value="<%= searchFilter.getIndexFieldName() %>"><fmt:message
                                key="<%= fkey %>"/></option>
                        <%
                            }
                        %>
                    </select>
                </div>
                <div class="form-group">
                    <select id="filtertype" name="filtertype" class="form-control">
                        <%
                            for (String opt : options) {
                                String fkey = "jsp.search.filter.op." + opt;
                        %>
                        <option value="<%= opt %>"><fmt:message key="<%= fkey %>"/></option>
                        <%
                            }
                        %>
                    </select>
                </div>
                <div class="form-group">
                    <span id="filterqueryfield"><input type="text" id="filterquery" name="filterquery" size="45"
                                                       required="required" class="form-control"/></span>
                </div>
                <input type="hidden" value="<%= rpp %>" name="rpp"/>
                <input type="hidden" value="<%= sortedBy %>" name="sort_by"/>
                <input type="hidden" value="<%= order %>" name="order"/>
                <input class="btn btn-default" type="submit" value="<fmt:message key="jsp.search.filter.add"/>"
                       onclick="return validateFilters()"/>
            </form>

            <form action="simple-search" method="get" class="form-inline">
                <input type="hidden" value="<%= StringEscapeUtils.escapeHtml(searchScope) %>" name="location"/>
                <input type="hidden" value="<%= StringEscapeUtils.escapeHtml(query) %>" name="query"/>
                <% if (appliedFilterQueries.size() > 0) {
                    int idx = 1;

                    for (String[] filter : appliedFilters) {
                        boolean found = false;
                        if ("dateIssued".equals(filter[0])) {
                            dateIssuedItemIndex = idx;
                        }
                %>
                <input type="hidden" id="filter_field_<%=idx %>" name="filter_field_<%=idx %>"
                       value="<%= filter[0] %>"/>
                <input type="hidden" id="filter_type_<%=idx %>" name="filter_type_<%=idx %>" value="<%= filter[1] %>"/>
                <input type="hidden" id="filter_year_value_<%=idx %>" name="filter_value_<%=idx %>"
                       value="<%= StringEscapeUtils.escapeHtml(filter[2]) %>"/>
                <%
                            idx++;
                        }
                    } %>

                <div class="col-md-11" style="margin-top:5px;">
                    <div class="row">
                        <div id="year-slider"></div>
                    </div>
                    <div class="row" style="margin-top:5px;">
                        <input type="submit" class="btn btn-primary pull-right"
                               value="<fmt:message key="jsp.search.yearslider.button"/>"/>
                    </div>
                </div>
                <% if (dateIssuedItemIndex == -1) { %>
                <input type="hidden" value="dateIssued" id="filtername" name="filtername"/>
                <input type="hidden" value="equals" id="filtertype" name="filtertype"/>
                <input type="hidden" value="[]" id="filterquery_year" name="filterquery"/>
                <% } %>
                <input type="hidden" value="<%= rpp %>" name="rpp"/>
                <input type="hidden" value="<%= sortedBy %>" name="sort_by"/>
                <input type="hidden" value="<%= order %>" name="order"/>

            </form>


        </div>
        <% } %>
    </div>

    <%
        DiscoverResult qResults = (DiscoverResult) request.getAttribute("queryresults");
        Item[] items = (Item[]) request.getAttribute("items");
        Community[] communities = (Community[]) request.getAttribute("communities");
        Collection[] collections = (Collection[]) request.getAttribute("collections");

        String minimalYearBound = EssuirUtils.getMinimalPaperYear();
        String maximalYearBound = String.valueOf(Calendar.getInstance().get(Calendar.YEAR));

        String minimalYear = minimalYearBound;
        String maximalYear = String.valueOf(Calendar.getInstance().get(Calendar.YEAR));

        if (dateIssuedItemIndex != -1) {
            String issuedFilterValue = appliedFilters.get(dateIssuedItemIndex - 1)[2];
            String[] yearsInFilter = issuedFilterValue.replaceAll("[\\[\\]]", "").split(" TO ");
            minimalYear = yearsInFilter[0];
            maximalYear = yearsInFilter[1];
        } else {
            List<FacetResult> years = qResults.getFacetResult("dateIssued.year");

            if (years.isEmpty()) {
                years = qResults.getFacetResult("dateIssued");
            }

            if (!years.isEmpty()) {
                List<String> papersYears = new LinkedList<>();

                for (FacetResult year : years) {
                    for (String currentYearItem : year.getDisplayedValue().split(" - ")) {
                        papersYears.add(currentYearItem);
                    }
                }
                Collections.sort(papersYears);

                minimalYear = papersYears.get(0);
                maximalYear = papersYears.get(papersYears.size() - 1);
            }
        }

    %>
    <script>
        var yearRangeValuesField = "#<%= (dateIssuedItemIndex == -1) ? "filterquery_year" : "filter_year_value_" + dateIssuedItemIndex %>";

        jQ(document).ready(function () {
            jQ("#year-slider").rangeSlider({
                arrows: false,
                step: 1,
                bounds: {
                    min: <%= minimalYearBound %>,
                    max: <%= maximalYearBound %>
                },
                defaultValues: {
                    min: <%= minimalYear %>,
                    max: <%= maximalYear %>
                }
            });

            jQ("#filterquery_year").val("[" + <%= minimalYear %> +" TO " + <%= maximalYear %>+"]");
        });
        jQ("#year-slider").bind("valuesChanged", function (e, data) {
            console.log(yearRangeValuesField);
            console.log("Values just changed. min: " + data.values.min + " max: " + data.values.max);

            jQ(yearRangeValuesField).val("[" + data.values.min + " TO " + data.values.max + "]");
        });
    </script>
    <%
        if (error) {
    %>
    <p align="center" class="submitFormWarn">
        <fmt:message key="jsp.search.error.discovery"/>
    </p>
    <%
    } else if (qResults != null && qResults.getTotalSearchResults() == 0) {
    %>
    <%-- <p align="center">Search produced no results.</p> --%>
    <p align="center"><fmt:message key="jsp.search.general.noresults"/></p>
    <%
    } else if (qResults != null) {
        long pageTotal = ((Long) request.getAttribute("pagetotal")).longValue();
        long pageCurrent = ((Long) request.getAttribute("pagecurrent")).longValue();
        long pageLast = ((Long) request.getAttribute("pagelast")).longValue();
        long pageFirst = ((Long) request.getAttribute("pagefirst")).longValue();

        // create the URLs accessing the previous and next search result pages
        String baseURL = request.getContextPath()
                + (searchScope != "" ? "/handle/" + searchScope : "")
                + "/simple-search?query="
                + URLEncoder.encode(query, "UTF-8")
                + httpFilters
                + "&amp;sort_by=" + sortedBy
                + "&amp;order=" + order
                + "&amp;rpp=" + rpp
                + "&amp;etal=" + etAl
                + "&amp;start=";

        String nextURL = baseURL;
        String firstURL = baseURL;
        String lastURL = baseURL;

        String prevURL = baseURL
                + (pageCurrent - 2) * qResults.getMaxResults();

        nextURL = nextURL
                + (pageCurrent) * qResults.getMaxResults();

        firstURL = firstURL + "0";
        lastURL = lastURL + (pageTotal - 1) * qResults.getMaxResults();
    %>

    <div class="discovery-result-results">
        <% if (communities.length > 0) { %>
        <div class="panel panel-info">
            <div class="panel-heading"><fmt:message key="jsp.search.results.comhits"/></div>
            <dspace:communitylist communities="<%= communities %>"/>
        </div>
        <% } %>

        <% if (collections.length > 0) { %>
        <div class="panel panel-info">
            <div class="panel-heading"><fmt:message key="jsp.search.results.colhits"/></div>
            <dspace:collectionlist collections="<%= collections %>"/>
        </div>
        <% } %>

        <% if (items.length > 0) { %>
        <div class="panel panel-info">
            <div class="panel-heading"><fmt:message key="jsp.search.results.itemhits"/>
                (<%= qResults.getTotalSearchResults() %>)
                <div class="pull-right"><img src="/image/cog-icon.png" width="20px" data-toggle="modal"
                                             data-target="#results-settings"></div>
            </div>
            <dspace:itemlist items="<%= items %>" authorLimit="<%= etAl %>"/>
        </div>
        <% } %>
    </div>
    <%@include file="../pagination/pagination-search.jsp" %>

    <% } %>
    <dspace:sidebar>
        <%
            boolean brefine = false;

            List<DiscoverySearchFilterFacet> facetsConf = (List<DiscoverySearchFilterFacet>) request.getAttribute("facetsConfig");
            Map<String, Boolean> showFacets = new HashMap<String, Boolean>();

            for (DiscoverySearchFilterFacet facetConf : facetsConf) {
                String f = facetConf.getIndexFieldName();
                List<FacetResult> facet = qResults.getFacetResult(f);
                if (facet.size() == 0) {
                    facet = qResults.getFacetResult(f + ".year");
                    if (facet.size() == 0) {
                        showFacets.put(f, false);
                        continue;
                    }
                }
                boolean showFacet = false;
                for (FacetResult fvalue : facet) {
                    if (!appliedFilterQueries.contains(f + "::" + fvalue.getFilterType() + "::" + fvalue.getAsFilterQuery())) {
                        showFacet = true;
                        break;
                    }
                }
                showFacets.put(f, showFacet);
                brefine = brefine || showFacet;
            }
            if (brefine) {
        %>

        <h3 class="facets"><fmt:message key="jsp.search.facet.refine"/></h3>

        <div id="facets" class="facetsBox">

            <%
                for (DiscoverySearchFilterFacet facetConf : facetsConf) {
                    String f = facetConf.getIndexFieldName();
                    if (!showFacets.get(f))
                        continue;
                    List<FacetResult> facet = qResults.getFacetResult(f);
                    if (facet.size() == 0) {
                        facet = qResults.getFacetResult(f + ".year");
                    }
                    int limit = facetConf.getFacetLimit() + 1;

                    String fkey = "jsp.search.facet.refine." + f;
            %>
            <div id="facet_<%= f %>" class="panel panel-success">
                <div class="panel-heading"><fmt:message key="<%= fkey %>"/></div>
                <ul class="list-group"><%
                    int idx = 1;
                    int currFp = UIUtil.getIntParameter(request, f + "_page");
                    if (currFp < 0) {
                        currFp = 0;
                    }
                    for (FacetResult fvalue : facet) {
                        if (idx != limit && !appliedFilterQueries.contains(f + "::" + fvalue.getFilterType() + "::" + fvalue.getAsFilterQuery())) {
                %>
                    <li class="list-group-item"><span class="badge"><%= fvalue.getCount() %></span> <a href="<%= request.getContextPath()
                + (!Objects.equals(searchScope, "")?"/handle/"+searchScope:"")
                + "/simple-search?query="
                + URLEncoder.encode(query,"UTF-8")
                + "&amp;sort_by=" + sortedBy
                + "&amp;order=" + order
                + "&amp;rpp=" + rpp
                + httpFilters
                + "&amp;etal=" + etAl
                + "&amp;filtername="+URLEncoder.encode(f,"UTF-8")
                + "&amp;filterquery="+URLEncoder.encode(fvalue.getAsFilterQuery(),"UTF-8")
                + "&amp;filtertype="+URLEncoder.encode(fvalue.getFilterType(),"UTF-8") %>"
                                                                                                       title="<fmt:message key="jsp.search.facet.narrow"><fmt:param><%=fvalue.getDisplayedValue() %></fmt:param></fmt:message>">
                        <%= StringUtils.abbreviate(fvalue.getDisplayedValue(), 36) %>
                    </a></li>
                    <%
                                idx++;
                            }
                            if (idx > limit) {
                                break;
                            }
                        }
                        if (currFp > 0 || idx == limit) {
                    %>
                    <li class="list-group-item"><span style="visibility: hidden;">.</span>
                        <% if (currFp > 0) { %>
                        <a class="pull-left" href="<%= request.getContextPath()
	            + (!Objects.equals(searchScope, "")?"/handle/"+searchScope:"")
                + "/simple-search?query="
                + URLEncoder.encode(query,"UTF-8")
                + "&amp;sort_by=" + sortedBy
                + "&amp;order=" + order
                + "&amp;rpp=" + rpp
                + httpFilters
                + "&amp;etal=" + etAl
                + "&amp;"+f+"_page="+(currFp-1) %>"><fmt:message key="jsp.search.facet.refine.previous"/></a>
                        <% } %>
                        <% if (idx == limit) { %>
                        <a href="<%= request.getContextPath()
	            + (!Objects.equals(searchScope, "")?"/handle/"+searchScope:"")
                + "/simple-search?query="
                + URLEncoder.encode(query,"UTF-8")
                + "&amp;sort_by=" + sortedBy
                + "&amp;order=" + order
                + "&amp;rpp=" + rpp
                + httpFilters
                + "&amp;etal=" + etAl
                + "&amp;"+f+"_page="+(currFp+1) %>"><span class="pull-right"><fmt:message
                                key="jsp.search.facet.refine.next"/></span></a>
                        <%
                            }
                        %></li>
                    <%
                        }
                    %></ul>
            </div>
            <%
                }

            %>

        </div>
        <% } %>
    </dspace:sidebar>
</dspace:layout>


<%-- Include a component for modifying sort by, order, results per page, and et-al limit --%>
<div class="modal fade" id="results-settings" tabindex="-1" role="dialog" aria-labelledby="myModalLabel">
    <div class="modal-dialog" role="document">
        <div class="modal-content">
            <form action="simple-search" method="get" class="form-horizontal">
                <div class="modal-body">

                    <input type="hidden" value="<%= StringEscapeUtils.escapeHtml(searchScope) %>" name="location"/>
                    <input type="hidden" value="<%= StringEscapeUtils.escapeHtml(query) %>" name="query"/>
                    <% if (appliedFilterQueries.size() > 0) {
                        int idx = 1;
                        for (String[] filter : appliedFilters) {
                            boolean found = false;
                    %>
                    <input type="hidden" id="filter_field_<%=idx %>" name="filter_field_<%=idx %>"
                           value="<%= filter[0] %>"/>
                    <input type="hidden" id="filter_type_<%=idx %>" name="filter_type_<%=idx %>"
                           value="<%= filter[1] %>"/>
                    <input type="hidden" id="filter_value_<%=idx %>" name="filter_value_<%=idx %>"
                           value="<%= StringEscapeUtils.escapeHtml(filter[2]) %>"/>
                    <%
                                idx++;
                            }
                        } %>
                    <div class="form-group">
                        <label for="rpp" class="control-label col-md-5"><fmt:message
                                key="search.results.perpage"/></label>

                        <div class="col-md-7">
                            <select name="rpp" class="form-control">
                                <%
                                    for (int i = 5; i <= 100; i += 5) {
                                        String selected = (i == rpp ? "selected=\"selected\"" : "");
                                %>
                                <option value="<%= i %>" <%= selected %>><%= i %>
                                </option>
                                <%
                                    }
                                %>
                            </select>
                        </div>
                    </div>
                    <%
                        if (sortOptions.size() > 0) {
                    %>
                    <div class="form-group">
                        <label for="sort_by" class="control-label col-md-5"><fmt:message
                                key="search.results.sort-by"/></label>

                        <div class="col-md-7">
                            <select name="sort_by" class="form-control">
                                <option value="score"><fmt:message key="search.sort-by.relevance"/></option>
                                <%
                                    for (String sortBy : sortOptions) {
                                        String selected = (sortBy.equals(sortedBy) ? "selected=\"selected\"" : "");
                                        String mKey = "search.sort-by." + sortBy;
                                %>
                                <option value="<%= sortBy %>" <%= selected %>><fmt:message key="<%= mKey %>"/></option>
                                <%
                                    }
                                %>
                            </select>
                        </div>
                    </div>
                    <%
                        }
                    %>
                    <div class="form-group">
                        <label for="order" class="control-label col-md-5"><fmt:message
                                key="search.results.order"/></label>

                        <div class="col-md-7">
                            <select name="order" class="form-control">
                                <option value="ASC" <%= ascSelected %>><fmt:message key="search.order.asc"/></option>
                                <option value="DESC" <%= descSelected %>><fmt:message key="search.order.desc"/></option>
                            </select>
                        </div>
                    </div>

                    <div class="form-group">
                        <label for="etal" class="control-label col-md-5"><fmt:message
                                key="search.results.etal"/></label>

                        <div class="col-md-7">
                            <select name="etal" class="form-control">
                                <%
                                    String unlimitedSelect = "";
                                    if (etAl < 1) {
                                        unlimitedSelect = "selected=\"selected\"";
                                    }
                                %>
                                <option value="0" <%= unlimitedSelect %>><fmt:message
                                        key="browse.full.etal.unlimited"/></option>
                                <%
                                    boolean insertedCurrent = false;
                                    for (int i = 0; i <= 50; i += 5) {
                                        // for the first one, we want 1 author, not 0
                                        if (i == 0) {
                                            String sel = (i + 1 == etAl ? "selected=\"selected\"" : "");
                                %>
                                <option value="1" <%= sel %>>1</option>
                                <%
                                    }

                                    // if the current i is greated than that configured by the user,
                                    // insert the one specified in the right place in the list
                                    if (i > etAl && !insertedCurrent && etAl > 1) {
                                %>
                                <option value="<%= etAl %>" selected="selected"><%= etAl %>
                                </option>
                                <%
                                        insertedCurrent = true;
                                    }

                                    // determine if the current not-special case is selected
                                    String selected = (i == etAl ? "selected=\"selected\"" : "");

                                    // do this for all other cases than the first and the current
                                    if (i != 0 && i != etAl) {
                                %>
                                <option value="<%= i %>" <%= selected %>><%= i %>
                                </option>
                                <%
                                        }
                                    }
                                %>
                            </select>
                        </div>
                    </div>


                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-warning" data-dismiss="modal">Close</button>
                    <%
                        if (admin_button) {
                    %><input type="submit" class="btn btn-default" name="submit_export_metadata"
                             value="<fmt:message key="jsp.general.metadataexport.button"/>"/><%
                    }
                %>

                    <input class="btn btn-primary" type="submit" name="submit_search"
                           value="<fmt:message key="search.update" />"/>
                </div>
            </form>
        </div>
    </div>
</div>