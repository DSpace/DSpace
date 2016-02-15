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


<dspace:layout locbar="link" navbar="admin"
	titlekey="jsp.dspace-admin.researchers-export">


	<form:form commandName="dto" method="post" name="dto">

		<%-- if you need to display all errors (both global and all field errors,
                                 use wildcard (*) in place of the property name --%>
		<spring:bind path="dto.*">
			<c:if test="${not empty status.errorMessages}">
				<div id="errorMessage">
					<c:forEach items="${status.errorMessages}" var="error">
						<span class="errorMessage"><fmt:message
								key="jsp.layout.hku.prefix-error-code" /> ${error}</span>
						<br />
					</c:forEach>
				</div>
			</c:if>
		</spring:bind>

		<div class="form-group">
			<label for="query"><fmt:message
					key="jsp.export.advanced.export.query.label" /></label>
					 
			<input type="text" class="form-control" id="query" name="query" placeholder="*:*">
			<p class="help-block">
				<fmt:message key="jsp.export.advanced.export.query.tip" />
			</p>
		</div>

		<div class="form-group">
			<label for="filter"><fmt:message
					key="jsp.export.advanced.export.select-filter.label" /></label> <select
				class="form-control" name="filter" type="select">
				<option value="9"><fmt:message
						key="jsp.export.advanced.export.option.researcher" /></option>
				<option value="10"><fmt:message
						key="jsp.export.advanced.export.option.project" /></option>
				<option value="11"><fmt:message
						key="jsp.export.advanced.export.option.orgunit" /></option>
				<c:forEach var="obj" items="${dynamicobjects}">
					<option value="${1000 + obj.id}">${obj.label}</option>					
				</c:forEach>
			</select>
			<p class="help-block">
				<fmt:message key="jsp.export.advanced.export.select-filter.tip" />
			</p>
		</div>
		<%-- 		<input type="submit"
			value="<fmt:message key="jsp.export.advanced.export1"/>"
			name="mainMode" /> --%>


		<input class="btn btn-default" type="submit"
			value="<fmt:message key="jsp.export.advanced.export2"/>"
			name="mainModeA" />

	</form:form>


</dspace:layout>