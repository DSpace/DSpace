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
<%@ taglib uri="jdynatags" prefix="dyna"%>
<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>
<%@page import="javax.servlet.jsp.jstl.fmt.LocaleSupport"%>

<dspace:layout locbar="link" style="submission" navbar="admin" titlekey="jsp.dspace-admin.cris.ous-list">
<h1><fmt:message key="jsp.dspace-admin.cris.ous-list" />
<a target="_blank"
	href='<%=LocaleSupport.getLocalizedMessage(pageContext,
                             "help.site-admin.cris.ou-list")%>'><fmt:message
	key="jsp.help" /></a></h1>
	
	<form:form commandName="dto" method="post">
	<c:set value="${message}" var="message" scope="request"/>	
	<c:if test="${!empty message}">		
    <div class="alert alert-default"><fmt:message key="${message}"/></div>    
	</c:if>
	<c:if test="${not empty messages}">
	<div class="message" id="successMessages"><c:forEach var="msg"
		items="${messages}">
		<div class="alert alert-success">${msg}</div>
	</c:forEach></div>
	<c:remove var="messages" scope="session" />
	</c:if>
		<%--  first bind on the object itself to display global errors - if available  --%>
		<spring:bind path="dto">
			<c:forEach items="${status.errorMessages}" var="error">
				<span id="errorMessage"><fmt:message key="jsp.layout.hku.prefix-error-code"/> ${error}</span>
				<br>
			</c:forEach>
		</spring:bind>
										
		<display:table name="${dto}" cellspacing="0" cellpadding="0" 
			requestURI="" id="objectList" htmlId="objectList"  class="table" export="false">
			<display:column headerClass="id" titleKey="jsp.layout.table.cris.admin-list.id" property="id" url="/cris/ou/details.htm" paramId="id" paramProperty="id" sortable="true" />							
			<display:column headerClass="uuid" titleKey="jsp.layout.table.cris.admin-list.uuid" property="uuid" url="/cris/ou/details.htm" paramId="id" paramProperty="id" sortable="true" />										
			<display:column headerClass="sourceID" class="sourceID" titleKey="jsp.layout.table.cris.admin-list.sourceID" sortable="false">
							<a href="<%=request.getContextPath()%>/cris/ou/details.htm?sourceid=${objectList.sourceID}&sourceref=${objectList.organizationUnit.sourceRef}">${objectList.sourceID}<c:if test="${!empty objectList.organizationUnit.sourceRef}">/${objectList.organizationUnit.sourceRef}</c:if></a>
			</display:column>										
			<display:column headerClass="name" titleKey="jsp.layout.table.cris.admin-list.name" property="name" sortable="false"/>					
			<display:column headerClass="active" titleKey="jsp.layout.table.cris.admin-list.status" sortable="true" sortProperty="status">				
				<form:checkbox cssClass="active" path="list[${objectList_rowNum-1}].status" value="1"/>				
			</display:column>

		</display:table>
		
		<input type="submit" class="btn btn-primary" 
		value="<fmt:message key="jsp.layout.hku.researcher.button.save" />" />
		
	</form:form>				 
</dspace:layout>
