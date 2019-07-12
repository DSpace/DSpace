<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%--
  - Display message indicating password is incorrect, and allow a retry
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"
    prefix="fmt" %>


<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%@ page import="javax.servlet.jsp.jstl.fmt.LocaleSupport" %>


<dspace:layout titlekey="jsp.login.orcid-incorrect.title">

    <p><fmt:message key="jsp.login.orcid-generic-error.text">
        <fmt:param><%= request.getContextPath() %>/oauth-login</fmt:param>
        <fmt:param><%= request.getAttribute("orcid.standalone.error") %></fmt:param>
    	<fmt:param><%= request.getAttribute("orcid.standalone.ticket") %></fmt:param>        
    </fmt:message></p>

</dspace:layout>
