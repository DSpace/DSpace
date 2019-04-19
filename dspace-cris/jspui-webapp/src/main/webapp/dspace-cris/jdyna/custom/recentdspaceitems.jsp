<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    https://github.com/CILEA/dspace-cris/wiki/License

--%>
<%@page import="org.apache.commons.lang.StringUtils"%>
<%@page import="java.util.List"%>
<%@ page import="org.apache.commons.lang.StringEscapeUtils" %>
<%@ page import="java.net.URLEncoder"            %>
<%@ page import="org.dspace.sort.SortOption" %>
<%@ page import="java.util.Enumeration" %>
<%@ page import="java.util.Set" %>
<%@ page import="java.util.Map" %>
<%@page import="org.dspace.eperson.EPerson"%>
<%@ page import="org.dspace.content.Item"        %>
<%@page import="org.dspace.app.webui.cris.dto.ComponentInfoDTO"%>
<%@page import="it.cilea.osd.jdyna.web.Box"%>
<%@page import="org.dspace.discovery.IGlobalSearchResult"%>
<%@page import="org.dspace.discovery.configuration.DiscoveryViewConfiguration"%>
<%@page import="org.dspace.discovery.SearchUtils"%>
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
	List<String[]> subLinks = (List<String[]>) request
            .getAttribute("activeTypes"+info.getRelationName());
    
	DiscoveryViewConfiguration configuration = SearchUtils.getRecentSubmissionConfiguration("site").getMetadataFields();
	if (info.getItems().length > 0) {
%>
<c:set var="info" value="<%= info %>" scope="request" />
<div class="panel-group ${extraCSS}" id="${holder.shortName}">
<c:set var="extraCSS">
	<c:choose>
		<c:when test="${holder.priority % 10 == 2}">col-md-6</c:when>
		<c:otherwise>col-md-12</c:otherwise>
	</c:choose>
</c:set>
	<div class="panel panel-default vertical-carousel" data-itemstoshow="3">
    	<div class="panel-heading">
    		<h4 class="panel-title">
        		<a data-toggle="collapse" data-parent="#${holder.shortName}" href="#collapseOne${holder.shortName}">
          			${holder.title}</a></h4>
        		<% if(subLinks!=null && subLinks.size()>1) {%>
        		<div class="btn-group">
			    <button type="button" class="btn btn-sm btn-default dropdown-toggle" data-toggle="dropdown">
    				<fmt:message key="jsp.components.button.seealso"/> <span class="fa fa-caret-down"></span>	
  				</button>
				<ul class="dropdown-menu dropdown-menu-right" role="menu">
				<% for (String[] sub : subLinks ) { %>
					<li><a href="?open=<%= sub[0] %>"><%= sub[1] %></a></li>
				<% } %>
				</ul>
				</div>
				<% } %>	
    	</div>
	<div id="collapseOne${holder.shortName}" class="panel-collapse collapse<c:if test="${holder.collapsed==false}"> in</c:if>">
		<div class="list-groups">
	<%	
		for (IGlobalSearchResult obj : info.getItems()) {
		%>
		
				<dspace:discovery-artifact style="global" artifact="<%= obj %>" view="<%= configuration %>" />
		
		<%
		     }
		%>
		</div>
		  </div>
     </div>
 </div>    

<% } %>
