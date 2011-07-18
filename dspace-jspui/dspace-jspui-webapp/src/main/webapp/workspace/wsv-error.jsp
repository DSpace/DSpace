<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%--
   - This page is displayed if there was a problem processing the workspace
   - request
   --%>
<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<dspace:layout titlekey="jsp.workspace.wsv-error.title">

<h1><fmt:message key="jsp.workspace.wsv-error.heading"/></h1>

<p>
<fmt:message key="jsp.workspace.wsv-error.errormsg1"/>
</p>

<p>
<fmt:message key="jsp.workspace.wsv-error.errormsg2"/>
</p>

<dspace:include page="/components/contact-info.jsp" />

<p align=center>
    <a href="<%= request.getContextPath() %>/"><fmt:message key="jsp.general.gohome"/></a>
</p>

</dspace:layout>
