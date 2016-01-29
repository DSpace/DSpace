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
<%@page import="javax.servlet.jsp.jstl.fmt.LocaleSupport"%>

<dspace:layout locbar="link" style="submission" navbar="admin"
	titlekey="jsp.dspace-admin.hku.jdyna-configuration.listtabs">

<h1><fmt:message key="jsp.dspace-admin.hku.jdyna-configuration.listtabs" />
	<a target="_blank"
	href='<%=request.getContextPath()%><%=LocaleSupport.getLocalizedMessage(pageContext,
                             "help.site-admin.rp")%>'><fmt:message
	key="jsp.help" /></a></h1>


	<c:if test="${not empty messages}">
		<div class="message" id="successMessages"><c:forEach var="msg"
			items="${messages}">
			<div id="authority-message" class="alert alert-success">${msg}</div>
		</c:forEach></div>
		<c:remove var="messages" scope="session" />
	</c:if>

	<fieldset>
	<ul class="blank-page">
		<c:forEach items="${listTab}" var="tab">
			<li>
			${tab.title} <c:if
				test="${tab.mandatory eq 'false'}">
				<a class="jdynaremovebutton"
					title="<fmt:message
				key="jsp.dspace-admin.hku.jdyna-configuration.deletedynamicfield" />"
					href="<%=request.getContextPath()%>/cris/administrator/${specificPartPath}/deleteTabs.htm?id=${tab.id}">
				<span class="fa fa-trash" id="remove_${tab.id}" ></span> </a>
			</c:if> <a class="jdynaeditbutton"
				title="<fmt:message
				key="jsp.dspace-admin.hku.jdyna-configuration.editdynamicfield" />"
				href="<%=request.getContextPath()%>/cris/administrator/${specificPartPath}/editTabs.htm?id=${tab.id}">
			<span class="fa fa-edit"			
				 id="edit_${tab.id}" ></span> </a>

			</li>
		</c:forEach>
		<a class="btn btn-primary pull-right"
		href="<%=request.getContextPath()%>/cris/administrator/${specificPartPath}/createTabs.htm">
	<fmt:message key="jsp.dspace-admin.hku.jdyna-configuration.newtab" />
	</a>
	</ul>
	</fieldset>

	
</dspace:layout>
