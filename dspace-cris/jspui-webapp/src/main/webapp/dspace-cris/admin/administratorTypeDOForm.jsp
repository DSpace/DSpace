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

<dspace:layout locbar="link" style="submission" navbar="admin" titlekey="jsp.dspace-admin.do-list">

	<h1><fmt:message key="jsp.dspace-admin.do-list" />
	<a target="_blank" class="pull-right" href='<%=LocaleSupport.getLocalizedMessage(pageContext,
                              "help.site-admin.do-list")%>'><fmt:message
		key="jsp.help" /></a></h1>

	<form:form commandName="dto" method="post">
	<spring:bind path="dto.*">
		<c:if test="${!empty status.errorMessages}">
			<div id="errorMessages">
		</c:if>
		<c:forEach items="${status.errorMessages}" var="error">
			<span class="alert alert-danger"><fmt:message
				key="jsp.layout.hku.prefix-error-code" /> ${error}</span>
			<br />
		</c:forEach>
		<c:if test="${!empty status.errorMessages}">
			</div>
		</c:if>
	</spring:bind>
			
	
		<dyna:text propertyPath="dto.shortName"  helpKey="help.jdyna.message.tdo.shortname"
			labelKey="jsp.layout.hku.label.shortname" visibility="false"/>
		<div class="dynaClear">
			&nbsp;
		</div>
		
		<dyna:text propertyPath="dto.label"  helpKey="help.jdyna.message.tdo.label"
			labelKey="jsp.layout.hku.label.label" visibility="false"/>
		<div class="dynaClear">
			&nbsp;
		</div>
	
		
		<input type="submit" value="<fmt:message key="jsp.layout.hku.researcher.button.save" />" />
		
	</form:form>				 
</dspace:layout>