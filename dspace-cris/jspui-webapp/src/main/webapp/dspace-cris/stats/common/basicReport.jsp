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

<%--		<h2 class="titlestats">${markerasnumber}) <span class="titlestats"><fmt:message key="view.${data.jspKey}.${statType}.page.title" /></span> 
		<h2 class="titlestats"><span class="titlestats"><fmt:message key="view.${data.jspKey}.${statType}.page.title"><fmt:param>${data.target.simpleName}</fmt:param></fmt:message></span>
			<span class="titlestats-condition">${condition}</span>
		</h2>--%>

	
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
	<div id="statstab-content-continent" class="statstab-content-item statstab-show blank-page">
				<c:set var="pieType" >continent</c:set>
				<stats:piewithtable data="${data}" statType="${fn:escapeXml(statType)}" objectName="${objectName}" pieType="${pieType}" useFmt="true"/>
	</div>
	<div id="statstab-content-countryCode" class="statstab-content-item blank-page">
				<c:set var="pieType" >countryCode</c:set>
				<stats:piewithtable data="${data}" statType="${fn:escapeXml(statType)}" objectName="${objectName}" pieType="${pieType}" useFmt="true"/>
	</div>
	<div id="statstab-content-city" class="statstab-content-item blank-page">
				<c:set var="pieType" >city</c:set>
				<stats:piewithtable data="${data}" statType="${fn:escapeXml(statType)}" objectName="${objectName}" pieType="${pieType}"/>
	</div>
	<c:if test="${mode == 'download'}">	
	<div id="statstab-content-id" class="statstab-content-item blank-page">	 
		<c:set var="pieType" >sectionid</c:set>
		<stats:piewithtable data="${data}" statType="${fn:escapeXml(statType)}" objectName="${objectName}" pieType="${pieType}" useLocalMap="true"/>
	</div>
	</c:if>
	<div id="statstab-content-time" class="statstab-content-item blank-page">
				<c:set var="objectName">time</c:set>				
				<%@include file="time.jsp"%>
	</div>
</div>



<script type="text/javascript">
<!--
	j(document).ready(function() {
		j(".statstabahref").click(function() {		
			var d = j('div#' + j(this).attr('id').replace('ahref', 'content'));
			d.trigger('redraw');
		});
		
		j("#statstabs").tabs({
			"activate": function( event, ui ) {
				j("li.ui-tabs-active").toggleClass("ui-tabs-active ui-state-active active");
			},
			"beforeActivate": function( event, ui ) {
   			 j("#statstab-menu").find("li.active").toggleClass("active");
			},
	   		"create": function( event, ui ) {
	               j("div.ui-tabs").toggleClass("ui-tabs ui-widget ui-widget-content ui-corner-all tabbable");
	               j("ul.ui-tabs-nav").toggleClass("ui-tabs-nav ui-helper-reset ui-helper-clearfix ui-widget-header ui-corner-all nav nav-tabs");
	               j("li.ui-tabs-active").toggleClass("ui-state-default ui-corner-top ui-tabs-active ui-state-active active");
	               j("li.ui-state-default").toggleClass("ui-state-default ui-corner-top");
	               j("div.ui-tabs-panel").toggleClass("ui-tabs-panel ui-widget-content ui-corner-bottom tab-content with-padding");
	        }
		});

	});

-->
</script>
</div>
</c:when>
<c:otherwise> 
	<span class="titlestats"><fmt:message key="view.${data.jspKey}.${fn:escapeXml(statType)}.data.empty" /></span>
</c:otherwise>
</c:choose>
				 
