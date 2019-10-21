<%@ tag body-content="empty" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>

<%@ attribute name="browseIndices" rtexprvalue="true" required="true" type="java.util.List" description="Browse indices list" %>
<%@ attribute name="handle" rtexprvalue="true" required="true" type="java.lang.String" description="Item handle" %>

<div class="panel panel-primary">
    <div class="panel-heading"><fmt:message key="jsp.general.browse"/></div>
    <div class="panel-body">
        <c:forEach items="${browseIndices}" var="browseIndex">
            <form method="get" action="/handle/${handle}/browse">
                <input type="hidden" name="type" value="${browseIndex.name}"/>
                <input class="btn btn-default col-md-3" type="submit" name="submit_browse" value="<fmt:message key="browse.menu.${browseIndex.name}"/>"/>
            </form>
        </c:forEach>
    </div>
</div>