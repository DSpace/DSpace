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

<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"
    prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core"
    prefix="c" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>
<%@page import="org.dspace.app.cris.model.ACrisObject" %>
<%@ page import="java.net.URLEncoder"            %>
<%@ page import="org.dspace.content.Community"   %>
<%@ page import="org.dspace.content.Collection"  %>
<%@ page import="org.dspace.content.Item"        %>
<%@ page import="org.dspace.search.QueryResults" %>
<%@ page import="org.dspace.sort.SortOption" %>
<%@ page import="java.util.Enumeration" %>
<%@ page import="java.util.Set" %>
<%@page import="org.dspace.discovery.IGlobalSearchResult"%>
<%@page import="java.util.StringTokenizer"%>
<%@page import="org.dspace.browse.BrowseInfo"%>
<%@page import="org.dspace.browse.BrowseDSpaceObject"%>
<%@page import="org.dspace.core.Utils"%>
<%@page import="org.dspace.discovery.configuration.*"%>
<%@page import="org.dspace.app.webui.util.UIUtil"%>
<%@page import="java.util.HashMap"%>
<%@page import="java.util.ArrayList"%>
<%@page import="org.dspace.discovery.DiscoverFacetField"%>
<%@page import="org.dspace.discovery.DiscoverFilterQuery"%>
<%@page import="org.dspace.discovery.DiscoverQuery"%>
<%@page import="org.apache.commons.lang.StringUtils"%>
<%@page import="java.util.Map"%>
<%@page import="org.dspace.discovery.DiscoverResult.FacetResult"%>
<%@page import="org.dspace.discovery.DiscoverResult.DSpaceObjectHighlightResult"%>
<%@page import="org.dspace.discovery.DiscoverResult"%>
<%@page import="org.dspace.content.DSpaceObject"%>
<%@page import="java.util.List"%>
<%
    // Get the attributes
    String query = (String) request.getAttribute("query");
	if (query == null)
	{
	    query = "";
	}
	
    String searchScope = (String) request.getParameter("location" );
    if (searchScope == null)
    {
        searchScope = "global";
    }
    
    List<String> sortOptions = (List<String>) request.getAttribute("sortOptions");
    
    Boolean error_b = (Boolean)request.getAttribute("search.error");
    boolean error = (error_b == null ? false : error_b.booleanValue());
    
    DiscoverQuery qArgs = (DiscoverQuery) request.getAttribute("queryArgs");
    String sortedBy = qArgs.getSortField();
    
    String httpFilters ="";
	String spellCheckQuery = (String) request.getAttribute("spellcheck");
    List<DiscoverySearchFilter> availableFilters = (List<DiscoverySearchFilter>) request.getAttribute("availableFilters");
	List<String[]> appliedFilters = (List<String[]>) request.getAttribute("appliedFilters");
	List<String> appliedFilterQueries = (List<String>) request.getAttribute("appliedFilterQueries");
	
	if (appliedFilters != null && appliedFilters.size() >0 ) 
	{
	    int idx = 1;
	    for (String[] filter : appliedFilters)
	    {
	        httpFilters += "&amp;filter_field_"+idx+"="+URLEncoder.encode(filter[0],"UTF-8");
	        httpFilters += "&amp;filter_type_"+idx+"="+URLEncoder.encode(filter[1],"UTF-8");
	        httpFilters += "&amp;filter_value_"+idx+"="+URLEncoder.encode(filter[2],"UTF-8");
	        idx++;
	    }
	}

    String[] options = new String[]{"equals","contains","authority","notequals","notcontains","notauthority"};
    
    // Admin user or not
    Boolean admin_b = (Boolean)request.getAttribute("admin_button");
    boolean admin_button = (admin_b == null ? false : admin_b.booleanValue());
    
    DiscoverResult qResults = (DiscoverResult)request.getAttribute("queryresults");
	DiscoverySearchFilterFacet facetGlobalConf = (DiscoverySearchFilterFacet) request.getAttribute("facetGlobalConfig");
	String fGlobal = facetGlobalConf.getIndexFieldName();
	List<FacetResult> facetGlobal = null;
    Map<String, Long> numResultsByType = new HashMap<String, Long>();
	String fkeyGlobal = null;
	if(qResults!=null) {
	    facetGlobal = qResults.getFacetResult(fGlobal);
	    fkeyGlobal = "jsp.search.facet.refine."+fGlobal;
	    for (FacetResult fg : facetGlobal) {
	    	numResultsByType.put(fg.getAuthorityKey(), fg.getCount());
	    }
	}

%>

<c:set var="dspace.layout.head.last" scope="request">
<script type="text/javascript">
	var jQ = jQuery.noConflict();
	jQ(document).ready(function() {
		jQ( "#spellCheckQuery").click(function(){
			jQ("#query").val(jQ(this).attr('data-spell'));
			jQ("#main-query-submit").click();
		});
		jQ( "#filterquery" )
			.autocomplete({
				source: function( request, response ) {
					jQ.ajax({
						url: "<%= request.getContextPath() %>/json/discovery/autocomplete?query=<%= URLEncoder.encode(query,"UTF-8")%><%= httpFilters.replaceAll("&amp;","&") %>",
						dataType: "json",
						cache: false,
						data: {
							auto_idx: jQ("#filtername").val(),
							auto_query: request.term,
							auto_sort: 'count',
							auto_type: jQ("#filtertype").val(),
							location: '<%= searchScope%>'	
						},
						success: function( data ) {
							response( jQ.map( data.autocomplete, function( item ) {
								var tmp_val = item.authorityKey;
								if (tmp_val == null || tmp_val == '')
								{
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
	});
	function validateFilters() {
		return document.getElementById("filterquery").value.length > 0;
	}
</script>		
</c:set>

<dspace:layout titlekey="jsp.search.title">

    <%-- <h1>Search Results</h1> --%>

<h2><fmt:message key="jsp.search.title"/></h2>

<div class="discovery-search-formt">
    <%-- Controls for a repeat search --%>
	<div class="discovery-query">
     <form id="update-form" action="global-search" method="get">
	 
                                <label for="query"><fmt:message key="jsp.search.results.searchfor"/></label>
                                <input type="text" size="50" id="query" name="query" value="<%= (query==null ? "" : Utils.addEntities(query)) %>"/>
                                <input type="submit" id="main-query-submit" class="btn btn-primary" value="<fmt:message key="jsp.general.go"/>" />
<% if (StringUtils.isNotBlank(spellCheckQuery)) {%>
	<p class="lead"><fmt:message key="jsp.search.didyoumean"><fmt:param><a id="spellCheckQuery" data-spell="<%= Utils.addEntities(spellCheckQuery) %>" href="#"><%= spellCheckQuery %></a></fmt:param></fmt:message></p>
<% } %>                  
<% if (appliedFilters.size() > 0 ) { %>                                
		<div class="discovery-search-appliedFilters">
		<span><fmt:message key="jsp.search.filter.applied" /></span>
		<%
			int idx = 1;
			for (String[] filter : appliedFilters)
			{
			    boolean found = false;
			    %>
			    <select id="filter_field_<%=idx %>" name="filter_field_<%=idx %>">
				<%
					for (DiscoverySearchFilter searchFilter : availableFilters)
					{
					    String fkey = "jsp.search.filter."+searchFilter.getIndexFieldName();
					    %><option value="<%= Utils.addEntities(searchFilter.getIndexFieldName()) %>"<% 
					            if (filter[0].equals(searchFilter.getIndexFieldName()))
					            {
					                %> selected="selected"<%
					                found = true;
					            }
					            %>><fmt:message key="<%= fkey %>"/></option><%
					}
					if (!found)
					{
					    String fkey = "jsp.search.filter."+filter[0];
					    %><option value="<%= Utils.addEntities(filter[0]) %>" selected="selected"><fmt:message key="<%= fkey %>"/></option><%
					}
				%>
				</select>
				<select id="filter_type_<%=idx %>" name="filter_type_<%=idx %>">
				<%
					for (String opt : options)
					{
					    String fkey = "jsp.search.filter.op."+opt;
					    %><option value="<%= Utils.addEntities(opt) %>"<%= opt.equals(filter[1])?" selected=\"selected\"":"" %>><fmt:message key="<%= fkey %>"/></option><%
					}
				%>
				</select>
				<input type="text" id="filter_value_<%=idx %>" name="filter_value_<%=idx %>" value="<%= Utils.addEntities(filter[2]) %>" size="45"/>
				<input class="btn btn-default" type="submit" id="submit_filter_remove_<%=idx %>" name="submit_filter_remove_<%=idx %>" value="X" />
				<br/>
				<%
				idx++;
			}
		%>
		</div>
<% } %>
<a class="btn btn-default" href="<%= request.getContextPath()+"/global-search" %>"><fmt:message key="jsp.search.general.new-search" /></a>	
		</form>
		</div>
<% if (availableFilters.size() > 0) { %>
		<div class="discovery-search-filters">
		<h5><fmt:message key="jsp.search.filter.heading" /></h5>
		<p class="discovery-search-filters-hint"><fmt:message key="jsp.search.filter.hint" /></p>
		<form action="global-search" method="get">
		<input type="hidden" value="<%= Utils.addEntities(query) %>" name="query" />
		<% if (appliedFilterQueries.size() > 0 ) { 
				int idx = 1;
				for (String[] filter : appliedFilters)
				{
				    boolean found = false;
				    %>
				    <input type="hidden" id="filter_field_<%=idx %>" name="filter_field_<%=idx %>" value="<%= Utils.addEntities(filter[0]) %>" />
					<input type="hidden" id="filter_type_<%=idx %>" name="filter_type_<%=idx %>" value="<%= Utils.addEntities(filter[1]) %>" />
					<input type="hidden" id="filter_value_<%=idx %>" name="filter_value_<%=idx %>" value="<%= Utils.addEntities(filter[2]) %>" />
					<%
					idx++;
				}
		} %>
		<select id="filtername" name="filtername">
		<%
			for (DiscoverySearchFilter searchFilter : availableFilters)
			{
			    String fkey = "jsp.search.filter."+searchFilter.getIndexFieldName();
			    %><option value="<%= searchFilter.getIndexFieldName() %>"><fmt:message key="<%= fkey %>"/></option><%
			}
		%>
		</select>
		<select id="filtertype" name="filtertype">
		<%
			for (String opt : options)
			{
			    String fkey = "jsp.search.filter.op."+opt;
			    %><option value="<%= opt %>"><fmt:message key="<%= fkey %>"/></option><%
			}
		%>
		</select>
		<input type="text" id="filterquery" name="filterquery" size="45" required="required" />
		<input class="btn btn-default" type="submit" value="<fmt:message key="jsp.search.filter.add"/>" onclick="return validateFilters()" />
		</form>
		</div>        
<% } %>
</div>   
<% 

Map<String, List<IGlobalSearchResult>> collapsedResults = (Map<String, List<IGlobalSearchResult>>) request.getAttribute("results");
Map<String,DiscoveryViewConfiguration> mapViewMetadata = (Map<String,DiscoveryViewConfiguration>) request.getAttribute("viewMetadata");
String selectorViewMetadata = (String)request.getAttribute("selectorViewMetadata");

if( error )
{
 %>
	<p align="center" class="submitFormWarn">
		<fmt:message key="jsp.search.error.discovery" />
	</p>
	<%
}
else if( (qResults != null && qResults.getTotalSearchResults() == 0) || collapsedResults == null)
{
 %>
    <%-- <p align="center">Search produced no results.</p> --%>
    <p align="center"><fmt:message key="jsp.search.general.noresults"/></p>
<%
}
else if( qResults != null && collapsedResults != null)
{
	
	
%>

<div class="discovery-result-results">
		<%
			Set<String> otherTypes = collapsedResults.keySet();
					if (otherTypes != null && otherTypes.size() > 0) {
						for (String otypeSensitive : otherTypes) {
							String otypelower = otypeSensitive.toLowerCase();
							String okey = "jsp.search.results.cris." + otypelower;
		%>
		
		<div class="panel panel-info">
			<div class="panel-heading">
				<fmt:message key="<%=okey%>" />
			</div>
			<div class="list-item">
				<%
					for (IGlobalSearchResult obj : collapsedResults.get(otypeSensitive)) {
				%>
				
				<dspace:discovery-artifact style="global" hlt="<%= qResults.getHighlightedResults((DSpaceObject) obj) %>" artifact="<%= obj %>" view="<%= mapViewMetadata.get(otypelower) %>" selectorCssView="<%=selectorViewMetadata %>"/>
				
				<%	
				}
				String messageAllGlobalType = "jsp.search.global.all." + otypeSensitive;
				%>					
			</div>
		<% if (collapsedResults.get(otypeSensitive).size() < numResultsByType.get(otypeSensitive)) { %>
			<div class="panel-footer text-right">	
				<a class="btn btn-link text-primary" role="button" href="<%= request.getContextPath()
                + "/simple-search?query="
                + URLEncoder.encode(query,"UTF-8")
                + httpFilters                
                + "&amp;location="+URLEncoder.encode(otypeSensitive,"UTF-8") %>">
                <fmt:message key="<%= messageAllGlobalType %>">
                	<fmt:param><%= numResultsByType.get(otypeSensitive) %></fmt:param>
                </fmt:message></a>                
			</div>
		<% } %>
		</div>
		<%
			}
		}
		%>
	
</div>
<%
		}
	%>
<dspace:sidebar>
<%


boolean brefine = false;
List<DiscoverySearchFilterFacet> facetsConf = (List<DiscoverySearchFilterFacet>) request.getAttribute("facetsConfig");
Map<String, Boolean> showFacets = new HashMap<String, Boolean>();
	
for (DiscoverySearchFilterFacet facetConf : facetsConf)
{
	if(qResults!=null) {
	    String f = facetConf.getIndexFieldName();
	    List<FacetResult> facet = qResults.getFacetResult(f);
	    if (facet.size() == 0)
	    {
	        facet = qResults.getFacetResult(f+".year");
		    if (facet.size() == 0)
		    {
		        showFacets.put(f, false);
		        continue;
		    }
	    }
	    boolean showFacet = false;
	    for (FacetResult fvalue : facet)
	    { 
			if(!appliedFilterQueries.contains(f+"::"+fvalue.getFilterType()+"::"+fvalue.getAsFilterQuery()))
		    {
		        showFacet = true;
		        break;
		    }
	    }
	    showFacets.put(f, showFacet);
	    brefine = brefine || showFacet;
	}
}

	    if (facetGlobal != null && facetGlobal.size() > 0) { %>
	    <h3 class="facets"><fmt:message key="jsp.search.facet.refine" /></h3>

		<div id="globalFacet" class="facetsBox">
	    <div id="facet_<%= fkeyGlobal %>" class="panel panel-primary">
	    <div class="panel-heading"><fmt:message key="<%= fkeyGlobal %>" /></div>
	    <ul class="list-group"><%
	    for (FacetResult fvalue : facetGlobal)
	    { 
	        %><li class="list-group-item"><span class="badge"><%= fvalue.getCount() %></span> <a href="<%= request.getContextPath()
                + "/simple-search?query="
                + URLEncoder.encode(query,"UTF-8")
                + httpFilters                
                + "&amp;location="+URLEncoder.encode(fvalue.getAuthorityKey(),"UTF-8") %>"
                title="<fmt:message key="jsp.search.facet.narrow"><fmt:param><%=fvalue.getDisplayedValue() %></fmt:param></fmt:message>">
                <%= StringUtils.abbreviate(fvalue.getDisplayedValue(),36) %></a></li><%
	    }
	    %></ul></div>
	    </div>			
<% }
	if (brefine) {
%>

<div id="facets" class="facetsBox">

<%
	for (DiscoverySearchFilterFacet facetConf : facetsConf)
	{
	    String f = facetConf.getIndexFieldName();
	    if (!showFacets.get(f))
	        continue;
	    List<FacetResult> facet = qResults.getFacetResult(f);
	    if (facet.size() == 0)
	    {
	        facet = qResults.getFacetResult(f+".year");
	    }
	    int limit = facetConf.getFacetLimit()+1;
	    
	    String fkey = "jsp.search.facet.refine."+f;
	    %><div id="facet_<%= f %>" class="panel panel-success">
	    <div class="panel-heading"><fmt:message key="<%= fkey %>" /></div>
	    <ul class="list-group"><%
	    int idx = 1;
	    int currFp = UIUtil.getIntParameter(request, f+"_page");
	    if (currFp < 0)
	    {
	        currFp = 0;
	    }
	    for (FacetResult fvalue : facet)
	    { 
	        if (idx != limit && !appliedFilterQueries.contains(f+"::"+fvalue.getFilterType()+"::"+fvalue.getAsFilterQuery()))
	        {
	        %><li class="list-group-item"><span class="badge"><%= fvalue.getCount() %></span> <a href="<%= request.getContextPath()
                + "/global-search?query="
                + URLEncoder.encode(query,"UTF-8")
				+ "&amp;location="+searchScope
                + httpFilters
                + "&amp;filtername="+URLEncoder.encode(f,"UTF-8")
                + "&amp;filterquery="+URLEncoder.encode(fvalue.getAsFilterQuery(),"UTF-8")
                + "&amp;filtertype="+URLEncoder.encode(fvalue.getFilterType(),"UTF-8") %>"
                title="<fmt:message key="jsp.search.facet.narrow"><fmt:param><%=fvalue.getDisplayedValue() %></fmt:param></fmt:message>">
                <%= StringUtils.abbreviate(fvalue.getDisplayedValue(),36) %></a></li><%
                idx++;
	        }
	        if (idx > limit)
	        {
	            break;
	        }
	    }
	    if (currFp > 0 || idx == limit)
	    {
	        %><li class="list-group-item"><span style="visibility: hidden;">.</span>
	        <% if (currFp > 0) { %>
	        <a class="pull-left" href="<%= request.getContextPath()
                + "/global-search?query="
                + URLEncoder.encode(query,"UTF-8")
				+ "&amp;location="+searchScope
                + httpFilters
                + "&amp;"+f+"_page="+(currFp-1) %>"><fmt:message key="jsp.search.facet.refine.previous" /></a>
            <% } %>
            <% if (idx == limit) { %>
            <a href="<%= request.getContextPath()
                + "/global-search?query="
                + URLEncoder.encode(query,"UTF-8")
				+ "&amp;location="+searchScope
                + httpFilters
                + "&amp;"+f+"_page="+(currFp+1) %>"><span class="pull-right"><fmt:message key="jsp.search.facet.refine.next" /></span></a>
            <%
            }
            %></li><%
	    }
	    %></ul></div><%
	}

%>

</div>
<% } %>
</dspace:sidebar>
</dspace:layout>