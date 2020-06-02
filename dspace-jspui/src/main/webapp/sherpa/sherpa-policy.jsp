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
		<c:when test="${empty sherpaResponses or fn:length(sherpaResponses) == 0}">
			<fmt:message key="jsp.sherpa.noresult" />
		</c:when>
		<c:otherwise>
		<c:choose>
			<c:when test="${fn:length(sherpaResponses) == 1}">
				<fmt:message key="jsp.sherpa.oneresult" />
			</c:when>
			<c:otherwise>
				<c:choose>
					<c:when test="${some_errors}">
						<fmt:message key="jsp.sherpa.some-errors" />
					</c:when>
					<c:otherwise>
						<fmt:message key="jsp.sherpa.moreresults" />
					</c:otherwise>
				</c:choose>
			</c:otherwise>
		</c:choose>
			<c:forEach var="sherpaResponse" items="${sherpaResponses}">
				<c:if test="${!sherpaResponse.error}">
					<div class="sherpa-response">
						<!-- New SHERPA v2 API model - each version + deposit option has its own conditions, prerequisites
                        and allowed locations -->
						<c:forEach var="journal" items="${sherpaResponse.journals}">
							<div class="row sherpa-journal">
								<div class="header container">
									<fmt:message key="jsp.sherpa.jornaltitle">
										<c:if test="${!empty journal.titles}">
											<fmt:param value="${journal.titles[0]}" />
										</c:if>
									</fmt:message>
									<fmt:message key="jsp.sherpa.jornalissn">
										<c:if test="${!empty journal.issns}">
											<fmt:param value="${journal.issns[0]}" />
										</c:if>
									</fmt:message>
									<c:choose>
										<c:when test="${empty journal.publisher}">
											<fmt:message key="jsp.sherpa.publisher.unknow" />
										</c:when>
										<c:when test="${empty journal.romeoPub}">
											<fmt:message key="jsp.sherpa.publisher.onlyname">
												<fmt:param value="${journal.romeoPub}" />
											</fmt:message>
										</c:when>
										<c:otherwise>
											<fmt:message key="jsp.sherpa.publisher">
												<fmt:param value="${journal.publisher.name}" />
												<fmt:param value="${journal.url}" />
											</fmt:message>
										</c:otherwise>
									</c:choose>
								</div>
							</div>
							<div class="sherpa-policies">
								<c:forEach var="policy" items="${journal.policies}" varStatus="policy_counter">
									<!-- In this initial implementation we will ignore additional policies as
									more thought is needed for handling and display - displaying the first policy
									should result in the same information available in the v1 API -->
									<c:if test="${policy_counter.index == 0}">
										<!-- Simple "can" / "cannot" style indicators as with legacy data -->
										<div class="sherpaPre">
											<fmt:message key="jsp.sherpa.pre-print.${policy.preArchiving}">
												<fmt:param value="<%=request.getContextPath()%>" />
											</fmt:message>
										</div>
										<div class="sherpaPost">
											<fmt:message key="jsp.sherpa.post-print.${policy.postArchiving}">
												<fmt:param value="<%=request.getContextPath()%>" />
											</fmt:message>
										</div>
										<div class="sherpaPub">
											<fmt:message key="jsp.sherpa.publisher-version.${policy.pubArchiving}">
												<fmt:param value="<%=request.getContextPath()%>" />
											</fmt:message>
										</div>
										<hr/>
										<!-- Link to full policy on ROMeO v2 site -->
										<div class="sherpa-policy-link">
											<a href='<c:out value="${sherpaResponse.metadata.uri}"/>' target='_blank'>
												<fmt:message key="jsp.sherpa.policy-link"/>
											</a>
										</div>
										<!-- Print conditions, restrictions etc for each version (and option within that version) -->
										<c:forEach var="version" items="${policy.permittedVersions}">
											<div class="sherpa-version">
												<c:choose>
													<c:when test="${version.option == 1}">
														<h4>
															<fmt:message key="jsp.sherpa.version.permissions">
																<fmt:param value="${version.articleVersionLabel}"/>
															</fmt:message>
														</h4>
													</c:when>
													<c:otherwise>
														<p class="sherpa-option">
															<fmt:message key="jsp.sherpa.version.option">
																<fmt:param value="${version.option}"/>
															</fmt:message>
														</p>
													</c:otherwise>
												</c:choose>
												<!-- Prerequisites (eg. required by funder) -->
												<c:if test="${not empty version.prerequisites}">
													<div class="sherpa-prerequisites">
														<strong><fmt:message key="jsp.sherpa.prerequisites-heading"/></strong>
														<c:forEach var="prereq" items="${version.prerequisites}">
															<br/>
															<c:out value="${prereq}" />
														</c:forEach>
													</div>
												</c:if>
												<!-- Locations (eg. author's website, institutional repository) -->
												<c:if test="${not empty version.locations}">
													<div class="sherpa-locations">
														<strong><fmt:message key="jsp.sherpa.allowed-location-heading"/></strong><br/>
														<c:forEach var="location" items="${version.locations}" varStatus="loop">
															<c:if test="${loop.index > 0}">, </c:if>
															<c:out value="${location}" />
														</c:forEach>
													</div>
												</c:if>
												<!-- Conditions (eg. must link to publisher version) -->
												<c:if test="${not empty version.conditions}">
													<div class="sherpa-conditions">
														<strong><fmt:message key="jsp.sherpa.conditions-heading"/></strong>
														<c:forEach var="condition" items="${version.conditions}">
															<br/>
															<c:out value="${condition}" />
														</c:forEach>
													</div>
												</c:if>
												<!-- Licences (eg. must be CC-BY-NC-ND) -->
												<c:if test="${not empty version.licenses}">
													<div class="sherpa-licenses">
														<strong><fmt:message key="jsp.sherpa.required-license-heading"/></strong>
														<c:forEach var="license" items="${version.licenses}" varStatus="loop">
															<c:if test="${loop.index > 0}">, </c:if>
															<c:out value="${license}" />
														</c:forEach>
													</div>
												</c:if>
											</div>
										</c:forEach>
									</c:if>
								</c:forEach>
							</div>
							<div class="sherpa-publishers">
								<c:forEach var="publisher" items="${journal.publishers}"/>
								<c:if test="${!empty publisher.paidAccessUrl}">
									<div class="sherpaPaid">
										<p>
											<strong><fmt:message key="jsp.sherpa.paidoption-label"/>: </strong>
											<a href='<c:out value="${publisher.paidAccessUrl}"/>' target="_blank">
												<c:out value="${publisher.paidAccessDescription}"/>
											</a>. <fmt:message key="jsp.sherpa.paidoption-message"/>.
										</p>
									</div>
								</c:if>
							</div>
							<hr/>
							<c:forEach var="policy" items="${journal.policies}" varStatus="policy_counter">
								<c:if test="${policy_counter.index == 0}">
									<div class="sherpa-copyright">
										<fmt:message key="jsp.sherpa.copyright" />
										<ul class="sherpa-copyright-links">
											<c:forEach var="copyright_url" items="${policy.urls}">
												<li><a href="${copyright_url.key}" target="_blank">${copyright_url.value}</a></li>
											</c:forEach>
										</ul>
									</div>
								</c:if>
							</c:forEach>
							<!-- If there are multiple policies, note this -->
							<c:if test="${fn:length(journal.policies) > 0}">
								<p><fmt:message key="jsp.sherpa.multiple-policies">
									<fmt:param value="${sherpaResponse.metadata.uri}"/>
								</fmt:message></p>
							</c:if>
						</c:forEach>
					</div>
				</c:if>
			</c:forEach>
		</c:otherwise>
	</c:choose>
</c:otherwise>
</c:choose>