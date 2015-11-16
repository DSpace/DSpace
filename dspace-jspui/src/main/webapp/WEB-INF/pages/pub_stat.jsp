<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>
<%@ taglib uri='http://java.sun.com/jsp/jstl/core' prefix='c'%>
<%@ page import="javax.servlet.jsp.jstl.fmt.LocaleSupport" %>

<%
    org.dspace.core.Context context = org.dspace.app.webui.util.UIUtil.obtainContext(request);
%>

<dspace:layout locbar="nolink" titlekey="jsp.layout.navbar-admin.statistics">
    <script type="text/javascript" src="ajaxStatistics.js"></script>

    <h2><%= LocaleSupport.getLocalizedMessage(pageContext, "jsp.general-statistics.stat-sumdu") %></h2>

    <h3><%= LocaleSupport.getLocalizedMessage(pageContext, "jsp.general-statistics.calculation") %></h3>

    <table width="95%%" align="center" class="table">
        <tr class="oddRowOddCol">
            <td id="TotalCount">-</td>
            <td><%= LocaleSupport.getLocalizedMessage(pageContext, "jsp.general-statistics.total-resources") %></td>
        </tr>
        <tr class="evenRowOddCol">
            <td id="TotalViews">-</td>
            <td><%= LocaleSupport.getLocalizedMessage(pageContext, "jsp.general-statistics.total-views") %></td>
        </tr>
        <tr class="oddRowOddCol">
            <td id="TotalDownloads">-</td>
            <td><%= LocaleSupport.getLocalizedMessage(pageContext, "jsp.general-statistics.total-downloads") %></td>
        </tr>
    </table>
    <c:forEach var="yearStatistics" items="${listYearStatistics}" varStatus="cnt" begin="0" end="${listYearStatistics.size() - 1}">
        <h3><c:out value="${yearStatistics.getYear().toString()}" />&nbsp<%= LocaleSupport.getLocalizedMessage(pageContext, "jsp.general-statistics.year") %></h3>
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
                <c:forEach var="cntViews" items="${yearStatistics.getYearViews()}" varStatus="status" begin="0" end="11">
                    <c:choose>
                        <c:when test="${cnt.index == 0 && yearStatistics.getCurrentMonth() == status.index}">
                            <td id="CurrentMonthStatisticsViews">-</td>
                        </c:when>
                        <c:otherwise>
                            <td><c:out value="${cntViews.toString()}"/></td>
                        </c:otherwise>
                    </c:choose>
                </c:forEach>
                <c:if test="${cnt.index == 0}">
                    <td><span id="CurrentYearStatisticsViews"></span> - <%= LocaleSupport.getLocalizedMessage(pageContext, "jsp.general-statistics.views") %></td>
                </c:if>
                <c:if test="${cnt.index != 0}">
                    <td><c:out value="${yearStatistics.getTotalYearViews().toString()}"/> - <%= LocaleSupport.getLocalizedMessage(pageContext, "jsp.general-statistics.views") %></td>
                </c:if>

            </tr>

            <tr class="oddRowOddCol" align="center">
                <c:forEach var="cntDownloads" items="${yearStatistics.getYearDownloads()}" varStatus="status" begin="0" end="11">
                    <c:choose>
                        <c:when test="${cnt.index == 0 && yearStatistics.getCurrentMonth() == status.index}">
                            <td id="CurrentMonthStatisticsDownloads">-</td>
                        </c:when>
                        <c:otherwise>
                            <td><c:out value="${cntDownloads.toString()}"/></td>
                        </c:otherwise>
                    </c:choose>
                </c:forEach>
                <c:if test="${cnt.index == 0}">
                    <td ><span id="CurrentYearStatisticsDownloads"></span > - <%= LocaleSupport.getLocalizedMessage(pageContext, "jsp.general-statistics.downloads") %></td>
                </c:if>
                <c:if test="${cnt.index != 0}">
                    <td><c:out value="${yearStatistics.getTotalYearDownloads().toString()}"/> - <%= LocaleSupport.getLocalizedMessage(pageContext, "jsp.general-statistics.downloads") %></td>
                </c:if>

            </tr>

        </table>

    </c:forEach>

</dspace:layout>
