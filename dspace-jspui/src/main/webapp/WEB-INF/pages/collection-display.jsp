<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="C" uri="http://java.sun.com/jsp/jstl/core" %>
<%@taglib prefix="essuir" tagdir="/WEB-INF/tags/essuir"%>


<%@ page import="org.dspace.app.webui.util.UIUtil" %>
<%@ page import="org.dspace.browse.BrowseIndex" %>
<%@ page import="org.dspace.app.webui.servlet.MyDSpaceServlet" %>
<%@ page import="org.dspace.app.webui.servlet.admin.EditCommunitiesServlet" %>
<%@ page import="javax.servlet.jsp.jstl.fmt.LocaleSupport" %>
<%@ page import="org.dspace.core.Context" %>

<%
    int discovery_panel_cols = 12;
    int discovery_facet_cols = 12;
%>
<dspace:layout locbar="commLink" title="">
    <div class="panel panel-default">
        <div class="panel-body">
            <h2>${title} [${itemCount}] </h2>
        </div>
    </div>

    <essuir:browseByBlock browseIndices="${browseIndices}" handle="${handle}"/>

    <div>
        <div class="panel panel-primary">
            <div class="panel-heading text-center">
                <fmt:message key="browse.full.range">
                    <fmt:param value="${startIndex}"/>
                    <fmt:param value="${finishIndex}"/>
                    <fmt:param value="${totalItems}"/>
                </fmt:message>
                <a href="#" class="pull-right glyphicon glyphicon-filter" aria-hidden="true"  data-toggle="modal" data-target="#searchModal"></a>
            </div>

            <essuir:browseExtendedTable items="${items}" />

            <div class="panel-footer text-center">
                <essuir:pagination links="${links}" prevPageUrl="${prevPageUrl}" prevPageDisabled="${prevPageDisabled}" nextPageUrl="${nextPageUrl}" nextPageDisabled="${nextPageDisabled}"/>
            </div>
        </div>
    </div>
    <dspace:sidebar>
        <c:if test="${editorButton or adminButton}">
            <div class="panel panel-warning">
            <div class="panel-heading"><fmt:message key="jsp.admintools"/>
                <span class="pull-right"><dspace:popup page="<%= LocaleSupport.getLocalizedMessage(pageContext, \"help.collection-admin\")%>"><fmt:message key="jsp.adminhelp"/></dspace:popup></span>
            </div>
            <div class="panel-body">
            <c:if test="${editorButton}">
                <form method="post" action="<%=request.getContextPath()%>/tools/edit-communities">
                    <input type="hidden" name="collection_id" value="${collection.ID}" />
                    <input type="hidden" name="community_id" value="${community.ID}" />
                    <input type="hidden" name="action" value="<%= EditCommunitiesServlet.START_EDIT_COLLECTION %>" />
                    <input class="btn btn-default col-md-12" type="submit" value="<fmt:message key="jsp.general.edit.button"/>" />
                </form>
            </c:if>
            <c:if test="${editorButton or adminButton}">

                <form method="post" action="<%=request.getContextPath()%>/tools/itemmap">
                    <input type="hidden" name="cid" value="${collection.ID}" />
                    <input class="btn btn-default col-md-12" type="submit" value="<fmt:message key="jsp.collection-home.item.button"/>" />
                </form>
                <c:if test="${not empty submitters}">
                    <form method="get" action="<%=request.getContextPath()%>/tools/group-edit">
                        <input type="hidden" name="group_id" value="${submitters.ID}" />
                        <input class="btn btn-default col-md-12" type="submit" name="submit_edit" value="<fmt:message key="jsp.collection-home.editsub.button"/>" />
                    </form>
                </c:if>
                <c:if test="${editorButton or adminButton}">
                    <form method="post" action="<%=request.getContextPath()%>/mydspace">
                        <input type="hidden" name="collection_id" value="${collection.ID}" />
                        <input type="hidden" name="step" value="<%= MyDSpaceServlet.REQUEST_EXPORT_ARCHIVE %>" />
                        <input class="btn btn-default col-md-12" type="submit" value="<fmt:message key="jsp.mydspace.request.export.collection"/>" />
                    </form>
                    <form method="post" action="<%=request.getContextPath()%>/mydspace">
                        <input type="hidden" name="collection_id" value="${collection.ID}" />
                        <input type="hidden" name="step" value="<%= MyDSpaceServlet.REQUEST_MIGRATE_ARCHIVE %>" />
                        <input class="btn btn-default col-md-12" type="submit" value="<fmt:message key="jsp.mydspace.request.export.migratecollection"/>" />
                    </form>
                    <form method="post" action="<%=request.getContextPath()%>/dspace-admin/metadataexport">
                        <input type="hidden" name="handle" value="${handle}" />
                        <input class="btn btn-default col-md-12" type="submit" value="<fmt:message key="jsp.general.metadataexport.button"/>" />
                    </form>
                    </div>
                    </div>
                </c:if>
            </c:if>
        </c:if>

        <%@ include file="/discovery/static-sidebar-facet.jsp" %>
    </dspace:sidebar>
</dspace:layout>
<%
    // Obtain DSpace context
    Context context = UIUtil.obtainContext(request);
    context.complete();
%>