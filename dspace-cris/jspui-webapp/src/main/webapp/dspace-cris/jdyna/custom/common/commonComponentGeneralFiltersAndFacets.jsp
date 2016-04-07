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

<c:set var="info" value="${componentinfomap}" scope="page" />
<%
	Box holder = (Box)request.getAttribute("holder");
	ComponentInfoDTO info = ((Map<String, ComponentInfoDTO>)(request.getAttribute("componentinfomap"))).get(holder.getShortName());
	List<String[]> subLinks = (List<String[]>) request.getAttribute("activeTypes"+info.getRelationName());
%>

<div class="btn-group" style="margin-top: -5px;">
	<%  if(subLinks.size()>1) {%>
	<button type="button" class="btn btn-link dropdown-toggle"
		style="text-decoration: none;" data-toggle="dropdown">
		<fmt:message
			key="jsp.layout.dspace.detail.fieldset-legend.component.boxtitle.${info[holder.shortName].type}">
			<fmt:param>
				<span class="fa fa-caret-down"></span>
			</fmt:param>
		</fmt:message>
	</button>
	<ul class="dropdown-menu dropdown-menu-right" role="menu">
		<% for (String[] sub : subLinks ) { %>
		<li><a href="?open=<%= sub[0] %>#${holder.shortName}"><%= sub[1] %></a></li>
		<% } %>
	</ul>
	<% } else { %>
		<fmt:message
		key="jsp.layout.dspace.detail.fieldset-legend.component.boxtitle.${info[holder.shortName].type}">
			<fmt:param>							
			</fmt:param>
		</fmt:message>
	<% } %>

</div>
<button class="btn btn-default pull-right" style="margin-top: -7px;" type="button"
	data-toggle="collapse" data-target="#collapseFacet_${holder.shortName}"
	aria-expanded="false" aria-controls="collapseFacet"
	title="<fmt:message key="jsp.components.button.seealso.button" />">
	<fmt:message key="jsp.components.button.seealso.open.facet" />
	&nbsp;<i class="fa fa-angle-double-down"></i>
</button>
