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
<!DOCTYPE fieldset PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<title><fmt:message
		key="jsp.pmc.details.title" /> 
		<fmt:message
		key="jsp.pmc.details.citation">
		<fmt:param value="${pmccitation.id}" />
		<fmt:param value="${pmccitation.numCitations}" />
		<fmt:param value="${pmccitation.timeStampInfo.lastModificationTime}" />
</fmt:message></title>
<link rel="stylesheet" href="<%= request.getContextPath() %>/static/css/modalbox.css" type="text/css" media="screen" />
<link rel="stylesheet" href="<%= request.getContextPath() %>/static/css/pmc.css" type="text/css" /></head>

<body>
<ul class="pmcCitedByList">
<c:forEach var="pmc" items="${pmccitation.pmcRecords}">
<li>
	<c:if test="${!empty pmc.title}">
	<span class="pmcTitle">${pmc.title}</span><br/>
	</c:if>
	<c:if test="${!empty pmc.authors}">
	<span class="pmcAuthors">${pmc.authors}</span><br/>
	</c:if>
	<c:if test="${!empty pmc.publicationNote}">
	<span class="pmcPublication">${pmc.publicationNote}</span><br/>
	</c:if>	
	<a class="pmcLink" 	href="http://www.ncbi.nlm.nih.gov/pmc/articles/PMC${pmc.id}/" 
		title="<fmt:message
		key="jsp.pmc.details.linkpmc" />">
	PMC${pmc.id}
	</a>
	
	<c:forEach var="pid" items="${pmc.pubmedIDs}">
	| <a class="pmcLink" href="http://www.ncbi.nlm.nih.gov/pubmed/${pid}/" title="<fmt:message
		key="jsp.pmc.details.linkpubmed" />">
	PMID ${pid}
	</a>
	</c:forEach>
	<c:forEach var="handle" items="${pmc.handles}">
	| <a class="pmcLink" href="<%= request.getContextPath() %>/handle/${handle}" 
	title="<fmt:message
		key="jsp.pmc.details.linkdspace" />">
	Hub Item
	</a>
	</c:forEach>
	</li>
</c:forEach>
</ul>
</body>
</html>