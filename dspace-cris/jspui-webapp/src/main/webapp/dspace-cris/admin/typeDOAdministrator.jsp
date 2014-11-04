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
<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace"%>
<%@ taglib uri="http://displaytag.sf.net" prefix="display"%>
<%@page import="javax.servlet.jsp.jstl.fmt.LocaleSupport"%>
<%@page import="org.dspace.app.cris.model.CrisConstants"%>

<c:set var="CRIS_DYNAMIC_TYPE_ID_START"><%=CrisConstants.CRIS_DYNAMIC_TYPE_ID_START%></c:set>
<dspace:layout locbar="link" style="submission" navbar="admin" titlekey="jsp.dspace-admin.do">
			<h1><fmt:message key="jsp.dspace-admin.do" />
			<a class="pull-right" target="_blank" href='<%=request.getContextPath()%><%=LocaleSupport.getLocalizedMessage(pageContext,
                                "help.site-admin.do")%>'><fmt:message
				key="jsp.help" /></a></h1>

	<c:if test="${not empty messages}">
		<div class="alert alert-success" id="successMessages"><c:forEach var="msg"
			items="${messages}">
			<div id="authority-message">${msg}</div>
		</c:forEach></div>
		<c:remove var="messages" scope="session" />
	</c:if>


	<c:if test="${!empty error}">
		<span id="errorMessage" class="alert alert-danger"><fmt:message key="jsp.layout.hku.prefix-error-code"/> <fmt:message key="${error}"/></span>
	</c:if>

	<ul>
		<li>
			<a id="addentity" href="<%=request.getContextPath()%>/cris/administrator/do/add.htm"><fmt:message
			key="jsp.dspace-admin.hku.add-typodynamicobject" /></a>	
		</li>
	</ul>
	
	<fieldset><legend><fmt:message key="jsp.dspace-admin.hku.list-typodynamicobject" /></legend>
	<div style="padding: 0; margin: 0 10px;">			
			<display:table name="${researchobjects}" cellspacing="0" cellpadding="0" 
			requestURI="" id="objectList" htmlId="objectList"  class="table" export="false">
				<display:column titleKey="jsp.layout.table.cris.admin-list.id"> 
					<a href="<%=request.getContextPath()%>/cris/administrator/${objectList.shortName}/index.htm?id=${objectList.id}">${objectList.id}</a>
				</display:column>									
				<display:column titleKey="jsp.layout.table.cris.admin-list.shortname" sortable="true" >
					<a href="<%=request.getContextPath()%>/cris/administrator/${objectList.shortName}/index.htm?id=${objectList.id}">${objectList.shortName}</a>
			   </display:column>
				<display:column titleKey="jsp.layout.table.cris.admin-list.label" property="label" sortable="true"/>
				<display:column titleKey="jsp.layout.table.cris.admin-list.typodef" sortable="true">
					${objectList.id + CRIS_DYNAMIC_TYPE_ID_START}
				</display:column>
				<display:column titleKey="jsp.layout.table.cris.admin-list.actions">
					<a href="<%=request.getContextPath()%>/cris/administrator/do/edit.htm?id=${objectList.id}">Edit</a>
					<a href="<%=request.getContextPath()%>/cris/administrator/do/delete.htm?id=${objectList.id}">Delete</a>					
				</display:column>	
			</display:table>
	</div>
	</fieldset>
</dspace:layout>
