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
		
			jQuery("#group-left-info").popover({
			   	trigger: 'click',
			   	html: true,
				title: function(){
					return jQuery('#group-left-info-popover-head').html();
				},
				content: function(){
					return jQuery('#group-left-info-popover-content').html();
				}
			});
			
			jQuery('#group-left-info').on('click',function(){
				jQuery('#group-center-info').popover('hide');
				jQuery('#group-right-info').popover('hide');
			});
			
			jQuery('#group-center-info').popover({
			   	trigger: 'click',
			   	html: true,
				title: function(){
					return jQuery('#group-center-info-popover-head').html();
				},
				content: function(){
					return jQuery('#group-center-info-popover-content').html();
				}
			});
			jQuery('#group-center-info').on('click',function(){
				jQuery('#group-left-info').popover('hide');
				jQuery('#group-right-info').popover('hide');
			});
			
			jQuery('#group-right-info').popover({
			   	trigger: 'click',
			   	html: true,
				title: function(){
					return jQuery('#group-right-info-popover-head').html();
				},
				content: function(){
					return jQuery('#group-right-info-popover-content').html();
				}
			});
			jQuery('#group-right-info').on('click',function(){
				jQuery('#group-left-info').popover('hide');
				jQuery('#group-center-info').popover('hide');
			});
			

			
	});	
-->
</script>
<%

	String facetGlobalName = (String) request.getAttribute("facetGlobalName");
	
	Map<String, String> mapFacetFirstLevel = (Map<String, String>) request.getAttribute("facetGlobalFirstLevel");
	Map<String, String> mapFacetSecondLevel = (Map<String, String>) request.getAttribute("facetGlobalSecondLevel");
	
%>
		
		
<hr/>
<div class="row">
<form id="searchglobalprocessor" name="searchglobalprocessor" class="col-md-10 col-md-offset-1" action="<%= request.getContextPath() %>/simple-search" method="get">
<div class="input-group">
    <input type="text" class="form-control" name="query" placeholder="Search term...">
    <span class="input-group-btn">
        <button class="btn btn-primary" type="submit"><i class="fa fa-search"></i></button>
    </span>    
    <div class="input-group-btn search-panel">
        <button type="button" class="btn btn-default dropdown-toggle" data-toggle="dropdown">
        	<span id="search_concept">All</span>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; <b><span class="caret"></b>
        </button>
        <ul class="dropdown-menu menu-global-processor" role="menu">
        
        <%
		if(facetsConf!=null) {
			for (DiscoverySearchFilterFacet facetConf : facetsConf)
			{
		    	String f = facetConf.getIndexFieldName();
		    	if(f.equals(facetGlobalName)) {
		    	List<FacetResult> facet = mapFacetes.get(f);
		    	
		    	if(facet!=null) {
				  	for (FacetResult fvalue : facet)
			    	{ 
		        %>

					<li class="menu-global-processor">
						<a href="#"
						title="<%=fvalue.getAuthorityKey()%>"><span class="badge pull-right"><%=fvalue.getCount()%></span> <%=StringUtils.abbreviate(fvalue.getDisplayedValue(), 36)%> &nbsp;&nbsp;&nbsp; <span class="badge invisible"><%=fvalue.getCount()%></span> </a></li>
					<%
			    	}
		    	}	    		    
				}
			}
		}
		%>
        
        
        </ul>
    </div>
    <input type="hidden" name="location" value="global" id="search_param">         
</div>
</form>
</div>
<hr/>
