<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%--
  - Display item/collection/community statistics
  -
  - Attributes:
  -    statsVisits - bean containing name, data, column and row labels
  -    statsMonthlyVisits - bean containing name, data, column and row labels
  -    statsFileDownloads - bean containing name, data, column and row labels
  -    statsCountryVisits - bean containing name, data, column and row labels
  -    statsCityVisits - bean containing name, data, column and row labels
  -    isItem - boolean variable, returns true if the DSO is an Item 
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<%@page import="org.dspace.app.webui.servlet.MyDSpaceServlet"%>

<% Boolean isItem = (Boolean)request.getAttribute("isItem"); %>


<dspace:layout titlekey="jsp.statistics.title">
<h1><fmt:message key="jsp.statistics.title"/></h1>
<h2><fmt:message key="jsp.statistics.heading.visits"/></h2>
<table class="statsTable">
<tr>
<th><!-- spacer cell --></th>
<th><fmt:message key="jsp.statistics.heading.views"/></th>
</tr>
<c:forEach items="${statsVisits.matrix}" var="row" varStatus="counter">
<c:forEach items="${row}" var="cell" varStatus="rowcounter">
<c:choose>
<c:when test="${rowcounter.index % 2 == 0}">
<c:set var="rowClass" value="evenRowOddCol"/>
</c:when>
<c:otherwise>
<c:set var="rowClass" value="oddRowOddCol"/>
</c:otherwise>
</c:choose>
<tr class="${rowClass}">
<td>
<c:out value="${statsVisits.colLabels[counter.index]}"/>
<td>
<c:out value="${cell}"/>
</td>
</c:forEach>
</tr>
</c:forEach>
</table>

<h2><fmt:message key="jsp.statistics.heading.monthlyvisits"/></h2>
<table class="statsTable">
<tr>
<th><!-- spacer cell --></th>
<c:forEach items="${statsMonthlyVisits.colLabels}" var="headerlabel" varStatus="counter">
<th>
<c:out value="${headerlabel}"/>
</th>
</c:forEach>
</tr>
<c:forEach items="${statsMonthlyVisits.matrix}" var="row" varStatus="counter">
<c:choose>
<c:when test="${counter.index % 2 == 0}">
<c:set var="rowClass" value="evenRowOddCol"/>
</c:when>
<c:otherwise>
<c:set var="rowClass" value="oddRowOddCol"/>
</c:otherwise>
</c:choose>
<tr class="${rowClass}">
<td>
<c:out value="${statsMonthlyVisits.rowLabels[counter.index]}"/>
</td>
<c:forEach items="${row}" var="cell">
<td>
<c:out value="${cell}"/>
</td>
</c:forEach>
</tr>
</c:forEach>
</table>

<% if(isItem) { %>

<h2><fmt:message key="jsp.statistics.heading.filedownloads"/></h2>
<table class="statsTable">
<tr>
<th><!-- spacer cell --></th>
<th><fmt:message key="jsp.statistics.heading.views"/></th>
</tr>
<c:forEach items="${statsFileDownloads.matrix}" var="row" varStatus="counter">
<c:forEach items="${row}" var="cell" varStatus="rowcounter">
<c:choose>
<c:when test="${rowcounter.index % 2 == 0}">
<c:set var="rowClass" value="evenRowOddCol"/>
</c:when>
<c:otherwise>
<c:set var="rowClass" value="oddRowOddCol"/>
</c:otherwise>
</c:choose>
<tr class="${rowClass}">
<td>
<c:out value="${statsFileDownloads.colLabels[rowcounter.index]}"/>
<td>
<c:out value="${cell}"/>
</td>
</c:forEach>
</tr>
</c:forEach>
</table>

<% } %>

<h2><fmt:message key="jsp.statistics.heading.countryvisits"/></h2>
<table class="statsTable">
<tr>
<th><!-- spacer cell --></th>
<th><fmt:message key="jsp.statistics.heading.views"/></th>
</tr>
<c:forEach items="${statsCountryVisits.matrix}" var="row" varStatus="counter">
<c:forEach items="${row}" var="cell" varStatus="rowcounter">
<c:choose>
<c:when test="${rowcounter.index % 2 == 0}">
<c:set var="rowClass" value="evenRowOddCol"/>
</c:when>
<c:otherwise>
<c:set var="rowClass" value="oddRowOddCol"/>
</c:otherwise>
</c:choose>
<tr class="${rowClass}">
<td>
<c:out value="${statsCountryVisits.colLabels[rowcounter.index]}"/>
<td>
<c:out value="${cell}"/>
</tr>
</td>
</c:forEach>
</c:forEach>
</table>

<h2><fmt:message key="jsp.statistics.heading.cityvisits"/></h2>
<table class="statsTable">
<tr>
<th><!-- spacer cell --></th>
<th><fmt:message key="jsp.statistics.heading.views"/></th>
</tr>
<c:forEach items="${statsCityVisits.matrix}" var="row" varStatus="counter">
<c:forEach items="${row}" var="cell" varStatus="rowcounter">
<c:choose>
<c:when test="${rowcounter.index % 2 == 0}">
<c:set var="rowClass" value="evenRowOddCol"/>
</c:when>
<c:otherwise>
<c:set var="rowClass" value="oddRowOddCol"/>
</c:otherwise>
</c:choose>
<tr class="${rowClass}">
<td>
<c:out value="${statsCityVisits.colLabels[rowcounter.index]}"/>
<td>
<c:out value="${cell}"/>
</td>
</tr>
</c:forEach>
</c:forEach>
</table>


</dspace:layout>



