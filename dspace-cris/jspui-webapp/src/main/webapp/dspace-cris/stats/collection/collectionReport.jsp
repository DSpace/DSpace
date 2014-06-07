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
	<link href="${contextPath}/css/stats.css" type="text/css" rel="stylesheet" />
	<style type="text/css">
	  #map_canvas { height: 100% }
	</style>
	<script type="text/javascript"
	    src="http://maps.google.com/maps/api/js?sensor=true">
	</script>	
	
	<script type="text/javascript">
		function setMessage(message,div){
			document.getElementById(div).innerHTML=message;
		}
		function setGenericEmpityDataMessage(div){
			document.getElementById(div).innerHTML='<fmt:message key="view.generic.data.empty" />';
		}
	</script>
</c:set>

<c:set var="type"><%=request.getParameter("type") %></c:set>
<c:set var="dspace.cris.navbar" scope="request">
	<%@ include file="/dspace-cris/stats/collection/_collectionReport-right.jsp" %>
</c:set>



<dspace:layout titlekey="jsp.statistics.item-title">

<div id="content">
<div class="title detail"><h1><fmt:message key="view.${data.jspKey}.page.title"><fmt:param>${data.title}</fmt:param></fmt:message></h1></div>

<div class="richeditor">
<div class="top"></div>
<div class="container">
	<%@ include file="/dspace-cris/stats/collection/_collectionReport.jsp" %>
</div>
<div class="bottom"></div>
</div>
<div class="clear"></div>
</div>

</dspace:layout>
