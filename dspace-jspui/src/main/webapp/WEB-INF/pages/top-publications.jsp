<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>

<%@ page import="org.dspace.core.Context" %>
<%@ page import="org.dspace.app.webui.util.UIUtil" %>
<%@ page import="org.dspace.eperson.EPerson" %>
<%@ page import="java.sql.*" %>
<%@ page import="org.dspace.core.ConfigurationManager" %>
<%@ page import="java.util.Locale"%>
<%@ page import="org.dspace.app.webui.util.UIUtil" %>


<%
    org.dspace.core.Context context = org.dspace.app.webui.util.UIUtil.obtainContext(request);
%>

<dspace:layout locbar="commLink" titlekey="jsp.top50items" feedData="NONE">

    <h2><fmt:message key="jsp.top50items"/></h2>

    <table align="center" width="95%" border="0">
        <tr>
            <th></th>
            <th style = "text-align:center;"><fmt:message key="org.dspace.app.webui.jsptag.ItemTag.downloads"/></th>
        </tr>
        <tr>
            <td colspan="2"><span style="width: 100%; display: block; border-bottom: 1px dotted #777;" /></td>
        </tr>

        <c:forEach items="${publicationList}" var="publication" varStatus="i">
            <tr height="30">
                <td><a href="${publication.link}">${i.index + 1}. ${publication.name}</a></td>
                <td align="center">${publication.downloadCount}</td>
            </tr>

            <tr>
                <td colspan="2"><span style="width: 100%; display: block; border-bottom: 1px dotted #777;" /></td>
            </tr>
        </c:forEach>
    </table>
</dspace:layout>