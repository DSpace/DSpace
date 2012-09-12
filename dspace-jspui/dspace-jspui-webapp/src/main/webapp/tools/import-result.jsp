<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://displaytag.sf.net" prefix="display"%>
<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>

<%
	ImportResultBean result = (ImportResultBean) request.getAttribute("result");
%>

<%@page import="org.dspace.app.importer.ImportResultBean"%>
<%@page import="org.dspace.app.importer.SingleImportResultBean"%>

<dspace:layout titlekey="jsp.tools.import-item.results.title">

<c:choose>
	<c:when test="${result.scheduled}">
<h3><fmt:message key="jsp.tools.import-item.results.heading-scheduled" /></h3>
	</c:when>
	<c:otherwise>
<h3><fmt:message key="jsp.tools.import-item.results.heading" /></h3>	
	</c:otherwise>
</c:choose>
<c:choose>
	<c:when test="${result.scheduled}">
		<fmt:message key="jsp.tools.import-item.results.intro-scheduled">
			<fmt:param><%= result.getTot() %></fmt:param>
		</fmt:message>
	</c:when>
	<c:otherwise>
		<fmt:message key="jsp.tools.import-item.results.intro">
			<fmt:param><%= result.getTot() %></fmt:param>
		</fmt:message>
		<fmt:message key="jsp.tools.import-item.results.intro-summary">
			<fmt:param><%= result.getTot() %></fmt:param>
		</fmt:message>
	</c:otherwise>
</c:choose>

<c:if test="${not result.scheduled}">
<fmt:message key="jsp.tools.import-item.results.details.intro" />
<ul>
	<li><fmt:message key="jsp.tools.import-item.results.details.success" />: <%= result.getSuccess() %></li>
	<li><fmt:message key="jsp.tools.import-item.results.details.warning" />: <%= result.getWarning() %></li>
	<li><fmt:message key="jsp.tools.import-item.results.details.error" />: <%= result.getFailure() %></li>
</ul>

<display:table 
	name="requestScope.result.details" 
	id="importResult">
	<display:setProperty name='css.table' value='miscTable'/>
	    <display:setProperty name='css.tr.even' value='oddRowOddCol'/>
	    <display:setProperty name='css.tr.odd' value='oddRowEvenCol'/>
	    <display:setProperty name="paging.banner.all_items_found" value="" />
		<display:column property="importIdentifier" titleKey="jsp.tools.import-item.details.importIdentifier" 
			sortable="false" headerClass="evenRowEvenCol" />
		<display:column titleKey="jsp.tools.import-item.details.status" 
			sortable="false" headerClass="evenRowOddCol">
			<fmt:message key="jsp.tools.import-item.details.status${importResult.status}" />
		</display:column>
		<display:column property="message" titleKey="jsp.tools.import-item.details.message" 
			sortable="false" headerClass="evenRowEvenCol" />
		<display:column titleKey="jsp.tools.import-item.details.witemId" 
			sortable="false" headerClass="evenRowOddCol" style="text-align: center" >
			<c:choose>
				<c:when test="${importResult.witemId eq -1}">
	<fmt:message key="jsp.tools.import-item.details.nowitem" />
				</c:when>
				<c:otherwise>
			<form action="<%= request.getContextPath() %>/workspace" method="post">
               <input type="hidden" name="workspace_id" value="${importResult.witemId}"/>
               <input type="submit" name="submit_open" value="<fmt:message key="jsp.tools.import-item.details.openwitem" />"/>
            </form>
				</c:otherwise>
			</c:choose>
		</display:column>	
</display:table>
</c:if>

<p>
<fmt:message key="jsp.tools.import-item.results.endpage">
	<fmt:param><%= request.getContextPath() %>/mydspace</fmt:param>
</fmt:message>
</p>
</dspace:layout>
