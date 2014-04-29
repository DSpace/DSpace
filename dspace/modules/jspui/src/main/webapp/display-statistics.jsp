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

<% Boolean isItem = (Boolean) request.getAttribute("isItem");%>


<dspace:layout titlekey="jsp.statistics.title">
  <h1><fmt:message key="jsp.statistics.title"/></h1>
  <h2><fmt:message key="jsp.statistics.heading.visits"/></h2>
  <table class="table statsTable">
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
              <c:out value="${statsVisits.colLabels[counter.index]}"/></span>
          <td>
              <span class="badge"><c:out value="${cell}"/></span>
          </td>
        </c:forEach>
      </tr>
    </c:forEach>
  </table>

  <h2><fmt:message key="jsp.statistics.heading.monthlyvisits"/></h2>
  <div id="sixmonth_canvas"></div>
  <table class="table statsTable">
    <tr>
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
        <c:forEach items="${row}" var="cell">
          <td>
              <span class="badge"><c:out value="${cell}"/></span>
          </td>
        </c:forEach>
      </tr>
    </c:forEach>
  </table>

  <h2><fmt:message key="jsp.statistics.heading.countryvisits"/></h2>
  <div id="map_canvas"></div>
  <table class="table statsTable">
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
<table class="table statsTable">
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
<% 
    if (isItem)
    {
%>

  <h2><fmt:message key="jsp.statistics.heading.filedownloads"/></h2>
  <table class="table statsTable">
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
              <span class="badge"><c:out value="${cell}"/></span>
          </td>
        </c:forEach>
      </tr>
    </c:forEach>
  </table>

  <% }%>
<script type="text/javascript" src="https://www.google.com/jsapi"></script>
<script type='text/javascript'>
  google.load('visualization', '1.0', {'packages': ['barchart','geomap','linechart']});
  google.setOnLoadCallback(drawAll);
  
  function resizeHandler () {
        drawAll();
  }
  if (window.addEventListener) {
        window.addEventListener('resize', resizeHandler, false);
  }
  else if (window.attachEvent) {
        window.attachEvent('onresize', resizeHandler);
  }

  function drawAll() {
    drawSixMonthChart();
    drawMap();
  }
   
  function drawSixMonthChart() {

      var data = new google.visualization.DataTable();
      data.addColumn('string', 'Mes');
      data.addColumn('number', '<fmt:message key="jsp.statistics.heading.views"/>');

  <c:forEach items="${statsMonthlyVisits.matrix}" var="row" varStatus="counter">
      <c:forEach items="${row}" var="cell" varStatus="rowcounter">      
          data.addRow(['<c:out value="${statsMonthlyVisits.colLabels[rowcounter.index]}"/>', <c:out value="${cell}"/>]);
    </c:forEach>
  </c:forEach>
      var container = document.getElementById('sixmonth_canvas');
      var options = {};
			
      //options['width']  = getElementWidth(container);
      options['width']  = '100%';
      options['height'] = 175;
      options['legend'] = 'top';
      options['legendFontSize'] = '12';
      options['axisFontSize'] = '12';
      options['showCategories'] = true;
			
      var chart = new google.visualization.LineChart(container);
      chart.draw(data, options);
  		
    };
   
    function drawMap() {

      var data = new google.visualization.DataTable();
      data.addRows(10);
      data.addColumn('string', 'País');
      data.addColumn('number', '<fmt:message key="jsp.statistics.heading.views"/>');

  <c:forEach items="${statsCountryVisits.matrix}" var="row" varStatus="counter">
      <c:forEach items="${row}" var="cell" varStatus="rowcounter">      
          data.setValue(<c:out value="${rowcounter.index}"/>,0,'<c:out value="${statsCountryVisits.colLabels[rowcounter.index]}"/>');
          data.setValue(<c:out value="${rowcounter.index}"/>,1,<c:out value="${cell}"/>);
    </c:forEach>
  </c:forEach>

      var container = document.getElementById('map_canvas');
      var options = {};
	
      options['dataMode'] = 'regions';
      //options['width']    = '100%';
      options['height']   = Math.round(getElementWidth(container) * 0.40); 

      var geomap = new google.visualization.GeoChart(container);
      geomap.draw(data, options);

    };

    function getElementWidth(obj) {
      if (obj == null || typeof obj == "undefined") {
        return 0;
      }
      if (typeof obj.clip !== "undefined") {
        return obj.clip.width;
      } else {
        if (obj.style.pixelWidth) {
          return obj.style.pixelWidth;
        } else {
          return obj.offsetWidth;
        }
      }
      return 0;
    }

    function getElementHeight(obj) {
      if (obj == null || typeof obj == "undefined") {
        return 0;
      }
      if (typeof obj.clip !== "undefined") {
        return obj.clip.height;
      } else {
        if (obj.style.pixelHeight) {
          return obj.style.pixelHeight;
        } else {
          return obj.offsetHeight;
        }
      }
      return 0;
    }
</script>
</dspace:layout>
