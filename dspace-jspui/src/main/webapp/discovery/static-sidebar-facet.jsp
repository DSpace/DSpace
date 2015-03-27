<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%--
  - fragment JSP to be included in site, community or collection home to show discovery facets
  -
  - Attributes required:
  -    discovery.fresults    - the facets result to show
  -    discovery.facetsConf  - the facets configuration
  -    discovery.searchScope - the search scope 
  --%>

<%@page import="org.dspace.discovery.configuration.DiscoverySearchFilterFacet"%>
<%@ page import="java.util.HashMap"%>
<%@ page import="java.util.Set"%>
<%@ page import="java.util.Map"%>
<%@ page import="org.dspace.discovery.DiscoverResult.FacetResult"%>
<%@ page import="java.util.List"%>
<%@ page import="java.net.URLEncoder"%>
<%@ page import="org.apache.commons.lang.StringUtils"%>
<script type="text/javascript">
<!--
	jQuery(document).ready(function(){
		
			jQuery("#searchglobalprocessor .search-panel .dropdown-menu li a").click(function(){
				  jQuery('#search_param').val('');  
				  jQuery('#search_param').val(jQuery(this).attr('title'));
				  return jQuery('#searchglobalprocessor').submit();
			});
		
			jQuery("#rp-info").click(function(){
				  jQuery('#search_param').val('');  
				  jQuery('#search_param').val(jQuery(this).attr('data-id'));
				  return jQuery('#searchglobalprocessor').submit();
			});
			
			jQuery('#rp-info').on('click',function(){
				jQuery('#others-info').popover('hide');
				jQuery('#publications-info').popover('hide');
			});
			
			jQuery('#others-info').popover({
			   	trigger: 'click',
			   	html: true,
				title: function(){
					return jQuery('#others-info-popover-head').html();
				},
				content: function(){
					return jQuery('#others-info-popover-content').html();
				}
			});
			jQuery('#others-info').on('click',function(){
				jQuery('#rp-info').popover('hide');
				jQuery('#publications-info').popover('hide');
			});
			
			jQuery('#publications-info').popover({
			   	trigger: 'click',
			   	html: true,
				title: function(){
					return jQuery('#publications-info-popover-head').html();
				},
				content: function(){
					return jQuery('#publications-info-popover-content').html();
				}
			});
			jQuery('#publications-info').on('click',function(){
				jQuery('#rp-info').popover('hide');
				jQuery('#others-info').popover('hide');
			});
			

			
	});	
-->
</script>
<%
	boolean brefine = false;
	
	Map<String, List<FacetResult>> mapFacetes = (Map<String, List<FacetResult>>) request.getAttribute("discovery.fresults");
	List<DiscoverySearchFilterFacet> facetsConf = (List<DiscoverySearchFilterFacet>) request.getAttribute("facetsConfig");
	String searchScope = (String) request.getAttribute("discovery.searchScope");
	String processor = (String) request.getAttribute("processor");
	
	if("global".equals(processor)) { %>
		
		
<hr/>
<div class="row">
<form id="searchglobalprocessor" name="searchglobalprocessor" class="col-md-10 col-md-offset-1" action="/jspui/simple-search" method="get">
<div class="input-group">
    <input type="text" class="form-control" name="query" placeholder="Search term...">
    <span class="input-group-btn">
        <button class="btn btn-primary" type="submit">Search <span class="fa fa-search"></span></button>
    </span>    
    <div class="input-group-btn search-panel">
        <button type="button" class="btn btn-default dropdown-toggle" data-toggle="dropdown">
        	<span id="search_concept">All</span> <span class="caret"></span>
        </button>
        <ul class="dropdown-menu menu-global-processor" role="menu">
        
        <%
		if(facetsConf!=null) {
			for (DiscoverySearchFilterFacet facetConf : facetsConf)
			{
		    	String f = facetConf.getIndexFieldName();
		    	List<FacetResult> facet = mapFacetes.get(f);
		    	
			    for (FacetResult fvalue : facet)
		    	{ 
		        %>

					<li class="menu-global-processor">
						<a href="#"
						title="<%=fvalue.getDisplayedValue()%>"><%=StringUtils.abbreviate(fvalue.getDisplayedValue(), 36)%><span class="badge"><%=fvalue.getCount()%></span></a></li>

					<%
		    	}
		    		    
		    	
			}
		}
		%>
        
        
        </ul>
    </div>
    <input type="hidden" name="location" value="global" id="search_param">         

</div>
<hr/>


<%
	long totPeople = 0;
		long totActivities = 0;
		long totPublications = 0;
		if (facetsConf != null) {
			for (DiscoverySearchFilterFacet facetConf : facetsConf) {
				String f = facetConf.getIndexFieldName();
				List<FacetResult> facet = mapFacetes.get(f);
				for (FacetResult ft : facet) {
					if (StringUtils.startsWith(ft.getAuthorityKey(), "dspace")) {
						totPublications += ft.getCount();
					} else if (StringUtils.equalsIgnoreCase("crisrp", ft.getAuthorityKey())) {
						totPeople += ft.getCount();
					} else {
						totActivities += ft.getCount();
					}
				}
			}
		}
%>
<div class="row">
	<a href="#rp-info"><div class="col-md-4 text-center">
		<h4 class="text-success">People</h4>
		<p><span class="fa fa-users fa-5x" id="rp-info" data-placement="right" data-id="crisrp"></span> <span class="badge"><%= totPeople %></span></p>
		<small class="label label-success"><fmt:message key="jsp.home.explore.rp" /></small>
	</div></a>
	
	<a href="#others-info-popover-content"><div class="col-md-4 text-center">
		<h4 class="text-success">Others</h4>
		<p><span class="fa fa-cogs fa-5x" id="others-info"  data-placement="bottom"></span> <span class="badge"><%= totActivities%></span></p>
		<small class="label label-success"><fmt:message key="jsp.home.explore.other" /></small>
	</div></a>
	
	<a href="#publications-info-popover-content"><div class="col-md-4 text-center">
		<h4 class="text-success">Publications <small>articles, reports, etc.</small></h4>
		<p><span class="fa fa-file-text-o fa-5x" id="publications-info"  data-placement="left"></span> <span class="badge"><%= totPublications %></span></p>
		<small class="label label-success"><fmt:message key="jsp.home.explore.publications" /></small>
	</div></a>
</div>


<hr/>

<div id="others-info-popover-head" class="hide">
	<fmt:message key="jsp.home.othersinfo.head"/>
</div>
<div id="publications-info-popover-head" class="hide">
	<fmt:message key="jsp.home.publicationsinfo.head"/>
</div>
<div id="others-info-popover-content" class="hide">
	<ul class="list-group">
        <%
		if(facetsConf!=null) {
			for (DiscoverySearchFilterFacet facetConf : facetsConf)
			{
		    	String f = facetConf.getIndexFieldName();
		    	List<FacetResult> facet = mapFacetes.get(f);
		    	
			    for (FacetResult fvalue : facet)
		    	{ 
			    	if(StringUtils.startsWith(fvalue.getAuthorityKey(), "cris") && !StringUtils.equalsIgnoreCase("crisrp", fvalue.getAuthorityKey())) {
			    		String fkey =  "jsp.home.othersinfo."+fvalue.getAuthorityKey();
		        %>

				<li class="list-group-item"> <span class="badge"><%= fvalue.getCount() %></span> <a href="<%= request.getContextPath() %>/simple-search?query=&location=<%=fvalue.getAuthorityKey()%>"><fmt:message key="<%= fkey %>"/></a></li>
		<%
			    	}
		    	}
		    		    
		    	
			}
		}
		%>
		</ul>
</div>		

<div id="publications-info-popover-content" class="hide">
	<ul class="list-group">
        <%
		if(facetsConf!=null) {
			for (DiscoverySearchFilterFacet facetConf : facetsConf)
			{
		    	String f = facetConf.getIndexFieldName();
		    	List<FacetResult> facet = mapFacetes.get(f);
		    	
			    for (FacetResult fvalue : facet)
		    	{ 
			    	if(StringUtils.startsWith(fvalue.getAuthorityKey(), "dspace")) {
			    		String fkey =  "jsp.home.publicationsinfo."+fvalue.getAuthorityKey();
		        %>

				<li class="list-group-item"> <span class="badge"><%= fvalue.getCount() %></span> <a href="<%= request.getContextPath() %>/simple-search?query=&location=<%=fvalue.getAuthorityKey()%>"><fmt:message key="<%= fkey %>"/></a></li>
		<%
			    	}
		    	}
		    		    
		    	
			}
		}
		%>
		</ul>
</div>		

</form>
</div>


<%	    
	} else {

	if (searchScope == null)
	{
	    searchScope = "";
	}
	
	if (mapFacetes != null)
	{
	    for (DiscoverySearchFilterFacet facetConf : facetsConf)
		{
		    String f = facetConf.getIndexFieldName();
		    List<FacetResult> facet = mapFacetes.get(f);
		    if (facet != null && facet.size() > 0)
		    {
		        brefine = true;
		        break;
		    }
		    else
		    {
		        facet = mapFacetes.get(f+".year");
			    if (facet != null && facet.size() > 0)
			    {
			        brefine = true;
			        break;
			    }
		    }
		}
	}
	if (brefine) {
%>
<div class="col-md-<%= discovery_panel_cols %>">
<h3 class="facets"><fmt:message key="jsp.search.facet.refine" /></h3>
<div id="facets" class="facetsBox row panel">
<%
	for (DiscoverySearchFilterFacet facetConf : facetsConf)
	{
    	String f = facetConf.getIndexFieldName();
    	List<FacetResult> facet = mapFacetes.get(f);
 	    if (facet == null)
 	    {
 	        facet = mapFacetes.get(f+".year");
 	    }
 	    if (facet == null)
 	    {
 	        continue;
 	    }
	    String fkey = "jsp.search.facet.refine."+f;
	    int limit = facetConf.getFacetLimit()+1;
	    %><div id="facet_<%= f %>" class="facet col-md-<%= discovery_facet_cols %>">
	    <span class="facetName"><fmt:message key="<%= fkey %>" /></span>
	    <ul class="list-group"><%
	    int idx = 1;
	    int currFp = UIUtil.getIntParameter(request, f+"_page");
	    if (currFp < 0)
	    {
	        currFp = 0;
	    }
	    if (facet != null)
	    {
		    for (FacetResult fvalue : facet)
		    { 
		        if (idx != limit)
		        {
		        %><li class="list-group-item"><span class="badge"><%= fvalue.getCount() %></span> <a href="<%= request.getContextPath()
		            + searchScope
	                + "/simple-search?filterquery="+URLEncoder.encode(fvalue.getAsFilterQuery(),"UTF-8")
	                + "&amp;filtername="+URLEncoder.encode(f,"UTF-8")
	                + "&amp;filtertype="+URLEncoder.encode(fvalue.getFilterType(),"UTF-8") %>"
	                title="<fmt:message key="jsp.search.facet.narrow"><fmt:param><%=fvalue.getDisplayedValue() %></fmt:param></fmt:message>">
	                <%= StringUtils.abbreviate(fvalue.getDisplayedValue(),36) %></a></li><%
		        }
		        idx++;
		    }
		    if (currFp > 0 || idx > limit)
		    {
		        %><li class="list-group-item"><span style="visibility: hidden;">.</span>
		        <% if (currFp > 0) { %>
		        <a class="pull-left" href="<%= request.getContextPath()
		                + searchScope
		                + "?"+f+"_page="+(currFp-1) %>"><fmt:message key="jsp.search.facet.refine.previous" /></a>
	            <% } %>
	            <% if (idx > limit) { %>
	            <a href="<%= request.getContextPath()
		            + searchScope
	                + "?"+f+"_page="+(currFp+1) %>"><span class="pull-right"><fmt:message key="jsp.search.facet.refine.next" /></span></a>
	            <%
	            }
	            %></li><%
		    }
	    }
	    %></ul></div><%
	}
%></div></div><%
	}
	
}
%>