<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    https://github.com/CILEA/dspace-cris/wiki/License

--%>
<%@ page contentType="text/html; charset=UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%@ taglib uri="http://www.springframework.org/tags/form" prefix="form"%>
<%@ taglib uri="http://www.springframework.org/tags" prefix="spring"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<%@ taglib uri="http://displaytag.sf.net" prefix="display"%>
<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace"%>
<%@page import="javax.servlet.jsp.jstl.fmt.LocaleSupport"%>

<c:set var="dspace.layout.head.last" scope="request">
	<script type="text/javascript" src="<%= request.getContextPath() %>/static/js/jquery/jquery-1.8.2.min.js"></script>
		<script type="text/javascript">
	var j = jQuery.noConflict();
	j(document).ready(function() {
		j("#create").click(function() {
			location.href = '<%= request.getContextPath() %>/cris/administrator/webservices/edit.htm';			
		});		
	});
    </script>
</c:set>
<dspace:layout locbar="link" style="submission"	titlekey="jsp.dspace-admin.rp">
<h1><fmt:message key="jsp.dspace-admin.userws" />
<a target="_blank"
	href='<%=request.getContextPath()%><%=LocaleSupport.getLocalizedMessage(pageContext,
                             "help.site-admin.userws")%>'><fmt:message
	key="jsp.help" /></a></h1>

	<c:if test="${not empty messages}">
		<div class="message" id="successMessages"><c:forEach var="msg"
			items="${messages}">
			<div id="authority-message" class="alert alert-success">${msg}</div>
		</c:forEach></div>
		<c:remove var="messages" scope="session" />
	</c:if>
	<c:if test="${!empty error}">
		<span id="errorMessage" class="alert alert-danger"><fmt:message key="jsp.layout.hku.prefix-error-code"/> <fmt:message key="${error}"/></span>
	</c:if>

	<display:table name="${listUsers}" cellspacing="0" cellpadding="0" 
			requestURI="" id="objectList" htmlId="objectList"  class="table" export="false">
			<display:column titleKey="jsp.layout.table.hku.ws.type" sortable="true">
					${objectList.typeDef}
			</display:column>
			
			<c:choose>
			<c:when test="${!empty objectList.username}">
				<display:column titleKey="jsp.layout.table.hku.ws.info" url="/cris/administrator/webservices/edit.htm" paramId="id" paramProperty="id" sortable="true">
					${objectList.username}				
				</display:column>
			</c:when>
			<c:otherwise>			
				<display:column titleKey="jsp.layout.table.hku.ws.info" url="/cris/administrator/webservices/edit.htm" paramId="id" paramProperty="id" sortable="true">
		    		${objectList.token} &nbsp; (${objectList.fromIP}<c:if test="${!empty objectList.toIP}">/${objectList.toIP}</c:if>)				
				</display:column>
			</c:otherwise>
			</c:choose>
						
			<display:column titleKey="jsp.layout.table.hku.researchers.enabled" property="enabled" sortable="true"/>									

			<display:column>
				<a href="<%= request.getContextPath() %>/cris/administrator/webservices/delete.htm?id=${objectList.id}"><fmt:message key="jsp.layout.hku.ws.delete"/></a>		
			</display:column>
			
	</display:table>
	<br/>
	<input class="btn btn-primary" type="button" id="create" value="<fmt:message key="jsp.layout.hku.ws.create.new"/>"/>
	
	
</dspace:layout>
