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
	titlekey="jsp.dspace-admin.researchers-import">

	<table width="95%">
		<tr>
			<td align="left">
				<h1>
					<fmt:message key="jsp.dspace-admin.researchers-import" />
				</h1>
			</td>
			<td align="right" class="standard"><a target="_blank"
				href='<%=LocaleSupport.getLocalizedMessage(pageContext,
						"help.site-admin.rp-import")%>'><fmt:message
						key="jsp.help" /></a></td>
		</tr>
	</table>

	<c:if test="${not empty messages}">
		<div class="message" id="successMessages">
			<c:forEach var="msg" items="${messages}">
				<div id="authority-message">${msg}</div>
			</c:forEach>
		</div>
		<c:remove var="messages" scope="session" />
	</c:if>

	<table width="98%" align="left" cellpadding="0" cellspacing="0">
		<tr>
			<td><form:form commandName="dto" method="post"
					enctype="multipart/form-data">


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


					<hr />
					<div class="form-group">
						<label for="type"><fmt:message
								key="jsp.import.advanced.import.select-filter.label" /></label> <select
							class="form-control" name="type" type="select">
							<option value="researcher"><fmt:message
									key="jsp.import.advanced.import.option.researcher" /></option>
							<option value="project"><fmt:message
									key="jsp.import.advanced.import.option.project" /></option>
							<option value="orgunit"><fmt:message
									key="jsp.import.advanced.import.option.orgunit" /></option>
							<c:forEach var="obj" items="${dynamicobjects}">
								<option value="${obj.shortName}">${obj.label}</option>					
							</c:forEach>
						</select>
						<p class="help-block">
							<fmt:message key="jsp.import.advanced.import.select-filter.tip" />
						</p>

						<fmt:message key="jsp.layout.hku.import.box.label" />
						</legend>
						<%--				<select name="format">
						<option value="ExcelBulkChangesService">Excel 97</option>
						<option value="CSVBulkChangesService">CSV</option>
						<option value="XMLBulkChangesService">XML</option>
					</select> --%>
						<input type="hidden" name="format" value="ExcelBulkChangesService" />
						<div class="form-group">
							<label for="file"><fmt:message
									key="jsp.import.advanced.import.label.file" /></label> <input
								type="file" size="50%" name="file" id="file" />
							<p class="help-block">
								<fmt:message key="jsp.import.advanced.import.help.file" />
							</p>
						</div>
					</div>
					<%-- 			<input type="submit"
			value="<fmt:message key="jsp.import.advanced.downloadxsd"/>"
			name="modeXSD" /> --%>
					<input class="btn btn-default" type="submit"
						value="<fmt:message key="jsp.import.advanced.import"/>"
						name="submit" />
				</form:form></td>
		</tr>
	</table>
</dspace:layout>