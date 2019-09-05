<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<%@ page import="org.dspace.core.Context" %>
<%@ page import="org.dspace.services.factory.DSpaceServicesFactory" %>


<%
    org.dspace.core.Context context = org.dspace.app.webui.util.UIUtil.obtainContext(request);
%>

<dspace:layout locbar="commLink" titlekey="jsp.top50items" feedData="NONE">
    <%
        int topAuthorsCount = DSpaceServicesFactory.getInstance().getConfigurationService().getIntProperty("jsp.view.top_authors_count");
        request.setAttribute("dspace.layout.title", ((String)request.getAttribute("dspace.layout.title")).replace("{0}", String.valueOf(topAuthorsCount)));
    %>

    <h2>
        <fmt:message key="jsp.top10authors">
            <fmt:param value="${listSize}"/>
        </fmt:message>
    </h2>

    <table align="center" width="95%" border="0">
        <tr>
            <th></th>
            <th style = "text-align:center;"><fmt:message key="org.dspace.app.webui.jsptag.ItemTag.downloads"/></th>
        </tr>
        <tr>
            <td colspan="2"><span style="width: 100%; display: block; border-bottom: 1px dotted #777;" /></td>
        </tr>

        <c:forEach items="${authorList}" var="author" varStatus="i">
            <tr height="30">
                <td><a href="/browse?type=author&value=${author.key}">${i.index + 1}. ${author.key}</a></td>
                <td align="center">${author.value}</td>
            </tr>

            <tr>
                <td colspan="2"><span style="width: 100%; display: block; border-bottom: 1px dotted #777;" /></td>
            </tr>
        </c:forEach>
    </table>
</dspace:layout>