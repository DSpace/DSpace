<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    https://github.com/CILEA/dspace-cris/wiki/License

--%>
<%@ taglib uri="statstags" prefix="stats" %>
<!-- Target div for drill down -->
<!-- div id="drillDownDiv" style="width: 800px; height: 400px; margin: 1 auto;position:relative;top:0; left:0; font-size:50px; z-index:2;border-style:solid;border-width:5px;"></div-->
        <c:set var="itemId" scope="page">${data.id}</c:set>
		<c:set var="statType">selectedObject</c:set>

<%--		<h2 class="titlestats">${markerasnumber}) <span class="titlestats"><fmt:message key="view.${data.jspKey}.${statType}.page.title" /></span> --%>
		<h2 class="titlestats"><span class="titlestats"><fmt:message key="view.${data.jspKey}.${statType}.page.title"><fmt:param>${data.target.simpleName}</fmt:param></fmt:message></span>
			<span class="titlestats-condition">${condition}</span>
		</h2>

	
		<c:choose>
			<c:when test="${data.resultBean.dataBeans[statType]['time']['total'].dataTable[0][0] > 0}">

				<c:set var="objectName">geo</c:set>	
				<%@include file="../modules/map/map.jsp" %>

<div id="statstabs">
<div id="statstab-menu">
<ul>
	<li id="statstab-menu-continent" class="statstab-current"><a id="statstab-ahref-continent" class="statstabahref" href="#statstab-content-continent">Region</a></li>
	<li id="statstab-menu-countryCode"><a id="statstab-ahref-countryCode" class="statstabahref" href="#statstab-content-countryCode">Country</a></li>
	<li id="statstab-menu-city"><a id="statstab-ahref-city" class="statstabahref" href="#statstab-content-city">City</a></li>
	<c:if test="${mode == 'download'}">	
	<li id="statstab-menu-id"><a id="statstab-ahref-id" class="statstabahref" href="#statstab-content-id">File</a></li>
	</c:if>
	<li id="statstab-menu-time"><a id="statstab-ahref-time" class="statstabahref" href="#statstab-content-time">Time</a></li>
</ul>
</div>
<div id="statstab-content">
	<div id="statstab-content-continent" class="statstab-content-item statstab-show">
				<c:set var="pieType" >continent</c:set>
				<stats:piewithtable data="${data}" statType="${statType}" objectName="${objectName}" pieType="${pieType}" useFmt="true"/>
	</div>
	<div id="statstab-content-countryCode" class="statstab-content-item">
				<c:set var="pieType" >countryCode</c:set>
				<stats:piewithtable data="${data}" statType="${statType}" objectName="${objectName}" pieType="${pieType}" useFmt="true"/>
	</div>
	<div id="statstab-content-city" class="statstab-content-item">
				<c:set var="pieType" >city</c:set>
				<stats:piewithtable data="${data}" statType="${statType}" objectName="${objectName}" pieType="${pieType}"/>
	</div>
	<c:if test="${mode == 'download'}">	
	<div id="statstab-content-id" class="statstab-content-item">	 
		<c:set var="pieType" >sectionid</c:set>
		<stats:piewithtable data="${data}" statType="${statType}" objectName="${objectName}" pieType="${pieType}" useLocalMap="true"/>
	</div>
	</c:if>
	<div id="statstab-content-time" class="statstab-content-item">
				<c:set var="objectName">time</c:set>				
				<%@include file="time.jsp"%>
	</div>
</div>



<script type="text/javascript">
<!--
	var j = jQuery.noConflict();
	j(document).ready(function() {

		
		j(".statstabahref").click(function() {		
			var d = j('div#' + j(this).attr('id').replace('ahref', 'content'));		
			d.trigger('redraw');			
		});
		
		j("#statstabs").tabs();
	});

-->
</script>
</div>
</c:when>
<c:otherwise> 
	<fmt:message key="view.${data.jspKey}.${statType}.data.empty" />
</c:otherwise>
</c:choose>
				 
