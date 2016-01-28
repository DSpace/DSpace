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
<%@ page import="org.dspace.discovery.configuration.DiscoverySearchFilterFacet"%>
<%@ page import="org.dspace.discovery.configuration.TagCloudConfiguration"%>
<%@ page import="java.util.HashMap"%>
<%@ page import="java.util.Set"%>
<%@ page import="java.util.Map"%>
<%@ page import="java.util.List"%>
<%@ page import="org.dspace.discovery.DiscoverResult.FacetResult"%>

<%
		Map<String, List<FacetResult>> tcMapFacetes = (Map<String, List<FacetResult>>) request.getAttribute("tagcloud.fresults");
		List<DiscoverySearchFilterFacet> tcFacetsConf = (List<DiscoverySearchFilterFacet>) request.getAttribute("tagCloudFacetsConfig");
		TagCloudConfiguration tagCloudConfiguration = (TagCloudConfiguration) request.getAttribute("tagCloudConfig");
		String tcSearchScope = (String) request.getAttribute("tagcloud.searchScope");
		
    	String scope = tcSearchScope;
    	if (tcMapFacetes!=null) {
    		for (DiscoverySearchFilterFacet facetConf : tcFacetsConf)
    		{
    			Map<String, Integer> data = new HashMap<String, Integer>();
    			String index = facetConf.getIndexFieldName();
    			
        		List<FacetResult> facet = tcMapFacetes.get(index);
        		if (facet!=null){
        			for (FacetResult fvalue : facet)
    		   		{ 
        				data.put(fvalue.getDisplayedValue(), (int)fvalue.getCount());
    				}
        		}
    %>
    			<div>
    				<dspace:tagcloud parameters='<%= tagCloudConfiguration %>' index='<%= index %>' scope='<%= scope %>' data='<%= data %>'/><br/><br/>
    			</div>
    <%		
    		}
    	}
	%>