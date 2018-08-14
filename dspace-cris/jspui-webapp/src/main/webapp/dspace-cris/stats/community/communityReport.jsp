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
<c:set var="handlePrefix" scope="page"><%= ConfigurationManager.getProperty("handle.prefix") %></c:set>
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
	         	<c:choose>
	         	<c:when test="${fn:startsWith(data.object.handle, handlePrefix)}">
					<h3><fmt:message key="view.stats-global.page.title"></fmt:message></h3>
				</c:when>
				<c:otherwise>
					<h1><fmt:message key="view.${data.jspKey}.page.title"><fmt:param><a href="${contextPath}/handle/${data.object.handle}">${data.title}</a></fmt:param></fmt:message></h1>
				</c:otherwise>
				</c:choose>				
   			 </div>
		</div>
	</div>

	<div class="pull-right">
		<span class="label label-info"><fmt:message key="view.statistics.range.from" /></span> &nbsp; 
			<c:if test="${empty data.stats_from_date}"><fmt:message key="view.statistics.range.no-start-date" /></c:if>
			${fn:escapeXml(data.stats_from_date)} &nbsp;&nbsp;&nbsp; 
		<span class="label label-info"><fmt:message key="view.statistics.range.to" /></span> &nbsp; 
			<c:if test="${empty data.stats_to_date}"><fmt:message key="view.statistics.range.no-end-date" /></c:if>
			${fn:escapeXml(data.stats_to_date)} &nbsp;&nbsp;&nbsp;
		<a class="btn btn-default" data-toggle="modal" data-target="#stats-date-change-dialog"><fmt:message key="view.statistics.change-range" /></a>
	</div>	

	<c:set var="type"><%=request.getParameter("type") %></c:set>
    <%@include file="/dspace-cris/stats/common/changeRange.jsp"%> 	
		<%@ include file="/dspace-cris/stats/community/_communityReport-right.jsp" %> 
	<div class="richeditor">
		<div class="top"></div>
			<%@ include file="/dspace-cris/stats/community/_communityReport.jsp" %>
		<div class="bottom">
			<c:if test="${data.seeParentObject}">			
				<c:set var="parentLink">${contextPath}/cris/stats/community.html?handle=${data.parentObject.handle}&type=${type}</c:set>
				<div class="list-group">
					<a class="list-group-item" href="${parentLink}"><fmt:message key="view.${data.jspKey}.${type}.parentStats"><fmt:param>${data.parentObject.name}</fmt:param></fmt:message></a>
				</div>
			</c:if>
			<div class="list-group">
				<c:forEach var="child" items="${data.childrenObjects}">
				<c:if test="${child.type eq 3}">
					<a class="list-group-item" href="${contextPath}/cris/stats/collection.html?handle=${child.handle}&type=${type}&stats_from_date=${fn:escapeXml(data.stats_from_date)}&stats_to_date=${fn:escapeXml(data.stats_to_date)}"><fmt:message key="view.${data.jspKey}.${type}.childrenStats"><fmt:param>${child.name}</fmt:param></fmt:message></a>
				</c:if>
				<c:if test="${child.type eq 4}">
					<a class="list-group-item" href="${contextPath}/cris/stats/community.html?handle=${child.handle}&type=${type}&stats_from_date=${fn:escapeXml(data.stats_from_date)}&stats_to_date=${fn:escapeXml(data.stats_to_date)}"><fmt:message key="view.${data.jspKey}.${type}.childrenStats"><fmt:param>${child.name}</fmt:param></fmt:message></a>
				</c:if>	
				</c:forEach>
			</div>
		</div>
	</div>
	</div>
	</div>	
</div>
<div class="clear"></div>

</dspace:layout>
