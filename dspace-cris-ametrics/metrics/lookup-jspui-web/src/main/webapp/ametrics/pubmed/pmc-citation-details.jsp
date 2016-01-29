<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    https://github.com/CILEA/dspace-cris/wiki/License

--%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
  <div class="modal-dialog">
    <div class="modal-content">
      
  
<div class="modal-header">
        <button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
        <h4 class="modal-title"> 
		<fmt:message
		key="jsp.pmc.details.popup.header.citation">
		<fmt:param value="${pmccitation.id}" />
		<fmt:param value="${pmccitation.numCitations}" />		
</fmt:message></h4>        
</div>
<div class="modal-body-with-padding">
  
 <c:forEach var="pmc" items="${pmccitation.pmcRecords}" varStatus="count">


    <c:if test="${!empty pmc.title}">
	<p>${pmc.title}</p>
	</c:if>
	<c:if test="${!empty pmc.authors}">
	<p>${pmc.authors}</p>
	</c:if>
	<c:if test="${!empty pmc.publicationNote}">
	<p>${pmc.publicationNote}</p>
	</c:if>	
	<p>
	<a class="label label-default" href="http://www.ncbi.nlm.nih.gov/pmc/articles/PMC${pmc.id}/" 
		title="<fmt:message
		key="jsp.pmc.details.linkpmc" />">
	<fmt:message
		key="jsp.pmc.details.pmcid"><fmt:param>${pmc.id}</fmt:param></fmt:message>
	</a>
	
	<c:forEach var="pid" items="${pmc.pubmedIDs}">
	<a class="label label-info" href="http://www.ncbi.nlm.nih.gov/pubmed/${pid}/" title="<fmt:message
		key="jsp.pmc.details.linkpubmed" />">
	<fmt:message
		key="jsp.pmc.details.pmid"><fmt:param>${pid}</fmt:param></fmt:message>
	</a>
	</c:forEach>
	<c:forEach var="handle" items="${pmc.handles}">
	<a class="label label-success" href="<%= request.getContextPath() %>/handle/${handle}" 
	title="<fmt:message
		key="jsp.pmc.details.linkdspace" />">
	<fmt:message
		key="jsp.pmc.details.thisrepository"/>
	</a>
	</c:forEach>
	</p>
	
</c:forEach>

</div>
<div class="modal-footer">
	<p><fmt:message	key="jsp.pmc.details.popup.footer.citation"><fmt:param value="${pmccitation.timeStampInfo.lastModificationTime}" /></fmt:message></p>
</div>
  </div>
  </div>