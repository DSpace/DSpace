<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    https://github.com/CILEA/dspace-cris/wiki/License

--%>
<%@ taglib uri="statstags" prefix="stats" %>
<c:set var="statType" >top</c:set>
<%--h2 class="titlestats">${markerasnumber}) <span class="titlestats"><fmt:message key="view.${data.jspKey}.${statType}.${objectName}.page.title" /></span></h2--%>
<h2 class="titlestats"><span class="titlestats"><fmt:message key="view.${data.jspKey}.${statType}.${objectName}.page.title" /></span></h2>


<c:choose>
	<c:when test="${data.resultBean.dataBeans[statType][objectName]['total'].dataTable[0][0] > 0}">
		<c:set var="drillDownInfo" >drillDown-${pieType}-${objectName}</c:set>
	
		<%@include file="../modules/map/map.jsp" %> 
<div id="statstabs">
<div id="statstab-menu">
<ul>
	<li id="statstab-menu-continent" class="statstab-current"><a id="statstab-ahref-continent" class="statstabahref" href="#statstab-content-continent">Region</a></li>
	<li id="statstab-menu-countryCode"><a id="statstab-ahref-countryCode" class="statstabahref" href="#statstab-content-countryCode">Country</a></li>
	<li id="statstab-menu-city"><a id="statstab-ahref-city" class="statstabahref" href="#statstab-content-city">City</a></li>
	<c:choose>
	<c:when test="${mode == 'download' && objectName!='bitstream'}">	
		<li id="statstab-menu-id"><a id="statstab-ahref-id" class="statstabahref" href="#statstab-content-id">File</a></li>
	</c:when>
	<c:otherwise>
		<li id="statstab-menu-id" style="text-transform: capitalize"><a id="statstab-ahref-id" class="statstabahref" href="#statstab-content-id">${objectName}</a></li>
	</c:otherwise>
	</c:choose>
	<c:if test="${data.showExtraTab}">
		<li id="statstab-menu-category"><a id="statstab-ahref-category" class="statstabahref" href="#statstab-content-category">Category</a></li>
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
	<c:choose>	
	<c:when test="${mode == 'download' && objectName!='bitstream'}">
		<div id="statstab-content-id" class="statstab-content-item">		 
		<c:set var="pieType">sectionid</c:set>
		<stats:piewithtable data="${data}" statType="${statType}" objectName="${objectName}" pieType="${pieType}" useLocalMap="true"/>
		</div>	
	</c:when>
	<c:otherwise>
		<div id="statstab-content-id" class="statstab-content-item">		 
		<c:set var="pieType">id</c:set>
		<stats:piewithtable data="${data}" statType="${statType}" objectName="${objectName}" pieType="${pieType}" useLocalMap="true"/>
		</div>	
	</c:otherwise>
	</c:choose>
	<c:if test="${data.showExtraTab}">
	<div id="statstab-content-category" class="statstab-content-item">	 
		<c:set var="pieType" >category</c:set>
		<stats:piewithtable data="${data}" statType="${statType}" objectName="${objectName}" pieType="${pieType}" useLocalMap="true"/>
	</div>
	</c:if>
	<div id="statstab-content-time" class="statstab-content-item">		
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
		<fmt:message key="view.${data.jspKey}.${statType}.${objectName}.data.empty" />		
	</c:otherwise>
</c:choose>
