<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    https://github.com/CILEA/dspace-cris/wiki/License

--%>
<%@ taglib uri="statstags" prefix="stats" %>
<c:set var="statType">top</c:set>
<c:set var="objectName">upload</c:set>
<%--h2 class="titlestats">${markerasnumber}) <span class="titlestats"><fmt:message key="view.${data.jspKey}.${statType}.${objectName}.page.title" /></span></h2
<h2 class="titlestats"><span class="titlestats"><fmt:message key="view.${data.jspKey}.${statType}.${objectName}.page.title" /></span></h2> --%>

<c:choose>
	<c:when test="${data.resultBean.dataBeans[statType][objectName]['total'].dataTable[0][0] > 0}">
		<c:set var="drillDownInfo" >drillDown-${pieType}-${objectName}</c:set>
	
		<%-- @include file="../modules/map/map.jsp" --%> 
<div id="statstabs"> 
<div id="statstab-content">
	<div id="statstab-content-time" class="statstab-content-item">		
		<%@include file="time.jsp"%> 
	</div>
	<c:choose>	
	<c:when test="${mode == 'download' && objectName!='bitstream'}">
		<div id="statstab-content-id" class="statstab-content-item">		 
		<c:set var="pieType">sectionid</c:set>
		<stats:piewithtable data="${data}" statType="${fn:escapeXml(statType)}" objectName="${objectName}" pieType="${pieType}" useLocalMap="true"/>
		</div>	
	</c:when>
<%-- 	<c:otherwise>
		<div id="statstab-content-id" class="statstab-content-item">		 
		<c:set var="pieType">id</c:set>
		<stats:piewithtable data="${data}" statType="${statType}" objectName="${objectName}" pieType="${pieType}" useLocalMap="true"/>
		</div>	
	</c:otherwise>--%>
	</c:choose>
	
</div>
</div>
	</c:when>
	<c:otherwise> 
		<fmt:message key="view.${data.jspKey}.${fn:escapeXml(statType)}.${objectName}.data.empty" />
				
	</c:otherwise>
</c:choose>
