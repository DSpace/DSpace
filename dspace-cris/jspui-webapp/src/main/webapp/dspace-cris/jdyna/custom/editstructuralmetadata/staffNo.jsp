<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    https://github.com/CILEA/dspace-cris/wiki/License

--%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/sql" prefix="sql"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<%@ taglib uri="http://www.springframework.org/tags/form" prefix="form" %>
<%@ taglib uri="http://www.springframework.org/tags" prefix="spring" %>


<%@ taglib uri="jdynatags" prefix="dyna" %>

<%@ taglib uri="http://ajaxtags.org/tags/ajax" prefix="ajax" %>
<c:set var="root"><%=request.getContextPath()%></c:set>
<%
	Boolean isAdminB = (Boolean) request.getAttribute("is.admin");
	boolean isAdmin = (isAdminB != null ? isAdminB.booleanValue()
			: false);
%>


<table width="98%" cellpadding="0" cellspacing="4">

	<tr>
		<td>
		<table id="tabledatafields" align="left" cellpadding="0"
			cellspacing="4">
			&nbsp;

			<%
				if (isAdmin) {
			%>

			<tr>
				<td><fmt:message key="jsp.layout.hku.primarydata.label.staffno" /></td>
				<td><form:input path="staffNo" size="80%" /></td>
				<td></td>
			</tr>
			<tr>

				<td><fmt:message
					key="jsp.layout.hku.primarydata.label.fullname" /></td>
				<td><form:input path="fullName" size="80%" /></td>
				<td></td>
			</tr>
			<tr>
				<td>&nbsp;</td>
			</tr>
			<%
				} else {
			%>
			<tr>
				<td><fmt:message key="jsp.layout.hku.primarydata.label.staffno" /></td>
				<td>${anagraficadto.sourceID}</td>
				<td></td>
			</tr>
			<tr>

				<td><fmt:message
					key="jsp.layout.hku.primarydata.label.fullname" /></td>
				<td>${anagraficadto.fullName}</td>
				<td></td>
			</tr>
			<tr>
				<td>&nbsp;</td>
			</tr>
			<%
				}
			%>


			<p></p>
		</table>
		</td>
	</tr>
</table>
