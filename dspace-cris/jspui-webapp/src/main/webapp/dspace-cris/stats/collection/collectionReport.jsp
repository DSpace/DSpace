<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    https://github.com/CILEA/dspace-cris/wiki/License

--%>
<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%@ taglib uri="http://www.springframework.org/tags/form" prefix="form"%>
<%@ taglib uri="http://www.springframework.org/tags" prefix="spring"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>
<%@ taglib uri="http://displaytag.sf.net" prefix="display"%>
<%@ taglib uri="researchertags" prefix="researcher"%>
<%@ page import="org.dspace.core.ConfigurationManager"%>

<c:set var="contextPath" scope="application">${pageContext.request.contextPath}</c:set>
<c:set var="dspace.layout.head" scope="request">
	<script type="text/javascript" src="${contextPath}/js/rgbcolor.js"></script>
	<script type="text/javascript" src="${contextPath}/js/canvg.js"></script>
	<script type="text/javascript" src="${contextPath}/js/stats.js"></script>
    <script type="text/javascript" src="https://www.google.com/jsapi"></script>
    <script type="text/javascript">      
      google.load('visualization', '1.1', {packages: ['corechart', 'controls']});
    </script>
    
	
	<meta name="viewport" content="initial-scale=1.0, user-scalable=no" />
	<style type="text/css">
	  #map_canvas { height: 100% }
	</style>
	<script src="//maps.googleapis.com/maps/api/js?key=<%= ConfigurationManager.getProperty("key.googleapi.maps") %>&sensor=true&v=3" type="text/javascript"></script>
	
	<script type="text/javascript">
		function setMessage(message,div){
			document.getElementById(div).innerHTML=message;
		}
		function setGenericEmpityDataMessage(div){
			document.getElementById(div).innerHTML='<fmt:message key="view.generic.data.empty" />';
		}
	</script>
</c:set>


<dspace:layout titlekey="jsp.statistics.item-title">

<div id="content">
	<div class="col-lg-12">
		<div class="form-inline">
	         <div class="form-group">
				<h1><fmt:message key="view.${data.jspKey}.page.title"><fmt:param>${data.title}</fmt:param></fmt:message></h1>
   			 </div>
		</div>
	</div>
	<div class="pull-right">
		<span class="label label-info">from:</span>&nbsp; 
			<c:if test="${empty data.stats_from_date}"><fmt:message key="view.statistics.range.no-start-date" /></c:if>
			${data.stats_from_date} &nbsp;&nbsp;&nbsp; 
		<span class="label label-info">to:</span> &nbsp; 
			<c:if test="${empty data.stats_to_date}"><fmt:message key="view.statistics.range.no-end-date" /></c:if>
			${data.stats_to_date} &nbsp;&nbsp;&nbsp;
		<a class="btn btn-default" data-toggle="modal" data-target="#stats-date-change-dialog"><fmt:message key="view.statistics.change-range" /></a>
	</div>		
	<c:set var="type"><%=request.getParameter("type") %></c:set>
	<%@include file="/dspace-cris/stats/common/changeRange.jsp"%>
		<%@ include file="/dspace-cris/stats/collection/_collectionReport-right.jsp" %>
<div class="richeditor">
<div class="top"></div>

	<%@ include file="/dspace-cris/stats/collection/_collectionReport.jsp" %>
<div class="bottom">
			<c:if test="${data.seeParentObject}">
				<c:set var="parentLink">${contextPath}/cris/stats/community.html?handle=${data.parentObject.handle}&type=${type}&stats_from_date=${data.stats_from_date}&stats_to_date=${data.stats_to_date}</c:set>
				<div class="list-group">
					<a class="list-group-item" href="${parentLink}"><fmt:message key="view.${data.jspKey}.${type}.parentStats"><fmt:param>${data.parentObject.name}</fmt:param></fmt:message></a>
				</div>
			</c:if>
</div>
</div>
</div>
</div>
</div>
<div class="clear"></div>

</dspace:layout>
