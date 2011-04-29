<%--
  - display-statistics.jsp
  -
  - Version: $Revision$
  -
  - Date: $Date$
  -
  - Copyright (c) 2002, Hewlett-Packard Company and Massachusetts
  - Institute of Technology.  All rights reserved.
  -
  - Redistribution and use in source and binary forms, with or without
  - modification, are permitted provided that the following conditions are
  - met:
  -
  - - Redistributions of source code must retain the above copyright
  - notice, this list of conditions and the following disclaimer.
  -
  - - Redistributions in binary form must reproduce the above copyright
  - notice, this list of conditions and the following disclaimer in the
  - documentation and/or other materials provided with the distribution.
  -
  - - Neither the name of the Hewlett-Packard Company nor the name of the
  - Massachusetts Institute of Technology nor the names of their
  - contributors may be used to endorse or promote products derived from
  - this software without specific prior written permission.
  -
  - THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
  - ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
  - LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
  - A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
  - HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
  - INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
  - BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
  - OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
  - ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
  - TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
  - USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
  - DAMAGE.
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



