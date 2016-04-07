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
	
	DiscoverResult qResults = (DiscoverResult) request.getAttribute("qResults"+relationName);
	
	List<String[]> appliedFilters = (List<String[]>) request.getAttribute("appliedFilters"+relationName);
	List<String> appliedFilterQueries = (List<String>) request.getAttribute("appliedFilterQueries"+relationName);
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
    List<DiscoverySearchFilterFacet> facetsConf = (List<DiscoverySearchFilterFacet>) request.getAttribute("facetsConfig"+relationName);
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

	<div id="collapseFacet_${holder.shortName}" class="collapse">
		
	    <div class="panel-group" id="facets_${holder.shortName}" class="facetsBox" role="tablist">
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
				    <div class="col-sm-6">
				    <div id="${holder.shortName}_facet_<%= f %>" class="panel panel-default">
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
					                + "&amp;filtername" + relationName + "="+URLEncoder.encode(f,"UTF-8")
					                + "&amp;filterquery" + relationName + "="+URLEncoder.encode(fvalue.getAsFilterQuery(),"UTF-8")
					                + "&amp;filtertype" + relationName + "="+URLEncoder.encode(fvalue.getFilterType(),"UTF-8") %>#${holder.shortName}"
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
					                + "&amp;"+f+"_page="+(currFp-1) %>#${holder.shortName}"><fmt:message key="jsp.search.facet.refine.previous" /></a>
					            <% } %>
					            <% if (idx == limit) { %>
					            <a href="?open=<%=info.getType() + httpFilters
					                + "&amp;"+f+"_page="+(currFp+1) %>#${holder.shortName}"><span class="pull-right"><fmt:message key="jsp.search.facet.refine.next" /></span></a>
					            <%
					            }
					            %></li><%
						    }
						    %></ul>
					    
							</div>
					</div></div>
					<%
				}
			
			%>
			</div>
			<div class="clearfix"></div>
			<br/>
			<div class="row text-center">
					<button class="btn btn-default col-xs-12" type="button" data-toggle="collapse" data-target="#collapseFacet_${holder.shortName}" aria-expanded="false" aria-controls="collapseFacet" title="<fmt:message key="jsp.components.button.seealso.button" />">
  						<fmt:message key="jsp.components.button.seealso.close.facet"/>&nbsp;<i class="fa fa-angle-double-up"></i>
					</button>
			</div>
			<hr/>
	</div>
	