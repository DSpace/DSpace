<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    https://github.com/CILEA/dspace-cris/wiki/License

--%>
<%@page import="java.util.Map"%>
<%@ page contentType="text/html;charset=UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<%@ taglib uri="researchertags" prefix="researcher"%>
<%@ taglib uri="jdynatags" prefix="dyna"%>
<%@ taglib uri="http://displaytag.sf.net" prefix="display"%>
<%
	

	Map<String,String> relations = (Map<String,String>) request.getAttribute("relations");
	String type = (String)request.getAttribute("type");
	
%>

<% if(type.equals("coauthors"))  { %>

<table id="edgetableinformation" class="table">
	<thead>
		<tr>
			<th class="rowNo">&nbsp;&nbsp;</th>
			<th>Co-authored Publications</th>
			<%--th><fmt:message key="jsp.network.relation.coauthorstitle"/></th--%>
			<%--th><fmt:message key="jsp.network.relation.coauthorshandle"/></th--%>
		</tr>
	</thead>
	<tbody>
<c:set value="0" var="rowIndex" />
		<c:forEach items="${relations}" var="relation" varStatus="relationStatus">
<c:forEach items="${relation.value}" var="entry" varStatus="entryStatus">
<c:set value="${rowIndex + 1}" var="rowIndex" />
			<tr>
				<td>${rowIndex})&nbsp;</td>
				<td><c:if test="${!empty entry}"><a target="new" href="<%=request.getContextPath()%>/handle/${entry}"></c:if>${relation.key}<c:if test="${!empty entry}"></a></c:if></td>
			</tr>
		</c:forEach>
</c:forEach>
	</tbody>
</table>
<% } else if(type.equals("cowinners"))  { %>

<table id="edgetableinformation" class="table">
	<thead>
		<tr>
			<th class="rowNo">&nbsp;&nbsp;</th>
			<th><fmt:message key="jsp.network.relation.awardsdate"/></th>
			<th><fmt:message key="jsp.network.relation.awards"/></th>
			<%--th><fmt:message key="jsp.network.relation.awardsachievements"/></th>
			<th><fmt:message key="jsp.network.relation.awardswith"/><th--%>
		</tr>
	</thead>
	<tbody>
	<% 
	int rowNo = 0;	
	for(String relation : relations.keySet()) {
		rowNo++;
		String[] relationTMP = relation.split("\\|#\\|#\\|#");
		String[] relationA = relationTMP[0].split("###");
//		String[] relationB = relationTMP[1].split("###");
if (!relationA[1].isEmpty()) relationA[1] = relationA[1].substring(relationA[1].indexOf(":")+7);
relationA[3] = "";
	%>
		<tr>
			<td><%=rowNo%>)&nbsp;</td>
	<%	
		for(String rr : relationA) {
		    if(!rr.isEmpty()) {
	%>
		
		
				<td><%= rr.substring(rr.indexOf(":")+1) %></td>
										
			
	<% } } %>
				
				<%--td>
				<div--%>
				<%-- int ii = 0; for(String b : relationB) { 
					if(!b.isEmpty()) { %>
					<% if(ii>0){ %> - <% } %>
					<%
					    try {
				--%%> 
				    <%--= b.substring(b.indexOf("|||")+3, b.lastIndexOf("|||")) --%> 
				<%-- } catch(Exception e) { --%> 
				    
				   <%--= b.substring(b.indexOf("|||")+3) --%> 
				<%-- } } ii++;--%>
					
				<%-- } --%>
				<%--/div>
				</td--%>
		</tr>
		
			<% } %>
	</tbody>
</table>
<% } else if(type.equals("coinvestigators"))  { %>
	<table id="edgetableinformation" class="table">
	<thead>
		<tr>
			<th class="rowNo">&nbsp;&nbsp;</th>
			<th>Co-investigated Grants</th>
		</tr>
	</thead>
	<tbody>
	<c:forEach items="${relations}" var="relation" varStatus="relationStatus">
<c:forEach items="${relation.value}" var="entry" varStatus="entryStatus">
<c:set value="${rowIndex + 1}" var="rowIndex" />
	<tr>
		<td>${rowIndex})&nbsp;</td>
		<td>
			<a target="_blank" href="<%= request.getContextPath() %>/cris/project/details.htm?code=${entry}">${relation.key}</a>
		</td>
	</tr>
</c:forEach>
	</c:forEach>
	</tbody>
	</table>

<% } else if(type.equals("kwdpub"))  { %>
	<table id="edgetableinformation" class="table">
	<thead>
		<tr><th class="rowNo">&nbsp;&nbsp;</th>
		<th>Common Keywords in Publications</th></tr>
	</thead>
	<tbody>
	<c:forEach items="${relations}" var="relation" varStatus="relationStatus">
	<tr>
		<td>${relationStatus.index + 1})&nbsp;</td>
		<td>
		<c:choose>
		<c:when test="${fn:startsWith(authoritytarget, 'rp')}">
			<c:set var="fieldauthorforkedpub" value="author_authority"/>
		</c:when>
		<c:otherwise>
			<c:set var="fieldauthorforkedpub" value="author_keyword"/>			
		</c:otherwise>
	</c:choose>
<a target="_blank" href="<c:url value="/simple-search">
	
	<c:param name="query" value="subject_keyword:\"${relation.key}\" AND ${fieldauthorforkedpub}:\"${authoritytarget}\""/>
	<c:param name="location" value="dspacebasic" />
</c:url>">${relation.key}</a>
		</td>
	</tr>
	</c:forEach>
	</tbody>
	</table>
<% } else if(type.equals("keywordsgrants"))  { %>
	<table id="edgetableinformation" class="table">
	<thead>
		<tr><th class="rowNo">&nbsp;&nbsp;</th>
		<th>Common Keywords in Grants</th></tr>
	</thead>
	<tbody>
	<c:forEach items="${relations}" var="relation" varStatus="relationStatus">
	<tr>
		<td>${relationStatus.index + 1})&nbsp;</td>
		<td>
<a target="_blank" href="<c:url value="/simple-search">	
	<c:param name="query" value="pjkeywords:\"${relation.key}\" AND pjinvestigators:\"${authoritytarget}\"" />
	<c:param name="location" value="crisproject" />	
</c:url>">${relation.key}</a></td>
	</tr>
	</c:forEach>
	</tbody>
	</table>
<% } else if(type.equals("disciplines"))  { %>
	<table id="edgetableinformation" class="table">
	<thead>
		<tr><th class="rowNo">&nbsp;&nbsp;</th>
		<th>Common Disciplines in Grants</th></tr>
	</thead>
	<tbody>
	<c:forEach items="${relations}" var="relation" varStatus="relationStatus">
	<tr>
		<td>${relationStatus.index + 1})&nbsp;</td>
<td><a target="new" href="<c:url value="/simple-search">
	<c:param name="query" value="\"discipline:${relation.key}\" AND pjinvestigators:\"${authoritytarget}\"" />
	<c:param name="location" value="crisproject" />	
</c:url>">${relation.key}</a></td>
	</tr>
	</c:forEach>
	</tbody>
	</table>
<% }  else { %>
<table id="edgetableinformation" class="table">
	<thead>
		<tr><th class="rowNo">&nbsp;&nbsp;</th><th><%if (type.equals("appointments")) {%>Committee Appointments<%}%></th></tr>
	</thead>
	<tbody>
		<c:forEach items="${relations}" var="relation" varStatus="relationStatus">
			<tr>
				<td>${relationStatus.index + 1})&nbsp;</td>
				<td>${relation.key}</td>		
			</tr>
		</c:forEach>
	</tbody>
</table>
<% } %>


<script>

	jQuery(document).ready(function() {
		jQuery("#edgetableinformation").dataTable({
			"bFilter" : false,
			"bDestroy" : true,
			"bSort" : false,
			"bInfo" : false,
			"bLengthChange" : false,
			"bPaginate" : false
		});
	});
</script>
