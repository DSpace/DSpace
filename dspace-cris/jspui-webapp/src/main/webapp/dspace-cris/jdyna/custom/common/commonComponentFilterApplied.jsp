<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    https://github.com/CILEA/dspace-cris/wiki/License

--%>
<%@page import="org.apache.commons.lang.StringUtils"%>
<%@page import="java.util.List"%>
<%@page import="org.apache.commons.lang.StringEscapeUtils" %>
<%@page import="java.net.URLEncoder"            %>
<%@page import="org.dspace.sort.SortOption" %>
<%@page import="java.util.Enumeration" %>
<%@page import="java.util.Set" %>
<%@page import="java.util.Map" %>
<%@page import="org.dspace.eperson.EPerson"%>
<%@page import="org.dspace.content.Item"        %>
<%@page import="org.dspace.app.webui.cris.dto.ComponentInfoDTO"%>
<%@page import="it.cilea.osd.jdyna.web.Box"%>
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
<%@page import="org.dspace.core.Utils"%>
<%@ page import="org.dspace.core.ConfigurationManager" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>
<%@ taglib uri="jdynatags" prefix="dyna"%>

<%
	Box holder = (Box)request.getAttribute("holder");
	ComponentInfoDTO info = ((Map<String, ComponentInfoDTO>)(request.getAttribute("componentinfomap"))).get(holder.getShortName());
	
	String relationName = info.getRelationName();
		
	DiscoverResult qResults = (DiscoverResult) request.getAttribute("qResults"+info.getRelationName());
	
	List<String[]> appliedFilters = (List<String[]>) request.getAttribute("appliedFilters"+info.getRelationName());
	List<String> appliedFilterQueries = (List<String>) request.getAttribute("appliedFilterQueries"+info.getRelationName());
	Map<String, String> displayAppliedFilters = new HashMap<String, String>();
	
    String httpFilters ="";
	if (appliedFilters != null && appliedFilters.size() >0 ) 
	{
	    int idx = 1;
	    for (String[] filter : appliedFilters)
	    {
	        httpFilters += "&amp;filter_field_" + relationName + "_"+idx+"="+URLEncoder.encode(filter[0],"UTF-8");
	        httpFilters += "&amp;filter_type_" + relationName + "_"+idx+"="+URLEncoder.encode(filter[1],"UTF-8");
	        httpFilters += "&amp;filter_value_" + relationName + "_"+idx+"="+URLEncoder.encode(filter[2],"UTF-8");
	        idx++;
	    }
	}
	
	boolean globalShowFacets = false;
    boolean brefine = false;
    List<DiscoverySearchFilterFacet> facetsConf = (List<DiscoverySearchFilterFacet>) request.getAttribute("facetsConfig"+info.getRelationName());
    Map<String, Boolean> showFacets = new HashMap<String, Boolean>();
    
    for (DiscoverySearchFilterFacet facetConf : facetsConf)
    {
    	if(qResults!=null) {
    	    String f = facetConf.getIndexFieldName();
    	    List<FacetResult> facet = qResults.getFacetFieldResult(f);
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
    			    globalShowFacets = true;
    		        showFacet = true;
    		    }
				else {
					displayAppliedFilters.put(f+"::"+fvalue.getFilterType()+"::"+fvalue.getAsFilterQuery(),
							fvalue.getDisplayedValue());
				}
    	    }
    	    showFacets.put(f, showFacet);
    	    brefine = brefine || showFacet;
    	}
    }
	
%>
<div id="containerfilterApplied_${holder.shortName}" class="facetsBox">		
 
			<div class="discovery-search-appliedFilters">
			<span class="span-filter-applied-title"><fmt:message key="jsp.components.collapse.facet.panet.filter.applied" /></span>
			<%
				int idx = 1;
				for (String[] filter : appliedFilters)
				{
				    boolean showDisplay = displayAppliedFilters.containsKey(filter[0]+"::"+filter[1]+"::"+filter[2]) && !StringUtils.equalsIgnoreCase(displayAppliedFilters.get(filter[0]+"::"+filter[1]+"::"+filter[2]), filter[2]);
				    String fkey = "jsp.search.filter."+filter[0];
				    %>				    	    
				    <div class="btn btn-default">
					    <span class="span-filter-applied-bordered">				    
		  				<fmt:message key="<%= fkey %>" />:&nbsp;		  							    
					    <b><%= showDisplay?displayAppliedFilters.get(filter[0]+"::"+filter[1]+"::"+filter[2]):filter[2] %></b>
		  					<a class="a-filter-applied-remove" href="?open=<%=info.getType()							
					                + httpFilters
					                + "&amp;submit_filter_remove_"+ relationName + "_" + idx +"="+Utils.addEntities(filter[2]) %>#${holder.shortName}"><i class="remove fa fa-times"></i></a> 
					    
					    </span>
				    </div>
					<%
					idx++;
				}
			%>
			<hr>
			</div>
				    
</div>