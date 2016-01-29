<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    https://github.com/CILEA/dspace-cris/wiki/License

--%>
<%@ page contentType="text/html;charset=UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>

<c:choose>
<c:when test="${pmccitation != null}">
<c:choose>
	<c:when test="${pmccitation.numCitations gt 0}">
<fmt:message key="jsp.display-item.citation.pmccentral" />	
	<c:choose>
		<c:when test="${fn:length(pmccitation.pmcRecords) == pmccitation.numCitations}">
			<span class="badge"><a id="openerPMC" data-toggle="modal" href="<%=request.getContextPath()%>/pmcCitedBy?pmid=${pmccitation.id}" data-target="#dialogPMC" title="<fmt:message key="jsp.pmc.details.title" />">${pmccitation.numCitations}</a></span>
		</c:when>
		<c:otherwise>
			<span class="badge"><a target="_blank" href="http://www.ncbi.nlm.nih.gov/pmc/articles/pmid/${pmccitation.id}/citedby/?tool=pubmed"
				title="<fmt:message key="jsp.pmc.details.title" />">${pmccitation.numCitations}</a></span>
		</c:otherwise>
	</c:choose>
	</c:when>
	<c:otherwise>
		<fmt:message key="jsp.pmc.details.notfound" />
	</c:otherwise>	
</c:choose>
</c:when>
<c:otherwise>
	<fmt:message key="jsp.pmc.details.notfound" />
</c:otherwise>
</c:choose>
