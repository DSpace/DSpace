<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    https://github.com/CILEA/dspace-cris/wiki/License

--%>
<%@ taglib uri="statstags" prefix="stats" %>
<c:set var="statType" >top</c:set>
<%--h2 class="titlestats">${markerasnumber}) <span class="titlestats"><fmt:message key="view.${data.jspKey}.${statType}.${objectName}.page.title" /></span></h2
<h2 class="titlestats"><span class="titlestats"><fmt:message key="view.${data.jspKey}.${statType}.${objectName}.page.title" /></span></h2> --%>


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
		<stats:piewithtable mostViewedItem="true" data="${data}" statType="${statType}" objectName="${objectName}" pieType="${pieType}" useLocalMap="true"/>
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
		
		j(".datatable-mostviewed")
		.dataTable({
					dom : "<'pull-left'f><'pull-right'B>rtip",
					searching : true,
					info : false,
					paging : false,
					ordering : true,
					order: [[ 1, "desc" ]],
					aoColumnDefs: [
						{ bSortable: false, aTargets: [ 0 ] }
					],
					buttons : [
							{
								extend : 'excelHtml5',								
								text : '<i class="fa fa-file-excel-o"></i> Download Excel'
							},
							{
								extend : 'csvHtml5',
								text : '<i class="fa fa-file-text-o"></i> Download CSV'
							}
					]					
		});
});

-->
</script>
	</c:when>
	<c:otherwise> 
		<fmt:message key="view.${data.jspKey}.${statType}.${objectName}.data.empty" />		
	</c:otherwise>
</c:choose>
