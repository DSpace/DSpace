<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>
<%@ taglib uri='http://java.sun.com/jsp/jstl/core' prefix='c'%>
<%@ page import="javax.servlet.jsp.jstl.fmt.LocaleSupport" %>

<%
    org.dspace.core.Context context = org.dspace.app.webui.util.UIUtil.obtainContext(request);
%>

<dspace:layout locbar="nolink" titlekey="jsp.layout.navbar-admin.statistics">

    <h2><%= LocaleSupport.getLocalizedMessage(pageContext, "jsp.general-statistics.stat-sumdu") %></h2>

    <h3><%= LocaleSupport.getLocalizedMessage(pageContext, "jsp.general-statistics.calculation") %></h3>
    <c:set var="totalViews" value="0" scope="page" />
    <c:set var="totalDownloads" value="0" scope="page" />
    <c:forEach var="yearStatistics" items="${listYearStatistics}" varStatus="cnt" begin="0" end="${listYearStatistics.size() - 1}">
        <c:set var="totalViews" value="${totalViews + yearStatistics.totalYearViews}" scope="page" />
        <c:set var="totalDownloads" value="${totalDownloads + yearStatistics.totalYearDownloads}" scope="page" />
    </c:forEach>

    <table width="95%%" align="center" class="table">
        <tr class="oddRowOddCol">
            <td id="TotalCount">${totalItemCount}</td>
            <td><%= LocaleSupport.getLocalizedMessage(pageContext, "jsp.general-statistics.total-resources") %></td>
        </tr>
        <tr class="evenRowOddCol">
            <td id="TotalViews">${totalViews}</td>
            <td><%= LocaleSupport.getLocalizedMessage(pageContext, "jsp.general-statistics.total-views") %></td>
        </tr>
        <tr class="oddRowOddCol">
            <td id="TotalDownloads">${totalDownloads}</td>
            <td><%= LocaleSupport.getLocalizedMessage(pageContext, "jsp.general-statistics.total-downloads") %></td>
        </tr>
    </table>
    <c:forEach var="yearStatistics" items="${listYearStatistics}" varStatus="cnt" begin="0" end="${listYearStatistics.size() - 1}">
        <h3><c:out value="${yearStatistics.year}" />&nbsp<%= LocaleSupport.getLocalizedMessage(pageContext, "jsp.general-statistics.year") %></h3>
        <table width="95%%" align="center" class="table">
            <tr class="oddRowOddCol" align="center">
                <% for (int i = 0; i < 12; i++) {
                    String key = "jsp.general-statistics.month" + Integer.valueOf(i).toString();
                %>
                <td><%= LocaleSupport.getLocalizedMessage(pageContext, key) %></td>
                <% } %>
                <td><%= LocaleSupport.getLocalizedMessage(pageContext, "jsp.general-statistics.total") %></td>
            </tr>

            <tr class="evenRowOddCol" align="center">
                <c:forEach var="cntViews" items="${yearStatistics.yearViews}" varStatus="status" begin="0" end="11">
                    <td><c:out value="${cntViews}"/></td>
                </c:forEach>
                <td><c:out value="${yearStatistics.totalYearViews}"/> - <%= LocaleSupport.getLocalizedMessage(pageContext, "jsp.general-statistics.views") %></td>
            </tr>

            <tr class="oddRowOddCol" align="center">
                <c:forEach var="cntDownloads" items="${yearStatistics.yearDownloads}" varStatus="status" begin="0" end="11">
                    <td><c:out value="${cntDownloads}"/></td>
                </c:forEach>
                <td><c:out value="${yearStatistics.totalYearDownloads}"/> - <%= LocaleSupport.getLocalizedMessage(pageContext, "jsp.general-statistics.downloads") %></td>
            </tr>

        </table>

    </c:forEach>

</dspace:layout>