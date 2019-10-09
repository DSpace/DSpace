
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>


<%@page import="org.dspace.content.service.CollectionService"%>
<%@page import="org.dspace.content.factory.ContentServiceFactory"%>
<%@page import="org.dspace.content.service.CommunityService"%>
<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%@ page import="org.apache.commons.lang.StringUtils" %>
<%@ page import="org.dspace.app.webui.components.RecentSubmissions" %>

<%@ page import="org.dspace.app.webui.servlet.admin.EditCommunitiesServlet" %>
<%@ page import="org.dspace.app.webui.util.UIUtil" %>
<%@ page import="org.dspace.browse.BrowseIndex" %>
<%@ page import="org.dspace.browse.ItemCounter" %>
<%@ page import="org.dspace.content.*" %>
<%@ page import="org.dspace.core.Utils" %>
<%@ page import="org.dspace.services.ConfigurationService" %>
<%@ page import="org.dspace.services.factory.DSpaceServicesFactory" %>
<%@ page import="javax.servlet.jsp.jstl.fmt.LocaleSupport" %>

<%
    int discovery_panel_cols = 12;
    int discovery_facet_cols = 4;
    BrowseIndex[] bis = BrowseIndex.getBrowseIndices();
%>
<dspace:layout title="${title}">
    <div class="panel panel-default">
        <div class="panel-body">
            <h2>${title} [${itemCount}] </h2>
        </div>
    </div>
    <div class="panel panel-primary">
        <div class="panel-heading"><fmt:message key="jsp.general.browse"/></div>
        <div class="panel-body">
                <%-- Insert the dynamic list of browse options --%>
            <%
                for (int i = 0; i < bis.length; i++)
                {
                    String key = "browse.menu." + bis[i].getName();
            %>
            <form method="get" action="<%= request.getContextPath() %>/handle/${handle}/browse">
                <input type="hidden" name="type" value="<%= bis[i].getName() %>"/>
                    <%-- <input type="hidden" name="community" value="<%= community.getHandle() %>" /> --%>
                <input class="btn btn-default col-md-3" type="submit" name="submit_browse" value="<fmt:message key="<%= key %>"/>"/>
            </form>
            <%
                }
            %>

        </div>
    </div>

    <div class="row">
        <%@ include file="/discovery/static-sidebar-facet.jsp" %>
    </div>

    <div class="row">
        <div class=" col-md-6">
            <h3><fmt:message key="jsp.community-home.heading3"/></h3>
            <ul class="list-group">
                <c:forEach items="${subCommunities}" var="community">
                    <li class="list-group-item"><a href = "${community.handle}">${community.title} </a><span class="badge">${community.itemCount}</span></li>
                </c:forEach>
            </ul>
        </div>

        <div class=" col-md-6">
            <h3><fmt:message key="jsp.community-home.heading2"/></h3>
            <ul class="list-group">
                <c:forEach items="${collections}" var="collection">

                    <li class="list-group-item"><a href = "${collection.handle}">${collection.title} </a><span class="badge">${collection.itemCount}</span></li>
                </c:forEach>
            </ul>
        </div>
    </div>
</dspace:layout>