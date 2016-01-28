<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%--
  - fragment JSP to be included in single browse gsp pages
  -
  --%>

<%@page import="org.dspace.content.DSpaceObject"%>
<%@ page import="java.net.URLEncoder"%>
<%@ page import="org.apache.commons.lang.StringUtils"%>
<%@ page import="java.util.HashMap"%>
<%@ page import="java.util.Map"%>
<%@ page import="org.dspace.browse.BrowseInfo" %>
<%@ page import="org.dspace.browse.BrowseIndex" %>
<%@ page import="org.dspace.core.Utils" %>
<%@ page import="org.dspace.discovery.configuration.TagCloudConfiguration"%>

<%
		BrowseInfo bi2 = (BrowseInfo) request.getAttribute("browse.info");
		BrowseIndex bix2 = bi2.getBrowseIndex();
		TagCloudConfiguration tagCloudConfiguration = (TagCloudConfiguration) request.getAttribute("tagCloudConfig");
		
		String type2 = "1";
		String index = bix2.getName();
		String scope2 = "";
		DSpaceObject dso = bi2.getBrowseContainer();
		if (dso!=null){
			scope2 = "/handle/" + dso.getHandle();
		}
		
		Map<String, Integer> data = new HashMap<String, Integer>();
		
		String[][] results2 = bi2.getStringResults();

	    for (int i = 0; i < results2.length; i++)
	    {
	    	String value = Utils.addEntities(results2[i][0]);
	    	int count = Integer.parseInt(results2[i][2]);
			data.put(value, count);
	    }
    %>
    			<div>
    				<dspace:tagcloud parameters='<%= tagCloudConfiguration %>' index='<%= index %>' scope='<%= scope2 %>' data='<%= data %>' type='<%= type2 %>'/><br/><br/>
    			</div>
    <%		
	    	
	%>