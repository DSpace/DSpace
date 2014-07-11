<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    https://github.com/CILEA/dspace-cris/wiki/License

--%>
<div id="content-stats">

<c:choose>
<c:when test="${empty type or type == 'null' or type eq 'selectedObject'}">
	<c:set value="" var="condition"/>
	<%@include file="../common/basicReport.jsp"%>
</c:when>
<c:otherwise>

	<c:set var="objectName" scope="page">${data.relationType}</c:set>	
	<%@include file="../common/topObject.jsp"%>	
	
</c:otherwise>
</c:choose>
</div>