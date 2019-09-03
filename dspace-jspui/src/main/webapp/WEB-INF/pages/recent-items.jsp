<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>

<%@ page import="org.dspace.core.Context" %>

<%
    org.dspace.core.Context context = org.dspace.app.webui.util.UIUtil.obtainContext(request);
%>

<dspace:layout locbar="nolink" titlekey="jsp.collection-home.recentsub" feedData="NONE">

    <h2><fmt:message key="jsp.collection-home.recentsub"/></h2>

    <table align="center" width="95%" border="0">
        <tr>
            <td colspan="2"><span style="width: 100%; display: block; border-bottom: 1px dotted #777;" /></td>
        </tr>

        <c:forEach items="${recentItems}" var="item">
            <tr height="30">
                <td width="80%"><a href="<%= request.getContextPath() %>/handle/${item.handle}">${item.title}</a></td>
                <td align="right">[${item.type}]</td>
            </tr>

            <tr>
                <td colspan="2"><span style="width: 100%; display: block; border-bottom: 1px dotted #777;" /></td>
            </tr>
        </c:forEach>
    </table>
</dspace:layout>