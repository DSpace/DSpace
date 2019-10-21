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
        <%@ include file="/discovery/static-sidebar-facet.jsp" %>
    </dspace:sidebar>
</dspace:layout>