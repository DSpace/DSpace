<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"
    prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"%>
<fmt:message key="jsp.sherpa.heading">
	<fmt:param value="<%=request.getContextPath()%>" />
</fmt:message>
<c:choose>
<c:when test="${error}">
	<fmt:message key="jsp.sherpa.error" />
</c:when>
<c:otherwise>
	<c:choose>
		<c:when test="${empty result or fn:length(result) == 0}">
			<fmt:message key="jsp.sherpa.noresult" />	
		</c:when>
		<c:otherwise>
			<c:choose>
				<c:when test="${fn:length(result) == 1}">
					<fmt:message key="jsp.sherpa.oneresult" />
				</c:when>
				<c:otherwise>
					<fmt:message key="jsp.sherpa.moreresults" />
				</c:otherwise>
			</c:choose>
			
			<c:forEach var="r" items="${result}">			
				<div class="${r[1].romeocolour}RomeoPublisher row">
				<div class="header container">
					<fmt:message key="jsp.sherpa.jornaltitle">
						<fmt:param value="${r[0].title}" />
					</fmt:message>
					<fmt:message key="jsp.sherpa.jornalissn">
						<fmt:param value="${r[0].issn}" />
					</fmt:message>
					<c:choose>
						<c:when test="${empty r[1]}">
							<fmt:message key="jsp.sherpa.publisher.unknow" />
						</c:when>
						<c:when test="${empty r[0].romeopub}">
							<fmt:message key="jsp.sherpa.publisher.onlyname">
								<fmt:param value="${r[0].romeopub}" />
							</fmt:message>
						</c:when>
						<c:otherwise>
							<fmt:message key="jsp.sherpa.publisher">
								<fmt:param value="${r[1].name}" />
								<fmt:param value="${r[1].homeurl}" />
							</fmt:message>
						</c:otherwise>
					</c:choose>
				</div>
				<div class="policy container">
					<c:choose>
						<c:when test="${empty r[1]}">
							<fmt:message key="jsp.sherpa.publisher.nodata" />
						</c:when>
						<c:otherwise>
							<div class="sherpaPre">
								<fmt:message key="jsp.sherpa.pre-print.${r[1].prearchiving}">
									<fmt:param value="<%=request.getContextPath()%>" />
								</fmt:message>
								<c:if test="${r[1].prearchiving eq 'restricted'}">
								<ul><c:forEach var="cond" items="${r[1].prerestriction}"><li>${cond}</li></c:forEach></ul>
								</c:if>
							</div>
							
							<div class="sherpaPost">
								<fmt:message key="jsp.sherpa.post-print.${r[1].postarchiving}">
									<fmt:param value="<%=request.getContextPath()%>" />
								</fmt:message>
								<c:if test="${r[1].postarchiving eq 'restricted'}">
								<ul><c:forEach var="cond" items="${r[1].postrestriction}"><li>${cond}</li></c:forEach></ul>
								</c:if>
							</div>
							
							<div class="sherpaPub">
								<fmt:message key="jsp.sherpa.publisher-version.${r[1].pubarchiving}">
									<fmt:param value="<%=request.getContextPath()%>" />
								</fmt:message>
								<c:if test="${r[1].pubarchiving eq 'restricted'}">
								<ul><c:forEach var="cond" items="${r[1].pubrestriction}"><li>${cond}</li></c:forEach></ul>
								</c:if>
							</div>
							
							<div class="sherpaConditions">
								<fmt:message key="jsp.sherpa.generalconditions" />
								<ul><c:forEach var="cond" items="${r[1].condition}"><li>${cond}</li></c:forEach></ul>
							</div>
							
							<div class="sherpaPaid">
								<fmt:message key="jsp.sherpa.paidoption">
									<fmt:param value="${r[1].paidaccessname}" />
									<fmt:param value="${r[1].paidaccessurl}" />
									<c:choose>
									<c:when test="${!empty r[1].paidaccessnotes}">
										<fmt:param value="${r[1].paidaccessnotes}" />
									</c:when>
									<c:otherwise>
										<fmt:param value="" />
									</c:otherwise>
									</c:choose>
								</fmt:message>
							</div>
							
							<div class="sherpaCopyright">
								<fmt:message key="jsp.sherpa.copyright" />
								<ul><c:forEach var="copyright" items="${r[1].copyright}"><li><a href="${copyright[1]}" target="_blank">${copyright[0]}</a></li></c:forEach></ul>
							</div>
							
							<div class="sherpaColor">
								<fmt:message key="jsp.sherpa.publisher.romeocolour">
									<fmt:param value="${r[1].romeocolour}" />
								</fmt:message>
							</div>
						</c:otherwise>
					</c:choose>
				</div>
				<fmt:message key="jsp.sherpa.legend" />
				</div>
			</c:forEach>
		</c:otherwise>
	</c:choose>
</c:otherwise>
</c:choose>