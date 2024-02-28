
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>


<%@page import="org.dspace.content.service.CollectionService"%>
<%@page import="org.dspace.content.factory.ContentServiceFactory"%>
<%@page import="org.dspace.content.service.CommunityService"%>
<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>
<%@taglib prefix="essuir" tagdir="/WEB-INF/tags/essuir"%>

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
<%@ page import="org.dspace.app.webui.servlet.MyDSpaceServlet" %>
<%@ page import="org.dspace.core.Context" %>

<%
    int discovery_panel_cols = 12;
    int discovery_facet_cols = 4;
%>
<dspace:layout title="${title}">
    <div class="panel panel-default">
        <div class="panel-body">
            <h2>${title} [${itemCount}] </h2>
        </div>
    </div>
    
    <essuir:browseByBlock browseIndices="${browseIndices}" handle="${handle}"/>

    <div class="row">
        <%@ include file="/discovery/static-sidebar-facet.jsp" %>
    </div>

    <div class="row">
        <c:if test="${not empty subCommunities}">
            <div class=" col-md-6">
                <h3><fmt:message key="jsp.community-home.heading3"/></h3>
                <ul class="list-group">
                    <c:forEach items="${subCommunities}" var="subCommunity">
                        <li class="list-group-item"><a href = "/handle/${subCommunity.handle}">${subCommunity.title} </a><span class="badge">${subCommunity.itemCount}</span>
                            <c:if test="${removeButton}">
                                <form class="btn-group" method="post" action="<%=request.getContextPath()%>/tools/edit-communities">
                                    <input type="hidden" name="parent_community_id" value="${community.ID}" />
                                    <input type="hidden" name="community_id" value="${subCommunity.id}" />
                                    <input type="hidden" name="action" value="<%=EditCommunitiesServlet.START_DELETE_COMMUNITY%>" />
                                    <button type="submit" class="btn btn-xs btn-danger"><span class="glyphicon glyphicon-trash"></span></button>
                                </form>
                            </c:if>
                        </li>
                    </c:forEach>
                </ul>
            </div>
        </c:if>
        <c:if test="${not empty collections}">
            <div class=" col-md-6">
                <h3><fmt:message key="jsp.community-home.heading2"/></h3>
                <ul class="list-group">
                    <c:forEach items="${collections}" var="collection">

                        <li class="list-group-item"><a href = "/handle/${collection.handle}">${collection.title} </a><span class="badge">${collection.itemCount}</span>
                            <c:if test="${removeButton}">
                                <form class="btn-group" method="post" action="<%=request.getContextPath()%>/tools/edit-communities">
                                    <input type="hidden" name="parent_community_id" value="${community.ID}" />
                                    <input type="hidden" name="community_id" value="${community.ID}" />
                                    <input type="hidden" name="collection_id" value="${collection.id}" />
                                    <input type="hidden" name="action" value="<%=EditCommunitiesServlet.START_DELETE_COLLECTION%>" />
                                    <button type="submit" class="btn btn-xs btn-danger"><span class="glyphicon glyphicon-trash"></span></button>
                                </form>
                            </c:if>
                        </li>
                    </c:forEach>
                </ul>
            </div>
        </c:if>
    </div>

    <c:if test="${addButton or editorButton}">
    <dspace:sidebar>

        <div class="panel panel-warning">
            <div class="panel-heading">
                <fmt:message key="jsp.admintools"/>
                <span class="pull-right">
             		<dspace:popup page="<%= LocaleSupport.getLocalizedMessage(pageContext, \"help.site-admin\")%>"><fmt:message key="jsp.adminhelp"/></dspace:popup>
             	</span>
            </div>
            <div class="panel-body">

                <c:if test="${editorButton}">
                <form method="post" action="<%=request.getContextPath()%>/tools/edit-communities">
                    <input type="hidden" name="community_id" value="${community.ID}" />
                    <input type="hidden" name="action" value="<%=EditCommunitiesServlet.START_EDIT_COMMUNITY%>" />
                        <%--<input type="submit" value="Edit..." />--%>
                    <input class="btn btn-default col-md-12" type="submit" value="<fmt:message key="jsp.general.edit.button"/>" />
                </form>
                </c:if>

                <c:if test="${addButton}">
                <form method="post" action="<%=request.getContextPath()%>/tools/collection-wizard">
                    <input type="hidden" name="community_id" value="${community.ID}" />
                    <input class="btn btn-default col-md-12" type="submit" value="<fmt:message key="jsp.community-home.create1.button"/>" />
                </form>

                <form method="post" action="<%=request.getContextPath()%>/tools/edit-communities">
                    <input type="hidden" name="action" value="<%= EditCommunitiesServlet.START_CREATE_COMMUNITY%>" />
                    <input type="hidden" name="parent_community_id" value="${community.ID}" />
                        <%--<input type="submit" name="submit" value="Create Sub-community" />--%>
                    <input class="btn btn-default col-md-12" type="submit" name="submit" value="<fmt:message key="jsp.community-home.create2.button"/>" />
                </form>
                </c:if>

                <c:if test="${editorButton}">
                <form method="post" action="<%=request.getContextPath()%>/mydspace">
                    <input type="hidden" name="community_id" value="${community.ID}" />
                    <input type="hidden" name="step" value="<%= MyDSpaceServlet.REQUEST_EXPORT_ARCHIVE %>" />
                    <input class="btn btn-default col-md-12" type="submit" value="<fmt:message key="jsp.mydspace.request.export.community"/>" />
                </form>
                <form method="post" action="<%=request.getContextPath()%>/mydspace">
                    <input type="hidden" name="community_id" value="${community.ID}" />
                    <input type="hidden" name="step" value="<%= MyDSpaceServlet.REQUEST_MIGRATE_ARCHIVE %>" />
                    <input class="btn btn-default col-md-12" type="submit" value="<fmt:message key="jsp.mydspace.request.export.migratecommunity"/>" />
                </form>
                <form method="post" action="<%=request.getContextPath()%>/dspace-admin/metadataexport">
                    <input type="hidden" name="handle" value="${handle}" />
                    <input class="btn btn-default col-md-12" type="submit" value="<fmt:message key="jsp.general.metadataexport.button"/>" />
                </form>
                </c:if>

            </div>
        </div>


    </dspace:sidebar>
</c:if>
</dspace:layout>
<%
    // Obtain DSpace context
    Context context = UIUtil.obtainContext(request);
    context.complete();
%>