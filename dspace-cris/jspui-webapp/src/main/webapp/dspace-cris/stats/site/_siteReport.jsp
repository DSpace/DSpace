<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    https://github.com/CILEA/dspace-cris/wiki/License

--%>
<div id="content-stats">
<c:choose>
<c:when test="${type eq 'community'}">
	<c:set value="2" var="markerasnumber"/>
	<%@include file="../common/topCommunity.jsp"%>
</c:when>
<c:when test="${type eq 'collection'}">
	<c:set value="3" var="markerasnumber"/>
	<%@include file="../common/topCollection.jsp"%>	
</c:when>
<c:when test="${(type eq 'upload') && (data.seeUpload)}">
	<c:set value="6" var="markerasnumber"/>
	<%@include file="../common/upload.jsp"%>
</c:when>
<c:when test="${type eq 'bitstream'}">
	<c:set value="5" var="markerasnumber"/>
	<%@include file="../common/topBitStream.jsp"%>
</c:when>
<c:otherwise>
	<c:set value="1" var="markerasnumber"/>
	<%@include file="../common/topSite.jsp"%>
</c:otherwise>
</c:choose>
</div>
