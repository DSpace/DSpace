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

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>
<%@ taglib uri="jdynatags" prefix="dyna"%>

<c:set var="root"><%=request.getContextPath()%></c:set>
<c:set var="info" value="${componentinfomap}" scope="page" />
<%
	
	Box holder = (Box)request.getAttribute("holder");
	ComponentInfoDTO info = ((Map<String, ComponentInfoDTO>)(request.getAttribute("componentinfomap"))).get(holder.getShortName());
	List<String[]> subLinks = (List<String[]>) request.getAttribute("activeTypes"+info.getRelationName());
	
	DiscoverResult qResults = (DiscoverResult) request.getAttribute("qResults"+info.getRelationName());
	List<String> appliedFilterQueries = (List<String>) request.getAttribute("appliedFilterQueries"+info.getRelationName());
	List<String[]> appliedFilters = (List<String[]>) request.getAttribute("appliedFilters"+info.getRelationName());
	
    String httpFilters ="";
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
	
	if (info.getItems().length > 0) {
%>

<c:set var="info" value="<%= info %>" scope="request" />

<% if(subLinks!=null && subLinks.size()>0) {

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
    		        showFacet = true;
    		        break;
    		    }
    	    }
    	    showFacets.put(f, showFacet);
    	    brefine = brefine || showFacet;
    	}
    }
	%>
	
	<div id="collapseFacet" class="collapse">
		<div class="panel panel-default">
		<a href="#" data-toggle="collapse" data-target="#collapseFacet" aria-expanded="false" aria-controls="collapseFacet">
			<span class="pull-right">
	  			<i class="fa fa-times"></i>
	  		</span>
			</a>		
		<div class="panel-body">
		<div id="containerFacetSubtype" class="facetsBox">
			<%  if(subLinks.size()>1) {%>
		    <div id="facetSubType" class="panel panel-primary">
			    <div class="panel-heading"><fmt:message key="jsp.components.button.seealso" /></div>
				    <ul class="list-group"><%
					for (String[] sub : subLinks )
				    { 
				    %>
				        <li class="list-group-item <%= (info.getType().equals(sub[0]))?"active":"" %>"><span class="badge"><%= sub[2] %></span> <a href="?open=<%= sub[0] %>"><%= StringUtils.abbreviate(sub[1],36) %></a></li>
			        <%
				    }
				    %></ul>
		    </div>	    
		    <% } %>	    
		    <% if (appliedFilters.size() > 0 ) { %> 
		    <hr>                               
			<div class="discovery-search-appliedFilters">
			<span><fmt:message key="jsp.search.filter.applied" /></span>
			<br/>
			<%
				int idx = 1;
				for (String[] filter : appliedFilters)
				{
				    %>			    
				    <% if(idx%2==0) { %>
				    <span class="tag label label-info">
				    <% } else { %>
				    <span class="tag label label-default">
				    <% } %>
	  					<span><%= Utils.addEntities(filter[0]) %>::
				    <%= Utils.addEntities(filter[1]) %>::
				    <%= Utils.addEntities(filter[2]) %></span>
	  					<a href="?open=<%=info.getType()							
				                + httpFilters
				                + "&amp;submit_filter_remove_"+ idx +"="+Utils.addEntities(filter[2]) %>"><i class="remove fa fa-times"></i></a> 
				    
				    </span>
					<%
					idx++;
				}
			%>
			<hr>
			</div>
			<% } %>	    
	    </div>
	    <div class="panel-group" id="facets" class="facetsBox" role="tablist">
			<%
				for (DiscoverySearchFilterFacet facetConf : facetsConf)
				{
				    String f = facetConf.getIndexFieldName();
				    if (!showFacets.get(f))
				        continue;
				    List<FacetResult> facet = qResults.getFacetFieldResult(f);
				    if (facet.size() == 0)
				    {
				        facet = qResults.getFacetResult(f+".year");
				    }
				    int limit = facetConf.getFacetLimit()+1;
				    
				    String fkey = "jsp.search.facet.refine."+f;
				    %>
				    <div id="facet_<%= f %>" class="panel panel-default">
						    <div class="panel-heading" role="tab" id="heading<%= f %>">
						    <h4 class="panel-title">
	          					<fmt:message key="<%= fkey %>" />
		        			</h4>
						    </div>
		      				<div class="panel-body">
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
						        %><li class="list-group-item"><span class="badge"><%= fvalue.getCount() %></span> <a href="?open=<%=info.getType()							
					                + httpFilters
					                + "&amp;filtername="+URLEncoder.encode(f,"UTF-8")
					                + "&amp;filterquery="+URLEncoder.encode(fvalue.getAsFilterQuery(),"UTF-8")
					                + "&amp;filtertype="+URLEncoder.encode(fvalue.getFilterType(),"UTF-8") %>"
					                title="<fmt:message key="jsp.search.facet.narrow"><fmt:param><%=fvalue.getDisplayedValue() %></fmt:param></fmt:message>">
					                <%= fvalue.getDisplayedValue() %></a></li><%
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
						        <a class="pull-left" href="?open=<%=info.getType() + httpFilters
					                + "&amp;"+f+"_page="+(currFp-1) %>"><fmt:message key="jsp.search.facet.refine.previous" /></a>
					            <% } %>
					            <% if (idx == limit) { %>
					            <a href="?open=<%=info.getType() + httpFilters
					                + "&amp;"+f+"_page="+(currFp+1) %>"><span class="pull-right"><fmt:message key="jsp.search.facet.refine.next" /></span></a>
					            <%
					            }
					            %></li><%
						    }
						    %></ul>
					    
							</div>
					</div><%
				}
			
			%>
			</div>
			</div>
	    </div>
	</div>
<% } %>	

<div class="panel-group" id="${holder.shortName}">
	<div class="panel panel-default col-md-12">
    	<div class="panel-heading">
    		<h4 class="panel-title">
        		<a data-toggle="collapse" data-parent="#${holder.shortName}" href="#collapseOne${holder.shortName}">
          			${holder.title} <fmt:message
				key="jsp.layout.dspace.detail.fieldset-legend.component.boxtitle.${info[holder.shortName].type}"/>
        		</a></h4>
        		<% if(subLinks!=null && subLinks.size()>0) {%>
					<button class="btn btn-default" type="button" data-toggle="collapse" data-target="#collapseFacet" aria-expanded="false" aria-controls="collapseFacet" title="<fmt:message key="jsp.components.button.seealso.button" />">
  						<i class="fa fa-angle-double-up animated"></i>
					</button>
				<% } %>	
    	</div>
	<div id="collapseOne${holder.shortName}" class="panel-collapse collapse in">
		<div class="panel-body">	
	<p>


<!-- prepare pagination controls -->
<%
    // create the URLs accessing the previous and next search result pages
    StringBuilder sb = new StringBuilder();
	sb.append("<div class=\"block text-center\"><ul class=\"pagination\">");
	
    String prevURL = info.buildPrevURL(); 
    String nextURL = info.buildNextURL();


if (info.getPagefirst() != info.getPagecurrent()) {
	  sb.append("<li><a class=\"\" href=\"");
	  sb.append(prevURL);
	  sb.append("\"><i class=\"fa fa-long-arrow-left\"> </i></a></li>");
}

for( int q = info.getPagefirst(); q <= info.getPagelast(); q++ )
{
   	String myLink = info.buildMyLink(q);
    sb.append("<li");
    if (q == info.getPagecurrent()) {
    	sb.append(" class=\"active\"");	
    }
    sb.append("> " + myLink+"</li>");
} // for


if (info.getPagetotal() > info.getPagecurrent()) {
  sb.append("<li><a class=\"\" href=\"");
  sb.append(nextURL);
  sb.append("\"><i class=\"fa fa-long-arrow-right\"> </i></a></li>");
}

sb.append("</ul></div>");

%>


<div align="center" class="browse_range">

	<p align="center"><fmt:message key="jsp.search.results.results">
        <fmt:param><%=info.getStart()+1%></fmt:param>
        <fmt:param><%=info.getStart()+info.getItems().length%></fmt:param>
        <fmt:param><%=info.getTotal()%></fmt:param>
        <fmt:param><%=(float)info.getSearchTime() / 1000%></fmt:param>
    </fmt:message></p>

</div>
<%
if (info.getPagetotal() > 1)
{
%>
<%= sb %>
<%
	}
%>
			
<form id="sortform<%= info.getType() %>" action="#<%= info.getType() %>" method="get">
	   <input id="sort_by<%= info.getType() %>" type="hidden" name="sort_by<%= info.getType() %>" value=""/>
       <input id="order<%= info.getType() %>" type="hidden" name="order<%= info.getType() %>" value="<%= info.getOrder() %>" />
       <% if (appliedFilters != null && appliedFilters.size() >0 ) 
   		{
	   	    int idx = 1;
	   	    for (String[] filter : appliedFilters)
	   	    { %>
	   	    	<input id="filter_field_<%= idx %>" type="hidden" name="filter_field_<%= idx %>" value="<%= filter[0]%>"/>
	   	    	<input id="filter_type_<%= idx %>" type="hidden" name="filter_type_<%= idx %>" value="<%= filter[1]%>"/>
	   	    	<input id="filter_value_<%= idx %>" type="hidden" name="filter_value_<%= idx %>" value="<%= filter[2] %>"/>
	   	      <%  
	   	        idx++;
	   	    }
   		} %>
	   <input type="hidden" name="open" value="<%= info.getType() %>" />
</form>
<div class="row">
<div class="table-responsive">
<dspace:itemlist itemStart="<%=info.getStart()+1%>" items="<%= (Item[])info.getItems() %>" sortOption="<%= info.getSo() %>" authorLimit="<%= info.getEtAl() %>" order="<%= info.getOrder() %>" config="${info[holder.shortName].type}" />
</div>
</div>
<script type="text/javascript"><!--
    function sortBy(sort_by, order) {
        j('#sort_by<%= info.getType() %>').val(sort_by);
        j('#order<%= info.getType() %>').val(order);
        j('#sortform<%= info.getType() %>').submit();        
    }
--></script>

<%-- show pagination controls at bottom --%>
<%
	if (info.getPagetotal() > 1)
	{
%>
<%= sb %>
<%
	}
%>


</p>
</div>
										  </div>
								   </div>
							</div>

<% } %>
