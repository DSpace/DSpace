<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    https://github.com/CILEA/dspace-cris/wiki/License

--%>
<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="utf-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>
<%@ taglib uri="http://displaytag.sf.net" prefix="display"%>
<%@ taglib uri="http://www.springframework.org/tags" prefix="spring"%>

<%@ taglib uri="jdynatags" prefix="dyna"%>
<%@ taglib uri="researchertags" prefix="researcher"%>
	
	<div id="tabs">
		<ul>
					<c:forEach items="${tabList}" var="area" varStatus="rowCounter">
						<c:set var="tablink"><c:choose>
							<c:when test="${rowCounter.count == 1}">${root}/cris/${specificPartPath}/${authority}?onlytab=true</c:when>
							<c:otherwise>${root}/cris/${specificPartPath}/${authority}/${area.shortName}.html?onlytab=true</c:otherwise>
						</c:choose></c:set>
						<li data-tabname="${area.shortName}" id="bar-tab-${area.id}">
						<c:choose>
							<c:when test="${area.id == tabId}">
								<a href="#tab-${area.id}">
								<c:if test="${!empty area.ext}">
								<img style="width: 16px;vertical-align: middle;" border="0" 
									src="<%=request.getContextPath()%>/cris/researchertabimage/${area.id}" alt="icon" />
								</c:if>	
								<spring:message code="${entity.class.simpleName}.tab.${area.shortName}.label" text="${area.title}"></spring:message></a>
							</c:when>
							<c:otherwise>
									<a href="${tablink}">
									<c:if test="${!empty area.ext}">
									<img style="width: 16px;vertical-align: middle;" border="0"
										src="<%=request.getContextPath()%>/cris/researchertabimage/${area.id}"
			    						alt="icon" />
			    					</c:if>	
			    					<spring:message code="${entity.class.simpleName}.tab.${area.shortName}.label" text="${area.title}"></spring:message></a>
							</c:otherwise>
						</c:choose></li>

					</c:forEach>
		</ul>
	

<c:forEach items="${tabList}" var="areaIter" varStatus="rowCounter">
	<c:if test="${areaIter.id == tabId}">
	<c:set var="area" scope="request" value="${areaIter}"></c:set>
	<jsp:include page="singleTabDetailsPage.jsp"></jsp:include>
	</c:if>
	
</c:forEach>

</div>
<div class="clearfix">&nbsp;</div>