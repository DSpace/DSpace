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

<c:if test="${pmccitation.numCitations gt 0}">
<c:choose>
<c:when test="${fn:length(pmccitation.pmcRecords) == pmccitation.numCitations}">
<a id="openerPMC" title="<fmt:message key="jsp.pmc.details.title" />">${pmccitation.numCitations}</a>

<div id="dialogPMC" title="<fmt:message key="jsp.pmc.details.title" />">
	<p id="pPMC"></p>
</div>

				<script>
					j(function() {
	
						j("#dialogPMC").dialog({
							autoOpen : false,
							modal : true,
							width: 600, 
							height: 480							
						});
						j("#openerPMC").click(function() {
							j.get("<%=request.getContextPath()%>/pmcCitedBy?pmid=${pmccitation.id}", function(data) {
								j("#pPMC").html(data);
								j("#dialogPMC").dialog("open");
							});
							
						});
					});
				</script>
			
			
</c:when>
<c:otherwise>
<a target="_blank" href="http://www.ncbi.nlm.nih.gov/pmc/articles/pmid/${pmccitation.id}/citedby/?tool=pubmed"
	title="<fmt:message key="jsp.pmc.details.title" />">${pmccitation.numCitations}</a>
</c:otherwise>
</c:choose>
</c:if>

<%--fieldset><legend><fmt:message
		key="jsp.pmc.fieldset.title" /></legend>

<table>
	<tbody>
		<tr>
			<td width="100%">
			<fmt:message
		key="jsp.pmc.citation">
		<fmt:param value="${pmccitation.id}" />
		<fmt:param value="${pmccitation.numCitations}"  />
		<fmt:param value="${pmccitation.timeStampInfo.lastModificationTime}" />
	</fmt:message>
			</td>
			<td nowrap="true" style="padding: 0px;">

	<c:choose>
	 <c:when
		test="${fn:length(pmccitation.pmcRecords) == pmccitation.numCitations}">
		
		<a onclick="Modalbox.show(this.href, {title: this.title, width: 600, height: 480}); return false;"
			href="<%= request.getContextPath() %>/pmcCitedBy?pmid=${pmccitation.id}"
			title="<fmt:message	key="jsp.pmc.details.title" />">
		<font style="color:white; background-color: red">&nbsp;&nbsp;<fmt:message key="jsp.pmc.citation.seedetails" />&nbsp;&nbsp;</font></a>
	</c:when>
	<c:otherwise>
			<a target="_blank" href="http://www.ncbi.nlm.nih.gov/pmc/articles/pmid/${pmccitation.id}/citedby/?tool=pubmed"
				title="<fmt:message	key="jsp.pmc.details.title" />">
				<font style="color:white; background-color: red">&nbsp;&nbsp;<fmt:message key="jsp.pmc.citation.seedetailsonpmc" />&nbsp;&nbsp;</font>
			</a>
	</c:otherwise>
	</c:choose>
	
			</td>
		</tr>
	</tbody>
</table>


</fieldset--%>

